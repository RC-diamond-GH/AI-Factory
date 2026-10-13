package com.rcdiamond.aifactory.addon.ae2.server

import appeng.api.config.Actionable
import appeng.api.crafting.IPatternDetails
import appeng.api.crafting.PatternDetailsHelper
import appeng.api.inventories.InternalInventory
import appeng.api.networking.IGrid
import appeng.api.networking.security.IActionSource
import appeng.api.parts.IPartHost
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import appeng.api.storage.MEStorage
import appeng.core.definitions.AEItems
import appeng.crafting.pattern.AECraftingPattern
import appeng.crafting.pattern.AEProcessingPattern
import appeng.crafting.pattern.AESmithingTablePattern
import appeng.crafting.pattern.AEStonecuttingPattern
import appeng.helpers.patternprovider.PatternContainer
import appeng.helpers.patternprovider.PatternProviderLogicHost
import appeng.menu.AutoCraftingMenu
import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.addon.ae2.part.MEIntelligentTerminalPart
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2InventoryView
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2PatternToolArguments
import com.rcdiamond.aifactory.addon.ae2.tool.Ae2ToolLimits
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.TransientCraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SmithingRecipe
import net.minecraft.world.item.crafting.SmithingTransformRecipe
import net.minecraft.world.item.crafting.SmithingTrimRecipe
import net.minecraft.world.item.crafting.StonecutterRecipe
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.registries.ForgeRegistries
import java.util.Collections
import java.util.IdentityHashMap
import java.util.Locale

/**
 * Server-side implementation of [Ae2ToolLimits.OPERATION_MANAGE_PATTERNS].
 *
 * ## What it is
 * The tool-shaped equivalent of AE2's ME Pattern Access Terminal, plus the one thing that terminal's GUI does
 * not offer: encoding a pattern and installing it into an arbitrary container of the *current* network. It
 * reuses AE2's own API instead of re-implementing it:
 * - container discovery walks `IGrid.getMachineClasses()` / `IGrid.getActiveMachines(Class)` exactly like
 *   [appeng.menu.implementations.PatternAccessTermMenu] does and filters with the same
 *   `PatternContainer.isVisibleInTerminal()` / "is full" rules behind the `show` parameter;
 * - patterns are read through `PatternDetailsHelper.decodePattern`, so the reported mode, inputs and outputs
 *   are AE2's own decoding of the item, never a guess;
 * - edits are encoded by `PatternDetailsHelper.encodeCraftingPattern` / `encodeProcessingPattern` /
 *   `encodeStonecuttingPattern` / `encodeSmithingTablePattern`, and the result is decoded again *before* it is
 *   stored, so an item AE2 cannot decode is never installed;
 * - the write goes through the container's own `InternalInventory.setItemDirect`, the same call AE2's pattern
 *   access terminal uses, which makes the provider save itself and refresh its crafting patterns.
 *
 * ## No invented recipes, no invented items
 * A crafting, stonecutting or smithing pattern is only ever built from a recipe the live `RecipeManager`
 * returns for the given inputs, and the encoded pattern is verified against what that recipe really produces.
 * A blank pattern for an empty slot is taken from the terminal's ME storage (simulated first, then really
 * extracted); it is never created, duplicated or taken from the player. Keys are named by fingerprint or by a
 * plain registry id, and a fingerprint is re-resolved against this network *and* against the live contents of
 * this grid's patterns, so no value is trusted across calls.
 *
 * ## Nothing is overwritten by accident
 * An install must name the container, the slot and the *expected* current content of that slot (`"empty"` or
 * the pattern key the view reported). That expectation is checked when the request is read and again
 * immediately before the write. The container is re-checked for permission, load state, the same grid and
 * visibility right before the write, so a container that was unloaded, wrenched off the network or taken over
 * by another session is never modified. If anything fails after items were taken, the slot and the network are
 * put back exactly as they were.
 */
object Ae2PatternToolExecutor {
    private val LOG = LogUtils.getLogger()

    /**
     * The terminal is re-resolved with the same relaxation the parent executor uses: the session lease has
     * already been verified, so distance is not re-enforced, but load, dimension, interaction permission and an
     * active grid always are.
     */
    private const val ALLOW_REMOTE_AFTER_LOCK = true

    /** Containers one response may look at. */
    private const val MAX_CONTAINERS = 256

    /** Slots one request may decode, bounding the cost of a scan on a very large network. */
    private const val MAX_SCANNED_SLOTS = 1024

    /** Empty slot indices reported per container. */
    private const val MAX_EMPTY_SLOT_LIST = 64

    /** Input/output rows reported per pattern. */
    private const val MAX_IO_ROWS = AEProcessingPattern.MAX_INPUT_SLOTS

    /** Tooltip lines reported per container. */
    private const val MAX_TOOLTIP_LINES = 8

    /** Length of the per-pattern search text. */
    private const val MAX_SEARCH_TEXT = 320

    /** Keys contributed to the search text of one pattern. */
    private const val MAX_SEARCH_KEYS = 8

    private const val SIDE_BLOCK = "block"

    private const val MODE_BLANK = "blank"
    private const val MODE_INVALID = "invalid"
    private const val MODE_UNSUPPORTED = "unsupported"

    /** AE2's own NBT key for the recipe id of an encoded crafting pattern (`CraftingPatternEncoding`). */
    private const val NBT_RECIPE = "recipe"

    /**
     * Runs one request and always answers with a JSON object; nothing throws out of here except programming
     * errors, which are logged and reported as `internal_error`.
     *
     * [authorize] re-validates the caller's session lease and mounted intelligent cell. It is called exactly
     * once, and only by an install, immediately before the first persistent change (taking the blank pattern
     * and writing the container slot); the `view` action never touches it.
     */
    fun execute(
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
        arguments: JsonObject,
        fingerprintOf: (AEKey) -> String,
        resolveKey: (IGrid, String) -> AEKey?,
        authorize: () -> Unit,
    ): JsonObject = try {
        when (val request = Ae2PatternToolArguments.parse(arguments)) {
            is Ae2PatternToolArguments.View -> view(player, terminal, request, fingerprintOf)
            is Ae2PatternToolArguments.EncodeInstall ->
                install(player, terminal, request.install, fingerprintOf, resolveKey, authorize)
        }
    } catch (denied: TerminalUnavailable) {
        failure("terminal_unavailable", denied.message ?: "the ME terminal is not available")
    } catch (rejected: Rejected) {
        failure(rejected.error, rejected.message ?: "the request was rejected")
    } catch (invalid: IllegalArgumentException) {
        failure("invalid_arguments", invalid.message ?: "invalid arguments")
    } catch (failure: Exception) {
        LOG.error("ME pattern operation failed", failure)
        failure("internal_error", "the ME pattern operation failed")
    }

