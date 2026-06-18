package com.betterenchanting.data;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.compat.SilentGearCompat;
import com.betterenchanting.registry.ModTags;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private static volatile Rules rules = Rules.defaults();

    private EnchantmentLimitRules() {
    }

    public static int maxEnchantments(ItemStack stack) {
        return rules.maxEnchantments(stack);
    }

    public static int baseMaxEnchantments(ItemStack stack) {
        return rules.baseMaxEnchantments(stack);
    }

    public static int currentEnchantmentCount(ItemStack stack) {
        Map<Holder<Enchantment>, Integer> enchantments = new HashMap<>();
        addEnchantments(enchantments, stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        addEnchantments(enchantments, stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        return enchantments.size();
    }

    public static int remainingCapacity(ItemStack stack) {
        return Math.max(0, maxEnchantments(stack) - currentEnchantmentCount(stack));
    }

    public static boolean canApplyAll(ItemStack target, Iterable<EnchantmentInstance> additions) {
        Map<Holder<Enchantment>, Integer> enchantments = new HashMap<>();
        addEnchantments(enchantments, target.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        addEnchantments(enchantments, target.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        for (EnchantmentInstance addition : additions) {
            enchantments.put(addition.enchantment, addition.level);
        }
        return enchantments.size() <= maxEnchantments(target);
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
            int bonus = 0;
            Set<ResourceLocation> virtualMaterialTags = Set.copyOf(SilentGearCompat.materialItemTags(stack));
            for (Map.Entry<ResourceLocation, Integer> entry : this.materialBonuses.entrySet()) {
                if (stack.is(TagKey.create(Registries.ITEM, entry.getKey()))
                        || virtualMaterialTags.contains(entry.getKey())) {
                    bonus += entry.getValue();
                }
            }
            return Math.max(0, baseLimit + bonus);
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
            if (stack.is(ModTags.Items.ARMOR) || stack.is(ModTags.Items.ARMOUR)) {
                limit = lowerTypeLimit(limit, ARMOR_KEYS);
            }
            if (stack.is(ModTags.Items.WEAPONS)) {
                limit = lowerTypeLimit(limit, WEAPON_KEYS);
            }
            if (stack.is(ModTags.Items.TOOLS)) {
                limit = lowerTypeLimit(limit, TOOL_KEYS);
            }
            return limit;
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
