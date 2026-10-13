package com.rcdiamond.aifactory.shared

/**
 * Capacity rules shared by every intelligent ME cell tier.
 *
 * A cell stores at most [MAX_SESSIONS_PER_CELL] LLM sessions regardless of its size (the corrected rule:
 * eight for *all* tiers), and every session's charge is drawn from the cell's shared budget of
 * [TOKENS_PER_ME_BYTE] context tokens per ME byte, using the fixed tokenizer in [SharedContextCodec].
 * The charge of one session is `ceil(tokens / 32)` ME bytes, so [CELL_256K_BYTES] holds
 * `8 x 1Mi` storage tokens when all eight sessions are full.
 *
 * These numbers are deliberately free of Minecraft and AE2 types so the client, the tests and the addon
 * agree on one source of truth.
 */
object SharedSessionLimits {
    /** Sessions one cell may hold, for every tier. */
    const val MAX_SESSIONS_PER_CELL: Int = 8

    /** Fixed-tokenizer context tokens one ME byte buys. */
    const val TOKENS_PER_ME_BYTE: Long = 32

    /** Longest accepted session title. */
    const val MAX_TITLE_LENGTH: Int = 128

    /** ME byte capacity of each cell tier. */
    const val CELL_1K_BYTES: Long = 1024L
    const val CELL_4K_BYTES: Long = 4096L
    const val CELL_16K_BYTES: Long = 16384L
    const val CELL_64K_BYTES: Long = 65536L
    const val CELL_256K_BYTES: Long = 262144L

    /** Context tokens one cell tier can hold in total. */
    const val CELL_256K_TOKENS: Long = CELL_256K_BYTES * TOKENS_PER_ME_BYTE

    /**
     * ME bytes one session with [tokens] context tokens is charged: `ceil(tokens / TOKENS_PER_ME_BYTE)`.
     * Negative input is clamped to zero so a malformed count can never create free space.
     */
    fun chargeFor(tokens: Long): Long {
        if (tokens <= 0) return 0
        return (tokens - 1) / TOKENS_PER_ME_BYTE + 1
    }
}
