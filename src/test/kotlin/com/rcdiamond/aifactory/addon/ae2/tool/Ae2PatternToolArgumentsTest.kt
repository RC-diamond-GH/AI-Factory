package com.rcdiamond.aifactory.addon.ae2.tool

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val FINGERPRINT = "a".repeat(Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)
private val OTHER_FINGERPRINT = "b".repeat(Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)

private fun args(vararg pairs: Pair<String, JsonElement>) = JsonObject(linkedMapOf(*pairs))

private fun view(vararg pairs: Pair<String, JsonElement>) = Ae2PatternToolArguments.parse(
    args(Ae2PatternToolArguments.ACTION to JsonPrimitive(Ae2PatternToolArguments.ACTION_VIEW), *pairs),
)

private fun install(
    vararg pairs: Pair<String, JsonElement>,
): Ae2PatternToolArguments.EncodeInstall = Ae2PatternToolArguments.parse(
    args(
        Ae2PatternToolArguments.ACTION to JsonPrimitive(Ae2PatternToolArguments.ACTION_ENCODE_INSTALL),
        *pairs,
    ),
) as Ae2PatternToolArguments.EncodeInstall

private fun fingerprint(value: String) = JsonPrimitive(value)

private fun item(id: String) = buildJsonObject { put(Ae2PatternToolArguments.ITEM, id) }

private fun entry(slot: Int? = null, key: JsonElement = fingerprint(FINGERPRINT), amount: Long? = null) =
    buildJsonObject {
        if (slot != null) put(Ae2PatternToolArguments.SLOT, slot)
        put(Ae2PatternToolArguments.KEY, key)
        if (amount != null) put(Ae2PatternToolArguments.AMOUNT, amount)
    }

/** Runs [block] and requires a safe validation rejection. */
private fun rejected(block: () -> Any?): IllegalArgumentException =
    assertFailsWith<IllegalArgumentException> { block() }

/** Re-parsing the resolved JSON must be a no-op, which is what keeps client and server validation identical. */
private fun assertRoundTrips(request: Ae2PatternToolArguments.Request) {
    val resolved = request.toJson()
    assertEquals(resolved, Ae2PatternToolArguments.parse(resolved).toJson())
}

class Ae2PatternToolArgumentsTest {

    @Test
    fun `the action decides the parameter space`() {
        assertTrue(view() is Ae2PatternToolArguments.View)
        rejected { Ae2PatternToolArguments.parse(args()) }
        rejected { Ae2PatternToolArguments.parse(args(Ae2PatternToolArguments.ACTION to JsonPrimitive("edit"))) }
        rejected { Ae2PatternToolArguments.parse(args(Ae2PatternToolArguments.ACTION to JsonPrimitive(3))) }

        // A view parameter on an install (and the other way round) is rejected as unknown.
        rejected {
            install(
                Ae2PatternToolArguments.CONTAINER to JsonPrimitive("1,2,3/block"),
                Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:overworld"),
                Ae2PatternToolArguments.SLOT to JsonPrimitive(0),
                Ae2PatternToolArguments.EXPECTED to JsonPrimitive(Ae2PatternToolArguments.EXPECTED_EMPTY),
                Ae2PatternToolArguments.MODE to JsonPrimitive(Ae2PatternToolArguments.MODE_CRAFTING),
                Ae2PatternToolArguments.GRID to buildJsonArray { add(fingerprint(FINGERPRINT)) },
                Ae2PatternToolArguments.FILTER to JsonPrimitive("iron"),
            )
        }
        rejected { view(Ae2PatternToolArguments.SLOT to JsonPrimitive(0)) }
    }

