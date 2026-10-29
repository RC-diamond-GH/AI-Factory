package com.rcdiamond.aifactory.addon.integrated_dynamics.block

import com.rcdiamond.aifactory.addon.integrated_dynamics.IntegratedDynamicsAddon
import com.rcdiamond.aifactory.addon.integrated_dynamics.server.IntegratedDynamicsSessionRecord
import com.rcdiamond.aifactory.addon.integrated_dynamics.server.IntegratedDynamicsSessions
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.util.LazyOptional
import org.cyclops.cyclopscore.datastructure.DimPos
import org.cyclops.integrateddynamics.api.network.INetwork
import org.cyclops.integrateddynamics.api.network.INetworkElement
import org.cyclops.integrateddynamics.api.network.INetworkElementProvider
import org.cyclops.integrateddynamics.capability.network.NetworkCarrierConfig
import org.cyclops.integrateddynamics.capability.networkelementprovider.NetworkElementProviderConfig
import org.cyclops.integrateddynamics.capability.networkelementprovider.NetworkElementProviderSingleton
import org.cyclops.integrateddynamics.core.blockentity.BlockEntityCableConnectable

/**
 * The block entity of the Integrated Dynamics terminal.
 *
 * It is a first-class member of an Integrated Dynamics cable network, not a listener that peeks at a
 * neighbouring cable: like Integrated Dynamics' own cable-connected machines it extends
 * [BlockEntityCableConnectable] (the mod's own cable, network-carrier and path-element wiring) and
 * *advertises* an [INetworkElementProvider] through [NetworkElementProviderConfig]. Integrated Dynamics
 * therefore discovers this block during its own network setup, stores the formed network in the carrier and
 * keeps this position in the network's path cluster; the terminal never guesses which network it belongs to.
 *
 * ### Persistence
 * Only two things are persisted, both stable identities and both deliberately tiny:
 * - `TerminalId`, a UUID generated on first use and kept for the life of the block. Conversations belong to
 *   this identity, so moving the terminal must not change it.
 * - `SessionIds`, the set of conversation ids this terminal references, at most
 *   [IntegratedDynamicsSessions.MAX_SESSIONS_PER_TERMINAL] entries. The conversation bodies live in the
 *   world save, never here; the block only keeps the references it may resolve.
 *
 * Both keys are what the block's loot table copies into the dropped item (`copy_nbt` from `block_entity`), so
 * a broken-and-replaced terminal keeps its identity and its sessions instead of orphaning them. A terminal
 * placed from a crafted item carries no `BlockEntityTag` and simply generates a fresh identity.
 *
 * ### Access
 * [resolve] is the single gate every operation goes through. It only ever trusts the [ServerPlayer] the
 * network layer resolved from the connection and re-derives everything else from the world, mirroring the
 * AE2 terminal's contract: loaded, same dimension, interaction permission, and - unless the caller is
 * performing a leased background operation - eight blocks of distance. The returned network is the
 * authoritative one Integrated Dynamics built.
 */
class IntegratedDynamicsTerminalBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntityCableConnectable(IntegratedDynamicsAddon.TERMINAL_BLOCK_ENTITY.get(), pos, state) {

    /**
     * Creates this terminal's network element on demand, exactly like Integrated Dynamics' own cable-connected
     * block entities do through cyclopscore's capability dispatcher.
     */
    private val elementProvider: INetworkElementProvider = object : NetworkElementProviderSingleton() {
        override fun createNetworkElement(world: Level, blockPos: BlockPos): INetworkElement =
            IntegratedDynamicsTerminalNetworkElement(DimPos.of(world, blockPos))
    }

    /** The stable identity of this terminal; generated exactly once and then persisted. */
    private var storedTerminalId: UUID? = null

    /** The referenced conversations, in insertion order, so the persisted list is stable across saves. */
    private val sessions: MutableSet<UUID> = LinkedHashSet()

    init {
        // Must happen during construction: CyclopsBlockEntity freezes its capability map when it loads.
        // LazyOptional only accepts a supplier (Forge's NonNullSupplier), never a plain value.
        addCapabilityInternal(
            NetworkElementProviderConfig.CAPABILITY,
            LazyOptional.of<INetworkElementProvider> { elementProvider },
        )
    }

    /**
     * The stable terminal id. Generated lazily so a freshly placed terminal has one before anything can be
     * stored against it, and always the same afterwards - including across a break/place cycle, because the
     * loot table carries it in the item.
     */
    val terminalId: UUID
        get() {
            storedTerminalId?.let { return it }
            val created = UUID.randomUUID()
            storedTerminalId = created
            setChanged()
            return created
        }

    /** A read-only snapshot of the referenced conversation ids; the caller can never mutate our set. */
    val sessionIds: Set<UUID>
        get() = LinkedHashSet(sessions)

    /** How many conversations this terminal references, without copying the set. */
    val sessionCount: Int
        get() = sessions.size

    /** Whether this terminal references [id], without copying the set. */
    fun hasSession(id: UUID): Boolean = sessions.contains(id)

    /**
     * Starts referencing [id]. Refuses to grow past the shared server bound and is a no-op for an id that is
     * already referenced; both cases are reported by the return value.
     */
    fun addSession(id: UUID): Boolean {
        if (sessions.size >= IntegratedDynamicsSessions.MAX_SESSIONS_PER_TERMINAL && !sessions.contains(id)) {
            return false
        }
        if (!sessions.add(id)) return false
        setChanged()
        return true
    }

