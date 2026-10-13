package com.rcdiamond.aifactory.addon.ae2.item

import com.rcdiamond.aifactory.shared.SharedSessionLimits

/**
 * The five intelligent ME cell tiers.
 *
 * Every tier holds the same number of shared LLM sessions ([SharedSessionLimits.MAX_SESSIONS_PER_CELL],
 * eight for *all* tiers) out of one per-cell budget of [SharedSessionLimits.TOKENS_PER_ME_BYTE] context
 * tokens per ME byte. The byte capacities come from the shared core constants, so the ME drive, the
 * terminal screen and the client can never disagree about how much room a cell has.
 *
 * [idleDrain] mirrors the idle drain AE2 uses for the item storage cell of the same size
 * (0.5 / 1.0 / 1.5 / 2.0 / 2.5 AE/t), so an intelligent cell costs exactly as much to keep powered as the
 * vanilla cell it is built from.
 */
enum class MEIntelligentCellTier(
    /** Registry path of the cell item, for example `me_intelligent_cell_256k`. */
    val registryName: String,
    /** ME byte capacity available to the sessions bound to this cell. */
    val bytes: Long,
    /** Passive AE/t draw while the cell is mounted in a powered drive. */
    val idleDrain: Double,
) {
    K1("me_intelligent_cell_1k", SharedSessionLimits.CELL_1K_BYTES, 0.5),
    K4("me_intelligent_cell_4k", SharedSessionLimits.CELL_4K_BYTES, 1.0),
    K16("me_intelligent_cell_16k", SharedSessionLimits.CELL_16K_BYTES, 1.5),
    K64("me_intelligent_cell_64k", SharedSessionLimits.CELL_64K_BYTES, 2.0),
    K256("me_intelligent_cell_256k", SharedSessionLimits.CELL_256K_BYTES, 2.5),
    ;

    /** Context tokens this tier can hold in total (`bytes * 32`); 8 Mi for the 256k tier. */
    val tokenBudget: Long get() = bytes * SharedSessionLimits.TOKENS_PER_ME_BYTE
}
