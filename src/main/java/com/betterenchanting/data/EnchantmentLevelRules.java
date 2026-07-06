package com.betterenchanting.data;

import com.betterenchanting.compat.ApothicEnchantingCompat;
import com.betterenchanting.registry.ModDataComponents;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class EnchantmentLevelRules {
    private EnchantmentLevelRules() {
    }

    public static int maxLevel(Holder<Enchantment> enchantment) {
        int baseMax = enchantment.value().getMaxLevel();
        if (baseMax <= 1) {
            return baseMax;
        }

        OptionalInt apothicMax = ApothicEnchantingCompat.maxLevel(enchantment);
        return apothicMax.isPresent() ? Math.max(baseMax, apothicMax.getAsInt()) : baseMax;
    }

    public static int clampLevel(Holder<Enchantment> enchantment, int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, maxLevel(enchantment));
    }

    public static int overlevelMaxLevel(Holder<Enchantment> enchantment) {
        return maxLevel(enchantment) + 1;
    }

    public static int effectiveLevel(Holder<Enchantment> enchantment, int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, overlevelMaxLevel(enchantment));
    }

    public static boolean isOverleveled(Holder<Enchantment> enchantment, int level) {
        return level > maxLevel(enchantment);
    }

    public static boolean isOverlevelPrimed(Holder<Enchantment> enchantment, int level) {
        int maxLevel = maxLevel(enchantment);
        return maxLevel > 1 && level == maxLevel;
    }

    public static boolean hasNonCurseEnchantments(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return hasNonCurseEnchantments(EnchantmentHelper.getEnchantmentsForCrafting(stack))
                || hasNonCurseEnchantments(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
    }

    public static boolean removeNonCurseEnchantments(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        boolean hadOverleveledMarker = stack.has(ModDataComponents.OVERLEVELED.get());
        boolean changed = removeNonCurseItemEnchantments(stack);
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!stored.isEmpty()) {
            ItemEnchantments kept = cursesOnly(stored);
            if (!stored.equals(kept)) {
                if (kept.isEmpty()) {
                    stack.remove(DataComponents.STORED_ENCHANTMENTS);
                } else {
                    stack.set(DataComponents.STORED_ENCHANTMENTS, kept);
                }
                changed = true;
            }
        }

        syncOverleveledMarker(stack);
        return changed || (hadOverleveledMarker && !stack.has(ModDataComponents.OVERLEVELED.get()));
    }

    public static boolean hasOverleveledEnchantment(ItemStack stack) {
        return overleveledEnchantmentCount(stack) > 0;
    }

    public static int overleveledEnchantmentCount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        int count = 0;
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : current.entrySet()) {
            if (isOverleveled(entry.getKey(), entry.getIntValue())) {
                count++;
            }
        }
        return count;
    }

    public static Optional<OverlevelTarget> overlevelTarget(ItemStack stack, ItemStack essence) {
        if (stack.isEmpty() || hasOverleveledEnchantment(stack)) {
            return Optional.empty();
        }

        Optional<EssenceDefinition> definition = EssenceDefinitions.get(essence);
        if (definition.isEmpty()) {
            return Optional.empty();
        }

        List<OverlevelTarget> matches = new ArrayList<>();
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : current.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            int maxLevel = maxLevel(enchantment);
            if (level == maxLevel && matchesEssence(enchantment, definition.get())) {
                matches.add(new OverlevelTarget(enchantment, level, overlevelMaxLevel(enchantment)));
            }
        }

        matches.sort(Comparator.comparing(target -> enchantmentSortKey(target.enchantment())));
        return matches.stream().findFirst();
    }

    public static void clampEnchantments(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (current.isEmpty()) {
            syncOverleveledMarker(stack);
            return;
        }

        EnchantmentHelper.updateEnchantments(stack, mutable -> {
            boolean keptOverlevel = false;
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : current.entrySet()) {
                int clampedLevel = clampStackLevel(entry.getKey(), entry.getIntValue(), !keptOverlevel);
                if (isOverleveled(entry.getKey(), clampedLevel)) {
                    keptOverlevel = true;
                }
                if (clampedLevel != entry.getIntValue()) {
                    mutable.set(entry.getKey(), clampedLevel);
                }
            }
        });
        syncOverleveledMarker(stack);
    }

    public static void overlevel(ItemStack stack, Holder<Enchantment> enchantment) {
        EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(enchantment, overlevelMaxLevel(enchantment)));
        stack.set(ModDataComponents.OVERLEVELED.get(), true);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    private static boolean removeNonCurseItemEnchantments(ItemStack stack) {
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (current.isEmpty()) {
            return false;
        }

        ItemEnchantments kept = cursesOnly(current);
        if (current.equals(kept)) {
            return false;
        }

        EnchantmentHelper.setEnchantments(stack, kept);
        return true;
    }

    private static boolean hasNonCurseEnchantments(ItemEnchantments enchantments) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getIntValue() > 0 && !entry.getKey().is(EnchantmentTags.CURSE)) {
                return true;
            }
        }
        return false;
    }

    private static ItemEnchantments cursesOnly(ItemEnchantments enchantments) {
        if (enchantments.isEmpty()) {
            return ItemEnchantments.EMPTY;
        }

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getIntValue() > 0 && entry.getKey().is(EnchantmentTags.CURSE)) {
                mutable.set(entry.getKey(), entry.getIntValue());
            }
        }
        return mutable.toImmutable();
    }

    private static int clampStackLevel(Holder<Enchantment> enchantment, int level, boolean allowOverlevel) {
        if (level <= 0) {
            return 0;
        }
        int maxLevel = maxLevel(enchantment);
        if (allowOverlevel && level > maxLevel) {
            return Math.min(level, overlevelMaxLevel(enchantment));
        }
        return Math.min(level, maxLevel);
    }

    private static boolean matchesEssence(Holder<Enchantment> enchantment, EssenceDefinition definition) {
        for (ResourceLocation tag : definition.tags()) {
            if (enchantment.is(TagKey.create(Registries.ENCHANTMENT, tag))) {
                return true;
            }
        }
        return false;
    }

    private static String enchantmentSortKey(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey()
                .map(ResourceKey::location)
                .map(ResourceLocation::toString)
                .orElse(enchantment.toString());
    }

    private static void syncOverleveledMarker(ItemStack stack) {
        boolean overleveled = overleveledEnchantmentCount(stack) > 0;
        if (overleveled) {
            stack.set(ModDataComponents.OVERLEVELED.get(), true);
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            boolean hadOverleveledMarker = stack.has(ModDataComponents.OVERLEVELED.get());
            stack.remove(ModDataComponents.OVERLEVELED.get());
            if (hadOverleveledMarker) {
                stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            }
        }
    }

    public record OverlevelTarget(Holder<Enchantment> enchantment, int currentLevel, int overleveledLevel) {
    }
}
