package com.rcdiamond.aifactory.lang

/** A callable argument to a pure primitive. Implementations must be strict. */
fun interface PurePrimitive {
    fun invoke(arguments: List<PureValue>): PureValue
}

/** Error raised by a pure primitive implementation; the evaluator adds the current source span. */
class PurePrimitiveException(message: String) : RuntimeException(message)

sealed interface PureValue {
    data class BooleanValue(val value: Boolean) : PureValue

    data class IntegerValue(val value: Int) : PureValue

    data class LongValue(val value: Long) : PureValue

    data class DoubleValue(val value: Double) : PureValue

    data class StringValue(val value: String) : PureValue

    data class ListValue(val elements: List<PureValue>) : PureValue

    data class ObjectValue(
        val kind: ObjectKind,
        val typeName: String,
        val arguments: List<PureValue>,
    ) : PureValue

    /**
     * A curried value. [signature] shrinks as arguments are applied; [requiredArity] is the total
     * number of arguments of the original function. [appliedArguments] is exposed for tests and
     * tooling (partial application is observable).
     */
    class FunctionValue internal constructor(
        val signature: LangType.FunctionType,
        private val applied: List<PureValue>,
        val requiredArity: Int,
        private val implementation: (List<PureValue>) -> PureValue,
    ) : PureValue {
        init {
            require(requiredArity >= 0) { "required arity must be non-negative" }
            require(applied.size <= requiredArity) { "more arguments applied than the function accepts" }
        }

        val appliedArguments: List<PureValue> get() = applied

        /** Public test/tooling entry point for completing or extending a partial application. */
        fun applyTo(argument: PureValue): PureValue = apply(argument)

        /** Invoke a genuinely zero-parameter function value (`Fn() -> R`). */
        fun callZero(): PureValue = invokeZero()

        internal fun apply(argument: PureValue): PureValue {
            if (requiredArity == 0) {
                throw PurePrimitiveException("cannot apply an argument to a zero-parameter function")
            }
            if (applied.size >= requiredArity) {
                throw PurePrimitiveException("function is already fully applied")
            }
            val next = applied + argument
            return if (next.size == requiredArity) {
                implementation(next)
            } else {
                FunctionValue(shrinkSignature(), next, requiredArity, implementation)
            }
        }

        internal fun invokeZero(): PureValue {
            if (requiredArity != 0) {
                throw PurePrimitiveException("function requires $requiredArity arguments, not zero")
            }
            return implementation(emptyList())
        }

        private fun shrinkSignature(): LangType.FunctionType {
            val parameters = signature.parameters
            if (parameters.isEmpty()) {
                throw PurePrimitiveException("function signature has no remaining parameters")
            }
            return LangType.FunctionType(parameters.drop(1), signature.result)
        }

        override fun toString(): String =
            "FunctionValue(${signature.display()}, applied=${applied.size}/$requiredArity)"

        companion object {
            internal fun ofPrimitive(
                signature: LangType.FunctionType,
                primitive: PurePrimitive,
            ): FunctionValue = FunctionValue(
                signature,
                emptyList(),
                signature.parameters.size,
                primitive::invoke,
            )

            internal fun ofSingleArgument(
                signature: LangType.FunctionType,
                implementation: (PureValue) -> PureValue,
            ): FunctionValue = FunctionValue(
                signature,
                emptyList(),
                1,
                { arguments -> implementation(arguments[0]) },
            )
        }
    }

    /** Structural match against a fully resolved language type. Empty lists match any list type. */
    fun matches(type: LangType): Boolean = when (this) {
        is BooleanValue -> type == LangTypes.BOOLEAN
        is IntegerValue -> type == LangTypes.INTEGER
        is LongValue -> type == LangTypes.LONG
        is DoubleValue -> type == LangTypes.DOUBLE
        is StringValue -> type == LangTypes.STRING
        is FunctionValue -> type is LangType.FunctionType && signature == type
        is ObjectValue -> type is LangType.Nominal && type.name == typeName
        is ListValue -> when (type) {
            is LangType.ListType -> elements.all { it.matches(type.element) }
            else -> false
        }
    }
}