    // ------------------------------------------------------------------ view

    private fun view(
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
        options: Ae2PatternToolArguments.View,
        fingerprintOf: (AEKey) -> String,
    ): JsonObject {
        val grid = resolveGrid(player, terminal)
        val level = requireServerLevel(terminal)
        val scan = Scan(level)
        val containers = scan.discover(grid, options.show, options.container)

        val entries = ArrayList<PatternEntry>()
        var truncated = false
        scan@ for (container in containers) {
            val inventory = container.inventory
            for (slot in 0 until inventory.size()) {
                if (!scan.takeSlot()) {
                    truncated = true
                    break@scan
                }
                val stack = inventory.getStackInSlot(slot)
                if (stack.isEmpty) continue
                entries += scan.describeEntry(container, slot, stack)
            }
        }

        val rows = entries.map {
            Ae2InventoryView.Row(id = it.searchable, name = it.sortKey, count = 1L, handle = it)
        }
        val page = Ae2InventoryView.select(rows, options.filter, options.page, options.pageSize)
        val blankPatterns = blankPatternsAvailable(grid, player, terminal)

        return buildJsonObject {
            put("ok", true)
            put("action", Ae2PatternToolArguments.ACTION_VIEW)
            putJsonObject("grid") {
                put("active", true)
                put("dimension", level.dimension().location().toString())
                put("containers_found", scan.containersFound)
                put("containers_shown", containers.size)
                put("containers_skipped", scan.containersSkipped)
                put("patterns_scanned", entries.size)
                put("scan_truncated", truncated || scan.containersSkipped > 0)
            }
            put("show", options.show)
            put("filter", options.filter)
            put("page", page.page)
            put("page_size", page.pageSize)
            put("page_count", page.pageCount)
            put("total_patterns", page.totalItems)
            put("has_more", page.page + 1 < page.pageCount)
            put("blank_patterns", blankPatterns)
            putJsonObject("limits") {
                put("crafting_grid_slots", AECraftingPattern.CRAFTING_GRID_SLOTS)
                put("processing_input_slots", AEProcessingPattern.MAX_INPUT_SLOTS)
                put("processing_output_slots", AEProcessingPattern.MAX_OUTPUT_SLOTS)
                put("max_amount", Ae2ToolLimits.MAX_AMOUNT)
                put("max_target_slot", Ae2PatternToolArguments.MAX_TARGET_SLOT)
                putJsonArray("modes") {
                    add(JsonPrimitive(Ae2PatternToolArguments.MODE_CRAFTING))
                    add(JsonPrimitive(Ae2PatternToolArguments.MODE_PROCESSING))
                    add(JsonPrimitive(Ae2PatternToolArguments.MODE_STONECUTTING))
                    add(JsonPrimitive(Ae2PatternToolArguments.MODE_SMITHING))
                }
            }
            put(
                "note",
                "A container is only part of this list when it is an active machine of this network and its " +
                    "own terminal visibility allows it. To encode or replace a pattern, call this tool again " +
                    "with action '${Ae2PatternToolArguments.ACTION_ENCODE_INSTALL}', the 'container' id of a " +
                    "container marked addressable, its 'slot', and 'expected' set to that slot's pattern_key " +
                    "(or '${Ae2PatternToolArguments.EXPECTED_EMPTY}' for one of its empty_slots).",
            )
            putJsonArray("containers") {
                for (container in containers) add(containerJson(container))
            }
            putJsonArray("patterns") {
                for (row in page.rows) add(patternJson(row.handle, fingerprintOf))
            }
        }
    }

    // --------------------------------------------------------------- install

