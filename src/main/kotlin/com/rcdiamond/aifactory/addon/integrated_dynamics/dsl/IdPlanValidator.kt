package com.rcdiamond.aifactory.addon.integrated_dynamics.dsl

import com.rcdiamond.aifactory.lang.LangType
import com.rcdiamond.aifactory.lang.LangTypes
import com.rcdiamond.aifactory.lang.SchemeConstraint
import com.rcdiamond.aifactory.lang.TypeConstraint
import com.rcdiamond.aifactory.lang.TypeScheme
import com.rcdiamond.aifactory.lang.utf8ByteLength
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Server-authoritative concrete type and finiteness of one resolved import. */
data class IdResolvedInput(val type: IdDslType, val finiteness: IdFiniteness)

data class IdPlanValidation(
    val ok: Boolean,
    val diagnostics: List<IdDiagnostic>,
    val plan: IdPlan?,
) {
    fun requireValid(): IdPlan =
        plan ?: throw IllegalStateException("plan is invalid: ${diagnostics.joinToString("; ") { it.message }}")
}

/**
 * Independent plan validator, usable by the server without a DSL parser or any Minecraft class.
 *
 * It re-checks the exact schema, limits, topology, types, catalog signatures, literal structure and
 * list finiteness. Import types must be supplied by the server from its own resolution; the plan's
 * declared import types are never trusted.
 */
object IdPlanValidator {
    fun validate(planJson: JsonObject, resolvedInputs: Map<String, IdResolvedInput>): IdPlanValidation {
        val plan = try {
            IdDslWire.parsePlan(planJson)
        } catch (failure: IdWireException) {
            return IdPlanValidation(
                ok = false,
                diagnostics = listOf(
                    IdDiagnostic(
                        code = failure.code,
                        severity = IdSeverity.ERROR,
                        message = failure.message,
                        hint = IdDslCompiler.hintFor(failure.code),
                        node = failure.nodeId,
                        input = failure.inputName,
                    ),
                ),
                plan = null,
            )
        }
        return validateParsed(plan, resolvedInputs)
    }

    fun validate(plan: IdPlan, resolvedInputs: Map<String, IdResolvedInput>): IdPlanValidation =
        validate(IdDslWire.encodePlan(plan), resolvedInputs)

