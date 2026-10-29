package com.rcdiamond.aifactory.addon.integrated_dynamics.dsl

import kotlinx.serialization.json.JsonElement

/** Frozen wire/protocol version. */
const val ID_DSL_CONTRACT: String = "id-1.31.3-dsl-1"

object IdDslLimits {
    /** UTF-8 bytes of source text. */
    const val MAX_SOURCE_BYTES: Int = 32_768

    /** Structural depth of one type JSON. */
    const val MAX_TYPE_DEPTH: Int = 16

    /** Definitions (value/native/call nodes) in one plan. */
    const val MAX_NODES: Int = 256

    /** Shared typed IR nodes before static literals collapse into physical card definitions. */
    const val MAX_TYPED_NODES: Int = 8_192

    /** Named imports or named outputs in one plan. */
    const val MAX_PORTS: Int = 64

    /** UTF-8 bytes of one literal text (String, NBT/SNBT, resource id). */
    const val MAX_LITERAL_TEXT_BYTES: Int = 8_192

    /** Elements of one list literal, at every nesting level. */
    const val MAX_LIST_LENGTH: Int = 256

    /** Materialized JSON nodes across all plan value nodes, counting repeated shared subtrees. */
    const val MAX_VALUE_JSON_NODES: Int = 4_096

    /** Maximum UTF-8 size of one encoded plan JSON. */
    const val MAX_PLAN_BYTES: Int = 256 * 1024

    /** Physical JSON nesting, including type and plan envelopes, before schema decoding. */
    const val MAX_PLAN_JSON_DEPTH: Int = 64

    /** Retained diagnostics per compile/validation. */
    const val MAX_DIAGNOSTICS: Int = 64
}

/** One external input binding. Exactly one representation is possible. */
sealed interface IdBinding {
    data class Variable(val id: Int) : IdBinding

    data class Aspect(val partId: Int, val aspect: String) : IdBinding
}

/** A reference in a plan: an earlier node or a symbolic import. */
sealed interface IdReference {
    data class Node(val id: Int) : IdReference

    data class Input(val name: String) : IdReference
}

/** A concrete input declaration and its server-provided binding. */
data class IdInputPort(
    val name: String,
    val binding: IdBinding,
    val type: IdDslType,
    val finiteness: IdFiniteness,
)

sealed interface IdPlanNode {
    val id: Int
    val type: IdDslType

    data class Value(
        override val id: Int,
        override val type: IdDslType,
        val value: JsonElement,
    ) : IdPlanNode

    data class Native(
        override val id: Int,
        override val type: IdDslType,
        val operator: String,
    ) : IdPlanNode

    data class Call(
        override val id: Int,
        override val type: IdDslType,
        val operator: String,
        val arguments: List<IdReference>,
    ) : IdPlanNode
}

data class IdPlanImport(
    val name: String,
    val binding: IdBinding,
    val type: IdDslType,
    val finiteness: IdFiniteness,
)

data class IdPlanOutput(
    val name: String,
    val reference: IdReference,
    val type: IdDslType,
)

/**
 * Transport plan. `nodes[i].id == i`; every node reference points to a smaller id; imports are in
 * source declaration order; outputs are in source declaration order.
 */
data class IdPlan(
    val contract: String = ID_DSL_CONTRACT,
    val imports: List<IdPlanImport>,
    val nodes: List<IdPlanNode>,
    val outputs: List<IdPlanOutput>,
) {
    fun importByName(name: String): IdPlanImport? = imports.firstOrNull { it.name == name }

    fun node(id: Int): IdPlanNode = nodes[id]
}

/** Client-only mapping from plan references back to UTF-16 source ranges. */
data class IdSourceMap(
    val nodes: Map<Int, IdSourceSpan>,
    val imports: Map<String, IdSourceSpan>,
    val outputs: Map<String, IdSourceSpan>,
)

/** The parsed server description that supplies authoritative input types and bindings. */
data class IdInputDescription(val inputs: List<IdInputPort>) {
    fun byName(name: String): IdInputPort? = inputs.firstOrNull { it.name == name }
}
