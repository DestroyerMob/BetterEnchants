package com.betterenchanting.data;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.compat.ModularMaterialCompat;
import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModTags;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;

public final class EnchantmentLimitRules {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_GLOBAL_MAX = 6;
    private static final int TAG_MAX = 2;
    private static volatile Rules rules = Rules.defaults();

    private EnchantmentLimitRules() {
    }

    public static int maxEnchantments(ItemStack stack) {
        if (!overridesVanillaLimits()) {
            return Integer.MAX_VALUE;
        }
        OptionalInt routedLimit = ModularMaterialCompat.routedMaxEnchantments(stack);
        if (routedLimit.isPresent()) {
            return routedLimit.getAsInt();
        }
        return rules.maxEnchantments(stack);
    }

    public static int baseMaxEnchantments(ItemStack stack) {
        if (!overridesVanillaLimits()) {
            return Integer.MAX_VALUE;
        }
        OptionalInt routedLimit = ModularMaterialCompat.routedMaxEnchantments(stack);
        if (routedLimit.isPresent()) {
            return routedLimit.getAsInt();
        }
        return rules.baseMaxEnchantments(stack);
    }

    public static int materialCapacityBonus(ItemStack stack) {
        if (!overridesVanillaLimits()) {
            return 0;
        }
        return rules.materialBonus(stack);
    }

