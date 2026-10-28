package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

/**
 * Concrete value types accepted by the Integrated Dynamics DSL wire.
 *
 * There is deliberately no `Any` and no wire-level type variable. An inference variable belongs to
 * the frontend only; by the time a plan is built every node and port carries a concrete type.
 */
sealed interface IdDslType {
    data class Scalar(val name: String) : IdDslType

    data class ListType(val element: IdDslType) : IdDslType

    data class FunctionType(val parameters: List<IdDslType>, val result: IdDslType) : IdDslType

    fun display(): String = when (this) {
        is Scalar -> name
        is ListType -> "[${element.display()}]"
        is FunctionType -> "Fn(${parameters.joinToString(", ") { it.display() }}) -> ${result.display()}"
    }

    fun depth(): Int = when (this) {
        is Scalar -> 1
        is ListType -> 1 + element.depth()
        is FunctionType -> 1 + maxOf(result.depth(), parameters.maxOfOrNull { it.depth() } ?: 0)
    }
}

/** Concrete nominal names exposed by the pinned core environment. */
object IdDslTypes {
    val BOOLEAN = IdDslType.Scalar("Boolean")
    val INTEGER = IdDslType.Scalar("Integer")
    val LONG = IdDslType.Scalar("Long")
    val DOUBLE = IdDslType.Scalar("Double")
    val STRING = IdDslType.Scalar("String")
    val ITEM = IdDslType.Scalar("Item")
    val BLOCK = IdDslType.Scalar("Block")
    val ENTITY = IdDslType.Scalar("Entity")
    val FLUID = IdDslType.Scalar("Fluid")
    val NBT = IdDslType.Scalar("NBT")
    val INGREDIENTS = IdDslType.Scalar("Ingredients")
    val RECIPE = IdDslType.Scalar("Recipe")

    /**
     * The twelve concrete core value types. `Operator`, `List` and `Fn` are represented structurally,
     * and a bare `Operator` is not a source type.
     */
    val CORE_SCALARS: Set<String> = linkedSetOf(
        "Boolean", "Integer", "Long", "Double", "String",
        "Item", "Block", "Entity", "Fluid", "NBT", "Ingredients", "Recipe",
    )

    val NUMERIC_SCALARS: Set<String> = setOf("Integer", "Long", "Double")

    fun isSupported(type: IdDslType): Boolean = when (type) {
        is IdDslType.Scalar -> type.name in CORE_SCALARS
        is IdDslType.ListType -> isSupported(type.element)
        is IdDslType.FunctionType -> type.parameters.all(::isSupported) && isSupported(type.result)
    }

    fun list(element: IdDslType): IdDslType.ListType = IdDslType.ListType(element)

    fun fn(parameters: List<IdDslType>, result: IdDslType): IdDslType.FunctionType =
        IdDslType.FunctionType(parameters, result)

    fun fn(vararg parameters: IdDslType, result: IdDslType): IdDslType.FunctionType =
        IdDslType.FunctionType(parameters.toList(), result)
}

/** Compile-time finiteness of a list value. Scalars have no meaningful finiteness. */
enum class IdFiniteness(val wire: String) {
    FINITE("finite"),
    INFINITE("infinite"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromWire(value: String): IdFiniteness? = entries.firstOrNull { it.wire == value }
    }
}
