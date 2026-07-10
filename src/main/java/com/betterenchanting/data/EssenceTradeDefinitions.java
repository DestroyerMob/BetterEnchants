package com.betterenchanting.data;

import com.betterenchanting.BetterEnchanting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

public final class EssenceTradeDefinitions {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile Map<ResourceLocation, List<EssenceTrade>> tradesByProfession = Map.of();

    private EssenceTradeDefinitions() {
    }

    public static List<EssenceTrade> tradesFor(VillagerProfession profession) {
        ResourceLocation professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        return tradesByProfession.getOrDefault(professionId, List.of());
    }

    private static EssenceTrade parseTrade(JsonObject object) {
        ResourceLocation professionId = parseId(GsonHelper.getAsString(object, "profession"), "minecraft");
        String itemValue = object.has("item")
                ? GsonHelper.getAsString(object, "item")
                : GsonHelper.getAsString(object, "essence");
        ResourceLocation itemId = parseId(itemValue, BetterEnchanting.MOD_ID);
        if (BuiltInRegistries.VILLAGER_PROFESSION.getOptional(professionId).isEmpty()) {
            throw new IllegalArgumentException("Unknown villager profession: " + professionId);
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown villager trade item: " + itemId));

        return new EssenceTrade(
                professionId,
                item,
                GsonHelper.getAsInt(object, "level", 1),
                GsonHelper.getAsInt(object, "emerald_cost", 1),
                GsonHelper.getAsInt(object, "count", 1),
                GsonHelper.getAsInt(object, "max_uses", 8),
                GsonHelper.getAsInt(object, "xp", 1),
                GsonHelper.getAsFloat(object, "price_multiplier", 0.05F)
        );
    }

    private static ResourceLocation parseId(String value, String defaultNamespace) {
        return value.indexOf(':') >= 0
                ? ResourceLocation.parse(value)
                : ResourceLocation.fromNamespaceAndPath(defaultNamespace, value);
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/essence_trades");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, List<EssenceTrade>> loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), "essence trades");
                    JsonArray trades = GsonHelper.getAsJsonArray(object, "trades");
                    for (JsonElement tradeElement : trades) {
                        EssenceTrade trade = parseTrade(GsonHelper.convertToJsonObject(tradeElement, "essence trade"));
                        loaded.computeIfAbsent(trade.profession(), ignored -> new ArrayList<>()).add(trade);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid essence trade file {}", entry.getKey(), exception);
                }
            }

            Map<ResourceLocation, List<EssenceTrade>> immutable = new LinkedHashMap<>();
            loaded.forEach((profession, trades) -> immutable.put(profession, List.copyOf(trades)));
            tradesByProfession = Map.copyOf(immutable);
            LOGGER.info("Loaded {} Better Enchanting essence trade profession group(s)", tradesByProfession.size());
        }
    }

    public record EssenceTrade(
            ResourceLocation profession,
            Item item,
            int level,
            int emeraldCost,
            int count,
            int maxUses,
            int xp,
            float priceMultiplier
    ) {
    }
}
