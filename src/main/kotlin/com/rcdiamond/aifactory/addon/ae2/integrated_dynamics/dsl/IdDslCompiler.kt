package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import com.rcdiamond.aifactory.lang.CompilationResult
import com.rcdiamond.aifactory.lang.ConstructorArgument
import com.rcdiamond.aifactory.lang.Declaration
import com.rcdiamond.aifactory.lang.Diagnostic
import com.rcdiamond.aifactory.lang.InputDeclaration
import com.rcdiamond.aifactory.lang.IrArithmeticOperator
import com.rcdiamond.aifactory.lang.IrComparisonOperator
import com.rcdiamond.aifactory.lang.IrOperation
import com.rcdiamond.aifactory.lang.IrRef
import com.rcdiamond.aifactory.lang.IrStaticArgument
import com.rcdiamond.aifactory.lang.LangCompiler
import com.rcdiamond.aifactory.lang.LangType
import com.rcdiamond.aifactory.lang.LanguageLimits
import com.rcdiamond.aifactory.lang.LiteralValue
import com.rcdiamond.aifactory.lang.ObjectKind
import com.rcdiamond.aifactory.lang.SourceSpan
import com.rcdiamond.aifactory.lang.SourceText
import com.rcdiamond.aifactory.lang.TypedModule
import com.rcdiamond.aifactory.lang.TypeRef
import com.rcdiamond.aifactory.lang.utf8ByteLength
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Result of one pure client compilation. [plan] is null whenever any error exists. */
data class IdCompileResult(
    val plan: JsonObject?,
    val typedPlan: IdPlan?,
    val diagnostics: List<IdDiagnostic>,
    val sourceMap: IdSourceMap?,
    val outputs: List<IdPlanOutput>,
) {
    val success: Boolean
        get() = plan != null && typedPlan != null && diagnostics.none { it.isError }

    val nodeCount: Int get() = typedPlan?.nodes?.size ?: 0

    /** New definitions and distinct aspect cards; aliases of existing variables need no new card. */
    val cardsRequired: Int
        get() {
            val plan = typedPlan ?: return 0
            val aspects = HashSet<IdBinding.Aspect>()
            for (input in plan.imports) {
                val binding = input.binding
                if (binding is IdBinding.Aspect) aspects.add(binding)
            }
            return plan.nodes.size + aspects.size
        }

    val outputCount: Int get() = typedPlan?.outputs?.size ?: 0

    fun renderDiagnostics(): String = diagnostics.joinToString("\n") { it.render() }
}

/**
 * Pure ID DSL compiler.
 *
 * `description` is the backend `id_describe_inputs` reply: authoritative concrete input types,
 * bindings and finiteness. The compiler never reads a live value and never trusts a source annotation
 * over the description.
 */
object IdDslCompiler {
    const val CONTRACT: String = ID_DSL_CONTRACT

    private val languageLimits = LanguageLimits(
        maxSourceBytes = IdDslLimits.MAX_SOURCE_BYTES,
        maxSourceChars = IdDslLimits.MAX_SOURCE_BYTES,
        maxTokens = 100_000,
        maxDeclarations = 1_024,
        maxNestingDepth = 128,
        maxTypeDepth = IdDslLimits.MAX_TYPE_DEPTH,
        maxListLength = IdDslLimits.MAX_LIST_LENGTH,
        maxStringLength = IdDslLimits.MAX_LITERAL_TEXT_BYTES,
        maxNodes = IdDslLimits.MAX_TYPED_NODES,
        maxDependencyDepth = 512,
        maxDiagnostics = IdDslLimits.MAX_DIAGNOSTICS,
    )