    @Test
    fun `the view enforces pagination and address bounds`() {

        val bounded = view(
            Ae2PatternToolArguments.FILTER to JsonPrimitive("  iron ingot  "),
            Ae2PatternToolArguments.PAGE to JsonPrimitive(Ae2ToolLimits.MAX_PAGE),
            Ae2PatternToolArguments.PAGE_SIZE to JsonPrimitive(Ae2ToolLimits.MAX_PAGE_SIZE),
            Ae2PatternToolArguments.SHOW to JsonPrimitive(Ae2PatternToolArguments.SHOW_ALL),
            Ae2PatternToolArguments.CONTAINER to JsonPrimitive("12,64,-3/up"),
        ) as Ae2PatternToolArguments.View
        assertEquals("iron ingot", bounded.filter)
        assertEquals(Ae2ToolLimits.MAX_PAGE, bounded.page)
        assertEquals(Ae2ToolLimits.MAX_PAGE_SIZE, bounded.pageSize)
        assertEquals("12,64,-3/up", bounded.container?.toId())
        assertRoundTrips(bounded)

        rejected { view(Ae2PatternToolArguments.SHOW to JsonPrimitive("everything")) }
        rejected { view(Ae2PatternToolArguments.PAGE to JsonPrimitive(-1)) }
        rejected { view(Ae2PatternToolArguments.PAGE_SIZE to JsonPrimitive(0)) }
        rejected { view(Ae2PatternToolArguments.PAGE_SIZE to JsonPrimitive(Ae2ToolLimits.MAX_PAGE_SIZE + 1)) }
        rejected { view(Ae2PatternToolArguments.CONTAINER to JsonPrimitive("north")) }
        rejected { view(Ae2PatternToolArguments.CONTAINER to JsonPrimitive("1,2,3/blocked")) }
        rejected { view(Ae2PatternToolArguments.CONTAINER to JsonPrimitive("99999999,2,3/block")) }
        rejected { view(Ae2PatternToolArguments.FILTER to JsonPrimitive(7)) }
        rejected { view(Ae2PatternToolArguments.PAGE to JsonPrimitive("2")) }

        // A rejection must not echo arbitrary model text.
        val tooLong = "x".repeat(Ae2ToolLimits.MAX_FILTER_LENGTH + 1)
        val failure = rejected { view(Ae2PatternToolArguments.FILTER to JsonPrimitive(tooLong)) }
        assertFalse(failure.message.orEmpty().contains(tooLong))
    }

    @Test
    fun `an install demands a container, a slot, an expectation and a mode`() {
        val base = mapOf(
            Ae2PatternToolArguments.CONTAINER to JsonPrimitive("1,2,3/block"),
            Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:overworld"),
            Ae2PatternToolArguments.SLOT to JsonPrimitive(4),
            Ae2PatternToolArguments.EXPECTED to JsonPrimitive(Ae2PatternToolArguments.EXPECTED_EMPTY),
            Ae2PatternToolArguments.MODE to JsonPrimitive(Ae2PatternToolArguments.MODE_PROCESSING),
            Ae2PatternToolArguments.INPUTS to buildJsonArray { add(entry(key = item("minecraft:water"))) },
            Ae2PatternToolArguments.OUTPUTS to buildJsonArray { add(entry(slot = 0, key = item("minecraft:stone"))) },
        )
        val resolved = install(*base.entries.map { it.key to it.value }.toTypedArray()).install
        assertEquals(Ae2PatternToolArguments.ContainerAddress(1, 2, 3, "block"), resolved.container)
        assertEquals(4, resolved.slot)
        assertEquals(Ae2PatternToolArguments.EXPECTED_EMPTY, resolved.expected)

        for (missing in base.keys) {
            rejected {
                install(*base.filterKeys { it != missing }.entries.map { it.key to it.value }.toTypedArray())
            }
        }

        rejected {
            install(
                *base.plus(Ae2PatternToolArguments.EXPECTED to JsonPrimitive("empty ")).entries
                    .map { it.key to it.value }.toTypedArray(),
            )
        }
        rejected {
            install(
                *base.plus(Ae2PatternToolArguments.EXPECTED to JsonPrimitive(FINGERPRINT.uppercase())).entries
                    .map { it.key to it.value }.toTypedArray(),
            )
        }
        rejected {
            install(
                *base.plus(Ae2PatternToolArguments.SLOT to JsonPrimitive(Ae2PatternToolArguments.MAX_TARGET_SLOT + 1))
                    .entries.map { it.key to it.value }.toTypedArray(),
            )
        }
        rejected {
            install(
                *base.plus(Ae2PatternToolArguments.MODE to JsonPrimitive("assembling"))
                    .entries.map { it.key to it.value }.toTypedArray(),
            )
        }
    }