    private fun install(
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
        request: Ae2PatternToolArguments.Install,
        fingerprintOf: (AEKey) -> String,
        resolveKey: (IGrid, String) -> AEKey?,
        authorize: () -> Unit,
    ): JsonObject {
        val grid = resolveGrid(player, terminal)
        val level = requireServerLevel(terminal)
        val scan = Scan(level)

        // 1. The container must still be a live, visible machine of *this* grid.
        val containers = scan.discover(grid, Ae2PatternToolArguments.SHOW_ALL, request.container)
        val target = containers.firstOrNull {
            it.address == request.container && it.level.dimension().location().toString() == request.dimension
        }
            ?: throw Rejected(
                "unknown_container",
                "no pattern container of this network is at that address; run action " +
                    "'${Ae2PatternToolArguments.ACTION_VIEW}' and use the container id it reports",
            )
        if (target.container.grid !== grid) {
            throw Rejected("container_off_grid", "that pattern container is not connected to this network any more")
        }
        if (!target.visible) {
            throw Rejected(
                "container_hidden",
                "that pattern container is hidden from the pattern access terminal, so this tool leaves it " +
                    "alone; enable the container's pattern access terminal setting first",
            )
        }
        val address = target.address
            ?: throw Rejected("container_not_addressable", "that pattern container has no stable world position")
        val containerLevel = target.level as? ServerLevel
            ?: throw Rejected("container_unloaded", "that pattern container is not in a server level")

        // 2. Permission and load state of the container's own position, not only of the terminal.
        val containerPos = BlockPos(address.x, address.y, address.z)
        if (!containerLevel.isLoaded(containerPos)) {
            throw Rejected("container_unloaded", "that pattern container is not loaded")
        }
        if (!containerLevel.mayInteract(player, containerPos)) {
            throw Rejected("no_permission", "you are not allowed to modify that pattern container")
        }

        // 3. The target slot and the expected current content.
        val inventory = target.inventory
        val slot = request.slot
        if (slot < 0 || slot >= inventory.size()) {
            throw Rejected(
                "invalid_slot",
                "that pattern container has ${inventory.size()} pattern slots, so '$slot' does not exist",
            )
        }
        val current = inventory.getStackInSlot(slot).copy()
        val previous = if (current.isEmpty) null else scan.describeEntry(target, slot, current)
        val blankSource = expectation(request.expected, current, fingerprintOf)

        // 4. A blank pattern is required - and must really exist - before anything is encoded.
        val source = IActionSource.ofPlayer(player, terminal)
        val network = grid.storageService.inventory
        val blank: AEItemKey? = if (blankSource == BlankSource.EXTRACT) {
            val key = blankPatternKey()
                ?: throw Rejected("no_blank_pattern", "this server has no blank pattern item available")
            if (network.extract(key, 1, Actionable.SIMULATE, source) < 1) {
                throw Rejected(
                    "no_blank_pattern",
                    "this network has no blank pattern to install, and a pattern is never created out of " +
                        "nothing; store or craft a blank pattern first, or target a slot that already holds a " +
                        "pattern (reported as editable in the view)",
                )
            }
            key
        } else {
            null
        }

        // 5. Encode (pure: nothing in the world has changed yet) and let AE2 decode the result again.
        val keys = Keys(scan, grid, containers, resolveKey, fingerprintOf)
        val encoded = when (val payload = request.payload) {
            is Ae2PatternToolArguments.Crafting -> encodeCrafting(level, payload, keys)
            is Ae2PatternToolArguments.Processing -> encodeProcessing(level, payload, keys)
            is Ae2PatternToolArguments.Stonecutting -> encodeStonecutting(level, payload, keys)
            is Ae2PatternToolArguments.Smithing -> encodeSmithing(level, payload, keys)
        }

        // 6. Everything that can still change is checked once more, right before the first side effect.
        if (resolveGrid(player, terminal) !== grid) {
            throw Rejected("terminal_unavailable", "the terminal is not on the same network any more")
        }
        if (target.container.grid !== grid) {
            throw Rejected("container_off_grid", "that pattern container left this network while encoding")
        }
        if (!containerLevel.isLoaded(containerPos) || !target.container.isVisibleInTerminal ||
            !containerLevel.mayInteract(player, containerPos)
        ) {
            throw Rejected("no_permission", "you are no longer allowed to modify that pattern container")
        }
        if (!ItemStack.matches(inventory.getStackInSlot(slot), current)) {
            throw Rejected(
                "stale_slot",
                "that slot changed while the pattern was being encoded, so nothing was written; run action " +
                    "'${Ae2PatternToolArguments.ACTION_VIEW}' again and repeat the install",
            )
        }
        try {
            authorize()
        } catch (denied: IllegalArgumentException) {
            throw Rejected("not_authorized", denied.message ?: "the terminal session is no longer valid")
        }

        // 7. Take the blank pattern (if needed), then write. Anything that fails puts both back.
        var taken: AEItemKey? = null
        if (blank != null) {
            if (network.extract(blank, 1, Actionable.MODULATE, source) < 1) {
                throw Rejected(
                    "no_blank_pattern",
                    "the blank pattern was taken by something else in the same tick; nothing was installed",
                )
            }
            taken = blank
        }
        val takenBlank = taken != null
        try {
            inventory.setItemDirect(slot, encoded)
            if (!ItemStack.matches(inventory.getStackInSlot(slot), encoded)) {
                throw Rejected("install_failed", "that container did not accept the encoded pattern")
            }
        } catch (failure: Exception) {
            // Restore the slot before refunding: a container callback can throw after applying the write.
            try {
                inventory.setItemDirect(slot, current)
            } catch (restoreFailure: Exception) {
                failure.addSuppressed(restoreFailure)
            }
            if (ItemStack.matches(inventory.getStackInSlot(slot), current)) {
                taken?.let { returnBlank(network, it, source, player) }
            } else {
                // Refunding while the encoded item remains installed would duplicate the physical pattern.
                LOG.error("Pattern container rollback failed; no blank pattern was refunded", failure)
            }
            throw failure
        }

        val installed = scan.describeEntry(target, slot, inventory.getStackInSlot(slot))

        return buildJsonObject {
            put("ok", true)
            put("action", Ae2PatternToolArguments.ACTION_ENCODE_INSTALL)
            put("installed", true)
            put("mode", request.payload.mode)
            put("consumed_blank_pattern", takenBlank)
            put("reused_existing_pattern_item", !takenBlank)
            put("blank_patterns_remaining", blankPatternsAvailable(grid, player, terminal))
            put("slot", slot)
            put("expected", request.expected)
            putJsonObject("container") {
                put("id", target.id.orEmpty())
                put("name", target.name)
                put("slots", inventory.size())
                put("dimension", request.dimension)
                putJsonObject("position") {
                    put("x", address.x)
                    put("y", address.y)
                    put("z", address.z)
                    put("side", address.side)
                }
            }
            previous?.let { put("previous", patternJson(it, fingerprintOf)) }
            put("pattern", patternJson(installed, fingerprintOf))
            put(
                "note",
                if (takenBlank) {
                    "The pattern was encoded onto one blank pattern taken from this ME network and written " +
                        "into the container's own pattern inventory, which is what makes AE2 save the " +
                        "container and re-read its patterns; the network's crafting service picks the new " +
                        "pattern up on its own next update. Nothing else was changed."
                } else {
                    "The container's existing pattern item was re-encoded in place, so nothing was taken from " +
                        "the network. AE2 saves the container and re-reads its patterns; the network's crafting " +
                        "service picks the new pattern up on its own next update. Nothing else was changed."
                },
            )
        }
    }

    // ------------------------------------------------------------- encoding

    private fun encodeCrafting(
        level: ServerLevel,
        payload: Ae2PatternToolArguments.Crafting,
        keys: Keys,
    ): ItemStack {
        val dimension = AECraftingPattern.CRAFTING_GRID_DIMENSION
        val container: CraftingContainer = TransientCraftingContainer(AutoCraftingMenu(), dimension, dimension)
        val grid = Array(AECraftingPattern.CRAFTING_GRID_SLOTS) { ItemStack.EMPTY }
        for ((index, reference) in payload.grid.withIndex()) {
            if (reference == null) continue
            val key = keys.item(reference, "${Ae2PatternToolArguments.GRID}[$index]")
            grid[index] = key.toStack(1)
            container.setItem(index, grid[index])
        }

        val manager = level.recipeManager
        val recipe: CraftingRecipe = if (payload.recipe != null) {
            val id = resourceId(payload.recipe, Ae2PatternToolArguments.RECIPE)
            val byId = manager.getAllRecipesFor(RecipeType.CRAFTING).firstOrNull { it.id == id }
                ?: throw Rejected(
                    "unknown_recipe",
                    "parameter '${Ae2PatternToolArguments.RECIPE}' names a crafting recipe this world does not have",
                )
            if (!byId.matches(container, level)) {
                throw Rejected(
                    "recipe_mismatch",
                    "that crafting recipe does not match the given grid, so it cannot be encoded",
                )
            }
            byId
        } else {
            manager.getRecipeFor(RecipeType.CRAFTING, container, level).orElse(null)
                ?: throw Rejected(
                    "no_recipe",
                    "no crafting recipe of this world matches that ${dimension}x$dimension grid; this tool " +
                        "only encodes real recipes, use mode '${Ae2PatternToolArguments.MODE_PROCESSING}' for a " +
                        "custom process",
                )
        }

        val output = recipe.assemble(container, level.registryAccess())
        if (output.isEmpty) {
            throw Rejected("recipe_no_output", "that crafting recipe produced no item, so nothing was encoded")
        }
        payload.output?.let { claimed ->
            val claimedKey = keys.item(claimed, Ae2PatternToolArguments.OUTPUT)
            if (!AEItemKey.matches(claimedKey, output)) {
                throw Rejected(
                    "output_mismatch",
                    "parameter '${Ae2PatternToolArguments.OUTPUT}' is not what that crafting recipe really " +
                        "produces; only the recipe's own output is ever encoded",
                )
            }
        }

        val encoded = try {
            PatternDetailsHelper.encodeCraftingPattern(
                recipe, grid, output, payload.substitute, payload.fluidSubstitute,
            )
        } catch (invalid: IllegalArgumentException) {
            throw Rejected("encode_failed", invalid.message ?: "AE2 rejected that crafting pattern")
        }
        verifyEncoded(encoded, level, Ae2PatternToolArguments.MODE_CRAFTING, output)
        return encoded
    }

