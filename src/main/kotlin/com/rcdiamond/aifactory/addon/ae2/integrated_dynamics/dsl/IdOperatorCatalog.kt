package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import com.rcdiamond.aifactory.lang.EnvironmentBuilder
import com.rcdiamond.aifactory.lang.LangType
import com.rcdiamond.aifactory.lang.LangTypes
import com.rcdiamond.aifactory.lang.LanguageEnvironment
import com.rcdiamond.aifactory.lang.ObjectKind
import com.rcdiamond.aifactory.lang.OperatorContract
import com.rcdiamond.aifactory.lang.PrimitiveTypeRule
import com.rcdiamond.aifactory.lang.StandardScalarEnvironment
import com.rcdiamond.aifactory.lang.TypeConstraint
import com.rcdiamond.aifactory.lang.TypeScheme
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/** A statically known native operator contract from the pinned 1.31.3 core. */
data class IdNativeOperator(
    val id: String,
    val signature: IdNativeSignature,
    /** Zero-based argument indexes whose native implementation rejects known-infinite lists. */
    val finiteListArguments: Set<Int> = emptySet(),
    val description: String = "",
    /**
     * Scheme usable by `op "..."` when the native signature is dynamic. [signature] remains the
     * authoritative plan contract; a dynamic native is exposed to source only when a conservative
     * first-order scheme exists.
     */
    val sourceScheme: TypeScheme? = null,
) {
    val sourceCallable: TypeScheme?
        get() = sourceScheme ?: (signature as? IdNativeSignature.Fixed)?.scheme
}

sealed interface IdNativeSignature {
    data class Fixed(val scheme: TypeScheme) : IdNativeSignature

    /** `operator_apply`: first argument is a function, second its next argument. */
    data object ApplyOne : IdNativeSignature

    /** `operator_apply0`: first argument is a zero-parameter function. */
    data object ApplyZero : IdNativeSignature

    /** `operator_flip`: first argument is a function with at least two parameters. */
    data object Flip : IdNativeSignature
}

sealed interface IdPrimitiveLowering {
    /** Apply or reference the named native operator. */
    data class Native(val operatorId: String) : IdPrimitiveLowering

    /** A literal-only primitive such as `emptyItem`; JSON is already the plan value. */
    data class Literal(val json: JsonElement) : IdPrimitiveLowering
}

data class IdPrimitive(
    val name: String,
    val lowering: IdPrimitiveLowering,
    val scheme: TypeScheme? = null,
    val rule: PrimitiveTypeRule? = null,
) {
    init {
        require((scheme == null) != (rule == null)) { "a primitive needs exactly one of scheme or rule" }
    }
}

/**
 * Pinned Integrated Dynamics 1.31.3 operator catalog.
 *
 * ids are the operator `ResourceLocation`s derived from `Operators.java` (`<kind>_<operatorName>`),
 * not Java field names or book display names. The server cross-checks these ids against its live
 * `Operators.REGISTRY`; this class itself never touches Minecraft.
 */
object IdOperatorCatalog {
    const val MOD_ID = "integrateddynamics"

    private fun id(name: String): String = "$MOD_ID:$name"

    val LOGICAL_AND = id("logical_and")
    val LOGICAL_OR = id("logical_or")
    val LOGICAL_NOT = id("logical_not")

    val ARITHMETIC_ADDITION = id("arithmetic_addition")
    val ARITHMETIC_SUBTRACTION = id("arithmetic_subtraction")
    val ARITHMETIC_MULTIPLICATION = id("arithmetic_multiplication")
    val ARITHMETIC_DIVISION = id("arithmetic_division")
    val ARITHMETIC_MODULUS = id("arithmetic_modulus")
    val ARITHMETIC_MAXIMUM = id("arithmetic_maximum")
    val ARITHMETIC_MINIMUM = id("arithmetic_minimum")
    val ARITHMETIC_INCREMENT = id("arithmetic_increment")
    val ARITHMETIC_DECREMENT = id("arithmetic_decrement")

    val RELATIONAL_EQUALS = id("relational_equals")
    val RELATIONAL_NOTEQUALS = id("relational_notequals")
    val RELATIONAL_LT = id("relational_lt")
    val RELATIONAL_LE = id("relational_le")
    val RELATIONAL_GT = id("relational_gt")
    val RELATIONAL_GE = id("relational_ge")

