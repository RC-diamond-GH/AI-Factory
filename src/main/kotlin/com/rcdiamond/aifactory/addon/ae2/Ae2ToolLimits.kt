package com.rcdiamond.aifactory.addon.ae2

/**
 * Engine-independent names and bounds shared by the client-side ME terminal tools and the
 * server-side executor.
 *
 * Both sides validate the same arguments against these numbers, so a model-correctable rejection
 * looks identical no matter where it happens. Nothing here references Applied Energistics or
 * Minecraft, which keeps the client tools free of AE2 types and makes the bounds testable.
 */
object Ae2ToolLimits {
    /** Reads a bounded page of the terminal's grid inventory. */
    const val OPERATION_READ_INVENTORY = "ae2_read_inventory"

    /** Reads one key's identity plus a bounded, navigable view of its NBT. */
    const val OPERATION_ITEM_DETAILS = "ae2_item_details"

    /** Computes a crafting plan (never submits a job). */
    const val OPERATION_CRAFTING_PLAN = "ae2_crafting_plan"

    /** Every operation the executor accepts; anything else is rejected. */
    val OPERATIONS: Set<String> = setOf(OPERATION_READ_INVENTORY, OPERATION_ITEM_DETAILS, OPERATION_CRAFTING_PLAN)

    /** Length of the lowercase hex SHA-256 fingerprint that identifies one full AE key. */
    const val KEY_FINGERPRINT_LENGTH = 64

    const val MAX_FILTER_LENGTH = 120

    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    const val MAX_PAGE = 100_000

    const val DEFAULT_NBT_DEPTH = 3
    const val MAX_NBT_DEPTH = 6
    const val MAX_PATH_LENGTH = 512
    const val MAX_PATH_STEPS = 16
    const val MAX_PATH_SEGMENT_LENGTH = 128
    const val MAX_PATH_INDEX = 4096

    /** Total nodes one rendered NBT view may contain. */
    const val MAX_RENDERED_NODES = 512
    const val MAX_RENDERED_STRING_LENGTH = 256
    const val MAX_ARRAY_PREVIEW = 32

    const val MAX_MISSING_ITEMS = 64

    const val MIN_AMOUNT = 1L
    const val MAX_AMOUNT = 1_000_000_000L

    /**
     * A pending plan is cancelled after this many server ticks (12 seconds at 20 TPS).
     * Low TPS can outlast the client's wall-clock tool timeout; no job is submitted in either case.
     */
    const val PLAN_TIMEOUT_TICKS = 240

    /** Plans that may be in flight at the same time across all players. */
    const val MAX_PENDING_PLANS = 4

    /** True for the lowercase 64 character hex fingerprints this addon hands out and accepts. */
    fun isItemKeyFingerprint(value: String): Boolean {
        if (value.length != KEY_FINGERPRINT_LENGTH) return false
        return value.all { it in '0'..'9' || it in 'a'..'f' }
    }
}
