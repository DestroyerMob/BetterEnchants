package com.betterenchanting.compat;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

    private static void mergeCounts(Map<ResourceLocation, Integer> counts, Map<ResourceLocation, Integer> additions) {
        for (Map.Entry<ResourceLocation, Integer> entry : additions.entrySet()) {
            counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }
}