    @Test
    fun `every mode rejects the parameters of the other modes`() {
        // Processing must not accept the crafting grid, a recipe or the substitution flags.
        for (name in listOf(
            Ae2PatternToolArguments.GRID,
            Ae2PatternToolArguments.RECIPE,
            Ae2PatternToolArguments.SUBSTITUTE,
            Ae2PatternToolArguments.FLUID_SUBSTITUTE,
            Ae2PatternToolArguments.INPUT,
            Ae2PatternToolArguments.OUTPUT,
        )) {
            rejected { processing(name to JsonPrimitive(1)) }
        }
        // Crafting must not accept the processing entries.
        rejected { crafting(Ae2PatternToolArguments.INPUTS to JsonArray(emptyList())) }
        rejected { crafting(Ae2PatternToolArguments.OUTPUTS to JsonArray(emptyList())) }
        // Stonecutting must not accept a processing entry or a smithing role.
        rejected { stonecutting(Ae2PatternToolArguments.TEMPLATE to item("minecraft:netherite_upgrade_smithing_template")) }
        // Processing must not accept the smithing roles.
        rejected { processing(Ae2PatternToolArguments.BASE to item("minecraft:iron_sword")) }
    }

    private fun processing(vararg extra: Pair<String, JsonElement>) = install(
        Ae2PatternToolArguments.CONTAINER to JsonPrimitive("1,2,3/block"),
        Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:overworld"),
        Ae2PatternToolArguments.SLOT to JsonPrimitive(0),
        Ae2PatternToolArguments.EXPECTED to JsonPrimitive(Ae2PatternToolArguments.EXPECTED_EMPTY),
        Ae2PatternToolArguments.MODE to JsonPrimitive(Ae2PatternToolArguments.MODE_PROCESSING),
        Ae2PatternToolArguments.INPUTS to buildJsonArray { add(entry()) },
        Ae2PatternToolArguments.OUTPUTS to buildJsonArray { add(entry(slot = 0)) },
        *extra,
    )

    private fun crafting(vararg extra: Pair<String, JsonElement>) = install(
        Ae2PatternToolArguments.CONTAINER to JsonPrimitive("1,2,3/block"),
        Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:overworld"),
        Ae2PatternToolArguments.SLOT to JsonPrimitive(0),
        Ae2PatternToolArguments.EXPECTED to JsonPrimitive(Ae2PatternToolArguments.EXPECTED_EMPTY),
        Ae2PatternToolArguments.MODE to JsonPrimitive(Ae2PatternToolArguments.MODE_CRAFTING),
        Ae2PatternToolArguments.GRID to buildJsonArray { add(fingerprint(FINGERPRINT)) },
        *extra,
    )

    private fun stonecutting(vararg extra: Pair<String, JsonElement>) = install(
        Ae2PatternToolArguments.CONTAINER to JsonPrimitive("1,2,3/block"),
        Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:overworld"),
        Ae2PatternToolArguments.SLOT to JsonPrimitive(0),
        Ae2PatternToolArguments.EXPECTED to JsonPrimitive(FINGERPRINT),
        Ae2PatternToolArguments.MODE to JsonPrimitive(Ae2PatternToolArguments.MODE_STONECUTTING),
        Ae2PatternToolArguments.INPUT to fingerprint(FINGERPRINT),
        *extra,
    )

    @Test
    fun `a crafting grid is bounded, sparse and needs at least one item`() {
        val grid = buildJsonArray {
            add(JsonNull)
            add(fingerprint(FINGERPRINT))
            add(item("minecraft:oak_planks"))
        }
        val resolved = crafting(Ae2PatternToolArguments.GRID to grid).install.payload
        assertTrue(resolved is Ae2PatternToolArguments.Crafting)
        assertEquals(3, resolved.grid.size)
        assertNull(resolved.grid[0])
        assertEquals(Ae2PatternToolArguments.KeyRef.Fingerprint(FINGERPRINT), resolved.grid[1])
        assertEquals(Ae2PatternToolArguments.KeyRef.RegistryId(false, "minecraft:oak_planks"), resolved.grid[2])
        assertRoundTrips(crafting(Ae2PatternToolArguments.GRID to grid))

        rejected { crafting(Ae2PatternToolArguments.GRID to JsonArray(emptyList())) }
        rejected { crafting(Ae2PatternToolArguments.GRID to buildJsonArray { add(JsonNull) }) }
        rejected {
            crafting(
                Ae2PatternToolArguments.GRID to buildJsonArray {
                    repeat(Ae2PatternToolArguments.MAX_CRAFTING_SLOTS + 1) { add(fingerprint(FINGERPRINT)) }
                },
            )
        }
        rejected { crafting(Ae2PatternToolArguments.GRID to JsonPrimitive(FINGERPRINT)) }
    }

