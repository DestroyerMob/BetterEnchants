package com.betterenchanting.world;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.compat.ModularMaterialCompat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashSet;
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
    private static volatile List<TargetTagRule> targetTagRules = defaults();

    private EnchantmentTargetTags() {
    }

    public static List<ResourceLocation> resolve(ItemStack target) {
        return resolve(target, true);
    }

    public static List<ResourceLocation> resolveForActivation(ItemStack target) {
        return resolve(target, false);
    }

    public static List<ResourceLocation> resolveForRouting(ItemStack target) {
        return resolve(target, false);
    }

    private static List<ResourceLocation> resolve(ItemStack target, boolean directTarget) {
        if (target.isEmpty() || isBookTarget(target)) {
            return List.of();
        }
        if (directTarget) {
            List<ResourceLocation> routedTags = ModularMaterialCompat.routedTargetTags(target);
            if (!routedTags.isEmpty()) {
                return routedTags;
            }
        }
        if (directTarget && ModularMaterialCompat.blocksFinishedToolEnchanting(target)) {
            return List.of();
        }

        Set<ResourceLocation> tags = new LinkedHashSet<>();
        List<ResourceLocation> virtualItemTags = ModularMaterialCompat.materialItemTags(target);
        for (TargetTagRule rule : targetTagRules) {
            if (target.is(rule.itemTag()) || virtualItemTags.contains(rule.itemTag().location())) {
                tags.add(rule.enchantmentTag().location());
            }
        }
        tags.addAll(ModularMaterialCompat.materialTargetTags(target));
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

    private static List<TargetTagRule> defaults() {
        return List.of(
                rule("durability", "targets/durability"),
                rule("armor", "targets/armor"),
                rule("armor/helmets", "targets/armor/helmets"),
                rule("armor/body_armor", "targets/armor/body_armor"),
                rule("armor/leggings", "targets/armor/leggings"),
                rule("armor/boots", "targets/armor/boots"),
                rule("tools", "targets/tools"),
                rule("harvestable", "targets/tools/harvesters"),
                rule("harvesters", "targets/tools/harvesters"),
                rule("tools/harvesters", "targets/tools/harvesters"),
                rule("tools/pickaxes", "targets/tools/pickaxes"),
                rule("tools/axes", "targets/tools/axes"),
                rule("tools/shovels", "targets/tools/shovels"),
                rule("tools/hoes", "targets/tools/hoes"),
                rule("tools/shears", "targets/tools/shears"),
                rule("tools/fishing_rods", "targets/tools/fishing_rods"),
                rule("tools/brushes", "targets/tools/brushes"),
                rule("tools/flint_and_steel", "targets/tools/flint_and_steel"),
                rule("weapons", "targets/weapons"),
                rule("weapons/melee", "targets/weapons/melee"),
                rule("weapons/ranged", "targets/weapons/ranged"),
                rule("weapons/swords", "targets/weapons/swords"),
                rule("weapons/maces", "targets/weapons/maces"),
                rule("weapons/bows", "targets/weapons/bows"),
                rule("weapons/crossbows", "targets/weapons/crossbows"),
                rule("weapons/tridents", "targets/weapons/tridents"),
                rule("materials/wood", "targets/wood")
        );
    }

    private static TargetTagRule rule(String itemTag, String enchantmentTag) {
        return new TargetTagRule(
                TagKey.create(Registries.ITEM, BetterEnchanting.id(itemTag)),
                TagKey.create(Registries.ENCHANTMENT, BetterEnchanting.id(enchantmentTag))
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
            Set<TargetTagRule> loaded = new LinkedHashSet<>(defaults());
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), "enchantment target rules");
                    JsonArray ruleArray = GsonHelper.getAsJsonArray(object, "rules");
                    for (JsonElement ruleElement : ruleArray) {
                        TargetTagRule rule = parseRule(GsonHelper.convertToJsonObject(ruleElement, "enchantment target rule"));
                        loaded.add(rule);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid enchantment target rules {}", entry.getKey(), exception);
                }
            }
            targetTagRules = List.copyOf(loaded);
            LOGGER.info("Loaded {} Better Enchanting target tag rule(s) from {} file(s)", targetTagRules.size(), resources.size());
        }
    }

    private record TargetTagRule(TagKey<Item> itemTag, TagKey<Enchantment> enchantmentTag) {
    }
}