    val NUMBER_CAST_DOUBLE = id("number_cast_double")
    val NUMBER_CAST_LONG = id("number_cast_long")
    val NUMBER_CAST_INTEGER = id("number_cast_integer")

    val STRING_CONCAT = id("string_concat")

    val LIST_LENGTH = id("list_length")
    val LIST_EMPTY = id("list_empty")
    val LIST_GET = id("list_get")
    val LIST_GET_OR_DEFAULT = id("list_get_or_default")
    val LIST_APPEND = id("list_append")
    val LIST_CONCAT = id("list_concat")
    val LIST_SLICE = id("list_slice")
    val LIST_LAZYBUILT = id("list_lazybuilt")

    val OPERATOR_APPLY = id("operator_apply")
    val OPERATOR_APPLY_0 = id("operator_apply0")
    val OPERATOR_MAP = id("operator_map")
    val OPERATOR_FILTER = id("operator_filter")
    val OPERATOR_REDUCE = id("operator_reduce")
    val OPERATOR_CONJUNCTION = id("operator_conjunction")
    val OPERATOR_DISJUNCTION = id("operator_disjunction")
    val OPERATOR_NEGATION = id("operator_negation")
    val OPERATOR_PIPE = id("operator_pipe")
    val OPERATOR_PIPE2 = id("operator_pipe2")
    val OPERATOR_FLIP = id("operator_flip")

    val GENERAL_CHOICE = id("general_choice")
    val GENERAL_IDENTITY = id("general_identity")
    val GENERAL_CONSTANT = id("general_constant")

    val ITEMSTACK_ITEMLISTCOUNT = id("itemstack_itemlistcount")
    val ITEMSTACK_MOD = id("itemstack_mod")
    val ITEMSTACK_STACKABLE = id("itemstack_stackable")
    val ITEMSTACK_ENCHANTABLE = id("itemstack_enchantable")

    /** Convenience name to native lowering. Populated together with the language environment. */
    val primitives: Map<String, IdPrimitive> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { buildPrimitives() }

    /** Native id to pinned contract. */
    val natives: Map<String, IdNativeOperator> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { buildNatives() }

    /** Source environment: core types, object constructors, convenience primitives and `op` catalog. */
    val environment: LanguageEnvironment by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { buildEnvironment() }

    fun resolveNative(operatorId: String): IdNativeOperator? = natives[operatorId]

    fun resolvePrimitive(name: String): IdPrimitive? = primitives[name]

    fun primitiveScheme(name: String): TypeScheme? =
        primitives[name]?.scheme ?: environment.primitive(name)?.scheme

