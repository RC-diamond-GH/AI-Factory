package com.rcdiamond.aifactory.lang

internal data class LoweringResult(val graph: TypedGraph?, val diagnostics: List<Diagnostic>)

private class LoweringFailure(val diagnostic: Diagnostic) : RuntimeException(diagnostic.message)

/**
 * Lowers checked expressions to the target-independent [TypedGraph].
 *
 * Only reachable definitions are emitted, in demand order, so every reference points to an earlier
 * node. A `let` is an alias and never creates a dedicated node; literals and native calls are not
 * deduplicated beyond that. Node ids are therefore deterministic for identical source and
 * environment.
 */
internal class GraphBuilder(
    private val program: Program,
    private val checked: CheckedProgram,
    private val limits: LanguageLimits,
) {
    private val nodes = ArrayList<TypedNode>()
    private val inputs = ArrayList<TypedInput>()
    private val outputs = ArrayList<TypedOutput>()
    private val inputReferences = LinkedHashMap<String, IrRef>()
    private val letReferences = LinkedHashMap<String, IrRef>()
    private val loweringLets = HashSet<String>()
    private val inputDeclarations: Map<String, InputDeclaration> =
        program.declarations.filterIsInstance<InputDeclaration>().associateBy { it.name }
    private val letDeclarations: Map<String, LetDeclaration> =
        program.declarations.filterIsInstance<LetDeclaration>().associateBy { it.name }

    fun build(): LoweringResult {
        return try {
            for (declaration in program.declarations) {
                if (declaration is OutputDeclaration) {
                    val reference = lowerExpression(declaration.expression)
                    outputs.add(
                        TypedOutput(
                            declaration.name,
                            checked.outputTypes[declaration]
                                ?: error("no output type for '${declaration.name}'"),
                            reference,
                            declaration.span,
                        ),
                    )
                }
            }
            LoweringResult(TypedGraph(nodes.toList(), inputs.toList(), outputs.toList()), emptyList())
        } catch (failure: LoweringFailure) {
            LoweringResult(null, listOf(failure.diagnostic))
        }
    }

    private fun lowerExpression(expression: Expr): IrRef {
        return when (expression) {
            is Expr.BooleanLiteral -> emit(
                LangTypes.BOOLEAN,
                IrOperation.Constant(LiteralValue.Boolean(expression.value)),
                expression.span,
            )

            is Expr.IntegerLiteral -> emit(
                LangTypes.INTEGER,
                IrOperation.Constant(LiteralValue.Integer(expression.value)),
                expression.span,
            )

            is Expr.LongLiteral -> emit(
                LangTypes.LONG,
                IrOperation.Constant(LiteralValue.Long(expression.value)),
                expression.span,
            )

            is Expr.DoubleLiteral -> emit(
                LangTypes.DOUBLE,
                IrOperation.Constant(LiteralValue.Double(expression.value)),
                expression.span,
            )

            is Expr.StringLiteral -> emit(
                LangTypes.STRING,
                IrOperation.Constant(LiteralValue.String(expression.value)),
                expression.span,
            )

            is Expr.Name -> lowerName(expression)
            is Expr.ListLiteral -> {
                val elements = expression.elements.map { lowerExpression(it) }
                emit(checked.expressionType(expression), IrOperation.ListLiteral(elements), expression.span)
            }

            is Expr.Call -> lowerCall(expression)
            is Expr.If -> {
                val condition = lowerExpression(expression.condition)
                val whenTrue = lowerExpression(expression.thenBranch)
                val whenFalse = lowerExpression(expression.elseBranch)
                emit(
                    checked.expressionType(expression),
                    IrOperation.Choice(condition, whenTrue, whenFalse),
                    expression.span,
                )
            }

            is Expr.Binary -> lowerBinary(expression)
            is Expr.Negate -> {
                val operand = lowerExpression(expression.operand)
                emit(checked.expressionType(expression), IrOperation.Negate(operand), expression.span)
            }

            is Expr.Construct -> emit(
                checked.expressionType(expression),
                IrOperation.Construct(
                    expression.kind,
                    expression.arguments.map { argument ->
                        when (argument) {
                            is ConstructorArgument.Text -> IrStaticArgument.Text(argument.value)
                            is ConstructorArgument.Integer -> IrStaticArgument.Integer(argument.value)
                        }
                    },
                ),
                expression.span,
            )

            is Expr.OperatorReference -> emit(
                checked.expressionType(expression),
                IrOperation.OperatorLiteral(expression.id),
                expression.span,
            )
        }
    }

    private fun lowerName(expression: Expr.Name): IrRef {
        val input = findInput(expression.name)
        if (input != null) return lowerInput(input)

        val let = findLet(expression.name)
        if (let != null) return lowerLet(let)

        // A primitive occurrence is a fresh value node with the occurrence's instantiated signature.
        return emit(
            checked.expressionType(expression),
            IrOperation.Primitive(expression.name),
            expression.span,
        )
    }

    private fun lowerInput(declaration: InputDeclaration): IrRef {
        inputReferences[declaration.name]?.let { return it }
        val reference = emit(
            checked.inputTypes[declaration] ?: error("no type for input '${declaration.name}'"),
            IrOperation.Input(declaration.name),
            declaration.span,
        )
        inputReferences[declaration.name] = reference
        inputs.add(
            TypedInput(
                declaration.name,
                checked.inputTypes.getValue(declaration),
                (reference as IrRef.Node).id,
                declaration.span,
            ),
        )
        return reference
    }

    private fun lowerLet(declaration: LetDeclaration): IrRef {
        letReferences[declaration.name]?.let { return it }
        if (!loweringLets.add(declaration.name)) {
            fail(
                DiagnosticCode.E_CYCLE,
                declaration.nameSpan,
                "immediate definition cycle while lowering '${declaration.name}'",
            )
        }
        return try {
            val reference = lowerExpression(declaration.expression)
            letReferences[declaration.name] = reference
            reference
        } finally {
            loweringLets.remove(declaration.name)
        }
    }

    private fun lowerCall(expression: Expr.Call): IrRef {
        var reference = lowerExpression(expression.callee)
        var currentType = checked.expressionType(expression.callee)
        for (argument in expression.arguments) {
            val argumentReference = lowerExpression(argument)
            val argumentType = checked.expressionType(argument)
            val resultType = applyResultType(currentType, argumentType, expression.span)
            reference = emit(resultType, IrOperation.Apply(reference, argumentReference), expression.span)
            currentType = resultType
        }
        return reference
    }

    private fun lowerBinary(expression: Expr.Binary): IrRef {
        val left = lowerExpression(expression.left)
        val right = lowerExpression(expression.right)
        val type = checked.expressionType(expression)
        val operation = when (expression.operator) {
            BinaryOperator.PLUS -> IrOperation.Arithmetic(IrArithmeticOperator.ADD, left, right)
            BinaryOperator.MINUS -> IrOperation.Arithmetic(IrArithmeticOperator.SUBTRACT, left, right)
            BinaryOperator.STAR -> IrOperation.Arithmetic(IrArithmeticOperator.MULTIPLY, left, right)
            BinaryOperator.SLASH -> IrOperation.Arithmetic(IrArithmeticOperator.DIVIDE, left, right)
            BinaryOperator.PERCENT -> IrOperation.Arithmetic(IrArithmeticOperator.MODULO, left, right)
            BinaryOperator.EQUAL -> IrOperation.Comparison(IrComparisonOperator.EQUAL, left, right)
            BinaryOperator.NOT_EQUAL -> IrOperation.Comparison(IrComparisonOperator.NOT_EQUAL, left, right)
            BinaryOperator.LESS -> IrOperation.Comparison(IrComparisonOperator.LESS, left, right)
            BinaryOperator.LESS_EQUAL -> IrOperation.Comparison(IrComparisonOperator.LESS_EQUAL, left, right)
            BinaryOperator.GREATER -> IrOperation.Comparison(IrComparisonOperator.GREATER, left, right)
            BinaryOperator.GREATER_EQUAL -> IrOperation.Comparison(IrComparisonOperator.GREATER_EQUAL, left, right)
            BinaryOperator.AND -> IrOperation.LogicalAnd(left, right)
            BinaryOperator.OR -> IrOperation.LogicalOr(left, right)
            BinaryOperator.CONCAT -> IrOperation.Concat(left, right)
            BinaryOperator.COMPOSE -> IrOperation.Compose(left, right)
        }
        return emit(type, operation, expression.span)
    }

    private fun applyResultType(callee: LangType, argument: LangType, span: SourceSpan): LangType {
        if (callee !is LangType.FunctionType) {
            error("checked call has non-function callee ${callee.display()} at $span")
        }
        if (callee.parameters.isEmpty()) {
            error("checked call applies an argument to a zero-parameter function at $span")
        }
        // Unify is finished; the argument type is already the parameter type, but retaining the
        // parameter in the remaining signature keeps the graph's intermediate types exact.
        return if (callee.parameters.size == 1) {
            callee.result
        } else {
            LangType.FunctionType(callee.parameters.drop(1), callee.result)
        }
    }

    private fun emit(type: LangType, operation: IrOperation, span: SourceSpan): IrRef {
        if (!type.isConcrete()) {
            error("attempted to emit unresolved type ${type.display()} at $span")
        }
        if (nodes.size >= limits.maxNodes) {
            fail(
                DiagnosticCode.E_LIMIT,
                span,
                "typed graph node count exceeds the limit ${limits.maxNodes}",
            )
        }
        val node = TypedNode(nodes.size, type, operation, span)
        nodes.add(node)
        return IrRef.Node(node.id)
    }

    private fun findInput(name: String): InputDeclaration? = inputDeclarations[name]

    private fun findLet(name: String): LetDeclaration? = letDeclarations[name]

    private fun fail(code: DiagnosticCode, span: SourceSpan, message: String): Nothing =
        throw LoweringFailure(Diagnostic.error(code, span, message))
}
