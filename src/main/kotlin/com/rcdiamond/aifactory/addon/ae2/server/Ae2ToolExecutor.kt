package com.rcdiamond.aifactory.addon.ae2.server

import appeng.api.networking.IGrid
import appeng.api.networking.crafting.CalculationStrategy
import appeng.api.networking.crafting.CraftingSubmitErrorCode
import appeng.api.networking.crafting.ICraftingPlan
import appeng.api.networking.crafting.ICraftingSimulationRequester
import appeng.api.networking.crafting.ICraftingSubmitResult
import appeng.api.networking.crafting.UnsuitableCpus
import appeng.api.networking.security.IActionHost
import appeng.api.networking.security.IActionSource
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import appeng.api.stacks.KeyCounter
import appeng.api.storage.AEKeyFilter
import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.addon.ae2.part.MEIntelligentTerminalPart
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2InventoryView
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2NbtView
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2Tag
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2TagType
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2ToolArguments
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2ToolLimits
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongArrayTag
import net.minecraft.nbt.LongTag
import net.minecraft.nbt.ShortTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.function.Consumer

/**
 * Server-side dispatch for the five ME terminal operations.
 *
 * The addon backend calls [execute] on the server thread once it has verified the caller's session and
 * terminal lock; this class still re-resolves the terminal itself, so it never trusts an identifier
 * that came from a client. It reads the grid's tick-cached inventory (never mutating it), identifies
 * keys by a full-content fingerprint, and answers with bounded JSON.
 *
 * ## Crafting plans are plans only
 * [Ae2ToolLimits.OPERATION_CRAFTING_PLAN] runs the real AE2 crafting calculation
 * (`ICraftingService.beginCraftingCalculation` with [CalculationStrategy.REPORT_MISSING_ITEMS]) and
 * reports its actual `missingItems`, `bytes` and `simulation` result. It **never** calls `submitJob`,
 * so nothing is ever queued, extracted or reserved: a plan is a snapshot of what would be needed at
 * the moment it was computed.
 *
 * ## Requesting a craft is an irreversible side effect
 * [Ae2ToolLimits.OPERATION_REQUEST_CRAFTING] computes the same plan, then - once the calculation has
 * finished and only if it is not a simulation - submits it with
 * `ICraftingService.submitJob(plan, null, null, true, source)`: AE2 itself extracts the used items,
 * runs the job and puts the output back into the network. Before that step the terminal is resolved
 * again, the grid must be the *same* grid the plan was computed for, the key must still be craftable
 * and the caller's [authorize] callback (the shared session lease and the mounted cell) is re-checked.
 * A disconnection, a changed grid, a lost lease or a rejected CPU is reported as `ok:false` and never
 * as a submitted job. A missing-item plan is rejected with the real `missingItems` instead of being
 * submitted, since AE2 refuses an incomplete plan anyway.
 *
 * ## Asynchronous and non-blocking
 * The calculation future is never awaited on the server thread. [register] installs one server tick
 * listener that only calls `isDone()`, then reads the finished plan and re-validates the terminal
 * before replying or submitting. Requests are bounded by [Ae2ToolLimits.MAX_PENDING_PLANS] and by
 * [Ae2ToolLimits.PLAN_TIMEOUT_TICKS]; an abandoned plan is cancelled the way AE2's own crafting
 * confirmation menu cancels one, and every pending future is cancelled when the server stops so a
 * stale plan can never submit against a later server.
 */
object Ae2ToolExecutor {
    private val LOG = LogUtils.getLogger()

    /**
     * `true` when re-resolving the terminal. The core already verified the caller holds the locked
     * session before invoking this executor, so distance is not re-enforced here (the model may be
     * reasoning while the screen is closed); the terminal still enforces loaded, same dimension,
     * active grid and `mayInteract`.
     */
    private const val ALLOW_REMOTE_AFTER_LOCK = true

    /**
     * Passed to `ICraftingService.submitJob` as `prioritizePower`, the value AE2's own craft-confirmation
     * screen uses: with no CPU selected AE2 auto-picks a suitable one and prefers the fastest, matching how
     * a player-triggered craft behaves.
     */
    private const val PRIORITIZE_POWER = true

