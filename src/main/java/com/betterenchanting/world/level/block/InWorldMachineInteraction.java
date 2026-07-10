package com.betterenchanting.world.level.block;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
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
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = stored.copy();
        container.setItem(slot, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(taken.copy());
        return taken;
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
