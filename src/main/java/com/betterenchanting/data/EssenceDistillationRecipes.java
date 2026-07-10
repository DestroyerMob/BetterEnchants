package com.betterenchanting.data;

import com.betterenchanting.BetterEnchanting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
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

/** Data-pack-defined recipes used by the Arcane Crucible. */
public final class EssenceDistillationRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile List<Recipe> recipes = List.of();

    private EssenceDistillationRecipes() {
    }

    public static Optional<Recipe> find(ItemStack medium, ItemStack catalystOne, ItemStack catalystTwo) {
        return recipes.stream().filter(recipe -> recipe.matches(medium, catalystOne, catalystTwo)).findFirst();
    }

    public static boolean isMedium(ItemStack stack) {
        return !stack.isEmpty() && recipes.stream().anyMatch(recipe -> recipe.medium().matches(stack));
    }

    public static boolean isCatalyst(ItemStack stack) {
        return !stack.isEmpty() && recipes.stream()
                .flatMap(recipe -> recipe.catalysts().stream())
                .anyMatch(ingredient -> ingredient.matches(stack));
    }

    public static List<Recipe> all() {
        return recipes;
    }

    private static Recipe parse(ResourceLocation id, JsonObject json) {
        IngredientSpec medium = parseIngredient(GsonHelper.getAsJsonObject(json, "medium"));
        JsonArray catalystJson = GsonHelper.getAsJsonArray(json, "catalysts");
        if (catalystJson.isEmpty() || catalystJson.size() > 2) {
            throw new IllegalArgumentException("Distillation recipes require one or two catalysts");
        }

        List<IngredientSpec> catalysts = new ArrayList<>();
        for (JsonElement element : catalystJson) {
            catalysts.add(parseIngredient(GsonHelper.convertToJsonObject(element, "catalyst")));
        }

        JsonObject resultJson = GsonHelper.getAsJsonObject(json, "result");
        ResourceLocation resultId = ResourceLocation.parse(GsonHelper.getAsString(resultJson, "id"));
        Item resultItem = BuiltInRegistries.ITEM.getOptional(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown distillation result item " + resultId));
        int count = GsonHelper.getAsInt(resultJson, "count", 1);
        if (count < 1 || count > resultItem.getDefaultMaxStackSize()) {
            throw new IllegalArgumentException("Invalid distillation result count " + count);
        }
        return new Recipe(id, medium, List.copyOf(catalysts), new ItemStack(resultItem, count));
    }

    private static IngredientSpec parseIngredient(JsonObject json) {
        boolean hasItem = json.has("item");
        boolean hasTag = json.has("tag");
        if (hasItem == hasTag) {
            throw new IllegalArgumentException("An ingredient must define exactly one of item or tag");
        }
        if (hasItem) {
            ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(json, "item"));
            Item item = BuiltInRegistries.ITEM.getOptional(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown distillation ingredient " + itemId));
            return new IngredientSpec(item, null);
        }
        ResourceLocation tagId = ResourceLocation.parse(GsonHelper.getAsString(json, "tag"));
        return new IngredientSpec(null, TagKey.create(Registries.ITEM, tagId));
    }

    public record Recipe(ResourceLocation id, IngredientSpec medium, List<IngredientSpec> catalysts, ItemStack result) {
        public boolean matches(ItemStack mediumStack, ItemStack catalystOne, ItemStack catalystTwo) {
            if (!medium.matches(mediumStack)) {
                return false;
            }

            List<ItemStack> supplied = new ArrayList<>(2);
            if (!catalystOne.isEmpty()) {
                supplied.add(catalystOne);
            }
            if (!catalystTwo.isEmpty()) {
                supplied.add(catalystTwo);
            }
            if (supplied.size() != catalysts.size()) {
                return false;
            }

            List<ItemStack> unmatched = new ArrayList<>(supplied);
            for (IngredientSpec catalyst : catalysts) {
                int matchingIndex = -1;
                for (int index = 0; index < unmatched.size(); index++) {
                    if (catalyst.matches(unmatched.get(index))) {
                        matchingIndex = index;
                        break;
                    }
                }
                if (matchingIndex < 0) {
                    return false;
                }
                unmatched.remove(matchingIndex);
            }
            return unmatched.isEmpty();
        }

        public ItemStack resultCopy() {
            return result.copy();
        }
    }

    public record IngredientSpec(Item item, TagKey<Item> tag) {
        public boolean matches(ItemStack stack) {
            return item != null ? stack.is(item) : tag != null && stack.is(tag);
        }

        public Optional<ItemStack> displayStack() {
            if (item != null) {
                return Optional.of(new ItemStack(item));
            }
            return BuiltInRegistries.ITEM.getTag(tag)
                    .flatMap(named -> named.stream().findFirst())
                    .map(holder -> new ItemStack(holder.value()));
        }
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/distillation");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            List<Recipe> loaded = new ArrayList<>();
            resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                try {
                    loaded.add(parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(), "distillation recipe")));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid essence distillation recipe {}", entry.getKey(), exception);
                }
            });
            recipes = List.copyOf(loaded);
            LOGGER.info("Loaded {} Better Enchanting essence distillation recipes", recipes.size());
        }
    }
}