internal fun applyPure(function: PureValue, argument: PureValue): PureValue {
    val callable = function as? PureValue.FunctionValue
        ?: throw PurePrimitiveException("cannot apply an argument to non-function value $function")
    return callable.apply(argument)
}

internal fun valuesEqual(left: PureValue, right: PureValue): Boolean = when (left) {
    is PureValue.BooleanValue -> right is PureValue.BooleanValue && left.value == right.value
    is PureValue.IntegerValue -> right is PureValue.IntegerValue && left.value == right.value
    is PureValue.LongValue -> right is PureValue.LongValue && left.value == right.value
    is PureValue.DoubleValue -> right is PureValue.DoubleValue && left.value == right.value
    is PureValue.StringValue -> right is PureValue.StringValue && left.value == right.value
    is PureValue.ListValue -> right is PureValue.ListValue &&
        left.elements.size == right.elements.size &&
        left.elements.indices.all { valuesEqual(left.elements[it], right.elements[it]) }

    is PureValue.ObjectValue -> left == right
    is PureValue.FunctionValue -> left === right
}

internal object ScalarOps {
    fun arithmetic(operator: IrArithmeticOperator, left: PureValue, right: PureValue): PureValue =
        when (left) {
            is PureValue.IntegerValue -> {
                val other = right as? PureValue.IntegerValue ?: mismatch(left, right)
                PureValue.IntegerValue(
                    when (operator) {
                        IrArithmeticOperator.ADD -> left.value + other.value
                        IrArithmeticOperator.SUBTRACT -> left.value - other.value
                        IrArithmeticOperator.MULTIPLY -> left.value * other.value
                        IrArithmeticOperator.DIVIDE -> {
                            if (other.value == 0) throw PurePrimitiveException("Integer division by zero")
                            left.value / other.value
                        }

                        IrArithmeticOperator.MODULO -> {
                            if (other.value == 0) throw PurePrimitiveException("Integer remainder by zero")
                            left.value % other.value
                        }
                    },
                )
            }

            is PureValue.LongValue -> {
                val other = right as? PureValue.LongValue ?: mismatch(left, right)
                PureValue.LongValue(
                    when (operator) {
                        IrArithmeticOperator.ADD -> left.value + other.value
                        IrArithmeticOperator.SUBTRACT -> left.value - other.value
                        IrArithmeticOperator.MULTIPLY -> left.value * other.value
                        IrArithmeticOperator.DIVIDE -> {
                            if (other.value == 0L) throw PurePrimitiveException("Long division by zero")
                            left.value / other.value
                        }

                        IrArithmeticOperator.MODULO -> {
                            if (other.value == 0L) throw PurePrimitiveException("Long remainder by zero")
                            left.value % other.value
                        }
                    },
                )
            }

            is PureValue.DoubleValue -> {
                val other = right as? PureValue.DoubleValue ?: mismatch(left, right)
                PureValue.DoubleValue(
                    when (operator) {
                        IrArithmeticOperator.ADD -> left.value + other.value
                        IrArithmeticOperator.SUBTRACT -> left.value - other.value
                        IrArithmeticOperator.MULTIPLY -> left.value * other.value
                        IrArithmeticOperator.DIVIDE -> left.value / other.value
                        IrArithmeticOperator.MODULO -> left.value % other.value
                    },
                )
            }

            else -> mismatch(left, right)
        }

    fun minimum(left: PureValue, right: PureValue): PureValue = when (left) {
        is PureValue.IntegerValue -> {
            val other = right as? PureValue.IntegerValue ?: mismatch(left, right)
            PureValue.IntegerValue(if (left.value <= other.value) left.value else other.value)
        }

        is PureValue.LongValue -> {
            val other = right as? PureValue.LongValue ?: mismatch(left, right)
            PureValue.LongValue(if (left.value <= other.value) left.value else other.value)
        }

        is PureValue.DoubleValue -> {
            val other = right as? PureValue.DoubleValue ?: mismatch(left, right)
            PureValue.DoubleValue(if (left.value <= other.value) left.value else other.value)
        }

        else -> mismatch(left, right)
    }