    /** Upper bound on the craftable-key probe used when a fingerprint is not in the network inventory. */
    private const val MAX_CRAFTABLE_SCAN = 20_000

    /** Cached fingerprints, bounded so a long session cannot grow them without limit. */
    private const val FINGERPRINT_CACHE_SIZE = 8192

    private val fingerprintByKey = lruCache<AEKey, String>(FINGERPRINT_CACHE_SIZE)
    private val keyByFingerprint = lruCache<String, AEKey>(FINGERPRINT_CACHE_SIZE)

    private val allKeys = AEKeyFilter { true }

    private val pendingPlans = ArrayList<PendingPlan>()

    @Volatile
    private var registered = false

    /**
     * Installs the plan poller. Called once by the addon bootstrap during server setup; repeat calls
     * are ignored. This deliberately registers an explicit listener object instead of using an
     * annotated event subscriber, so nothing in the addon subscribes to forge events before AE2 and
     * this class are actually available.
     */
    fun register() {
        if (registered) return
        registered = true
        MinecraftForge.EVENT_BUS.addListener(Consumer<TickEvent.ServerTickEvent> { onServerTick(it) })
        // Pending plans outlive a stopped server otherwise: this object, its listener and its list are
        // static, so without this a plan computed before a shutdown could still be submitted once a new
        // integrated server starts in the same JVM.
        MinecraftForge.EVENT_BUS.addListener(Consumer<ServerStoppingEvent> { cancelPendingPlans() })
    }

    /** Cancels and forgets every in-flight plan; called when the server stops. */
    private fun cancelPendingPlans() {
        for (plan in pendingPlans) {
            plan.future.cancel(true)
        }
        pendingPlans.clear()
    }

    /**
     * Runs one operation and replies exactly once, either immediately or later from the server tick
     * poller. Failures are reported as `{ok:false,error:...}` so the model can correct itself.
     *
     * [authorize] re-validates the caller's session lease and mounted cell. Mutating operations call it
     * immediately before submission or pattern installation; read-only operations never touch it.
     */
    fun execute(
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
        operation: String,
        arguments: JsonObject,
        authorize: () -> Unit,
        reply: (JsonObject) -> Unit,
    ) {
        try {
            when (operation) {
                Ae2ToolLimits.OPERATION_READ_INVENTORY -> reply(readInventory(player, terminal, arguments))
                Ae2ToolLimits.OPERATION_ITEM_DETAILS -> reply(itemDetails(player, terminal, arguments))
                Ae2ToolLimits.OPERATION_CRAFTING_PLAN -> startCraftingPlan(player, terminal, arguments, reply)
                Ae2ToolLimits.OPERATION_REQUEST_CRAFTING ->
                    startCraftingRequest(player, terminal, arguments, authorize, reply)
                Ae2ToolLimits.OPERATION_MANAGE_PATTERNS -> reply(
                    Ae2PatternToolExecutor.execute(player, terminal, arguments, ::fingerprintOf, ::resolveKey, authorize),
                )
                else -> reply(failure("unknown_operation", "unsupported ME terminal operation"))
            }
        } catch (denied: TerminalUnavailable) {
            reply(failure("terminal_unavailable", denied.message ?: "the ME terminal is not available"))
        } catch (invalid: IllegalArgumentException) {
            reply(failure("invalid_arguments", invalid.message ?: "invalid arguments"))
        } catch (failure: Exception) {
            LOG.error("ME terminal operation '{}' failed", operation, failure)
            reply(failure("internal_error", "the ME terminal operation failed"))
        }
    }

