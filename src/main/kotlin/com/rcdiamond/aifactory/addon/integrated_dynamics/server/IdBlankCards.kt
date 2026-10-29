package com.rcdiamond.aifactory.addon.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.integrated_dynamics.dsl.IdDiagnosticCode
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import org.cyclops.integrateddynamics.RegistryEntries

/**
 * Preflights and consumes real blank Integrated Dynamics variable cards in the player's **main**
 * inventory (hotbar and the 27 backpack slots; armour and offhand are never touched).
 *
 * A card is blank only when it is exactly `integrateddynamics:variable` with either no tag or an
 * empty tag. A card carrying `_type`/`_id` or any other tag is a programmed/stale card and is never
 * overwritten. Creation needs one item per new definition; IDs are not allocated until [plan] has
 * proved both the blank-card count and the final slot capacity.
 *
 * The planner is deliberately conservative about slot mathematics: a full stack can yield one card
 * in its own slot plus one per free slot, while a partially consumed stack must move its remainder
 * and therefore needs a free slot for every card it yields. It never claims capacity it cannot use.
 */
internal object IdBlankCards {
    const val MAIN_SLOTS = 36

    data class Plan(
        /** How many blank items to take from each main-inventory slot. */
        val extractions: Map<Int, Int>,
        /** One main-inventory slot for every created card, in a stable order. */
        val outputSlots: List<Int>,
    ) {
        val cardCount: Int get() = outputSlots.size
        val blankCount: Int get() = extractions.values.sum()
    }

    fun isBlank(stack: ItemStack): Boolean {
        if (stack.isEmpty || stack.item !== RegistryEntries.ITEM_VARIABLE) return false
        val tag = stack.tag ?: return true
        return tag.isEmpty
    }

    /** Pure-ish preflight: reads the inventory but changes nothing when it fails. */
    fun plan(inventory: Inventory, needed: Int): Plan {
        if (needed == 0) return Plan(emptyMap(), emptyList())
        val counts = IntArray(MAIN_SLOTS)
        for (slot in counts.indices) {
            val stack = inventory.getItem(slot)
            counts[slot] = when {
                stack.isEmpty -> 0
                isBlank(stack) -> stack.count
                else -> -1
            }
        }
        return plan(counts, needed)
    }

    /** Slot snapshot: zero is empty, positive is a blank stack, negative is an occupied nonblank slot. */
    fun plan(counts: IntArray, needed: Int): Plan {
        require(counts.size == MAIN_SLOTS && needed >= 0) { "Invalid main-inventory allocation request" }
        if (needed == 0) return Plan(emptyMap(), emptyList())
        if (needed > MAIN_SLOTS) {
            throw IdBackendError.preflight(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "the main inventory cannot hold $needed distinct cards in $MAIN_SLOTS slots",
                hint = "Split the program into smaller card batches; no cards were consumed.",
            )
        }

        val singles = ArrayList<Int>()
        val multi = ArrayList<Int>()
        val emptySlots = ArrayList<Int>()
        var blanks = 0L

        for (slot in 0 until MAIN_SLOTS) {
            val count = counts[slot]
            if (count == 0) {
                emptySlots.add(slot)
            } else if (count > 0) {
                blanks += count
                if (count == 1) {
                    singles.add(slot)
                } else {
                    // Drain smaller stacks first; partial extraction must not strand a usable slot.
                    var index = multi.size
                    while (index > 0 && counts[multi[index - 1]] > count) index--
                    multi.add(index, slot)
                }
            }
        }

        if (blanks < needed) {
            throw IdBackendError.preflight(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "creating $needed card(s) needs $needed blank Integrated Dynamics variable card(s), " +
                    "but the main inventory holds $blanks",
                hint = "Put truly blank integrateddynamics:variable cards (no NBT) in your inventory; " +
                    "programmed cards are never overwritten.",
            )
        }

        var remaining = needed
        val outputSlots = ArrayList<Int>(needed)
        val extractions = LinkedHashMap<Int, Int>()
        var emptyCursor = 0

        fun takeEmpty(): Int {
            val slot = emptySlots[emptyCursor]
            emptyCursor++
            return slot
        }

        // A single blank card can be replaced in place without consuming another slot.
        for (slot in singles) {
            if (remaining == 0) break
            outputSlots.add(slot)
            extractions[slot] = 1
            remaining--
        }

        // A full stack can fill its own slot plus one card per free slot. A partial stack leaves a
        // remainder, which must itself occupy a slot, so each produced card needs its own free slot.
        for (slot in multi) {
            if (remaining == 0) break
            val count = counts[slot]
            val free = emptySlots.size - emptyCursor
            if (remaining >= count && free >= count - 1) {
                outputSlots.add(slot)
                repeat(count - 1) { outputSlots.add(takeEmpty()) }
                extractions[slot] = count
                remaining -= count
            } else {
                val amount = minOf(remaining, free)
                if (amount > 0 && amount < count) {
                    repeat(amount) { outputSlots.add(takeEmpty()) }
                    extractions[slot] = amount
                    remaining -= amount
                }
            }
        }

        if (remaining > 0) {
            throw IdBackendError.preflight(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "the main inventory cannot hold $needed created card(s) without overwriting items; " +
                    "need $remaining more free slot(s)",
                hint = "Clear main-inventory slots or merge blank card stacks, then retry. " +
                    "Cards are never placed in armour or offhand slots.",
            )
        }
        return Plan(extractions, outputSlots)
    }

    /**
     * Removes the planned blank items and returns count-1 copies in creation order. The caller must
     * already have succeeded [plan]; any mismatch here is a truthful race rejection.
     */
    fun extract(inventory: Inventory, plan: Plan): List<ItemStack> {
        val blanks = ArrayList<ItemStack>(plan.blankCount)
        for ((slot, amount) in plan.extractions) {
            var stack = inventory.getItem(slot)
            if (!isBlank(stack) || stack.count < amount) {
                throw IdBackendError.create(
                    IdDiagnosticCode.E_TARGET_CONTRACT,
                    "the blank-card inventory changed while preparing this request; no IDs were allocated",
                    hint = "Retry after the inventory settles.",
                )
            }
            repeat(amount) {
                blanks.add(stack.copyWithCount(1))
                stack.shrink(1)
            }
            inventory.setItem(slot, if (stack.isEmpty) ItemStack.EMPTY else stack)
        }
        inventory.setChanged()
        return blanks
    }

    /** Places one created card into its reserved main-inventory slot. */
    fun place(inventory: Inventory, slot: Int, card: ItemStack) {
        if (slot !in 0 until MAIN_SLOTS || !inventory.getItem(slot).isEmpty) {
            throw IdBackendError.create(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "the reserved main-inventory slot $slot is no longer empty; the created card was not placed",
                hint = "The card's ID was allocated; move items out of the slot and inspect the inventory.",
            )
        }
        inventory.setItem(slot, card)
        inventory.setChanged()
    }

    /** Best-effort return of unused blank items after a post-preflight failure. */
    fun restore(inventory: Inventory, blanks: List<ItemStack>) {
        for (blank in blanks) {
            if (!inventory.add(blank)) break
        }
        inventory.setChanged()
    }
}
