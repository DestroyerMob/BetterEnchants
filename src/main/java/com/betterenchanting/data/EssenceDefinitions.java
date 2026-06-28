package com.betterenchanting.data;

import com.betterenchanting.BetterEnchanting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class EssenceDefinitions {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile Map<ResourceLocation, EssenceDefinition> definitions = defaults();

    private EssenceDefinitions() {
    }

    public static Optional<EssenceDefinition> get(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    public static boolean isEssence(ItemStack stack) {
        return get(stack).isPresent();
    }

    public static List<ResourceLocation> tagsFor(ItemStack stack) {
        return get(stack).map(EssenceDefinition::tags).orElse(List.of());
    }

    public static String compactTagName(ResourceLocation tag) {
        String path = tag.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static Map<ResourceLocation, EssenceDefinition> defaults() {
        Map<ResourceLocation, EssenceDefinition> map = new HashMap<>();
        add(map, "fire_essence", 1.0, true, "fire");
        add(map, "frost_essence", 1.0, true, "frost");
        add(map, "lightning_essence", 1.0, true, "lightning");
        add(map, "physical_essence", 1.0, true, "physical");
        add(map, "mining_essence", 1.0, true, "mining");
        add(map, "defensive_essence", 1.0, true, "defensive");
        add(map, "vitality_essence", 1.0, true, "vitality");
        add(map, "mobility_essence", 1.0, true, "mobility");
        add(map, "void_essence", 1.0, true, "void");
        addSpecial(map, "purification_essence", 1.0, false, List.of(EnchantmentTags.CURSE.location()), true, true, "purification");
        return Map.copyOf(map);
    }

    private static void add(
            Map<ResourceLocation, EssenceDefinition> map,
            String itemPath,
            double multiplier,
            boolean restrictsPool,
            String... tagPaths
    ) {
        addSpecial(map, itemPath, multiplier, restrictsPool, List.of(), false, false, tagPaths);
    }

    private static void addSpecial(
            Map<ResourceLocation, EssenceDefinition> map,
            String itemPath,
            double multiplier,
            boolean restrictsPool,
            List<ResourceLocation> removedTags,
            boolean appliesToAllOffers,
            boolean blocksOffer,
            String... tagPaths
    ) {
        ResourceLocation item = BetterEnchanting.id(itemPath);
        List<ResourceLocation> tags = new ArrayList<>();
        for (String tagPath : tagPaths) {
            tags.add(BetterEnchanting.id(tagPath));
        }
        map.put(item, new EssenceDefinition(
                item,
                List.copyOf(tags),
                List.copyOf(removedTags),
                multiplier,
                restrictsPool,
                appliesToAllOffers,
                blocksOffer
        ));
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/essences");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, EssenceDefinition> loaded = new HashMap<>(defaults());
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    EssenceDefinition definition = parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(), "essence"));
                    loaded.put(definition.item(), definition);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid essence definition {}", entry.getKey(), exception);
                }
            }
            definitions = Map.copyOf(loaded);
            LOGGER.info("Loaded {} Better Enchanting essence definitions", definitions.size());
        }

        private static EssenceDefinition parse(ResourceLocation id, JsonObject object) {
            ResourceLocation item = ResourceLocation.parse(GsonHelper.getAsString(object, "item", id.toString()));
            List<ResourceLocation> tags = parseTags(object, "tags");
            if (tags.isEmpty()) {
                throw new IllegalArgumentException("Essence " + id + " must declare at least one tag");
            }
            List<ResourceLocation> removedTags = new ArrayList<>(parseTags(object, "removed_tags"));
            if (GsonHelper.getAsBoolean(object, "removes_curses", false) && !removedTags.contains(EnchantmentTags.CURSE.location())) {
                removedTags.add(EnchantmentTags.CURSE.location());
            }
            double weightMultiplier = GsonHelper.getAsDouble(object, "weight_multiplier", 1.0D);
            boolean restrictsPool = GsonHelper.getAsBoolean(object, "restricts_pool", true);
            boolean appliesToAllOffers = GsonHelper.getAsBoolean(object, "applies_to_all_offers", false);
            boolean blocksOffer = GsonHelper.getAsBoolean(object, "blocks_offer", false);
            return new EssenceDefinition(
                    item,
                    List.copyOf(tags),
                    List.copyOf(removedTags),
                    weightMultiplier,
                    restrictsPool,
                    appliesToAllOffers,
                    blocksOffer
            );
        }

        private static List<ResourceLocation> parseTags(JsonObject object, String key) {
            JsonArray tagArray = GsonHelper.getAsJsonArray(object, key, new JsonArray());
            List<ResourceLocation> tags = new ArrayList<>();
            for (JsonElement tagElement : tagArray) {
                tags.add(ResourceLocation.parse(GsonHelper.convertToString(tagElement, key + " entry")));
            }
            return List.copyOf(tags);
        }
    }
}
