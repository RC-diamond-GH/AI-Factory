package com.rcdiamond.aifactory.addon.ae2.part

import appeng.api.networking.IGrid
import appeng.api.parts.IPartItem
import appeng.api.parts.IPartModel
import appeng.api.parts.PartModels
import appeng.parts.PartModel
import appeng.parts.reporting.AbstractDisplayPart
import com.rcdiamond.aifactory.AIFactory
import com.rcdiamond.aifactory.network.SharedSessionNetwork
import com.rcdiamond.aifactory.shared.SharedTerminalLocator
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/**
 * The intelligent terminal, installed as an AE2 part on a cable bus.
 *
 * A part shares its block with the cable it sits on, so one cable hosts up to six genuinely different
 * terminals; every request addresses one of them by the exact side it is installed on
 * ([SharedTerminalLocator]) and no face is ever assumed.
 *
 * ### What AE2 already provides
 * [AbstractDisplayPart] is AE2's own reporting part, so the complete installation and persistence path is
 * AE2's: the cable bus item places the part, the host stores it in the cable's block entity, and the part is
 * saved, streamed and removed with its host. The collision boxes ([appeng.api.parts.IPart.getBoxes], which
 * is the tiny screen panel and not the full block), the in-face spin a wrench turns, the client power and
 * channel state, the `REQUIRE_CHANNEL` flag and the idle draw all come from the superclass. This class adds
 * only the three things that make the part *our* terminal: its three screen models, the permission-checked
 * [resolve], and the open request.
 *
 * ### Server / client split
 * No client-only type is referenced here. Opening the screen is a server decision ([onPartActivate] runs the
 * full [resolve] before it asks the core to send the locator); the client only renders the models AE2 bakes
 * for it. The renderer is therefore AE2's static part renderer, not a dynamic one.
 */
class MEIntelligentTerminalPart(partItem: IPartItem<*>) : AbstractDisplayPart(partItem, true) {

    init {
        // Runs after the superclass constructor, so the main node exists. The reporting part constructor
        // already required a channel from the network and set an idle draw of 0.5 AE/t; the intelligent
        // terminal keeps the channel requirement and draws 1 AE/t while it is connected.
        mainNode.setIdlePowerUsage(TERMINAL_IDLE_POWER)
    }