    private fun buildNatives(): Map<String, IdNativeOperator> {
        val entries = ArrayList<IdNativeOperator>()

        fun add(
            operatorId: String,
            signature: IdNativeSignature,
            description: String,
            finite: Set<Int> = emptySet(),
            source: TypeScheme? = null,
        ) {
            entries += IdNativeOperator(operatorId, signature, finite, description, source)
        }

        fun fixed(
            operatorId: String,
            scheme: TypeScheme,
            description: String,
            finite: Set<Int> = emptySet(),
        ) = add(operatorId, IdNativeSignature.Fixed(scheme), description, finite)

        fixed(LOGICAL_AND, booleanBinary(), "short-circuit boolean AND")
        fixed(LOGICAL_OR, booleanBinary(), "short-circuit boolean OR")
        fixed(LOGICAL_NOT, booleanUnary(), "boolean NOT")

        fixed(ARITHMETIC_ADDITION, numericBinary(), "same-type numeric addition")
        fixed(ARITHMETIC_SUBTRACTION, numericBinary(), "same-type numeric subtraction")
        fixed(ARITHMETIC_MULTIPLICATION, numericBinary(), "same-type numeric multiplication")
        fixed(ARITHMETIC_DIVISION, numericBinary(), "same-type numeric division")
        fixed(ARITHMETIC_MODULUS, numericBinary(), "same-type numeric modulus")
        fixed(ARITHMETIC_MAXIMUM, numericBinary(), "same-type numeric maximum")
        fixed(ARITHMETIC_MINIMUM, numericBinary(), "same-type numeric minimum")
        fixed(ARITHMETIC_INCREMENT, numericUnary(), "same-type numeric increment")
        fixed(ARITHMETIC_DECREMENT, numericUnary(), "same-type numeric decrement")

        fixed(RELATIONAL_EQUALS, equality(), "same-type equality")
        fixed(RELATIONAL_NOTEQUALS, equality(), "same-type inequality")
        fixed(RELATIONAL_LT, numericComparison(), "numeric less-than")
        fixed(RELATIONAL_LE, numericComparison(), "numeric less-than-or-equal")
        fixed(RELATIONAL_GT, numericComparison(), "numeric greater-than")
        fixed(RELATIONAL_GE, numericComparison(), "numeric greater-than-or-equal")

        fixed(NUMBER_CAST_DOUBLE, numericCast(LangTypes.DOUBLE), "numeric cast to Double")
        fixed(NUMBER_CAST_LONG, numericCast(LangTypes.LONG), "numeric cast to Long")
        fixed(NUMBER_CAST_INTEGER, numericCast(LangTypes.INTEGER), "numeric cast to Integer")

        fixed(STRING_CONCAT, LangTypes.function(LangTypes.STRING, LangTypes.STRING, result = LangTypes.STRING), "string concatenation")

        fixed(LIST_LENGTH, listLength(), "list length")
        fixed(LIST_EMPTY, listEmpty(), "list emptiness")
        fixed(LIST_GET, listGet(), "list element; out-of-range fails natively")
        fixed(LIST_GET_OR_DEFAULT, listGetOrDefault(), "list element with default")
        fixed(LIST_APPEND, listAppend(), "append one element")
        fixed(LIST_CONCAT, listConcat(), "concatenate two lists")
        fixed(LIST_SLICE, listSlice(), "list range [from, to); native requires 0 <= from < to")
        fixed(LIST_LAZYBUILT, lazyBuilt(), "lazy list from seed and step operator")

        add(OPERATOR_APPLY, IdNativeSignature.ApplyOne, "apply one argument; exact or curried")
        add(OPERATOR_APPLY_0, IdNativeSignature.ApplyZero, "apply a zero-argument operator")
        add(OPERATOR_FLIP, IdNativeSignature.Flip, "swap the first two parameters of a function", source = flipScheme())
        fixed(OPERATOR_MAP, listMap(), "map a unary function over a list")
        fixed(OPERATOR_FILTER, listFilter(), "filter with a predicate", finite = setOf(1))
        fixed(OPERATOR_REDUCE, listReduce(), "left fold (operator, list, seed)", finite = setOf(1))
        fixed(OPERATOR_CONJUNCTION, predicateCombination(), "predicate conjunction")
        fixed(OPERATOR_DISJUNCTION, predicateCombination(), "predicate disjunction")
        fixed(OPERATOR_NEGATION, predicateNegation(), "predicate negation")
        fixed(OPERATOR_PIPE, pipe(), "unary left-to-right composition")
        fixed(OPERATOR_PIPE2, pipe2(), "two-branch unary pipeline")

        fixed(GENERAL_CHOICE, choice(), "choice; only the selected branch is demanded")
        fixed(GENERAL_IDENTITY, identity(), "identity")
        fixed(GENERAL_CONSTANT, constant(), "constant (first argument)")

        fixed(ITEMSTACK_ITEMLISTCOUNT, itemCount(), "count same item and NBT in an item list", finite = setOf(0))
        fixed(ITEMSTACK_MOD, LangTypes.function(item(), result = LangTypes.STRING), "item mod display name")
        fixed(ITEMSTACK_STACKABLE, LangTypes.function(item(), result = LangTypes.BOOLEAN), "item is stackable")
        fixed(ITEMSTACK_ENCHANTABLE, LangTypes.function(item(), result = LangTypes.BOOLEAN), "item is enchantable")

        return entries.associateByTo(LinkedHashMap()) { it.id }
    }