    @Test
    fun `a processing pattern keeps its sparse slots and needs a primary output`() {
        val inputs = buildJsonArray {
            add(entry(slot = 3, key = item("minecraft:water"), amount = 1000))
            add(entry(key = fingerprint(OTHER_FINGERPRINT), amount = 2))
        }
        val outputs = buildJsonArray { add(entry(slot = 0, key = item("minecraft:stone"))) }
        val resolved = processing(
            Ae2PatternToolArguments.INPUTS to inputs,
            Ae2PatternToolArguments.OUTPUTS to outputs,
        ).install.payload
        assertTrue(resolved is Ae2PatternToolArguments.Processing)
        assertEquals(listOf(3, 1), resolved.inputs.map { it.slot })
        assertEquals(1000L, resolved.inputs[0].amount)
        assertEquals(2L, resolved.inputs[1].amount)
        assertEquals(1L, resolved.outputs[0].amount)
        assertRoundTrips(processing(Ae2PatternToolArguments.INPUTS to inputs, Ae2PatternToolArguments.OUTPUTS to outputs))

        rejected { processing(Ae2PatternToolArguments.INPUTS to JsonArray(emptyList())) }
        rejected { processing(Ae2PatternToolArguments.OUTPUTS to JsonArray(emptyList())) }
        // The primary output must be in slot 0, a slot may not repeat and the bounds are enforced.
        rejected {
            processing(Ae2PatternToolArguments.OUTPUTS to buildJsonArray { add(entry(slot = 1)) })
        }
        rejected {
            processing(Ae2PatternToolArguments.INPUTS to buildJsonArray { add(entry(slot = 2)); add(entry(slot = 2)) })
        }
        rejected {
            processing(
                Ae2PatternToolArguments.INPUTS to buildJsonArray {
                    add(entry(slot = Ae2PatternToolArguments.MAX_PROCESSING_INPUT_SLOT + 1))
                },
            )
        }
        rejected {
            processing(
                Ae2PatternToolArguments.INPUTS to buildJsonArray {
                    add(entry(amount = Ae2ToolLimits.MAX_AMOUNT + 1))
                },
            )
        }
        rejected { processing(Ae2PatternToolArguments.INPUTS to buildJsonArray { add(entry(amount = 0)) }) }
        rejected { processing(Ae2PatternToolArguments.INPUTS to buildJsonArray { add(entry(key = JsonNull)) }) }
    }

    @Test
    fun `stonecutting and smithing require their roles and accept an optional output`() {
        val cut = stonecutting(Ae2PatternToolArguments.OUTPUT to item("minecraft:stone_slab")).install.payload
        assertTrue(cut is Ae2PatternToolArguments.Stonecutting)
        assertEquals(Ae2PatternToolArguments.KeyRef.RegistryId(false, "minecraft:stone_slab"), cut.output)
        assertRoundTrips(stonecutting(Ae2PatternToolArguments.OUTPUT to item("minecraft:stone_slab")))

        // An explicit JSON null is the same as leaving the optional output out.
        val cutWithout = install(
            Ae2PatternToolArguments.CONTAINER to JsonPrimitive("1,2,3/block"),
            Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:overworld"),
            Ae2PatternToolArguments.SLOT to JsonPrimitive(0),
            Ae2PatternToolArguments.EXPECTED to JsonPrimitive(FINGERPRINT),
            Ae2PatternToolArguments.MODE to JsonPrimitive(Ae2PatternToolArguments.MODE_STONECUTTING),
            Ae2PatternToolArguments.INPUT to fingerprint(FINGERPRINT),
            Ae2PatternToolArguments.OUTPUT to JsonNull,
        ).install.payload
        assertTrue(cutWithout is Ae2PatternToolArguments.Stonecutting)
        assertNull(cutWithout.output)

        val smithing = install(
            Ae2PatternToolArguments.CONTAINER to JsonPrimitive("1,2,3/block"),
            Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:overworld"),
            Ae2PatternToolArguments.SLOT to JsonPrimitive(0),
            Ae2PatternToolArguments.EXPECTED to JsonPrimitive(FINGERPRINT),
            Ae2PatternToolArguments.MODE to JsonPrimitive(Ae2PatternToolArguments.MODE_SMITHING),
            Ae2PatternToolArguments.TEMPLATE to item("minecraft:netherite_upgrade_smithing_template"),
            Ae2PatternToolArguments.BASE to item("minecraft:diamond_sword"),
            Ae2PatternToolArguments.ADDITION to item("minecraft:netherite_ingot"),
            Ae2PatternToolArguments.RECIPE to JsonPrimitive("minecraft:netherite_sword_smithing"),
        )
        val payload = smithing.install.payload
        assertTrue(payload is Ae2PatternToolArguments.Smithing)
        assertEquals("minecraft:netherite_ingot", (payload.addition as Ae2PatternToolArguments.KeyRef.RegistryId).id)
        assertRoundTrips(smithing)

        rejected { stonecutting(Ae2PatternToolArguments.INPUT to JsonPrimitive("minecraft:stone")) }
        rejected { stonecutting(Ae2PatternToolArguments.RECIPE to JsonPrimitive("Stone")) }
        rejected {
            install(
                Ae2PatternToolArguments.CONTAINER to JsonPrimitive("1,2,3/block"),
                Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:overworld"),
                Ae2PatternToolArguments.SLOT to JsonPrimitive(0),
                Ae2PatternToolArguments.EXPECTED to JsonPrimitive(FINGERPRINT),
                Ae2PatternToolArguments.MODE to JsonPrimitive(Ae2PatternToolArguments.MODE_SMITHING),
                Ae2PatternToolArguments.TEMPLATE to item("minecraft:netherite_upgrade_smithing_template"),
                Ae2PatternToolArguments.BASE to item("minecraft:diamond_sword"),
            )
        }
    }

