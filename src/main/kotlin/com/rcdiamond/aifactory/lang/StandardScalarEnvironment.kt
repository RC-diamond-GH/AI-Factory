package com.rcdiamond.aifactory.lang

/**
 * Environment with scalar semantics, homogeneous lists and higher-order list/predicate primitives.
 * It contains no Minecraft, Forge or Integrated Dynamics type. Hosts can start from
 * [LanguageEnvironment.basic] or reuse this environment and add their own nominal types and
 * primitives.
 */
object StandardScalarEnvironment {
    val environment: LanguageEnvironment by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { create() }

    fun create(): LanguageEnvironment {
        val builder = LanguageEnvironment.basic().builder()

        builder.addPrimitive("add", numericBinary(), "numeric addition")
        builder.addPrimitive("sub", numericBinary(), "numeric subtraction")
        builder.addPrimitive("mul", numericBinary(), "numeric multiplication")
        builder.addPrimitive("divide", numericBinary(), "numeric division")
        builder.addPrimitive("mod", numericBinary(), "numeric remainder")
        builder.addPrimitive("min", numericBinary(), "numeric minimum")
        builder.addPrimitive("max", numericBinary(), "numeric maximum")
        builder.addPrimitive("increment", numericUnary(), "add one")
        builder.addPrimitive("decrement", numericUnary(), "subtract one")
        builder.addPrimitive("eq", equality(), "same-type equality")
        builder.addPrimitive("ne", equality(), "same-type inequality")
        builder.addPrimitive("lt", numericComparison(), "numeric less-than")
        builder.addPrimitive("le", numericComparison(), "numeric less-than-or-equal")
        builder.addPrimitive("gt", numericComparison(), "numeric greater-than")
        builder.addPrimitive("ge", numericComparison(), "numeric greater-than-or-equal")
        builder.addPrimitive(
            "not",
            TypeScheme(LangTypes.function(LangTypes.BOOLEAN, result = LangTypes.BOOLEAN)),
            "Boolean negation",
        )
        builder.addPrimitive("toInteger", numericCast(LangTypes.INTEGER), "numeric to Integer")
        builder.addPrimitive("toLong", numericCast(LangTypes.LONG), "numeric to Long")
        builder.addPrimitive("toDouble", numericCast(LangTypes.DOUBLE), "numeric to Double")
        builder.addPrimitive("length", listLength(), "list length")
        builder.addPrimitive("isEmpty", listEmpty(), "list emptiness")
        builder.addPrimitive("get", listGet(), "list element")
        builder.addPrimitive("getOr", listGetOrDefault(), "list element with default")
        builder.addPrimitive("append", listAppend(), "append one element")
        builder.addPrimitive("slice", listSlice(), "list range [start, end)")
        builder.addPrimitive("map", listMap(), "map a unary function over a finite list")
        builder.addPrimitive("filter", listFilter(), "filter with a unary predicate")
        builder.addPrimitive("reduce", listReduce(), "left fold")
        builder.addPrimitive("identity", identity(), "identity function")
        builder.addPrimitive("flip", FlipPrimitiveRule, "swap the first two arguments")
        builder.addPrimitive("call0", call0(), "apply a zero-parameter function")
        builder.addPrimitive("allOf", predicateCombination(), "predicate conjunction")
        builder.addPrimitive("anyOf", predicateCombination(), "predicate disjunction")
        builder.addPrimitive("negatePred", predicateNegation(), "predicate negation")
        builder.addPrimitive("pipe2", pipe2(), "two-branch unary pipeline")

        return builder.build()
    }

