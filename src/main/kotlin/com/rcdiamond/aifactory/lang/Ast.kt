package com.rcdiamond.aifactory.lang

/** A source type annotation. Names are resolved against [LanguageEnvironment] by the type checker. */
sealed interface TypeRef {
    val span: SourceSpan

    data class Named(val name: String, override val span: SourceSpan) : TypeRef

    data class ListOf(val element: TypeRef, override val span: SourceSpan) : TypeRef

    data class Function(
        val parameters: List<TypeRef>,
        val result: TypeRef,
        override val span: SourceSpan,
    ) : TypeRef

    fun display(): String = when (this) {
        is Named -> name
        is ListOf -> "[${element.display()}]"
        is Function -> "Fn(${parameters.joinToString(", ") { it.display() }}) -> ${result.display()}"
    }
}

enum class ObjectKind { ITEM, BLOCK, FLUID, NBT }

/** Literal-only argument of a static object constructor such as `fluid "minecraft:water" 1000`. */
sealed interface ConstructorArgument {
    val span: SourceSpan

    data class Text(val value: String, override val span: SourceSpan) : ConstructorArgument

    data class Integer(val value: Int, override val span: SourceSpan) : ConstructorArgument
}

enum class BinaryOperator(val symbol: String) {
    PLUS("+"),
    MINUS("-"),
    STAR("*"),
    SLASH("/"),
    PERCENT("%"),
    CONCAT("++"),
    COMPOSE(">>"),
    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS("<"),
    LESS_EQUAL("<="),
    GREATER(">"),
    GREATER_EQUAL(">="),
    AND("&&"),
    OR("||");

    val isArithmetic: Boolean
        get() = this == PLUS || this == MINUS || this == STAR || this == SLASH || this == PERCENT

    val isOrdering: Boolean
        get() = this == LESS || this == LESS_EQUAL || this == GREATER || this == GREATER_EQUAL

    val isEquality: Boolean
        get() = this == EQUAL || this == NOT_EQUAL
}

/**
 * An expression after parsing. Every node carries the exact UTF-16 source range it came from.
 * There is no lambda, assignment or user-defined function syntax.
 */
sealed interface Expr {
    val span: SourceSpan

    data class BooleanLiteral(val value: Boolean, override val span: SourceSpan) : Expr

    data class IntegerLiteral(val value: Int, override val span: SourceSpan) : Expr

    data class LongLiteral(val value: Long, override val span: SourceSpan) : Expr

    data class DoubleLiteral(val value: Double, override val span: SourceSpan) : Expr

    data class StringLiteral(val value: String, override val span: SourceSpan) : Expr

    data class ListLiteral(val elements: List<Expr>, override val span: SourceSpan) : Expr

    data class Name(val name: String, override val span: SourceSpan) : Expr

    /** `callee arg1 arg2 ...`, left-associative. */
    data class Call(
        val callee: Expr,
        val arguments: List<Expr>,
        override val span: SourceSpan,
    ) : Expr

    data class If(
        val condition: Expr,
        val thenBranch: Expr,
        val elseBranch: Expr,
        override val span: SourceSpan,
    ) : Expr

    data class Binary(
        val operator: BinaryOperator,
        val left: Expr,
        val right: Expr,
        override val span: SourceSpan,
    ) : Expr

    /** Prefix `-` on a non-literal expression; signed numeric literals are folded by the parser. */
    data class Negate(val operand: Expr, override val span: SourceSpan) : Expr

    data class Construct(
        val kind: ObjectKind,
        val arguments: List<ConstructorArgument>,
        override val span: SourceSpan,
    ) : Expr

    /** `op "namespace:registered_id"`; the target catalog supplies its concrete type scheme. */
    data class OperatorReference(val id: String, override val span: SourceSpan) : Expr
}

sealed interface Declaration {
    val name: String
    val nameSpan: SourceSpan
    val span: SourceSpan
}

data class InputDeclaration(
    override val name: String,
    override val nameSpan: SourceSpan,
    val type: TypeRef,
    override val span: SourceSpan,
) : Declaration

data class LetDeclaration(
    override val name: String,
    override val nameSpan: SourceSpan,
    val annotation: TypeRef?,
    val expression: Expr,
    override val span: SourceSpan,
) : Declaration

data class OutputDeclaration(
    override val name: String,
    override val nameSpan: SourceSpan,
    val annotation: TypeRef?,
    val expression: Expr,
    override val span: SourceSpan,
) : Declaration

data class Program(val declarations: List<Declaration>, val span: SourceSpan)