    fun maximum(left: PureValue, right: PureValue): PureValue = when (left) {
        is PureValue.IntegerValue -> {
            val other = right as? PureValue.IntegerValue ?: mismatch(left, right)
            PureValue.IntegerValue(if (left.value >= other.value) left.value else other.value)
        }

        is PureValue.LongValue -> {
            val other = right as? PureValue.LongValue ?: mismatch(left, right)
            PureValue.LongValue(if (left.value >= other.value) left.value else other.value)
        }

        is PureValue.DoubleValue -> {
            val other = right as? PureValue.DoubleValue ?: mismatch(left, right)
            PureValue.DoubleValue(if (left.value >= other.value) left.value else other.value)
        }

        else -> mismatch(left, right)
    }

    fun increment(value: PureValue): PureValue = when (value) {
        is PureValue.IntegerValue -> PureValue.IntegerValue(value.value + 1)
        is PureValue.LongValue -> PureValue.LongValue(value.value + 1L)
        is PureValue.DoubleValue -> PureValue.DoubleValue(value.value + 1.0)
        else -> throw PurePrimitiveException("numeric operation requires a numeric value")
    }

    fun decrement(value: PureValue): PureValue = when (value) {
        is PureValue.IntegerValue -> PureValue.IntegerValue(value.value - 1)
        is PureValue.LongValue -> PureValue.LongValue(value.value - 1L)
        is PureValue.DoubleValue -> PureValue.DoubleValue(value.value - 1.0)
        else -> throw PurePrimitiveException("numeric operation requires a numeric value")
    }

    fun negate(value: PureValue): PureValue = when (value) {
        is PureValue.IntegerValue -> PureValue.IntegerValue(-value.value)
        is PureValue.LongValue -> PureValue.LongValue(-value.value)
        is PureValue.DoubleValue -> PureValue.DoubleValue(-value.value)
        else -> throw PurePrimitiveException("cannot negate non-numeric value $value")
    }

    fun comparison(operator: IrComparisonOperator, left: PureValue, right: PureValue): PureValue {
        if (operator == IrComparisonOperator.EQUAL || operator == IrComparisonOperator.NOT_EQUAL) {
            val equal = valuesEqual(left, right)
            return PureValue.BooleanValue(if (operator == IrComparisonOperator.EQUAL) equal else !equal)
        }
        val result = when (left) {
            is PureValue.IntegerValue -> {
                val other = right as? PureValue.IntegerValue ?: mismatch(left, right)
                when (operator) {
                    IrComparisonOperator.LESS -> left.value < other.value
                    IrComparisonOperator.LESS_EQUAL -> left.value <= other.value
                    IrComparisonOperator.GREATER -> left.value > other.value
                    IrComparisonOperator.GREATER_EQUAL -> left.value >= other.value
                    else -> error("equality handled above")
                }
            }

            is PureValue.LongValue -> {
                val other = right as? PureValue.LongValue ?: mismatch(left, right)
                when (operator) {
                    IrComparisonOperator.LESS -> left.value < other.value
                    IrComparisonOperator.LESS_EQUAL -> left.value <= other.value
                    IrComparisonOperator.GREATER -> left.value > other.value
                    IrComparisonOperator.GREATER_EQUAL -> left.value >= other.value
                    else -> error("equality handled above")
                }
            }

            is PureValue.DoubleValue -> {
                val other = right as? PureValue.DoubleValue ?: mismatch(left, right)
                when (operator) {
                    IrComparisonOperator.LESS -> left.value < other.value
                    IrComparisonOperator.LESS_EQUAL -> left.value <= other.value
                    IrComparisonOperator.GREATER -> left.value > other.value
                    IrComparisonOperator.GREATER_EQUAL -> left.value >= other.value
                    else -> error("equality handled above")
                }
            }

            else -> throw PurePrimitiveException("ordering comparisons require numeric values")
        }
        return PureValue.BooleanValue(result)
    }

    fun concat(left: PureValue, right: PureValue): PureValue = when (left) {
        is PureValue.StringValue -> {
            val other = right as? PureValue.StringValue ?: mismatch(left, right)
            PureValue.StringValue(left.value + other.value)
        }

        is PureValue.ListValue -> {
            val other = right as? PureValue.ListValue ?: mismatch(left, right)
            PureValue.ListValue(left.elements + other.elements)
        }

        else -> throw PurePrimitiveException("concatenation requires String or List values")
    }

