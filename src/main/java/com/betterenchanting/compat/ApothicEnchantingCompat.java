package com.betterenchanting.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.fml.ModList;

public final class ApothicEnchantingCompat {
    private static final String MOD_ID = "apothic_enchanting";
    private static final String STATS_CLASS = "dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats";
    private static final String HELPER_CLASS = "dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentHelper";
    private static final String APOTHIC_CLASS = "dev.shadowsoffire.apothic_enchanting.ApothicEnchanting";
    private static final String INFO_CLASS = "dev.shadowsoffire.apothic_enchanting.EnchantmentInfo";
    private static final String ARCANA_CLASS = "dev.shadowsoffire.apothic_enchanting.table.Arcana";
    private static final String RECIPE_TYPES_CLASS = "dev.shadowsoffire.apothic_enchanting.Ench$RecipeTypes";
    private static final String INFUSION_RECIPE_CLASS = "dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe";

    private static boolean initialized;
    private static boolean available;
    private static boolean infusionAvailable;
    private static Method gatherStats;
    private static Method eterna;
    private static Method quanta;
    private static Method arcana;
    private static Method clues;
    private static Method blacklist;
    private static Method treasure;
    private static Method stable;
    private static Method getEnchantmentCost;
    private static Method getQuantaFactor;
    private static Method getEnchInfo;
    private static Method getMaxLevel;
    private static Method getMinPower;
    private static Method getMaxPower;
    private static Method getArcanaForThreshold;
    private static Method adjustArcanaWeight;
    private static Method findInfusionMatch;
    private static Method findInfusionItemMatch;
    private static Method assembleInfusionRecipe;
    private static Object infusionRecipeType;

    private ApothicEnchantingCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean shouldLetApothicHandleVanillaTable() {
        return isLoaded();
    }

