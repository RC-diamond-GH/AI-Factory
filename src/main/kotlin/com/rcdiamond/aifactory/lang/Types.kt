package com.rcdiamond.aifactory.lang

/**
 * A checked language type. Source annotations never create [Variable]s; variables come from
 * polymorphic primitive schemes and are eliminated by inference before a [TypedModule] is returned.
 *
 * There is deliberately no `Any` type: an unresolved variable is a compile error, never an escape
 * hatch. [FunctionType.parameters] is a parameter sequence, not a tuple value, and its arity can be
 * zero.
 */
sealed interface LangType {
    data class Nominal(val name: String) : LangType

    data class ListType(val element: LangType) : LangType

    data class FunctionType(val parameters: List<LangType>, val result: LangType) : LangType

    /** An inference variable. It is not a user-visible type. */
    data class Variable(val id: Int) : LangType

    fun display(): String = when (this) {
        is Nominal -> name
        is ListType -> "[${element.display()}]"
        is FunctionType -> "Fn(${parameters.joinToString(", ") { it.display() }}) -> ${result.display()}"
        is Variable -> "#$id"
    }

    /** Structural id used by environments and tests; [Variable]s compare by id. */
    fun isConcrete(): Boolean = when (this) {
        is Nominal -> true
        is ListType -> element.isConcrete()
        is FunctionType -> parameters.all { it.isConcrete() } && result.isConcrete()
        is Variable -> false
    }

    fun containsVariable(): Boolean = !isConcrete
}

object LangTypes {
    val BOOLEAN: LangType.Nominal = LangType.Nominal("Boolean")
    val INTEGER: LangType.Nominal = LangType.Nominal("Integer")
    val LONG: LangType.Nominal = LangType.Nominal("Long")
    val DOUBLE: LangType.Nominal = LangType.Nominal("Double")
    val STRING: LangType.Nominal = LangType.Nominal("String")

    fun nominal(name: String): LangType.Nominal = LangType.Nominal(name)

    fun list(element: LangType): LangType.ListType = LangType.ListType(element)

    fun function(parameters: List<LangType>, result: LangType): LangType.FunctionType =
        LangType.FunctionType(parameters, result)

    fun function(vararg parameters: LangType, result: LangType): LangType.FunctionType =
        LangType.FunctionType(parameters.toList(), result)
}

/**
 * Constraints attached to inferred variables. `Numeric` and `Concat` are language-level constraints;
 * environments choose which nominal types count as numeric and may register [Named] constraints for
 * extension primitives. A constraint never converts a value; it only rejects a resolved type.
 */
sealed interface TypeConstraint {
    data object Numeric : TypeConstraint

    data object Concat : TypeConstraint

    data object Equatable : TypeConstraint

    data class Named(val name: String) : TypeConstraint

    fun display(): String = when (this) {
        Numeric -> "Numeric"
        Concat -> "Concat"
        Equatable -> "Equatable"
        is Named -> name
    }
}

/** A constraint on a quantified scheme variable. [variableId] must exist in [TypeScheme.quantified]. */
data class SchemeConstraint(val variableId: Int, val constraint: TypeConstraint)

/**
 * A primitive contract. Quantified variable ids are local to the scheme and are replaced by fresh
 * inference variables at every use, so one `eq` or `add` can be used at several concrete types.
 *
 * Example: `add` is `Numeric N => Fn(N, N) -> N`:
 * ```
 * val n = LangType.Variable(0)
 * TypeScheme(
 *     LangTypes.function(n, n, result = n),
 *     quantified = setOf(0),
 *     constraints = listOf(SchemeConstraint(0, TypeConstraint.Numeric)),
 * )
 * ```
 */
data class TypeScheme(
    val type: LangType,
    val quantified: Set<Int> = emptySet(),
    val constraints: List<SchemeConstraint> = emptyList(),
) {
    init {
        require(quantified.all { it >= 0 }) { "quantified type variable ids must be non-negative" }
        require(constraints.all { it.variableId in quantified }) {
            "scheme constraints must target quantified variables"
        }
        val free = collectFreeVariables(type)
        require(free.all { it in quantified }) {
            "scheme type contains unquantified variables: ${free - quantified}"
        }
    }

    companion object {
        fun monomorphic(type: LangType): TypeScheme = TypeScheme(type)

        fun of(type: LangType, variable: Int, constraint: TypeConstraint): TypeScheme =
            TypeScheme(type, setOf(variable), listOf(SchemeConstraint(variable, constraint)))
    }
}

private fun collectFreeVariables(type: LangType, into: MutableSet<Int> = LinkedHashSet()): Set<Int> {
    when (type) {
        is LangType.Nominal -> Unit
        is LangType.Variable -> into.add(type.id)
        is LangType.ListType -> collectFreeVariables(type.element, into)
        is LangType.FunctionType -> {
            type.parameters.forEach { collectFreeVariables(it, into) }
            collectFreeVariables(type.result, into)
        }
    }
    return into
}

internal fun LangType.depth(limit: Int): Int {
    fun visit(current: LangType, remaining: Int): Int {
        if (remaining < 0) return remaining
        return when (current) {
            is LangType.Nominal -> 1
            is LangType.Variable -> 1
            is LangType.ListType -> 1 + visit(current.element, remaining - 1)
            is LangType.FunctionType -> {
                var max = 1
                for (parameter in current.parameters) {
                    max = maxOf(max, 1 + visit(parameter, remaining - 1))
                }
                maxOf(max, 1 + visit(current.result, remaining - 1))
            }
        }
    }
    return visit(this, limit)
}