    private fun encodeProcessing(
        level: ServerLevel,
        payload: Ae2PatternToolArguments.Processing,
        keys: Keys,
    ): ItemStack {
        val inputs = arrayOfNulls<GenericStack>(AEProcessingPattern.MAX_INPUT_SLOTS)
        for (entry in payload.inputs) {
            inputs[entry.slot] = GenericStack(
                keys.key(entry.key, "${Ae2PatternToolArguments.INPUTS} slot ${entry.slot}"),
                entry.amount,
            )
        }
        val outputs = arrayOfNulls<GenericStack>(AEProcessingPattern.MAX_OUTPUT_SLOTS)
        for (entry in payload.outputs) {
            outputs[entry.slot] = GenericStack(
                keys.key(entry.key, "${Ae2PatternToolArguments.OUTPUTS} slot ${entry.slot}"),
                entry.amount,
            )
        }

        if (inputs.none { it != null }) {
            throw Rejected("invalid_arguments", "a processing pattern needs at least one input")
        }
        if (outputs[0] == null) {
            throw Rejected("invalid_arguments", "a processing pattern needs its primary output in slot 0")
        }

        val encoded = try {
            PatternDetailsHelper.encodeProcessingPattern(inputs, outputs)
        } catch (invalid: IllegalArgumentException) {
            throw Rejected("encode_failed", invalid.message ?: "AE2 rejected that processing pattern")
        }
        val details = verifyEncoded(encoded, level, Ae2PatternToolArguments.MODE_PROCESSING, null)
        if (details !is AEProcessingPattern) {
            throw Rejected("encode_failed", "the encoded item is not a processing pattern")
        }

        // AE2 condenses a processing pattern's sparse slots by key, so the real decoding is compared with what
        // was asked for: a pattern that silently lost or merged an entry is never installed.
        val decodedInputs = LinkedHashMap<AEKey, Long>()
        for (input in details.inputs) {
            val primary = input.possibleInputs.firstOrNull() ?: continue
            decodedInputs.merge(primary.what(), input.multiplier, Long::plus)
        }
        if (decodedInputs != totals(inputs)) {
            throw Rejected(
                "encode_failed",
                "AE2's decoding of the processing pattern does not match the requested inputs",
            )
        }
        if (totals(details.outputs) != totals(outputs)) {
            throw Rejected(
                "encode_failed",
                "AE2's decoding of the processing pattern does not match the requested outputs",
            )
        }
        if (details.outputs.firstOrNull()?.what() != outputs[0]?.what()) {
            throw Rejected("encode_failed", "AE2 reordered the processing pattern's primary output")
        }
        return encoded
    }

    private fun encodeStonecutting(
        level: ServerLevel,
        payload: Ae2PatternToolArguments.Stonecutting,
        keys: Keys,
    ): ItemStack {
        val input = keys.item(payload.input, Ae2PatternToolArguments.INPUT)
        val container: Container = SimpleContainer(1)
        container.setItem(0, input.toStack())
        val claimed = payload.output?.let { keys.item(it, Ae2PatternToolArguments.OUTPUT) }

        val manager = level.recipeManager
        val recipe: StonecutterRecipe = if (payload.recipe != null) {
            val id = resourceId(payload.recipe, Ae2PatternToolArguments.RECIPE)
            val byId = manager.getAllRecipesFor(RecipeType.STONECUTTING).firstOrNull { it.id == id }
                ?: throw Rejected(
                    "unknown_recipe",
                    "parameter '${Ae2PatternToolArguments.RECIPE}' names a stonecutter recipe this world does not have",
                )
            if (!byId.matches(container, level)) {
                throw Rejected(
                    "recipe_mismatch",
                    "that stonecutter recipe does not cut the given input, so it cannot be encoded",
                )
            }
            if (claimed != null && !AEItemKey.matches(claimed, byId.getResultItem(level.registryAccess()))) {
                throw Rejected(
                    "output_mismatch",
                    "parameter '${Ae2PatternToolArguments.OUTPUT}' is not what that stonecutter recipe really produces",
                )
            }
            byId
        } else {
            val candidates = manager.getRecipesFor(RecipeType.STONECUTTING, container, level)
            if (candidates.isEmpty()) {
                throw Rejected(
                    "no_recipe",
                    "no stonecutter recipe of this world cuts that input; this tool only encodes real recipes",
                )
            }
            if (claimed != null) {
                candidates.firstOrNull {
                    AEItemKey.matches(claimed, it.getResultItem(level.registryAccess()))
                } ?: throw Rejected(
                    "output_mismatch",
                    "no stonecutter recipe cuts that input into the requested output; the recipes that match " +
                        "produce ${describeOutputs(candidates.map { it.getResultItem(level.registryAccess()) })}",
                )
            } else {
                val distinct = candidates.map { AEItemKey.of(it.getResultItem(level.registryAccess())) }.distinct()
                if (distinct.size != 1) {
                    throw Rejected(
                        "ambiguous_output",
                        "that input can be cut into ${distinct.size} different outputs, so pass " +
                            "'${Ae2PatternToolArguments.OUTPUT}'; they are " +
                            describeOutputs(candidates.map { it.getResultItem(level.registryAccess()) }),
                    )
                }
                candidates.first()
            }
        }

        val output = recipe.assemble(container, level.registryAccess())
        if (output.isEmpty) {
            throw Rejected("recipe_no_output", "that stonecutter recipe produced no item, so nothing was encoded")
        }
        val encoded = try {
            PatternDetailsHelper.encodeStonecuttingPattern(
                recipe, input, AEItemKey.of(output), payload.substitute,
            )
        } catch (invalid: IllegalArgumentException) {
            throw Rejected("encode_failed", invalid.message ?: "AE2 rejected that stonecutting pattern")
        }
        verifyEncoded(encoded, level, Ae2PatternToolArguments.MODE_STONECUTTING, output)
        return encoded
    }

