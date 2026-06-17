package com.betterenchanting.world.inventory;

import com.betterenchanting.data.EssenceDefinition;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

final class PoolModifierRules {
    private PoolModifierRules() {
    }

    static boolean isPoolModifier(ItemStack stack) {
        return isEssenceModifier(stack) || isEnchantedBook(stack);
    }

    static boolean isEssenceModifier(ItemStack stack) {
        return stack.is(ModTags.Items.ESSENCES) || EssenceDefinitions.isEssence(stack);
    }

    static boolean isPurificationModifier(ItemStack stack) {
        return EssenceDefinitions.get(stack)
                .map(EssenceDefinition::removesCurses)
                .orElse(false);
    }

    static boolean isEnchantedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.hasAnyEnchantments(stack);
    }

    static ItemStack modifierStack(Container container, int firstSlot, int slotCount, int option) {
        if (option < 0 || option >= slotCount) {
            return ItemStack.EMPTY;
        }
        return container.getItem(firstSlot + option);
    }

    static List<ItemStack> optionEssences(Container container, int firstSlot, int slotCount, int option) {
        List<ItemStack> stacks = new ArrayList<>();
        ItemStack modifier = modifierStack(container, firstSlot, slotCount, option);
        if (isEssenceModifier(modifier) && !isPurificationModifier(modifier)) {
            stacks.add(modifier);
        }
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack purification = modifierStack(container, firstSlot, slotCount, slot);
            if (isPurificationModifier(purification)) {
                stacks.add(purification);
            }
        }
        return List.copyOf(stacks);
    }

    static List<ItemStack> optionBooks(Container container, int firstSlot, int slotCount, int option) {
        ItemStack modifier = modifierStack(container, firstSlot, slotCount, option);
        return isEnchantedBook(modifier) ? List.of(modifier) : List.of();
    }

    static void consumeForOption(Container container, int firstSlot, int slotCount, int option, Player player) {
        consumeInputSlot(container, firstSlot + option, player);
        for (int slot = 0; slot < slotCount; slot++) {
            int absoluteSlot = firstSlot + slot;
            if (slot != option && isPurificationModifier(container.getItem(absoluteSlot))) {
                consumeInputSlot(container, absoluteSlot, player);
            }
        }
    }

    private static void consumeInputSlot(Container container, int slot, Player player) {
        ItemStack stack = container.getItem(slot);
        if (!stack.isEmpty()) {
            stack.consume(1, player);
            if (stack.isEmpty()) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
    }
}
