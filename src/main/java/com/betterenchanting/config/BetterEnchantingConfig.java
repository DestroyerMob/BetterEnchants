package com.betterenchanting.config;

import java.util.Locale;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class BetterEnchantingConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue ANVIL_MAX_COST;
    private static final ModConfigSpec.EnumValue<AnvilLevelMergeMode> ANVIL_LEVEL_MERGE_MODE;
    private static final ModConfigSpec.BooleanValue ENHANCED_TABLE_TAKEOVER;
    private static final ModConfigSpec.IntValue ENCHANTING_MAX_BOOKSHELF_POWER;
    private static final ModConfigSpec.IntValue ENCHANTING_MIN_BASE_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_MAX_BASE_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_BASE_COST_PER_BOOKSHELF_POWER;
    private static final ModConfigSpec.IntValue ENCHANTING_LAPIS_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_ESSENCE_POWER_BONUS;
    private static final ModConfigSpec.IntValue ENCHANTING_BOOK_POWER_BONUS;
    private static final ModConfigSpec.IntValue ENCHANTING_GOLD_MATERIAL_POWER_BONUS;
    private static final ModConfigSpec.DoubleValue ENCHANTING_BOOK_WEIGHT_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue ENCHANTING_NEW_TAG_COMBO_MULTIPLIER;
    private static final ModConfigSpec.IntValue ENCHANTING_MAX_CANDIDATE_WEIGHT;
    private static final ModConfigSpec.IntValue VEIN_MINER_CONNECTED_BLOCKS_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue SHOCKED_DAMAGE_MULTIPLIER;
    private static final ModConfigSpec.IntValue SHOCKING_DURATION_TICKS;
    private static final ModConfigSpec.DoubleValue CURSE_OF_REBOUND_REFLECTED_DAMAGE_RATIO;
    private static final ModConfigSpec.IntValue VERDANT_REGROWTH_BASE_REPAIR_INTERVAL_TICKS;
    private static final ModConfigSpec.IntValue VERDANT_REGROWTH_FAST_REPAIR_INTERVAL_TICKS;
    private static final ModConfigSpec.IntValue VERDANT_REGROWTH_SCAN_HORIZONTAL_RADIUS;
    private static final ModConfigSpec.IntValue VERDANT_REGROWTH_SCAN_VERTICAL_RADIUS;
    private static final ModConfigSpec.DoubleValue ESSENCE_ACQUISITION_DIRECT_DROP_CHANCE;
    private static final ModConfigSpec.IntValue MENDING_BASE_CHANCE_DENOMINATOR;
    private static final ModConfigSpec.IntValue MENDING_DENOMINATOR_REDUCTION_PER_LEVEL;
    private static final ModConfigSpec.IntValue MENDING_MIN_CHANCE_DENOMINATOR;
    private static final ModConfigSpec.IntValue MENDING_DURABILITY_REPAIRED_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue FORTUNES_TOUCH_SECONDARY_DROP_CHANCE_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue FORTUNES_TOUCH_SECONDARY_DROP_MAX_CHANCE;
    private static final ModConfigSpec.IntValue ROLLER_MULTI_ENCHANT_ROLL_BOUND;
    private static final ModConfigSpec.IntValue ROLLER_MULTI_ENCHANT_LEVEL_DIVISOR;
    private static final ModConfigSpec.IntValue ROLLER_ENCHANTABILITY_DIVISOR;
    private static final ModConfigSpec.DoubleValue ROLLER_LEVEL_VARIANCE;
    private static final ModConfigSpec.EnumValue<ExperienceCurve> EXPERIENCE_CURVE;
    private static final ModConfigSpec.IntValue LINEAR_XP_PER_LEVEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("anvil");
        ANVIL_MAX_COST = builder
                .comment("Maximum XP level cost shown and charged by anvils. This also removes vanilla's Too Expensive cutoff.")
                .defineInRange("max_cost", 30, 1, 1_000_000);
        ANVIL_LEVEL_MERGE_MODE = builder
                .comment("VANILLA uses vanilla's same-level-ranks-up rule. ADDITIVE adds matching enchantment levels together.")
                .defineEnum("enchantment_level_merge", AnvilLevelMergeMode.ADDITIVE);
        builder.pop();

        builder.push("enchanting");
        ENHANCED_TABLE_TAKEOVER = builder
                .comment("When true, vanilla enchanting tables open Better Enchanting's enhanced UI. Disable this to leave vanilla/other mods' enchanting table behavior alone and use the Arcane Crucible block instead.")
                .define("enhanced_table_takeover", true);
        ENCHANTING_MAX_BOOKSHELF_POWER = builder
                .comment("Bookshelf power used as the maximum for enhanced enchanting offer cost and roll quality.")
                .defineInRange("max_bookshelf_power", 15, 0, 64);
        ENCHANTING_MIN_BASE_COST = builder
                .comment("Minimum XP level cost for an enhanced enchanting offer.")
                .defineInRange("min_base_cost", 1, 0, 1_000_000);
        ENCHANTING_MAX_BASE_COST = builder
                .comment("Maximum XP level cost for an enhanced enchanting offer before event hooks.")
                .defineInRange("max_base_cost", 30, 1, 1_000_000);
        ENCHANTING_BASE_COST_PER_BOOKSHELF_POWER = builder
                .comment("XP level cost added per bookshelf power point.")
                .defineInRange("base_cost_per_bookshelf_power", 2, 0, 1_000_000);
        ENCHANTING_LAPIS_COST = builder
                .comment("Lapis lazuli consumed by each enhanced enchanting offer.")
                .defineInRange("lapis_cost", 1, 0, 64);
        ENCHANTING_ESSENCE_POWER_BONUS = builder
                .comment("Extra roll power added when an option is controlled by a normal essence.")
                .defineInRange("essence_power_bonus", 2, 0, 1_000_000);
        ENCHANTING_BOOK_POWER_BONUS = builder
                .comment("Extra roll power added when an option is controlled by an enchanted book.")
                .defineInRange("book_power_bonus", 2, 0, 1_000_000);
        ENCHANTING_GOLD_MATERIAL_POWER_BONUS = builder
                .comment("Extra roll power added when the target item has the Better Enchanting gold material tag.")
                .defineInRange("gold_material_power_bonus", 1, 0, 1_000_000);
        ENCHANTING_BOOK_WEIGHT_MULTIPLIER = builder
                .comment("Weight multiplier applied for each matching enchanted book in a roll.")
                .defineInRange("book_weight_multiplier", 8.0D, 1.0D, 1_000_000.0D);
        ENCHANTING_NEW_TAG_COMBO_MULTIPLIER = builder
                .comment("Weight multiplier used when a multi-enchant roll can add a new represented essence tag.")
                .defineInRange("new_tag_combo_multiplier", 3.0D, 1.0D, 1_000_000.0D);
        ENCHANTING_MAX_CANDIDATE_WEIGHT = builder
                .comment("Safety cap for any single weighted enchantment candidate.")
                .defineInRange("max_candidate_weight", 1_000_000, 1, 1_000_000_000);
        builder.pop();

        builder.push("vein_miner");
        VEIN_MINER_CONNECTED_BLOCKS_PER_LEVEL = builder
                .comment("Maximum connected matching blocks broken per Vein Miner level.")
                .defineInRange("connected_blocks_per_level", 16, 1, 4096);
        builder.pop();

        builder.push("shocked");
        SHOCKED_DAMAGE_MULTIPLIER = builder
                .comment("Incoming damage multiplier applied to entities with the Shocked effect.")
                .defineInRange("damage_multiplier", 1.2D, 0.0D, 100.0D);
        builder.pop();

        builder.push("shocking");
        SHOCKING_DURATION_TICKS = builder
                .comment("Duration, in ticks, applied by the Shocking enchantment.")
                .defineInRange("duration_ticks", 100, 0, 72_000);
        builder.pop();

        builder.push("curse_of_rebound");
        CURSE_OF_REBOUND_REFLECTED_DAMAGE_RATIO = builder
                .comment("Fraction of final dealt damage reflected back to the attacking player.")
                .defineInRange("reflected_damage_ratio", 0.25D, 0.0D, 100.0D);
        builder.pop();

        builder.push("verdant_regrowth");
        VERDANT_REGROWTH_BASE_REPAIR_INTERVAL_TICKS = builder
                .comment("Normal repair interval in ticks when Verdant Regrowth is near growth or in a verdant biome.")
                .defineInRange("base_repair_interval_ticks", 200, 1, 72_000);
        VERDANT_REGROWTH_FAST_REPAIR_INTERVAL_TICKS = builder
                .comment("Accelerated repair interval in ticks when Verdant Regrowth also has sunlight or rain.")
                .defineInRange("fast_repair_interval_ticks", 100, 1, 72_000);
        VERDANT_REGROWTH_SCAN_HORIZONTAL_RADIUS = builder
                .comment("Horizontal radius scanned for nearby growth blocks.")
                .defineInRange("scan_horizontal_radius", 4, 0, 32);
        VERDANT_REGROWTH_SCAN_VERTICAL_RADIUS = builder
                .comment("Vertical radius scanned for nearby growth blocks.")
                .defineInRange("scan_vertical_radius", 2, 0, 32);
        builder.pop();

        builder.push("essence_acquisition");
        ESSENCE_ACQUISITION_DIRECT_DROP_CHANCE = builder
                .comment("Chance for event-driven essence drops such as Fortune mining, charged creepers, lava fishing, Luck of the Sea fishing, and zombie villager curing. Loot-table injected essence chances stay data-pack controlled.")
                .defineInRange("direct_drop_chance", 0.2D, 0.0D, 1.0D);
        builder.pop();

        builder.push("mending");
        MENDING_BASE_CHANCE_DENOMINATOR = builder
                .comment("Base denominator for Mending's per-XP repair roll. The default starts level 1 at a 1 in 10 chance.")
                .defineInRange("base_chance_denominator", 10, 1, 1_000_000);
        MENDING_DENOMINATOR_REDUCTION_PER_LEVEL = builder
                .comment("How much the Mending roll denominator is reduced for each level above zero.")
                .defineInRange("denominator_reduction_per_level", 1, 0, 1_000_000);
        MENDING_MIN_CHANCE_DENOMINATOR = builder
                .comment("Lower bound for the Mending roll denominator.")
                .defineInRange("min_chance_denominator", 1, 1, 1_000_000);
        MENDING_DURABILITY_REPAIRED_PER_LEVEL = builder
                .comment("Durability repaired for each successful Mending roll per enchantment level.")
                .defineInRange("durability_repaired_per_level", 2, 0, 1_000_000);
        builder.pop();

        builder.push("fortunes_touch");
        FORTUNES_TOUCH_SECONDARY_DROP_CHANCE_PER_LEVEL = builder
                .comment("Secondary ordinary-drop chance added by each Fortunes Touch level.")
                .defineInRange("secondary_drop_chance_per_level", 0.1D, 0.0D, 1.0D);
        FORTUNES_TOUCH_SECONDARY_DROP_MAX_CHANCE = builder
                .comment("Maximum secondary ordinary-drop chance for Fortunes Touch.")
                .defineInRange("secondary_drop_max_chance", 1.0D, 0.0D, 1.0D);
        builder.pop();

        builder.push("enchanting_rolls");
        ROLLER_MULTI_ENCHANT_ROLL_BOUND = builder
                .comment("Exclusive random bound used when deciding whether another enchantment is added to an offer. Vanilla-like default is 50.")
                .defineInRange("multi_enchant_roll_bound", 50, 1, 1_000_000);
        ROLLER_MULTI_ENCHANT_LEVEL_DIVISOR = builder
                .comment("Divisor applied to roll level after each extra enchantment is selected.")
                .defineInRange("multi_enchant_level_divisor", 2, 1, 1_000_000);
        ROLLER_ENCHANTABILITY_DIVISOR = builder
                .comment("Divisor used for the target item's enchantability bonus rolls.")
                .defineInRange("enchantability_divisor", 4, 1, 1_000_000);
        ROLLER_LEVEL_VARIANCE = builder
                .comment("Random level variance used by the offer roller. The default 0.15 means plus or minus 15 percent.")
                .defineInRange("level_variance", 0.15D, 0.0D, 10.0D);
        builder.pop();

        builder.push("experience");
        EXPERIENCE_CURVE = builder
                .comment("EXPONENTIAL keeps vanilla's increasing XP-per-level curve. LINEAR makes every level require the same XP amount.")
                .defineEnum("curve", ExperienceCurve.EXPONENTIAL);
        LINEAR_XP_PER_LEVEL = builder
                .comment("XP required for each level when experience.curve is LINEAR.")
                .defineInRange("linear_xp_per_level", 7, 1, 1_000_000);
        builder.pop();

        SPEC = builder.build();
    }

    private BetterEnchantingConfig() {
    }

    public static int anvilMaxCost() {
        return ANVIL_MAX_COST.get();
    }

    public static boolean usesAdditiveAnvilLevelMerging() {
        return ANVIL_LEVEL_MERGE_MODE.get() == AnvilLevelMergeMode.ADDITIVE;
    }

    public static boolean takesOverEnchantingTable() {
        return ENHANCED_TABLE_TAKEOVER.get();
    }

    public static int maxBookshelfPower() {
        return ENCHANTING_MAX_BOOKSHELF_POWER.get();
    }

    public static int minEnchantingBaseCost() {
        return ENCHANTING_MIN_BASE_COST.get();
    }

    public static int maxEnchantingBaseCost() {
        return ENCHANTING_MAX_BASE_COST.get();
    }

    public static int enchantingBaseCostPerBookshelfPower() {
        return ENCHANTING_BASE_COST_PER_BOOKSHELF_POWER.get();
    }

    public static int enchantingLapisCost() {
        return ENCHANTING_LAPIS_COST.get();
    }

    public static int essencePowerBonus() {
        return ENCHANTING_ESSENCE_POWER_BONUS.get();
    }

    public static int bookPowerBonus() {
        return ENCHANTING_BOOK_POWER_BONUS.get();
    }

    public static int goldMaterialPowerBonus() {
        return ENCHANTING_GOLD_MATERIAL_POWER_BONUS.get();
    }

    public static double bookWeightMultiplier() {
        return ENCHANTING_BOOK_WEIGHT_MULTIPLIER.get();
    }

    public static double newTagComboMultiplier() {
        return ENCHANTING_NEW_TAG_COMBO_MULTIPLIER.get();
    }

    public static int maxCandidateWeight() {
        return ENCHANTING_MAX_CANDIDATE_WEIGHT.get();
    }

    public static int veinMinerConnectedBlocksPerLevel() {
        return VEIN_MINER_CONNECTED_BLOCKS_PER_LEVEL.get();
    }

    public static float shockedDamageMultiplier() {
        return SHOCKED_DAMAGE_MULTIPLIER.get().floatValue();
    }

    public static int shockingDurationTicks() {
        return SHOCKING_DURATION_TICKS.get();
    }

    public static float curseOfReboundReflectedDamageRatio() {
        return CURSE_OF_REBOUND_REFLECTED_DAMAGE_RATIO.get().floatValue();
    }

    public static int verdantRegrowthBaseRepairIntervalTicks() {
        return VERDANT_REGROWTH_BASE_REPAIR_INTERVAL_TICKS.get();
    }

    public static int verdantRegrowthFastRepairIntervalTicks() {
        return VERDANT_REGROWTH_FAST_REPAIR_INTERVAL_TICKS.get();
    }

    public static int verdantRegrowthScanHorizontalRadius() {
        return VERDANT_REGROWTH_SCAN_HORIZONTAL_RADIUS.get();
    }

    public static int verdantRegrowthScanVerticalRadius() {
        return VERDANT_REGROWTH_SCAN_VERTICAL_RADIUS.get();
    }

    public static float essenceDirectDropChance() {
        return ESSENCE_ACQUISITION_DIRECT_DROP_CHANCE.get().floatValue();
    }

    public static int mendingBaseChanceDenominator() {
        return MENDING_BASE_CHANCE_DENOMINATOR.get();
    }

    public static int mendingDenominatorReductionPerLevel() {
        return MENDING_DENOMINATOR_REDUCTION_PER_LEVEL.get();
    }

    public static int mendingMinChanceDenominator() {
        return MENDING_MIN_CHANCE_DENOMINATOR.get();
    }

    public static int mendingDurabilityRepairedPerLevel() {
        return MENDING_DURABILITY_REPAIRED_PER_LEVEL.get();
    }

    public static float fortunesTouchSecondaryDropChancePerLevel() {
        return FORTUNES_TOUCH_SECONDARY_DROP_CHANCE_PER_LEVEL.get().floatValue();
    }

    public static float fortunesTouchSecondaryDropMaxChance() {
        return FORTUNES_TOUCH_SECONDARY_DROP_MAX_CHANCE.get().floatValue();
    }

    public static int rollerMultiEnchantRollBound() {
        return ROLLER_MULTI_ENCHANT_ROLL_BOUND.get();
    }

    public static int rollerMultiEnchantLevelDivisor() {
        return ROLLER_MULTI_ENCHANT_LEVEL_DIVISOR.get();
    }

    public static int rollerEnchantabilityDivisor() {
        return ROLLER_ENCHANTABILITY_DIVISOR.get();
    }

    public static float rollerLevelVariance() {
        return ROLLER_LEVEL_VARIANCE.get().floatValue();
    }

    public static boolean usesLinearExperienceCurve() {
        return EXPERIENCE_CURVE.get() == ExperienceCurve.LINEAR;
    }

    public static int xpNeededForNextLevel() {
        return LINEAR_XP_PER_LEVEL.get();
    }

    public enum ExperienceCurve {
        EXPONENTIAL,
        LINEAR;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum AnvilLevelMergeMode {
        VANILLA,
        ADDITIVE;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
