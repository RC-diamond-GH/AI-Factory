package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.block

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.IntegratedDynamicsAddon
import com.rcdiamond.aifactory.network.SharedSessionNetwork
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.cyclops.integrateddynamics.core.block.BlockWithEntityGuiCabled
import org.cyclops.integrateddynamics.core.blockentity.BlockEntityCableConnectable
import org.cyclops.integrateddynamics.core.helper.WrenchHelpers

/**
 * The Integrated Dynamics terminal, as a whole block.
 *
 * Because it is a block and not a cable part, it occupies a position of its own: a plain
 * `{kind,dimension,x,y,z}` address is unambiguous, and no face is ever part of the contract.
 *
 * It joins an Integrated Dynamics network natively. This class extends the mod's own base for cable-connected
 * machines, [BlockWithEntityGuiCabled], and inherits the complete native lifecycle from it: cables are
 * connected on place and disconnected on removal, and every neighbour change is reported through
 * [org.cyclops.integrateddynamics.core.helper.NetworkHelpers.onElementProviderBlockNeighborChange] - the hook
 * Integrated Dynamics requires from any block that advertises a network element provider, and the one that
 * makes networks extend, split and re-form around this block. The block entity advertises that provider, so
 * the block becomes a real network member rather than something that inspects a neighbouring cable.
 *
 * Right clicking never opens a container menu: the server validates the terminal through
 * [IntegratedDynamicsTerminalBlockEntity.resolve] and then has the shared session layer send the client the
 * terminal locator, which is what opens the session screen. A rejected use reports the safe reason on the
 * action bar and opens nothing.
 */
class IntegratedDynamicsTerminalBlock : BlockWithEntityGuiCabled(
    BlockBehaviour.Properties.of()
        .strength(2.0f, 5.0f)
        .sound(SoundType.METAL),
    { pos, state -> IntegratedDynamicsTerminalBlockEntity(pos, state) },
) {

    /**
     * Server-side ticking: Integrated Dynamics' cable-connected block ticker refreshes this block's cable
     * connections when they are unknown and revalidates the network element after its chunk was reloaded. The
     * connection state is persisted and streamed to clients, so the client needs no ticker - the same split
     * Integrated Dynamics uses for its own cable-connected machines.
     */
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>,
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        if (type != IntegratedDynamicsAddon.TERMINAL_BLOCK_ENTITY.get()) return null
        // The ticker only ever runs for this block's own block entity; the cast is what BaseEntityBlock's
        // createTickerHelper does internally, spelled out so the generic bounds stay obvious.
        @Suppress("UNCHECKED_CAST")
        return TERMINAL_TICKER as BlockEntityTicker<T>
    }

    /**
     * Gives a freshly placed terminal its stable identity immediately, so the id exists (and is saved) before
     * anything can be stored against it. A stack carrying a `BlockEntityTag` has already been applied by the
     * block item, in which case the id it carries wins, so a terminal moved by breaking it keeps its identity.
     */
    override fun setPlacedBy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack,
    ) {
        super.setPlacedBy(level, pos, state, placer, stack)
        if (!level.isClientSide) {
            (level.getBlockEntity(pos) as? IntegratedDynamicsTerminalBlockEntity)?.terminalId
        }
    }

    /**
     * A wrench held while sneaking removes the terminal with its own drops (the inherited loot table carries
     * the terminal id and the session references); any other use validates the terminal and asks the server's
     * session layer to open it on this client.
     *
     * The interaction is consumed in every case, so the click never falls through to whatever is behind the
     * terminal, and the server stays authoritative: the client is only ever told to open a locator the server
     * has just proven resolvable.
     */
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult,
    ): InteractionResult {
        val heldItem = player.getItemInHand(hand)
        if (!level.isClientSide &&
            WrenchHelpers.isWrench(player, heldItem, level, pos, hit.direction) &&
            player.isSecondaryUseActive
        ) {
            if (!level.mayInteract(player, pos)) {
                player.displayClientMessage(Component.literal(IntegratedDynamicsTerminalBlockEntity.ERROR_PERMISSION), true)
                return InteractionResult.FAIL
            }
            val range = IntegratedDynamicsTerminalBlockEntity.INTERACTION_RANGE
            if (player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) > range * range) {
                player.displayClientMessage(Component.literal(IntegratedDynamicsTerminalBlockEntity.ERROR_TOO_FAR), true)
                return InteractionResult.FAIL
            }
            Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, heldItem)
            level.destroyBlock(pos, false)
            return InteractionResult.SUCCESS
        }

        val serverPlayer = player as? ServerPlayer
        if (serverPlayer != null) {
            val terminal = level.getBlockEntity(pos) as? IntegratedDynamicsTerminalBlockEntity
            if (terminal == null) {
                serverPlayer.displayClientMessage(
                    Component.literal(IntegratedDynamicsTerminalBlockEntity.ERROR_NOT_LOADED),
                    true,
                )
            } else {
                try {
                    terminal.resolve(serverPlayer, allowRemote = false)
                    SharedSessionNetwork.openTerminal(serverPlayer, terminalLocator(level, pos))
                } catch (rejected: IllegalArgumentException) {
                    serverPlayer.displayClientMessage(
                        Component.literal(
                            rejected.message ?: IntegratedDynamicsTerminalBlockEntity.DEFAULT_REJECTION,
                        ),
                        true,
                    )
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    /** The address the session layer parses: a kind plus the terminal's exact block position. */
    private fun terminalLocator(level: Level, pos: BlockPos): JsonObject = buildJsonObject {
        put(KIND_FIELD, KIND)
        put("dimension", level.dimension().location().toString())
        put("x", pos.x)
        put("y", pos.y)
        put("z", pos.z)
    }

    companion object {
        /** Locator field naming the terminal kind; the session layer routes on its value. */
        const val KIND_FIELD: String = "kind"

        /** The only value [KIND_FIELD] may carry for this block. */
        const val KIND: String = "integrated_dynamics"

        /** One shared server-side ticker instance; the base ticker is stateless. */
        private val TERMINAL_TICKER: BlockEntityCableConnectable.Ticker<IntegratedDynamicsTerminalBlockEntity> =
            BlockEntityCableConnectable.Ticker()
    }
}