    public static Optional<TableStats> gatherTableStats(Level level, BlockPos pos, ItemStack target) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            int enchantability = target.getItem().getEnchantmentValue(target);
            Object stats = gatherStats.invoke(null, level, pos, enchantability);
            return Optional.of(new TableStats(
                    floatValue(eterna.invoke(stats)),
                    floatValue(quanta.invoke(stats)),
                    floatValue(arcana.invoke(stats)),
                    intValue(clues.invoke(stats)),
                    enchantmentSet(blacklist.invoke(stats)),
                    booleanValue(treasure.invoke(stats)),
                    booleanValue(stable.invoke(stats))
            ));
        } catch (ReflectiveOperationException | RuntimeException error) {
            return Optional.empty();
        }
    }

    public static int offerRequirement(TableStats stats, int option, int enchantmentSeed, ItemStack target) {
        if (!isAvailable()) {
            return fallbackOfferRequirement(stats, option, enchantmentSeed);
        }
        try {
            RandomSource random = RandomSource.create();
            random.setSeed(enchantmentSeed);
            int requirement = 0;
            for (int index = 0; index <= option; index++) {
                requirement = normalizeOfferRequirement(
                        intValue(getEnchantmentCost.invoke(null, random, index, stats.effectiveEterna(), target)),
                        index
                );
            }
            return Math.max(0, requirement);
        } catch (ReflectiveOperationException | RuntimeException error) {
            return fallbackOfferRequirement(stats, option, enchantmentSeed);
        }
    }

    public static int adjustRollPower(Optional<TableStats> stats, int option, int enchantmentSeed, int basePower) {
        if (stats.isEmpty()) {
            return basePower;
        }
        return adjustRollPower(stats.get(), option, enchantmentSeed, basePower);
    }

    public static int adjustRollPower(TableStats stats, int option, int enchantmentSeed, int basePower) {
        if (basePower <= 0) {
            return 0;
        }
        RandomSource random = RandomSource.create();
        random.setSeed((long) enchantmentSeed + option);
        float factor = quantaFactor(random, stats.quanta(), stats.stable());
        return Mth.clamp(Math.round(basePower * factor), 1, 200);
    }

    public static OptionalInt bestLevelForPower(Holder<Enchantment> enchantment, int power) {
        if (!isAvailable()) {
            return OptionalInt.empty();
        }
        try {
            Object info = getEnchInfo.invoke(null, enchantment);
            int maxLevel = intValue(getMaxLevel.invoke(info));
            int minLevel = enchantment.value().getMinLevel();
            for (int level = maxLevel; level >= minLevel; level--) {
                int minPower = intValue(getMinPower.invoke(info, level));
                int maxPower = intValue(getMaxPower.invoke(info, level));
                if (power >= minPower && power <= maxPower) {
                    return OptionalInt.of(level);
                }
            }
            return OptionalInt.of(0);
        } catch (InvocationTargetException error) {
            return OptionalInt.empty();
        } catch (ReflectiveOperationException | RuntimeException error) {
            return OptionalInt.empty();
        }
    }

    public static OptionalInt maxLevel(Holder<Enchantment> enchantment) {
        if (!isAvailable()) {
            return OptionalInt.empty();
        }
        try {
            Object info = getEnchInfo.invoke(null, enchantment);
            return OptionalInt.of(intValue(getMaxLevel.invoke(info)));
        } catch (InvocationTargetException error) {
            return OptionalInt.empty();
        } catch (ReflectiveOperationException | RuntimeException error) {
            return OptionalInt.empty();
        }
    }

    public static int adjustedWeight(Holder<Enchantment> enchantment, TableStats stats) {
        int baseWeight = Math.max(1, enchantment.value().getWeight());
        if (!isAvailable()) {
            return baseWeight;
        }
        try {
            Object arcanaTier = getArcanaForThreshold.invoke(null, stats.arcana());
            return Math.max(1, intValue(adjustArcanaWeight.invoke(arcanaTier, baseWeight)));
        } catch (ReflectiveOperationException | RuntimeException error) {
            return baseWeight;
        }
    }

    public static int guaranteedSelectionCount(TableStats stats) {
        int count = 1;
        if (stats.arcana() >= 33F) {
            count++;
        }
        if (stats.arcana() >= 66F) {
            count++;
        }
        if (stats.arcana() >= 99F) {
            count++;
        }
        return count;
    }

    public static boolean hasInfusionItemMatch(Level level, ItemStack target) {
        if (!isInfusionAvailable() || target.isEmpty()) {
            return false;
        }
        try {
            return findInfusionItemMatch.invoke(null, level, target) != null;
        } catch (ReflectiveOperationException | RuntimeException error) {
            return false;
        }
    }

    public static Optional<ItemStack> assembleInfusion(Level level, ItemStack target, TableStats stats) {
        return findInfusion(level, target, stats).map(match -> match.result().copy());
    }

    public static Optional<InfusionMatch> findInfusion(Level level, ItemStack target, TableStats stats) {
        if (!isInfusionAvailable() || target.isEmpty()) {
            return Optional.empty();
        }
        try {
            Object recipe = findInfusionMatch.invoke(null, level, target, stats.eterna(), stats.quanta(), stats.arcana());
            if (recipe == null) {
                return Optional.empty();
            }
            Object result = assembleInfusionRecipe.invoke(recipe, target, stats.eterna(), stats.quanta(), stats.arcana());
            if (result instanceof ItemStack stack && !stack.isEmpty()) {
                return Optional.of(new InfusionMatch(infusionRecipeId(level, recipe), stack.copy()));
            }
            return Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException error) {
            return Optional.empty();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Optional<ResourceLocation> infusionRecipeId(Level level, Object recipe) {
        if (infusionRecipeType == null) {
            return Optional.empty();
        }

        try {
            for (Object holderObject : level.getRecipeManager().getAllRecipesFor((RecipeType) infusionRecipeType)) {
                if (holderObject instanceof RecipeHolder<?> holder && holder.value() == recipe) {
                    return Optional.of(holder.id());
                }
            }
        } catch (RuntimeException error) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static boolean isAvailable() {
        if (!isLoaded()) {
            return false;
        }
        init();
        return available;
    }

    private static boolean isInfusionAvailable() {
        if (!isAvailable()) {
            return false;
        }
        return infusionAvailable;
    }

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> statsClass = Class.forName(STATS_CLASS);
            gatherStats = statsClass.getMethod("gatherStats", LevelReader.class, BlockPos.class, int.class);
            eterna = statsClass.getMethod("eterna");
            quanta = statsClass.getMethod("quanta");
            arcana = statsClass.getMethod("arcana");
            clues = statsClass.getMethod("clues");
            blacklist = statsClass.getMethod("blacklist");
            treasure = statsClass.getMethod("treasure");
            stable = statsClass.getMethod("stable");

            Class<?> helperClass = Class.forName(HELPER_CLASS);
            getEnchantmentCost = helperClass.getMethod("getEnchantmentCost", RandomSource.class, int.class, float.class, ItemStack.class);
            getQuantaFactor = helperClass.getMethod("getQuantaFactor", RandomSource.class, float.class, boolean.class);

            Class<?> apothicClass = Class.forName(APOTHIC_CLASS);
            getEnchInfo = apothicClass.getMethod("getEnchInfo", Holder.class);

            Class<?> infoClass = Class.forName(INFO_CLASS);
            getMaxLevel = infoClass.getMethod("getMaxLevel");
            getMinPower = infoClass.getMethod("getMinPower", int.class);
            getMaxPower = infoClass.getMethod("getMaxPower", int.class);

            Class<?> arcanaClass = Class.forName(ARCANA_CLASS);
            getArcanaForThreshold = arcanaClass.getMethod("getForThreshold", float.class);
            adjustArcanaWeight = arcanaClass.getMethod("adjustWeight", int.class);
            available = true;
            initInfusion();
        } catch (ReflectiveOperationException | RuntimeException error) {
            available = false;
            infusionAvailable = false;
        }
    }

    private static void initInfusion() {
        try {
            Class<?> recipeTypesClass = Class.forName(RECIPE_TYPES_CLASS);
            Field infusionRecipeTypeField = recipeTypesClass.getField("INFUSION");
            infusionRecipeType = infusionRecipeTypeField.get(null);

            Class<?> infusionRecipeClass = Class.forName(INFUSION_RECIPE_CLASS);
            findInfusionMatch = infusionRecipeClass.getMethod("findMatch", Level.class, ItemStack.class, float.class, float.class, float.class);
            findInfusionItemMatch = infusionRecipeClass.getMethod("findItemMatch", Level.class, ItemStack.class);
            assembleInfusionRecipe = infusionRecipeClass.getMethod("assemble", ItemStack.class, float.class, float.class, float.class);
            infusionAvailable = true;
        } catch (ReflectiveOperationException | RuntimeException error) {
            infusionRecipeType = null;
            infusionAvailable = false;
        }
    }

    public record InfusionMatch(Optional<ResourceLocation> recipeId, ItemStack result) {
        public InfusionMatch {
            result = result.copy();
        }
    }

    private static int fallbackOfferRequirement(TableStats stats, int option, int enchantmentSeed) {
        RandomSource random = RandomSource.create();
        random.setSeed(enchantmentSeed);
        int requirement = 0;
        for (int index = 0; index <= option; index++) {
            requirement = normalizeOfferRequirement(fallbackEnchantmentCost(random, index, stats.effectiveEterna()), index);
        }
        return requirement;
    }

    private static int fallbackEnchantmentCost(RandomSource random, int option, float eterna) {
        int level = Math.round(eterna);
        if (option == 2) {
            return level;
        }
        float lowBound = 0.6F - 0.4F * (1 - option);
        float highBound = 0.8F - 0.4F * (1 - option);
        return Math.max(1, Math.round(level * Mth.nextFloat(random, lowBound, highBound)));
    }

    private static int normalizeOfferRequirement(int requirement, int option) {
        return requirement < option + 1 ? requirement + 1 : requirement;
    }

    private static float quantaFactor(RandomSource random, float quanta, boolean stable) {
        if (isAvailable()) {
            try {
                return floatValue(getQuantaFactor.invoke(null, random, quanta, stable));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Fall through to the local equivalent.
            }
        }
        if (stable) {
            return 1 + quanta * random.nextFloat() / 100F;
        }
        float gaussian = (float) random.nextGaussian();
        float factor = Mth.clamp(gaussian / 3F, -1F, 1F);
        return 1 + quanta * factor / 100F;
    }

    private static float floatValue(Object value) {
        return value instanceof Number number ? number.floatValue() : 0F;
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    @SuppressWarnings("unchecked")
    private static Set<Holder<Enchantment>> enchantmentSet(Object value) {
        if (value instanceof Set<?> set) {
            return Set.copyOf((Set<Holder<Enchantment>>) set);
        }
        return Set.of();
    }

    public record TableStats(
            float eterna,
            float quanta,
            float arcana,
            int clues,
            Set<Holder<Enchantment>> blacklist,
            boolean treasure,
            boolean stable
    ) {
        public float effectiveEterna() {
            return Math.max(1.5F, this.eterna);
        }

        public int bookshelfPower() {
            return Math.max(0, Math.round(effectiveEterna()));
        }

        public boolean isBlacklisted(Holder<Enchantment> enchantment) {
            return this.blacklist.contains(enchantment);
        }
    }
}
