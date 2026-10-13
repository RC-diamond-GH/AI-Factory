package com.rcdiamond.aifactory.shared

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Exercises the production locator: the wire form a client sends back must describe exactly one terminal,
 * including its face. The tests are pure JVM — no game bootstrap is involved.
 */
class SharedTerminalLocatorTest {

    private val overworld = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld")
    private val pos = BlockPos(12, 64, -30)

    private fun locatorJson(
        dimension: JsonElement = JsonPrimitive("minecraft:overworld"),
        x: JsonElement = JsonPrimitive(12),
        y: JsonElement = JsonPrimitive(64),
        z: JsonElement = JsonPrimitive(-30),
        side: JsonElement = JsonPrimitive("north"),
    ): JsonObject = buildJsonObject {
        put("dimension", dimension)
        put("x", x)
        put("y", y)
        put("z", z)
        put("side", side)
    }

    @Test
    fun `every face round-trips through the wire form`() {
        val faces = listOf(
            Direction.DOWN to "down",
            Direction.UP to "up",
            Direction.NORTH to "north",
            Direction.SOUTH to "south",
            Direction.WEST to "west",
            Direction.EAST to "east",
        )

        for ((side, name) in faces) {
            val locator = SharedTerminalLocator(overworld, pos, side)

            val json = locator.toJson()

            assertEquals(name, (json["side"] as JsonPrimitive).content)
            assertEquals(locator, SharedTerminalLocator.parse(json))
        }
    }

    @Test
    fun `a locator without a side is rejected instead of defaulting to a face`() {
        val withoutSide = JsonObject(locatorJson() - "side")

        assertFailsWith<IllegalArgumentException> { SharedTerminalLocator.parse(withoutSide) }
    }

    @Test
    fun `a side that is unknown, wrongly cased or not a string is rejected`() {
        for (bad in listOf<JsonElement>(JsonPrimitive("NORTH"), JsonPrimitive("northwest"), JsonPrimitive(""), JsonPrimitive(3))) {
            assertFailsWith<IllegalArgumentException>("side $bad") {
                SharedTerminalLocator.parse(locatorJson(side = bad))
            }
        }
    }

    @Test
    fun `a dimension that is absent, not a string or not a resource location is rejected`() {
        val absent = JsonObject(locatorJson() - "dimension")
        assertFailsWith<IllegalArgumentException> { SharedTerminalLocator.parse(absent) }
        assertFailsWith<IllegalArgumentException> { SharedTerminalLocator.parse(locatorJson(dimension = JsonPrimitive(7))) }
        assertFailsWith<IllegalArgumentException> {
            SharedTerminalLocator.parse(locatorJson(dimension = JsonPrimitive("not a dimension!")))
        }
    }

    @Test
    fun `coordinates that are absent, fractional or not integers are rejected`() {
        val absent = JsonObject(locatorJson() - "y")
        assertFailsWith<IllegalArgumentException> { SharedTerminalLocator.parse(absent) }
        assertFailsWith<IllegalArgumentException> { SharedTerminalLocator.parse(locatorJson(x = JsonPrimitive("twelve"))) }
        assertFailsWith<IllegalArgumentException> { SharedTerminalLocator.parse(locatorJson(z = JsonPrimitive(1.5))) }
        assertFailsWith<IllegalArgumentException> { SharedTerminalLocator.parse(locatorJson(y = JsonPrimitive(true))) }
    }
}