    private fun validateParsed(plan: IdPlan, resolvedInputs: Map<String, IdResolvedInput>): IdPlanValidation {
        val diagnostics = ArrayList<IdDiagnostic>()

        fun error(code: IdDiagnosticCode, message: String, node: Int? = null, input: String? = null) {
            diagnostics += IdDiagnostic(
                code = code,
                severity = IdSeverity.ERROR,
                message = message,
                hint = IdDslCompiler.hintFor(code),
                node = node,
                input = input,
            )
        }

        fun warning(code: IdDiagnosticCode, message: String, node: Int? = null, input: String? = null) {
            diagnostics += IdDiagnostic(
                code = code,
                severity = IdSeverity.WARNING,
                message = message,
                hint = IdDslCompiler.hintFor(code),
                node = node,
                input = input,
            )
        }

        if (plan.nodes.size > IdDslLimits.MAX_NODES) {
            error(IdDiagnosticCode.E_LIMIT, "plan has more than ${IdDslLimits.MAX_NODES} nodes")
        }
        if (plan.imports.size > IdDslLimits.MAX_PORTS) {
            error(IdDiagnosticCode.E_LIMIT, "plan has more than ${IdDslLimits.MAX_PORTS} imports")
        }
        if (plan.outputs.isEmpty()) {
            error(IdDiagnosticCode.E_TARGET_CONTRACT, "plan has no outputs")
        }
        if (plan.outputs.size > IdDslLimits.MAX_PORTS) {
            error(IdDiagnosticCode.E_LIMIT, "plan has more than ${IdDslLimits.MAX_PORTS} outputs")
        }

        var valueJsonNodes = 0
        var valueBytes = 0L
        for (node in plan.nodes) {
            if (node !is IdPlanNode.Value) continue
            val measurement = try {
                IdDslValueBudget.measure(
                    node.value,
                    remainingNodes = IdDslLimits.MAX_VALUE_JSON_NODES - valueJsonNodes,
                    remainingBytes = IdDslLimits.MAX_PLAN_BYTES - valueBytes,
                )
            } catch (failure: IdValueBudgetExceeded) {
                error(
                    IdDiagnosticCode.E_LIMIT,
                    failure.message ?: "plan value budget exceeded",
                    node.id,
                    null,
                )
                return IdPlanValidation(false, diagnostics, null)
            }
            valueJsonNodes += measurement.nodes
            valueBytes += measurement.bytes
        }

        val importsByName = LinkedHashMap<String, IdPlanImport>()
        for (import in plan.imports) {
            if (importsByName.put(import.name, import) != null) {
                error(IdDiagnosticCode.E_INPUT_BINDING, "duplicate import '${import.name}'", input = import.name)
                continue
            }
            val resolved = resolvedInputs[import.name]
            if (resolved == null) {
                error(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    "import '${import.name}' was not resolved by the server",
                    input = import.name,
                )
            } else {
                if (resolved.type != import.type) {
                    error(
                        IdDiagnosticCode.E_TYPE,
                        "import '${import.name}' claims ${import.type.display()} but the server resolved ${resolved.type.display()}",
                        input = import.name,
                    )
                }
                if (resolved.finiteness != import.finiteness) {
                    error(
                        IdDiagnosticCode.E_INPUT_BINDING,
                        "import '${import.name}' claims ${import.finiteness.wire} but the server resolved ${resolved.finiteness.wire}",
                        input = import.name,
                    )
                }
            }
            val aspect = import.binding as? IdBinding.Aspect
            if (aspect != null) {
                val contract = IdAspectContracts.find(aspect.aspect)
                if (contract != null) {
                    if (contract.type != import.type) {
                        error(
                            IdDiagnosticCode.E_INPUT_BINDING,
                            "aspect '${aspect.aspect}' must read ${contract.type.display()}, not ${import.type.display()}",
                            input = import.name,
                        )
                    }
                    if (contract.finiteness != import.finiteness) {
                        error(
                            IdDiagnosticCode.E_INPUT_BINDING,
                            "aspect '${aspect.aspect}' must be ${contract.finiteness.wire}, not ${import.finiteness.wire}",
                            input = import.name,
                        )
                    }
                } else if (import.type is IdDslType.ListType) {
                    error(
                        IdDiagnosticCode.E_INPUT_BINDING,
                        "list aspect '${aspect.aspect}' has no fixed element-type contract",
                        input = import.name,
                    )
                }
            }
        }

        val outputNames = LinkedHashSet<String>()
        for (output in plan.outputs) {
            if (!outputNames.add(output.name)) {
                error(IdDiagnosticCode.E_TARGET_CONTRACT, "duplicate output port '${output.name}'")
            }
        }

        if (diagnostics.any { it.isError }) {
            return IdPlanValidation(false, diagnostics, null)
        }

        // Types/topology are checked by the decoder; verify semantic types here.
        for (node in plan.nodes) {
            if (!IdDslTypes.isSupported(node.type)) {
                error(IdDiagnosticCode.E_TYPE, "node ${node.id} has unsupported type ${node.type.display()}", node = node.id)
            }
            if (node.type.depth() > IdDslLimits.MAX_TYPE_DEPTH) {
                error(IdDiagnosticCode.E_LIMIT, "node ${node.id} type is too deep", node = node.id)
            }
        }
        if (diagnostics.any { it.isError }) {
            return IdPlanValidation(false, diagnostics, null)
        }

        val finiteness = IdPlanFiniteness(plan, importsByName)
        val applyCallees = BooleanArray(plan.nodes.size)
        for (node in plan.nodes) {
            if (node !is IdPlanNode.Call || node.operator != IdOperatorCatalog.OPERATOR_APPLY) continue
            val callee = node.arguments.firstOrNull() as? IdReference.Node ?: continue
            if (callee.id in applyCallees.indices) applyCallees[callee.id] = true
        }

        for (node in plan.nodes) {
            val referenceType: (IdReference) -> IdDslType? = { reference ->
                when (reference) {
                    is IdReference.Node -> plan.nodes.getOrNull(reference.id)?.type
                    is IdReference.Input -> importsByName[reference.name]?.type
                }
            }

            when (node) {
                is IdPlanNode.Value -> validateLiteral(node.type, node.value, node.id, 0, ::error)
                is IdPlanNode.Native -> validateNativeNode(node, ::error)
                is IdPlanNode.Call -> {
                    val argumentTypes = node.arguments.map { referenceType(it) }
                    if (argumentTypes.any { it == null }) {
                        error(IdDiagnosticCode.E_TYPE, "call node ${node.id} references an unknown argument", node = node.id)
                    } else {
                        validateCall(node, argumentTypes.filterNotNull(), ::error)
                        invalidSliceBounds(plan, node, applyCallees[node.id])?.let { message ->
                            error(IdDiagnosticCode.E_LITERAL_RANGE, message, node = node.id)
                        }
                        val native = IdOperatorCatalog.resolveNative(node.operator)
                        if (native != null) {
                            for (index in native.finiteListArguments) {
                                val argument = node.arguments.getOrNull(index)
                                if (argument == null) {
                                    error(
                                        IdDiagnosticCode.E_NATIVE_CONTRACT,
                                        "operator '${node.operator}' has no argument $index",
                                        node = node.id,
                                    )
                                    continue
                                }
                                when (finiteness.of(argument)) {
                                    IdFiniteness.INFINITE -> error(
                                        IdDiagnosticCode.E_INFINITE_CONSUMER,
                                        "operator '${native.id}' consumes a known-infinite list at argument $index",
                                        node = node.id,
                                    )

                                    IdFiniteness.UNKNOWN -> warning(
                                        IdDiagnosticCode.W_UNKNOWN_FINITENESS,
                                        "operator '${native.id}' consumes a list of unknown finiteness at argument $index; the native call may fail at runtime",
                                        node = node.id,
                                    )

                                    IdFiniteness.FINITE -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }

        for (output in plan.outputs) {
            val actual = when (val reference = output.reference) {
                is IdReference.Node -> plan.nodes.getOrNull(reference.id)?.type
                is IdReference.Input -> importsByName[reference.name]?.type
            }
            if (actual == null) {
                error(IdDiagnosticCode.E_TARGET_CONTRACT, "output '${output.name}' references a missing value")
            } else if (actual != output.type) {
                error(
                    IdDiagnosticCode.E_TYPE,
                    "output '${output.name}' claims ${output.type.display()} but its reference has ${actual.display()}",
                )
            }
        }

        // Only definitions reachable from outputs may create cards; imports must be used.
        val reachableNodes = LinkedHashSet<Int>()
        val reachableImports = LinkedHashSet<String>()
        fun visit(reference: IdReference) {
            when (reference) {
                is IdReference.Input -> reachableImports += reference.name
                is IdReference.Node -> {
                    if (!reachableNodes.add(reference.id)) return
                    val node = plan.nodes.getOrNull(reference.id) ?: return
                    for (child in references(node)) visit(child)
                }
            }
        }
        for (output in plan.outputs) visit(output.reference)
        plan.nodes.firstOrNull { it.id !in reachableNodes }?.let {
            error(IdDiagnosticCode.E_LIMIT, "node ${it.id} is not reachable from any output", node = it.id)
        }
        plan.imports.firstOrNull { it.name !in reachableImports }?.let {
            error(IdDiagnosticCode.E_INPUT_BINDING, "import '${it.name}' is not used by any output", input = it.name)
        }

        val ok = diagnostics.none { it.isError }
        return IdPlanValidation(ok, diagnostics, if (ok) plan else null)
    }

    // ------------------------------------------------------------------ signatures

    private fun validateNativeNode(
        node: IdPlanNode.Native,
        error: (IdDiagnosticCode, String, Int?, String?) -> Unit,
    ) {
        val native = IdOperatorCatalog.resolveNative(node.operator)
        if (native == null) {
            error(IdDiagnosticCode.E_NATIVE_CONTRACT, "operator '${node.operator}' is not in the pinned catalog", node.id, null)
            return
        }
        when (val signature = native.signature) {
            is IdNativeSignature.Fixed -> {
                val matcher = SchemeMatcher(signature.scheme)
                val expected = matcher.instantiate()
                if (!matcher.unify(expected, node.type.toLangType())) {
                    error(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "native operator '${native.id}' cannot have type ${node.type.display()}",
                        node.id,
                        null,
                    )
                } else {
                    matcher.finish()?.let { (code, message) -> error(code, message, node.id, null) }
                }
            }

            IdNativeSignature.Flip -> {
                if (!isFlipConstantType(node.type)) {
                    error(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "native operator '${native.id}' must have type Fn(Fn(a,b,...)->r)->Fn(b,a,...)->r, found ${node.type.display()}",
                        node.id,
                        null,
                    )
                }
            }

            IdNativeSignature.ApplyOne,
            IdNativeSignature.ApplyZero -> error(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                "'${native.id}' has no first-order operator-constant type",
                node.id,
                null,
            )
        }
    }

    private fun validateCall(
        node: IdPlanNode.Call,
        argumentTypes: List<IdDslType>,
        error: (IdDiagnosticCode, String, Int?, String?) -> Unit,
    ) {
        val native = IdOperatorCatalog.resolveNative(node.operator)
        if (native == null) {
            error(IdDiagnosticCode.E_NATIVE_CONTRACT, "operator '${node.operator}' is not in the pinned catalog", node.id, null)
            return
        }
        when (val signature = native.signature) {
            is IdNativeSignature.Fixed -> {
                val schemeType = signature.scheme.type as? LangType.FunctionType
                if (schemeType == null) {
                    error(IdDiagnosticCode.E_NATIVE_CONTRACT, "'${native.id}' has no callable fixed contract", node.id, null)
                    return
                }
                if (schemeType.parameters.size != argumentTypes.size) {
                    error(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "'${native.id}' needs ${schemeType.parameters.size} arguments, found ${argumentTypes.size}",
                        node.id,
                        null,
                    )
                    return
                }
                val matcher = SchemeMatcher(signature.scheme)
                val expected = matcher.instantiate() as LangType.FunctionType
                var valid = true
                for (index in argumentTypes.indices) {
                    if (!matcher.unify(expected.parameters[index], argumentTypes[index].toLangType())) {
                        error(
                            IdDiagnosticCode.E_FN_SIGNATURE,
                            "argument $index of '${native.id}' must be ${expected.parameters[index].display()}, found ${argumentTypes[index].display()}",
                            node.id,
                            null,
                        )
                        valid = false
                    }
                }
                if (valid && !matcher.unify(expected.result, node.type.toLangType())) {
                    error(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "'${native.id}' result cannot be ${node.type.display()}",
                        node.id,
                        null,
                    )
                } else if (valid) {
                    matcher.finish()?.let { (code, message) -> error(code, message, node.id, null) }
                }
            }

            IdNativeSignature.ApplyOne -> {
                if (argumentTypes.size != 2) {
                    error(IdDiagnosticCode.E_FN_SIGNATURE, "operator_apply needs exactly 2 arguments", node.id, null)
                    return
                }
                val callee = argumentTypes[0] as? IdDslType.FunctionType
                if (callee == null) {
                    error(IdDiagnosticCode.E_NOT_CALLABLE, "operator_apply first argument is not a function", node.id, null)
                    return
                }
                if (callee.parameters.isEmpty()) {
                    error(IdDiagnosticCode.E_NOT_CALLABLE, "operator_apply cannot apply a zero-argument function; use operator_apply0", node.id, null)
                    return
                }
                if (callee.parameters[0] != argumentTypes[1]) {
                    error(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "operator_apply argument must be ${callee.parameters[0].display()}, found ${argumentTypes[1].display()}",
                        node.id,
                        null,
                    )
                    return
                }
                val result = if (callee.parameters.size == 1) {
                    callee.result
                } else {
                    IdDslType.FunctionType(callee.parameters.drop(1), callee.result)
                }
                if (result != node.type) {
                    error(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "operator_apply result must be ${result.display()}, found ${node.type.display()}",
                        node.id,
                        null,
                    )
                }
            }

            IdNativeSignature.ApplyZero -> {
                if (argumentTypes.size != 1) {
                    error(IdDiagnosticCode.E_FN_SIGNATURE, "operator_apply0 needs exactly 1 argument", node.id, null)
                    return
                }
                val callee = argumentTypes[0] as? IdDslType.FunctionType
                if (callee == null || callee.parameters.isNotEmpty()) {
                    error(
                        IdDiagnosticCode.E_NOT_CALLABLE,
                        "operator_apply0 requires a zero-parameter function",
                        node.id,
                        null,
                    )
                    return
                }
                if (callee.result != node.type) {
                    error(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "operator_apply0 result must be ${callee.result.display()}, found ${node.type.display()}",
                        node.id,
                        null,
                    )
                }
            }

            IdNativeSignature.Flip -> {
                if (argumentTypes.size != 1) {
                    error(IdDiagnosticCode.E_FN_SIGNATURE, "operator_flip needs exactly 1 argument", node.id, null)
                    return
                }
                val inner = argumentTypes[0] as? IdDslType.FunctionType
                if (inner == null || inner.parameters.size < 2) {
                    error(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "operator_flip requires a function with at least two parameters",
                        node.id,
                        null,
                    )
                    return
                }
                val expected = flipResult(inner)
                if (expected != node.type) {
                    error(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "operator_flip result must be ${expected.display()}, found ${node.type.display()}",
                        node.id,
                        null,
                    )
                }
            }
        }
    }

    private fun flipResult(function: IdDslType.FunctionType): IdDslType.FunctionType {
        val parameters = ArrayList(function.parameters)
        val first = parameters[0]
        parameters[0] = parameters[1]
        parameters[1] = first
        return IdDslType.FunctionType(parameters, function.result)
    }

    private fun isFlipConstantType(type: IdDslType): Boolean {
        val outer = type as? IdDslType.FunctionType ?: return false
        if (outer.parameters.size != 1) return false
        val inner = outer.parameters[0] as? IdDslType.FunctionType ?: return false
        if (inner.parameters.size < 2) return false
        return outer.result == flipResult(inner)
    }

    // ------------------------------------------------------------------ literals

    private fun validateLiteral(
        type: IdDslType,
        value: JsonElement,
        node: Int,
        depth: Int,
        error: (IdDiagnosticCode, String, Int?, String?) -> Unit,
    ) {
        if (depth > IdDslLimits.MAX_TYPE_DEPTH) {
            error(IdDiagnosticCode.E_LIMIT, "literal nesting is too deep", node, null)
            return
        }
        when (type) {
            is IdDslType.Scalar -> validateScalarLiteral(type.name, value, node, error)
            is IdDslType.ListType -> {
                val array = value as? JsonArray ?: run {
                    error(IdDiagnosticCode.E_TYPE, "list literal must be a JSON array", node, null)
                    return
                }
                if (array.size > IdDslLimits.MAX_LIST_LENGTH) {
                    error(
                        IdDiagnosticCode.E_LIMIT,
                        "list literal has ${array.size} elements; the limit is ${IdDslLimits.MAX_LIST_LENGTH}",
                        node,
                        null,
                    )
                }
                for (element in array) validateLiteral(type.element, element, node, depth + 1, error)
            }

            is IdDslType.FunctionType -> error(
                IdDiagnosticCode.E_TYPE,
                "a function value cannot be encoded as a value literal",
                node,
                null,
            )
        }
    }

    private fun validateScalarLiteral(
        name: String,
        value: JsonElement,
        node: Int,
        error: (IdDiagnosticCode, String, Int?, String?) -> Unit,
    ) {
        when (name) {
            "Boolean" -> if (booleanOrNull(value) == null) {
                error(IdDiagnosticCode.E_TYPE, "Boolean literal must be true or false", node, null)
            }

            "Integer" -> if (IdDslWire.intElement(value) == null) {
                error(IdDiagnosticCode.E_TYPE, "Integer literal must be a 32-bit JSON integer", node, null)
            }

            "Long" -> if (IdDslWire.longElement(value) == null) {
                error(IdDiagnosticCode.E_TYPE, "Long literal must be a 64-bit JSON integer", node, null)
            }

            "Double" -> if (IdDslWire.doubleElement(value) == null) {
                error(IdDiagnosticCode.E_TYPE, "Double literal must be a finite JSON number", node, null)
            }

            "String" -> {
                val text = stringOrNull(value)
                if (text == null) error(IdDiagnosticCode.E_TYPE, "String literal must be a JSON string", node, null)
                else checkText(text, node, error)
            }

            "Item", "Block", "Fluid" -> validateObjectLiteral(name, value, node, error)
            "NBT" -> {
                val text = stringOrNull(value)
                if (text == null) {
                    error(IdDiagnosticCode.E_TYPE, "NBT literal must be an SNBT JSON string", node, null)
                } else {
                    checkText(text, node, error)
                    IdSnbt.validate(text)?.let { reason ->
                        error(IdDiagnosticCode.E_LITERAL_NBT, "invalid SNBT: $reason", node, null)
                    }
                }
            }

            "Entity", "Ingredients", "Recipe" ->
                error(IdDiagnosticCode.E_TYPE, "$name has no literal encoding in the DSL wire", node, null)

            else -> error(IdDiagnosticCode.E_TYPE, "unsupported literal type '$name'", node, null)
        }
    }

    private fun validateObjectLiteral(
        name: String,
        value: JsonElement,
        node: Int,
        error: (IdDiagnosticCode, String, Int?, String?) -> Unit,
    ) {
        if (name == "Item" && value is JsonNull) return // emptyItem
        if (name == "Fluid" && value is JsonNull) return // emptyFluid
        val obj = value as? JsonObject ?: run {
            error(IdDiagnosticCode.E_TYPE, "$name literal must be an object", node, null)
            return
        }
        when (name) {
            "Item", "Block" -> {
                if (obj.keys != setOf("resource")) {
                    error(IdDiagnosticCode.E_TYPE, "$name literal must be exactly {resource}", node, null)
                    return
                }
                val resource = stringOrNull(obj["resource"]!!)
                if (resource == null || !IdDslWire.isResourceId(resource)) {
                    error(IdDiagnosticCode.E_LITERAL_RESOURCE, "invalid $name resource id", node, null)
                }
            }

            "Fluid" -> {
                if (obj.keys != setOf("resource", "amount")) {
                    error(IdDiagnosticCode.E_TYPE, "Fluid literal must be exactly {resource,amount}", node, null)
                    return
                }
                val resource = stringOrNull(obj["resource"]!!)
                if (resource == null || !IdDslWire.isResourceId(resource)) {
                    error(IdDiagnosticCode.E_LITERAL_RESOURCE, "invalid Fluid resource id", node, null)
                }
                val amount = IdDslWire.intElement(obj["amount"]!!)
                if (amount == null || amount <= 0) {
                    error(IdDiagnosticCode.E_LITERAL_RANGE, "Fluid amount must be a positive Integer", node, null)
                }
            }
        }
    }

    private fun checkText(
        text: String,
        node: Int,
        error: (IdDiagnosticCode, String, Int?, String?) -> Unit,
    ) {
        val bytes = text.toByteArray(Charsets.UTF_8).size
        if (bytes > IdDslLimits.MAX_LITERAL_TEXT_BYTES) {
            error(
                IdDiagnosticCode.E_LIMIT,
                "literal text is $bytes bytes; the limit is ${IdDslLimits.MAX_LITERAL_TEXT_BYTES}",
                node,
                null,
            )
        }
    }

    private fun stringOrNull(value: JsonElement): String? {
        val primitive = value as? JsonPrimitive ?: return null
        return if (primitive.isString) primitive.content else null
    }

    private fun booleanOrNull(value: JsonElement): Boolean? {
        val primitive = value as? JsonPrimitive ?: return null
        if (primitive.isString) return null
        return when (primitive.content) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    private fun invalidSliceBounds(plan: IdPlan, node: IdPlanNode.Call, isApplyCallee: Boolean): String? {
        if (node.operator == IdOperatorCatalog.OPERATOR_APPLY && isApplyCallee) return null
        val arguments = sliceArguments(plan, node) ?: return null
        val from = arguments.getOrNull(1)?.let { integerLiteral(plan, it) }
        val to = arguments.getOrNull(2)?.let { integerLiteral(plan, it) }
        if (from != null && from < 0 || to != null && to <= 0 ||
            from != null && to != null && from >= to
        ) {
            val end = to?.toString() ?: if (arguments.size < 3) "not supplied" else "dynamic"
            return "slice requires 0 <= from < to; found from=${from ?: "dynamic"}, to=$end"
        }
        return null
    }

    /** Collects supplied native slice arguments without evaluating values or opaque input functions. */
    private fun sliceArguments(plan: IdPlan, node: IdPlanNode.Call): List<IdReference>? {
        if (node.operator == IdOperatorCatalog.LIST_SLICE) {
            return node.arguments.takeIf { it.size == 3 }
        }
        if (node.operator != IdOperatorCatalog.OPERATOR_APPLY) return null
        val arguments = ArrayList<IdReference>(3)
        var current: IdPlanNode = node
        while (current is IdPlanNode.Call && current.operator == IdOperatorCatalog.OPERATOR_APPLY) {
            if (current.arguments.size != 2 || arguments.size == 3) return null
            arguments += current.arguments[1]
            val callee = current.arguments[0] as? IdReference.Node ?: return null
            current = plan.nodes.getOrNull(callee.id) ?: return null
        }
        if (current !is IdPlanNode.Native || current.operator != IdOperatorCatalog.LIST_SLICE) return null
        arguments.reverse()
        return arguments
    }

    private fun integerLiteral(plan: IdPlan, reference: IdReference): Int? {
        val id = (reference as? IdReference.Node)?.id ?: return null
        val value = plan.nodes.getOrNull(id) as? IdPlanNode.Value ?: return null
        return if (value.type == IdDslTypes.INTEGER) IdDslWire.intElement(value.value) else null
    }

    // ------------------------------------------------------------------ finiteness

    private fun references(node: IdPlanNode): List<IdReference> = when (node) {
        is IdPlanNode.Value, is IdPlanNode.Native -> emptyList()
        is IdPlanNode.Call -> node.arguments
    }

    private class IdPlanFiniteness(
        private val plan: IdPlan,
        private val imports: Map<String, IdPlanImport>,
    ) {
        private val nativeFiniteness = HashMap<Int, IdFiniteness>()

        fun of(reference: IdReference): IdFiniteness = when (reference) {
            is IdReference.Input -> imports[reference.name]?.finiteness ?: IdFiniteness.UNKNOWN
            is IdReference.Node -> ofNode(reference.id)
        }

        private fun ofNode(id: Int): IdFiniteness {
            nativeFiniteness[id]?.let { return it }
            val node = plan.nodes.getOrNull(id) ?: return IdFiniteness.UNKNOWN
            val result = when (node) {
                is IdPlanNode.Value -> if (node.type is IdDslType.ListType) IdFiniteness.FINITE else IdFiniteness.UNKNOWN
                is IdPlanNode.Native -> IdFiniteness.UNKNOWN
                is IdPlanNode.Call -> callFiniteness(node)
            }
            nativeFiniteness[id] = result
            return result
        }

        private fun callFiniteness(node: IdPlanNode.Call): IdFiniteness {
            if (node.type !is IdDslType.ListType) return IdFiniteness.UNKNOWN
            return when (node.operator) {
                IdOperatorCatalog.LIST_LAZYBUILT -> IdFiniteness.INFINITE
                IdOperatorCatalog.LIST_APPEND -> of(node.arguments.getOrNull(0) ?: return IdFiniteness.UNKNOWN)
                IdOperatorCatalog.OPERATOR_MAP,
                IdOperatorCatalog.OPERATOR_FILTER -> of(node.arguments.getOrNull(1) ?: return IdFiniteness.UNKNOWN)

                IdOperatorCatalog.GENERAL_IDENTITY,
                IdOperatorCatalog.GENERAL_CONSTANT -> of(node.arguments.getOrNull(0) ?: return IdFiniteness.UNKNOWN)

                IdOperatorCatalog.LIST_CONCAT -> combine(
                    of(node.arguments.getOrNull(0) ?: return IdFiniteness.UNKNOWN),
                    of(node.arguments.getOrNull(1) ?: return IdFiniteness.UNKNOWN),
                )

                IdOperatorCatalog.LIST_SLICE,
                IdOperatorCatalog.OPERATOR_APPLY -> {
                    val arguments = sliceArguments(plan, node) ?: return IdFiniteness.UNKNOWN
                    if (arguments.size != 3) return IdFiniteness.UNKNOWN
                    val list = of(arguments[0])
                    if (list == IdFiniteness.FINITE) {
                        IdFiniteness.FINITE
                    } else {
                        val bound = integerLiteral(plan, arguments[2])
                        if (bound != null && bound > 0) IdFiniteness.FINITE else IdFiniteness.UNKNOWN
                    }
                }

                IdOperatorCatalog.GENERAL_CHOICE -> combine(
                    of(node.arguments.getOrNull(1) ?: return IdFiniteness.UNKNOWN),
                    of(node.arguments.getOrNull(2) ?: return IdFiniteness.UNKNOWN),
                )

                else -> IdFiniteness.UNKNOWN
            }
        }


        private fun combine(first: IdFiniteness, second: IdFiniteness): IdFiniteness = when {
            first == IdFiniteness.INFINITE || second == IdFiniteness.INFINITE -> IdFiniteness.INFINITE
            first == IdFiniteness.FINITE && second == IdFiniteness.FINITE -> IdFiniteness.FINITE
            else -> IdFiniteness.UNKNOWN
        }
    }

    // ------------------------------------------------------------------ unification

    private class SchemeMatcher(private val scheme: TypeScheme) {
        private val substitutions = HashMap<Int, LangType>()
        private val constraints = HashMap<Int, MutableList<TypeConstraint>>()
        private val quantified = LinkedHashSet<Int>()
        private var nextVariable = 10_000

        fun instantiate(): LangType {
            val replacement = HashMap<Int, LangType>()
            for (id in scheme.quantified) {
                val fresh = nextVariable++
                replacement[id] = LangType.Variable(fresh)
                quantified += fresh
                constraints.getOrPut(fresh) { ArrayList() }
            }
            for (constraint in scheme.constraints) {
                replacement[constraint.variableId]?.let { fresh ->
                    @Suppress("UNCHECKED_CAST")
                    constraints.getOrPut((fresh as LangType.Variable).id) { ArrayList() }.add(constraint.constraint)
                }
            }
            return replaceVariables(scheme.type, replacement)
        }

        fun unify(left: LangType, right: LangType): Boolean {
            val a = resolve(left)
            val b = resolve(right)
            if (a is LangType.Variable) {
                if (b is LangType.Variable && b.id == a.id) return true
                substitutions[a.id] = b
                return true
            }
            if (b is LangType.Variable) {
                substitutions[b.id] = a
                return true
            }
            return when {
                a is LangType.Nominal && b is LangType.Nominal -> a.name == b.name
                a is LangType.ListType && b is LangType.ListType -> unify(a.element, b.element)
                a is LangType.FunctionType && b is LangType.FunctionType -> {
                    if (a.parameters.size != b.parameters.size) {
                        false
                    } else {
                        a.parameters.indices.all { unify(a.parameters[it], b.parameters[it]) } &&
                            unify(a.result, b.result)
                    }
                }

                else -> false
            }
        }

        fun finish(): Pair<IdDiagnosticCode, String>? {
            for (id in quantified) {
                val type = resolve(LangType.Variable(id))
                if (type is LangType.Variable) {
                    return IdDiagnosticCode.E_UNRESOLVED_TYPE to "could not determine every type variable of the native contract"
                }
                for (constraint in constraints[id].orEmpty()) {
                    if (!accepts(type, constraint)) {
                        return IdDiagnosticCode.E_TYPE to "type ${type.display()} does not satisfy ${constraint.display()}"
                    }
                }
            }
            return null
        }

        private fun accepts(type: LangType, constraint: TypeConstraint): Boolean = when (constraint) {
            TypeConstraint.Numeric -> type is LangType.Nominal && type.name in IdDslTypes.NUMERIC_SCALARS
            TypeConstraint.Concat -> type is LangType.Nominal && type.name == "String" || type is LangType.ListType
            TypeConstraint.Equatable -> when (type) {
                is LangType.Nominal -> true
                is LangType.ListType -> accepts(type.element, constraint)
                is LangType.FunctionType, is LangType.Variable -> false
            }
            is TypeConstraint.Named -> false
        }

        private fun resolve(type: LangType): LangType = when (type) {
            is LangType.Variable -> {
                val bound = substitutions[type.id]
                if (bound == null) type else resolve(bound)
            }

            is LangType.Nominal -> type
            is LangType.ListType -> LangType.ListType(resolve(type.element))
            is LangType.FunctionType -> LangType.FunctionType(
                type.parameters.map { resolve(it) },
                resolve(type.result),
            )
        }

        private fun replaceVariables(type: LangType, replacement: Map<Int, LangType>): LangType = when (type) {
            is LangType.Variable -> replacement[type.id] ?: type
            is LangType.Nominal -> type
            is LangType.ListType -> LangType.ListType(replaceVariables(type.element, replacement))
            is LangType.FunctionType -> LangType.FunctionType(
                type.parameters.map { replaceVariables(it, replacement) },
                replaceVariables(type.result, replacement),
            )
        }
    }
}

/** Convert a concrete wire type to the shared frontend type used by catalog schemes. */
internal fun IdDslType.toLangType(): LangType = when (this) {
    is IdDslType.Scalar -> LangType.Nominal(name)
    is IdDslType.ListType -> LangTypes.list(element.toLangType())
    is IdDslType.FunctionType -> LangTypes.function(
        parameters.map { it.toLangType() },
        result.toLangType(),
    )
}
