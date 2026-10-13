package com.rcdiamond.aifactory.addon.ae2.tool

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun text(value: String) = Ae2Tag.Text(value)

private fun int(value: Int) = Ae2Tag.Signed(Ae2TagType.INT, value.toLong())

private fun compound(vararg members: Pair<String, Ae2Tag>) = Ae2Tag.Compound(linkedMapOf(*members))

private fun countValues(element: JsonElement): Int = when (element) {
    is JsonObject -> element.values.sumOf { countValues(it) }
    is JsonArray -> element.sumOf { countValues(it) }
    is JsonPrimitive -> 1
}

class Ae2NbtViewTest {
    @Test
    fun `NBT variants and key types have distinct identities`() {
        val first = compound("id" to text("minecraft:iron_ingot"), "tag" to compound("Damage" to int(3)))
        val second = compound("id" to text("minecraft:iron_ingot"), "tag" to compound("Damage" to int(4)))
        assertNotEquals(Ae2NbtView.fingerprint("ae2:i", first), Ae2NbtView.fingerprint("ae2:i", second))
        assertNotEquals(Ae2NbtView.fingerprint("ae2:i", first), Ae2NbtView.fingerprint("ae2:f", first))
    }

    @Test
    fun `fingerprint length and alphabet match what the tool accepts`() {
        val fingerprint = Ae2NbtView.fingerprint("ae2:i", compound("id" to text("minecraft:stone")))
        assertEquals(Ae2ToolLimits.KEY_FINGERPRINT_LENGTH, fingerprint.length)
        assertTrue(Ae2ToolLimits.isItemKeyFingerprint(fingerprint))
    }

    @Test
    fun `canonical bytes do not depend on member order`() {
        val first = compound("a" to int(1), "b" to int(2))
        val reordered = Ae2Tag.Compound(linkedMapOf("b" to int(2), "a" to int(1)))
        assertEquals(
            Ae2NbtView.canonicalBytes(first).toList(),
            Ae2NbtView.canonicalBytes(reordered).toList(),
        )
        assertEquals(
            Ae2NbtView.fingerprint("ae2:i", first),
            Ae2NbtView.fingerprint("ae2:i", reordered),
        )
    }

    @Test
    fun `an intact container renders as plain JSON`() {
        val rendered = Ae2NbtView.render(compound("a" to text("x"), "b" to int(2)), depth = 3, page = 0, pageSize = 20)
        assertEquals("compound", rendered.kind)
        val value = assertIs<JsonObject>(rendered.value)
        assertEquals(setOf("a", "b"), value.keys)
        assertEquals("x", value.getValue("a").jsonPrimitive.content)
        assertFalse(rendered.truncated)
        assertEquals(2, rendered.childTotal)
        assertEquals(2, rendered.childShown)
    }

    @Test
    fun `the selected node is paged and marked when not everything fits`() {
        val root = compound("a" to text("1"), "b" to text("2"), "c" to text("3"))

        val first = Ae2NbtView.render(root, depth = 3, page = 0, pageSize = 2)
        val firstValue = assertIs<JsonObject>(first.value)
        assertEquals("compound", firstValue.getValue("_type").jsonPrimitive.content)
        assertEquals(2, firstValue.getValue("_shown").jsonPrimitive.int)
        assertEquals(3, firstValue.getValue("_total").jsonPrimitive.int)
        assertEquals(setOf("a", "b"), assertIs<JsonObject>(firstValue.getValue("_entries")).keys)
        assertTrue(first.truncated)

        val second = Ae2NbtView.render(root, depth = 3, page = 1, pageSize = 2)
        val secondValue = assertIs<JsonObject>(second.value)
        assertEquals(setOf("c"), assertIs<JsonObject>(secondValue.getValue("_entries")).keys)
        assertEquals(1, secondValue.getValue("_shown").jsonPrimitive.int)
        // Page 1 of 3 members still omits the previous page, which is what `truncated` reports.
        assertTrue(second.truncated)
    }

    @Test
    fun `depth zero reports only the container shape`() {
        val rendered = Ae2NbtView.render(compound("a" to text("x"), "b" to int(2)), depth = 0, page = 0, pageSize = 20)
        val value = assertIs<JsonObject>(rendered.value)
        assertEquals("compound", value.getValue("_type").jsonPrimitive.content)
        assertEquals(2, value.getValue("_size").jsonPrimitive.int)
        assertTrue(rendered.truncated)
        assertNull(value["a"])
    }

    @Test
    fun `a wide tree stays inside the node budget`() {
        val children = LinkedHashMap<String, Ae2Tag>()
        repeat(100) { index ->
            val members = LinkedHashMap<String, Ae2Tag>()
            repeat(8) { member -> members["m$member"] = text("value") }
            children["c%03d".format(index)] = Ae2Tag.Compound(members)
        }
        val rendered = Ae2NbtView.render(Ae2Tag.Compound(children), depth = 6, page = 0, pageSize = 100)
        assertTrue(rendered.truncated, "a tree far larger than the node budget must report truncation")
        // 100 children x 8 members would emit 801 values; the budget stops well before that.
        assertTrue(countValues(rendered.value) < 801)
        assertEquals(100, rendered.childTotal)
    }