    private fun buildPrimitives(): Map<String, IdPrimitive> {
        val base = StandardScalarEnvironment.environment
        val entries = ArrayList<IdPrimitive>()

        fun native(name: String, operatorId: String) {
            val contract = base.primitive(name)
                ?: error("standard environment has no primitive '$name'")
            entries += IdPrimitive(name, IdPrimitiveLowering.Native(operatorId), contract.scheme, contract.rule)
        }

        native("add", ARITHMETIC_ADDITION)
        native("sub", ARITHMETIC_SUBTRACTION)
        native("mul", ARITHMETIC_MULTIPLICATION)
        native("divide", ARITHMETIC_DIVISION)
        native("mod", ARITHMETIC_MODULUS)
        native("min", ARITHMETIC_MINIMUM)
        native("max", ARITHMETIC_MAXIMUM)
        native("increment", ARITHMETIC_INCREMENT)
        native("decrement", ARITHMETIC_DECREMENT)
        native("eq", RELATIONAL_EQUALS)
        native("ne", RELATIONAL_NOTEQUALS)
        native("lt", RELATIONAL_LT)
        native("le", RELATIONAL_LE)
        native("gt", RELATIONAL_GT)
        native("ge", RELATIONAL_GE)
        native("not", LOGICAL_NOT)
        native("toInteger", NUMBER_CAST_INTEGER)
        native("toLong", NUMBER_CAST_LONG)
        native("toDouble", NUMBER_CAST_DOUBLE)
        native("length", LIST_LENGTH)
        native("isEmpty", LIST_EMPTY)
        native("get", LIST_GET)
        native("getOr", LIST_GET_OR_DEFAULT)
        native("append", LIST_APPEND)
        native("slice", LIST_SLICE)
        native("map", OPERATOR_MAP)
        native("filter", OPERATOR_FILTER)
        native("reduce", OPERATOR_REDUCE)
        native("identity", GENERAL_IDENTITY)
        native("flip", OPERATOR_FLIP)
        native("call0", OPERATOR_APPLY_0)
        native("allOf", OPERATOR_CONJUNCTION)
        native("anyOf", OPERATOR_DISJUNCTION)
        native("negatePred", OPERATOR_NEGATION)
        native("pipe2", OPERATOR_PIPE2)

        entries += IdPrimitive("iterate", IdPrimitiveLowering.Native(LIST_LAZYBUILT), lazyBuilt())
        entries += IdPrimitive("constant", IdPrimitiveLowering.Native(GENERAL_CONSTANT), constant())
        entries += IdPrimitive("itemCount", IdPrimitiveLowering.Native(ITEMSTACK_ITEMLISTCOUNT), itemCount())
        entries += IdPrimitive("itemMod", IdPrimitiveLowering.Native(ITEMSTACK_MOD), LangTypes.function(item(), result = LangTypes.STRING))
        entries += IdPrimitive("itemIsStackable", IdPrimitiveLowering.Native(ITEMSTACK_STACKABLE), LangTypes.function(item(), result = LangTypes.BOOLEAN))
        entries += IdPrimitive("itemIsEnchantable", IdPrimitiveLowering.Native(ITEMSTACK_ENCHANTABLE), LangTypes.function(item(), result = LangTypes.BOOLEAN))

        entries += IdPrimitive(
            "emptyItem",
            IdPrimitiveLowering.Literal(JsonNull),
            TypeScheme.monomorphic(item()),
        )
        entries += IdPrimitive(
            "emptyFluid",
            IdPrimitiveLowering.Literal(JsonNull),
            TypeScheme.monomorphic(fluid()),
        )

        return entries.associateByTo(LinkedHashMap()) { it.name }
    }

