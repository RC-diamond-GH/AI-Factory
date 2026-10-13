package com.rcdiamond.aifactory.shared

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation

/**
 * The address of one intelligent terminal: the dimension and position of its part host, plus the exact side
 * the terminal part is installed on.
 *
 * A terminal is an AE2 part, so a position alone is ambiguous — one cable block hosts up to six terminals
 * which are genuinely different terminals. Requests, leases and the wire format therefore all carry [side]
 * and never guess: a locator without a side is rejected rather than resolved to some default face.
 *
 * No AE2 type appears here, so the core shared package can address a terminal without depending on the addon.
 *
 * The JSON form is `{"dimension":<resource id>,"x":<int>,"y":<int>,"z":<int>,"side":"<face name>"}`, where the
 * face name is one of `down`, `up`, `north`, `south`, `west`, `east` — lowercase, the same six names
 * `Direction` serializes itself with. Unknown fields are ignored; the five named ones are all required.
 */
data class SharedTerminalLocator(
    /** Resource id of the dimension the part host is in. */
    val dimension: ResourceLocation,
    /** Position of the host block the part is mounted on. */
    val pos: BlockPos,
    /** The face of the host the terminal part occupies. */
    val side: Direction,
) {
    /** The wire form; [parse] reads exactly this shape. */
    fun toJson(): JsonObject = buildJsonObject {
        put("dimension", dimension.toString())
        put("x", pos.x)
        put("y", pos.y)
        put("z", pos.z)
        put("side", side.serializedName)
    }

    companion object {
        /**
         * Reads the wire form, rejecting anything that is not a complete locator.
         *
         * The side is mandatory and must be one of the six lowercase face names: a missing or misspelled side
         * never falls back to a default face, because that would silently address the wrong terminal.
         *
         * @throws IllegalArgumentException with a message that is safe to show a player.
         */
        fun parse(locator: JsonObject): SharedTerminalLocator {
            val dimension = ResourceLocation.tryParse(locator.requireString("dimension"))
                ?: throw IllegalArgumentException("Field 'dimension' is not a valid resource location")
            val pos = BlockPos(locator.requireInt("x"), locator.requireInt("y"), locator.requireInt("z"))
            val side = sideByName(locator.requireString("side"))
                ?: throw IllegalArgumentException("Field 'side' must be one of $SIDE_NAMES")
            return SharedTerminalLocator(dimension, pos, side)
        }

        /** The accepted side names, spelled out for the rejection message. */
        private const val SIDE_NAMES = "down, up, north, south, west, east"
    }
}

/** The face [name] denotes, or `null` when it is not one of the six lowercase names. */
private fun sideByName(name: String): Direction? = when (name) {
    "down" -> Direction.DOWN
    "up" -> Direction.UP
    "north" -> Direction.NORTH
    "south" -> Direction.SOUTH
    "west" -> Direction.WEST
    "east" -> Direction.EAST
    else -> null
}
