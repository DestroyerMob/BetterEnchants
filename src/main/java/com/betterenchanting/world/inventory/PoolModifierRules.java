package com.betterenchanting.world.inventory;

import com.betterenchanting.data.EssenceDefinition;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.registry.ModTags;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

final class PoolModifierRules {
    private PoolModifierRules() {
    }

    static boolean isPoolModifier(ItemStack stack) {
        return isEssenceModifier(stack) || isEnchantedBook(stack);
    }

    static boolean isEssenceModifier(ItemStack stack) {
        return stack.is(ModTags.Items.ESSENCES) || EssenceDefinitions.isEssence(stack);
    }

    static boolean blocksOffer(ItemStack stack) {
        return EssenceDefinitions.get(stack)
                .map(EssenceDefinition::blocksOffer)
                .orElse(false);
    }

    private static boolean appliesToAllOffers(ItemStack stack) {
        return EssenceDefinitions.get(stack)
                .map(EssenceDefinition::appliesToAllOffers)
                .orElse(false);
    }

    static boolean isEnchantedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.hasAnyEnchantments(stack);
    }

    static ModifierPlan plan(Container container, int firstSlot, int slotCount, int enchantmentSeed) {
        List<ModifierInput> modifiers = normalizedModifiers(container, firstSlot, slotCount);
        Map<Integer, ModifierInput> bySlot = new LinkedHashMap<>();
        List<ModifierPlanner.Input> plannerInputs = new ArrayList<>();
        for (ModifierInput modifier : modifiers) {
            bySlot.put(modifier.slot(), modifier);
            plannerInputs.add(new ModifierPlanner.Input(
                    modifier.slot(),
                    modifier.sortKey(),
                    appliesToAllOffers(modifier.stack()),
                    blocksOffer(modifier.stack())
            ));
        }

        ModifierPlanner.Plan plan = ModifierPlanner.plan(plannerInputs, slotCount, enchantmentSeed);
        ModifierInput[] directModifiers = new ModifierInput[slotCount];
        for (int option = 0; option < slotCount; option++) {
            directModifiers[option] = plan.directModifiers().get(option)
                    .map(input -> bySlot.get(input.slot()))
                    .orElse(null);
        }
        List<ModifierInput> globalModifiers = plan.globalModifiers().stream()
                .map(input -> bySlot.get(input.slot()))
                .toList();

        return new ModifierPlan(globalModifiers, directModifiers, plan.blockedOffers());
    }

    static boolean blocksOffer(ModifierPlan plan, int option) {
        return plan.blocksOffer(option);
    }

    static ItemStack modifierStack(ModifierPlan plan, int option) {
        return plan.directModifier(option)
                .map(ModifierInput::stack)
                .orElse(ItemStack.EMPTY);
    }

    static List<ItemStack> optionEssences(ModifierPlan plan, int option) {
        List<ItemStack> stacks = new ArrayList<>();
        ItemStack modifier = modifierStack(plan, option);
        if (isEssenceModifier(modifier)) {
            stacks.add(modifier);
        }
        for (ModifierInput globalModifier : plan.globalModifiers()) {
            if (isEssenceModifier(globalModifier.stack())) {
                stacks.add(globalModifier.stack());
            }
        }
        return List.copyOf(stacks);
    }

    static List<ItemStack> optionBooks(ModifierPlan plan, int option) {
        ItemStack modifier = modifierStack(plan, option);
        return isEnchantedBook(modifier) ? List.of(modifier) : List.of();
    }

    static List<ItemStack> globalModifierStacks(ModifierPlan plan) {
        return plan.globalModifiers().stream()
                .map(ModifierInput::stack)
                .toList();
    }

    static void consumeForOption(Container container, ModifierPlan plan, int option, Player player) {
        List<Integer> slotsToConsume = new ArrayList<>();
        plan.directModifier(option).ifPresent(modifier -> slotsToConsume.add(modifier.slot()));
        for (ModifierInput globalModifier : plan.globalModifiers()) {
            slotsToConsume.add(globalModifier.slot());
        }
        for (int slot : slotsToConsume.stream().distinct().toList()) {
            consumeInputSlot(container, slot, player);
        }
    }

    private static List<ModifierInput> normalizedModifiers(Container container, int firstSlot, int slotCount) {
        List<ModifierInput> modifiers = new ArrayList<>();
        for (int slot = 0; slot < slotCount; slot++) {
            int absoluteSlot = firstSlot + slot;
            ItemStack stack = container.getItem(absoluteSlot);
            if (isPoolModifier(stack)) {
                modifiers.add(new ModifierInput(absoluteSlot, stack, modifierSortKey(stack)));
            }
        }
        modifiers.sort(Comparator.comparing(ModifierInput::sortKey).thenComparingInt(ModifierInput::slot));
        return List.copyOf(modifiers);
    }

    private static String modifierSortKey(ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!isEnchantedBook(stack)) {
            return itemId;
        }

        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        List<String> entries = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            String enchantmentId = entry.getKey()
                    .unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse(entry.getKey().toString());
            entries.add(enchantmentId + ":" + entry.getIntValue());
        }
        entries.sort(String::compareTo);
        return itemId + "|" + String.join(",", entries);
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

    static final class ModifierPlan {
        private final List<ModifierInput> globalModifiers;
        private final ModifierInput[] directModifiers;
        private final boolean[] blockedOffers;

        private ModifierPlan(List<ModifierInput> globalModifiers, ModifierInput[] directModifiers, boolean[] blockedOffers) {
            this.globalModifiers = globalModifiers;
            this.directModifiers = directModifiers.clone();
            this.blockedOffers = blockedOffers.clone();
        }

        private boolean blocksOffer(int option) {
            return option >= 0 && option < this.blockedOffers.length && this.blockedOffers[option];
        }

        private java.util.Optional<ModifierInput> directModifier(int option) {
            if (option < 0 || option >= this.directModifiers.length) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.ofNullable(this.directModifiers[option]);
        }

        private List<ModifierInput> globalModifiers() {
            return this.globalModifiers;
        }
    }

    private record ModifierInput(int slot, ItemStack stack, String sortKey) {
        private ModifierInput {
            Objects.requireNonNull(stack);
            Objects.requireNonNull(sortKey);
        }
    }
}
