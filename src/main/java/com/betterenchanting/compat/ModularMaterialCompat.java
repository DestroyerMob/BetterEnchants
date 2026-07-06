package com.betterenchanting.compat;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public final class ModularMaterialCompat {
    private ModularMaterialCompat() {
    }

    public static List<ResourceLocation> materialItemTags(ItemStack stack) {
        Set<ResourceLocation> tags = new LinkedHashSet<>();
        tags.addAll(SilentGearCompat.materialItemTags(stack));
        tags.addAll(MobsToolForgingCompat.materialItemTags(stack));
        return List.copyOf(tags);
    }

    public static Map<ResourceLocation, Integer> materialItemTagCounts(ItemStack stack) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        mergeCounts(counts, SilentGearCompat.materialItemTagCounts(stack));
        mergeCounts(counts, MobsToolForgingCompat.materialItemTagCounts(stack));
        counts.entrySet().removeIf(entry -> entry.getValue() <= 0);
        return Map.copyOf(counts);
    }

    public static boolean hasMaterialItemTag(ItemStack stack, TagKey<Item> tag) {
        return hasMaterialItemTag(stack, tag.location());
    }

    public static boolean hasMaterialItemTag(ItemStack stack, ResourceLocation tag) {
        return materialItemTags(stack).contains(tag);
    }

    public static List<ResourceLocation> materialTargetTags(ItemStack stack) {
        Set<ResourceLocation> tags = new LinkedHashSet<>();
        tags.addAll(SilentGearCompat.materialTargetTags(stack));
        tags.addAll(MobsToolForgingCompat.materialTargetTags(stack));
        return List.copyOf(tags);
    }

    public static boolean blocksFinishedToolEnchanting(ItemStack stack) {
        return MobsToolForgingCompat.blocksFinishedToolEnchanting(stack);
    }

    public static boolean blocksDirectPartEnchanting(ItemStack stack) {
        return MobsToolForgingCompat.blocksDirectPartEnchanting(stack);
    }

    public static boolean hasRoutedParts(ItemStack stack) {
        return MobsToolForgingCompat.hasRoutedParts(stack);
    }

    public static OptionalInt routedMaxEnchantments(ItemStack stack) {
        return MobsToolForgingCompat.routedMaxEnchantments(stack);
    }

    public static List<ResourceLocation> routedTargetTags(ItemStack stack) {
        return MobsToolForgingCompat.routedTargetTags(stack);
    }

    public static Set<Holder<Enchantment>> storedRoutedEnchantments(ItemStack stack) {
        return MobsToolForgingCompat.storedRoutedEnchantments(stack);
    }

    public static boolean canApplyRoutedEnchantments(RegistryAccess registryAccess, ItemStack target, Iterable<Holder<Enchantment>> additions) {
        return MobsToolForgingCompat.canApplyRoutedEnchantments(registryAccess, target, additions);
    }

    public static java.util.Optional<ItemStack> applyRoutedEnchantments(RegistryAccess registryAccess, ItemStack target, List<EnchantmentInstance> additions) {
        return MobsToolForgingCompat.applyRoutedEnchantments(registryAccess, target, additions);
    }

    public static java.util.Optional<ItemStack> overlevelRoutedEnchantment(RegistryAccess registryAccess, ItemStack target, Holder<Enchantment> enchantment) {
        return MobsToolForgingCompat.overlevelRoutedEnchantment(registryAccess, target, enchantment);
    }

    public static boolean reconcileRoutedEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        return MobsToolForgingCompat.reconcileRoutedEnchantments(registryAccess, stack);
    }

    public static boolean removeNonCurseRoutedEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        return MobsToolForgingCompat.removeNonCurseRoutedEnchantments(registryAccess, stack);
    }

    private static void mergeCounts(Map<ResourceLocation, Integer> counts, Map<ResourceLocation, Integer> additions) {
        for (Map.Entry<ResourceLocation, Integer> entry : additions.entrySet()) {
            counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }
}
