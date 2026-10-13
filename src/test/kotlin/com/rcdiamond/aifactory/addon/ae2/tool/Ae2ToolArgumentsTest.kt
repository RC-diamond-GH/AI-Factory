package com.rcdiamond.aifactory.addon.ae2.tool

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val FINGERPRINT = "a".repeat(Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)

private fun args(vararg pairs: Pair<String, JsonElement>) = JsonObject(linkedMapOf(*pairs))

/** Runs [block] and requires a safe validation rejection. */
private fun rejected(block: () -> Unit): IllegalArgumentException =
    assertFailsWith<IllegalArgumentException> { block() }

class Ae2ToolArgumentsTest {

    @Test
    fun `the filter is trimmed and bounded`() {
        assertEquals(
            "iron ingot",
            Ae2ToolArguments.readInventory(args(Ae2ToolArguments.FILTER to JsonPrimitive("  iron ingot  "))).filter,
        )

        val tooLong = "x".repeat(Ae2ToolLimits.MAX_FILTER_LENGTH + 1)
        val failure = rejected {
            Ae2ToolArguments.readInventory(args(Ae2ToolArguments.FILTER to JsonPrimitive(tooLong)))
        }
        assertFalse(
            failure.message.orEmpty().contains(tooLong),
            "rejections must not echo arbitrary model text",
        )
    }

    @Test
    fun `page and page size stay inside their bounds`() {
        assertEquals(3, Ae2ToolArguments.readInventory(args(Ae2ToolArguments.PAGE to JsonPrimitive(3))).page)
        assertEquals(1, Ae2ToolArguments.readInventory(args(Ae2ToolArguments.PAGE_SIZE to JsonPrimitive(1))).pageSize)
        assertEquals(
            Ae2ToolLimits.MAX_PAGE_SIZE,
            Ae2ToolArguments.readInventory(
                args(Ae2ToolArguments.PAGE_SIZE to JsonPrimitive(Ae2ToolLimits.MAX_PAGE_SIZE)),
            ).pageSize,
        )

        rejected { Ae2ToolArguments.readInventory(args(Ae2ToolArguments.PAGE to JsonPrimitive(-1))) }
        rejected { Ae2ToolArguments.readInventory(args(Ae2ToolArguments.PAGE to JsonPrimitive(Ae2ToolLimits.MAX_PAGE + 1))) }
        rejected { Ae2ToolArguments.readInventory(args(Ae2ToolArguments.PAGE_SIZE to JsonPrimitive(0))) }
        rejected { Ae2ToolArguments.readInventory(args(Ae2ToolArguments.PAGE_SIZE to JsonPrimitive(Ae2ToolLimits.MAX_PAGE_SIZE + 1))) }
    }


    @Test
    fun `item details rejects keys that are not fingerprints and malformed paths`() {
        for (key in listOf("", "abc", FINGERPRINT.uppercase(), FINGERPRINT.dropLast(1), FINGERPRINT + "0", "g".repeat(64))) {
            rejected {
                Ae2ToolArguments.itemDetails(args(Ae2ToolArguments.KEY to JsonPrimitive(key)))
            }
        }
        rejected {
            Ae2ToolArguments.itemDetails(
                args(Ae2ToolArguments.KEY to JsonPrimitive(FINGERPRINT), Ae2ToolArguments.PATH to JsonPrimitive("tag..x")),
            )
        }
        rejected {
            Ae2ToolArguments.itemDetails(
                args(
                    Ae2ToolArguments.KEY to JsonPrimitive(FINGERPRINT),
                    Ae2ToolArguments.DEPTH to JsonPrimitive(Ae2ToolLimits.MAX_NBT_DEPTH + 1),
                ),
            )
        }

        val path = "tag.Items[3].id"
        assertEquals(
            path,
            Ae2ToolArguments.itemDetails(
                args(Ae2ToolArguments.KEY to JsonPrimitive(FINGERPRINT), Ae2ToolArguments.PATH to JsonPrimitive(path)),
            ).path,
        )
    }

    @Test
    fun `crafting plan quantities stay inside their bounds`() {
        assertEquals(
            64L,
            Ae2ToolArguments.craftingPlan(
                args(Ae2ToolArguments.KEY to JsonPrimitive(FINGERPRINT), Ae2ToolArguments.AMOUNT to JsonPrimitive(64)),
            ).amount,
        )
        assertEquals(
            Ae2ToolLimits.MAX_AMOUNT,
            Ae2ToolArguments.craftingPlan(
                args(
                    Ae2ToolArguments.KEY to JsonPrimitive(FINGERPRINT),
                    Ae2ToolArguments.AMOUNT to JsonPrimitive(Ae2ToolLimits.MAX_AMOUNT),
                ),
            ).amount,
        )

        rejected {
            Ae2ToolArguments.craftingPlan(
                args(Ae2ToolArguments.KEY to JsonPrimitive(FINGERPRINT), Ae2ToolArguments.AMOUNT to JsonPrimitive(0)),
            )
        }
        rejected {
            Ae2ToolArguments.craftingPlan(
                args(
                    Ae2ToolArguments.KEY to JsonPrimitive(FINGERPRINT),
                    Ae2ToolArguments.AMOUNT to JsonPrimitive(Ae2ToolLimits.MAX_AMOUNT + 1),
                ),
            )
        }
        rejected {
            Ae2ToolArguments.craftingPlan(
                args(Ae2ToolArguments.KEY to JsonPrimitive(FINGERPRINT), Ae2ToolArguments.AMOUNT to JsonPrimitive(1.5)),
            )
        }

    }

    @Test
    fun `unknown parameters and wrong types are rejected on every operation`() {
        val withUnknown = args(Ae2ToolArguments.KEY to JsonPrimitive(FINGERPRINT), "nbt" to JsonPrimitive("x"))
        rejected { Ae2ToolArguments.readInventory(withUnknown) }
        rejected { Ae2ToolArguments.itemDetails(withUnknown) }
        rejected { Ae2ToolArguments.craftingPlan(withUnknown) }

        rejected { Ae2ToolArguments.readInventory(args(Ae2ToolArguments.PAGE to JsonPrimitive("2"))) }
        rejected { Ae2ToolArguments.readInventory(args(Ae2ToolArguments.FILTER to JsonPrimitive(7))) }
        rejected { Ae2ToolArguments.itemDetails(args(Ae2ToolArguments.KEY to JsonNull)) }
        rejected { Ae2ToolArguments.itemDetails(JsonObject(emptyMap())) }
        rejected { Ae2ToolArguments.craftingPlan(JsonObject(emptyMap())) }
    }

}