    private val compiler: LangCompiler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LangCompiler(IdOperatorCatalog.environment, languageLimits)
    }

    fun compile(
        source: String,
        description: JsonObject,
        sourceName: String = "id-dsl",
    ): IdCompileResult {
        sourceLimitDiagnostic(source)?.let { return failed(listOf(it)) }
        val text = SourceText.of(sourceName, source)
        val parsedDescription = try {
            IdDslWire.parseDescription(description)
        } catch (failure: IdWireException) {
            val code = when (failure.code) {
                IdDiagnosticCode.E_LIMIT -> IdDiagnosticCode.E_LIMIT
                IdDiagnosticCode.E_TARGET_CONTRACT -> IdDiagnosticCode.E_TARGET_CONTRACT
                else -> IdDiagnosticCode.E_TARGET_CONTRACT
            }
            return failed(
                listOf(
                    IdDiagnostic(
                        code = code,
                        severity = IdSeverity.ERROR,
                        message = "invalid describe response: ${failure.message}",
                        hint = hintFor(code),
                        node = failure.nodeId,
                        input = failure.inputName,
                    ),
                ),
            )
        } catch (failure: RuntimeException) {
            return failed(
                listOf(
                    IdDiagnostic(
                        code = IdDiagnosticCode.E_TARGET_CONTRACT,
                        severity = IdSeverity.ERROR,
                        message = "invalid describe response: ${failure.message ?: failure.javaClass.simpleName}",
                        hint = hintFor(IdDiagnosticCode.E_TARGET_CONTRACT),
                    ),
                ),
            )
        }
        return compile(text, parsedDescription)
    }

    fun compile(source: String, description: IdInputDescription, sourceName: String = "id-dsl"): IdCompileResult {
        sourceLimitDiagnostic(source)?.let { return failed(listOf(it)) }
        return compile(SourceText.of(sourceName, source), description)
    }

    /**
     * Bounds the two public String entry points before a line index is built. The language compiler
     * repeats the same byte check, but a shared API must not allocate over a giant input first.
     */
    private fun sourceLimitDiagnostic(source: String): IdDiagnostic? {
        if (source.length > IdDslLimits.MAX_SOURCE_BYTES) {
            return IdDiagnostic(
                code = IdDiagnosticCode.E_LIMIT,
                severity = IdSeverity.ERROR,
                message = "source has ${source.length} UTF-16 units; the limit is ${IdDslLimits.MAX_SOURCE_BYTES}",
                line = 1,
                column = 1,
                hint = hintFor(IdDiagnosticCode.E_LIMIT),
            )
        }
        val bytes = utf8ByteLength(source, IdDslLimits.MAX_SOURCE_BYTES)
        if (bytes > IdDslLimits.MAX_SOURCE_BYTES) {
            return IdDiagnostic(
                code = IdDiagnosticCode.E_LIMIT,
                severity = IdSeverity.ERROR,
                message = "source exceeds ${IdDslLimits.MAX_SOURCE_BYTES} UTF-8 bytes",
                line = 1,
                column = 1,
                hint = hintFor(IdDiagnosticCode.E_LIMIT),
            )
        }
        return null
    }

    private fun compile(source: SourceText, description: IdInputDescription): IdCompileResult {
        val compiled = try {
            compiler.compile(source)
        } catch (failure: RuntimeException) {
            return failed(
                listOf(
                    IdDiagnostic(
                        code = IdDiagnosticCode.E_EVALUATION,
                        severity = IdSeverity.ERROR,
                        message = "frontend failure: ${failure.message ?: failure.javaClass.simpleName}",
                        line = 1,
                        column = 1,
                    ),
                ),
            )
        }
        val diagnostics = ArrayList<IdDiagnostic>()
        diagnostics += compiled.diagnostics.map { it.toIdDiagnostic(source) }

        val module = compiled.module
        if (!compiled.success || module == null) {
            return failed(diagnostics)
        }

        val bindingDiagnostics = try {
            validateBindings(module, description, source)
        } catch (failure: RuntimeException) {
            diagnostics += IdDiagnostic(
                code = IdDiagnosticCode.E_EVALUATION,
                severity = IdSeverity.ERROR,
                message = "input validation failure: ${failure.message ?: failure.javaClass.simpleName}",
                line = 1,
                column = 1,
            )
            return failed(diagnostics)
        }
        diagnostics += bindingDiagnostics
        if (diagnostics.any { it.isError }) {
            return failed(diagnostics)
        }

        val built = try {
            IdPlanBuilder(module, source, description).build()
        } catch (failure: IdLoweringFailure) {
            diagnostics += failure.diagnostic
            return failed(diagnostics)
        } catch (failure: RuntimeException) {
            diagnostics += IdDiagnostic(
                code = IdDiagnosticCode.E_EVALUATION,
                severity = IdSeverity.ERROR,
                message = "plan lowering failure: ${failure.message ?: failure.javaClass.simpleName}",
                line = 1,
                column = 1,
            )
            return failed(diagnostics)
        }

        val resolved = built.plan.imports.associateTo(LinkedHashMap()) { import ->
            import.name to IdResolvedInput(import.type, import.finiteness)
        }
        val validation = try {
            IdPlanValidator.validate(built.plan, resolved)
        } catch (failure: RuntimeException) {
            diagnostics += IdDiagnostic(
                code = IdDiagnosticCode.E_EVALUATION,
                severity = IdSeverity.ERROR,
                message = "plan validation failure: ${failure.message ?: failure.javaClass.simpleName}",
                line = 1,
                column = 1,
            )
            return failed(diagnostics)
        }
        for (diagnostic in validation.diagnostics) {
            diagnostics += diagnostic.withSource(built.sourceMap, source)
        }
        if (validation.diagnostics.any { it.isError } || validation.plan == null) {
            return failed(diagnostics)
        }

        val planJson = IdDslWire.encodePlan(built.plan)
        val encodedPlan = planJson.toString()
        if (utf8ByteLength(encodedPlan, IdDslLimits.MAX_PLAN_BYTES) > IdDslLimits.MAX_PLAN_BYTES) {
            diagnostics += IdDiagnostic(
                code = IdDiagnosticCode.E_LIMIT,
                severity = IdSeverity.ERROR,
                message = "encoded plan exceeds ${IdDslLimits.MAX_PLAN_BYTES} UTF-8 bytes",
                line = 1,
                column = 1,
                hint = hintFor(IdDiagnosticCode.E_LIMIT),
            )
            return failed(diagnostics)
        }
        return IdCompileResult(
            plan = planJson,
            typedPlan = built.plan,
            diagnostics = diagnostics,
            sourceMap = built.sourceMap,
            outputs = built.plan.outputs,
        )
    }

    private fun failed(diagnostics: List<IdDiagnostic>): IdCompileResult =
        IdCompileResult(null, null, diagnostics, null, emptyList())

    // ------------------------------------------------------------------ input contracts

    private fun validateBindings(
        module: TypedModule,
        description: IdInputDescription,
        source: SourceText,
    ): List<IdDiagnostic> {
        val diagnostics = ArrayList<IdDiagnostic>()
        val declared = module.program.declarations.filterIsInstance<InputDeclaration>()
        val declaredNames = declared.mapTo(LinkedHashSet()) { it.name }
        val usedNames = module.graph.inputs.mapTo(LinkedHashSet()) { it.name }

        if (declared.size > IdDslLimits.MAX_PORTS) {
            diagnostics += sourceDiagnostic(
                IdDiagnosticCode.E_LIMIT,
                source,
                declared[IdDslLimits.MAX_PORTS].span,
                "source declares more than ${IdDslLimits.MAX_PORTS} inputs",
            )
        }
        if (module.graph.outputs.size > IdDslLimits.MAX_PORTS) {
            diagnostics += sourceDiagnostic(
                IdDiagnosticCode.E_LIMIT,
                source,
                module.graph.outputs[IdDslLimits.MAX_PORTS].span,
                "source declares more than ${IdDslLimits.MAX_PORTS} outputs",
            )
        }

        for (input in declared) {
            val described = description.byName(input.name)
            if (described == null) {
                diagnostics += sourceDiagnostic(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    source,
                    input.nameSpan,
                    "input '${input.name}' has no binding in the description",
                    input = input.name,
                )
            } else {
                val declaredType = typeRefToDsl(input.type)
                if (declaredType == null) {
                    diagnostics += sourceDiagnostic(
                        IdDiagnosticCode.E_TYPE,
                        source,
                        input.type.span,
                        "input '${input.name}' does not have a concrete core type",
                        input = input.name,
                    )
                } else if (declaredType != described.type) {
                    diagnostics += sourceDiagnostic(
                        IdDiagnosticCode.E_TYPE,
                        source,
                        input.nameSpan,
                        "input '${input.name}' is declared ${declaredType.display()} but the target binds ${described.type.display()}",
                        input = input.name,
                        hint = "A binding is a checked contract, not a cast; change the annotation or the target.",
                    )
                }

                val graphInput = module.graph.input(input.name)
                if (graphInput != null) {
                    val actual = langTypeToDsl(graphInput.type)
                    if (actual != null && actual != described.type) {
                        diagnostics += sourceDiagnostic(
                            IdDiagnosticCode.E_TYPE,
                            source,
                            input.nameSpan,
                            "input '${input.name}' has type ${actual.display()} in the checked graph but ${described.type.display()} in the description",
                            input = input.name,
                        )
                    }
                }

                if (described.binding is IdBinding.Aspect) {
                    val contract = IdAspectContracts.find(described.binding.aspect)
                    if (contract != null) {
                        if (contract.type != described.type) {
                            diagnostics += sourceDiagnostic(
                                IdDiagnosticCode.E_INPUT_BINDING,
                                source,
                                input.nameSpan,
                                "aspect '${described.binding.aspect}' must read ${contract.type.display()}, not ${described.type.display()}",
                                input = input.name,
                            )
                        }
                        if (contract.finiteness != described.finiteness) {
                            diagnostics += sourceDiagnostic(
                                IdDiagnosticCode.E_INPUT_BINDING,
                                source,
                                input.nameSpan,
                                "aspect '${described.binding.aspect}' must be ${contract.finiteness.wire}, not ${described.finiteness.wire}",
                                input = input.name,
                            )
                        }
                    } else if (described.type is IdDslType.ListType) {
                        diagnostics += sourceDiagnostic(
                            IdDiagnosticCode.E_INPUT_BINDING,
                            source,
                            input.nameSpan,
                            "list aspect '${described.binding.aspect}' has no fixed element-type contract",
                            input = input.name,
                            hint = "Only source-verified core list aspects are accepted; unknown list aspects cannot be statically proved.",
                        )
                    }
                }
            }

            if (input.name !in usedNames) {
                diagnostics += sourceDiagnostic(
                    IdDiagnosticCode.W_UNUSED_BINDING,
                    source,
                    input.nameSpan,
                    "input '${input.name}' is bound but not reachable from any output",
                    severity = IdSeverity.WARNING,
                    input = input.name,
                )
            }
        }

        for (input in description.inputs) {
            if (input.name !in declaredNames) {
                diagnostics += IdDiagnostic(
                    code = IdDiagnosticCode.E_INPUT_BINDING,
                    severity = IdSeverity.ERROR,
                    message = "binding '${input.name}' does not match any declared input",
                    hint = "Remove the extra binding; names are case-sensitive.",
                    input = input.name,
                )
            }
        }

        return diagnostics
    }

    private fun langTypeToDsl(type: LangType): IdDslType? = when (type) {
        is LangType.Nominal -> IdDslType.Scalar(type.name).takeIf { it.name in IdDslTypes.CORE_SCALARS }
        is LangType.ListType -> langTypeToDsl(type.element)?.let { IdDslType.ListType(it) }
        is LangType.FunctionType -> {
            val parameters = type.parameters.map { langTypeToDsl(it) ?: return null }
            langTypeToDsl(type.result)?.let { IdDslType.FunctionType(parameters, it) }
        }

        is LangType.Variable -> null
    }

    private fun typeRefToDsl(type: TypeRef): IdDslType? = when (type) {
        is TypeRef.Named -> IdDslType.Scalar(type.name).takeIf { it.name in IdDslTypes.CORE_SCALARS }
        is TypeRef.ListOf -> typeRefToDsl(type.element)?.let { IdDslType.ListType(it) }
        is TypeRef.Function -> {
            val parameters = type.parameters.map { typeRefToDsl(it) ?: return null }
            typeRefToDsl(type.result)?.let { IdDslType.FunctionType(parameters, it) }
        }
    }

    // ------------------------------------------------------------------ diagnostics

    private fun Diagnostic.toIdDiagnostic(source: SourceText): IdDiagnostic {
        val idSpan = span.toIdSpan()
        val position = source.positionAt(span.start)
        return IdDiagnostic(
            code = IdDiagnosticCode.fromLang(code),
            severity = if (isError) IdSeverity.ERROR else IdSeverity.WARNING,
            message = message,
            span = idSpan,
            line = position.line,
            column = position.column,
            hint = hintFor(IdDiagnosticCode.fromLang(code)),
            related = related.map { IdRelatedDiagnostic(it.message, it.span.toIdSpan()) },
        )
    }

    private fun sourceDiagnostic(
        code: IdDiagnosticCode,
        source: SourceText,
        span: SourceSpan,
        message: String,
        severity: IdSeverity = IdSeverity.ERROR,
        hint: String? = hintFor(code),
        input: String? = null,
    ): IdDiagnostic {
        val position = source.positionAt(span.start)
        return IdDiagnostic(
            code = code,
            severity = severity,
            message = message,
            span = span.toIdSpan(),
            line = position.line,
            column = position.column,
            hint = hint,
            input = input,
        )
    }

    internal fun loweringDiagnostic(
        code: IdDiagnosticCode,
        source: SourceText,
        span: SourceSpan,
        message: String,
        hint: String? = hintFor(code),
    ): IdDiagnostic {
        val position = source.positionAt(span.start)
        return IdDiagnostic(
            code = code,
            severity = IdSeverity.ERROR,
            message = message,
            span = span.toIdSpan(),
            line = position.line,
            column = position.column,
            hint = hint,
        )
    }

    internal fun hintFor(code: IdDiagnosticCode): String? = when (code) {
        IdDiagnosticCode.E_TYPE ->
            "Match operand, branch and annotation types; use explicit numeric casts or an L suffix when needed. Functions have no value equality."
        IdDiagnosticCode.E_UNRESOLVED_TYPE ->
            "Add a concrete annotation or use the value in a context that determines every type variable."
        IdDiagnosticCode.E_CYCLE ->
            "Definitions may refer forward, but the immediate dependency graph must stay acyclic."
        IdDiagnosticCode.E_INPUT_BINDING ->
            "Bindings are keyed by exact input name and must match the server-resolved type and finiteness."
        IdDiagnosticCode.E_LITERAL_RANGE ->
            "Keep Integer/Long literals in range (L for Long), fluid amounts positive, and slice bounds at 0 <= from < to."
        IdDiagnosticCode.E_LITERAL_RESOURCE ->
            "Use an explicit lowercase namespace:path resource id."
        IdDiagnosticCode.E_LITERAL_NBT ->
            "Use the pinned 1.20.1 SNBT tag syntax; raw ItemStack JSON and card NBT are not accepted."
        IdDiagnosticCode.E_NATIVE_CONTRACT ->
            "Only pinned native operators with a checkable contract can be referenced by op."
        IdDiagnosticCode.E_INFINITE_CONSUMER ->
            "filter, reduce and itemCount consume the whole list; pass a finite list or bound it with slice."
        IdDiagnosticCode.E_FN_SIGNATURE ->
            "Check the native operator parameter order and the function's parameter/return types."
        IdDiagnosticCode.E_NOT_CALLABLE ->
            "Apply arguments only to values with a concrete Fn type."
        IdDiagnosticCode.E_LIMIT ->
            "Reduce source size, nesting, list length or generated definitions to the published limits."
        IdDiagnosticCode.E_TARGET_CONTRACT ->
            "The compilation and deployment target contracts differ; refresh the target description."
        IdDiagnosticCode.W_UNKNOWN_FINITENESS ->
            "The list may be infinite at runtime; the native operator may fail if it is."
        else -> null
    }
}

