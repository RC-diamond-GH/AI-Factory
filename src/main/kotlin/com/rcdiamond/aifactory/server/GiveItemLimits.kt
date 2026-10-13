package com.rcdiamond.aifactory.server

/**
 * Count limits independently enforced by the server, without loading game registries.
 */
internal object GiveItemLimits {
    /** A request must ask for at least one item. */
    const val MIN_COUNT: Int = 1

    /** At most 36 full stacks of 64 per request. */
    const val MAX_COUNT: Int = 2304

    fun isCountInRange(count: Int): Boolean = count in MIN_COUNT..MAX_COUNT

}