    private fun buildEnvironment(): LanguageEnvironment {
        val builder: EnvironmentBuilder = StandardScalarEnvironment.environment.toBuilder()
        val base = StandardScalarEnvironment.environment
        for ((name, description) in listOf(
            "Item" to "item stack",
            "Block" to "block state",
            "Entity" to "entity",
            "Fluid" to "fluid stack",
            "NBT" to "SNBT tag",
            "Ingredients" to "ingredient list",
            "Recipe" to "recipe",
        )) {
            if (!base.hasType(name)) builder.addNominalType(name, description)
        }

        if (base.objectConstructor(ObjectKind.ITEM) == null) {
            builder.addObjectConstructor(
                ObjectKind.ITEM,
                "Item",
                listOf(com.rcdiamond.aifactory.lang.ConstructorArgumentKind.RESOURCE_ID),
            )
        }
        if (base.objectConstructor(ObjectKind.BLOCK) == null) {
            builder.addObjectConstructor(
                ObjectKind.BLOCK,
                "Block",
                listOf(com.rcdiamond.aifactory.lang.ConstructorArgumentKind.RESOURCE_ID),
            )
        }
        if (base.objectConstructor(ObjectKind.FLUID) == null) {
            builder.addObjectConstructor(
                ObjectKind.FLUID,
                "Fluid",
                listOf(
                    com.rcdiamond.aifactory.lang.ConstructorArgumentKind.RESOURCE_ID,
                    com.rcdiamond.aifactory.lang.ConstructorArgumentKind.POSITIVE_INTEGER,
                ),
            )
        }
        if (base.objectConstructor(ObjectKind.NBT) == null) {
            builder.addObjectConstructor(
                ObjectKind.NBT,
                "NBT",
                listOf(com.rcdiamond.aifactory.lang.ConstructorArgumentKind.TEXT),
            )
        }

        for (primitive in primitives.values) {
            if (base.primitive(primitive.name) != null) continue
            val scheme = primitive.scheme
            val rule = primitive.rule
            if (scheme != null) {
                builder.addPrimitive(primitive.name, scheme, "ID DSL convenience")
            } else if (rule != null) {
                builder.addPrimitive(primitive.name, rule, "ID DSL convenience")
            }
        }

        builder.operatorCatalog { operatorId ->
            natives[operatorId]?.sourceCallable?.let { contract ->
                OperatorContract(operatorId, contract, "pinned Integrated Dynamics operator")
            }
        }
        return builder.build()
    }

    // ------------------------------------------------------------------ schemes

    private fun nominal(name: String): LangType = LangType.Nominal(name)

    private fun item(): LangType = nominal("Item")

    private fun fluid(): LangType = nominal("Fluid")

    private fun variable(id: Int): LangType = LangType.Variable(id)

    private fun numericBinary(): TypeScheme {
        val n = variable(0)
        return TypeScheme(
            LangTypes.function(n, n, result = n),
            setOf(0),
            listOf(com.rcdiamond.aifactory.lang.SchemeConstraint(0, TypeConstraint.Numeric)),
        )
    }

    private fun numericUnary(): TypeScheme {
        val n = variable(0)
        return TypeScheme(
            LangTypes.function(n, result = n),
            setOf(0),
            listOf(com.rcdiamond.aifactory.lang.SchemeConstraint(0, TypeConstraint.Numeric)),
        )
    }

    private fun numericComparison(): TypeScheme {
        val n = variable(0)
        return TypeScheme(
            LangTypes.function(n, n, result = LangTypes.BOOLEAN),
            setOf(0),
            listOf(com.rcdiamond.aifactory.lang.SchemeConstraint(0, TypeConstraint.Numeric)),
        )
    }

    private fun numericCast(target: LangType): TypeScheme {
        val n = variable(0)
        return TypeScheme(
            LangTypes.function(n, result = target),
            setOf(0),
            listOf(com.rcdiamond.aifactory.lang.SchemeConstraint(0, TypeConstraint.Numeric)),
        )
    }

    private fun equality(): TypeScheme {
        val a = variable(0)
        return TypeScheme(LangTypes.function(a, a, result = LangTypes.BOOLEAN), setOf(0))
    }

    private fun booleanBinary(): TypeScheme =
        TypeScheme.monomorphic(LangTypes.function(LangTypes.BOOLEAN, LangTypes.BOOLEAN, result = LangTypes.BOOLEAN))

    private fun booleanUnary(): TypeScheme =
        TypeScheme.monomorphic(LangTypes.function(LangTypes.BOOLEAN, result = LangTypes.BOOLEAN))

    private fun listLength(): TypeScheme {
        val a = variable(0)
        return TypeScheme(LangTypes.function(LangTypes.list(a), result = LangTypes.INTEGER), setOf(0))
    }

    private fun listEmpty(): TypeScheme {
        val a = variable(0)
        return TypeScheme(LangTypes.function(LangTypes.list(a), result = LangTypes.BOOLEAN), setOf(0))
    }

    private fun listGet(): TypeScheme {
        val a = variable(0)
        return TypeScheme(LangTypes.function(LangTypes.list(a), LangTypes.INTEGER, result = a), setOf(0))
    }

    private fun listGetOrDefault(): TypeScheme {
        val a = variable(0)
        return TypeScheme(
            LangTypes.function(LangTypes.list(a), LangTypes.INTEGER, a, result = a),
            setOf(0),
        )
    }