    fun toInteger(value: PureValue): PureValue = when (value) {
        is PureValue.IntegerValue -> value
        is PureValue.LongValue -> PureValue.IntegerValue(value.value.toInt())
        is PureValue.DoubleValue -> PureValue.IntegerValue(value.value.toInt())
        else -> throw PurePrimitiveException("numeric cast requires a numeric value")
    }

    fun toLong(value: PureValue): PureValue = when (value) {
        is PureValue.IntegerValue -> PureValue.LongValue(value.value.toLong())
        is PureValue.LongValue -> value
        is PureValue.DoubleValue -> PureValue.LongValue(value.value.toLong())
        else -> throw PurePrimitiveException("numeric cast requires a numeric value")
    }

    fun toDouble(value: PureValue): PureValue = when (value) {
        is PureValue.IntegerValue -> PureValue.DoubleValue(value.value.toDouble())
        is PureValue.LongValue -> PureValue.DoubleValue(value.value.toDouble())
        is PureValue.DoubleValue -> value
        else -> throw PurePrimitiveException("numeric cast requires a numeric value")
    }

    private fun mismatch(left: PureValue, right: PureValue): Nothing =
        throw PurePrimitiveException("numeric operand types do not match: $left and $right")
}

data class PureEvaluation(
    val outputs: Map<String, PureValue>,
    val steps: Int,
)

/**
 * Strict pure backend over [TypedGraph]. It exists to smoke-test the shared frontend and to serve as
 * the reference [TypedBackend] implementation. It evaluates only demanded nodes, preserving
 * language-level `if` and `&&`/`||` short-circuiting, and uses the supplied primitive runtimes.
 *
 * Ordinary [IrOperation.Apply] arguments are evaluated eagerly. Consequently this backend supports
 * strict primitives only; native primitives that ignore or defer parameters (for example ID's
 * `constant`) belong in a native backend, which flattens complete applications itself.
 */
class PureEvaluator(
    private val primitives: Map<String, PurePrimitive> = StandardScalarRuntime.primitives,
    private val inputs: Map<String, PureValue> = emptyMap(),
    private val limits: EvaluationLimits = EvaluationLimits.DEFAULT,
) : TypedBackend<PureEvaluation> {
    override fun lower(module: TypedModule): BackendResult<PureEvaluation> = evaluate(module)

    fun evaluate(module: TypedModule): BackendResult<PureEvaluation> {
        return try {
            BackendResult.success(EvaluationRun(module, primitives, inputs, limits).evaluate())
        } catch (failure: EvaluationFailure) {
            BackendResult.failure(listOf(failure.diagnostic))
        }
    }
}

fun evaluateLanguage(
    module: TypedModule,
    inputs: Map<String, PureValue> = emptyMap(),
    primitives: Map<String, PurePrimitive> = StandardScalarRuntime.primitives,
    limits: EvaluationLimits = EvaluationLimits.DEFAULT,
): BackendResult<PureEvaluation> = PureEvaluator(primitives, inputs, limits).evaluate(module)

private class EvaluationFailure(val diagnostic: Diagnostic) : RuntimeException(diagnostic.message)

