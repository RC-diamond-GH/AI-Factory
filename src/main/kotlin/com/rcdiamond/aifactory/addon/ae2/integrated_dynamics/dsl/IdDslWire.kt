package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** A schema/limit failure while decoding wire JSON. */
class IdWireException(
    val code: IdDiagnosticCode,
    override val message: String,
    val nodeId: Int? = null,
    val inputName: String? = null,
) : RuntimeException(message)

/**
 * Exact JSON codec for the frozen ID DSL wire.
 *
 * This is pure: it never resolves a binding, a resource or an operator, and it never parses DSL
 * source. [parsePlan] bounds raw JSON before decoding shape, types and local topology; signature and
 * literal semantic checks live in [IdPlanValidator] so a server can run both independently.
 */
object IdDslWire {
    private val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
    private val RESOURCE_ID = Regex("[a-z0-9_.-]+:[a-z0-9/._-]+")
    private val INTEGER_TEXT = Regex("-?(0|[1-9][0-9]*)")
    private val LONG_TEXT = Regex("-?(0|[1-9][0-9]*)")
    private val DOUBLE_TEXT = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")

    fun isIdentifier(value: String): Boolean = IDENTIFIER.matches(value)

    fun isResourceId(value: String): Boolean =
        value.length <= IdDslLimits.MAX_LITERAL_TEXT_BYTES && RESOURCE_ID.matches(value)

    fun isWireFinite(value: String): Boolean = IdFiniteness.fromWire(value) != null

    // ------------------------------------------------------------------ types

    fun encodeType(type: IdDslType): JsonElement = when (type) {
        is IdDslType.Scalar -> JsonPrimitive(type.name)
        is IdDslType.ListType -> buildJsonObject { put("list", encodeType(type.element)) }
        is IdDslType.FunctionType -> buildJsonObject {
            putJsonArray("fn") { type.parameters.forEach { add(encodeType(it)) } }
            put("result", encodeType(type.result))
        }
    }

    fun parseType(json: JsonElement): IdDslType = parseType(json, 0)

    private fun parseType(json: JsonElement, depth: Int): IdDslType {
        if (depth > IdDslLimits.MAX_TYPE_DEPTH) {
            throw IdWireException(IdDiagnosticCode.E_LIMIT, "type depth exceeds ${IdDslLimits.MAX_TYPE_DEPTH}")
        }
        return when (json) {
            is JsonPrimitive -> {
                if (!json.isString || !isIdentifier(json.content)) {
                    throw IdWireException(IdDiagnosticCode.E_TYPE, "a scalar type must be an ASCII identifier string")
                }
                IdDslType.Scalar(json.content)
            }

            is JsonObject -> when {
                json.keys == setOf("list") -> {
                    IdDslType.ListType(parseType(required(json, "list"), depth + 1))
                }

                json.keys == setOf("fn", "result") -> {
                    val parameters = required(json, "fn")
                    if (parameters !is JsonArray) {
                        throw IdWireException(IdDiagnosticCode.E_TYPE, "type 'fn' must be an array")
                    }
                    if (parameters.size > IdDslLimits.MAX_TYPE_DEPTH) {
                        throw IdWireException(IdDiagnosticCode.E_LIMIT, "function type has too many parameters")
                    }
                    IdDslType.FunctionType(
                        parameters.map { parseType(it, depth + 1) },
                        parseType(required(json, "result"), depth + 1),
                    )
                }

                else -> throw IdWireException(
                    IdDiagnosticCode.E_TYPE,
                    "a type object must be exactly {list:T} or {fn:[...],result:T}",
                )
            }

            else -> throw IdWireException(IdDiagnosticCode.E_TYPE, "a type must be a string or an object")
        }
    }

    // ------------------------------------------------------------------ bindings

    fun encodeBinding(binding: IdBinding): JsonObject = when (binding) {
        is IdBinding.Variable -> buildJsonObject {
            put("kind", "variable")
            put("id", binding.id)
        }

        is IdBinding.Aspect -> buildJsonObject {
            put("kind", "aspect")
            put("part_id", binding.partId)
            put("aspect", binding.aspect)
        }
    }

