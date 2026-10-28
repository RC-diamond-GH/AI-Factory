package com.rcdiamond.aifactory.lang

/** A reference to an earlier [TypedNode]. Nodes are created in dependency order, so ids are topological. */
sealed interface IrRef {
    data class Node(val id: Int) : IrRef
}

enum class IrArithmeticOperator { ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO }

enum class IrComparisonOperator { EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL }

sealed interface IrStaticArgument {
    data class Text(val value: String) : IrStaticArgument

    data class Integer(val value: Int) : IrStaticArgument
}

/**
 * Semantic operation of one typed graph node. The operation is target-independent; a backend maps
 * primitive names and language operations to its own native definitions.
 *
 * Language-level laziness is explicit: [Choice], [LogicalAnd] and [LogicalOr] have short-circuit
 * semantics. Other operand demand belongs to the callee: [Apply] only states that an argument is
 * supplied. A native backend may flatten a fully-applied [Primitive] chain and preserve that
 * primitive's non-strict parameters; [com.rcdiamond.aifactory.lang.PureEvaluator] evaluates
 * arguments eagerly and therefore only supports strict primitives (it deliberately has no
 * `constant`-style discard primitive).
 */
sealed interface IrOperation {
    /** External input binding with the declaration name. */
    data class Input(val name: String) : IrOperation

    /** A primitive used as a function value at this node's instantiated signature. */
    data class Primitive(val name: String) : IrOperation

    /** `op "..."`; type comes from the environment's operator catalog. */
    data class OperatorLiteral(val id: String) : IrOperation

    data class Constant(val value: LiteralValue) : IrOperation

    /** Homogeneous list; static elements may still be dynamic nodes. */
    data class ListLiteral(val elements: List<IrRef>) : IrOperation

    /** One function application step. Partial application is an Apply chain whose type is a function. */
    data class Apply(val callee: IrRef, val argument: IrRef) : IrOperation

    /** `if condition then yes else no`; only one branch is demanded. */
    data class Choice(val condition: IrRef, val whenTrue: IrRef, val whenFalse: IrRef) : IrOperation

    data class Arithmetic(
        val operator: IrArithmeticOperator,
        val left: IrRef,
        val right: IrRef,
    ) : IrOperation

    data class Comparison(
        val operator: IrComparisonOperator,
        val left: IrRef,
        val right: IrRef,
    ) : IrOperation

    /** Short-circuit Boolean conjunction. */
    data class LogicalAnd(val left: IrRef, val right: IrRef) : IrOperation

    /** Short-circuit Boolean disjunction. */
    data class LogicalOr(val left: IrRef, val right: IrRef) : IrOperation

    /** String concatenation or same-element-type list concatenation, selected by the node type. */
    data class Concat(val left: IrRef, val right: IrRef) : IrOperation

    /** Left-to-right unary function composition. */
    data class Compose(val left: IrRef, val right: IrRef) : IrOperation

    data class Negate(val operand: IrRef) : IrOperation

    /** A validated static object literal such as `item "..."` or `fluid "..." 1000`. */
    data class Construct(
        val kind: ObjectKind,
        val arguments: List<IrStaticArgument>,
    ) : IrOperation
}

sealed interface LiteralValue {
    data class Boolean(val value: Boolean) : LiteralValue

    data class Integer(val value: Int) : LiteralValue

    data class Long(val value: Long) : LiteralValue

    data class Double(val value: Double) : LiteralValue

    data class String(val value: String) : LiteralValue
}

data class TypedNode(
    val id: Int,
    val type: LangType,
    val operation: IrOperation,
    val span: SourceSpan,
) {
    init {
        require(id >= 0) { "node id must be non-negative" }
    }

    val key: String get() = "node/$id"

    fun references(): List<IrRef> = when (val op = operation) {
        is IrOperation.Input,
        is IrOperation.Primitive,
        is IrOperation.OperatorLiteral,
        is IrOperation.Constant,
        is IrOperation.Construct -> emptyList()

        is IrOperation.ListLiteral -> op.elements
        is IrOperation.Apply -> listOf(op.callee, op.argument)
        is IrOperation.Choice -> listOf(op.condition, op.whenTrue, op.whenFalse)
        is IrOperation.Arithmetic -> listOf(op.left, op.right)
        is IrOperation.Comparison -> listOf(op.left, op.right)
        is IrOperation.LogicalAnd -> listOf(op.left, op.right)
        is IrOperation.LogicalOr -> listOf(op.left, op.right)
        is IrOperation.Concat -> listOf(op.left, op.right)
        is IrOperation.Compose -> listOf(op.left, op.right)
        is IrOperation.Negate -> listOf(op.operand)
    }
}

data class TypedInput(
    val name: String,
    val type: LangType,
    val nodeId: Int,
    val span: SourceSpan,
)

data class TypedOutput(
    val name: String,
    val type: LangType,
    val reference: IrRef,
    val span: SourceSpan,
)

/**
 * Deterministic typed DAG. `nodes[i].id == i`, every reference points to a strictly smaller id, and
 * only nodes reachable from an output are present. Identical source and environment therefore yield
 * identical ids and operations.
 */
data class TypedGraph(
    val nodes: List<TypedNode>,
    val inputs: List<TypedInput>,
    val outputs: List<TypedOutput>,
) {
    init {
        for ((index, node) in nodes.withIndex()) {
            require(node.id == index) { "node at index $index has id ${node.id}" }
            for (reference in node.references()) {
                val target = (reference as IrRef.Node).id
                require(target in 0 until index) {
                    "node $index references node $target, which is not earlier in the graph"
                }
            }
            require(node.type.isConcrete()) { "node $index has unresolved type ${node.type.display()}" }
        }
        for (input in inputs) {
            require(input.nodeId in nodes.indices) { "input '${input.name}' points to a missing node" }
            val node = nodes[input.nodeId]
            require(node.operation is IrOperation.Input) { "input '${input.name}' does not point to an Input node" }
            require((node.operation as IrOperation.Input).name == input.name) {
                "input '${input.name}' name disagrees with its node"
            }
            require(input.type.isConcrete()) { "input '${input.name}' has unresolved type" }
        }
        for (output in outputs) {
            val id = (output.reference as IrRef.Node).id
            require(id in nodes.indices) { "output '${output.name}' points to a missing node" }
            require(output.type.isConcrete()) { "output '${output.name}' has unresolved type" }
        }
    }

    fun node(id: Int): TypedNode = nodes[id]

    fun output(name: String): TypedOutput? = outputs.firstOrNull { it.name == name }

    fun input(name: String): TypedInput? = inputs.firstOrNull { it.name == name }
}

/** A fully checked program plus its deterministic typed graph. */
data class TypedModule(
    val source: SourceText,
    val program: Program,
    val graph: TypedGraph,
) {
    fun output(name: String): TypedOutput? = graph.output(name)

    fun input(name: String): TypedInput? = graph.input(name)

    val outputs: List<TypedOutput> get() = graph.outputs

    val inputs: List<TypedInput> get() = graph.inputs
}
