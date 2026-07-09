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
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class EnchantmentGuideEntries {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile Map<ResourceLocation, Entry> entries = Map.of();

    private EnchantmentGuideEntries() {
    }

    public static Entry get(ResourceLocation enchantmentId) {
        return entries.getOrDefault(enchantmentId, Entry.EMPTY);
    }

    private static void readEntry(ResourceLocation fallbackId, JsonObject object, Map<ResourceLocation, Entry> output) {
        ResourceLocation enchantmentId = optionalString(object, "enchantment")
                .map(EnchantmentGuideEntries::parseId)
                .orElse(fallbackId);
        output.put(enchantmentId, new Entry(
                optionalString(object, "summary"),
                optionalString(object, "summary_key"),
                readStringList(object, "notes"),
                GsonHelper.getAsBoolean(object, "hidden", false)
        ));
    }

    private static List<String> readStringList(JsonObject object, String key) {
        if (!object.has(key)) {
            return List.of();
        }
        JsonArray array = GsonHelper.getAsJsonArray(object, key);
        List<String> values = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            values.add(GsonHelper.convertToString(element, key + " entry"));
        }
        return List.copyOf(values);
    }

    private static Optional<String> optionalString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(object, key))
                : Optional.empty();
    }

    private static ResourceLocation parseId(String value) {
        return value.indexOf(':') >= 0 ? ResourceLocation.parse(value) : BetterEnchanting.id(value);
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/enchantment_guide");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, Entry> loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> resource : resources.entrySet()) {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(resource.getValue(), "enchantment guide");
                    if (object.has("entries")) {
                        JsonArray array = GsonHelper.getAsJsonArray(object, "entries");
                        for (JsonElement element : array) {
                            readEntry(resource.getKey(), GsonHelper.convertToJsonObject(element, "enchantment guide entry"), loaded);
                        }
                    } else {
                        readEntry(resource.getKey(), object, loaded);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid enchantment guide entry {}", resource.getKey(), exception);
                }
            }
            entries = Map.copyOf(loaded);
            LOGGER.info("Loaded {} Better Enchanting JEI guide entries", entries.size());
        }
    }

    public record Entry(
            Optional<String> summary,
            Optional<String> summaryKey,
            List<String> notes,
            boolean hidden
    ) {
        private static final Entry EMPTY = new Entry(Optional.empty(), Optional.empty(), List.of(), false);

        public Entry {
            summary = summary == null ? Optional.empty() : summary;
            summaryKey = summaryKey == null ? Optional.empty() : summaryKey;
            notes = notes == null ? List.of() : List.copyOf(notes);
        }
    }
}