    private fun encodeSmithing(
        level: ServerLevel,
        payload: Ae2PatternToolArguments.Smithing,
        keys: Keys,
    ): ItemStack {
        val template = keys.item(payload.template, Ae2PatternToolArguments.TEMPLATE)
        val base = keys.item(payload.base, Ae2PatternToolArguments.BASE)
        val addition = keys.item(payload.addition, Ae2PatternToolArguments.ADDITION)
        val container: Container = SimpleContainer(3)
        container.setItem(0, template.toStack())
        container.setItem(1, base.toStack())
        container.setItem(2, addition.toStack())
        val claimed = payload.output?.let { keys.item(it, Ae2PatternToolArguments.OUTPUT) }

        val manager = level.recipeManager
        val recipe: SmithingRecipe = if (payload.recipe != null) {
            val id = resourceId(payload.recipe, Ae2PatternToolArguments.RECIPE)
            val byId = manager.getAllRecipesFor(RecipeType.SMITHING).firstOrNull { it.id == id }
                ?: throw Rejected(
                    "unknown_recipe",
                    "parameter '${Ae2PatternToolArguments.RECIPE}' names a smithing recipe this world does not have",
                )
            if (!byId.matches(container, level)) {
                throw Rejected(
                    "recipe_mismatch",
                    "that smithing recipe does not match the given template, base and addition",
                )
            }
            byId
        } else {
            manager.getRecipeFor(RecipeType.SMITHING, container, level).orElse(null)
                ?: throw Rejected(
                    "no_recipe",
                    "no smithing recipe of this world matches that template, base and addition; this tool only " +
                        "encodes real recipes",
                )
        }

        // AE2's own smithing pattern only ever handles the vanilla smithing recipe classes; anything else would
        // produce an item AE2 cannot decode again, which is exactly what must never be installed.
        if (recipe !is SmithingTransformRecipe && recipe !is SmithingTrimRecipe) {
            throw Rejected(
                "unsupported_recipe",
                "that smithing recipe is not one of the smithing recipe types AE2 can encode",
            )
        }

        val output = recipe.assemble(container, level.registryAccess())
        if (output.isEmpty) {
            throw Rejected(
                "recipe_no_output",
                "that smithing recipe produced no item, so nothing could be encoded (armor trim recipes " +
                    "produce no storable item and cannot be encoded)",
            )
        }
        claimed?.let {
            if (!AEItemKey.matches(it, output)) {
                throw Rejected(
                    "output_mismatch",
                    "parameter '${Ae2PatternToolArguments.OUTPUT}' is not what that smithing recipe really produces",
                )
            }
        }

        val encoded = try {
            PatternDetailsHelper.encodeSmithingTablePattern(
                recipe, template, base, addition, AEItemKey.of(output), payload.substitute,
            )
        } catch (invalid: IllegalArgumentException) {
            throw Rejected("encode_failed", invalid.message ?: "AE2 rejected that smithing pattern")
        }
        verifyEncoded(encoded, level, Ae2PatternToolArguments.MODE_SMITHING, output)
        return encoded
    }

    /**
     * Decodes the freshly encoded item with AE2's own decoder and checks that it really is a pattern of the
     * requested mode that produces [output]; an item AE2 cannot decode is never installed.
     */
    private fun verifyEncoded(
        encoded: ItemStack,
        level: ServerLevel,
        mode: String,
        output: ItemStack?,
    ): IPatternDetails {
        val key = AEItemKey.of(encoded) ?: throw Rejected("encode_failed", "AE2 produced an empty pattern")
        val details = PatternDetailsHelper.decodePattern(key, level)
            ?: throw Rejected(
                "encode_failed",
                "AE2 could not decode the pattern it just encoded, so it was not installed",
            )
        if (modeOf(details) != mode) {
            throw Rejected("encode_failed", "the encoded item is not a '$mode' pattern")
        }
        if (output != null) {
            val produced = details.outputs.firstOrNull()
            val expected = AEItemKey.of(output)
            if (produced == null || expected == null || produced.what() != expected) {
                throw Rejected(
                    "encode_failed",
                    "the encoded pattern does not produce the recipe's real output, so it was not installed",
                )
            }
        }
        return details
    }

    /** AE2's mode name for a decoded pattern, or null for a decoder this tool does not know. */
    private fun modeOf(details: IPatternDetails?): String? = when (details) {
        is AECraftingPattern -> Ae2PatternToolArguments.MODE_CRAFTING
        is AEProcessingPattern -> Ae2PatternToolArguments.MODE_PROCESSING
        is AEStonecuttingPattern -> Ae2PatternToolArguments.MODE_STONECUTTING
        is AESmithingTablePattern -> Ae2PatternToolArguments.MODE_SMITHING
        else -> null
    }

    private fun totals(stacks: Array<out GenericStack?>): Map<AEKey, Long> {
        val totals = LinkedHashMap<AEKey, Long>()
        for (stack in stacks) {
            if (stack == null) continue
            totals.merge(stack.what(), stack.amount(), Long::plus)
        }
        return totals
    }

    private fun resourceId(value: String, name: String): ResourceLocation =
        ResourceLocation.tryParse(value)
            ?: throw Rejected("invalid_arguments", "parameter '$name' is not a valid resource id")

    private fun describeOutputs(outputs: List<ItemStack>): String =
        outputs.mapNotNull { AEItemKey.of(it)?.id?.toString() }.distinct().take(5)
            .joinToString(", ").ifEmpty { "nothing" }

    // --------------------------------------------------------- expectations

    /** Where the pattern item for the target slot comes from. */
    private enum class BlankSource { REUSE, EXTRACT }

    private fun expectation(
        expected: String,
        current: ItemStack,
        fingerprintOf: (AEKey) -> String,
    ): BlankSource {
        if (expected == Ae2PatternToolArguments.EXPECTED_EMPTY) {
            if (!current.isEmpty) {
                throw Rejected(
                    "stale_slot",
                    "that slot is not empty any more (it holds ${current.count} x ${itemId(current)}); run " +
                        "action '${Ae2PatternToolArguments.ACTION_VIEW}' and retry with the key the view reports",
                )
            }
            return BlankSource.EXTRACT
        }

        if (current.isEmpty) {
            throw Rejected(
                "stale_slot",
                "that slot is empty, so it does not hold the pattern key named as 'expected'; pass " +
                    "'${Ae2PatternToolArguments.EXPECTED_EMPTY}' to install into an empty slot",
            )
        }
        val key = AEItemKey.of(current)
            ?: throw Rejected("stale_slot", "that slot holds an item this tool cannot identify; run the view again")
        if (fingerprintOf(key) != expected) {
            throw Rejected(
                "stale_slot",
                "that slot changed since the view, so nothing was written; run action " +
                    "'${Ae2PatternToolArguments.ACTION_VIEW}' again and retry with the current pattern_key",
            )
        }
        if (!AEItems.BLANK_PATTERN.isSameAs(current) && !PatternDetailsHelper.isEncodedPattern(current)) {
            throw Rejected(
                "slot_not_pattern",
                "that slot holds ${itemId(current)}, which is not a blank or encoded pattern; this tool never " +
                    "deletes other items, so take it out first",
            )
        }
        if (current.count != 1) {
            throw Rejected(
                "slot_not_single",
                "that slot holds ${current.count} items, and a pattern slot must be empty or hold exactly one " +
                    "item; pick one of the container's empty_slots instead",
            )
        }
        return BlankSource.REUSE
    }

