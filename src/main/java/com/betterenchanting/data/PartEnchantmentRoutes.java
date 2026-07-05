package com.betterenchanting.data;

import com.betterenchanting.BetterEnchanting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class PartEnchantmentRoutes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile List<Route> routes = List.of();

    private PartEnchantmentRoutes() {
    }

    public static Optional<Route> routeFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        for (Route route : routes) {
            if (route.matches(stack)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    private static Route parseRoute(JsonObject object) {
        Optional<ResourceLocation> item = optionalLocation(object, "item")
                .or(() -> optionalLocation(object, "tool"));
        Optional<TagKey<Item>> itemTag = optionalLocation(object, "item_tag")
                .or(() -> optionalLocation(object, "tool_tag"))
                .map(id -> TagKey.create(Registries.ITEM, id));
        JsonArray slots = GsonHelper.getAsJsonArray(object, object.has("slots") ? "slots" : "parts");
        List<SlotRule> slotRules = new ArrayList<>();
        for (JsonElement element : slots) {
            slotRules.add(parseSlot(GsonHelper.convertToJsonObject(element, "part enchantment route slot")));
        }
        if (item.isEmpty() && itemTag.isEmpty()) {
            throw new IllegalArgumentException("Part enchantment route needs an item, tool, item_tag, or tool_tag");
        }
        if (slotRules.isEmpty()) {
            throw new IllegalArgumentException("Part enchantment route needs at least one slot");
        }
        return new Route(item, itemTag, List.copyOf(slotRules));
    }

    private static SlotRule parseSlot(JsonObject object) {
        Optional<String> id = optionalString(object, "id")
                .or(() -> optionalString(object, "slot"));
        Optional<String> partType = optionalString(object, "part_type")
                .or(() -> optionalString(object, "part"));
        Optional<ResourceLocation> item = optionalLocation(object, "item");
        Optional<TagKey<Item>> itemTag = optionalLocation(object, "item_tag")
                .or(() -> optionalLocation(object, "tag"))
                .map(location -> TagKey.create(Registries.ITEM, location));
        int limit = Math.max(0, GsonHelper.getAsInt(object, "limit", GsonHelper.getAsInt(object, "max", 1)));
        if (partType.isEmpty() && item.isEmpty() && itemTag.isEmpty()) {
            throw new IllegalArgumentException("Part enchantment route slot needs a part_type, part, item, item_tag, or tag");
        }
        return new SlotRule(id, partType, item, itemTag, limit);
    }

    private static Optional<ResourceLocation> optionalLocation(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? Optional.of(parseId(GsonHelper.getAsString(object, key)))
                : Optional.empty();
    }

    private static Optional<String> optionalString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(object, key))
                : Optional.empty();
    }

    private static ResourceLocation parseId(String value) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.indexOf(':') >= 0) {
            return ResourceLocation.parse(normalized);
        }
        return BetterEnchanting.id(normalized);
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/part_enchantment_routes");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            LinkedHashSet<Route> loaded = new LinkedHashSet<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), "part enchantment routes");
                    JsonArray routeArray = GsonHelper.getAsJsonArray(object, "routes");
                    for (JsonElement element : routeArray) {
                        loaded.add(parseRoute(GsonHelper.convertToJsonObject(element, "part enchantment route")));
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid part enchantment route file {}", entry.getKey(), exception);
                }
            }
            routes = List.copyOf(loaded);
            LOGGER.info("Loaded {} Better Enchanting part enchantment route(s) from {} file(s)", routes.size(), resources.size());
        }
    }

    public record Route(Optional<ResourceLocation> item, Optional<TagKey<Item>> itemTag, List<SlotRule> slots) {
        private boolean matches(ItemStack stack) {
            return item.filter(id -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)).isPresent()
                    || itemTag.filter(stack::is).isPresent();
        }
    }

    public record SlotRule(
            Optional<String> id,
            Optional<String> partType,
            Optional<ResourceLocation> item,
            Optional<TagKey<Item>> itemTag,
            int limit
    ) {
        public boolean matches(ItemStack stack, Optional<String> stackPartType) {
            if (stack.isEmpty()) {
                return false;
            }
            if (partType.isPresent() && !partType.equals(stackPartType)) {
                return false;
            }
            if (item.isPresent() && !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item.get())) {
                return false;
            }
            return itemTag.isEmpty() || stack.is(itemTag.get());
        }
    }
}
