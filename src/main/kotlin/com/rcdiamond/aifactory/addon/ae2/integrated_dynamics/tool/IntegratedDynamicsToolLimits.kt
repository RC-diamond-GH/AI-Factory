package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool

/**
 * Engine-independent names and bounds shared by the client-side Integrated Dynamics tools and the
 * server-side inspector.
 *
 * Both sides validate the same arguments against these numbers, and both import the matching
 * [IntegratedDynamicsToolArguments] parser, so a model-correctable rejection looks identical no
 * matter where it happens. Nothing here references Integrated Dynamics or Minecraft, which keeps the
 * client tools free of game classes and makes every bound testable without booting the game.
 */
object IntegratedDynamicsToolLimits {
    /** Summarizes the terminal's network: part and variable counts plus the aspects present. */
    const val OPERATION_NETWORK_INFO = "id_network_info"

    /** Lists one bounded page of the network's parts with their real ids and target positions. */
    const val OPERATION_LIST_PARTS = "id_list_parts"

    /** Lists one bounded page of the network's variable cards and their definition references. */
    const val OPERATION_LIST_VARIABLES = "id_list_variables"

    /**
     * Resolves input bindings to concrete, server-authoritative contracts. This never executes a
     * variable and is safe to call before compiling.
     */
    const val OPERATION_DESCRIBE_INPUTS = "id_describe_inputs"

    /** Creates real native variable cards from an already compiled, fully validated plan. */
    const val OPERATION_CREATE_CARDS = "id_create_cards"

    /** Every operation the session tool endpoint accepts; anything else is rejected. */
    val OPERATIONS: Set<String> = setOf(
        OPERATION_NETWORK_INFO,
        OPERATION_LIST_PARTS,
        OPERATION_LIST_VARIABLES,
        OPERATION_DESCRIBE_INPUTS,
        OPERATION_CREATE_CARDS,
    )

    /** Longest free-form filter text the model may send; matched case-insensitively by the server. */
    const val MAX_FILTER_LENGTH = 128

    /** Entries per page when `page_size` is omitted. */
    const val DEFAULT_PAGE_SIZE = 20

    /** Largest page the model may request. */
    const val MAX_PAGE_SIZE = 100

    /** Highest page number either listing accepts, which also bounds the total offset. */
    const val MAX_PAGE = 10_000
}
