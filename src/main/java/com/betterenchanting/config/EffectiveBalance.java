package com.betterenchanting.config;

import com.betterenchanting.config.BetterEnchantingConfig.AnvilLevelMergeMode;
import com.betterenchanting.config.BetterEnchantingConfig.BalancePreset;
import com.betterenchanting.config.BetterEnchantingConfig.ExperienceCurve;

public final class EffectiveBalance {
    private static final BalanceValues VANILLA_PLUS = new BalanceValues(
            false,
            30,
            AnvilLevelMergeMode.VANILLA,
            15,
            1,
            30,
            2,
            1,
            3,
            5,
            1,
            1,
            1,
            0,
            4.0D,
            2.0D,
            1_000_000,
            8,
            64,
            4,
            4,
            1.0D,
            3,
            1.5D,
            -0.05D,
            0.05D,
            1.1D,
            60,
            0.2D,
            0.5D,
            2400,
            1200,
            1,
            1,
            1,
            0.1D,
            6,
            0,
            6,
            1,
            0.05D,
            0.25D,
            60,
            2,
            4,
            0.1D,
            ExperienceCurve.EXPONENTIAL,
            7
    );
    private static final BalanceValues BALANCED = new BalanceValues(
            true,
            30,
            AnvilLevelMergeMode.ADDITIVE,
            15,
            1,
            30,
            2,
            1,
            3,
            5,
            1,
            2,
            2,
            1,
            8.0D,
            3.0D,
            1_000_000,
            16,
            96,
            4,
            4,
            1.0D,
            4,
            2.0D,
            -0.12D,
            0.12D,
            1.2D,
            100,
            0.25D,
            1.0D,
            1200,
            600,
            1,
            1,
            1,
            0.2D,
            10,
            1,
            1,
            2,
            0.1D,
            1.0D,
            50,
            2,
            4,
            0.15D,
            ExperienceCurve.EXPONENTIAL,
            7
    );
    private static final BalanceValues OVERHAUL = new BalanceValues(
            true,
            40,
            AnvilLevelMergeMode.ADDITIVE,
            15,
            1,
            30,
            2,
            1,
            3,
            5,
            1,
            3,
            3,
            1,
            12.0D,
            4.0D,
            1_000_000,
            20,
            96,
            4,
            4,
            0.95D,
            5,
            2.0D,
            -0.15D,
            0.15D,
            1.25D,
            120,
            0.2D,
            1.0D,
            1000,
            500,
            1,
            1,
            1,
            0.25D,
            12,
            1,
            1,
            2,
            0.12D,
            1.0D,
            45,
            2,
            4,
            0.18D,
            ExperienceCurve.EXPONENTIAL,
            7
    );
    private static final BalanceValues POWER_FANTASY = new BalanceValues(
            true,
            100,
            AnvilLevelMergeMode.ADDITIVE,
            20,
            1,
            20,
            1,
            1,
            2,
            10,
            0,
            5,
            5,
            2,
            16.0D,
            5.0D,
            1_000_000_000,
            32,
            192,
            4,
            2,
            0.9D,
            8,
            2.5D,
            -0.05D,
            0.25D,
            1.5D,
            160,
            0.1D,
            1.5D,
            600,
            300,
            2,
            2,
            2,
            0.5D,
            4,
            1,
            1,
            4,
            0.2D,
            1.0D,
            35,
            2,
            3,
            0.25D,
            ExperienceCurve.LINEAR,
            7
    );

    private EffectiveBalance() {
    }

    public static BalancePreset preset() {
        return BetterEnchantingConfig.preset();
    }

    public static boolean usesAdvancedConfigValues() {
        return BetterEnchantingConfig.preset() == BalancePreset.CUSTOM || BetterEnchantingConfig.allowsAdvancedOverrides();
    }

