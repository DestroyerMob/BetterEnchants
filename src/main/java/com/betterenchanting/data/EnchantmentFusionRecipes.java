package com.betterenchanting.data;

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
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;

public final class EnchantmentFusionRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile List<FusionRecipe> recipes = List.of();

    private EnchantmentFusionRecipes() {
    }

    public static boolean apply(RegistryAccess registryAccess, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        List<ResolvedFusionRecipe> resolvedRecipes = resolveRecipes(registry);
        if (resolvedRecipes.isEmpty()) {
            return false;
        }

        boolean[] changed = new boolean[]{false};
        EnchantmentHelper.updateEnchantments(stack, mutable -> {
            for (int pass = 0; pass < 16; pass++) {
                boolean changedThisPass = false;
                for (ResolvedFusionRecipe recipe : resolvedRecipes) {
                    if (!recipe.matches(mutable)) {
                        continue;
                    }

                    int resultLevel = recipe.resultLevel(mutable);
                    mutable.removeIf(recipe::isIngredient);
                    mutable.set(recipe.result(), Math.max(resultLevel, mutable.getLevel(recipe.result())));
                    changed[0] = true;
                    changedThisPass = true;
                }

                if (!changedThisPass) {
                    break;
                }
            }
        });
        return changed[0];
    }

    public static boolean applyToEnchantmentSet(RegistryAccess registryAccess, Set<Holder<Enchantment>> enchantments) {
        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        List<ResolvedFusionRecipe> resolvedRecipes = resolveRecipes(registry);
        boolean changed = false;
        for (int pass = 0; pass < 16; pass++) {
            boolean changedThisPass = false;
            for (ResolvedFusionRecipe recipe : resolvedRecipes) {
                if (!enchantments.containsAll(recipe.ingredients())) {
                    continue;
                }

                enchantments.removeAll(recipe.ingredients());
                enchantments.add(recipe.result());
                changed = true;
                changedThisPass = true;
            }

            if (!changedThisPass) {
                break;
            }
        }
        return changed;
    }

    public static boolean conflictsWithFusionIngredient(RegistryAccess registryAccess, Holder<Enchantment> candidate, Set<Holder<Enchantment>> existingEnchantments) {
        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        for (ResolvedFusionRecipe recipe : resolveRecipes(registry)) {
            boolean candidateIsResult = candidate.equals(recipe.result());
            boolean candidateIsIngredient = recipe.ingredients().contains(candidate);
            if (!candidateIsResult && !candidateIsIngredient) {
                continue;
            }

            for (Holder<Enchantment> existing : existingEnchantments) {
                if ((candidateIsResult && recipe.ingredients().contains(existing))
                        || (candidateIsIngredient && existing.equals(recipe.result()))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean areRecipeIngredients(RegistryAccess registryAccess, Holder<Enchantment> first, Holder<Enchantment> second) {
        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        for (ResolvedFusionRecipe recipe : resolveRecipes(registry)) {
            if (recipe.ingredients().contains(first) && recipe.ingredients().contains(second)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFusionResult(RegistryAccess registryAccess, Holder<Enchantment> enchantment) {
        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        for (ResolvedFusionRecipe recipe : resolveRecipes(registry)) {
            if (enchantment.equals(recipe.result())) {
                return true;
            }
        }
        return false;
    }

    private static List<ResolvedFusionRecipe> resolveRecipes(Registry<Enchantment> registry) {
        List<ResolvedFusionRecipe> resolved = new ArrayList<>();
        for (FusionRecipe recipe : recipes) {
            Optional<Holder.Reference<Enchantment>> result = holder(registry, recipe.result());
            if (result.isEmpty()) {
                continue;
            }

            List<Holder<Enchantment>> ingredients = new ArrayList<>();
            boolean missingIngredient = false;
            for (ResourceLocation ingredientId : recipe.ingredients()) {
                Optional<Holder.Reference<Enchantment>> ingredient = holder(registry, ingredientId);
                if (ingredient.isEmpty()) {
                    missingIngredient = true;
                    break;
                }
                ingredients.add(ingredient.get());
            }

            if (!missingIngredient && !ingredients.isEmpty()) {
                resolved.add(new ResolvedFusionRecipe(recipe.id(), List.copyOf(ingredients), result.get(), recipe.levelRule(), recipe.ingredients()));
            }
        }
        return resolved;
    }

    private static Optional<Holder.Reference<Enchantment>> holder(Registry<Enchantment> registry, ResourceLocation id) {
        return registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, id));
    }

    private static FusionRecipe parse(ResourceLocation id, JsonObject object) {
        JsonArray ingredientArray = GsonHelper.getAsJsonArray(object, "ingredients");
        List<ResourceLocation> ingredients = new ArrayList<>();
        for (JsonElement element : ingredientArray) {
            ingredients.add(parseEnchantmentId(element, "ingredient"));
        }

        JsonElement resultElement = object.get("result");
        ResourceLocation result;
        LevelRule levelRule = LevelRule.constant(1);
        if (resultElement != null && resultElement.isJsonObject()) {
            JsonObject resultObject = resultElement.getAsJsonObject();
            result = parseEnchantmentId(resultObject, "result");
            if (resultObject.has("level")) {
                levelRule = parseLevelRule(resultObject.get("level"));
            }
        } else {
            result = parseEnchantmentId(resultElement, "result");
        }

        if (object.has("result_level")) {
            levelRule = parseLevelRule(object.get("result_level"));
        }

        return new FusionRecipe(id, List.copyOf(ingredients), result, levelRule);
    }

    private static ResourceLocation parseEnchantmentId(JsonElement element, String name) {
        if (element == null) {
            throw new IllegalArgumentException("Missing " + name + " enchantment id");
        }
        if (element.isJsonPrimitive()) {
            return ResourceLocation.parse(GsonHelper.convertToString(element, name));
        }
        return parseEnchantmentId(GsonHelper.convertToJsonObject(element, name), name);
    }

    private static ResourceLocation parseEnchantmentId(JsonObject object, String name) {
        if (object.has("enchantment")) {
            return ResourceLocation.parse(GsonHelper.getAsString(object, "enchantment"));
        }
        if (object.has("id")) {
            return ResourceLocation.parse(GsonHelper.getAsString(object, "id"));
        }
        throw new IllegalArgumentException("Missing enchantment id for " + name);
    }

    private static LevelRule parseLevelRule(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                return LevelRule.constant(Math.max(1, element.getAsInt()));
            }
            return LevelRule.source(ResourceLocation.parse(element.getAsString()));
        }

        JsonObject object = GsonHelper.convertToJsonObject(element, "result level");
        String type = GsonHelper.getAsString(object, "type", "constant");
        return switch (type) {
            case "ingredient", "source" -> LevelRule.source(parseSourceId(object));
            case "additive", "sum", "sum_ingredient", "sum_ingredients" -> LevelRule.sumIngredients();
            case "max", "highest", "max_ingredient", "max_ingredients", "highest_ingredient" -> LevelRule.maxOfIngredients();
            case "min", "lowest", "min_ingredient", "min_ingredients", "lowest_ingredient" -> LevelRule.minOfIngredients();
            case "constant" -> LevelRule.constant(Math.max(1, GsonHelper.getAsInt(object, "value", 1)));
            default -> throw new IllegalArgumentException("Unknown enchantment fusion level rule type: " + type);
        };
    }

    private static ResourceLocation parseSourceId(JsonObject object) {
        if (object.has("enchantment")) {
            return ResourceLocation.parse(GsonHelper.getAsString(object, "enchantment"));
        }
        if (object.has("source")) {
            return ResourceLocation.parse(GsonHelper.getAsString(object, "source"));
        }
        throw new IllegalArgumentException("Ingredient level rule requires an enchantment or source id");
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/enchantment_fusions");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, FusionRecipe> loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    loaded.put(entry.getKey(), parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(), "enchantment fusion recipe")));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid enchantment fusion recipe {}", entry.getKey(), exception);
                }
            }
            recipes = List.copyOf(loaded.values());
            LOGGER.info("Loaded {} Better Enchanting enchantment fusion recipe(s)", recipes.size());
        }
    }

    private record FusionRecipe(ResourceLocation id, List<ResourceLocation> ingredients, ResourceLocation result, LevelRule levelRule) {
    }

    private record ResolvedFusionRecipe(
            ResourceLocation id,
            List<Holder<Enchantment>> ingredients,
            Holder<Enchantment> result,
            LevelRule levelRule,
            List<ResourceLocation> ingredientIds
    ) {
        private boolean matches(ItemEnchantments.Mutable enchantments) {
            for (Holder<Enchantment> ingredient : this.ingredients) {
                if (enchantments.getLevel(ingredient) <= 0) {
                    return false;
                }
            }
            return true;
        }

        private boolean isIngredient(Holder<Enchantment> enchantment) {
            return this.ingredients.contains(enchantment);
        }

        private int resultLevel(ItemEnchantments.Mutable enchantments) {
            Map<ResourceLocation, Integer> ingredientLevels = new LinkedHashMap<>();
            for (int index = 0; index < this.ingredients.size(); index++) {
                ingredientLevels.put(this.ingredientIds.get(index), enchantments.getLevel(this.ingredients.get(index)));
            }
            return Math.max(1, this.levelRule.resolve(ingredientLevels));
        }
    }

    private record LevelRule(int constant, ResourceLocation source, IngredientLevelMode ingredientLevelMode) {
        private static LevelRule constant(int value) {
            return new LevelRule(value, null, IngredientLevelMode.NONE);
        }

        private static LevelRule source(ResourceLocation source) {
            return new LevelRule(1, source, IngredientLevelMode.NONE);
        }

        private static LevelRule sumIngredients() {
            return new LevelRule(1, null, IngredientLevelMode.SUM);
        }

        private static LevelRule maxOfIngredients() {
            return new LevelRule(1, null, IngredientLevelMode.MAX);
        }

        private static LevelRule minOfIngredients() {
            return new LevelRule(1, null, IngredientLevelMode.MIN);
        }

        private int resolve(Map<ResourceLocation, Integer> ingredientLevels) {
            if (this.source != null) {
                return ingredientLevels.getOrDefault(this.source, this.constant);
            }
            if (this.ingredientLevelMode == IngredientLevelMode.SUM) {
                int sum = 0;
                for (int level : ingredientLevels.values()) {
                    sum += level;
                }
                return Math.max(1, sum);
            }
            if (this.ingredientLevelMode == IngredientLevelMode.MAX) {
                int max = 1;
                for (int level : ingredientLevels.values()) {
                    max = Math.max(max, level);
                }
                return max;
            }
            if (this.ingredientLevelMode == IngredientLevelMode.MIN) {
                int min = Integer.MAX_VALUE;
                for (int level : ingredientLevels.values()) {
                    min = Math.min(min, level);
                }
                return min == Integer.MAX_VALUE ? this.constant : Math.max(1, min);
            }
            return this.constant;
        }
    }

    private enum IngredientLevelMode {
        NONE,
        SUM,
        MAX,
        MIN
    }
}