    private fun blankPatternKey(): AEItemKey? = AEItemKey.of(AEItems.BLANK_PATTERN.stack())

    private fun blankPatternsAvailable(
        grid: IGrid,
        player: ServerPlayer,
        terminal: MEIntelligentTerminalPart,
    ): Long {
        val key = blankPatternKey() ?: return 0L
        val source = IActionSource.ofPlayer(player, terminal)
        return grid.storageService.inventory.extract(key, Ae2ToolLimits.MAX_AMOUNT, Actionable.SIMULATE, source)
    }

    /** Hands an extracted blank pattern back to the network when the write could not happen. */
    private fun returnBlank(network: MEStorage, blankKey: AEItemKey, source: IActionSource, player: ServerPlayer) {
        val remainder = blankKey.toStack(1)
        try {
            if (network.insert(blankKey, 1, Actionable.MODULATE, source) == 1L) return
        } catch (failure: Exception) {
            LOG.error("Could not refund an extracted blank pattern to ME storage", failure)
        }
        // The network may be read-only or full; return the actual extracted item, never discard it.
        player.inventory.add(remainder)
        if (!remainder.isEmpty) player.drop(remainder, false)
    }

    // ------------------------------------------------------------- scanning

    /**
     * One pass over the grid's pattern containers.
     *
     * It is deliberately stateless between calls: nothing about a container or a pattern is remembered, so a
     * reloaded chunk, a wrenched provider or a re-encoded pattern can never make a later answer stale.
     */
    private class Scan(
        private val level: Level,
    ) {
        var containersFound = 0
            private set
        var containersSkipped = 0
            private set
        private var slotsScanned = 0

        /** Returns false once the per-request slot budget is used up. */
        fun takeSlot(): Boolean {
            if (slotsScanned >= MAX_SCANNED_SLOTS) return false
            slotsScanned++
            return true
        }

        /**
         * Finds this grid's pattern containers the way AE2's pattern access terminal does: every machine class
         * that implements [PatternContainer], every *active* machine of it, each container once. [only]
         * additionally restricts the result to a single address.
         */
        fun discover(
            grid: IGrid,
            show: String,
            only: Ae2PatternToolArguments.ContainerAddress?,
        ): List<ContainerInfo> {
            val seen = Collections.newSetFromMap(IdentityHashMap<PatternContainer, Boolean>())
            val result = ArrayList<ContainerInfo>()

            for (machineClass in grid.machineClasses) {
                if (!PatternContainer::class.java.isAssignableFrom(machineClass)) continue
                @Suppress("UNCHECKED_CAST")
                val containerClass = machineClass as Class<PatternContainer>
                for (container in grid.getActiveMachines(containerClass)) {
                    if (!seen.add(container)) continue
                    containersFound++
                    if (result.size >= MAX_CONTAINERS) {
                        containersSkipped++
                        continue
                    }
                    val info = try {
                        describe(container)
                    } catch (failure: Exception) {
                        LOG.warn("Skipping pattern container {}", container.javaClass.name, failure)
                        containersSkipped++
                        null
                    } ?: continue
                    if (!visibleIn(info, show)) continue
                    if (only != null && info.address != only) continue
                    result += info
                }
            }

            result.sortWith(compareBy { it.sortKey })
            return result
        }

        /** AE2's own visibility rules: the container's setting, narrowed by the `show` mode. */
        private fun visibleIn(info: ContainerInfo, show: String): Boolean = when (show) {
            Ae2PatternToolArguments.SHOW_ALL -> true
            Ae2PatternToolArguments.SHOW_NOT_FULL -> info.visible && info.emptySlots.isNotEmpty()
            else -> info.visible
        }

        private fun describe(container: PatternContainer): ContainerInfo {
            val inventory = container.terminalPatternInventory
            val group = container.terminalGroup
            val visible = container.isVisibleInTerminal
            val sortOrder = container.terminalSortOrder

            val blockEntity = (container as? PatternProviderLogicHost)?.blockEntity
            val containerLevel = blockEntity?.level ?: level
            val pos = blockEntity?.blockPos
            val side = partSide(blockEntity, container) ?: SIDE_BLOCK
            val address = pos?.let { Ae2PatternToolArguments.ContainerAddress(it.x, it.y, it.z, side) }

            var patterns = 0
            val empty = ArrayList<Int>()
            var truncated = false
            for (slot in 0 until inventory.size()) {
                if (inventory.getStackInSlot(slot).isEmpty) {
                    if (empty.size < MAX_EMPTY_SLOT_LIST) empty += slot else truncated = true
                } else {
                    patterns++
                }
            }

            val name = group.name().string
            return ContainerInfo(
                container = container,
                inventory = inventory,
                address = address,
                id = address?.toId(),
                level = containerLevel,
                name = name,
                icon = group.icon(),
                tooltip = group.tooltip().take(MAX_TOOLTIP_LINES).map { it.string },
                sortOrder = sortOrder,
                visible = visible,
                patterns = patterns,
                emptySlots = empty,
                emptySlotsTruncated = truncated,
                sortKey = "%016x|%s|%s|%s".format(
                    sortOrder,
                    name.lowercase(Locale.ROOT),
                    containerLevel.dimension().location().toString(),
                    address?.toId() ?: "anon:" + container.javaClass.name,
                ),
            )
        }

        /** The side a part based container sits on; a block based provider has none. */
        private fun partSide(blockEntity: Any?, container: PatternContainer): String? {
            val partHost = blockEntity as? IPartHost ?: return null
            for (direction in Direction.values()) {
                if (partHost.getPart(direction) === container) return direction.name.lowercase(Locale.ROOT)
            }
            return null
        }

        /** Renders one non-empty slot into the entry the view and the install response share. */
        fun describeEntry(container: ContainerInfo, slot: Int, stack: ItemStack): PatternEntry {
            val blank = AEItems.BLANK_PATTERN.isSameAs(stack)
            val details = if (blank) {
                null
            } else {
                try {
                    PatternDetailsHelper.decodePattern(stack, container.level)
                } catch (failure: Exception) {
                    LOG.warn("Could not decode a pattern in {}", container.name, failure)
                    null
                }
            }
            val key = AEItemKey.of(stack)
            val mode = when {
                blank -> MODE_BLANK
                details != null -> modeOf(details) ?: MODE_UNSUPPORTED
                else -> MODE_INVALID
            }
            return PatternEntry(
                container = container,
                slot = slot,
                stack = stack,
                mode = mode,
                details = details,
                recipe = key?.let { recipeOf(details, it) },
                editable = blank || PatternDetailsHelper.isEncodedPattern(stack),
                searchable = searchable(container, key, mode, details),
            )
        }

        /** AE2 stores the recipe id in a pattern's own NBT for crafting patterns only. */
        private fun recipeOf(details: IPatternDetails?, key: AEItemKey): String? = when (details) {
            is AEStonecuttingPattern -> details.recipeId.toString()
            is AESmithingTablePattern -> details.recipeId.toString()
            is AECraftingPattern -> key.tag?.takeIf { it.contains(NBT_RECIPE) }?.getString(NBT_RECIPE)

            else -> null
        }

        private fun searchable(
            container: ContainerInfo,
            key: AEItemKey?,
            mode: String,
            details: IPatternDetails?,
        ): String {
            val text = StringBuilder(128)
            text.append(container.id.orEmpty()).append(' ').append(container.name).append(' ')
            text.append(key?.id?.toString().orEmpty()).append(' ').append(key?.displayName?.string.orEmpty())
            text.append(' ').append(mode)

            val keys = ArrayList<AEKey>(MAX_SEARCH_KEYS)
            collectKeys(keys, details)
            for (candidate in keys.take(MAX_SEARCH_KEYS)) {
                text.append(' ').append(candidate.id).append(' ').append(candidate.displayName.string)
            }
            return text.toString().take(MAX_SEARCH_TEXT)
        }

        /**
         * Every key the live patterns of [container] mention. Used to resolve a fingerprint the network does not
         * store and cannot craft, without trusting anything remembered from an earlier call.
         */
        fun patternKeys(container: ContainerInfo): List<AEKey> {
            val keys = ArrayList<AEKey>(16)
            val inventory = container.inventory
            for (slot in 0 until inventory.size()) {
                if (!takeSlot()) {
                    LOG.warn("Stopped resolving pattern keys of {} after {} slots", container.name, MAX_SCANNED_SLOTS)
                    break
                }
                val stack = inventory.getStackInSlot(slot)
                if (stack.isEmpty) continue
                val details = try {
                    PatternDetailsHelper.decodePattern(stack, container.level)
                } catch (failure: Exception) {
                    null
                } ?: continue
                collectKeys(keys, details)
            }
            return keys
        }

        /** Adds the keys a decoded pattern mentions to [out]. */
        private fun collectKeys(out: MutableList<AEKey>, details: IPatternDetails?) {
            when (details) {
                is AECraftingPattern -> {
                    details.sparseInputs.forEach { if (it != null) out += it.what() }
                    details.sparseOutputs.forEach { out += it.what() }
                }

                is AEProcessingPattern -> {
                    details.sparseInputs.forEach { if (it != null) out += it.what() }
                    details.sparseOutputs.forEach { if (it != null) out += it.what() }
                }

                is AEStonecuttingPattern -> {
                    out += details.input
                    details.outputs.forEach { out += it.what() }
                }

                is AESmithingTablePattern -> {
                    out += details.template
                    out += details.base
                    out += details.addition
                    details.outputs.forEach { out += it.what() }
                }

                else -> {}
            }
        }
    }

