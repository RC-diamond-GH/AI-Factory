package com.rcdiamond.aifactory.client.shared

/** Addon-specific protocol and presentation, not shared mutable agent state. */
data class SharedAgentProfile(
    val operationPrefix: String,
    val ownerField: String,
    val usesCells: Boolean,
    val browserTitleKey: String,
    val chatTitleKey: String,
    val systemInstruction: String = "",
)