    @Test
    fun `a key is a fingerprint or a plain registry entry, never arbitrary data`() {
        for (invalid in listOf(
            JsonPrimitive("minecraft:stone"),
            JsonPrimitive("a".repeat(Ae2ToolLimits.KEY_FINGERPRINT_LENGTH - 1)),
            JsonPrimitive(FINGERPRINT.uppercase()),
            JsonPrimitive("g".repeat(Ae2ToolLimits.KEY_FINGERPRINT_LENGTH)),
            JsonPrimitive(7),
            buildJsonObject { put(Ae2PatternToolArguments.ITEM, "minecraft:stone"); put(Ae2PatternToolArguments.FLUID, "minecraft:water") },
            buildJsonObject { put("nbt", "{CustomModelData:1}") },
            buildJsonObject { put(Ae2PatternToolArguments.ITEM, "Stone") },
            buildJsonObject { put(Ae2PatternToolArguments.ITEM, "minecraft:") },
            buildJsonObject { put(Ae2PatternToolArguments.ITEM, "minecraft:Stone") },
            buildJsonArray { add(fingerprint(FINGERPRINT)) },
        )) {
            rejected { stonecutting(Ae2PatternToolArguments.INPUT to invalid) }
        }

        val fluid = stonecutting(
            Ae2PatternToolArguments.INPUT to buildJsonObject {
                put(Ae2PatternToolArguments.FLUID, "minecraft:water")
            },
        ).install.payload
        assertEquals(
            Ae2PatternToolArguments.KeyRef.RegistryId(true, "minecraft:water"),
            (fluid as Ae2PatternToolArguments.Stonecutting).input,
        )
    }

    @Test
    fun `install identifies the dimension separately from coincident container coordinates`() {
        val overworld = processing().install
        val nether = processing(
            Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:the_nether"),
        ).install
        assertEquals(overworld.container, nether.container)
        assertEquals("minecraft:overworld", overworld.dimension)
        assertEquals("minecraft:the_nether", nether.dimension)
        rejected { processing(Ae2PatternToolArguments.DIMENSION to JsonPrimitive("bad dimension")) }
        rejected { processing(Ae2PatternToolArguments.DIMENSION to JsonPrimitive(1)) }
        assertRoundTrips(processing(Ae2PatternToolArguments.DIMENSION to JsonPrimitive("minecraft:the_nether")))
    }

    @Test
    fun `substitution flags accept booleans and reject strings`() {

        val set = crafting(
            Ae2PatternToolArguments.GRID to buildJsonArray { add(fingerprint(FINGERPRINT)) },
            Ae2PatternToolArguments.SUBSTITUTE to JsonPrimitive(true),
            Ae2PatternToolArguments.FLUID_SUBSTITUTE to JsonPrimitive(false),
        ).install.payload as Ae2PatternToolArguments.Crafting
        assertEquals(true, set.substitute)
        assertEquals(false, set.fluidSubstitute)

        rejected {
            crafting(
                Ae2PatternToolArguments.GRID to buildJsonArray { add(fingerprint(FINGERPRINT)) },
                Ae2PatternToolArguments.SUBSTITUTE to JsonPrimitive("yes"),
            )
        }
    }
}
