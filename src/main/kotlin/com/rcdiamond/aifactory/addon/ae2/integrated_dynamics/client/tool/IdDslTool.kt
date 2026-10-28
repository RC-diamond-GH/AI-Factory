package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.client.tool

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdCompileResult
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslCompiler
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslLimits
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslWire
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdSourceSpan
import com.rcdiamond.aifactory.client.llm.ToolDefinition
import com.rcdiamond.aifactory.client.tool.ClientTool
import com.rcdiamond.aifactory.lang.InputDeclaration
import com.rcdiamond.aifactory.lang.LanguageLexer
import com.rcdiamond.aifactory.lang.LanguageParser
import com.rcdiamond.aifactory.lang.SourceText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.buildJsonObject

/** Pure compilation runs in the caller's cancellable job; only bounded native plans cross the game RPC. */
internal class IdDslTool(
    private val gateway: IntegratedDynamicsToolGateway,
    private val createCards: Boolean,
) : ClientTool {
    override val definition = ToolDefinition(
        name = if (createCards) MAKE_CARDS else COMPILE,
        description = (if (createCards) {
            "Compile an ID DSL program and create real native variable cards in your main inventory. " +
                "Use only when the user requests card creation. Consumes one truly blank ID variable card " +
                "for each definition and reader-aspect import; no creative bypass. Checks resources, " +
                "network access and inventory capacity before writing. Does not overwrite existing cards, " +
                "place cards, configure parts or execute logistics. Put all dependency cards in the same " +
                "network's Variable Store and install output cards manually. A timeout or cancellation " +
                "does not undo completed cards; inspect your inventory before retrying. "
        } else {
            "Preview compilation of a typed Haskell-like DSL into an ID native variable-card graph. " +
                "Returns definitions, output types, required blank cards and precise diagnostics. " +
                "Does not create cards, change inventory, deploy or evaluate live variables. "
        }) + LANGUAGE_HELP,
        parameters = IdDslArguments.schema(),
    )

    override suspend fun execute(arguments: JsonElement): JsonElement {
        val request = IdDslArguments.parse(arguments)
        val description = gateway.execute(DESCRIBE_INPUTS, buildJsonObject { put("bindings", request.bindings) })
        if (!description.isOk()) return backendFailure(description, "describe", request.source, null)

        val compiled = withContext(Dispatchers.Default) {
            IdDslCompiler.compile(request.source, description)
        }
        if (!compiled.success) return compileFailure(compiled)
        if (!createCards) return preview(compiled)

        // A disconnected/cancelled turn must not send a stale compiled result to another session.
        currentCoroutineContext().ensureActive()
        val result = gateway.execute(CREATE_CARDS, buildJsonObject { put("plan", checkNotNull(compiled.plan)) })
        return if (result.isOk()) result else backendFailure(result, "create", request.source, compiled)
    }

    private fun preview(compiled: IdCompileResult): JsonObject {
        val plan = checkNotNull(compiled.typedPlan)
        val encoded = checkNotNull(compiled.plan)
        return buildJsonObject {
            put("ok", true)
            put("phase", "compile")
            put("node_count", plan.nodes.size)
            put("cards_required", compiled.cardsRequired)
            put("output_count", plan.outputs.size)
            put("outputs", encoded.getValue("outputs"))
            put("plan", encoded)
            put("diagnostics", JsonArray(compiled.diagnostics.map(IdDslWire::encodeDiagnostic)))
            put("hint", "Preview only. No cards were created. Use id_make_cards only when creation is requested.")
        }
    }

    private fun compileFailure(compiled: IdCompileResult): JsonObject {
        val error = compiled.diagnostics.first { it.isError }
        return buildJsonObject {
            put("ok", false)
            put("phase", "compile")
            put("error", error.code.name)
            put("message", error.message)
            error.hint?.let { put("hint", it) }
            put("diagnostics", JsonArray(compiled.diagnostics.map(IdDslWire::encodeDiagnostic)))
        }
    }

    /** Preserve server evidence (including partial creation) and attach client-only source locations. */
    private fun backendFailure(
        reply: JsonObject,
        fallbackPhase: String,
        source: String,
        compiled: IdCompileResult?,
    ): JsonObject {
        val phase = reply.text("phase") ?: fallbackPhase
        val code = reply.text("code") ?: reply.text("error") ?: "E_TARGET_CONTRACT"
        val message = reply.text("message") ?: reply.text("error") ?: "The ID request was rejected"
        val text = SourceText.of("id-dsl", source)
        val raw = reply["diagnostics"] as? JsonArray
        val diagnostics = if (!raw.isNullOrEmpty()) {
            raw.take(IdDslLimits.MAX_DIAGNOSTICS).mapNotNull { it as? JsonObject }
        } else {
            listOf(buildJsonObject {
                put("code", code); put("severity", "error"); put("message", message)
                reply["node"]?.let { put("node", it) }
                reply["input"]?.let { put("input", it) }
                reply["hint"]?.let { put("hint", it) }
            })
        }
        val inputSpans = if (compiled == null && diagnostics.any { it.text("input") != null }) {
            val tokens = LanguageLexer(text).lex()
            LanguageParser(text, tokens.tokens).parse().program.declarations
                .filterIsInstance<InputDeclaration>()
                .associate { it.name to IdSourceSpan(it.nameSpan.start, it.nameSpan.end) }
        } else emptyMap()
        return buildJsonObject {
            for ((key, value) in reply) put(key, value)
            put("ok", false)
            put("phase", phase)
            put("error", code)
            put("message", message)
            putJsonArray("diagnostics") {
                for (diagnostic in diagnostics) {
                    val node = (diagnostic["node"] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
                    val input = diagnostic.text("input")
                    val span = node?.let { compiled?.sourceMap?.nodes?.get(it) }
                        ?: input?.let { compiled?.sourceMap?.imports?.get(it) ?: inputSpans[it] }
                    add(buildJsonObject {
                        for ((key, value) in diagnostic) put(key, value)
                        if (span != null) {
                            val position = text.positionAt(span.start)
                            put("line", position.line); put("column", position.column)
                            putJsonObject("span") { put("start", span.start); put("end", span.end) }
                        }
                    })
                }
            }
        }
    }

    private fun JsonObject.isOk(): Boolean = (this["ok"] as? JsonPrimitive)?.booleanOrNull == true

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    companion object {
        const val COMPILE = "id_compile"
        const val MAKE_CARDS = "id_make_cards"
        private const val DESCRIBE_INPUTS = "id_describe_inputs"
        private const val CREATE_CARDS = "id_create_cards"
        private const val LANGUAGE_HELP = "Language: input name : Type; let name [: Type] = expr; " +
            "output port [: Type] = expr; each ends with a semicolon. " +
            "Types: Boolean, Integer, Long (64L literals), Double, String, Item, Block, Fluid, NBT, " +
            "Entity, Ingredients, Recipe, [T], Fn(T1,T2)->R. No implicit numeric widening. " +
            "Expressions: f x y, lists, if/then/else, arithmetic/comparison, &&/||, ++ and unary function " +
            "composition >>. Builtins include eq/lt/flip/constant/identity, map/filter/reduce/iterate, " +
            "length/get/getOr/append/slice, itemCount/itemMod/itemIsEnchantable/itemIsStackable, " +
            "allOf/anyOf/negatePred/pipe2, toInteger/toLong/toDouble, emptyItem/emptyFluid. " +
            "Object literals: item \"minecraft:iron_ingot\", block \"minecraft:stone\", " +
            "fluid \"minecraft:water\" 1000, nbt \"{Damage:10}\". " +
            "Functions are native Operator values with partial application; no lambdas, user function " +
            "parameters, recursion or file imports. Bind declared inputs to real variable or aspect IDs " +
            "from inspection; unsupported or unprovable list/function input types are rejected rather " +
            "than accepted from annotations. Fix phase/code/line/column/hint diagnostics before creation."
    }
}