private fun SourceSpan.toIdSpan(): IdSourceSpan = IdSourceSpan(start, end)

private fun IdDiagnostic.withSource(map: IdSourceMap, source: SourceText): IdDiagnostic {
    val span = node?.let { map.nodes[it] }
        ?: input?.let { map.imports[it] }
        ?: IdSourceSpan.EMPTY
    val position = source.positionAt(span.start)
    return copy(
        span = span,
        line = position.line,
        column = position.column,
        hint = hint ?: IdDslCompiler.hintFor(code),
    )
}

internal class IdLoweringFailure(val diagnostic: IdDiagnostic) : RuntimeException(diagnostic.message)

private class IdPlanBuilder(
    private val module: TypedModule,
    private val source: SourceText,
    private val description: IdInputDescription,
) {
    private val graph = module.graph
    private val nodes = ArrayList<IdPlanNode>()
    private val sourceNodes = LinkedHashMap<Int, IdSourceSpan>()
    private val valueNodes = LinkedHashMap<LiteralKey, IdReference>()
    private val memo = arrayOfNulls<IdReference>(graph.nodes.size)
    private val staticComputed = BooleanArray(graph.nodes.size)
    private val staticValues = arrayOfNulls<JsonElement>(graph.nodes.size)
    private var valueJsonNodes = 0
    private var valueBytes = 0L

    data class Built(val plan: IdPlan, val sourceMap: IdSourceMap)

    private data class LiteralKey(val type: IdDslType, val value: JsonElement)

    fun build(): Built {
        val declaredInputs = module.program.declarations.filterIsInstance<InputDeclaration>()
        val usedNames = graph.inputs.mapTo(LinkedHashSet()) { it.name }
        val imports = ArrayList<IdPlanImport>()
        val importSpans = LinkedHashMap<String, IdSourceSpan>()
        for (declaration in declaredInputs) {
            if (declaration.name !in usedNames) continue
            val described = description.byName(declaration.name)
                ?: throw fail(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    declaration.nameSpan,
                    "input '${declaration.name}' has no binding",
                )
            imports += IdPlanImport(
                declaration.name,
                described.binding,
                described.type,
                described.finiteness,
            )
            importSpans[declaration.name] = declaration.nameSpan.toIdSpan()
        }

        val outputs = ArrayList<IdPlanOutput>()
        val outputSpans = LinkedHashMap<String, IdSourceSpan>()
        for (output in graph.outputs) {
            val ref = lower((output.reference as IrRef.Node).id)
            outputs += IdPlanOutput(output.name, ref, toDslType(output.type, output.span))
            outputSpans[output.name] = output.span.toIdSpan()
        }

        val plan = IdPlan(imports = imports, nodes = nodes.toList(), outputs = outputs)
        return Built(plan, IdSourceMap(sourceNodes.toMap(), importSpans, outputSpans))
    }

    // ------------------------------------------------------------------ IR lowering

    private fun lower(id: Int): IdReference {
        memo[id]?.let { return it }
        val node = graph.nodes[id]
        val ref = when (val operation = node.operation) {
            is IrOperation.Input -> IdReference.Input(operation.name)
            is IrOperation.Constant -> emitValue(
                toDslType(node.type, node.span),
                encodeConstant(node),
                node.span,
            )

            is IrOperation.Construct -> emitValue(
                toDslType(node.type, node.span),
                encodeConstruct(node),
                node.span,
            )

            is IrOperation.ListLiteral -> lowerList(node.id, operation)
            is IrOperation.Primitive -> lowerPrimitive(node.id, operation.name)
            is IrOperation.OperatorLiteral -> lowerOperatorLiteral(node.id, operation.id)
            is IrOperation.Apply -> lowerApply(node.id)
            is IrOperation.Choice -> emitCall(
                IdOperatorCatalog.GENERAL_CHOICE,
                listOf(
                    lower(operation.condition.nodeId()),
                    lower(operation.whenTrue.nodeId()),
                    lower(operation.whenFalse.nodeId()),
                ),
                toDslType(node.type, node.span),
                node.span,
            )

            is IrOperation.Arithmetic -> emitCall(
                arithmeticId(operation.operator, node.span),
                listOf(lower(operation.left.nodeId()), lower(operation.right.nodeId())),
                toDslType(node.type, node.span),
                node.span,
            )

            is IrOperation.Comparison -> emitCall(
                comparisonId(operation.operator, node.span),
                listOf(lower(operation.left.nodeId()), lower(operation.right.nodeId())),
                toDslType(node.type, node.span),
                node.span,
            )

            is IrOperation.LogicalAnd -> emitCall(
                IdOperatorCatalog.LOGICAL_AND,
                listOf(lower(operation.left.nodeId()), lower(operation.right.nodeId())),
                toDslType(node.type, node.span),
                node.span,
            )

            is IrOperation.LogicalOr -> emitCall(
                IdOperatorCatalog.LOGICAL_OR,
                listOf(lower(operation.left.nodeId()), lower(operation.right.nodeId())),
                toDslType(node.type, node.span),
                node.span,
            )

            is IrOperation.Concat -> {
                val type = toDslType(node.type, node.span)
                val operator = when (type) {
                    is IdDslType.Scalar -> {
                        if (type.name != "String") {
                            throw fail(IdDiagnosticCode.E_TYPE, node.span, "++ is only defined for String and lists")
                        }
                        IdOperatorCatalog.STRING_CONCAT
                    }

                    is IdDslType.ListType -> IdOperatorCatalog.LIST_CONCAT
                    else -> throw fail(IdDiagnosticCode.E_TYPE, node.span, "++ is only defined for String and lists")
                }
                emitCall(
                    operator,
                    listOf(lower(operation.left.nodeId()), lower(operation.right.nodeId())),
                    type,
                    node.span,
                )
            }

            is IrOperation.Compose -> emitCall(
                IdOperatorCatalog.OPERATOR_PIPE,
                listOf(lower(operation.left.nodeId()), lower(operation.right.nodeId())),
                toDslType(node.type, node.span),
                node.span,
            )

            is IrOperation.Negate -> lowerNegate(node.id, operation.operand.nodeId())
        }
        memo[id] = ref
        return ref
    }

    private fun lowerPrimitive(id: Int, name: String): IdReference {
        val node = graph.nodes[id]
        val primitive = IdOperatorCatalog.resolvePrimitive(name)
            ?: throw fail(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                node.span,
                "primitive '$name' has no pinned native mapping",
            )
        return when (val lowering = primitive.lowering) {
            is IdPrimitiveLowering.Literal -> emitValue(
                toDslType(node.type, node.span),
                lowering.json,
                node.span,
            )

            is IdPrimitiveLowering.Native -> emitNative(
                lowering.operatorId,
                toDslType(node.type, node.span),
                node.span,
            )
        }
    }

    private fun lowerOperatorLiteral(id: Int, operatorId: String): IdReference {
        val node = graph.nodes[id]
        if (IdOperatorCatalog.resolveNative(operatorId) == null) {
            throw fail(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                node.span,
                "operator '$operatorId' is not in the pinned catalog",
            )
        }
        return emitNative(operatorId, toDslType(node.type, node.span), node.span)
    }

    private fun lowerList(id: Int, operation: IrOperation.ListLiteral): IdReference {
        val node = graph.nodes[id]
        val type = toDslType(node.type, node.span)
        if (type !is IdDslType.ListType) {
            throw fail(IdDiagnosticCode.E_TYPE, node.span, "list literal does not have a list type")
        }
        val static = staticLiteral(id)
        if (static != null) {
            return emitValue(type, static, node.span)
        }
        var current = emitValue(type, JsonArray(emptyList<JsonElement>()), node.span)
        for (element in operation.elements) {
            current = emitCall(
                IdOperatorCatalog.LIST_APPEND,
                listOf(current, lower(element.nodeId())),
                type,
                node.span,
            )
        }
        return current
    }

    private fun lowerNegate(id: Int, operandId: Int): IdReference {
        val node = graph.nodes[id]
        val type = toDslType(node.type, node.span)
        val zero = when (type) {
            IdDslTypes.INTEGER -> JsonPrimitive(0)
            IdDslTypes.LONG -> JsonPrimitive(0L)
            IdDslTypes.DOUBLE -> JsonPrimitive(0.0)
            else -> throw fail(IdDiagnosticCode.E_TYPE, node.span, "unary minus is only defined for numeric types")
        }
        val zeroRef = emitValue(type, zero, node.span)
        return emitCall(
            IdOperatorCatalog.ARITHMETIC_SUBTRACTION,
            listOf(zeroRef, lower(operandId)),
            type,
            node.span,
        )
    }

    private fun lowerApply(id: Int): IdReference {
        val node = graph.nodes[id]
        val (rootId, arguments) = flatten(id)
        val root = graph.nodes[rootId]
        return when (val operation = root.operation) {
            is IrOperation.Primitive -> {
                val primitive = IdOperatorCatalog.resolvePrimitive(operation.name)
                    ?: throw fail(
                        IdDiagnosticCode.E_NATIVE_CONTRACT,
                        root.span,
                        "primitive '${operation.name}' has no pinned native mapping",
                    )
                when (val lowering = primitive.lowering) {
                    is IdPrimitiveLowering.Literal -> throw fail(
                        IdDiagnosticCode.E_NOT_CALLABLE,
                        node.span,
                        "'${operation.name}' is a value and cannot be applied",
                    )

                    is IdPrimitiveLowering.Native -> lowerNativeInvocation(
                        lowering.operatorId,
                        rootId,
                        arguments,
                        node,
                    )
                }
            }

            is IrOperation.OperatorLiteral -> {
                if (IdOperatorCatalog.resolveNative(operation.id) == null) {
                    throw fail(
                        IdDiagnosticCode.E_NATIVE_CONTRACT,
                        root.span,
                        "operator '${operation.id}' is not in the pinned catalog",
                    )
                }
                lowerNativeInvocation(operation.id, rootId, arguments, node)
            }

            else -> {
                val rootType = toDslType(root.type, root.span)
                val calleeType = rootType as? IdDslType.FunctionType
                    ?: throw fail(IdDiagnosticCode.E_NOT_CALLABLE, root.span, "applied value is not a function")
                dynamicApply(
                    lower(rootId),
                    calleeType,
                    arguments,
                    node.span,
                    toDslType(node.type, node.span),
                )
            }
        }
    }

    private fun lowerNativeInvocation(
        operatorId: String,
        rootId: Int,
        arguments: List<Int>,
        top: com.rcdiamond.aifactory.lang.TypedNode,
    ): IdReference {
        val root = graph.nodes[rootId]
        val rootType = toDslType(root.type, root.span)
        val function = rootType as? IdDslType.FunctionType
            ?: throw fail(IdDiagnosticCode.E_NOT_CALLABLE, root.span, "native operator value is not applied as a function")
        val arity = function.parameters.size
        if (arguments.isEmpty()) {
            return emitNative(operatorId, rootType, root.span)
        }
        if (arity == 0) {
            throw fail(IdDiagnosticCode.E_NOT_CALLABLE, top.span, "a zero-parameter function cannot be applied with call syntax")
        }

        if (arguments.size < arity) {
            val callee = emitNative(operatorId, rootType, root.span)
            return dynamicApply(callee, function, arguments, top.span, toDslType(top.type, top.span))
        }

        val callType = function.result
        val call = emitCall(
            operatorId,
            arguments.take(arity).map { lower(it) },
            callType,
            top.span,
        )
        if (arguments.size == arity) {
            val expected = toDslType(top.type, top.span)
            if (callType != expected) {
                throw fail(
                    IdDiagnosticCode.E_TYPE,
                    top.span,
                    "native call result ${callType.display()} disagrees with checked type ${expected.display()}",
                )
            }
            return call
        }
        val remainingType = callType as? IdDslType.FunctionType
            ?: throw fail(
                IdDiagnosticCode.E_NOT_CALLABLE,
                top.span,
                "extra arguments applied to non-function native result ${callType.display()}",
            )
        return dynamicApply(
            call,
            remainingType,
            arguments.drop(arity),
            top.span,
            toDslType(top.type, top.span),
        )
    }

    private fun dynamicApply(
        callee: IdReference,
        calleeType: IdDslType.FunctionType,
        arguments: List<Int>,
        span: SourceSpan,
        expected: IdDslType,
    ): IdReference {
        var currentReference = callee
        var currentType: IdDslType = calleeType
        for (argument in arguments) {
            val function = currentType as? IdDslType.FunctionType
                ?: throw fail(IdDiagnosticCode.E_NOT_CALLABLE, span, "applied value is not a function")
            if (function.parameters.isEmpty()) {
                throw fail(IdDiagnosticCode.E_NOT_CALLABLE, span, "zero-parameter function applied with call syntax")
            }
            val argumentType = toDslType(graph.nodes[argument].type, graph.nodes[argument].span)
            if (function.parameters[0] != argumentType) {
                throw fail(
                    IdDiagnosticCode.E_FN_SIGNATURE,
                    span,
                    "argument has type ${argumentType.display()} but ${function.parameters[0].display()} is required",
                )
            }
            val result = if (function.parameters.size == 1) {
                function.result
            } else {
                IdDslType.FunctionType(function.parameters.drop(1), function.result)
            }
            currentReference = emitCall(
                IdOperatorCatalog.OPERATOR_APPLY,
                listOf(currentReference, lower(argument)),
                result,
                span,
            )
            currentType = result
        }
        if (currentType != expected) {
            throw fail(
                IdDiagnosticCode.E_TYPE,
                span,
                "application result ${currentType.display()} disagrees with checked type ${expected.display()}",
            )
        }
        return currentReference
    }

    private fun IrRef.nodeId(): Int = (this as IrRef.Node).id

    private fun flatten(topId: Int): Pair<Int, List<Int>> {
        var current = topId
        val reversed = ArrayList<Int>()
        while (true) {
            val operation = graph.nodes[current].operation
            if (operation !is IrOperation.Apply) break
            reversed += (operation.argument as IrRef.Node).id
            current = (operation.callee as IrRef.Node).id
        }
        reversed.reverse()
        return current to reversed
    }

    // ------------------------------------------------------------------ literals

    private fun staticLiteral(id: Int): JsonElement? {
        if (staticComputed[id]) return staticValues[id]
        staticComputed[id] = true
        val node = graph.nodes[id]
        val value: JsonElement? = when (val operation = node.operation) {
            is IrOperation.Constant -> encodeConstant(node)
            is IrOperation.Construct -> encodeConstruct(node)
            is IrOperation.Primitive -> {
                val primitive = IdOperatorCatalog.resolvePrimitive(operation.name)
                when (val lowering = primitive?.lowering) {
                    is IdPrimitiveLowering.Literal -> lowering.json
                    else -> null
                }
            }

            is IrOperation.ListLiteral -> {
                val elements = ArrayList<JsonElement>(operation.elements.size)
                var allStatic = true
                for (element in operation.elements) {
                    val literal = staticLiteral(element.nodeId())
                    if (literal == null) {
                        allStatic = false
                        break
                    }
                    elements += literal
                }
                if (allStatic) JsonArray(elements) else null
            }

            else -> null
        }
        staticValues[id] = value
        return value
    }

    private fun encodeConstant(node: com.rcdiamond.aifactory.lang.TypedNode): JsonElement {
        val type = toDslType(node.type, node.span) as? IdDslType.Scalar
            ?: throw fail(IdDiagnosticCode.E_TYPE, node.span, "scalar literal has a non-scalar type")
        return when (val value = (node.operation as IrOperation.Constant).value) {
            is LiteralValue.BooleanLiteral -> {
                if (type.name != "Boolean") throw fail(IdDiagnosticCode.E_TYPE, node.span, "Boolean literal type mismatch")
                JsonPrimitive(value.value)
            }

            is LiteralValue.IntegerLiteral -> {
                if (type.name != "Integer") throw fail(IdDiagnosticCode.E_TYPE, node.span, "Integer literal type mismatch")
                JsonPrimitive(value.value)
            }

            is LiteralValue.LongLiteral -> {
                if (type.name != "Long") throw fail(IdDiagnosticCode.E_TYPE, node.span, "Long literal type mismatch")
                JsonPrimitive(value.value)
            }

            is LiteralValue.DoubleLiteral -> {
                if (type.name != "Double") throw fail(IdDiagnosticCode.E_TYPE, node.span, "Double literal type mismatch")
                if (!value.value.isFinite()) throw fail(IdDiagnosticCode.E_LITERAL_RANGE, node.span, "Double literal is not finite")
                JsonPrimitive(value.value)
            }

            is LiteralValue.StringLiteral -> {
                if (type.name != "String") throw fail(IdDiagnosticCode.E_TYPE, node.span, "String literal type mismatch")
                checkText(value.value, node.span)
                JsonPrimitive(value.value)
            }
        }
    }

    private fun encodeConstruct(node: com.rcdiamond.aifactory.lang.TypedNode): JsonElement {
        val operation = node.operation as IrOperation.Construct
        val type = toDslType(node.type, node.span) as? IdDslType.Scalar
            ?: throw fail(IdDiagnosticCode.E_TYPE, node.span, "object literal has a non-scalar type")
        return when (operation.kind) {
            ObjectKind.ITEM, ObjectKind.BLOCK -> {
                val expected = if (operation.kind == ObjectKind.ITEM) "Item" else "Block"
                if (type.name != expected) throw fail(IdDiagnosticCode.E_TYPE, node.span, "$expected literal type mismatch")
                val text = singleText(operation.arguments, node.span)
                if (!IdDslWire.isResourceId(text)) {
                    throw fail(IdDiagnosticCode.E_LITERAL_RESOURCE, node.span, "'$text' is not a namespaced resource id")
                }
                buildJsonObject { put("resource", text) }
            }

            ObjectKind.FLUID -> {
                if (type.name != "Fluid") throw fail(IdDiagnosticCode.E_TYPE, node.span, "Fluid literal type mismatch")
                if (operation.arguments.size != 2) {
                    throw fail(IdDiagnosticCode.E_LITERAL_RANGE, node.span, "fluid needs a resource id and a positive amount")
                }
                val resource = (operation.arguments[0] as? IrStaticArgument.Text)?.value
                    ?: throw fail(IdDiagnosticCode.E_LITERAL_RESOURCE, node.span, "fluid resource must be text")
                val amount = (operation.arguments[1] as? IrStaticArgument.Integer)?.value
                    ?: throw fail(IdDiagnosticCode.E_LITERAL_RANGE, node.span, "fluid amount must be an Integer literal")
                if (!IdDslWire.isResourceId(resource)) {
                    throw fail(IdDiagnosticCode.E_LITERAL_RESOURCE, node.span, "'$resource' is not a namespaced resource id")
                }
                if (amount <= 0) {
                    throw fail(IdDiagnosticCode.E_LITERAL_RANGE, node.span, "fluid amount must be positive")
                }
                buildJsonObject {
                    put("resource", resource)
                    put("amount", amount)
                }
            }

            ObjectKind.NBT -> {
                if (type.name != "NBT") throw fail(IdDiagnosticCode.E_TYPE, node.span, "NBT literal type mismatch")
                val text = singleText(operation.arguments, node.span)
                checkText(text, node.span)
                IdSnbt.validate(text)?.let { reason ->
                    throw fail(IdDiagnosticCode.E_LITERAL_NBT, node.span, "invalid SNBT: $reason")
                }
                JsonPrimitive(text)
            }
        }
    }

    private fun singleText(arguments: List<IrStaticArgument>, span: SourceSpan): String {
        if (arguments.size != 1) {
            throw fail(IdDiagnosticCode.E_LITERAL_RANGE, span, "object literal needs exactly one text argument")
        }
        return (arguments[0] as? IrStaticArgument.Text)?.value
            ?: throw fail(IdDiagnosticCode.E_LITERAL_RESOURCE, span, "object literal resource must be text")
    }

    private fun checkText(text: String, span: SourceSpan) {
        if (utf8ByteLength(text, IdDslLimits.MAX_LITERAL_TEXT_BYTES) > IdDslLimits.MAX_LITERAL_TEXT_BYTES) {
            throw fail(
                IdDiagnosticCode.E_LIMIT,
                span,
                "literal text exceeds ${IdDslLimits.MAX_LITERAL_TEXT_BYTES} UTF-8 bytes",
            )
        }
    }

    // ------------------------------------------------------------------ emission

    private fun emitValue(type: IdDslType, value: JsonElement, span: SourceSpan): IdReference {
        val measurement = try {
            IdDslValueBudget.measure(
                value,
                remainingNodes = IdDslLimits.MAX_VALUE_JSON_NODES,
                remainingBytes = IdDslLimits.MAX_PLAN_BYTES.toLong(),
            )
        } catch (failure: IdValueBudgetExceeded) {
            throw fail(
                IdDiagnosticCode.E_LIMIT,
                span,
                failure.message ?: "plan value budget exceeded",
            )
        }
        val key = LiteralKey(type, value)
        valueNodes[key]?.let { return it }
        if (measurement.nodes > IdDslLimits.MAX_VALUE_JSON_NODES - valueJsonNodes ||
            measurement.bytes > IdDslLimits.MAX_PLAN_BYTES - valueBytes
        ) {
            throw fail(IdDiagnosticCode.E_LIMIT, span, "distinct plan values exceed the materialized JSON budget")
        }
        ensureNodeCapacity(span)
        valueJsonNodes += measurement.nodes
        valueBytes += measurement.bytes
        val id = nodes.size
        nodes += IdPlanNode.Value(id, type, value)
        sourceNodes[id] = span.toIdSpan()
        val ref = IdReference.Node(id)
        valueNodes[key] = ref
        return ref
    }

    private fun emitNative(operatorId: String, type: IdDslType, span: SourceSpan): IdReference {
        if (IdOperatorCatalog.resolveNative(operatorId) == null) {
            throw fail(IdDiagnosticCode.E_NATIVE_CONTRACT, span, "operator '$operatorId' is not in the pinned catalog")
        }
        ensureNodeCapacity(span)
        val id = nodes.size
        nodes += IdPlanNode.Native(id, type, operatorId)
        sourceNodes[id] = span.toIdSpan()
        return IdReference.Node(id)
    }

    private fun emitCall(
        operatorId: String,
        arguments: List<IdReference>,
        type: IdDslType,
        span: SourceSpan,
    ): IdReference {
        if (IdOperatorCatalog.resolveNative(operatorId) == null) {
            throw fail(IdDiagnosticCode.E_NATIVE_CONTRACT, span, "operator '$operatorId' is not in the pinned catalog")
        }
        ensureNodeCapacity(span)
        val id = nodes.size
        nodes += IdPlanNode.Call(id, type, operatorId, arguments)
        sourceNodes[id] = span.toIdSpan()
        return IdReference.Node(id)
    }

    private fun ensureNodeCapacity(span: SourceSpan) {
        if (nodes.size >= IdDslLimits.MAX_NODES) {
            throw fail(
                IdDiagnosticCode.E_LIMIT,
                span,
                "generated plan exceeds ${IdDslLimits.MAX_NODES} nodes",
            )
        }
    }

    // ------------------------------------------------------------------ types/ids

    private fun toDslType(type: LangType, span: SourceSpan): IdDslType {
        val converted = when (type) {
            is LangType.Nominal -> IdDslType.Scalar(type.name)
            is LangType.ListType -> IdDslType.ListType(toDslType(type.element, span))
            is LangType.FunctionType -> IdDslType.FunctionType(
                type.parameters.map { toDslType(it, span) },
                toDslType(type.result, span),
            )

            is LangType.Variable -> throw fail(
                IdDiagnosticCode.E_UNRESOLVED_TYPE,
                span,
                "unresolved type variable survived checking",
            )
        }
        if (!IdDslTypes.isSupported(converted)) {
            throw fail(IdDiagnosticCode.E_TYPE, span, "unsupported type ${converted.display()}")
        }
        if (converted.depth() > IdDslLimits.MAX_TYPE_DEPTH) {
            throw fail(IdDiagnosticCode.E_LIMIT, span, "type depth exceeds ${IdDslLimits.MAX_TYPE_DEPTH}")
        }
        return converted
    }

    private fun arithmeticId(operator: IrArithmeticOperator, span: SourceSpan): String = when (operator) {
        IrArithmeticOperator.ADD -> IdOperatorCatalog.ARITHMETIC_ADDITION
        IrArithmeticOperator.SUBTRACT -> IdOperatorCatalog.ARITHMETIC_SUBTRACTION
        IrArithmeticOperator.MULTIPLY -> IdOperatorCatalog.ARITHMETIC_MULTIPLICATION
        IrArithmeticOperator.DIVIDE -> IdOperatorCatalog.ARITHMETIC_DIVISION
        IrArithmeticOperator.MODULO -> IdOperatorCatalog.ARITHMETIC_MODULUS
    }

    private fun comparisonId(operator: IrComparisonOperator, span: SourceSpan): String = when (operator) {
        IrComparisonOperator.EQUAL -> IdOperatorCatalog.RELATIONAL_EQUALS
        IrComparisonOperator.NOT_EQUAL -> IdOperatorCatalog.RELATIONAL_NOTEQUALS
        IrComparisonOperator.LESS -> IdOperatorCatalog.RELATIONAL_LT
        IrComparisonOperator.LESS_EQUAL -> IdOperatorCatalog.RELATIONAL_LE
        IrComparisonOperator.GREATER -> IdOperatorCatalog.RELATIONAL_GT
        IrComparisonOperator.GREATER_EQUAL -> IdOperatorCatalog.RELATIONAL_GE
    }

    private fun fail(code: IdDiagnosticCode, span: SourceSpan, message: String): IdLoweringFailure =
        IdLoweringFailure(IdDslCompiler.loweringDiagnostic(code, source, span, message))

    private fun fail(
        code: IdDiagnosticCode,
        span: SourceSpan,
        message: String,
        hint: String?,
    ): IdLoweringFailure = IdLoweringFailure(IdDslCompiler.loweringDiagnostic(code, source, span, message, hint))
}
