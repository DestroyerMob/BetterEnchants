package com.betterenchanting.world.inventory;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.compat.SilentGearCompat;
import com.betterenchanting.registry.ModTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class EnchantingPowerRules {
    private EnchantingPowerRules() {
    }

    static int clampBookshelfPower(int bookshelfPower) {
        return Mth.clamp(bookshelfPower, 0, EffectiveBalance.maxBookshelfPower());
    }

    static int offerRequirementForBookshelfPower(int bookshelfPower) {
        int minCost = Math.min(EffectiveBalance.minEnchantingBaseCost(), EffectiveBalance.maxEnchantingBaseCost());
        int maxCost = Math.max(EffectiveBalance.minEnchantingBaseCost(), EffectiveBalance.maxEnchantingBaseCost());
        int maxBookshelfPower = EffectiveBalance.maxBookshelfPower();
        int clampedPower = clampBookshelfPower(bookshelfPower);
        if (minCost >= maxCost || maxBookshelfPower <= 0) {
            return minCost;
        }
        if (clampedPower >= maxBookshelfPower) {
            return maxCost;
        }

        long cost = (long) minCost + (long) clampedPower * EffectiveBalance.enchantingBaseCostPerBookshelfPower();
        int cappedCost = Mth.clamp((int) Math.min(Integer.MAX_VALUE, cost), minCost, maxCost - 1);
        return cappedCost;
    }

    static int levelCostForBookshelfPower(int bookshelfPower) {
        int minCost = Math.min(EffectiveBalance.minEnchantingLevelCost(), EffectiveBalance.maxEnchantingLevelCost());
        int maxCost = Math.max(EffectiveBalance.minEnchantingLevelCost(), EffectiveBalance.maxEnchantingLevelCost());
        if (minCost >= maxCost) {
            return minCost;
        }

        int clampedPower = clampBookshelfPower(bookshelfPower);
        int bandSize = Math.max(1, EffectiveBalance.bookshelfPowerPerLevelCost());
        int band = clampedPower <= 0 ? 0 : (clampedPower - 1) / bandSize;
        long cost = (long) minCost + band;
        return Mth.clamp((int) Math.min(Integer.MAX_VALUE, cost), minCost, maxCost);
    }

    static int rollPower(int offerCost, ItemStack target, ItemStack modifier) {
        int power = offerCost;
        if (PoolModifierRules.isEssenceModifier(modifier) && !PoolModifierRules.blocksOffer(modifier)) {
            power += EffectiveBalance.essencePowerBonus();
        } else if (PoolModifierRules.isEnchantedBook(modifier)) {
            power += EffectiveBalance.bookPowerBonus();
        }
        if (target.is(ModTags.Items.MATERIAL_GOLD) || SilentGearCompat.hasMaterialItemTag(target, ModTags.Items.MATERIAL_GOLD)) {
            power += EffectiveBalance.goldMaterialPowerBonus();
        }
        return Math.max(1, power);
    }
}