private class EvaluationRun(
    private val module: TypedModule,
    private val primitives: Map<String, PurePrimitive>,
    private val inputs: Map<String, PureValue>,
    private val limits: EvaluationLimits,
) {
    private val graph = module.graph
    private val memo = HashMap<Int, PureValue>()
    private var steps = 0
    private var depth = 0

    fun evaluate(): PureEvaluation {
        for (input in graph.inputs) {
            val value = inputs[input.name]
                ?: fail(DiagnosticCode.E_INPUT_BINDING, input.span, "missing input binding '${input.name}'")
            if (!value.matches(input.type)) {
                fail(
                    DiagnosticCode.E_INPUT_BINDING,
                    input.span,
                    "input '${input.name}' value does not match ${input.type.display()}",
                )
            }
        }

        val outputs = LinkedHashMap<String, PureValue>()
        for (output in graph.outputs) {
            val value = evalRef(output.reference)
            if (!value.matches(output.type)) {
                fail(
                    DiagnosticCode.E_TARGET_CONTRACT,
                    output.span,
                    "output '${output.name}' produced a value that does not match ${output.type.display()}",
                )
            }
            outputs[output.name] = value
        }
        return PureEvaluation(outputs, steps)
    }

    private fun evalRef(reference: IrRef): PureValue = evalNode((reference as IrRef.Node).id)

    private fun evalNode(id: Int): PureValue {
        memo[id]?.let { return it }
        val node = graph.nodes.getOrNull(id)
            ?: fail(DiagnosticCode.E_TARGET_CONTRACT, SourceSpan.EMPTY, "missing graph node $id")
        if (++steps > limits.maxSteps) {
            fail(DiagnosticCode.E_LIMIT, node.span, "evaluation exceeded the step limit ${limits.maxSteps}")
        }
        if (++depth > limits.maxDepth) {
            depth--
            fail(DiagnosticCode.E_LIMIT, node.span, "evaluation exceeded the depth limit ${limits.maxDepth}")
        }
        try {
            val value = try {
                compute(node)
            } catch (failure: PurePrimitiveException) {
                fail(
                    DiagnosticCode.E_EVALUATION,
                    node.span,
                    failure.message ?: "primitive evaluation failed",
                )
            }
            memo[id] = value
            return value
        } finally {
            depth--
        }
    }

    private fun compute(node: TypedNode): PureValue = when (val operation = node.operation) {
        is IrOperation.Input -> inputs[operation.name]
            ?: fail(DiagnosticCode.E_INPUT_BINDING, node.span, "missing input binding '${operation.name}'")

        is IrOperation.Primitive -> {
            val primitive = primitives[operation.name]
                ?: fail(
                    DiagnosticCode.E_NATIVE_CONTRACT,
                    node.span,
                    "no pure runtime registered for primitive '${operation.name}'",
                )
            when (val type = node.type) {
                is LangType.FunctionType -> PureValue.FunctionValue.ofPrimitive(type, primitive)
                else -> {
                    // A named constant primitive (for example an empty domain object) has no
                    // parameters. The runtime supplies its value with an empty argument list.
                    val value = primitive.invoke(emptyList())
                    if (!value.matches(type)) {
                        fail(
                            DiagnosticCode.E_TARGET_CONTRACT,
                            node.span,
                            "constant primitive '${operation.name}' produced a value that does not match ${type.display()}",
                        )
                    }
                    value
                }
            }
        }

        is IrOperation.OperatorLiteral -> fail(
            DiagnosticCode.E_NATIVE_CONTRACT,
            node.span,
            "operator '${operation.id}' requires a native backend",
        )

        is IrOperation.Constant -> constant(operation.value)

        is IrOperation.ListLiteral -> {
            if (operation.elements.size > limits.maxListElements) {
                fail(
                    DiagnosticCode.E_LIMIT,
                    node.span,
                    "list exceeds the element limit ${limits.maxListElements}",
                )
            }
            PureValue.ListValue(operation.elements.map { evalRef(it) })
        }

        is IrOperation.Apply -> {
            val callee = evalRef(operation.callee)
            val argument = evalRef(operation.argument)
            val function = callee as? PureValue.FunctionValue
                ?: fail(
                    DiagnosticCode.E_NOT_CALLABLE,
                    node.span,
                    "attempted to apply a non-function value",
                )
            function.apply(argument)
        }

        is IrOperation.Choice -> {
            val condition = evalRef(operation.condition) as? PureValue.BooleanValue
                ?: fail(DiagnosticCode.E_TYPE, node.span, "choice condition is not Boolean")
            if (condition.value) evalRef(operation.whenTrue) else evalRef(operation.whenFalse)
        }

        is IrOperation.Arithmetic -> ScalarOps.arithmetic(
            operation.operator,
            evalRef(operation.left),
            evalRef(operation.right),
        )

        is IrOperation.Comparison -> ScalarOps.comparison(
            operation.operator,
            evalRef(operation.left),
            evalRef(operation.right),
        )

        is IrOperation.LogicalAnd -> {
            val left = evalRef(operation.left) as? PureValue.BooleanValue
                ?: fail(DiagnosticCode.E_TYPE, node.span, "left side of '&&' is not Boolean")
            if (!left.value) {
                PureValue.BooleanValue(false)
            } else {
                val right = evalRef(operation.right) as? PureValue.BooleanValue
                    ?: fail(DiagnosticCode.E_TYPE, node.span, "right side of '&&' is not Boolean")
                PureValue.BooleanValue(right.value)
            }
        }

        is IrOperation.LogicalOr -> {
            val left = evalRef(operation.left) as? PureValue.BooleanValue
                ?: fail(DiagnosticCode.E_TYPE, node.span, "left side of '||' is not Boolean")
            if (left.value) {
                PureValue.BooleanValue(true)
            } else {
                val right = evalRef(operation.right) as? PureValue.BooleanValue
                    ?: fail(DiagnosticCode.E_TYPE, node.span, "right side of '||' is not Boolean")
                PureValue.BooleanValue(right.value)
            }
        }

        is IrOperation.Concat -> ScalarOps.concat(evalRef(operation.left), evalRef(operation.right))
        is IrOperation.Negate -> ScalarOps.negate(evalRef(operation.operand))

        is IrOperation.Compose -> {
            val left = evalRef(operation.left) as? PureValue.FunctionValue
                ?: fail(DiagnosticCode.E_NOT_CALLABLE, node.span, "left side of '>>' is not a function")
            val right = evalRef(operation.right) as? PureValue.FunctionValue
                ?: fail(DiagnosticCode.E_NOT_CALLABLE, node.span, "right side of '>>' is not a function")
            if (left.signature.parameters.size != 1 || right.signature.parameters.size != 1) {
                fail(DiagnosticCode.E_FN_SIGNATURE, node.span, "composition requires unary functions")
            }
            val signature = LangType.FunctionType(
                listOf(left.signature.parameters[0]),
                right.signature.result,
            )
            PureValue.FunctionValue.ofSingleArgument(signature) { argument ->
                val intermediate = applyPure(left, argument)
                applyPure(right, intermediate)
            }
        }

        is IrOperation.Construct -> {
            val arguments = operation.arguments.map { argument ->
                when (argument) {
                    is IrStaticArgument.Text -> PureValue.StringValue(argument.value)
                    is IrStaticArgument.Integer -> PureValue.IntegerValue(argument.value)
                }
            }
            val typeName = (node.type as? LangType.Nominal)?.name
                ?: fail(DiagnosticCode.E_TARGET_CONTRACT, node.span, "object literal is not nominal")
            PureValue.ObjectValue(operation.kind, typeName, arguments)
        }
    }

    private fun constant(value: LiteralValue): PureValue = when (value) {
        is LiteralValue.BooleanLiteral -> PureValue.BooleanValue(value.value)
        is LiteralValue.IntegerLiteral -> PureValue.IntegerValue(value.value)
        is LiteralValue.LongLiteral -> PureValue.LongValue(value.value)
        is LiteralValue.DoubleLiteral -> PureValue.DoubleValue(value.value)
        is LiteralValue.StringLiteral -> PureValue.StringValue(value.value)
    }

    private fun fail(code: DiagnosticCode, span: SourceSpan, message: String): Nothing =
        throw EvaluationFailure(Diagnostic.error(code, span, message))
}

