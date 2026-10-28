package com.rcdiamond.aifactory.lang

import java.util.IdentityHashMap

internal data class ResolveResult(
    val inputs: Map<String, InputDeclaration>,
    val lets: Map<String, LetDeclaration>,
    val outputs: Map<String, OutputDeclaration>,
    val dependencies: Map<String, List<String>>,
    val diagnostics: List<Diagnostic>,
)

/**
 * Collects declaration namespaces, checks duplicate/shadowing rules and immediate let cycles.
 *
 * Forward references are allowed: all names are collected before any expression is checked. Output
 * ports live in their own namespace and are never visible as variables.
 */
internal class NameResolver(
    private val program: Program,
    private val environment: LanguageEnvironment,
    private val limits: LanguageLimits,
) {
    private val inputs = LinkedHashMap<String, InputDeclaration>()
    private val lets = LinkedHashMap<String, LetDeclaration>()
    private val outputs = LinkedHashMap<String, OutputDeclaration>()
    private val dependencies = LinkedHashMap<String, List<String>>()
    private val diagnostics = ArrayList<Diagnostic>()

    fun resolve(): ResolveResult {
        for (declaration in program.declarations) {
            when (declaration) {
                is InputDeclaration -> declareBinding(declaration)
                is LetDeclaration -> declareBinding(declaration)
                is OutputDeclaration -> declareOutput(declaration)
            }
        }

        if (outputs.isEmpty()) {
            add(
                Diagnostic.error(
                    DiagnosticCode.E_SYNTAX,
                    program.span,
                    "program must declare at least one output",
                ),
            )
        }

        for (let in lets.values) {
            val names = LinkedHashSet<String>()
            collectNames(let.expression, names)
            val dependenciesForLet = ArrayList<String>()
            for (name in names) {
                if (name in lets) {
                    dependenciesForLet.add(name)
                } else if (name !in inputs && environment.primitive(name) == null) {
                    add(
                        Diagnostic.error(
                            DiagnosticCode.E_UNKNOWN_NAME,
                            findNameSpan(let.expression, name) ?: let.nameSpan,
                            "unknown name '$name'",
                        ),
                    )
                }
            }
            dependencies[let.name] = dependenciesForLet
        }

        for (output in outputs.values) {
            val names = LinkedHashSet<String>()
            collectNames(output.expression, names)
            for (name in names) {
                if (name !in lets && name !in inputs && environment.primitive(name) == null) {
                    add(
                        Diagnostic.error(
                            DiagnosticCode.E_UNKNOWN_NAME,
                            findNameSpan(output.expression, name) ?: output.nameSpan,
                            "unknown name '$name'",
                        ),
                    )
                }
            }
        }

        detectCycles()
        return ResolveResult(inputs, lets, outputs, dependencies, diagnostics)
    }

    private fun declareBinding(declaration: Declaration) {
        val existing = inputs[declaration.name] ?: lets[declaration.name]
        if (existing != null) {
            add(
                Diagnostic(
                    DiagnosticCode.E_DUPLICATE_NAME,
                    Severity.ERROR,
                    "name '${declaration.name}' is already declared",
                    declaration.nameSpan,
                    listOf(RelatedDiagnostic("first declaration of '${declaration.name}'", existing.nameSpan)),
                ),
            )
            return
        }
        val primitive = environment.primitive(declaration.name)
        if (primitive != null) {
            add(
                Diagnostic.error(
                    DiagnosticCode.E_DUPLICATE_NAME,
                    declaration.nameSpan,
                    "name '${declaration.name}' shadows a built-in primitive and cannot be declared",
                ),
            )
            return
        }
        when (declaration) {
            is InputDeclaration -> inputs[declaration.name] = declaration
            is LetDeclaration -> lets[declaration.name] = declaration
            is OutputDeclaration -> error("output passed to declareBinding")
        }
    }

    private fun declareOutput(declaration: OutputDeclaration) {
        val existing = outputs[declaration.name]
        if (existing != null) {
            add(
                Diagnostic(
                    DiagnosticCode.E_DUPLICATE_NAME,
                    Severity.ERROR,
                    "output port '${declaration.name}' is already declared",
                    declaration.nameSpan,
                    listOf(RelatedDiagnostic("first declaration of '${declaration.name}'", existing.nameSpan)),
                ),
            )
            return
        }
        outputs[declaration.name] = declaration
    }

    private fun collectNames(expression: Expr, into: MutableSet<String>) {
        val pending = ArrayDeque<Expr>()
        pending.addLast(expression)
        while (pending.isNotEmpty()) {
            when (val node = pending.removeLast()) {
                is Expr.Name -> into.add(node.name)
                is Expr.Call -> {
                    pending.addLast(node.callee)
                    for (argument in node.arguments.asReversed()) pending.addLast(argument)
                }

                is Expr.If -> {
                    pending.addLast(node.condition)
                    pending.addLast(node.thenBranch)
                    pending.addLast(node.elseBranch)
                }

                is Expr.Binary -> {
                    pending.addLast(node.left)
                    pending.addLast(node.right)
                }

                is Expr.Negate -> pending.addLast(node.operand)
                is Expr.ListLiteral -> {
                    for (element in node.elements.asReversed()) pending.addLast(element)
                }

                is Expr.BooleanLiteral,
                is Expr.IntegerLiteral,
                is Expr.LongLiteral,
                is Expr.DoubleLiteral,
                is Expr.StringLiteral,
                is Expr.Construct,
                is Expr.OperatorReference -> Unit
            }
        }
    }

    private fun findNameSpan(expression: Expr, name: String): SourceSpan? {
        val pending = ArrayDeque<Expr>()
        pending.addLast(expression)
        while (pending.isNotEmpty()) {
            when (val node = pending.removeLast()) {
                is Expr.Name -> if (node.name == name) return node.span
                is Expr.Call -> {
                    pending.addLast(node.callee)
                    for (argument in node.arguments.asReversed()) pending.addLast(argument)
                }

                is Expr.If -> {
                    pending.addLast(node.condition)
                    pending.addLast(node.thenBranch)
                    pending.addLast(node.elseBranch)
                }

                is Expr.Binary -> {
                    pending.addLast(node.left)
                    pending.addLast(node.right)
                }

                is Expr.Negate -> pending.addLast(node.operand)
                is Expr.ListLiteral -> {
                    for (element in node.elements.asReversed()) pending.addLast(element)
                }

                else -> Unit
            }
        }
        return null
    }

    private fun detectCycles() {
        val colors = HashMap<String, Int>() // 0 unseen, 1 on stack, 2 done
        val reported = HashSet<Set<String>>()

        for (root in lets.keys) {
            if ((colors[root] ?: 0) != 0) continue
            val path = ArrayList<String>()
            val stack = ArrayDeque<Frame>()
            colors[root] = 1
            path.add(root)
            stack.addLast(Frame(root, dependencies[root] ?: emptyList()))
            while (stack.isNotEmpty()) {
                val frame = stack.last()
                if (frame.next >= frame.dependencies.size) {
                    colors[frame.name] = 2
                    if (path.isNotEmpty()) path.removeAt(path.size - 1)
                    stack.removeLast()
                    continue
                }
                val dependency = frame.dependencies[frame.next]
                frame.next++
                when (colors[dependency] ?: 0) {
                    0 -> {
                        if (path.size >= limits.maxDependencyDepth) {
                            add(
                                Diagnostic.error(
                                    DiagnosticCode.E_LIMIT,
                                    lets[dependency]?.nameSpan ?: program.span,
                                    "dependency chain exceeds the limit ${limits.maxDependencyDepth}",
                                ),
                            )
                            colors[dependency] = 2
                        } else {
                            colors[dependency] = 1
                            path.add(dependency)
                            stack.addLast(Frame(dependency, dependencies[dependency] ?: emptyList()))
                        }
                    }

                    1 -> {
                        val cycleStart = path.indexOf(dependency)
                        if (cycleStart >= 0) {
                            val chain = ArrayList<String>()
                            for (position in cycleStart until path.size) chain.add(path[position])
                            chain.add(dependency)
                            val key = chain.dropLast(1).toSet()
                            if (reported.add(key)) {
                                val first = lets[chain.first()]
                                add(
                                    Diagnostic(
                                        DiagnosticCode.E_CYCLE,
                                        Severity.ERROR,
                                        "immediate definition cycle: ${chain.joinToString(" -> ")}",
                                        first?.nameSpan ?: program.span,
                                        chain.dropLast(1).mapNotNull { name ->
                                            lets[name]?.let {
                                                RelatedDiagnostic(
                                                    "let '$name' participates in this cycle",
                                                    it.nameSpan,
                                                )
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun add(diagnostic: Diagnostic) {
        if (diagnostics.size >= limits.maxDiagnostics) return
        diagnostics.add(diagnostic)
    }

    private class Frame(val name: String, val dependencies: List<String>) {
        var next: Int = 0
    }
}

/** Fully resolved types and expression/declaration type tables consumed by graph lowering. */
internal data class CheckedProgram(
    val inputTypes: Map<InputDeclaration, LangType>,
    val letTypes: Map<LetDeclaration, LangType>,
    val outputTypes: Map<OutputDeclaration, LangType>,
    val expressionTypes: Map<Expr, LangType>,
) {
    fun expressionType(expression: Expr): LangType =
        expressionTypes[expression] ?: error("no inferred type for expression at ${expression.span}")

    fun declarationType(declaration: Declaration): LangType = when (declaration) {
        is InputDeclaration -> inputTypes[declaration] ?: error("no type for input '${declaration.name}'")
        is LetDeclaration -> letTypes[declaration] ?: error("no type for let '${declaration.name}'")
        is OutputDeclaration -> outputTypes[declaration] ?: error("no type for output '${declaration.name}'")
    }
}

internal data class CheckResult(val checked: CheckedProgram?, val diagnostics: List<Diagnostic>)

private class TypeFailure(val diagnostic: Diagnostic) : RuntimeException(diagnostic.message)

/**
 * Monomorphic inference with fresh instantiation of environment schemes, exact unification and no
 * implicit conversions. Type errors abort the check (first error wins) so a failed check never
 * produces a partial typed graph.
 */
internal class TypeChecker(
    private val program: Program,
    private val environment: LanguageEnvironment,
    private val resolution: ResolveResult,
    private val limits: LanguageLimits,
) {
    private val substitutions = HashMap<Int, LangType>()
    private val constraints = ArrayList<ConstraintRecord>()
    private val expressionTypes = LinkedHashMap<Expr, LangType>()
    private val inputTypes = LinkedHashMap<InputDeclaration, LangType>()
    private val letTypes = LinkedHashMap<LetDeclaration, LangType>()
    private val outputTypes = LinkedHashMap<OutputDeclaration, LangType>()
    private val visitingLets = HashSet<String>()
    private var nextVariable = 0

    fun check(): CheckResult {
        return try {
            for (input in resolution.inputs.values) {
                inputTypes[input] = lowerType(input.type)
            }
            for (let in resolution.lets.values) {
                inferLet(let)
            }
            for (output in resolution.outputs.values) {
                val inferred = infer(output.expression)
                val declared = output.annotation?.let { annotation ->
                    val expected = lowerType(annotation)
                    unify(expected, inferred, output.nameSpan)
                    expected
                }
                outputTypes[output] = declared ?: inferred
            }
            val checked = finalizeTypes()
            CheckResult(checked, emptyList())
        } catch (failure: TypeFailure) {
            CheckResult(null, listOf(failure.diagnostic))
        }
    }

    private fun inferLet(declaration: LetDeclaration): LangType {
        letTypes[declaration]?.let { return it }
        if (!visitingLets.add(declaration.name)) {
            fail(DiagnosticCode.E_CYCLE, declaration.nameSpan, "immediate definition cycle at '${declaration.name}'")
        }
        return try {
            val inferred = infer(declaration.expression)
            val type = declaration.annotation?.let { annotation ->
                val expected = lowerType(annotation)
                unify(expected, inferred, declaration.nameSpan)
                expected
            } ?: inferred
            letTypes[declaration] = type
            type
        } finally {
            visitingLets.remove(declaration.name)
        }
    }

    private fun infer(expression: Expr): LangType {
        val type = when (expression) {
            is Expr.BooleanLiteral -> LangTypes.BOOLEAN
            is Expr.IntegerLiteral -> LangTypes.INTEGER
            is Expr.LongLiteral -> LangTypes.LONG
            is Expr.DoubleLiteral -> LangTypes.DOUBLE
            is Expr.StringLiteral -> LangTypes.STRING
            is Expr.Name -> inferName(expression)
            is Expr.ListLiteral -> {
                var element: LangType? = null
                for (item in expression.elements) {
                    val itemType = infer(item)
                    if (element == null) {
                        element = itemType
                    } else {
                        unify(element, itemType, item.span)
                    }
                }
                LangType.ListType(element ?: freshVariable())
            }

            is Expr.Call -> inferCall(expression)

            is Expr.If -> {
                val condition = infer(expression.condition)
                unify(LangTypes.BOOLEAN, condition, expression.condition.span)
                val thenType = infer(expression.thenBranch)
                val elseType = infer(expression.elseBranch)
                unify(thenType, elseType, expression.elseBranch.span)
                thenType
            }

            is Expr.Binary -> inferBinary(expression)
            is Expr.Negate -> {
                val numeric = freshConstrained(TypeConstraint.Numeric, expression.span)
                unify(numeric, infer(expression.operand), expression.operand.span)
                numeric
            }

            is Expr.Construct -> inferConstruct(expression)
            is Expr.OperatorReference -> {
                val catalog = environment.operatorCatalog
                    ?: fail(
                        DiagnosticCode.E_NATIVE_CONTRACT,
                        expression.span,
                        "the environment does not provide an operator catalog for 'op'",
                    )
                val contract = catalog.resolve(expression.id)
                    ?: fail(
                        DiagnosticCode.E_NATIVE_CONTRACT,
                        expression.span,
                        "operator '${expression.id}' has no contract in the target environment",
                    )
                instantiate(contract.scheme, expression.span)
            }
        }
        expressionTypes[expression] = type
        return type
    }

    private fun inferName(expression: Expr.Name): LangType {
        resolution.inputs[expression.name]?.let { declaration ->
            return inputTypes[declaration] ?: fail(
                DiagnosticCode.E_UNKNOWN_NAME,
                expression.span,
                "input '${expression.name}' has no type",
            )
        }
        resolution.lets[expression.name]?.let { declaration -> return inferLet(declaration) }
        val primitive = environment.primitive(expression.name)
            ?: fail(DiagnosticCode.E_UNKNOWN_NAME, expression.span, "unknown name '${expression.name}'")
        val scheme = primitive.scheme
            ?: fail(
                DiagnosticCode.E_UNRESOLVED_TYPE,
                expression.span,
                "primitive '${expression.name}' needs argument types before its signature is known",
            )
        return instantiate(scheme, expression.span)
    }

    private fun inferCall(expression: Expr.Call): LangType {
        val calleeExpression = expression.callee
        if (calleeExpression is Expr.Name) {
            val contract = environment.primitive(calleeExpression.name)
            val rule = contract?.rule
            if (rule != null) return inferRuleCall(expression, calleeExpression, rule)
        }

        var callee = infer(calleeExpression)
        for (argument in expression.arguments) {
            val argumentType = infer(argument)
            callee = applyFunction(callee, argumentType, expression.span, argument.span)
        }
        return callee
    }

    /**
     * Types an overloaded primitive application. Arguments are consumed until the rule fires; the
     * callee's occurrence type becomes a function type from those consumed arguments to the rule's
     * result, so graph lowering still sees one concrete primitive node. Remaining arguments are
     * ordinary applications of the result function.
     */
    private fun inferRuleCall(
        expression: Expr.Call,
        calleeExpression: Expr.Name,
        rule: PrimitiveTypeRule,
    ): LangType {
        val context = object : PrimitiveTypeContext {
            override fun resolve(type: LangType): LangType = this@TypeChecker.resolve(type)

            override fun freshVariable(): LangType.Variable = this@TypeChecker.freshVariable()

            override fun constrain(type: LangType, constraint: TypeConstraint) {
                constraints.add(ConstraintRecord(type, constraint, expression.span))
            }

            override fun unify(expected: LangType, actual: LangType) {
                this@TypeChecker.unify(expected, actual, expression.span)
            }
        }

        val consumed = ArrayList<LangType>()
        var result: LangType? = null
        var calleeType: LangType.FunctionType? = null
        for (argument in expression.arguments) {
            val argumentType = infer(argument)
            if (result == null) {
                consumed.add(argumentType)
                val computed = try {
                    rule.typeAfter(consumed.toList(), context)
                } catch (failure: PrimitiveTypeException) {
                    fail(failure.code, expression.span, failure.message ?: "primitive typing rule failed")
                }
                if (computed != null) {
                    result = computed
                    calleeType = LangType.FunctionType(consumed.toList(), computed)
                }
            } else {
                result = applyFunction(result!!, argumentType, expression.span, argument.span)
            }
        }
        val finalResult = result ?: fail(
            DiagnosticCode.E_UNRESOLVED_TYPE,
            expression.span,
            "cannot determine the type of '${calleeExpression.name}' from its arguments",
        )
        expressionTypes[calleeExpression] = calleeType
            ?: error("rule application produced a result without a callee type")
        return finalResult
    }

    private fun inferBinary(expression: Expr.Binary): LangType {
        val left = infer(expression.left)
        val right = infer(expression.right)
        return when (expression.operator) {
            BinaryOperator.PLUS,
            BinaryOperator.MINUS,
            BinaryOperator.STAR,
            BinaryOperator.SLASH,
            BinaryOperator.PERCENT -> {
                val numeric = freshConstrained(TypeConstraint.Numeric, expression.span)
                unify(numeric, left, expression.left.span)
                unify(numeric, right, expression.right.span)
                numeric
            }

            BinaryOperator.LESS,
            BinaryOperator.LESS_EQUAL,
            BinaryOperator.GREATER,
            BinaryOperator.GREATER_EQUAL -> {
                val numeric = freshConstrained(TypeConstraint.Numeric, expression.span)
                unify(numeric, left, expression.left.span)
                unify(numeric, right, expression.right.span)
                LangTypes.BOOLEAN
            }

            BinaryOperator.EQUAL,
            BinaryOperator.NOT_EQUAL -> {
                val common = freshVariable()
                unify(common, left, expression.left.span)
                unify(common, right, expression.right.span)
                LangTypes.BOOLEAN
            }

            BinaryOperator.AND,
            BinaryOperator.OR -> {
                unify(LangTypes.BOOLEAN, left, expression.left.span)
                unify(LangTypes.BOOLEAN, right, expression.right.span)
                LangTypes.BOOLEAN
            }

            BinaryOperator.CONCAT -> {
                val common = freshConstrained(TypeConstraint.Concat, expression.span)
                unify(common, left, expression.left.span)
                unify(common, right, expression.right.span)
                common
            }

            BinaryOperator.COMPOSE -> {
                val leftFunction = unaryFunction(left, expression.left.span, "left side of '>>'")
                val rightFunction = unaryFunction(right, expression.right.span, "right side of '>>'")
                unify(leftFunction.result, rightFunction.parameters[0], expression.span)
                LangType.FunctionType(
                    listOf(leftFunction.parameters[0]),
                    rightFunction.result,
                )
            }
        }
    }

    private fun inferConstruct(expression: Expr.Construct): LangType {
        val spec = environment.objectConstructor(expression.kind)
            ?: fail(
                DiagnosticCode.E_NATIVE_CONTRACT,
                expression.span,
                "object constructor '${expression.kind.name.lowercase()}' is not provided by the environment",
            )
        if (expression.arguments.size != spec.arguments.size) {
            fail(
                DiagnosticCode.E_NATIVE_CONTRACT,
                expression.span,
                "object constructor '${expression.kind.name.lowercase()}' expects ${spec.arguments.size} arguments",
            )
        }
        for ((position, expected) in spec.arguments.withIndex()) {
            val argument = expression.arguments[position]
            when (expected) {
                ConstructorArgumentKind.RESOURCE_ID -> {
                    val text = argument as? ConstructorArgument.Text
                        ?: fail(
                            DiagnosticCode.E_NATIVE_CONTRACT,
                            argument.span,
                            "expected a resource id literal",
                        )
                    val validator = environment.resourceIdValidator
                    if (validator != null && !validator.isValid(text.value)) {
                        fail(
                            DiagnosticCode.E_LITERAL_RESOURCE,
                            argument.span,
                            "invalid resource id '${text.value}'",
                        )
                    }
                }

                ConstructorArgumentKind.POSITIVE_INTEGER -> {
                    val number = argument as? ConstructorArgument.Integer
                        ?: fail(
                            DiagnosticCode.E_NATIVE_CONTRACT,
                            argument.span,
                            "expected a positive Integer literal",
                        )
                    if (number.value <= 0) {
                        fail(
                            DiagnosticCode.E_LITERAL_RANGE,
                            argument.span,
                            "expected a positive Integer, found ${number.value}",
                        )
                    }
                }

                ConstructorArgumentKind.TEXT -> {
                    // Arbitrary text (NBT uses SNBT). The language does not parse SNBT; native
                    // resource/catalog validation belongs to the target backend.
                    argument as? ConstructorArgument.Text
                        ?: fail(
                            DiagnosticCode.E_NATIVE_CONTRACT,
                            argument.span,
                            "expected a text literal",
                        )
                }
            }
        }
        return spec.resultType
    }

    private fun applyFunction(
        callee: LangType,
        argumentType: LangType,
        applicationSpan: SourceSpan,
        argumentSpan: SourceSpan,
    ): LangType {
        val function = resolve(callee)
        return when (function) {
            is LangType.FunctionType -> {
                val parameters = function.parameters
                if (parameters.isEmpty()) {
                    fail(
                        DiagnosticCode.E_NOT_CALLABLE,
                        applicationSpan,
                        "cannot apply an argument to a zero-parameter function ${function.display()}",
                    )
                }
                unify(parameters[0], argumentType, argumentSpan)
                if (parameters.size == 1) {
                    function.result
                } else {
                    LangType.FunctionType(parameters.drop(1), function.result)
                }
            }

            is LangType.Variable -> fail(
                DiagnosticCode.E_UNRESOLVED_TYPE,
                applicationSpan,
                "cannot determine the function signature being applied; add a type annotation",
            )

            else -> fail(
                DiagnosticCode.E_NOT_CALLABLE,
                applicationSpan,
                "cannot apply arguments to non-function type ${function.display()}",
            )
        }
    }

    private fun unaryFunction(type: LangType, span: SourceSpan, role: String): LangType.FunctionType {
        val resolved = resolve(type)
        if (resolved !is LangType.FunctionType) {
            fail(
                DiagnosticCode.E_FN_SIGNATURE,
                span,
                "$role must be a function, found ${resolved.display()}",
            )
        }
        if (resolved.parameters.size != 1) {
            fail(
                DiagnosticCode.E_FN_SIGNATURE,
                span,
                "$role must have exactly one parameter, found ${resolved.display()}",
            )
        }
        return resolved
    }

    private fun lowerType(reference: TypeRef, depth: Int = 0): LangType {
        if (depth > limits.maxTypeDepth) {
            fail(
                DiagnosticCode.E_LIMIT,
                reference.span,
                "type nesting exceeds the limit ${limits.maxTypeDepth}",
            )
        }
        return when (reference) {
            is TypeRef.Named -> {
                if (!environment.hasType(reference.name)) {
                    fail(DiagnosticCode.E_UNKNOWN_TYPE, reference.span, "unknown type '${reference.name}'")
                }
                LangType.Nominal(reference.name)
            }

            is TypeRef.ListOf -> LangType.ListType(lowerType(reference.element, depth + 1))

            is TypeRef.Function -> LangType.FunctionType(
                reference.parameters.map { lowerType(it, depth + 1) },
                lowerType(reference.result, depth + 1),
            )
        }
    }

    private fun instantiate(scheme: TypeScheme, span: SourceSpan): LangType {
        val mapping = HashMap<Int, LangType>()
        for (variable in scheme.quantified) mapping[variable] = freshVariable()
        val type = substitute(scheme.type, mapping, span, 0)
        for (constraint in scheme.constraints) {
            val target = mapping[constraint.variableId]
                ?: fail(
                    DiagnosticCode.E_NATIVE_CONTRACT,
                    span,
                    "primitive scheme constrains an unquantified variable",
                )
            constraints.add(ConstraintRecord(target, constraint.constraint, span))
        }
        validateNominalTypes(type, span)
        return type
    }

    private fun validateNominalTypes(type: LangType, span: SourceSpan) {
        when (type) {
            is LangType.Nominal -> if (!environment.hasType(type.name)) {
                fail(
                    DiagnosticCode.E_NATIVE_CONTRACT,
                    span,
                    "contract refers to unregistered type '${type.name}'",
                )
            }

            is LangType.Variable -> Unit
            is LangType.ListType -> validateNominalTypes(type.element, span)
            is LangType.FunctionType -> {
                type.parameters.forEach { validateNominalTypes(it, span) }
                validateNominalTypes(type.result, span)
            }
        }
    }

    private fun substitute(
        type: LangType,
        mapping: Map<Int, LangType>,
        span: SourceSpan,
        depth: Int,
    ): LangType {
        if (depth > limits.maxTypeDepth) {
            fail(DiagnosticCode.E_LIMIT, span, "type nesting exceeds the limit ${limits.maxTypeDepth}")
        }
        return when (type) {
            is LangType.Variable -> mapping[type.id] ?: type
            is LangType.Nominal -> type
            is LangType.ListType -> LangType.ListType(substitute(type.element, mapping, span, depth + 1))
            is LangType.FunctionType -> LangType.FunctionType(
                type.parameters.map { substitute(it, mapping, span, depth + 1) },
                substitute(type.result, mapping, span, depth + 1),
            )
        }
    }

    private fun unify(left: LangType, right: LangType, span: SourceSpan) {
        unifyRecursive(left, right, span, 0)
    }

    private fun unifyRecursive(left: LangType, right: LangType, span: SourceSpan, depth: Int) {
        if (depth > limits.maxTypeDepth) {
            fail(DiagnosticCode.E_LIMIT, span, "type unification exceeds the limit ${limits.maxTypeDepth}")
        }
        val a = resolve(left)
        val b = resolve(right)
        if (a == b) return
        when {
            a is LangType.Variable -> bind(a, b, span)
            b is LangType.Variable -> bind(b, a, span)
            a is LangType.Nominal && b is LangType.Nominal -> {
                if (a.name != b.name) {
                    fail(
                        DiagnosticCode.E_TYPE,
                        span,
                        "type mismatch: expected ${a.display()}, found ${b.display()}",
                    )
                }
            }

            a is LangType.ListType && b is LangType.ListType ->
                unifyRecursive(a.element, b.element, span, depth + 1)

            a is LangType.FunctionType && b is LangType.FunctionType -> {
                if (a.parameters.size != b.parameters.size) {
                    fail(
                        DiagnosticCode.E_FN_SIGNATURE,
                        span,
                        "function arity mismatch: expected ${a.display()}, found ${b.display()}",
                    )
                }
                for (position in a.parameters.indices) {
                    unifyRecursive(a.parameters[position], b.parameters[position], span, depth + 1)
                }
                unifyRecursive(a.result, b.result, span, depth + 1)
            }

            else -> fail(
                DiagnosticCode.E_TYPE,
                span,
                "type mismatch: expected ${a.display()}, found ${b.display()}",
            )
        }
    }

    private fun bind(variable: LangType.Variable, type: LangType, span: SourceSpan) {
        if (type is LangType.Variable && type.id == variable.id) return
        if (occurs(variable.id, type, span, 0)) {
            fail(
                DiagnosticCode.E_TYPE,
                span,
                "infinite type: #${variable.id} occurs in ${type.display()}",
            )
        }
        substitutions[variable.id] = type
    }

    private fun occurs(id: Int, type: LangType, span: SourceSpan, depth: Int): Boolean {
        if (depth > limits.maxTypeDepth) {
            fail(DiagnosticCode.E_LIMIT, span, "type occurs check exceeds the limit ${limits.maxTypeDepth}")
        }
        return when (val resolved = resolve(type)) {
            is LangType.Variable -> resolved.id == id
            is LangType.Nominal -> false
            is LangType.ListType -> occurs(id, resolved.element, span, depth + 1)
            is LangType.FunctionType ->
                resolved.parameters.any { occurs(id, it, span, depth + 1) } ||
                    occurs(id, resolved.result, span, depth + 1)
        }
    }

    private fun resolve(type: LangType): LangType {
        if (type !is LangType.Variable) return type
        val path = ArrayList<LangType.Variable>()
        var current: LangType = type
        val guard = limits.maxTypeDepth * 64 + 64
        while (current is LangType.Variable) {
            path.add(current)
            val next = substitutions[current.id] ?: break
            current = next
            if (path.size > guard) break
        }
        for (variable in path) {
            if (current !is LangType.Variable || current.id != variable.id) {
                substitutions[variable.id] = current
            }
        }
        return current
    }

    private fun resolveFully(type: LangType, depth: Int = 0): LangType {
        if (depth > limits.maxTypeDepth) return type
        return when (val resolved = resolve(type)) {
            is LangType.Variable -> resolved
            is LangType.Nominal -> resolved
            is LangType.ListType -> LangType.ListType(resolveFully(resolved.element, depth + 1))
            is LangType.FunctionType -> LangType.FunctionType(
                resolved.parameters.map { resolveFully(it, depth + 1) },
                resolveFully(resolved.result, depth + 1),
            )
        }
    }

    private fun finalizeTypes(): CheckedProgram {
        for (constraint in constraints) {
            val type = resolveFully(constraint.type)
            if (type.containsVariable()) {
                fail(
                    DiagnosticCode.E_UNRESOLVED_TYPE,
                    constraint.span,
                    "cannot determine a type for this ${constraint.constraint.display()} use",
                )
            }
            if (!environment.accepts(type, constraint.constraint)) {
                fail(
                    DiagnosticCode.E_TYPE,
                    constraint.span,
                    "type ${type.display()} does not satisfy constraint ${constraint.constraint.display()}",
                )
            }
        }

        val resolvedExpressions = LinkedHashMap<Expr, LangType>()
        for ((expression, type) in expressionTypes) {
            val resolved = resolveFully(type)
            if (resolved.containsVariable()) {
                if (expression is Expr.ListLiteral && expression.elements.isEmpty()) {
                    fail(
                        DiagnosticCode.E_EMPTY_LIST_TYPE,
                        expression.span,
                        "the element type of an empty list cannot be inferred; add a type annotation or use it in a typed context",
                    )
                }
                fail(
                    DiagnosticCode.E_UNRESOLVED_TYPE,
                    expression.span,
                    "cannot fully determine the type of this expression; ${resolved.display()} still contains an unknown",
                )
            }
            validateNominalTypes(resolved, expression.span)
            resolvedExpressions[expression] = resolved
        }

        val resolvedInputs = LinkedHashMap<InputDeclaration, LangType>()
        for ((declaration, type) in inputTypes) {
            val resolved = resolveFully(type)
            if (resolved.containsVariable()) {
                fail(
                    DiagnosticCode.E_UNRESOLVED_TYPE,
                    declaration.nameSpan,
                    "input type is not concrete",
                )
            }
            validateNominalTypes(resolved, declaration.nameSpan)
            resolvedInputs[declaration] = resolved
        }

        val resolvedLets = LinkedHashMap<LetDeclaration, LangType>()
        for ((declaration, type) in letTypes) {
            val resolved = resolveFully(type)
            if (resolved.containsVariable()) {
                fail(
                    DiagnosticCode.E_UNRESOLVED_TYPE,
                    declaration.nameSpan,
                    "let '${declaration.name}' does not have a fully determined type; add an annotation",
                )
            }
            validateNominalTypes(resolved, declaration.nameSpan)
            resolvedLets[declaration] = resolved
        }

        val resolvedOutputs = LinkedHashMap<OutputDeclaration, LangType>()
        for ((declaration, type) in outputTypes) {
            val resolved = resolveFully(type)
            if (resolved.containsVariable()) {
                fail(
                    DiagnosticCode.E_UNRESOLVED_TYPE,
                    declaration.nameSpan,
                    "output '${declaration.name}' does not have a fully determined type; add an annotation",
                )
            }
            validateNominalTypes(resolved, declaration.nameSpan)
            resolvedOutputs[declaration] = resolved
        }

        return CheckedProgram(
            inputTypes = resolvedInputs,
            letTypes = resolvedLets,
            outputTypes = resolvedOutputs,
            expressionTypes = resolvedExpressions,
        )
    }

    private fun freshVariable(): LangType.Variable = LangType.Variable(nextVariable++)

    private fun freshConstrained(constraint: TypeConstraint, span: SourceSpan): LangType {
        val variable = freshVariable()
        constraints.add(ConstraintRecord(variable, constraint, span))
        return variable
    }

    private fun fail(code: DiagnosticCode, span: SourceSpan, message: String): Nothing =
        throw TypeFailure(Diagnostic.error(code, span, message))

    private data class ConstraintRecord(
        val type: LangType,
        val constraint: TypeConstraint,
        val span: SourceSpan,
    )
}

internal fun resolveProgram(
    program: Program,
    environment: LanguageEnvironment,
    limits: LanguageLimits,
): Pair<CheckedProgram?, List<Diagnostic>> {
    val resolution = NameResolver(program, environment, limits).resolve()
    if (resolution.diagnostics.hasErrors()) return null to resolution.diagnostics
    val checked = TypeChecker(program, environment, resolution, limits).check()
    return checked.checked to checked.diagnostics
}
