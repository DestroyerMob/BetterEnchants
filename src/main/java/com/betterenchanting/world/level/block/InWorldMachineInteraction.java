package com.betterenchanting.world.level.block;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class InWorldMachineInteraction {
    private InWorldMachineInteraction() {
    }

    public static boolean insert(Container container, int slot, Player player, ItemStack held, int slotLimit) {
        ItemStack stored = container.getItem(slot);
        int maximum = Math.min(slotLimit, Math.min(container.getMaxStackSize(), held.getMaxStackSize()));
        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, held)) {
            return false;
        }
        int room = maximum - stored.getCount();
        int moved = Math.min(room, held.getCount());
        if (moved <= 0) {
            return false;
        }

        if (stored.isEmpty()) {
            container.setItem(slot, held.copyWithCount(moved));
        } else {
            stored.grow(moved);
            container.setChanged();
        }
        if (!player.hasInfiniteMaterials()) {
            held.shrink(moved);
        }
        return true;
    }

    public static ItemStack take(Container container, int slot, Player player) {
        ItemStack stored = container.getItem(slot);
        if (stored.isEmpty() || !canReceive(player, stored)) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = stored.copy();
        ItemStack toInsert = taken.copy();
        if (!player.getInventory().add(toInsert) || !toInsert.isEmpty()) {
            return ItemStack.EMPTY;
        }
        container.setItem(slot, ItemStack.EMPTY);
        return taken;
    }

    /** Returns true only when the complete stack can merge into inventory or occupy a free main slot. */
    public static boolean canReceive(Player player, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return false;
        }
        Inventory inventory = player.getInventory();
        int remaining = incoming.getCount();
        if (incoming.isStackable()) {
            for (ItemStack existing : inventory.items) {
                remaining -= availableStackSpace(inventory, existing, incoming);
                if (remaining <= 0) {
                    return true;
                }
            }
            for (ItemStack existing : inventory.offhand) {
                remaining -= availableStackSpace(inventory, existing, incoming);
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return inventory.getFreeSlot() >= 0 && remaining <= incoming.getMaxStackSize();
    }

    private static int availableStackSpace(Inventory inventory, ItemStack existing, ItemStack incoming) {
        if (existing.isEmpty()
                || !existing.isStackable()
                || !ItemStack.isSameItemSameComponents(existing, incoming)) {
            return 0;
        }
        return Math.max(0, inventory.getMaxStackSize(existing) - existing.getCount());
    }

    public static int takeAll(Container container, Player player, int... slots) {
        int stacks = 0;
        for (int slot : slots) {
            if (!take(container, slot, player).isEmpty()) {
                stacks++;
            }
        }
        return stacks;
    }
}