/** Runtime implementations for every primitive in [StandardScalarEnvironment]. */
object StandardScalarRuntime {
    val primitives: Map<String, PurePrimitive> = buildMap {
        put("add", arithmetic(IrArithmeticOperator.ADD))
        put("sub", arithmetic(IrArithmeticOperator.SUBTRACT))
        put("mul", arithmetic(IrArithmeticOperator.MULTIPLY))
        put("divide", arithmetic(IrArithmeticOperator.DIVIDE))
        put("mod", arithmetic(IrArithmeticOperator.MODULO))
        put("min", binary { left, right -> ScalarOps.minimum(left, right) })
        put("max", binary { left, right -> ScalarOps.maximum(left, right) })
        put("increment", unary(ScalarOps::increment))
        put("decrement", unary(ScalarOps::decrement))
        put("eq", binary { left, right -> PureValue.BooleanValue(valuesEqual(left, right)) })
        put("ne", binary { left, right -> PureValue.BooleanValue(!valuesEqual(left, right)) })
        put("lt", comparison(IrComparisonOperator.LESS))
        put("le", comparison(IrComparisonOperator.LESS_EQUAL))
        put("gt", comparison(IrComparisonOperator.GREATER))
        put("ge", comparison(IrComparisonOperator.GREATER_EQUAL))
        put("not", unary { value ->
            val boolean = value as? PureValue.BooleanValue
                ?: throw PurePrimitiveException("'not' requires Boolean")
            PureValue.BooleanValue(!boolean.value)
        })
        put("toInteger", unary(ScalarOps::toInteger))
        put("toLong", unary(ScalarOps::toLong))
        put("toDouble", unary(ScalarOps::toDouble))
        put("length", unary { value -> PureValue.IntegerValue(list(value).size) })
        put("isEmpty", unary { value -> PureValue.BooleanValue(list(value).isEmpty()) })
        put("get", binary { listValue, index ->
            val list = list(listValue)
            val position = integer(index)
            if (position !in list.indices) {
                throw PurePrimitiveException("list index $position is out of bounds for size ${list.size}")
            }
            list[position]
        })
        put("getOr", PurePrimitive { arguments ->
            requireArity(arguments, 3, "getOr")
            val list = list(arguments[0])
            val position = integer(arguments[1])
            if (position in list.indices) list[position] else arguments[2]
        })
        put("append", binary { listValue, element ->
            PureValue.ListValue(list(listValue) + element)
        })
        put("slice", PurePrimitive { arguments ->
            requireArity(arguments, 3, "slice")
            val list = list(arguments[0])
            val start = integer(arguments[1])
            val end = integer(arguments[2])
            if (start < 0 || end < start || end > list.size) {
                throw PurePrimitiveException("invalid slice [$start, $end) for size ${list.size}")
            }
            PureValue.ListValue(list.subList(start, end))
        })
        put("map", binary { function, listValue ->
            val values = list(listValue)
            PureValue.ListValue(values.map { applyPure(function, it) })
        })
        put("filter", binary { predicate, listValue ->
            val values = list(listValue)
            PureValue.ListValue(
                values.filter { element ->
                    val result = applyPure(predicate, element) as? PureValue.BooleanValue
                        ?: throw PurePrimitiveException("filter predicate did not return Boolean")
                    result.value
                },
            )
        })
        put("reduce", PurePrimitive { arguments ->
            requireArity(arguments, 3, "reduce")
            val function = arguments[0]
            val values = list(arguments[1])
            var accumulator = arguments[2]
            for (element in values) accumulator = applyPure(applyPure(function, accumulator), element)
            accumulator
        })
        put("identity", unary { it })
        put("flip", unary { value -> flip(value) })
        put("call0", unary { value ->
            val function = value as? PureValue.FunctionValue
                ?: throw PurePrimitiveException("call0 requires a function")
            function.invokeZero()
        })
        put("allOf", binary { first, second -> predicateCombination(first, second, conjunction = true) })
        put("anyOf", binary { first, second -> predicateCombination(first, second, conjunction = false) })
        put("negatePred", unary { value -> negatePredicate(value) })
        put("pipe2", PurePrimitive { arguments ->
            requireArity(arguments, 3, "pipe2")
            val first = arguments[0] as? PureValue.FunctionValue
                ?: throw PurePrimitiveException("pipe2 requires function arguments")
            val second = arguments[1] as? PureValue.FunctionValue
                ?: throw PurePrimitiveException("pipe2 requires function arguments")
            val combiner = arguments[2] as? PureValue.FunctionValue
                ?: throw PurePrimitiveException("pipe2 requires a binary combiner")
            if (first.signature.parameters.size != 1 || second.signature.parameters.size != 1) {
                throw PurePrimitiveException("pipe2 branches must be unary functions")
            }
            val signature = LangType.FunctionType(
                listOf(first.signature.parameters[0]),
                combiner.signature.result,
            )
            PureValue.FunctionValue.ofSingleArgument(signature) { argument ->
                val left = applyPure(first, argument)
                val right = applyPure(second, argument)
                applyPure(applyPure(combiner, left), right)
            }
        })
    }