    /** One pattern container of the grid. */
    private class ContainerInfo(
        val container: PatternContainer,
        val inventory: InternalInventory,
        val address: Ae2PatternToolArguments.ContainerAddress?,
        val id: String?,
        val level: Level,
        val name: String,
        val icon: AEItemKey?,
        val tooltip: List<String>,
        val sortOrder: Long,
        val visible: Boolean,
        val patterns: Int,
        val emptySlots: List<Int>,
        val emptySlotsTruncated: Boolean,
        val sortKey: String,
    )

    /** One non-empty pattern slot. */
    private class PatternEntry(
        val container: ContainerInfo,
        val slot: Int,
        val stack: ItemStack,
        val mode: String,
        val details: IPatternDetails?,
        val recipe: String?,
        val editable: Boolean,
        val searchable: String,
    ) {
        val sortKey: String get() = "${container.sortKey}|${slot.toString().padStart(4, '0')}"
    }

    /**
     * Resolves the model's key references.
     *
     * A fingerprint is looked up by the parent executor first (this network's inventory and its craftable keys)
     * and then in the live contents of this grid's patterns, because a pattern may well mention a key that is
     * stored nowhere on the network. A plain registry id is resolved from the item / fluid registry, which is
     * the only way to name a key this world does not contain yet.
     */
    private class Keys(
        private val scan: Scan,
        private val grid: IGrid,
        private val containers: List<ContainerInfo>,
        private val resolveKey: (IGrid, String) -> AEKey?,
        private val fingerprintOf: (AEKey) -> String,
    ) {
        private var patternKeysByFingerprint: Map<String, AEKey>? = null

        fun key(reference: Ae2PatternToolArguments.KeyRef, name: String): AEKey = when (reference) {
            is Ae2PatternToolArguments.KeyRef.Fingerprint -> byFingerprint(reference.value, name)
            is Ae2PatternToolArguments.KeyRef.RegistryId -> byRegistryId(reference, name)
        }

        fun item(reference: Ae2PatternToolArguments.KeyRef, name: String): AEItemKey {
            val key = key(reference, name)
            if (key !is AEItemKey) {
                throw Rejected(
                    "not_an_item",
                    "parameter '$name' must name an item; a crafting, stonecutting or smithing pattern cannot " +
                        "hold a fluid",
                )
            }
            return key
        }

        private fun byFingerprint(fingerprint: String, name: String): AEKey {
            resolveKey(grid, fingerprint)?.let { return it }

            var patternKeys = patternKeysByFingerprint
            if (patternKeys == null) {
                val indexed = HashMap<String, AEKey>()
                // Scan once per install, not once per input: the shared slot bound must not discard later keys.
                val allContainers = ArrayList<ContainerInfo>()
                allContainers.addAll(containers)
                for (container in scan.discover(grid, Ae2PatternToolArguments.SHOW_VISIBLE, null)) {
                    if (containers.none { it.container === container.container }) allContainers += container
                }
                for (container in allContainers) {
                    for (key in scan.patternKeys(container)) indexed[fingerprintOf(key)] = key
                }
                patternKeys = indexed
                patternKeysByFingerprint = indexed
            }
            patternKeys[fingerprint]?.let { return it }
            throw Rejected(
                "unknown_key",
                "parameter '$name' is not a key this network stores, can craft or uses in one of its patterns; " +
                    "run action '${Ae2PatternToolArguments.ACTION_VIEW}' and use a key it reports, or name a " +
                    "plain registry entry with '${Ae2PatternToolArguments.ITEM}' or " +
                    "'${Ae2PatternToolArguments.FLUID}'",
            )
        }

        private fun byRegistryId(reference: Ae2PatternToolArguments.KeyRef.RegistryId, name: String): AEKey {
            val id = resourceId(reference.id, name)
            val key = if (reference.fluid) {
                val fluid = ForgeRegistries.FLUIDS.getValue(id)
                if (!ForgeRegistries.FLUIDS.containsKey(id) || fluid == null || fluid === Fluids.EMPTY) null
                else AEFluidKey.of(fluid)
            } else {
                val item = ForgeRegistries.ITEMS.getValue(id)
                if (!ForgeRegistries.ITEMS.containsKey(id) || item == null || item === Items.AIR) null
                else AEItemKey.of(item)
            }
            return key ?: throw Rejected(
                "unknown_key",
                "parameter '$name' does not name a registered ${if (reference.fluid) "fluid" else "item"}",
            )
        }
    }