    /** Stops referencing [id]; no-op for an unknown id, as reported by the return value. */
    fun removeSession(id: UUID): Boolean {
        if (!sessions.remove(id)) return false
        setChanged()
        return true
    }

    /**
     * Resolves one of *this* terminal's referenced conversations through the session service.
     *
     * Deliberately not a store lookup by conversation id: the reference persisted here is what makes the
     * conversation reachable from this terminal, so an id this block does not reference answers `null` even if
     * the conversation exists elsewhere in the world.
     *
     * Returns `null` when this block is not on a server level (the service is server-main-thread only).
     */
    fun getSession(id: UUID): IntegratedDynamicsSessionRecord? {
        val serverLevel = level as? ServerLevel ?: return null
        return IntegratedDynamicsSessions.find(serverLevel, this, id)
    }

    /**
     * Resolves the network this terminal belongs to, if [player] may use it right now.
     *
     * Enforced, in order:
     * - this block entity is present, not removed, loaded, and [player] is in its dimension;
     * - [player] may interact with the block position (`Level.mayInteract`, i.e. spawn protection and the
     *   like);
     * - with `allowRemote = false` [player] is within [INTERACTION_RANGE] blocks - the terminal is being used
     *   at the block; `allowRemote = true` relaxes exactly that bound and is only passed by the session
     *   service for a lock holder's background turn;
     * - the carrier holds a network Integrated Dynamics has finished initializing and has not killed.
     *
     * @throws IllegalArgumentException with a message safe to show [player] when the terminal is not usable.
     */
    fun resolve(player: ServerPlayer, allowRemote: Boolean): INetwork {
        val terminalLevel = level as? ServerLevel
            ?: throw IllegalArgumentException(ERROR_NOT_LOADED)
        if (isRemoved || !terminalLevel.isLoaded(blockPos)) {
            throw IllegalArgumentException(ERROR_NOT_LOADED)
        }
        if (player.serverLevel() !== terminalLevel) {
            throw IllegalArgumentException(ERROR_NOT_LOADED)
        }
        // The world must still hold *this* instance at the position: a stale reference to a replaced block
        // entity (chunk reload, block swapped out) must never authorize anything.
        if (terminalLevel.getBlockEntity(blockPos) !== this) {
            throw IllegalArgumentException(ERROR_NOT_LOADED)
        }
        if (!terminalLevel.mayInteract(player, blockPos)) {
            throw IllegalArgumentException(ERROR_PERMISSION)
        }
        if (!allowRemote &&
            player.distanceToSqr(
                blockPos.x + 0.5,
                blockPos.y + 0.5,
                blockPos.z + 0.5,
            ) > INTERACTION_RANGE_SQ
        ) {
            throw IllegalArgumentException(ERROR_TOO_FAR)
        }

        val network = getCapability(NetworkCarrierConfig.CAPABILITY).resolve().getOrNull()?.network
            ?: throw IllegalArgumentException(ERROR_UNAVAILABLE)
        if (!network.isInitialized || network.isKilled) {
            throw IllegalArgumentException(ERROR_UNAVAILABLE)
        }
        return network
    }

    override fun read(tag: CompoundTag) {
        super.read(tag)
        storedTerminalId = if (tag.hasUUID(TERMINAL_ID_KEY)) tag.getUUID(TERMINAL_ID_KEY) else null
        sessions.clear()
        val stored = tag.getList(SESSION_IDS_KEY, Tag.TAG_INT_ARRAY.toInt())
        for (index in 0 until stored.size) {
            if (sessions.size >= IntegratedDynamicsSessions.MAX_SESSIONS_PER_TERMINAL) break
            val id = NbtUtils.loadUUID(stored.get(index))
            sessions.add(id)
        }
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        storedTerminalId?.let { tag.putUUID(TERMINAL_ID_KEY, it) }
        if (sessions.isNotEmpty()) {
            val list = ListTag()
            for (id in sessions) list.add(NbtUtils.createUUID(id))
            tag.put(SESSION_IDS_KEY, list)
        }
    }

    companion object {
        /**
         * NBT key holding the stable terminal id. The block's loot table copies this key into the dropped
         * item's `BlockEntityTag`; rename it only together with `data/ai_factory/loot_tables/blocks/
         * integrated_dynamics_terminal.json`.
         */
        const val TERMINAL_ID_KEY: String = "TerminalId"

        /**
         * NBT key holding the referenced conversation ids (a list of UUID int-arrays). Copied into the dropped
         * item alongside [TERMINAL_ID_KEY].
         */
        const val SESSION_IDS_KEY: String = "SessionIds"

        /** Blocks a player must be within when using the terminal at the block. */
        const val INTERACTION_RANGE: Double = 8.0

        private const val INTERACTION_RANGE_SQ: Double = INTERACTION_RANGE * INTERACTION_RANGE

        /** Safe messages; the same convention the shared session layer uses for `{"ok":false}` answers. */
        const val ERROR_NOT_LOADED: String = "That Integrated Dynamics terminal is not loaded"
        const val ERROR_PERMISSION: String = "You are not allowed to use that Integrated Dynamics terminal"
        const val ERROR_TOO_FAR: String = "That Integrated Dynamics terminal is too far away"
        const val ERROR_UNAVAILABLE: String =
            "That Integrated Dynamics terminal is not connected to an initialized Integrated Dynamics network"

        /** Fallback shown when a rejection somehow carries no message. */
        const val DEFAULT_REJECTION: String = "That Integrated Dynamics terminal cannot be used right now"
    }
}