    private fun readInventory(
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
        arguments: JsonObject,
    ): JsonObject {
        val options = Ae2ToolArguments.readInventory(arguments)
        val grid = resolveGrid(player, terminal)
        val inventory = grid.storageService.cachedInventory

        val rows = ArrayList<Ae2InventoryView.Row<AEKey>>(inventory.size())
        for (entry in inventory) {
            val key = entry.key
            rows += Ae2InventoryView.Row(key.id.toString(), key.displayName.string, entry.longValue, key)
        }

        val crafting = grid.craftingService
        for (key in crafting.getCraftables(allKeys)) {
            if (inventory.get(key) == 0L) {
                rows += Ae2InventoryView.Row(key.id.toString(), key.displayName.string, 0L, key)
            }
        }
        val page = Ae2InventoryView.select(rows, options.filter, options.page, options.pageSize, ::fingerprintOf)
        return Ae2InventoryView.toJson(page, options.filter) { key ->
            itemJson(key, inventory.get(key), crafting.isCraftable(key))
        }
    }

    private fun itemDetails(
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
        arguments: JsonObject,
    ): JsonObject {
        val options = Ae2ToolArguments.itemDetails(arguments)
        val grid = resolveGrid(player, terminal)
        val key = resolveKey(grid, options.key)
            ?: return failure(
                "unknown_key",
                "no key with that fingerprint is in this terminal's network; read the inventory again",
            )

        val steps = Ae2NbtView.parsePath(options.path)
        var node: Ae2Tag = keyRootTag(key)
        var found = true
        var failedAtStep = -1
        when (val navigation = Ae2NbtView.navigate(node, steps)) {
            is Ae2NbtView.Navigation.Found -> node = navigation.node
            is Ae2NbtView.Navigation.Missing -> {
                found = false
                failedAtStep = navigation.failedAtStep
                // Fall back to the deepest ancestor that does exist so the next call can be accurate.
                node = when (val ancestor = Ae2NbtView.navigate(keyRootTag(key), steps.take(failedAtStep))) {
                    is Ae2NbtView.Navigation.Found -> ancestor.node
                    is Ae2NbtView.Navigation.Missing -> keyRootTag(key)
                }
            }
        }

        val rendered = Ae2NbtView.render(node, options.depth, options.page, options.pageSize)
        val serialized = key.toTag()

        return buildJsonObject {
            put("ok", true)
            put("key", options.key)
            put("type", key.type.id.toString())
            put("id", key.id.toString())
            put("name", key.displayName.string)
            put("count", grid.storageService.cachedInventory.get(key))
            put("craftable", grid.craftingService.isCraftable(key))
            if (key is AEItemKey) {
                put("max_stack_size", key.maxStackSize)
                put("damaged", key.isDamaged)
            }
            putJsonObject("nbt") {
                put("present", serialized.contains("tag"))
                put("path", options.path)
                put("found", found)
                put("failed_at_step", failedAtStep)
                put("steps", steps.size)
                put("kind", rendered.kind)
                put("depth", options.depth)
                put("page", options.page)
                put("page_size", options.pageSize)
                put("child_total", rendered.childTotal)
                put("child_shown", rendered.childShown)
                put("truncated", rendered.truncated)
                put("value", rendered.value)
            }
        }
    }

    /**
     * Starts a crafting-plan calculation and answers immediately for anything that is already invalid.
     *
     * `authorize` is null for the read-only plan and non-null for `request_crafting`; when it is set the
     * finished plan is remembered for submission and the request is additionally restricted to item keys
     * the grid can actually craft, so a job that could never run is refused before any calculation.
     */
    private fun beginPlan(
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
        arguments: JsonObject,
        reply: (JsonObject) -> Unit,
        authorize: (() -> Unit)?,
    ) {
        val options = Ae2ToolArguments.craftingPlan(arguments)
        val grid = resolveGrid(player, terminal)
        val key = resolveKey(grid, options.key)
            ?: run {
                reply(
                    failure(
                        "unknown_key",
                        "no key with that fingerprint is in this terminal's network; read the inventory again",
                    ),
                )
                return
            }

        if (authorize != null) {
            if (key !is AEItemKey) {
                reply(
                    failure(
                        "unsupported_key",
                        "only item keys can be requested for crafting; fluids and other channels cannot",
                    ),
                )
                return
            }
            if (!grid.craftingService.isCraftable(key)) {
                reply(
                    failure(
                        "not_craftable",
                        "the network has no pattern that can craft that key, so no job can be submitted",
                    ),
                )
                return
            }
        }

        if (pendingPlans.size >= Ae2ToolLimits.MAX_PENDING_PLANS || pendingPlans.any { it.player === player }) {
            reply(failure("too_many_plans", "another crafting plan is still being computed; retry shortly"))
            return
        }

        // The terminal part *is* the action host, so the job is attributed to the real caller through AE2's
        // own source rather than a stand-in.
        val host: IActionHost = terminal
        if (host.actionableNode == null) {
            reply(failure("terminal_unavailable", "the ME terminal has no active grid node"))
            return
        }
        val level = terminal.level
        if (level !is ServerLevel) {
            reply(failure("terminal_unavailable", "the ME terminal is not in a loaded server level"))
            return
        }

        val source = IActionSource.ofPlayer(player, host)
        val requester = object : ICraftingSimulationRequester {
            override fun getActionSource(): IActionSource = source
        }

        val future = grid.craftingService.beginCraftingCalculation(
            level,
            requester,
            key,
            options.amount,
            CalculationStrategy.REPORT_MISSING_ITEMS,
        )
        pendingPlans += PendingPlan(
            player = player,
            terminal = terminal,
            key = key,
            fingerprint = options.key,
            amount = options.amount,
            requestTick = player.server.tickCount,
            reply = reply,
            future = future,
            submission = authorize?.let { Submission(startGrid = grid, source = source, authorize = it) },
        )
    }