    private fun numericBinary(): TypeScheme {
        val n = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(n, n, result = n),
            setOf(0),
            listOf(SchemeConstraint(0, TypeConstraint.Numeric)),
        )
    }

    private fun numericUnary(): TypeScheme {
        val n = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(n, result = n),
            setOf(0),
            listOf(SchemeConstraint(0, TypeConstraint.Numeric)),
        )
    }

    private fun numericComparison(): TypeScheme {
        val n = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(n, n, result = LangTypes.BOOLEAN),
            setOf(0),
            listOf(SchemeConstraint(0, TypeConstraint.Numeric)),
        )
    }

    private fun equality(): TypeScheme {
        val a = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(a, a, result = LangTypes.BOOLEAN),
            setOf(0),
            listOf(SchemeConstraint(0, TypeConstraint.Equatable)),
        )
    }

    private fun numericCast(target: LangType): TypeScheme {
        val n = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(n, result = target),
            setOf(0),
            listOf(SchemeConstraint(0, TypeConstraint.Numeric)),
        )
    }

    private fun listLength(): TypeScheme {
        val a = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(LangTypes.list(a), result = LangTypes.INTEGER),
            setOf(0),
        )
    }

    private fun listEmpty(): TypeScheme {
        val a = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(LangTypes.list(a), result = LangTypes.BOOLEAN),
            setOf(0),
        )
    }

    private fun listGet(): TypeScheme {
        val a = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(LangTypes.list(a), LangTypes.INTEGER, result = a),
            setOf(0),
        )
    }

    private fun listGetOrDefault(): TypeScheme {
        val a = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(LangTypes.list(a), LangTypes.INTEGER, a, result = a),
            setOf(0),
        )
    }

    private fun listAppend(): TypeScheme {
        val a = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(LangTypes.list(a), a, result = LangTypes.list(a)),
            setOf(0),
        )
    }

    private fun listSlice(): TypeScheme {
        val a = LangType.Variable(0)
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

    private fun listMap(): TypeScheme {
        val a = LangType.Variable(0)
        val b = LangType.Variable(1)
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
        val a = LangType.Variable(0)
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
        val a = LangType.Variable(0)
        val b = LangType.Variable(1)
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
        val a = LangType.Variable(0)
        return TypeScheme(LangTypes.function(a, result = a), setOf(0))
    }

    /**
     * `flip` is genuinely higher-order: its result retains the arity of the function argument, with
     * the first two parameters swapped. A fixed first-order scheme would reject arity 3+, so this
     * rule inspects the resolved first argument. Every later argument is then an ordinary
     * application of the resulting function type.
     */
    val FlipPrimitiveRule: PrimitiveTypeRule = PrimitiveTypeRule { arguments, context ->
        if (arguments.isEmpty()) {
            throw PrimitiveTypeException(DiagnosticCode.E_UNRESOLVED_TYPE, "flip needs a function argument")
        }
        val resolved = context.resolve(arguments[0])
        val function = resolved as? LangType.FunctionType
            ?: throw PrimitiveTypeException(
                DiagnosticCode.E_TYPE,
                "flip requires a function, found ${resolved.display()}",
            )
        if (function.parameters.size < 2) {
            throw PrimitiveTypeException(
                DiagnosticCode.E_FN_SIGNATURE,
                "flip requires at least two parameters, found ${function.display()}",
            )
        }
        val parameters = ArrayList(function.parameters)
        val first = parameters[0]
        parameters[0] = parameters[1]
        parameters[1] = first
        LangType.FunctionType(parameters, function.result)
    }

    fun runtime(): Map<String, PurePrimitive> = StandardScalarRuntime.primitives

    private fun call0(): TypeScheme {
        val a = LangType.Variable(0)
        return TypeScheme(
            LangTypes.function(LangTypes.function(result = a), result = a),
            setOf(0),
        )
    }

    private fun predicateCombination(): TypeScheme {
        val a = LangType.Variable(0)
        val predicate = LangTypes.function(a, result = LangTypes.BOOLEAN)
        return TypeScheme(
            LangTypes.function(predicate, predicate, result = predicate),
            setOf(0),
        )
    }

    private fun predicateNegation(): TypeScheme {
        val a = LangType.Variable(0)
        val predicate = LangTypes.function(a, result = LangTypes.BOOLEAN)
        return TypeScheme(LangTypes.function(predicate, result = predicate), setOf(0))
    }

    private fun pipe2(): TypeScheme {
        val a = LangType.Variable(0)
        val b = LangType.Variable(1)
        val c = LangType.Variable(2)
        val d = LangType.Variable(3)
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
}