    fun parseBinding(json: JsonElement): IdBinding {
        val obj = json as? JsonObject
            ?: throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "binding must be an object")
        val kind = stringField(obj, "kind")
        return when (kind) {
            "variable" -> {
                requireFields(obj, setOf("kind", "id"), "variable binding")
                IdBinding.Variable(integerField(obj, "id", minimum = 0))
            }

            "aspect" -> {
                requireFields(obj, setOf("kind", "part_id", "aspect"), "aspect binding")
                val aspect = stringField(obj, "aspect")
                if (!isResourceId(aspect)) {
                    throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "aspect is not a namespaced resource id: $aspect")
                }
                IdBinding.Aspect(integerField(obj, "part_id", minimum = 0), aspect)
            }

            else -> throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "unknown binding kind '$kind'")
        }
    }

    // ------------------------------------------------------------------ description

    fun parseDescription(json: JsonObject): IdInputDescription {
        val allowed = setOf("ok", "contract", "inputs")
        requireFields(json, allowed, "describe response")
        if (json.containsKey("ok")) {
            if (booleanElement(json["ok"]!!) != true) {
                throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "describe response must have ok=true")
            }
        }
        json["contract"]?.let {
            val contract = stringElement(it)
            if (contract != ID_DSL_CONTRACT) {
                throw IdWireException(
                    IdDiagnosticCode.E_TARGET_CONTRACT,
                    "describe contract '$contract' does not match '$ID_DSL_CONTRACT'",
                )
            }
        }
        val inputsJson = json["inputs"] as? JsonObject
            ?: throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "describe response has no inputs object")
        if (inputsJson.size > IdDslLimits.MAX_PORTS) {
            throw IdWireException(IdDiagnosticCode.E_LIMIT, "description has more than ${IdDslLimits.MAX_PORTS} inputs")
        }

        val inputs = ArrayList<IdInputPort>(inputsJson.size)
        for ((name, value) in inputsJson) {
            if (!isIdentifier(name)) {
                throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "'$name' is not an input identifier", inputName = name)
            }
            val obj = value as? JsonObject
                ?: throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "input '$name' must be an object", inputName = name)
            requireFields(obj, setOf("binding", "type", "finite"), "input '$name'")
            val type = parseType(required(obj, "type"))
            if (!IdDslTypes.isSupported(type)) {
                throw IdWireException(
                    IdDiagnosticCode.E_TYPE,
                    "input '$name' has unsupported type ${type.display()}",
                    inputName = name,
                )
            }
            val finiteText = stringField(obj, "finite")
            val finite = IdFiniteness.fromWire(finiteText)
                ?: throw IdWireException(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    "input '$name' has invalid finite value '$finiteText'",
                    inputName = name,
                )
            inputs += IdInputPort(
                name,
                parseBinding(required(obj, "binding")),
                type,
                finite,
            )
        }
        return IdInputDescription(inputs)
    }

    fun encodeDescription(description: IdInputDescription): JsonObject = buildJsonObject {
        put("ok", true)
        put("contract", ID_DSL_CONTRACT)
        putJsonObject("inputs") {
            for (input in description.inputs) {
                putJsonObject(input.name) {
                    put("binding", encodeBinding(input.binding))
                    put("type", encodeType(input.type))
                    put("finite", input.finiteness.wire)
                }
            }
        }
    }

    // ------------------------------------------------------------------ plan

    fun encodePlan(plan: IdPlan): JsonObject = buildJsonObject {
        put("version", 1)
        put("contract", plan.contract)
        putJsonObject("imports") {
            for (import in plan.imports) {
                putJsonObject(import.name) {
                    put("binding", encodeBinding(import.binding))
                    put("type", encodeType(import.type))
                    put("finite", import.finiteness.wire)
                }
            }
        }
        putJsonArray("nodes") {
            for (node in plan.nodes) {
                add(encodeNode(node))
            }
        }
        putJsonObject("outputs") {
            for (output in plan.outputs) {
                putJsonObject(output.name) {
                    put("ref", encodeReference(output.reference))
                    put("type", encodeType(output.type))
                }
            }
        }
    }

    fun encodeNode(node: IdPlanNode): JsonObject = buildJsonObject {
        put("id", node.id)
        put("type", encodeType(node.type))
        when (node) {
            is IdPlanNode.Value -> {
                put("kind", "value")
                put("value", node.value)
            }

            is IdPlanNode.Native -> {
                put("kind", "native")
                put("operator", node.operator)
            }

            is IdPlanNode.Call -> {
                put("kind", "call")
                put("operator", node.operator)
                putJsonArray("args") { node.arguments.forEach { add(encodeReference(it)) } }
            }
        }
    }

    fun encodeReference(reference: IdReference): JsonObject = buildJsonObject {
        when (reference) {
            is IdReference.Node -> put("node", reference.id)
            is IdReference.Input -> put("input", reference.name)
        }
    }

    fun parsePlan(json: JsonObject): IdPlan {
        try {
            IdDslValueBudget.measure(
                json,
                remainingNodes = 100_000,
                remainingBytes = IdDslLimits.MAX_PLAN_BYTES.toLong(),
                maxDepth = IdDslLimits.MAX_PLAN_JSON_DEPTH,
            )
        } catch (failure: IdValueBudgetExceeded) {
            throw IdWireException(
                IdDiagnosticCode.E_LIMIT,
                failure.message,
            )
        }
        requireFields(json, setOf("version", "contract", "imports", "nodes", "outputs"), "plan")
        val version = integerField(json, "version")
        if (version != 1) throw IdWireException(IdDiagnosticCode.E_TARGET_CONTRACT, "unsupported plan version $version")
        val contract = stringField(json, "contract")
        if (contract != ID_DSL_CONTRACT) {
            throw IdWireException(IdDiagnosticCode.E_TARGET_CONTRACT, "plan contract '$contract' is not '$ID_DSL_CONTRACT'")
        }

        val importsJson = required(json, "imports") as? JsonObject
            ?: throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "plan imports must be an object")
        if (importsJson.size > IdDslLimits.MAX_PORTS) {
            throw IdWireException(IdDiagnosticCode.E_LIMIT, "plan imports exceed ${IdDslLimits.MAX_PORTS}")
        }
        val imports = ArrayList<IdPlanImport>(importsJson.size)
        for ((name, value) in importsJson) {
            if (!isIdentifier(name)) {
                throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "import '$name' is not an identifier", inputName = name)
            }
            val obj = value as? JsonObject
                ?: throw IdWireException(IdDiagnosticCode.E_INPUT_BINDING, "import '$name' must be an object", inputName = name)
            requireFields(obj, setOf("binding", "type", "finite"), "import '$name'")
            val type = parseType(required(obj, "type"))
            if (!IdDslTypes.isSupported(type)) {
                throw IdWireException(
                    IdDiagnosticCode.E_TYPE,
                    "import '$name' has unsupported type ${type.display()}",
                    inputName = name,
                )
            }
            val finiteText = stringField(obj, "finite")
            val finite = IdFiniteness.fromWire(finiteText)
                ?: throw IdWireException(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    "import '$name' has invalid finite value '$finiteText'",
                    inputName = name,
                )
            imports += IdPlanImport(name, parseBinding(required(obj, "binding")), type, finite)
        }

        val nodesJson = required(json, "nodes") as? JsonArray
            ?: throw IdWireException(IdDiagnosticCode.E_LIMIT, "plan nodes must be an array")
        if (nodesJson.size > IdDslLimits.MAX_NODES) {
            throw IdWireException(IdDiagnosticCode.E_LIMIT, "plan has more than ${IdDslLimits.MAX_NODES} nodes")
        }
        val importNames = imports.mapTo(LinkedHashSet()) { it.name }
        val nodes = ArrayList<IdPlanNode>(nodesJson.size)
        for ((index, value) in nodesJson.withIndex()) {
            val obj = value as? JsonObject
                ?: throw IdWireException(IdDiagnosticCode.E_TYPE, "node $index must be an object", nodeId = index)
            if (integerField(obj, "id") != index) {
                throw IdWireException(IdDiagnosticCode.E_TYPE, "node id must equal its array index $index", nodeId = index)
            }
            val type = parseType(required(obj, "type"))
            if (!IdDslTypes.isSupported(type)) {
                throw IdWireException(
                    IdDiagnosticCode.E_TYPE,
                    "node $index has unsupported type ${type.display()}",
                    nodeId = index,
                )
            }
            nodes += parseNode(obj, index, type, importNames, index)
        }

        val outputsJson = required(json, "outputs") as? JsonObject
            ?: throw IdWireException(IdDiagnosticCode.E_TARGET_CONTRACT, "plan outputs must be an object")
        if (outputsJson.isEmpty()) {
            throw IdWireException(IdDiagnosticCode.E_TARGET_CONTRACT, "plan must have at least one output")
        }
        if (outputsJson.size > IdDslLimits.MAX_PORTS) {
            throw IdWireException(IdDiagnosticCode.E_LIMIT, "plan outputs exceed ${IdDslLimits.MAX_PORTS}")
        }
        val outputs = ArrayList<IdPlanOutput>(outputsJson.size)
        for ((name, value) in outputsJson) {
            if (!isIdentifier(name)) {
                throw IdWireException(IdDiagnosticCode.E_TARGET_CONTRACT, "output '$name' is not an identifier")
            }
            val obj = value as? JsonObject
                ?: throw IdWireException(IdDiagnosticCode.E_TARGET_CONTRACT, "output '$name' must be an object")
            requireFields(obj, setOf("ref", "type"), "output '$name'")
            val type = parseType(required(obj, "type"))
            if (!IdDslTypes.isSupported(type)) {
                throw IdWireException(IdDiagnosticCode.E_TYPE, "output '$name' has unsupported type ${type.display()}")
            }
            val reference = parseReference(required(obj, "ref"), nodes.size, importNames)
            outputs += IdPlanOutput(name, reference, type)
        }

        return IdPlan(contract, imports, nodes, outputs)
    }

    private fun parseNode(
        obj: JsonObject,
        index: Int,
        type: IdDslType,
        importNames: Set<String>,
        maxNodeExclusive: Int,
    ): IdPlanNode {
        val kind = stringField(obj, "kind")
        return when (kind) {
            "value" -> {
                requireFields(obj, setOf("id", "kind", "type", "value"), "value node $index")
                IdPlanNode.Value(index, type, required(obj, "value"))
            }

            "native" -> {
                requireFields(obj, setOf("id", "kind", "type", "operator"), "native node $index")
                IdPlanNode.Native(index, type, operatorField(obj, index))
            }

            "call" -> {
                requireFields(obj, setOf("id", "kind", "type", "operator", "args"), "call node $index")
                val argsJson = required(obj, "args") as? JsonArray
                    ?: throw IdWireException(IdDiagnosticCode.E_TYPE, "call node $index args must be an array", nodeId = index)
                if (argsJson.size > IdDslLimits.MAX_LIST_LENGTH) {
                    throw IdWireException(IdDiagnosticCode.E_LIMIT, "call node $index has too many arguments", nodeId = index)
                }
                IdPlanNode.Call(
                    index,
                    type,
                    operatorField(obj, index),
                    argsJson.map { parseReference(it, maxNodeExclusive, importNames) },
                )
            }

            else -> throw IdWireException(IdDiagnosticCode.E_TYPE, "node $index has unknown kind '$kind'", nodeId = index)
        }
    }

    private fun operatorField(obj: JsonObject, index: Int): String {
        val operator = stringField(obj, "operator")
        if (!isResourceId(operator)) {
            throw IdWireException(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                "node $index operator is not a namespaced registered id: $operator",
                nodeId = index,
            )
        }
        return operator
    }

    private fun parseReference(
        json: JsonElement,
        maxNodeExclusive: Int,
        importNames: Set<String>,
    ): IdReference {
        val obj = json as? JsonObject
            ?: throw IdWireException(IdDiagnosticCode.E_TYPE, "reference must be an object")
        return when {
            obj.keys == setOf("node") -> {
                val id = integerField(obj, "node", minimum = 0)
                if (id >= maxNodeExclusive) {
                    throw IdWireException(
                        IdDiagnosticCode.E_TYPE,
                        "reference to node $id is not earlier than the consumer (max ${maxNodeExclusive - 1})",
                        nodeId = id,
                    )
                }
                IdReference.Node(id)
            }

            obj.keys == setOf("input") -> {
                val name = stringField(obj, "input")
                if (name !in importNames) {
                    throw IdWireException(
                        IdDiagnosticCode.E_INPUT_BINDING,
                        "reference to unknown import '$name'",
                        inputName = name,
                    )
                }
                IdReference.Input(name)
            }

            else -> throw IdWireException(
                IdDiagnosticCode.E_TYPE,
                "a reference must be exactly {node:int} or {input:string}",
            )
        }
    }

    // ------------------------------------------------------------------ source map/diagnostics

    fun encodeSourceMap(map: IdSourceMap): JsonObject = buildJsonObject {
        putJsonObject("nodes") {
            for ((id, span) in map.nodes.toSortedMap()) {
                putJsonObject(id.toString()) {
                    put("start", span.start)
                    put("end", span.end)
                }
            }
        }
        putJsonObject("imports") {
            for ((name, span) in map.imports) {
                putJsonObject(name) {
                    put("start", span.start)
                    put("end", span.end)
                }
            }
        }
        putJsonObject("outputs") {
            for ((name, span) in map.outputs) {
                putJsonObject(name) {
                    put("start", span.start)
                    put("end", span.end)
                }
            }
        }
    }

    fun encodeDiagnostic(diagnostic: IdDiagnostic): JsonObject = buildJsonObject {
        put("code", diagnostic.code.name)
        put("severity", if (diagnostic.isError) "error" else "warning")
        put("message", diagnostic.message)
        putJsonObject("span") {
            put("start", diagnostic.span.start)
            put("end", diagnostic.span.end)
        }
        put("line", diagnostic.line)
        put("column", diagnostic.column)
        diagnostic.hint?.let { put("hint", it) }
        diagnostic.node?.let { put("node", it) }
        diagnostic.input?.let { put("input", it) }
        if (diagnostic.related.isNotEmpty()) {
            putJsonArray("related") {
                for (item in diagnostic.related) {
                    add(buildJsonObject {
                        put("message", item.message)
                        putJsonObject("span") {
                            put("start", item.span.start)
                            put("end", item.span.end)
                        }
                    })
                }
            }
        }
    }

    // ------------------------------------------------------------------ field helpers

    private fun required(obj: JsonObject, key: String): JsonElement =
        obj[key] ?: throw IdWireException(IdDiagnosticCode.E_TYPE, "missing required field '$key'")

    private fun requireFields(obj: JsonObject, allowed: Set<String>, what: String) {
        val extra = obj.keys - allowed
        if (extra.isNotEmpty()) {
            throw IdWireException(IdDiagnosticCode.E_TYPE, "$what has unknown field(s) ${extra.sorted().joinToString()}")
        }
    }

    private fun stringField(obj: JsonObject, key: String): String =
        stringElement(required(obj, key))

    private fun stringElement(json: JsonElement): String {
        val primitive = json as? JsonPrimitive
            ?: throw IdWireException(IdDiagnosticCode.E_TYPE, "expected a string")
        if (!primitive.isString) throw IdWireException(IdDiagnosticCode.E_TYPE, "expected a string")
        return primitive.content
    }

    private fun integerField(obj: JsonObject, key: String, minimum: Int? = null): Int {
        val value = integerElement(required(obj, key))
            ?: throw IdWireException(IdDiagnosticCode.E_TYPE, "field '$key' must be a JSON integer")
        if (minimum != null && value < minimum) {
            throw IdWireException(IdDiagnosticCode.E_TYPE, "field '$key' must be at least $minimum")
        }
        return value
    }

    private fun integerElement(json: JsonElement): Int? {
        val primitive = json as? JsonPrimitive ?: return null
        if (primitive.isString) return null
        if (!INTEGER_TEXT.matches(primitive.content)) return null
        return primitive.content.toIntOrNull()
    }

    private fun booleanElement(json: JsonElement): Boolean? {
        val primitive = json as? JsonPrimitive ?: return null
        if (primitive.isString) return null
        return when (primitive.content) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    fun longElement(json: JsonElement): Long? {
        val primitive = json as? JsonPrimitive ?: return null
        if (primitive.isString) return null
        if (!LONG_TEXT.matches(primitive.content)) return null
        return primitive.content.toLongOrNull()
    }

    fun doubleElement(json: JsonElement): Double? {
        val primitive = json as? JsonPrimitive ?: return null
        if (primitive.isString) return null
        if (!DOUBLE_TEXT.matches(primitive.content)) return null
        val value = primitive.content.toDoubleOrNull() ?: return null
        return if (value.isFinite()) value else null
    }

    fun intElement(json: JsonElement): Int? = integerElement(json)

    fun isNull(json: JsonElement): Boolean = json is JsonNull
}
