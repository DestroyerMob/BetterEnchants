package com.betterenchanting.world;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.data.EnchantmentLimitRules;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;

public final class EnchantmentActivationEvents {
    private static final String TARGET_TAG_PREFIX = "targets/";

    private EnchantmentActivationEvents() {
    }

    public static void suppressInactiveEnchantments(GetEnchantmentLevelEvent event) {
        ItemStack stack = event.getStack();
        if (stack.isEmpty() || isBookStack(stack)) {
            return;
        }

        List<Holder<Enchantment>> enchantments = List.copyOf(event.getEnchantments().keySet());
        for (Holder<Enchantment> enchantment : enchantments) {
            if (!status(stack, enchantment, event.getLookup()).active()) {
                event.getEnchantments().set(enchantment, 0);
            }
        }
    }

    public static List<TooltipEntry> tooltipEntries(ItemStack stack, HolderLookup.Provider registries) {
        if (registries == null) {
            return List.of();
        }

        HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        List<RawEntry> rawEntries = new ArrayList<>();
        addOrderedEntries(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY), enchantments, rawEntries);
        addOrderedEntries(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), enchantments, rawEntries);
        return rawEntries.stream()
                .map(entry -> new TooltipEntry(entry.enchantment(), entry.level(), status(stack, entry.enchantment(), enchantments)))
                .toList();
    }

    public static Status status(ItemStack stack, Holder<Enchantment> enchantment, HolderLookup.RegistryLookup<Enchantment> enchantments) {
        if (stack.isEmpty() || isBookStack(stack)) {
            return Status.ACTIVE;
        }

        EnumSet<InactiveReason> reasons = EnumSet.noneOf(InactiveReason.class);
        if (!matchesCurrentItem(stack, enchantment)) {
            reasons.add(InactiveReason.WRONG_TAG);
        } else if (isOverLimit(stack, enchantment, enchantments)) {
            reasons.add(InactiveReason.OVER_LIMIT);
        }
        return Status.of(reasons);
    }

    private static boolean isOverLimit(ItemStack stack, Holder<Enchantment> target, HolderLookup.RegistryLookup<Enchantment> enchantments) {
        int maxEnchantments = EnchantmentLimitRules.maxEnchantments(stack);
        if (maxEnchantments <= 0) {
            return orderedUniqueEnchantments(stack, enchantments).contains(target);
        }

        int activeIndex = 0;
        for (Holder<Enchantment> enchantment : orderedUniqueEnchantments(stack, enchantments)) {
            if (!matchesCurrentItem(stack, enchantment)) {
                continue;
            }
            if (enchantment.equals(target)) {
                return activeIndex >= maxEnchantments;
            }
            activeIndex++;
        }
        return false;
    }

    private static List<Holder<Enchantment>> orderedUniqueEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> enchantments) {
        Map<Holder<Enchantment>, Integer> ordered = new LinkedHashMap<>();
        addOrderedEntries(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY), enchantments, ordered);
        addOrderedEntries(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), enchantments, ordered);
        return List.copyOf(ordered.keySet());
    }

    private static void addOrderedEntries(
            ItemEnchantments source,
            HolderLookup.RegistryLookup<Enchantment> enchantments,
            Map<Holder<Enchantment>, Integer> output
    ) {
        List<RawEntry> ordered = new ArrayList<>();
        addOrderedEntries(source, enchantments, ordered);
        for (RawEntry entry : ordered) {
            output.putIfAbsent(entry.enchantment(), entry.level());
        }
    }

    private static void addOrderedEntries(
            ItemEnchantments source,
            HolderLookup.RegistryLookup<Enchantment> enchantments,
            List<RawEntry> output
    ) {
        if (source.isEmpty()) {
            return;
        }

        Set<Holder<Enchantment>> added = new HashSet<>();
        enchantments.get(EnchantmentTags.TOOLTIP_ORDER).ifPresent(ordered -> addTooltipOrderedEntries(source, ordered, added, output));
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : source.entrySet()) {
            if (entry.getIntValue() > 0 && added.add(entry.getKey())) {
                output.add(new RawEntry(entry.getKey(), entry.getIntValue()));
            }
        }
    }

    private static void addTooltipOrderedEntries(
            ItemEnchantments source,
            HolderSet.Named<Enchantment> ordered,
            Set<Holder<Enchantment>> added,
            List<RawEntry> output
    ) {
        for (Holder<Enchantment> enchantment : ordered) {
            int level = source.getLevel(enchantment);
            if (level > 0 && added.add(enchantment)) {
                output.add(new RawEntry(enchantment, level));
            }
        }
    }

    private static boolean matchesCurrentItem(ItemStack stack, Holder<Enchantment> enchantment) {
        Set<ResourceLocation> targetTags = betterEnchantingTargetTags(enchantment);
        if (!targetTags.isEmpty()) {
            Set<ResourceLocation> currentTargetTags = new HashSet<>(EnchantmentTargetTags.resolve(stack));
            return targetTags.stream().anyMatch(currentTargetTags::contains);
        }
        return stack.supportsEnchantment(enchantment);
    }

    private static Set<ResourceLocation> betterEnchantingTargetTags(Holder<Enchantment> enchantment) {
        return enchantment.tags()
                .map(TagKey::location)
                .filter(EnchantmentActivationEvents::isBetterEnchantingTargetTag)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static boolean isBetterEnchantingTargetTag(ResourceLocation tagId) {
        return tagId.getNamespace().equals(BetterEnchanting.MOD_ID)
                && tagId.getPath().startsWith(TARGET_TAG_PREFIX);
    }

    private static boolean isBookStack(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    private record RawEntry(Holder<Enchantment> enchantment, int level) {
    }

    public record TooltipEntry(Holder<Enchantment> enchantment, int level, Status status) {
    }

    public record Status(Set<InactiveReason> reasons) {
        private static final Status ACTIVE = new Status(Set.of());

        private static Status of(EnumSet<InactiveReason> reasons) {
            return reasons.isEmpty() ? ACTIVE : new Status(Set.copyOf(reasons));
        }

        public boolean active() {
            return this.reasons.isEmpty();
        }

        public boolean has(InactiveReason reason) {
            return this.reasons.contains(reason);
        }
    }

    public enum InactiveReason {
        WRONG_TAG("Wrong tag"),
        OVER_LIMIT("Over limit");

        private final String label;

        InactiveReason(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }
}