    private fun startCraftingPlan(
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
        arguments: JsonObject,
        reply: (JsonObject) -> Unit,
    ) = beginPlan(player, terminal, arguments, reply, authorize = null)

    private fun startCraftingRequest(
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
        arguments: JsonObject,
        authorize: () -> Unit,
        reply: (JsonObject) -> Unit,
    ) = beginPlan(player, terminal, arguments, reply, authorize = authorize)

    private fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END || pendingPlans.isEmpty()) return
        val tick = event.server.tickCount
        val iterator = pendingPlans.iterator()
        while (iterator.hasNext()) {
            val plan = iterator.next()
            val finished = try {
                handlePlan(plan, tick)
            } catch (failure: Exception) {
                LOG.error("ME crafting plan polling failed", failure)
                true
            }
            if (finished) iterator.remove()
        }
    }

    /** Returns true once the plan has been answered or abandoned, so the caller can drop it. */
    private fun handlePlan(plan: PendingPlan, tick: Int): Boolean = when {
        // Checked before the future so a player who left can never cause a submission, even if the plan
        // finished in the same tick.
        plan.player.hasDisconnected() -> {
            plan.future.cancel(true)
            answer(plan, failure("terminal_unavailable", "the requesting player is no longer connected"))
            true
        }

        plan.future.isDone -> {
            if (plan.submission == null) answerPlan(plan) else submitPlan(plan)
            true
        }

        tick - plan.requestTick >= Ae2ToolLimits.PLAN_TIMEOUT_TICKS -> {
            plan.future.cancel(true)
            answer(
                plan,
                failure(
                    "plan_timeout",
                    "the crafting plan took too long; ask again, or ask for a smaller amount",
                ),
            )
            true
        }

        else -> false
    }

    /**
     * Reads a finished plan and re-validates the terminal on the server thread before answering, so a
     * plan computed for a terminal that has since been unloaded, deactivated or made unreadable is
     * never reported as usable context.
     */
    private fun answerPlan(plan: PendingPlan) {
        val result = finishedPlan(plan) ?: return
        val grid = currentGrid(plan) ?: return
        if (plan.player.hasDisconnected()) {
            answer(plan, failure("terminal_unavailable", "the requesting player is no longer connected"))
            return
        }

        val json = try {
            planJson(plan, result, grid)
        } catch (failure: Exception) {
            LOG.error("ME crafting plan response could not be built", failure)
            answer(plan, failure("internal_error", "the crafting plan result could not be reported"))
            return
        }
        answer(plan, json)
    }

    /**
     * Submits a finished plan for a `request_crafting` call. Every check that matters for an irreversible
     * side effect is repeated here, on the server thread and against live state: the terminal still
     * resolves, it is still the same grid the plan was computed for, the key is still craftable, the plan
     * is not a simulation and the caller's session lease is still valid. Only then does AE2 extract the
     * items and start the job. Any shortfall is answered as `ok:false`, never as a submitted job.
     */
    private fun submitPlan(plan: PendingPlan) {
        val submission = plan.submission
        if (submission == null) {
            LOG.error("ME crafting request has no submission checks")
            answer(plan, failure("internal_error", "the crafting request has no submission checks"))
            return
        }

        val result = finishedPlan(plan) ?: return
        val grid = currentGrid(plan) ?: return

        if (grid !== submission.startGrid) {
            answer(
                plan,
                failure(
                    "terminal_changed",
                    "the ME terminal is no longer on the network the plan was computed for",
                ),
            )
            return
        }
        if (!grid.craftingService.isCraftable(plan.key)) {
            answer(plan, failure("not_craftable", "the network no longer has a pattern that can craft that key"))
            return
        }
        if (result.simulation()) {
            answer(plan, missingItemsFailure(plan, result, grid))
            return
        }

        // The last check before the irreversible step: the shared session lease and the mounted cell.
        // AE2 is about to extract items, so a session that is gone must abort instead of submitting.
        try {
            submission.authorize()
        } catch (denied: IllegalArgumentException) {
            answer(plan, failure("session_lost", denied.message ?: "the shared session is no longer available"))
            return
        }

        val submitted = try {
            grid.craftingService.submitJob(result, null, null, PRIORITIZE_POWER, submission.source)
        } catch (failure: Exception) {
            LOG.error("ME crafting request submission failed", failure)
            answer(plan, failure("submit_failed", "the crafting job could not be submitted"))
            return
        }
        answer(plan, if (submitted.successful()) submittedJson(plan, result, grid) else submitFailure(plan, submitted))
    }

    /**
     * Reads the finished plan without waiting: the caller only reaches this once `Future.isDone` is true,
     * so `get` returns immediately and the server thread is never stalled. On failure it answers and
     * returns null.
     */
    private fun finishedPlan(plan: PendingPlan): ICraftingPlan? = try {
        plan.future.get()
    } catch (failure: ExecutionException) {
        LOG.error("ME crafting plan failed", failure.cause)
        answer(plan, failure("plan_failed", "the crafting plan could not be computed"))
        null
    } catch (cancelled: CancellationException) {
        answer(plan, failure("plan_failed", "the crafting plan was cancelled"))
        null
    } catch (interrupted: InterruptedException) {
        Thread.currentThread().interrupt()
        answer(plan, failure("plan_failed", "the crafting plan was interrupted"))
        null
    }

    /** Re-resolves the terminal on the server thread; a denied terminal is answered and reported as null. */
    private fun currentGrid(plan: PendingPlan): IGrid? = try {
        plan.terminal.resolve(plan.player, ALLOW_REMOTE_AFTER_LOCK)
    } catch (denied: IllegalArgumentException) {
        answer(plan, failure("terminal_unavailable", denied.message ?: "the ME terminal is not available"))
        null
    }

    /** Delivers one reply; a failing reply path must never break the server tick loop. */
    private fun answer(plan: PendingPlan, reply: JsonObject) {
        try {
            plan.reply(reply)
        } catch (failure: Exception) {
            LOG.error("ME crafting plan reply failed", failure)
        }
    }

    private fun planJson(plan: PendingPlan, result: ICraftingPlan, grid: IGrid): JsonObject {
        val missing = result.missingItems()
        val ordered = orderedMissing(missing)

        val finalOutput = result.finalOutput()

        return buildJsonObject {
            put("ok", true)
            put("submitted", false)
            put(
                "note",
                "This is a crafting plan only. No crafting job was submitted and nothing was reserved; " +
                    "the numbers describe the network when the plan was computed.",
            )
            put("key", plan.fingerprint)
            put("type", plan.key.type.id.toString())
            put("id", plan.key.id.toString())
            put("name", plan.key.displayName.string)
            put("amount", plan.amount)
            put("simulation", result.simulation())
            put("craftable", grid.craftingService.isCraftable(plan.key))
            put("multiple_paths", result.multiplePaths())
            put("bytes", result.bytes())
            put("pattern_times", result.patternTimes().size)
            put("used_item_kinds", result.usedItems().size())
            put("emitted_item_kinds", result.emittedItems().size())
            put("stored_amount", grid.storageService.cachedInventory.get(plan.key))
            if (finalOutput != null) {
                putJsonObject("final_output") {
                    put("key", fingerprintOf(finalOutput.what()))
                    put("id", finalOutput.what().id.toString())
                    put("name", finalOutput.what().displayName.string)
                    put("count", finalOutput.amount())
                }
            }
            put("missing_item_kinds", missing.size())
            put("missing_items_truncated", ordered.size > Ae2ToolLimits.MAX_MISSING_ITEMS)
            putJsonArray("missing_items") {
                for (row in ordered.take(Ae2ToolLimits.MAX_MISSING_ITEMS)) {
                    val key = row.handle
                    add(itemJson(key, row.count, grid.craftingService.isCraftable(key)))
                }
            }
        }
    }

    private fun itemJson(key: AEKey, count: Long, craftable: Boolean): JsonObject = buildJsonObject {
        put("key", fingerprintOf(key))
        put("type", key.type.id.toString())
        put("id", key.id.toString())
        put("name", key.displayName.string)
        put("count", count)
        put("craftable", craftable)
    }

    /** Missing items ordered the way both the plan and a rejected request report them. */
    private fun orderedMissing(missing: KeyCounter): List<Ae2InventoryView.Row<AEKey>> {
        val ordered = ArrayList<Ae2InventoryView.Row<AEKey>>(missing.size())
        for (entry in missing) {
            val key = entry.key
            ordered += Ae2InventoryView.Row(key.id.toString(), key.displayName.string, entry.longValue, key)
        }
        ordered.sortWith(
            compareByDescending<Ae2InventoryView.Row<AEKey>> { it.count }
                .thenBy { it.name.lowercase(Locale.ROOT) }
                .thenBy { it.id },
        )
        return ordered
    }

    /**
     * Reports an incomplete plan instead of submitting it: AE2 itself refuses a simulation, so this never
     * reaches `submitJob`, and the model gets the real items the network is short of.
     */
    private fun missingItemsFailure(plan: PendingPlan, result: ICraftingPlan, grid: IGrid): JsonObject {
        val ordered = orderedMissing(result.missingItems())
        return buildJsonObject {
            put("ok", false)
            put("submitted", false)
            put("error", "missing_items")
            put(
                "message",
                "the network is missing items for that job, so nothing was submitted; stock or craft the " +
                    "missing items and ask again",
            )
            put("key", plan.fingerprint)
            put("amount", plan.amount)
            put("simulation", true)
            put("bytes", result.bytes())
            put("missing_item_kinds", result.missingItems().size())
            put("missing_items_truncated", ordered.size > Ae2ToolLimits.MAX_MISSING_ITEMS)
            putJsonArray("missing_items") {
                for (row in ordered.take(Ae2ToolLimits.MAX_MISSING_ITEMS)) {
                    val key = row.handle
                    add(itemJson(key, row.count, grid.craftingService.isCraftable(key)))
                }
            }
        }
    }

    /**
     * The successful answer of a `request_crafting` call: AE2 accepted the plan and the job is running. The
     * numbers come from the same real plan that was submitted, so the output amount is what the network has
     * started crafting.
     */
    private fun submittedJson(plan: PendingPlan, result: ICraftingPlan, grid: IGrid): JsonObject {
        val finalOutput = result.finalOutput()
        return buildJsonObject {
            put("ok", true)
            put("submitted", true)
            put(
                "note",
                "A real crafting job is now running on the network. AE2 extracted the used items from the " +
                    "network and the output goes back into the network, not into the player's inventory.",
            )
            put("key", plan.fingerprint)
            put("type", plan.key.type.id.toString())
            put("id", plan.key.id.toString())
            put("name", plan.key.displayName.string)
            put("amount", plan.amount)
            put("simulation", false)
            put("multiple_paths", result.multiplePaths())
            put("bytes", result.bytes())
            put("pattern_times", result.patternTimes().size)
            put("used_item_kinds", result.usedItems().size())
            put("emitted_item_kinds", result.emittedItems().size())
            put("stored_amount", grid.storageService.cachedInventory.get(plan.key))
            if (finalOutput != null) {
                putJsonObject("final_output") {
                    put("key", fingerprintOf(finalOutput.what()))
                    put("id", finalOutput.what().id.toString())
                    put("name", finalOutput.what().displayName.string)
                    put("count", finalOutput.amount())
                }
            }
        }
    }

    /** AE2's own rejection of a submission, including the error code and any structured detail it gave. */
    private fun submitFailure(plan: PendingPlan, submit: ICraftingSubmitResult): JsonObject {
        val code = submit.errorCode()
        return buildJsonObject {
            put("ok", false)
            put("submitted", false)
            put("error", "crafting_rejected")
            put("reason", code?.name ?: "UNKNOWN")
            put("message", submitMessage(code))
            put("key", plan.fingerprint)
            put("amount", plan.amount)
            when (code) {
                CraftingSubmitErrorCode.NO_SUITABLE_CPU_FOUND -> {
                    val detail = submit.errorDetail() as? UnsuitableCpus
                    if (detail != null) {
                        putJsonObject("unsuitable_cpus") {
                            put("offline", detail.offline())
                            put("busy", detail.busy())
                            put("too_small", detail.tooSmall())
                            put("excluded", detail.excluded())
                        }
                    }
                }

                CraftingSubmitErrorCode.MISSING_INGREDIENT -> {
                    val detail = submit.errorDetail() as? GenericStack
                    if (detail != null) {
                        putJsonObject("missing_ingredient") {
                            put("key", fingerprintOf(detail.what()))
                            put("id", detail.what().id.toString())
                            put("name", detail.what().displayName.string)
                            put("count", detail.amount())
                        }
                    }
                }

                else -> {}
            }
        }
    }

    private fun submitMessage(code: CraftingSubmitErrorCode?): String = when (code) {
        CraftingSubmitErrorCode.INCOMPLETE_PLAN ->
            "the plan is incomplete and was not submitted"
        CraftingSubmitErrorCode.NO_CPU_FOUND ->
            "the network has no crafting CPU that can run that job"
        CraftingSubmitErrorCode.NO_SUITABLE_CPU_FOUND ->
            "no available crafting CPU is suitable for that job"
        CraftingSubmitErrorCode.CPU_BUSY ->
            "the crafting CPU is already busy with another job"
        CraftingSubmitErrorCode.CPU_OFFLINE ->
            "the crafting CPU is offline"
        CraftingSubmitErrorCode.CPU_TOO_SMALL ->
            "the crafting CPU is too small for that job"
        CraftingSubmitErrorCode.MISSING_INGREDIENT ->
            "the network could not provide an ingredient for that job"
        null -> "the crafting job was rejected"
    }

    private fun resolveGrid(player: ServerPlayer, terminal: MEIntelligentTerminalPart): IGrid = try {
        terminal.resolve(player, ALLOW_REMOTE_AFTER_LOCK)
    } catch (denied: IllegalArgumentException) {
        throw TerminalUnavailable(denied.message)
    }

    /**
     * Finds the live key for a fingerprint the model passed back: the fingerprint cache first, then the
     * network's cached inventory, and finally the grid's craftable keys so a plan can be requested for
     * something that is craftable but not currently stored.
     */
    private fun resolveKey(grid: IGrid, fingerprint: String): AEKey? {
        keyByFingerprint[fingerprint]?.let { key ->
            if (grid.storageService.cachedInventory.get(key) > 0L || grid.craftingService.isCraftable(key)) {
                return key
            }
            keyByFingerprint.remove(fingerprint)
        }

        for (entry in grid.storageService.cachedInventory) {
            val key = entry.key
            if (fingerprintOf(key) == fingerprint) {
                remember(key, fingerprint)
                return key
            }
        }

        var scanned = 0
        for (key in grid.craftingService.getCraftables(allKeys)) {
            if (scanned++ >= MAX_CRAFTABLE_SCAN) break
            if (fingerprintOf(key) == fingerprint) {
                remember(key, fingerprint)
                return key
            }
        }
        return null
    }

    private fun remember(key: AEKey, fingerprint: String) {
        fingerprintByKey[key] = fingerprint
        keyByFingerprint[fingerprint] = key
    }

    /** Fingerprint of the full key: channel, resource id and every byte of its NBT. */
    private fun fingerprintOf(key: AEKey): String {
        val fingerprint = fingerprintByKey[key]
            ?: Ae2NbtView.fingerprint(key.type.id.toString(), adaptTag(key.toTag()))
        remember(key, fingerprint)
        return fingerprint
    }

    /** The key's own serialized form plus its channel, used as the root of the detail view. */
    private fun keyRootTag(key: AEKey): Ae2Tag {
        val serialized = adaptTag(key.toTag())
        val entries = LinkedHashMap<String, Ae2Tag>()
        if (serialized is Ae2Tag.Compound) entries.putAll(serialized.entries)
        entries["type"] = Ae2Tag.Text(key.type.id.toString())
        return Ae2Tag.Compound(entries)
    }

    private fun adaptTag(tag: Tag): Ae2Tag = when (tag) {
        is CompoundTag -> {
            val entries = sortedMapOf<String, Ae2Tag>()
            for (name in tag.allKeys) {
                val value = tag.get(name) ?: continue
                entries[name] = adaptTag(value)
            }
            Ae2Tag.Compound(entries)
        }

        is ListTag -> Ae2Tag.Sequence(tag.map { adaptTag(it) }, tag.elementType.toInt())

        is ByteTag -> Ae2Tag.Signed(Ae2TagType.BYTE, tag.asByte.toLong())
        is ShortTag -> Ae2Tag.Signed(Ae2TagType.SHORT, tag.asShort.toLong())
        is IntTag -> Ae2Tag.Signed(Ae2TagType.INT, tag.asInt.toLong())
        is LongTag -> Ae2Tag.Signed(Ae2TagType.LONG, tag.asLong)
        is FloatTag -> Ae2Tag.Decimal(Ae2TagType.FLOAT, java.lang.Float.floatToRawIntBits(tag.asFloat).toLong())
        is DoubleTag -> Ae2Tag.Decimal(Ae2TagType.DOUBLE, java.lang.Double.doubleToRawLongBits(tag.asDouble))
        is StringTag -> Ae2Tag.Text(tag.asString)
        is ByteArrayTag -> Ae2Tag.Bytes(tag.asByteArray)
        is IntArrayTag -> Ae2Tag.Ints(tag.asIntArray)
        is LongArrayTag -> Ae2Tag.Longs(tag.asLongArray)
        else -> Ae2Tag.Text(tag.asString)
    }

    private fun failure(error: String, message: String): JsonObject = buildJsonObject {
        put("ok", false)
        put("error", error)
        put("message", message)
    }

    private fun <K, V> lruCache(maximumSize: Int): LinkedHashMap<K, V> =
        object : LinkedHashMap<K, V>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean = size > maximumSize
        }

    /** Signals that the terminal itself refused the request, as opposed to malformed arguments. */
    private class TerminalUnavailable(message: String?) : RuntimeException(message)

    private class PendingPlan(
        val player: ServerPlayer,
        val terminal: MEIntelligentTerminalPart,
        val key: AEKey,
        val fingerprint: String,
        val amount: Long,
        val requestTick: Int,
        val reply: (JsonObject) -> Unit,
        val future: Future<ICraftingPlan>,
        /** Set only for `request_crafting`: what to re-check and then submit once the plan is ready. */
        val submission: Submission?,
    )

    /**
     * The extra state a `request_crafting` plan needs at submission time: the grid the plan belongs to, the
     * action source the extraction is attributed to, and the caller's lease/mount check.
     */
    private class Submission(
        /** The grid the plan was computed for; a different grid means the terminal changed networks. */
        val startGrid: IGrid,
        /** Reused from the calculation so the job extracts under the same player attribution. */
        val source: IActionSource,
        /** Re-validates the shared session lease and the mounted cell, immediately before submitting. */
        val authorize: () -> Unit,
    )
}