    fun runtime(): Map<String, PurePrimitive> = primitives

    private fun arithmetic(operator: IrArithmeticOperator): PurePrimitive =
        binary { left, right -> ScalarOps.arithmetic(operator, left, right) }

    private fun comparison(operator: IrComparisonOperator): PurePrimitive =
        binary { left, right -> ScalarOps.comparison(operator, left, right) }

    private fun binary(operation: (PureValue, PureValue) -> PureValue): PurePrimitive =
        PurePrimitive { arguments ->
            requireArity(arguments, 2, "binary primitive")
            operation(arguments[0], arguments[1])
        }

    private fun unary(operation: (PureValue) -> PureValue): PurePrimitive =
        PurePrimitive { arguments ->
            requireArity(arguments, 1, "unary primitive")
            operation(arguments[0])
        }

    private fun requireArity(arguments: List<PureValue>, expected: Int, name: String) {
        if (arguments.size != expected) {
            throw PurePrimitiveException("$name expected $expected arguments, found ${arguments.size}")
        }
    }

    private fun list(value: PureValue): List<PureValue> = when (value) {
        is PureValue.ListValue -> value.elements
        else -> throw PurePrimitiveException("expected a List, found $value")
    }

    private fun integer(value: PureValue): Int = when (value) {
        is PureValue.IntegerValue -> value.value
        else -> throw PurePrimitiveException("expected an Integer, found $value")
    }