    @Test
    fun `strings and arrays are shortened instead of being sent whole`() {
        val long = "x".repeat(1000)
        val rendered = Ae2NbtView.render(
            compound(
                "text" to text(long),
                "array" to Ae2Tag.Ints(IntArray(100) { it }),
            ),
            depth = 3,
            page = 0,
            pageSize = 20,
        )
        val value = assertIs<JsonObject>(rendered.value)

        val shortened = assertIs<JsonObject>(value.getValue("text"))
        assertEquals(1000, shortened.getValue("_length").jsonPrimitive.int)
        assertEquals(Ae2ToolLimits.MAX_RENDERED_STRING_LENGTH, shortened.getValue("_value").jsonPrimitive.content.length)

        val array = assertIs<JsonObject>(value.getValue("array"))
        assertEquals(100, array.getValue("_length").jsonPrimitive.int)
        assertEquals(Ae2ToolLimits.MAX_ARRAY_PREVIEW, assertIs<JsonArray>(array.getValue("_values")).size)

        assertTrue(rendered.truncated)
    }

    @Test
    fun `lists keep their own shape`() {
        val rendered = Ae2NbtView.render(
            Ae2Tag.Sequence(listOf(text("a"), text("b")), Ae2TagType.STRING),
            depth = 3,
            page = 0,
            pageSize = 20,
        )
        assertEquals("list", rendered.kind)
        assertEquals(2, assertIs<JsonArray>(rendered.value).size)
    }

    @Test
    fun `paths address members elements and escaped dots`() {
        assertEquals(emptyList(), Ae2NbtView.parsePath(""))
        assertEquals(
            listOf(Ae2NbtView.Step.Member("tag"), Ae2NbtView.Step.Member("display"), Ae2NbtView.Step.Member("Name")),
            Ae2NbtView.parsePath("tag.display.Name"),
        )
        assertEquals(
            listOf(Ae2NbtView.Step.Member("tag"), Ae2NbtView.Step.Member("Items"), Ae2NbtView.Step.Element(2), Ae2NbtView.Step.Member("id")),
            Ae2NbtView.parsePath("tag.Items[2].id"),
        )
        assertEquals(listOf(Ae2NbtView.Step.Element(0)), Ae2NbtView.parsePath("[0]"))
        assertEquals(listOf(Ae2NbtView.Step.Member("a.b")), Ae2NbtView.parsePath("a\\.b"))
        assertEquals(listOf(Ae2NbtView.Step.Member("a\\b")), Ae2NbtView.parsePath("a\\\\b"))
    }

    @Test
    fun `malformed paths are rejected without echoing the model text`() {
        for (path in listOf("a.", "a..b", "a[", "a[]", "a[x]", "a]", "a[1", "[", ".a", "\\")) {
            assertFailsWith<IllegalArgumentException>("path '$path' must be rejected") {
                Ae2NbtView.parsePath(path)
            }
        }

        val tooManySteps = List(Ae2ToolLimits.MAX_PATH_STEPS + 1) { "member" }.joinToString(".")
        assertFailsWith<IllegalArgumentException> { Ae2NbtView.parsePath(tooManySteps) }

        val longSegment = "s".repeat(Ae2ToolLimits.MAX_PATH_SEGMENT_LENGTH + 1)
        val failure = assertFailsWith<IllegalArgumentException> { Ae2NbtView.parsePath(longSegment) }
        assertFalse(
            failure.message.orEmpty().contains(longSegment),
            "rejections must name the position, not echo the model's text",
        )
    }

    @Test
    fun `navigation reports where a path left the tree`() {
        val root = compound("tag" to compound("display" to text("Name")))

        val found = Ae2NbtView.navigate(root, Ae2NbtView.parsePath("tag.display"))
        assertEquals(text("Name"), assertIs<Ae2NbtView.Navigation.Found>(found).node)

        val missing = assertIs<Ae2NbtView.Navigation.Missing>(
            Ae2NbtView.navigate(root, Ae2NbtView.parsePath("tag.nope.deeper")),
        )
        assertEquals(1, missing.failedAtStep)
        assertEquals(3, missing.stepCount)

        val wrongKind = assertIs<Ae2NbtView.Navigation.Missing>(
            Ae2NbtView.navigate(root, Ae2NbtView.parsePath("tag.display[0]")),
        )
        assertEquals(2, wrongKind.failedAtStep)
    }

    @Test
    fun `signed values keep a plain json shape`() {
        val rendered = Ae2NbtView.render(Ae2Tag.Signed(Ae2TagType.LONG, 42L), depth = 3, page = 0, pageSize = 20)
        assertEquals("long", rendered.kind)
        assertEquals(42L, assertIs<JsonPrimitive>(rendered.value).long)
    }
}
