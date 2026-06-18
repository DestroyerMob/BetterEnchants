package com.betterenchanting.world;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.compat.SilentGearCompat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;

public final class EnchantmentTargetTags {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile List<TargetTagRule> targetTagRules = List.of();

    private EnchantmentTargetTags() {
    }

    static List<ResourceLocation> resolve(ItemStack target) {
        if (target.isEmpty() || isBookTarget(target)) {
            return List.of();
        }

        Set<ResourceLocation> tags = new LinkedHashSet<>();
        List<ResourceLocation> virtualItemTags = SilentGearCompat.materialItemTags(target);
        for (TargetTagRule rule : targetTagRules) {
            if (target.is(rule.itemTag()) || virtualItemTags.contains(rule.itemTag().location())) {
                tags.add(rule.enchantmentTag().location());
            }
        }
        tags.addAll(SilentGearCompat.materialTargetTags(target));
        return List.copyOf(tags);
    }

    private static boolean isBookTarget(ItemStack target) {
        return target.is(Items.BOOK) || target.is(Items.ENCHANTED_BOOK);
    }

    private static TargetTagRule parseRule(JsonObject object) {
        ResourceLocation itemTag = parseTagId(GsonHelper.getAsString(object, "item_tag"));
        ResourceLocation enchantmentTag = parseTagId(GsonHelper.getAsString(object, "enchantment_tag"));
        return new TargetTagRule(
                TagKey.create(Registries.ITEM, itemTag),
                TagKey.create(Registries.ENCHANTMENT, enchantmentTag)
        );
    }

    private static ResourceLocation parseTagId(String value) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.indexOf(':') >= 0) {
            return ResourceLocation.parse(normalized);
        }
        return BetterEnchanting.id(normalized);
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/enchantment_targets");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, TargetTagRule> loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), "enchantment target rules");
                    JsonArray ruleArray = GsonHelper.getAsJsonArray(object, "rules");
                    for (JsonElement ruleElement : ruleArray) {
                        TargetTagRule rule = parseRule(GsonHelper.convertToJsonObject(ruleElement, "enchantment target rule"));
                        loaded.put(rule.itemTag().location(), rule);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid enchantment target rules {}", entry.getKey(), exception);
                }
            }
            targetTagRules = List.copyOf(loaded.values());
            LOGGER.info("Loaded {} Better Enchanting target tag rule(s)", targetTagRules.size());
        }
    }

    private record TargetTagRule(TagKey<Item> itemTag, TagKey<Enchantment> enchantmentTag) {
    }
}