    private fun flip(value: PureValue): PureValue {
        val function = value as? PureValue.FunctionValue
            ?: throw PurePrimitiveException("flip requires a function")
        val parameters = function.signature.parameters
        if (parameters.size < 2) {
            throw PurePrimitiveException("flip requires a function with at least two parameters")
        }
        val swapped = ArrayList(parameters)
        val first = swapped[0]
        swapped[0] = swapped[1]
        swapped[1] = first
        val signature = LangType.FunctionType(swapped, function.signature.result)
        return PureValue.FunctionValue(
            signature,
            emptyList(),
            swapped.size,
        ) { arguments ->
            val reordered = ArrayList(arguments)
            val argument = reordered[0]
            reordered[0] = reordered[1]
            reordered[1] = argument
            var current: PureValue = function
            for (item in reordered) current = applyPure(current, item)
            current
        }
    }

    private fun predicateCombination(
        first: PureValue,
        second: PureValue,
        conjunction: Boolean,
    ): PureValue {
        val left = first as? PureValue.FunctionValue
            ?: throw PurePrimitiveException("predicate combination requires functions")
        val right = second as? PureValue.FunctionValue
            ?: throw PurePrimitiveException("predicate combination requires functions")
        val signature = left.signature
        if (signature.parameters.size != 1) {
            throw PurePrimitiveException("predicate must be unary")
        }
        return PureValue.FunctionValue.ofSingleArgument(signature) { argument ->
            val leftValue = applyPure(left, argument) as? PureValue.BooleanValue
                ?: throw PurePrimitiveException("predicate did not return Boolean")
            val result = if ((conjunction && !leftValue.value) || (!conjunction && leftValue.value)) {
                !conjunction
            } else {
                val rightValue = applyPure(right, argument) as? PureValue.BooleanValue
                    ?: throw PurePrimitiveException("predicate did not return Boolean")
                rightValue.value
            }
            PureValue.BooleanValue(result)
        }
    }

    private fun negatePredicate(value: PureValue): PureValue {
        val predicate = value as? PureValue.FunctionValue
            ?: throw PurePrimitiveException("negatePred requires a function")
        if (predicate.signature.parameters.size != 1) {
            throw PurePrimitiveException("predicate must be unary")
        }
        return PureValue.FunctionValue.ofSingleArgument(predicate.signature) { argument ->
            val result = applyPure(predicate, argument) as? PureValue.BooleanValue
                ?: throw PurePrimitiveException("predicate did not return Boolean")
            PureValue.BooleanValue(!result.value)
        }
    }
}