    public static int currentEnchantmentCount(ItemStack stack) {
        Map<Holder<Enchantment>, Integer> enchantments = new HashMap<>();
        addEnchantments(enchantments, stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        addEnchantments(enchantments, stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        return enchantments.size();
    }

    public static int remainingCapacity(ItemStack stack) {
        if (!overridesVanillaLimits()) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, maxEnchantments(stack) - currentEnchantmentCount(stack));
    }

    public static boolean canApplyAll(ItemStack target, Iterable<EnchantmentInstance> additions) {
        if (ModularMaterialCompat.hasRoutedParts(target)) {
            java.util.ArrayList<Holder<Enchantment>> enchantments = new java.util.ArrayList<>();
            for (EnchantmentInstance addition : additions) {
                enchantments.add(addition.enchantment);
            }
            return ModularMaterialCompat.canApplyRoutedEnchantments(null, target, enchantments);
        }

        Map<Holder<Enchantment>, Integer> enchantments = new HashMap<>();
        addEnchantments(enchantments, target.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        addEnchantments(enchantments, target.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        for (EnchantmentInstance addition : additions) {
            enchantments.put(addition.enchantment, addition.level);
        }
        return canFitAll(target, enchantments.keySet());
    }

    public static boolean isWithinLimits(ItemStack stack) {
        Map<Holder<Enchantment>, Integer> enchantments = new HashMap<>();
        addEnchantments(enchantments, stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        addEnchantments(enchantments, stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        return canFitAll(stack, enchantments.keySet());
    }

    public static boolean canFitAll(ItemStack target, Set<Holder<Enchantment>> enchantments) {
        boolean fitsTotalLimit = !overridesVanillaLimits() || enchantments.size() <= maxEnchantments(target);
        return fitsTotalLimit && fitsPrimaryTagLimits(enchantments);
    }

    /**
     * Checks category limits without applying an item's total enchantment capacity.
     * Routed modular tools use this per part while assigning new enchantments; the
     * assembled tool may intentionally exceed a category limit until it is tuned.
     */
    public static boolean fitsPrimaryTagLimits(Iterable<Holder<Enchantment>> enchantments) {
        if (!overridesVanillaLimits()) {
            return true;
        }
        Map<ResourceLocation, Integer> tagCounts = new HashMap<>();
        for (Holder<Enchantment> enchantment : enchantments) {
            for (ResourceLocation tagId : limitedTagIds(enchantment)) {
                int count = tagCounts.merge(tagId, 1, Integer::sum);
                if (count > TAG_MAX) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isOverTagLimit(Holder<Enchantment> target, Iterable<Holder<Enchantment>> orderedEnchantments) {
        Map<ResourceLocation, Integer> tagCounts = new HashMap<>();
        for (Holder<Enchantment> enchantment : orderedEnchantments) {
            Set<ResourceLocation> tagIds = limitedTagIds(enchantment);
            boolean targetReached = enchantment.equals(target);
            boolean overLimit = false;
            for (ResourceLocation tagId : tagIds) {
                int count = tagCounts.getOrDefault(tagId, 0);
                if (targetReached && count >= TAG_MAX) {
                    overLimit = true;
                }
            }
            for (ResourceLocation tagId : tagIds) {
                tagCounts.merge(tagId, 1, Integer::sum);
            }
            if (targetReached) {
                return overLimit;
            }
        }
        return false;
    }

    public static boolean overridesVanillaLimits() {
        return EffectiveBalance.overridesVanillaEnchantmentLimits();
    }

    private static Set<ResourceLocation> limitedTagIds(Holder<Enchantment> enchantment) {
        Set<ResourceLocation> primaryAffinities = AffinityRoles.primaryAffinities(enchantment);
        if (!primaryAffinities.isEmpty()) {
            return primaryAffinities;
        }

        return enchantment.tags()
                .map(TagKey::location)
                .filter(EnchantmentLimitRules::isLimitedEnchantmentTag)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static boolean isLimitedEnchantmentTag(ResourceLocation tagId) {
        if (!tagId.getNamespace().equals(BetterEnchanting.MOD_ID)) {
            return false;
        }

        String path = tagId.getPath();
        return !path.startsWith("targets/")
                && !path.startsWith("exclusive_set/")
                && !AffinityRoles.isClassificationTag(tagId);
    }

    private static void addEnchantments(Map<Holder<Enchantment>, Integer> enchantments, ItemEnchantments source) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : source.entrySet()) {
            enchantments.put(entry.getKey(), entry.getIntValue());
        }
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/enchantment_limits");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            Rules loaded = Rules.defaults();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    loaded = loaded.merge(GsonHelper.convertToJsonObject(entry.getValue(), "enchantment limit rules"));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid enchantment limit rules {}", entry.getKey(), exception);
                }
            }
            rules = loaded;
            LOGGER.info("Loaded Better Enchanting enchantment limit rules from {} file(s)", resources.size());
        }
    }

    private record Rules(
            int globalMax,
            Map<String, Integer> typeLimits,
            Map<ResourceLocation, Integer> itemLimits,
            Map<ResourceLocation, Integer> materialBonuses
    ) {
        private static final Set<String> ARMOR_KEYS = Set.of("armor", "armour");
        private static final Set<String> WEAPON_KEYS = Set.of("weapon", "weapons");
        private static final Set<String> TOOL_KEYS = Set.of("tool", "tools");

        private static Rules defaults() {
            Map<String, Integer> typeLimits = new LinkedHashMap<>();
            typeLimits.put("armor", 4);
            typeLimits.put("weapon", 4);
            typeLimits.put("tool", 3);

            Map<ResourceLocation, Integer> materialBonuses = new LinkedHashMap<>();
            materialBonuses.put(BetterEnchanting.id("materials/gold"), 1);
            return new Rules(DEFAULT_GLOBAL_MAX, Map.copyOf(typeLimits), Map.of(), Map.copyOf(materialBonuses));
        }

        private Rules merge(JsonObject object) {
            int mergedGlobalMax = GsonHelper.getAsInt(object, "global_max", this.globalMax);
            Map<String, Integer> mergedTypeLimits = new LinkedHashMap<>(this.typeLimits);
            Map<ResourceLocation, Integer> mergedItemLimits = new LinkedHashMap<>(this.itemLimits);
            Map<ResourceLocation, Integer> mergedMaterialBonuses = new LinkedHashMap<>(this.materialBonuses);

            if (object.has("type_limits")) {
                readStringIntMap(GsonHelper.getAsJsonObject(object, "type_limits")).forEach(mergedTypeLimits::put);
            }
            if (object.has("item_limits")) {
                readItemLimitMap(GsonHelper.getAsJsonObject(object, "item_limits")).forEach(mergedItemLimits::put);
            }
            if (object.has("material_bonus")) {
                readMaterialBonusMap(GsonHelper.getAsJsonObject(object, "material_bonus")).forEach(mergedMaterialBonuses::put);
            }

            return new Rules(
                    Math.max(0, mergedGlobalMax),
                    Map.copyOf(mergedTypeLimits),
                    Map.copyOf(mergedItemLimits),
                    Map.copyOf(mergedMaterialBonuses)
            );
        }

        private int maxEnchantments(ItemStack stack) {
            int baseLimit = baseMaxEnchantments(stack);
            return Math.max(0, baseLimit + materialBonus(stack));
        }

        private int materialBonus(ItemStack stack) {
            int bonus = 0;
            Map<ResourceLocation, Integer> virtualMaterialTagCounts = ModularMaterialCompat.materialItemTagCounts(stack);
            for (Map.Entry<ResourceLocation, Integer> entry : this.materialBonuses.entrySet()) {
                if (stack.is(TagKey.create(Registries.ITEM, entry.getKey()))) {
                    bonus += entry.getValue();
                }
                int virtualCount = virtualMaterialTagCounts.getOrDefault(entry.getKey(), 0);
                if (virtualCount > 0) {
                    bonus += entry.getValue() * virtualCount;
                }
            }
            return bonus;
        }

        private int baseMaxEnchantments(ItemStack stack) {
            if (stack.isEmpty()) {
                return this.globalMax;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return Math.max(0, this.itemLimits.getOrDefault(itemId, baseLimitForType(stack)));
        }

        private int baseLimitForType(ItemStack stack) {
            int limit = this.globalMax;
            Set<ResourceLocation> virtualItemTags = Set.copyOf(ModularMaterialCompat.materialItemTags(stack));
            if (matches(stack, virtualItemTags, ModTags.Items.ARMOR) || matches(stack, virtualItemTags, ModTags.Items.ARMOUR)) {
                limit = lowerTypeLimit(limit, ARMOR_KEYS);
            }
            if (matches(stack, virtualItemTags, ModTags.Items.WEAPONS)) {
                limit = lowerTypeLimit(limit, WEAPON_KEYS);
            }
            if (matches(stack, virtualItemTags, ModTags.Items.TOOLS)) {
                limit = lowerTypeLimit(limit, TOOL_KEYS);
            }
            return limit;
        }

        private static boolean matches(ItemStack stack, Set<ResourceLocation> virtualItemTags, TagKey<net.minecraft.world.item.Item> tag) {
            return stack.is(tag) || virtualItemTags.contains(tag.location());
        }

        private int lowerTypeLimit(int current, Set<String> aliases) {
            int limit = current;
            for (String alias : aliases) {
                Integer configured = this.typeLimits.get(alias);
                if (configured != null) {
                    limit = Math.min(limit, configured);
                }
            }
            return Math.max(0, limit);
        }

        private static Map<String, Integer> readStringIntMap(JsonObject object) {
            Map<String, Integer> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                values.put(entry.getKey(), Math.max(0, GsonHelper.convertToInt(entry.getValue(), entry.getKey())));
            }
            return values;
        }

        private static Map<ResourceLocation, Integer> readItemLimitMap(JsonObject object) {
            Map<ResourceLocation, Integer> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                values.put(ResourceLocation.parse(entry.getKey()), Math.max(0, GsonHelper.convertToInt(entry.getValue(), entry.getKey())));
            }
            return values;
        }

        private static Map<ResourceLocation, Integer> readMaterialBonusMap(JsonObject object) {
            Map<ResourceLocation, Integer> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                values.put(resolveMaterialId(entry.getKey()), GsonHelper.convertToInt(entry.getValue(), entry.getKey()));
            }
            return values;
        }

        private static ResourceLocation resolveMaterialId(String key) {
            if (key.indexOf(':') >= 0) {
                return ResourceLocation.parse(key);
            }
            if (key.indexOf('/') >= 0) {
                return BetterEnchanting.id(key);
            }
            return BetterEnchanting.id("materials/" + key);
        }
    }
}
