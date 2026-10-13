package com.rcdiamond.aifactory.server

import com.rcdiamond.aifactory.client.llm.GiveItemRequest
import com.rcdiamond.aifactory.client.llm.GiveItemResult
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraftforge.registries.ForgeRegistries

/**
 * Server-authoritative implementation of the give-item game action.
 *
 * The request carries no target: the receiving connection's own player is always the recipient, so a client
 * can not name someone else. Delivery requires the same authority as the vanilla `/give` command, validates
 * the item id against the item registry and rejects air, NBT and arbitrary commands, then mirrors vanilla
 * `/give` delivery: fill the inventory, drop what does not fit at the player's feet and reserve it for them.
 *
 * Must be called on the server thread with a [ServerPlayer] that belongs to a running server.
 */
internal object GiveItemMechanism {
    /** Permission level of the vanilla `/give` command. */
    private const val REQUIRED_PERMISSION_LEVEL = 2

    fun grant(player: ServerPlayer, request: GiveItemRequest): GiveItemResult {
        val requestId = request.requestId
        if (!player.hasPermissions(REQUIRED_PERMISSION_LEVEL)) {
            return GiveItemResult(requestId, false, "Permission level $REQUIRED_PERMISSION_LEVEL is required to give items")
        }
        if (!GiveItemLimits.isCountInRange(request.count)) {
            return GiveItemResult(
                requestId,
                false,
                "Count must be between ${GiveItemLimits.MIN_COUNT} and ${GiveItemLimits.MAX_COUNT}",
            )
        }
        val itemId = ResourceLocation.tryParse(request.itemId)
            ?: return GiveItemResult(requestId, false, "Invalid item id: ${request.itemId}")
        if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
            return GiveItemResult(requestId, false, "Unknown item: $itemId")
        }
        val item = ForgeRegistries.ITEMS.getValue(itemId)
            ?: return GiveItemResult(requestId, false, "Unknown item: $itemId")
        if (item === Items.AIR) {
            return GiveItemResult(requestId, false, "Air is not a giveable item")
        }

        val delivery = deliver(player, item, request.count)
        if (delivery.total == 0) {
            return GiveItemResult(requestId, false, "Could not deliver ${request.count} x $itemId")
        }
        if (delivery.total != request.count) {
            return GiveItemResult(
                requestId, false,
                "Delivered ${delivery.total} of ${request.count} x $itemId; the remainder could not be delivered",
            )
        }
        return GiveItemResult(requestId, true, deliveredMessage(itemId, request.count, delivery))
    }

    private fun deliver(player: ServerPlayer, item: Item, count: Int): Delivery {
        val inventory = player.inventory
        var inserted = 0
        var dropped = 0
        var remaining = count
        while (remaining > 0) {
            val stack = ItemStack(item, remaining)
            val size = minOf(remaining, stack.maxStackSize)
            stack.count = size
            remaining -= size
            // Inventory.add discards overflow in creative mode. Insert into available slots instead.
            while (!stack.isEmpty) {
                var slot = inventory.getSlotWithRemainingSpace(stack)
                if (slot < 0) slot = inventory.freeSlot
                if (slot < 0) break
                val existing = inventory.getItem(slot)
                val room = minOf(stack.maxStackSize, inventory.maxStackSize) - existing.count
                if (room <= 0) break
                val moved = minOf(room, stack.count)
                if (existing.isEmpty) {
                    inventory.setItem(slot, stack.split(moved))
                } else {
                    existing.grow(moved)
                    stack.shrink(moved)
                }
            }
            val leftover = if (stack.isEmpty) 0 else stack.count
            inserted += size - leftover
            if (leftover > 0 && dropForOwner(player, stack)) {
                dropped += leftover
            }
        }
        if (inserted > 0) {
            inventory.setChanged()
            // Without this the client keeps showing the pre-give inventory until it resyncs by itself.
            player.containerMenu.broadcastChanges()
        }
        return Delivery(inserted, dropped)
    }

    /** Drops [stack] at the player's feet and reserves it for them, exactly like vanilla `/give` overflow. */
    private fun dropForOwner(player: ServerPlayer, stack: ItemStack): Boolean {
        // Player.drop fires the cancellable ItemTossEvent, so a null result means another mod removed the drop.
        val entity = player.drop(stack, false) ?: return false
        entity.setNoPickUpDelay()
        entity.setTarget(player.uuid)
        return true
    }

    private fun deliveredMessage(itemId: ResourceLocation, count: Int, delivery: Delivery): String =
        if (delivery.dropped == 0) {
            "Gave $count x $itemId"
        } else {
            "Gave $count x $itemId (${delivery.inserted} into the inventory, ${delivery.dropped} dropped at the player)"
        }

    private class Delivery(val inserted: Int, val dropped: Int) {
        val total: Int get() = inserted + dropped
    }
}