    /**
     * The part's screen, in AE2's three reporting states: unpowered, powered without a channel, and active.
     * The state is picked by [AbstractDisplayPart]'s own selector, so it follows the client power and
     * channel state the host streams - the same freshness a native terminal has.
     */
    override fun getStaticModels(): IPartModel = selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL)

    /**
     * A right click that is not a memory card use (that is handled by the final `onActivate`) and not a
     * wrench rotation: validate the terminal and ask the core to send this client the terminal locator, so
     * the session list can be opened as a screen. No container menu is opened.
     *
     * A rejected use reports the safe reason on the action bar and never opens anything - the server is
     * authoritative, the client only renders. The interaction is consumed in every case, so the click never
     * falls through to whatever is behind the cable.
     */
    override fun onPartActivate(player: Player, hand: InteractionHand, pos: Vec3): Boolean {
        // The wrench belongs to the reporting part: rotating the screen inside its face wins.
        if (super.onPartActivate(player, hand, pos)) {
            return true
        }

        val serverPlayer = player as? ServerPlayer ?: return true
        val serverLevel = level as? ServerLevel ?: return true

        try {
            resolve(serverPlayer, allowRemote = false)
        } catch (rejected: IllegalArgumentException) {
            serverPlayer.displayClientMessage(Component.literal(rejected.message ?: DEFAULT_REJECTION), true)
            return true
        }

        // The call above has just proven the host is present, loaded, still ours and active, so the locator
        // names a terminal the client's later requests will be able to resolve again.
        val hostEntity = getBlockEntity() ?: return true
        SharedSessionNetwork.openTerminal(
            serverPlayer,
            SharedTerminalLocator(serverLevel.dimension().location(), hostEntity.blockPos, side).toJson(),
        )
        return true
    }

    /**
     * Resolves the grid this terminal is on, if [player] is allowed to use it right now.
     *
     * Enforced, in order:
     * - the host block entity is present, not removed and loaded, the player is in its dimension, and the
     *   host still holds *this* part on *this* side - a part that was removed or replaced after a request was
     *   sent is not a terminal any more, so a background turn cannot keep using a dead instance;
     * - the player may interact with the host position (`Level.mayInteract`, i.e. spawn protection and the
     *   like; AE2 15.x has no grid security service, so this is the complete vanilla permission check);
     * - with `allowRemote = false` the player is within [INTERACTION_RANGE] blocks of the host;
     * - the node is active on a booted network.
     *
     * `allowRemote = true` relaxes only the distance, and is used for work that continues while the screen is
     * closed - a background agent turn. The caller decides which of the two applies; this method never trusts
     * a position, player id or cell id from the network, because it only ever gets the real [ServerPlayer] the
     * core resolved from the connection.
     *
     * @throws IllegalArgumentException with a message safe to show [player] when the terminal is not usable.
     */
    fun resolve(player: ServerPlayer, allowRemote: Boolean): IGrid {
        val hostEntity = getBlockEntity() ?: throw IllegalArgumentException(ERROR_NOT_LOADED)
        val terminalLevel = hostEntity.level as? ServerLevel ?: throw IllegalArgumentException(ERROR_NOT_LOADED)
        if (hostEntity.isRemoved || !terminalLevel.isLoaded(hostEntity.blockPos)) {
            throw IllegalArgumentException(ERROR_NOT_LOADED)
        }
        if (player.serverLevel() !== terminalLevel) {
            throw IllegalArgumentException(ERROR_NOT_LOADED)
        }
        val partHost = getHost() ?: throw IllegalArgumentException(ERROR_NOT_LOADED)
        if (partHost.getPart(side) !== this) {
            throw IllegalArgumentException(ERROR_NOT_LOADED)
        }
        if (!terminalLevel.mayInteract(player, hostEntity.blockPos)) {
            throw IllegalArgumentException(ERROR_PERMISSION)
        }
        if (!allowRemote &&
            player.distanceToSqr(
                hostEntity.blockPos.x + 0.5,
                hostEntity.blockPos.y + 0.5,
                hostEntity.blockPos.z + 0.5,
            ) > INTERACTION_RANGE_SQ
        ) {
            throw IllegalArgumentException(ERROR_TOO_FAR)
        }

        val node = mainNode.node ?: throw IllegalArgumentException(ERROR_UNAVAILABLE)
        if (!node.isActive) {
            throw IllegalArgumentException(ERROR_UNAVAILABLE)
        }
        return node.grid ?: throw IllegalArgumentException(ERROR_UNAVAILABLE)
    }

    companion object {
        /** Passive AE/t draw of a connected terminal: one AE per tick, as the terminal drew before. */
        const val TERMINAL_IDLE_POWER: Double = 1.0

        /** Blocks a player must be within when the screen is opened ([net.minecraft.world.level.Level.mayInteract] aside). */
        const val INTERACTION_RANGE: Double = 8.0

        private const val INTERACTION_RANGE_SQ: Double = INTERACTION_RANGE * INTERACTION_RANGE

        /** Safe messages; the same convention the shared session RPC layer uses for `{"ok":false}` answers. */
        const val ERROR_NOT_LOADED: String = "That intelligent terminal is not loaded"
        const val ERROR_PERMISSION: String = "You are not allowed to use that intelligent terminal"
        const val ERROR_TOO_FAR: String = "That intelligent terminal is too far away"
        const val ERROR_UNAVAILABLE: String = "That intelligent terminal is not connected to an active ME network"

        private const val DEFAULT_REJECTION = "That intelligent terminal cannot be used right now"

        /** AE2's panel body and status light, shared with every reporting part. */
        private val MODEL_BASE: ResourceLocation = ae2("part/display_base")
        private val MODEL_STATUS_OFF: ResourceLocation = ae2("part/display_status_off")
        private val MODEL_STATUS_ON: ResourceLocation = ae2("part/display_status_on")
        private val MODEL_STATUS_HAS_CHANNEL: ResourceLocation = ae2("part/display_status_has_channel")

        /** The powered screen overlays AE2's body; unpowered uses its blank monitor face. */
        private val MODEL_SCREEN_ON: ResourceLocation = mine("part/me_intelligent_terminal_front_on")

        private val MODELS_OFF: IPartModel = PartModel(MODEL_BASE, MODEL_STATUS_OFF)
        private val MODELS_ON: IPartModel = PartModel(MODEL_BASE, MODEL_SCREEN_ON, MODEL_STATUS_ON)
        private val MODELS_HAS_CHANNEL: IPartModel = PartModel(MODEL_BASE, MODEL_SCREEN_ON, MODEL_STATUS_HAS_CHANNEL)

        /**
         * Hands this part's own models to AE2.
         *
         * Must run during mod construction: AE2 collects the registered part models into the client's model
         * event and then freezes the registry, so a call after that (common setup, for instance) fails. The
         * AE2 models the screens are layered on are registered by AE2 itself.
         *
         * Registering a location twice is harmless; the registry is a set.
         */
        @JvmStatic
        fun registerModels() {
            PartModels.registerModels(MODEL_SCREEN_ON)
        }

        private fun ae2(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath("ae2", path)

        private fun mine(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(AIFactory.MODID, path)
    }
}
