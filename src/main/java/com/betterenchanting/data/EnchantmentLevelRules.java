package com.betterenchanting.data;

import com.betterenchanting.config.EffectiveBalance;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class EnchantmentLevelRules {
    private static final Map<ResourceKey<Enchantment>, Integer> VANILLA_MAX_LEVELS = Map.ofEntries(
            vanilla(Enchantments.BLAST_PROTECTION, 4),
            vanilla(Enchantments.BREACH, 4),
            vanilla(Enchantments.DEPTH_STRIDER, 3),
            vanilla(Enchantments.FEATHER_FALLING, 4),
            vanilla(Enchantments.FIRE_ASPECT, 2),
            vanilla(Enchantments.FIRE_PROTECTION, 4),
            vanilla(Enchantments.FORTUNE, 3),
            vanilla(Enchantments.FROST_WALKER, 2),
            vanilla(Enchantments.KNOCKBACK, 2),
            vanilla(Enchantments.LOOTING, 3),
            vanilla(Enchantments.LOYALTY, 3),
            vanilla(Enchantments.LUCK_OF_THE_SEA, 3),
            vanilla(Enchantments.LURE, 3),
            vanilla(Enchantments.MENDING, 1),
            vanilla(Enchantments.PIERCING, 4),
            vanilla(Enchantments.PROJECTILE_PROTECTION, 4),
            vanilla(Enchantments.PROTECTION, 4),
            vanilla(Enchantments.PUNCH, 2),
            vanilla(Enchantments.RESPIRATION, 3),
            vanilla(Enchantments.SOUL_SPEED, 3),
            vanilla(Enchantments.SWEEPING_EDGE, 3),
            vanilla(Enchantments.SWIFT_SNEAK, 3),
            vanilla(Enchantments.THORNS, 3),
            vanilla(Enchantments.UNBREAKING, 3)
    );

    private EnchantmentLevelRules() {
    }

    public static int maxLevel(Holder<Enchantment> enchantment) {
        int loadedMaxLevel = enchantment.value().getMaxLevel();
        if (EffectiveBalance.overridesVanillaEnchantmentLimits()) {
            return loadedMaxLevel;
        }

        return enchantment.unwrapKey()
                .map(key -> Math.min(loadedMaxLevel, VANILLA_MAX_LEVELS.getOrDefault(key, loadedMaxLevel)))
                .orElse(loadedMaxLevel);
    }

    public static int clampLevel(Holder<Enchantment> enchantment, int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, maxLevel(enchantment));
    }

    public static void clampEnchantments(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (current.isEmpty()) {
            return;
        }

        EnchantmentHelper.updateEnchantments(stack, mutable -> {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : current.entrySet()) {
                int clampedLevel = clampLevel(entry.getKey(), entry.getIntValue());
                if (clampedLevel != entry.getIntValue()) {
                    mutable.set(entry.getKey(), clampedLevel);
                }
            }
        });
    }

    private static Map.Entry<ResourceKey<Enchantment>, Integer> vanilla(ResourceKey<Enchantment> enchantment, int maxLevel) {
        return Map.entry(enchantment, maxLevel);
    }
}