    public static int anvilMaxCost() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.anvilMaxCost() : values.anvilMaxCost();
    }

    public static boolean usesAdditiveAnvilLevelMerging() {
        BalanceValues values = presetValues();
        return values == null
                ? BetterEnchantingConfig.usesAdditiveAnvilLevelMerging()
                : values.anvilLevelMergeMode() == AnvilLevelMergeMode.ADDITIVE;
    }

    public static boolean takesOverEnchantingTable() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.takesOverEnchantingTable() : values.enhancedTableTakeover();
    }

    public static int maxBookshelfPower() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.maxBookshelfPower() : values.maxBookshelfPower();
    }

    public static int minEnchantingBaseCost() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.minEnchantingBaseCost() : values.minEnchantingBaseCost();
    }

    public static int maxEnchantingBaseCost() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.maxEnchantingBaseCost() : values.maxEnchantingBaseCost();
    }

    public static int enchantingBaseCostPerBookshelfPower() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.enchantingBaseCostPerBookshelfPower() : values.enchantingBaseCostPerBookshelfPower();
    }

    public static int minEnchantingLevelCost() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.minEnchantingLevelCost() : values.minEnchantingLevelCost();
    }

    public static int maxEnchantingLevelCost() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.maxEnchantingLevelCost() : values.maxEnchantingLevelCost();
    }

    public static int bookshelfPowerPerLevelCost() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.bookshelfPowerPerLevelCost() : values.bookshelfPowerPerLevelCost();
    }

    public static int enchantingLapisCost() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.enchantingLapisCost() : values.enchantingLapisCost();
    }

    public static int essencePowerBonus() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.essencePowerBonus() : values.essencePowerBonus();
    }

    public static int bookPowerBonus() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.bookPowerBonus() : values.bookPowerBonus();
    }

    public static int goldMaterialPowerBonus() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.goldMaterialPowerBonus() : values.goldMaterialPowerBonus();
    }

    public static double bookWeightMultiplier() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.bookWeightMultiplier() : values.bookWeightMultiplier();
    }

    public static double newTagComboMultiplier() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.newTagComboMultiplier() : values.newTagComboMultiplier();
    }

    public static int maxCandidateWeight() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.maxCandidateWeight() : values.maxCandidateWeight();
    }

    public static int veinMinerConnectedBlocksPerLevel() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.veinMinerConnectedBlocksPerLevel() : values.veinMinerConnectedBlocksPerLevel();
    }

    public static int treeCapitatorMaxLogs() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.treeCapitatorMaxLogs() : values.treeCapitatorMaxLogs();
    }

    public static int treeCapitatorLeafScanRadius() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.treeCapitatorLeafScanRadius() : values.treeCapitatorLeafScanRadius();
    }

    public static int treeCapitatorMinNaturalLeaves() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.treeCapitatorMinNaturalLeaves() : values.treeCapitatorMinNaturalLeaves();
    }

    public static float perfectStrikeReadyThreshold() {
        BalanceValues values = presetValues();
        return (float) (values == null ? BetterEnchantingConfig.perfectStrikeReadyThreshold() : values.perfectStrikeReadyThreshold());
    }

    public static int perfectStrikeWindowTicks() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.perfectStrikeWindowTicks() : values.perfectStrikeWindowTicks();
    }

    public static float perfectStrikeDamageMultiplier() {
        BalanceValues values = presetValues();
        return (float) (values == null ? BetterEnchantingConfig.perfectStrikeDamageMultiplier() : values.perfectStrikeDamageMultiplier());
    }

    public static double perfectStrikeMinCooldownVariance() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.perfectStrikeMinCooldownVariance() : values.perfectStrikeMinCooldownVariance();
    }

    public static double perfectStrikeMaxCooldownVariance() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.perfectStrikeMaxCooldownVariance() : values.perfectStrikeMaxCooldownVariance();
    }

    public static float shockedDamageMultiplier() {
        BalanceValues values = presetValues();
        return (float) (values == null ? BetterEnchantingConfig.shockedDamageMultiplier() : values.shockedDamageMultiplier());
    }

    public static String shockedParticleType() {
        return BetterEnchantingConfig.shockedParticleType();
    }

    public static boolean shockedParticlesEnabled() {
        return BetterEnchantingConfig.shockedParticlesEnabled();
    }

    public static int shockedParticleIntervalTicks() {
        return BetterEnchantingConfig.shockedParticleIntervalTicks();
    }

    public static int shockedParticleCount() {
        return BetterEnchantingConfig.shockedParticleCount();
    }

    public static double shockedParticleHorizontalSpread() {
        return BetterEnchantingConfig.shockedParticleHorizontalSpread();
    }

    public static double shockedParticleVerticalSpread() {
        return BetterEnchantingConfig.shockedParticleVerticalSpread();
    }

    public static double shockedParticleSpeed() {
        return BetterEnchantingConfig.shockedParticleSpeed();
    }

    public static int shockingDurationTicks() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.shockingDurationTicks() : values.shockingDurationTicks();
    }

    public static float curseOfReboundReflectedDamageRatio() {
        BalanceValues values = presetValues();
        return (float) (values == null ? BetterEnchantingConfig.curseOfReboundReflectedDamageRatio() : values.curseOfReboundReflectedDamageRatio());
    }

    public static float seismicCushionExplosionRadiusPerLevel() {
        BalanceValues values = presetValues();
        return (float) (values == null ? BetterEnchantingConfig.seismicCushionExplosionRadiusPerLevel() : values.seismicCushionExplosionRadiusPerLevel());
    }

    public static int verdantRegrowthBaseRepairIntervalTicks() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.verdantRegrowthBaseRepairIntervalTicks() : values.verdantRegrowthBaseRepairIntervalTicks();
    }

    public static int verdantRegrowthFastRepairIntervalTicks() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.verdantRegrowthFastRepairIntervalTicks() : values.verdantRegrowthFastRepairIntervalTicks();
    }

    public static int verdantRegrowthDurabilityRepairedPerLevel() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.verdantRegrowthDurabilityRepairedPerLevel() : values.verdantRegrowthDurabilityRepairedPerLevel();
    }

    public static int verdantRegrowthScanHorizontalRadius() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.verdantRegrowthScanHorizontalRadius() : values.verdantRegrowthScanHorizontalRadius();
    }

    public static int verdantRegrowthScanVerticalRadius() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.verdantRegrowthScanVerticalRadius() : values.verdantRegrowthScanVerticalRadius();
    }

    public static float essenceDirectDropChance() {
        BalanceValues values = presetValues();
        return (float) (values == null ? BetterEnchantingConfig.essenceDirectDropChance() : values.essenceDirectDropChance());
    }

    public static int mendingBaseChanceDenominator() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.mendingBaseChanceDenominator() : values.mendingBaseChanceDenominator();
    }

    public static int mendingDenominatorReductionPerLevel() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.mendingDenominatorReductionPerLevel() : values.mendingDenominatorReductionPerLevel();
    }

    public static int mendingMinChanceDenominator() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.mendingMinChanceDenominator() : values.mendingMinChanceDenominator();
    }

    public static int mendingDurabilityRepairedPerLevel() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.mendingDurabilityRepairedPerLevel() : values.mendingDurabilityRepairedPerLevel();
    }

    public static float fortunesTouchSecondaryDropChancePerLevel() {
        BalanceValues values = presetValues();
        return (float) (values == null ? BetterEnchantingConfig.fortunesTouchSecondaryDropChancePerLevel() : values.fortunesTouchSecondaryDropChancePerLevel());
    }

    public static float fortunesTouchSecondaryDropMaxChance() {
        BalanceValues values = presetValues();
        return (float) (values == null ? BetterEnchantingConfig.fortunesTouchSecondaryDropMaxChance() : values.fortunesTouchSecondaryDropMaxChance());
    }

    public static int rollerMultiEnchantRollBound() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.rollerMultiEnchantRollBound() : values.rollerMultiEnchantRollBound();
    }

    public static int rollerMultiEnchantLevelDivisor() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.rollerMultiEnchantLevelDivisor() : values.rollerMultiEnchantLevelDivisor();
    }

    public static int rollerEnchantabilityDivisor() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.rollerEnchantabilityDivisor() : values.rollerEnchantabilityDivisor();
    }

    public static float rollerLevelVariance() {
        BalanceValues values = presetValues();
        return (float) (values == null ? BetterEnchantingConfig.rollerLevelVariance() : values.rollerLevelVariance());
    }

    public static boolean usesLinearExperienceCurve() {
        BalanceValues values = presetValues();
        return values == null
                ? BetterEnchantingConfig.usesLinearExperienceCurve()
                : values.experienceCurve() == ExperienceCurve.LINEAR;
    }

    public static int xpNeededForNextLevel() {
        BalanceValues values = presetValues();
        return values == null ? BetterEnchantingConfig.xpNeededForNextLevel() : values.xpNeededForNextLevel();
    }

    private static BalanceValues presetValues() {
        if (usesAdvancedConfigValues()) {
            return null;
        }
        return switch (BetterEnchantingConfig.preset()) {
            case VANILLA_PLUS -> VANILLA_PLUS;
            case BALANCED -> BALANCED;
            case OVERHAUL -> OVERHAUL;
            case POWER_FANTASY -> POWER_FANTASY;
            case CUSTOM -> null;
        };
    }
}