    // --------------------------------------------------------------- output

    private fun containerJson(container: ContainerInfo): JsonObject = buildJsonObject {
        container.id?.let { put("container", it) }
        put("addressable", container.id != null)
        put("dimension", container.level.dimension().location().toString())
        put("name", container.name)
        put("visible", container.visible)
        put("sort_order", container.sortOrder)
        putJsonObject("group_icon") {
            put("id", container.icon?.id?.toString().orEmpty())
            put("name", container.icon?.displayName?.string.orEmpty())
        }
        putJsonArray("tooltip") { for (line in container.tooltip) add(JsonPrimitive(line)) }
        container.address?.let { address ->
            putJsonObject("position") {
                put("x", address.x)
                put("y", address.y)
                put("z", address.z)
                put("side", address.side)
            }
        }
        put("slots", container.inventory.size())
        put("patterns", container.patterns)
        putJsonArray("empty_slots") { for (slot in container.emptySlots) add(JsonPrimitive(slot)) }
        put("empty_slots_truncated", container.emptySlotsTruncated)
        put("has_empty_slot", container.emptySlots.isNotEmpty())
    }

    private fun patternJson(entry: PatternEntry, fingerprintOf: (AEKey) -> String): JsonObject {
        val key = AEItemKey.of(entry.stack)
        val fingerprint = key?.let { fingerprintOf(it) }.orEmpty()
        return buildJsonObject {
            put("container", entry.container.id.orEmpty())
            put("dimension", entry.container.level.dimension().location().toString())
            put("container_name", entry.container.name)
            put("slot", entry.slot)
            put("pattern_key", fingerprint)
            put("expected", fingerprint)
            put("mode", entry.mode)
            put("editable", entry.editable)
            if (key != null) {
                putJsonObject("item") {
                    put("id", key.id.toString())
                    put("name", key.displayName.string)
                    put("count", entry.stack.count)
                }
            }
            entry.recipe?.let { put("recipe", it) }
            when (val details = entry.details) {
                is AECraftingPattern -> {
                    put("substitute", details.canSubstitute)
                    put("fluid_substitute", details.canSubstituteFluids)
                    putJsonArray("inputs") { addSparse(details.sparseInputs, fingerprintOf) }
                    putJsonArray("outputs") { addSparse(details.sparseOutputs, fingerprintOf) }
                }

                is AEProcessingPattern -> {
                    putJsonArray("inputs") { addSparse(details.sparseInputs, fingerprintOf) }
                    putJsonArray("outputs") { addSparse(details.sparseOutputs, fingerprintOf) }
                }

                is AEStonecuttingPattern -> {
                    put("substitute", details.canSubstitute)
                    putJsonArray("inputs") {
                        addRole(Ae2PatternToolArguments.INPUT, details.input, fingerprintOf)
                    }
                    putJsonArray("outputs") { addSparse(details.outputs, fingerprintOf) }
                }

                is AESmithingTablePattern -> {
                    put("substitute", details.canSubstitute)
                    putJsonArray("inputs") {
                        addRole(Ae2PatternToolArguments.TEMPLATE, details.template, fingerprintOf)
                        addRole(Ae2PatternToolArguments.BASE, details.base, fingerprintOf)
                        addRole(Ae2PatternToolArguments.ADDITION, details.addition, fingerprintOf)
                    }
                    putJsonArray("outputs") { addSparse(details.outputs, fingerprintOf) }
                }

                else -> {}
            }
        }
    }

    private fun JsonArrayBuilder.addSparse(stacks: Array<out GenericStack?>, fingerprintOf: (AEKey) -> String) {
        var rows = 0
        for (slot in stacks.indices) {
            val stack = stacks[slot] ?: continue
            if (rows++ >= MAX_IO_ROWS) break
            add(stackRow(stack, fingerprintOf, "slot" to JsonPrimitive(slot)))
        }
    }

    /** One input row of a stonecutting / smithing pattern, which names its slots by role instead of index. */
    private fun JsonArrayBuilder.addRole(role: String, key: AEKey, fingerprintOf: (AEKey) -> String) {
        add(stackRow(GenericStack(key, 1), fingerprintOf, "role" to JsonPrimitive(role)))
    }

    private fun stackRow(
        stack: GenericStack,
        fingerprintOf: (AEKey) -> String,
        vararg extra: Pair<String, JsonElement>,
    ): JsonObject {
        val key = stack.what()
        return buildJsonObject {
            for ((name, value) in extra) put(name, value)
            put("key", fingerprintOf(key))
            put("type", key.type.id.toString())
            put("id", key.id.toString())
            put("name", key.displayName.string)
            put("amount", stack.amount())
        }
    }

    // -------------------------------------------------------------- helpers

    private fun resolveGrid(player: ServerPlayer, terminal: MEIntelligentTerminalPart): IGrid = try {
        terminal.resolve(player, ALLOW_REMOTE_AFTER_LOCK)
    } catch (denied: IllegalArgumentException) {
        throw TerminalUnavailable(denied.message)
    }

    private fun requireServerLevel(terminal: MEIntelligentTerminalPart): ServerLevel =
        terminal.level as? ServerLevel
            ?: throw TerminalUnavailable("the ME terminal is not in a loaded server level")

    private fun itemId(stack: ItemStack): String = ForgeRegistries.ITEMS.getKey(stack.item).toString()

    private fun failure(error: String, message: String): JsonObject = buildJsonObject {
        put("ok", false)
        put("error", error)
        put("message", message)
    }

    /** Signals that the terminal itself refused the request, as opposed to malformed arguments. */
    private class TerminalUnavailable(message: String?) : RuntimeException(message)

    /** A request that was understood but cannot be carried out; reported with its own error code. */
    private class Rejected(val error: String, message: String) : RuntimeException(message)
}
