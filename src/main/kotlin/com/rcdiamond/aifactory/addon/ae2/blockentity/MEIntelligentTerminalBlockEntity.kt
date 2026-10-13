package com.rcdiamond.aifactory.addon.ae2.blockentity

import appeng.api.networking.GridFlags
import appeng.api.networking.IGrid
import appeng.api.networking.IManagedGridNode
import appeng.blockentity.grid.AENetworkBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * The intelligent terminal: a normal AE2 grid block entity that owns no storage and no menu of its own.
 *
 * It is an [AENetworkBlockEntity], so the whole grid lifecycle (node creation on ready, teardown when the
 * chunk unloads, saving and loading of the node, connectability on every side) is AE2's; this class adds
 * exactly two things: the channel requirement and idle power of a terminal, and [resolve], the one
 * permission-checked entry point every other part of the addon (and the tool executor) goes through.
 *
 * There is no menu: using the block validates the terminal and then hands the client a locator, so the
 * session list can be opened as a screen instead of an AE2 container.
 */
class MEIntelligentTerminalBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
) : AENetworkBlockEntity(type, pos, state) {

    /**
     * A terminal needs a channel from the network and draws its idle power while it is connected; the
     * values mirror AE2's own terminals.
     *
     * Called from the superclass constructor, so it must not touch this instance's state.
     */
    override fun createMainNode(): IManagedGridNode {
        return super.createMainNode()
            .setFlags(GridFlags.REQUIRE_CHANNEL)
            .setIdlePowerUsage(TERMINAL_IDLE_POWER)
    }

    /**
     * Resolves the grid this terminal is on, if [player] is allowed to use it right now.
     *
     * Enforced, in order:
     * - the block entity is loaded and not removed, and the player is in the same dimension;
     * - the player may interact with this position (`Level.mayInteract`, i.e. spawn protection and the like;
     *   AE2 15.x has no grid security service, so this is the complete vanilla permission check);
     * - with `allowRemote = false` the player is within [INTERACTION_RANGE] blocks of the terminal;
     * - the node is active on a booted network.
     *
     * `allowRemote = true` relaxes only the distance, and is used for work that continues while the screen
     * is closed - a background agent turn. The caller decides which of the two applies; this method never
     * trusts a position, player id or cell id from the network, because it only ever gets the real
     * [ServerPlayer] the core resolved from the connection.
     *
     * @throws IllegalArgumentException with a message safe to show [player] when the terminal is not usable.
     */
    fun resolve(player: ServerPlayer, allowRemote: Boolean): IGrid {
        val terminalLevel = level as? ServerLevel ?: throw IllegalArgumentException(ERROR_NOT_LOADED)
        if (isRemoved || !terminalLevel.isLoaded(blockPos)) {
            throw IllegalArgumentException(ERROR_NOT_LOADED)
        }
        if (player.serverLevel() !== terminalLevel) {
            throw IllegalArgumentException(ERROR_NOT_LOADED)
        }
        if (!terminalLevel.mayInteract(player, blockPos)) {
            throw IllegalArgumentException(ERROR_PERMISSION)
        }
        if (!allowRemote &&
            player.distanceToSqr(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5) > INTERACTION_RANGE_SQ
        ) {
            throw IllegalArgumentException(ERROR_TOO_FAR)
        }

        val node = mainNode.node ?: throw IllegalArgumentException(ERROR_UNAVAILABLE)
        if (!node.isActive) {
            throw IllegalArgumentException(ERROR_UNAVAILABLE)
        }
        return node.grid ?: throw IllegalArgumentException(ERROR_UNAVAILABLE)
    }

    /** Non-throwing form of [resolve] for callers that only need a yes/no answer (for example a tool tick). */
    fun isUsable(player: ServerPlayer, allowRemote: Boolean): Boolean = try {
        resolve(player, allowRemote)
        true
    } catch (_: IllegalArgumentException) {
        false
    }

    companion object {
        /** Passive AE/t draw of a connected terminal. */
        const val TERMINAL_IDLE_POWER: Double = 1.0

        /** Blocks a player must be within when the screen is opened ([Level.mayInteract] aside). */
        const val INTERACTION_RANGE: Double = 8.0

        private const val INTERACTION_RANGE_SQ: Double = INTERACTION_RANGE * INTERACTION_RANGE

        /** Safe messages; the same convention the shared session RPC layer uses for `{"ok":false}` answers. */
        const val ERROR_NOT_LOADED: String = "That intelligent terminal is not loaded"
        const val ERROR_PERMISSION: String = "You are not allowed to use that intelligent terminal"
        const val ERROR_TOO_FAR: String = "That intelligent terminal is too far away"
        const val ERROR_UNAVAILABLE: String = "That intelligent terminal is not connected to an active ME network"
    }
}
