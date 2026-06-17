package com.betterenchanting.world.inventory;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.registry.ModTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class EnchantingPowerRules {
    private EnchantingPowerRules() {
    }

    static int clampBookshelfPower(int bookshelfPower) {
        return Mth.clamp(bookshelfPower, 0, BetterEnchantingConfig.maxBookshelfPower());
    }

    static int offerCostForBookshelfPower(int bookshelfPower) {
        int minCost = Math.min(BetterEnchantingConfig.minEnchantingBaseCost(), BetterEnchantingConfig.maxEnchantingBaseCost());
        int maxCost = Math.max(BetterEnchantingConfig.minEnchantingBaseCost(), BetterEnchantingConfig.maxEnchantingBaseCost());
        long cost = (long) minCost + (long) clampBookshelfPower(bookshelfPower) * BetterEnchantingConfig.enchantingBaseCostPerBookshelfPower();
        return Mth.clamp((int) Math.min(Integer.MAX_VALUE, cost), minCost, maxCost);
    }

    static int rollPower(int offerCost, ItemStack target, ItemStack modifier) {
        int power = offerCost;
        if (PoolModifierRules.isEssenceModifier(modifier) && !PoolModifierRules.isPurificationModifier(modifier)) {
            power += BetterEnchantingConfig.essencePowerBonus();
        } else if (PoolModifierRules.isEnchantedBook(modifier)) {
            power += BetterEnchantingConfig.bookPowerBonus();
        }
        if (target.is(ModTags.Items.MATERIAL_GOLD)) {
            power += BetterEnchantingConfig.goldMaterialPowerBonus();
        }
        return Math.max(1, power);
    }
}