    private fun listAppend(): TypeScheme {
        val a = variable(0)
        return TypeScheme(
            LangTypes.function(LangTypes.list(a), a, result = LangTypes.list(a)),
            setOf(0),
        )
    }

    private fun listConcat(): TypeScheme {
        val a = variable(0)
        val list = LangTypes.list(a)
        return TypeScheme(LangTypes.function(list, list, result = list), setOf(0))
    }

    private fun listSlice(): TypeScheme {
        val a = variable(0)
        return TypeScheme(
            LangTypes.function(
                LangTypes.list(a),
                LangTypes.INTEGER,
                LangTypes.INTEGER,
                result = LangTypes.list(a),
            ),
            setOf(0),
        )
    }

    private fun lazyBuilt(): TypeScheme {
        val a = variable(0)
        return TypeScheme(
            LangTypes.function(a, LangTypes.function(a, result = a), result = LangTypes.list(a)),
            setOf(0),
        )
    }

    private fun listMap(): TypeScheme {
        val a = variable(0)
        val b = variable(1)
        return TypeScheme(
            LangTypes.function(
                LangTypes.function(a, result = b),
                LangTypes.list(a),
                result = LangTypes.list(b),
            ),
            setOf(0, 1),
        )
    }

    private fun listFilter(): TypeScheme {
        val a = variable(0)
        return TypeScheme(
            LangTypes.function(
                LangTypes.function(a, result = LangTypes.BOOLEAN),
                LangTypes.list(a),
                result = LangTypes.list(a),
            ),
            setOf(0),
        )
    }

    private fun listReduce(): TypeScheme {
        val a = variable(0)
        val b = variable(1)
        return TypeScheme(
            LangTypes.function(
                LangTypes.function(a, b, result = a),
                LangTypes.list(b),
                a,
                result = a,
            ),
            setOf(0, 1),
        )
    }

    private fun identity(): TypeScheme {
        val a = variable(0)
        return TypeScheme(LangTypes.function(a, result = a), setOf(0))
    }

    private fun constant(): TypeScheme {
        val a = variable(0)
        val b = variable(1)
        return TypeScheme(LangTypes.function(a, b, result = a), setOf(0, 1))
    }

    private fun choice(): TypeScheme {
        val a = variable(0)
        return TypeScheme(
            LangTypes.function(LangTypes.BOOLEAN, a, a, result = a),
            setOf(0),
        )
    }

    private fun predicateCombination(): TypeScheme {
        val a = variable(0)
        val predicate = LangTypes.function(a, result = LangTypes.BOOLEAN)
        return TypeScheme(LangTypes.function(predicate, predicate, result = predicate), setOf(0))
    }

    private fun predicateNegation(): TypeScheme {
        val a = variable(0)
        val predicate = LangTypes.function(a, result = LangTypes.BOOLEAN)
        return TypeScheme(LangTypes.function(predicate, result = predicate), setOf(0))
    }

    private fun pipe(): TypeScheme {
        val a = variable(0)
        val b = variable(1)
        val c = variable(2)
        return TypeScheme(
            LangTypes.function(
                LangTypes.function(a, result = b),
                LangTypes.function(b, result = c),
                result = LangTypes.function(a, result = c),
            ),
            setOf(0, 1, 2),
        )
    }

    private fun pipe2(): TypeScheme {
        val a = variable(0)
        val b = variable(1)
        val c = variable(2)
        val d = variable(3)
        return TypeScheme(
            LangTypes.function(
                LangTypes.function(a, result = b),
                LangTypes.function(a, result = c),
                LangTypes.function(b, c, result = d),
                result = LangTypes.function(a, result = d),
            ),
            setOf(0, 1, 2, 3),
        )
    }

    private fun flipScheme(): TypeScheme {
        val a = variable(0)
        val b = variable(1)
        val c = variable(2)
        return TypeScheme(
            LangTypes.function(
                LangTypes.function(a, b, result = c),
                result = LangTypes.function(b, a, result = c),
            ),
            setOf(0, 1, 2),
        )
    }

    private fun itemCount(): TypeScheme =
        TypeScheme.monomorphic(
            LangTypes.function(LangTypes.list(item()), item(), result = LangTypes.INTEGER),
        )
}
