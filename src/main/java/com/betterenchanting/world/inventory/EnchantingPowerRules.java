package com.betterenchanting.world.inventory;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.compat.ModularMaterialCompat;
import com.betterenchanting.registry.ModTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class EnchantingPowerRules {
    private EnchantingPowerRules() {
    }

    public static int clampBookshelfPower(int bookshelfPower) {
        return Mth.clamp(bookshelfPower, 0, EffectiveBalance.maxBookshelfPower());
    }

    public static int offerRequirementForBookshelfPower(int bookshelfPower) {
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

    public static int offerRequirementForBookshelfPower(int bookshelfPower, int option, int enchantmentSeed, ItemStack target) {
        int highRollRequirement = offerRequirementForBookshelfPower(bookshelfPower);
        if (highRollRequirement <= 0) {
            return 0;
        }

        int clampedOption = Mth.clamp(option, 0, 2);
        int clampedPower = clampBookshelfPower(bookshelfPower);
        RandomSource random = RandomSource.create();
        random.setSeed(enchantmentSeed);

        int selectedVanillaRequirement = 0;
        int highVanillaRequirement = 0;
        for (int index = 0; index < 3; index++) {
            int requirement = normalizeOfferRequirement(
                    EnchantmentHelper.getEnchantmentCost(random, index, clampedPower, target),
                    index
            );
            if (index == clampedOption) {
                selectedVanillaRequirement = requirement;
            }
            if (index == 2) {
                highVanillaRequirement = requirement;
            }
        }

        if (selectedVanillaRequirement <= 0 || highVanillaRequirement <= 0) {
            return 0;
        }
        int scaledRequirement = (int) Math.ceil((double) selectedVanillaRequirement * highRollRequirement / highVanillaRequirement);
        return Mth.clamp(scaledRequirement, 1, highRollRequirement);
    }

    private static int normalizeOfferRequirement(int requirement, int option) {
        return requirement < option + 1 ? requirement + 1 : requirement;
    }

    public static int rollPower(int offerCost, ItemStack target, ItemStack modifier) {
        int power = offerCost;
        if (PoolModifierRules.isEssenceModifier(modifier) && !PoolModifierRules.blocksOffer(modifier)) {
            power += EffectiveBalance.essencePowerBonus();
        } else if (PoolModifierRules.isEnchantedBook(modifier)) {
            power += EffectiveBalance.bookPowerBonus();
        }
        if (target.is(ModTags.Items.MATERIAL_GOLD) || ModularMaterialCompat.hasMaterialItemTag(target, ModTags.Items.MATERIAL_GOLD)) {
            power += EffectiveBalance.goldMaterialPowerBonus();
        }
        return Math.max(1, power);
    }
}
