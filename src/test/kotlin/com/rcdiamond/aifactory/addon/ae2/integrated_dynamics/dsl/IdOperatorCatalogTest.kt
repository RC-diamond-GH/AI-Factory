package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import com.rcdiamond.aifactory.lang.ObjectKind
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdOperatorCatalogTest {

    @Test
    fun `convenience names map to verified registered ids`() {
        val expected = mapOf(
            "add" to "integrateddynamics:arithmetic_addition",
            "sub" to "integrateddynamics:arithmetic_subtraction",
            "mul" to "integrateddynamics:arithmetic_multiplication",
            "divide" to "integrateddynamics:arithmetic_division",
            "mod" to "integrateddynamics:arithmetic_modulus",
            "min" to "integrateddynamics:arithmetic_minimum",
            "max" to "integrateddynamics:arithmetic_maximum",
            "increment" to "integrateddynamics:arithmetic_increment",
            "decrement" to "integrateddynamics:arithmetic_decrement",
            "eq" to "integrateddynamics:relational_equals",
            "ne" to "integrateddynamics:relational_notequals",
            "lt" to "integrateddynamics:relational_lt",
            "le" to "integrateddynamics:relational_le",
            "gt" to "integrateddynamics:relational_gt",
            "ge" to "integrateddynamics:relational_ge",
            "not" to "integrateddynamics:logical_not",
            "toInteger" to "integrateddynamics:number_cast_integer",
            "toLong" to "integrateddynamics:number_cast_long",
            "toDouble" to "integrateddynamics:number_cast_double",
            "length" to "integrateddynamics:list_length",
            "isEmpty" to "integrateddynamics:list_empty",
            "get" to "integrateddynamics:list_get",
            "getOr" to "integrateddynamics:list_get_or_default",
            "append" to "integrateddynamics:list_append",
            "slice" to "integrateddynamics:list_slice",
            "map" to "integrateddynamics:operator_map",
            "filter" to "integrateddynamics:operator_filter",
            "reduce" to "integrateddynamics:operator_reduce",
            "iterate" to "integrateddynamics:list_lazybuilt",
            "identity" to "integrateddynamics:general_identity",
            "constant" to "integrateddynamics:general_constant",
            "flip" to "integrateddynamics:operator_flip",
            "call0" to "integrateddynamics:operator_apply0",
            "allOf" to "integrateddynamics:operator_conjunction",
            "anyOf" to "integrateddynamics:operator_disjunction",
            "negatePred" to "integrateddynamics:operator_negation",
            "pipe2" to "integrateddynamics:operator_pipe2",
            "itemCount" to "integrateddynamics:itemstack_itemlistcount",
            "itemMod" to "integrateddynamics:itemstack_mod",
            "itemIsStackable" to "integrateddynamics:itemstack_stackable",
            "itemIsEnchantable" to "integrateddynamics:itemstack_enchantable",
        )
        for ((name, id) in expected) {
            val primitive = assertNotNull(IdOperatorCatalog.resolvePrimitive(name), name)
            val lowering = primitive.lowering as? IdPrimitiveLowering.Native
            assertEquals(id, lowering?.operatorId, name)
            assertNotNull(IdOperatorCatalog.resolveNative(id), id)
        }
    }

    @Test
    fun `parser and checker helper native ids are present`() {
        val ids = setOf(
            IdOperatorCatalog.LOGICAL_AND,
            IdOperatorCatalog.LOGICAL_OR,
            IdOperatorCatalog.STRING_CONCAT,
            IdOperatorCatalog.LIST_CONCAT,
            IdOperatorCatalog.OPERATOR_APPLY,
            IdOperatorCatalog.OPERATOR_APPLY_0,
            IdOperatorCatalog.OPERATOR_FLIP,
            IdOperatorCatalog.GENERAL_CHOICE,
            IdOperatorCatalog.GENERAL_IDENTITY,
            IdOperatorCatalog.GENERAL_CONSTANT,
        )
        for (id in ids) assertNotNull(IdOperatorCatalog.resolveNative(id), id)
        assertNull(IdOperatorCatalog.resolveNative("integrateddynamics:does_not_exist"))
        assertTrue(IdOperatorCatalog.natives.keys.all { it.startsWith("integrateddynamics:") })
    }

    @Test
    fun `dynamic signatures have conservative source contracts`() {
        val apply = assertNotNull(IdOperatorCatalog.resolveNative(IdOperatorCatalog.OPERATOR_APPLY))
        assertTrue(apply.signature is IdNativeSignature.ApplyOne)
        assertNull(apply.sourceCallable)

        val apply0 = assertNotNull(IdOperatorCatalog.resolveNative(IdOperatorCatalog.OPERATOR_APPLY_0))
        assertTrue(apply0.signature is IdNativeSignature.ApplyZero)
        assertNull(apply0.sourceCallable)

        val flip = assertNotNull(IdOperatorCatalog.resolveNative(IdOperatorCatalog.OPERATOR_FLIP))
        assertTrue(flip.signature is IdNativeSignature.Flip)
        assertNotNull(flip.sourceCallable)
    }

    @Test
    fun `environment exposes core types constructors and literal constants`() {
        val environment = IdOperatorCatalog.environment
        for (name in IdDslTypes.CORE_SCALARS) assertTrue(environment.hasType(name), name)
        assertFalse(environment.hasType("Operator"))
        assertFalse(environment.hasType("Any"))
        assertEquals("Item", environment.objectConstructor(ObjectKind.ITEM)!!.resultType.name)
        assertEquals("Block", environment.objectConstructor(ObjectKind.BLOCK)!!.resultType.name)
        assertEquals("Fluid", environment.objectConstructor(ObjectKind.FLUID)!!.resultType.name)
        assertEquals("NBT", environment.objectConstructor(ObjectKind.NBT)!!.resultType.name)
        assertNotNull(environment.primitive("emptyItem"))
        assertNotNull(environment.primitive("emptyFluid"))
        assertNotNull(environment.primitive("iterate"))
        assertNotNull(environment.primitive("constant"))
        assertNotNull(environment.primitive("flip")!!.rule)
        assertNull(environment.primitive("flip")!!.scheme)
    }

    @Test
    fun `verified list aspect table is complete and typed`() {
        val expected = mapOf(
            "integrateddynamics:list_inventory_itemstacks" to IdDslTypes.list(IdDslTypes.ITEM),
            "integrateddynamics:list_fluid_fluidstacks" to IdDslTypes.list(IdDslTypes.FLUID),
            "integrateddynamics:list_fluid_capacities" to IdDslTypes.list(IdDslTypes.INTEGER),
            "integrateddynamics:list_entity_entities" to IdDslTypes.list(IdDslTypes.ENTITY),
            "integrateddynamics:list_entity_players" to IdDslTypes.list(IdDslTypes.ENTITY),
            "integrateddynamics:list_extradimensional_players" to IdDslTypes.list(IdDslTypes.ENTITY),
            "integrateddynamics:list_world_players" to IdDslTypes.list(IdDslTypes.ENTITY),
            "integrateddynamics:list_recipehandler_recipes" to IdDslTypes.list(IdDslTypes.RECIPE),
        )
        assertEquals(expected.keys, IdAspectContracts.listAspects.keys)
        for ((id, type) in expected) {
            val contract = assertNotNull(IdAspectContracts.find(id))
            assertEquals(type, contract.type)
            assertEquals(IdFiniteness.FINITE, contract.finiteness)
            assertTrue(contract.partId.startsWith("integrateddynamics:"))
        }
        assertNull(IdAspectContracts.find("example:unknown"))
    }

    @Test
    fun `every native contract id is unique and every value primitive stays typed`() {
        assertEquals(IdOperatorCatalog.natives.size, IdOperatorCatalog.natives.keys.size)
        for ((id, native) in IdOperatorCatalog.natives) {
            assertEquals(id, native.id)
            if (native.signature is IdNativeSignature.Fixed) {
                assertNotNull(native.sourceCallable, id)
            }
        }
        for (primitive in IdOperatorCatalog.primitives.values) {
            assertTrue(primitive.scheme != null || primitive.rule != null, primitive.name)
        }
    }
}
