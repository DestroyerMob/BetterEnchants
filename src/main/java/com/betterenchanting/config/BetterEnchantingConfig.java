package com.betterenchanting.config;

import java.util.List;
import java.util.Locale;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class BetterEnchantingConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<BalancePreset> BALANCE_PRESET;
    private static final ModConfigSpec.BooleanValue USE_ADVANCED_CONFIG_VALUES;
    private static final ModConfigSpec.IntValue ANVIL_MAX_COST;
    private static final ModConfigSpec.EnumValue<AnvilLevelMergeMode> ANVIL_LEVEL_MERGE_MODE;
    private static final ModConfigSpec.BooleanValue ENHANCED_TABLE_TAKEOVER;
    private static final ModConfigSpec.BooleanValue OVERRIDE_VANILLA_ENCHANTMENT_LIMITS;
    private static final ModConfigSpec.BooleanValue ALWAYS_SHOW_ROUTED_ENCHANTMENT_OVERLAY;
    private static final ModConfigSpec.IntValue ENCHANTING_MAX_BOOKSHELF_POWER;
    private static final ModConfigSpec.IntValue ENCHANTING_MIN_BASE_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_MAX_BASE_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_BASE_COST_PER_BOOKSHELF_POWER;
    private static final ModConfigSpec.IntValue ENCHANTING_MIN_LEVEL_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_MAX_LEVEL_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_BOOKSHELF_POWER_PER_LEVEL_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_LAPIS_COST;
    private static final ModConfigSpec.IntValue ENCHANTING_ESSENCE_POWER_BONUS;
    private static final ModConfigSpec.IntValue ENCHANTING_BOOK_POWER_BONUS;
    private static final ModConfigSpec.IntValue ENCHANTING_GOLD_MATERIAL_POWER_BONUS;
    private static final ModConfigSpec.DoubleValue ENCHANTING_BOOK_WEIGHT_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue ENCHANTING_NEW_TAG_COMBO_MULTIPLIER;
    private static final ModConfigSpec.IntValue ENCHANTING_MAX_CANDIDATE_WEIGHT;
    private static final ModConfigSpec.IntValue VEIN_MINER_CONNECTED_BLOCKS_PER_LEVEL;
    private static final ModConfigSpec.IntValue RESONANCE_HIGHLIGHT_DURATION_TICKS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RESONANCE_BLOCK_WHITELIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RESONANCE_BLOCK_BLACKLIST;
    private static final ModConfigSpec.IntValue TREE_CAPITATOR_MAX_LOGS;
    private static final ModConfigSpec.IntValue TREE_CAPITATOR_LEAF_SCAN_RADIUS;
    private static final ModConfigSpec.IntValue TREE_CAPITATOR_MIN_NATURAL_LEAVES;
    private static final ModConfigSpec.DoubleValue PERFECT_STRIKE_READY_THRESHOLD;
    private static final ModConfigSpec.IntValue PERFECT_STRIKE_WINDOW_TICKS;
    private static final ModConfigSpec.DoubleValue PERFECT_STRIKE_DAMAGE_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue PERFECT_STRIKE_MIN_COOLDOWN_VARIANCE;
    private static final ModConfigSpec.DoubleValue PERFECT_STRIKE_MAX_COOLDOWN_VARIANCE;
    private static final ModConfigSpec.DoubleValue SHOCKED_DAMAGE_MULTIPLIER;
    private static final ModConfigSpec.ConfigValue<String> SHOCKED_PARTICLE_TYPE;
    private static final ModConfigSpec.BooleanValue SHOCKED_PARTICLES_ENABLED;
    private static final ModConfigSpec.IntValue SHOCKED_PARTICLE_INTERVAL_TICKS;
    private static final ModConfigSpec.IntValue SHOCKED_PARTICLE_COUNT;
    private static final ModConfigSpec.DoubleValue SHOCKED_PARTICLE_HORIZONTAL_SPREAD;
    private static final ModConfigSpec.DoubleValue SHOCKED_PARTICLE_VERTICAL_SPREAD;
    private static final ModConfigSpec.DoubleValue SHOCKED_PARTICLE_SPEED;
    private static final ModConfigSpec.IntValue SHOCKING_DURATION_TICKS;
    private static final ModConfigSpec.IntValue FROSTBITE_FROST_TICKS_PER_LEVEL;
    private static final ModConfigSpec.IntValue FROSTBITE_FROZEN_DURATION_TICKS;
    private static final ModConfigSpec.IntValue CINDERSTEP_DURATION_TICKS;
    private static final ModConfigSpec.DoubleValue CINDERSTEP_SPEED_BONUS_PER_LEVEL;
    private static final ModConfigSpec.IntValue OVERCHARGED_DURATION_TICKS;
    private static final ModConfigSpec.IntValue OVERCHARGED_STRENGTH_MAX_AMPLIFIER;
    private static final ModConfigSpec.IntValue OVERCHARGED_REGENERATION_MAX_AMPLIFIER;
    private static final ModConfigSpec.IntValue OVERCHARGED_SPEED_MAX_AMPLIFIER;
    private static final ModConfigSpec.DoubleValue BEHEADING_BASE_HEAD_DROP_CHANCE;
    private static final ModConfigSpec.DoubleValue BEHEADING_HEAD_DROP_CHANCE_PER_LOOTING_LEVEL;
    private static final ModConfigSpec.DoubleValue BEHEADING_HEADSHOT_UPPER_EYE_BAND;
    private static final ModConfigSpec.DoubleValue BEHEADING_HEADSHOT_LOWER_EYE_BAND;
    private static final ModConfigSpec.DoubleValue HEADSHOT_DAMAGE_BONUS_PER_LEVEL;
    private static final ModConfigSpec.DoubleValue HEADSHOT_UPPER_EYE_BAND;
    private static final ModConfigSpec.DoubleValue HEADSHOT_LOWER_EYE_BAND;
    private static final ModConfigSpec.DoubleValue CURSE_OF_FRAGILITY_DURABILITY_DAMAGE_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue CURSE_OF_REBOUND_REFLECTED_DAMAGE_RATIO;
    private static final ModConfigSpec.DoubleValue SEISMIC_CUSHION_EXPLOSION_RADIUS_PER_LEVEL;
    private static final ModConfigSpec.IntValue VERDANT_REGROWTH_BASE_REPAIR_INTERVAL_TICKS;
    private static final ModConfigSpec.IntValue VERDANT_REGROWTH_FAST_REPAIR_INTERVAL_TICKS;
    private static final ModConfigSpec.IntValue VERDANT_REGROWTH_DURABILITY_REPAIRED_PER_LEVEL;
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

        builder.push("general");
        BALANCE_PRESET = builder
                .comment("High-level balance template. Use CUSTOM to make the advanced config values below the source of truth.")
                .defineEnum("preset", BalancePreset.BALANCED);
        USE_ADVANCED_CONFIG_VALUES = builder
                .comment("When false, the selected preset controls balance. When true, the advanced config values below are used instead of preset values. CUSTOM always uses advanced config values.")
                .define("use_advanced_config_values", false);
        builder.pop();

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
        OVERRIDE_VANILLA_ENCHANTMENT_LIMITS = builder
                .comment("When true, Better Enchanting applies its raised vanilla enchantment max levels and item enchantment capacity rules. When false, Better Enchanting's table and anvil paths clamp vanilla enchantments to their vanilla max levels and do not enforce Better Enchanting's per-item capacity limits.")
                .define("override_vanilla_enchantment_limits", true);
        ALWAYS_SHOW_ROUTED_ENCHANTMENT_OVERLAY = builder
                .comment("When true, in-world routed enchantment overlays appear around compatible workstations whenever the player is close enough. When false, players must hold an Attunement Focus in either hand.")
                .define("always_show_routed_enchantment_overlay", false);
        ENCHANTING_MAX_BOOKSHELF_POWER = builder
                .comment("Bookshelf power used as the maximum for enhanced enchanting requirements, charged-level bands, and roll quality.")
                .defineInRange("max_bookshelf_power", 15, 0, 64);
        ENCHANTING_MIN_BASE_COST = builder
                .comment("Minimum level requirement used by enhanced enchanting offers. This gates access and feeds roll quality; it is not the number of levels consumed.")
                .defineInRange("min_base_cost", 1, 0, 1_000_000);
        ENCHANTING_MAX_BASE_COST = builder
                .comment("Maximum level requirement used by enhanced enchanting offers before event hooks. This gates access and feeds roll quality; it is not the number of levels consumed.")
                .defineInRange("max_base_cost", 30, 1, 1_000_000);
        ENCHANTING_BASE_COST_PER_BOOKSHELF_POWER = builder
                .comment("Level requirement added per bookshelf power point before the configured maximum requirement is reached.")
                .defineInRange("base_cost_per_bookshelf_power", 2, 0, 1_000_000);
        ENCHANTING_MIN_LEVEL_COST = builder
                .comment("Minimum XP levels consumed by an enhanced enchanting offer.")
                .defineInRange("min_level_cost", 1, 1, 1_000_000);
        ENCHANTING_MAX_LEVEL_COST = builder
                .comment("Maximum XP levels consumed by an enhanced enchanting offer.")
                .defineInRange("max_level_cost", 3, 1, 1_000_000);
        ENCHANTING_BOOKSHELF_POWER_PER_LEVEL_COST = builder
                .comment("Bookshelf power covered by each charged-level band. With defaults, power 0-5 costs 1 level, 6-10 costs 2, and 11-15 costs 3.")
                .defineInRange("bookshelf_power_per_level_cost", 5, 1, 1_000_000);
        ENCHANTING_LAPIS_COST = builder
                .comment("Lapis lazuli consumed by each enhanced enchanting offer.")
                .defineInRange("lapis_cost", 1, 0, 64);
        ENCHANTING_ESSENCE_POWER_BONUS = builder
                .comment("Extra roll power added when an option is controlled by a normal essence. Intended balanced defaults use 0 so essences control the pool without increasing roll strength.")
                .defineInRange("essence_power_bonus", 0, 0, 1_000_000);
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

        builder.push("resonance");
        RESONANCE_HIGHLIGHT_DURATION_TICKS = builder
                .comment("Duration, in ticks, that Resonance outlines matching nearby ores after one is broken.")
                .defineInRange("highlight_duration_ticks", 160, 1, 1200);
        RESONANCE_BLOCK_WHITELIST = builder
                .comment("Block ids or block tags allowed for Resonance, such as minecraft:diamond_ore or #c:ores/diamond. Empty means any block in the common ores tag is allowed.")
                .defineListAllowEmpty("block_whitelist", List.of(), () -> "", value -> value instanceof String);
        RESONANCE_BLOCK_BLACKLIST = builder
                .comment("Block ids or block tags excluded from Resonance, using the same format as the whitelist. Blacklist entries override whitelist entries.")
                .defineListAllowEmpty("block_blacklist", List.of(), () -> "", value -> value instanceof String);
        builder.pop();

        builder.push("tree_capitator");
        TREE_CAPITATOR_MAX_LOGS = builder
                .comment("Maximum connected matching logs affected by one Tree Capitator activation, including the originally broken log.")
                .defineInRange("max_logs", 96, 1, 4096);
        TREE_CAPITATOR_LEAF_SCAN_RADIUS = builder
                .comment("Radius around connected logs scanned for natural leaves before Tree Capitator activates.")
                .defineInRange("leaf_scan_radius", 4, 0, 16);
        TREE_CAPITATOR_MIN_NATURAL_LEAVES = builder
                .comment("Minimum nearby non-persistent leaves required before Tree Capitator treats connected logs as a tree.")
                .defineInRange("min_natural_leaves", 4, 0, 4096);
        builder.pop();

        builder.push("perfect_strike");
        PERFECT_STRIKE_READY_THRESHOLD = builder
                .comment("Attack-strength scale required before the Perfect Strike timing window opens. Vanilla fully ready is 1.0.")
                .defineInRange("ready_threshold", 1.0D, 0.0D, 1.0D);
        PERFECT_STRIKE_WINDOW_TICKS = builder
                .comment("Ticks after the weapon becomes ready where a direct hit receives the Perfect Strike damage multiplier.")
                .defineInRange("window_ticks", 4, 0, 200);
        PERFECT_STRIKE_DAMAGE_MULTIPLIER = builder
                .comment("Damage multiplier applied to the direct hit when Perfect Strike lands. This stacks after vanilla critical-hit damage.")
                .defineInRange("damage_multiplier", 2.0D, 0.0D, 100.0D);
        PERFECT_STRIKE_MIN_COOLDOWN_VARIANCE = builder
                .comment("Minimum temporary attack-speed multiplier applied after a successful Perfect Strike weapon hit. Negative values make the next cooldown slower.")
                .defineInRange("min_cooldown_variance", -0.12D, -0.9D, 10.0D);
        PERFECT_STRIKE_MAX_COOLDOWN_VARIANCE = builder
                .comment("Maximum temporary attack-speed multiplier applied after a successful Perfect Strike weapon hit. Positive values make the next cooldown faster.")
                .defineInRange("max_cooldown_variance", 0.12D, -0.9D, 10.0D);
        builder.pop();

        builder.push("shocked");
        SHOCKED_DAMAGE_MULTIPLIER = builder
                .comment("Incoming damage multiplier applied to entities with the Shocked effect.")
                .defineInRange("damage_multiplier", 1.2D, 0.0D, 100.0D);
        SHOCKED_PARTICLE_TYPE = builder
                .comment("Simple particle id emitted by entities with the Shocked effect.")
                .define("particle_type", "minecraft:electric_spark");
        SHOCKED_PARTICLES_ENABLED = builder
                .comment("When true, Shocked hides vanilla potion swirl particles and emits the configured electric particles instead.")
                .define("particles_enabled", true);
        SHOCKED_PARTICLE_INTERVAL_TICKS = builder
                .comment("How often Shocked emits electric particles, in ticks.")
                .defineInRange("particle_interval_ticks", 2, 1, 1_000_000);
        SHOCKED_PARTICLE_COUNT = builder
                .comment("Number of Shocked particles emitted each interval.")
                .defineInRange("particle_count", 3, 0, 1_000);
        SHOCKED_PARTICLE_HORIZONTAL_SPREAD = builder
                .comment("Horizontal particle spread around the affected entity.")
                .defineInRange("particle_horizontal_spread", 0.35D, 0.0D, 16.0D);
        SHOCKED_PARTICLE_VERTICAL_SPREAD = builder
                .comment("Vertical particle spread around the affected entity.")
                .defineInRange("particle_vertical_spread", 0.75D, 0.0D, 16.0D);
        SHOCKED_PARTICLE_SPEED = builder
                .comment("Particle speed passed to the Shocked particle emitter.")
                .defineInRange("particle_speed", 0.03D, 0.0D, 16.0D);
        builder.pop();

        builder.push("shocking");
        SHOCKING_DURATION_TICKS = builder
                .comment("Duration, in ticks, applied by the Shocking enchantment.")
                .defineInRange("duration_ticks", 100, 0, 72_000);
        builder.pop();

        builder.push("frostbite");
        FROSTBITE_FROST_TICKS_PER_LEVEL = builder
                .comment("Vanilla frozen ticks added by each Frostbite level when the enchanted weapon deals damage. The target freezes when this reaches its vanilla freeze threshold.")
                .defineInRange("frost_ticks_per_level", 25, 0, 72_000);
        FROSTBITE_FROZEN_DURATION_TICKS = builder
                .comment("Duration, in ticks, of the Frozen effect applied when Frostbite reaches the target's freeze threshold. The default 100 ticks is 5 seconds.")
                .defineInRange("frozen_duration_ticks", 100, 0, 72_000);
        builder.pop();

        builder.push("cinderstep");
        CINDERSTEP_DURATION_TICKS = builder
                .comment("Duration, in ticks, of the Cinderstep speed boost after the wearer takes fire damage.")
                .defineInRange("duration_ticks", 60, 0, 72_000);
        CINDERSTEP_SPEED_BONUS_PER_LEVEL = builder
                .comment("Movement speed multiplier added by each Cinderstep level while the Cinderstep effect is active. The default 0.06 means +30% at level 5.")
                .defineInRange("speed_bonus_per_level", 0.06D, 0.0D, 10.0D);
        builder.pop();

        builder.push("overcharged");
        OVERCHARGED_DURATION_TICKS = builder
                .comment("Duration, in ticks, of Overcharged's buffs after the wearer is struck by lightning.")
                .defineInRange("duration_ticks", 200, 0, 72_000);
        OVERCHARGED_STRENGTH_MAX_AMPLIFIER = builder
                .comment("Highest vanilla Strength amplifier Overcharged can apply at max enchantment level. Amplifier 0 is Strength I.")
                .defineInRange("strength_max_amplifier", 4, 0, 255);
        OVERCHARGED_REGENERATION_MAX_AMPLIFIER = builder
                .comment("Highest vanilla Regeneration amplifier Overcharged can apply at max enchantment level. Amplifier 0 is Regeneration I.")
                .defineInRange("regeneration_max_amplifier", 2, 0, 255);
        OVERCHARGED_SPEED_MAX_AMPLIFIER = builder
                .comment("Highest vanilla Speed amplifier Overcharged can apply at max enchantment level. Amplifier 0 is Speed I.")
                .defineInRange("speed_max_amplifier", 0, 0, 255);
        builder.pop();

        builder.push("beheading");
        BEHEADING_BASE_HEAD_DROP_CHANCE = builder
                .comment("Base chance for Beheading to add one matching head when a player kills a supported mob or player with a melee headshot from an enchanted sword or axe.")
                .defineInRange("base_head_drop_chance", 0.10D, 0.0D, 1.0D);
        BEHEADING_HEAD_DROP_CHANCE_PER_LOOTING_LEVEL = builder
                .comment("Additional Beheading head drop chance added by each active Looting level on the killing weapon. The final chance is capped at 100% and still drops at most one head.")
                .defineInRange("head_drop_chance_per_looting_level", 0.05D, 0.0D, 1.0D);
        BEHEADING_HEADSHOT_UPPER_EYE_BAND = builder
                .comment("Highest valid melee hit position above the victim's eye height, measured as a fraction of the victim's full hitbox height.")
                .defineInRange("headshot_upper_eye_band", 0.20D, -1.0D, 1.0D);
        BEHEADING_HEADSHOT_LOWER_EYE_BAND = builder
                .comment("Lowest valid melee hit position below the victim's eye height, measured as a fraction of the victim's full hitbox height.")
                .defineInRange("headshot_lower_eye_band", -0.10D, -1.0D, 1.0D);
        builder.pop();

        builder.push("headshot");
        HEADSHOT_DAMAGE_BONUS_PER_LEVEL = builder
                .comment("Extra ranged projectile damage applied by each Headshot level when the projectile strikes the target's head. The default 0.10 means +50% at level 5.")
                .defineInRange("damage_bonus_per_level", 0.10D, 0.0D, 100.0D);
        HEADSHOT_UPPER_EYE_BAND = builder
                .comment("Highest valid ranged projectile hit position above the victim's eye height, measured as a fraction of the victim's full hitbox height.")
                .defineInRange("upper_eye_band", 0.20D, -1.0D, 1.0D);
        HEADSHOT_LOWER_EYE_BAND = builder
                .comment("Lowest valid ranged projectile hit position below the victim's eye height, measured as a fraction of the victim's full hitbox height.")
                .defineInRange("lower_eye_band", -0.10D, -1.0D, 1.0D);
        builder.pop();

        builder.push("curse_of_rebound");
        CURSE_OF_REBOUND_REFLECTED_DAMAGE_RATIO = builder
                .comment("Fraction of final dealt damage reflected back to the attacking player.")
                .defineInRange("reflected_damage_ratio", 0.25D, 0.0D, 100.0D);
        builder.pop();

        builder.push("curse_of_fragility");
        CURSE_OF_FRAGILITY_DURABILITY_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier applied to final durability damage after vanilla durability processing such as Unbreaking.")
                .defineInRange("durability_damage_multiplier", 2.0D, 0.0D, 100.0D);
        builder.pop();

        builder.push("seismic_cushion");
        SEISMIC_CUSHION_EXPLOSION_RADIUS_PER_LEVEL = builder
                .comment("Explosion radius added by each Seismic Cushion level when crouch-landing.")
                .defineInRange("explosion_radius_per_level", 1.0D, 0.0D, 100.0D);
        builder.pop();

        builder.push("verdant_regrowth");
        VERDANT_REGROWTH_BASE_REPAIR_INTERVAL_TICKS = builder
                .comment("Normal repair interval in ticks when Verdant Regrowth is near growth or in a verdant biome.")
                .defineInRange("base_repair_interval_ticks", 1200, 1, 72_000);
        VERDANT_REGROWTH_FAST_REPAIR_INTERVAL_TICKS = builder
                .comment("Accelerated repair interval in ticks when Verdant Regrowth also has sunlight.")
                .defineInRange("fast_repair_interval_ticks", 600, 1, 72_000);
        VERDANT_REGROWTH_DURABILITY_REPAIRED_PER_LEVEL = builder
                .comment("Durability repaired by each Verdant Regrowth level whenever the repair interval fires.")
                .defineInRange("durability_repaired_per_level", 1, 0, 1_000_000);
        VERDANT_REGROWTH_SCAN_HORIZONTAL_RADIUS = builder
                .comment("Horizontal radius scanned for nearby growth blocks.")
                .defineInRange("scan_horizontal_radius", 1, 0, 32);
        VERDANT_REGROWTH_SCAN_VERTICAL_RADIUS = builder
                .comment("Vertical radius scanned for nearby growth blocks.")
                .defineInRange("scan_vertical_radius", 1, 0, 32);
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

    // These accessors expose raw config inputs. Runtime gameplay should read EffectiveBalance instead.
    public static BalancePreset preset() {
        return BALANCE_PRESET.get();
    }

    public static boolean usesAdvancedConfigValues() {
        return USE_ADVANCED_CONFIG_VALUES.get();
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

    public static boolean overridesVanillaEnchantmentLimits() {
        return OVERRIDE_VANILLA_ENCHANTMENT_LIMITS.get();
    }

    public static boolean alwaysShowsRoutedEnchantmentOverlay() {
        return ALWAYS_SHOW_ROUTED_ENCHANTMENT_OVERLAY.get();
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

    public static int minEnchantingLevelCost() {
        return ENCHANTING_MIN_LEVEL_COST.get();
    }

    public static int maxEnchantingLevelCost() {
        return ENCHANTING_MAX_LEVEL_COST.get();
    }

    public static int bookshelfPowerPerLevelCost() {
        return ENCHANTING_BOOKSHELF_POWER_PER_LEVEL_COST.get();
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

    public static int resonanceHighlightDurationTicks() {
        return RESONANCE_HIGHLIGHT_DURATION_TICKS.get();
    }

    public static List<? extends String> resonanceBlockWhitelist() {
        return RESONANCE_BLOCK_WHITELIST.get();
    }

    public static List<? extends String> resonanceBlockBlacklist() {
        return RESONANCE_BLOCK_BLACKLIST.get();
    }

    public static int treeCapitatorMaxLogs() {
        return TREE_CAPITATOR_MAX_LOGS.get();
    }

    public static int treeCapitatorLeafScanRadius() {
        return TREE_CAPITATOR_LEAF_SCAN_RADIUS.get();
    }

    public static int treeCapitatorMinNaturalLeaves() {
        return TREE_CAPITATOR_MIN_NATURAL_LEAVES.get();
    }

    public static float perfectStrikeReadyThreshold() {
        return PERFECT_STRIKE_READY_THRESHOLD.get().floatValue();
    }

    public static int perfectStrikeWindowTicks() {
        return PERFECT_STRIKE_WINDOW_TICKS.get();
    }

    public static float perfectStrikeDamageMultiplier() {
        return PERFECT_STRIKE_DAMAGE_MULTIPLIER.get().floatValue();
    }

    public static double perfectStrikeMinCooldownVariance() {
        return PERFECT_STRIKE_MIN_COOLDOWN_VARIANCE.get();
    }

    public static double perfectStrikeMaxCooldownVariance() {
        return PERFECT_STRIKE_MAX_COOLDOWN_VARIANCE.get();
    }

    public static float shockedDamageMultiplier() {
        return SHOCKED_DAMAGE_MULTIPLIER.get().floatValue();
    }

    public static String shockedParticleType() {
        return SHOCKED_PARTICLE_TYPE.get();
    }

    public static boolean shockedParticlesEnabled() {
        return SHOCKED_PARTICLES_ENABLED.get();
    }

    public static int shockedParticleIntervalTicks() {
        return SHOCKED_PARTICLE_INTERVAL_TICKS.get();
    }

    public static int shockedParticleCount() {
        return SHOCKED_PARTICLE_COUNT.get();
    }

    public static double shockedParticleHorizontalSpread() {
        return SHOCKED_PARTICLE_HORIZONTAL_SPREAD.get();
    }

    public static double shockedParticleVerticalSpread() {
        return SHOCKED_PARTICLE_VERTICAL_SPREAD.get();
    }

    public static double shockedParticleSpeed() {
        return SHOCKED_PARTICLE_SPEED.get();
    }

    public static int shockingDurationTicks() {
        return SHOCKING_DURATION_TICKS.get();
    }

    public static int frostbiteFrostTicksPerLevel() {
        return FROSTBITE_FROST_TICKS_PER_LEVEL.get();
    }

    public static int frostbiteFrozenDurationTicks() {
        return FROSTBITE_FROZEN_DURATION_TICKS.get();
    }

    public static int cinderstepDurationTicks() {
        return CINDERSTEP_DURATION_TICKS.get();
    }

    public static double cinderstepSpeedBonusPerLevel() {
        return CINDERSTEP_SPEED_BONUS_PER_LEVEL.get();
    }

    public static int overchargedDurationTicks() {
        return OVERCHARGED_DURATION_TICKS.get();
    }

    public static int overchargedStrengthMaxAmplifier() {
        return OVERCHARGED_STRENGTH_MAX_AMPLIFIER.get();
    }

    public static int overchargedRegenerationMaxAmplifier() {
        return OVERCHARGED_REGENERATION_MAX_AMPLIFIER.get();
    }

    public static int overchargedSpeedMaxAmplifier() {
        return OVERCHARGED_SPEED_MAX_AMPLIFIER.get();
    }

    public static float beheadingBaseHeadDropChance() {
        return BEHEADING_BASE_HEAD_DROP_CHANCE.get().floatValue();
    }

    public static float beheadingHeadDropChancePerLootingLevel() {
        return BEHEADING_HEAD_DROP_CHANCE_PER_LOOTING_LEVEL.get().floatValue();
    }

    public static double beheadingHeadshotUpperEyeBand() {
        return BEHEADING_HEADSHOT_UPPER_EYE_BAND.get();
    }

    public static double beheadingHeadshotLowerEyeBand() {
        return BEHEADING_HEADSHOT_LOWER_EYE_BAND.get();
    }

    public static float headshotDamageBonusPerLevel() {
        return HEADSHOT_DAMAGE_BONUS_PER_LEVEL.get().floatValue();
    }

    public static double headshotUpperEyeBand() {
        return HEADSHOT_UPPER_EYE_BAND.get();
    }

    public static double headshotLowerEyeBand() {
        return HEADSHOT_LOWER_EYE_BAND.get();
    }

    public static float curseOfReboundReflectedDamageRatio() {
        return CURSE_OF_REBOUND_REFLECTED_DAMAGE_RATIO.get().floatValue();
    }

    public static float curseOfFragilityDurabilityDamageMultiplier() {
        return CURSE_OF_FRAGILITY_DURABILITY_DAMAGE_MULTIPLIER.get().floatValue();
    }

    public static float seismicCushionExplosionRadiusPerLevel() {
        return SEISMIC_CUSHION_EXPLOSION_RADIUS_PER_LEVEL.get().floatValue();
    }

    public static int verdantRegrowthBaseRepairIntervalTicks() {
        return VERDANT_REGROWTH_BASE_REPAIR_INTERVAL_TICKS.get();
    }

    public static int verdantRegrowthFastRepairIntervalTicks() {
        return VERDANT_REGROWTH_FAST_REPAIR_INTERVAL_TICKS.get();
    }

    public static int verdantRegrowthDurabilityRepairedPerLevel() {
        return VERDANT_REGROWTH_DURABILITY_REPAIRED_PER_LEVEL.get();
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

    public enum BalancePreset {
        VANILLA_PLUS,
        BALANCED,
        OVERHAUL,
        POWER_FANTASY,
        CUSTOM;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
