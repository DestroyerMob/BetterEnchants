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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class ApothicInfusionModifierRules {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile Map<ResourceLocation, Rule> rulesByInfusion = Map.of();

    private ApothicInfusionModifierRules() {
    }

    public static Match unrestricted() {
        return Match.unrestricted();
    }

    public static Match match(ResourceLocation infusionId, Container container, int firstSlot, int slotCount) {
        Rule rule = rulesByInfusion.get(infusionId);
        if (rule == null) {
            return Match.unrestricted();
        }

        boolean[] usedSlots = new boolean[slotCount];
        List<Integer> matchedSlots = new ArrayList<>();
        List<ModifierIngredient> missing = new ArrayList<>();
        for (ModifierIngredient ingredient : rule.modifiers()) {
            int matchingSlot = findMatchingSlot(ingredient, container, firstSlot, slotCount, usedSlots);
            if (matchingSlot < 0) {
                missing.add(ingredient);
            } else {
                usedSlots[matchingSlot - firstSlot] = true;
                matchedSlots.add(matchingSlot);
            }
        }

        return new Match(true, missing.isEmpty(), rule.consume(), List.copyOf(matchedSlots), List.copyOf(missing));
    }

    public static void consume(Container container, Match match, Player player) {
        if (!match.shouldConsume()) {
            return;
        }

        for (int slot : match.matchedSlots().stream().distinct().toList()) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                stack.consume(1, player);
                if (stack.isEmpty()) {
                    container.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    private static int findMatchingSlot(
            ModifierIngredient ingredient,
            Container container,
            int firstSlot,
            int slotCount,
            boolean[] usedSlots
    ) {
        for (int offset = 0; offset < slotCount; offset++) {
            if (usedSlots[offset]) {
                continue;
            }
            int slot = firstSlot + offset;
            if (ingredient.matches(container.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static Rule parseRule(ResourceLocation fileId, JsonObject object) {
        if (!object.has("infusion")) {
            throw new IllegalArgumentException("Missing Apothic infusion recipe id");
        }

        ResourceLocation infusion = ResourceLocation.parse(GsonHelper.getAsString(object, "infusion"));
        List<ModifierIngredient> modifiers = parseModifiers(object);
        if (modifiers.isEmpty()) {
            throw new IllegalArgumentException("Apothic infusion modifier rule has no modifiers: " + infusion);
        }

        return new Rule(
                fileId,
                infusion,
                modifiers,
                GsonHelper.getAsBoolean(object, "consume", true)
        );
    }

    private static List<Rule> parseFile(ResourceLocation fileId, JsonObject object) {
        if (!object.has("rules")) {
            return List.of(parseRule(fileId, object));
        }

        JsonArray rules = GsonHelper.getAsJsonArray(object, "rules");
        List<Rule> parsed = new ArrayList<>();
        for (JsonElement element : rules) {
            parsed.add(parseRule(fileId, GsonHelper.convertToJsonObject(element, "Apothic infusion modifier rule")));
        }
        return List.copyOf(parsed);
    }

    private static List<ModifierIngredient> parseModifiers(JsonObject object) {
        if (object.has("modifier")) {
            return List.of(parseModifier(object.get("modifier")));
        }

        JsonArray modifierArray = GsonHelper.getAsJsonArray(object, "modifiers");
        List<ModifierIngredient> modifiers = new ArrayList<>();
        for (JsonElement element : modifierArray) {
            modifiers.add(parseModifier(element));
        }
        return List.copyOf(modifiers);
    }

    private static ModifierIngredient parseModifier(JsonElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Missing Apothic infusion modifier");
        }
        if (element.isJsonPrimitive()) {
            String value = GsonHelper.convertToString(element, "Apothic infusion modifier");
            if (value.startsWith("#")) {
                return ModifierIngredient.tag(parseId(value.substring(1), BetterEnchanting.MOD_ID));
            }
            return ModifierIngredient.item(parseItemId(value));
        }

        JsonObject object = GsonHelper.convertToJsonObject(element, "Apothic infusion modifier");
        if (object.has("item")) {
            return ModifierIngredient.item(parseItemId(GsonHelper.getAsString(object, "item")));
        }
        if (object.has("id")) {
            return ModifierIngredient.item(parseItemId(GsonHelper.getAsString(object, "id")));
        }
        if (object.has("tag")) {
            return ModifierIngredient.tag(parseId(GsonHelper.getAsString(object, "tag"), BetterEnchanting.MOD_ID));
        }
        throw new IllegalArgumentException("Apothic infusion modifier requires item, id, tag, or string syntax");
    }

    private static ResourceLocation parseItemId(String value) {
        ResourceLocation itemId = parseId(value, BetterEnchanting.MOD_ID);
        if (BuiltInRegistries.ITEM.getOptional(itemId).isEmpty()) {
            throw new IllegalArgumentException("Unknown Apothic infusion modifier item: " + itemId);
        }
        return itemId;
    }

    private static ResourceLocation parseId(String value, String defaultNamespace) {
        return value.indexOf(':') >= 0
                ? ResourceLocation.parse(value)
                : ResourceLocation.fromNamespaceAndPath(defaultNamespace, value);
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/apothic_infusion_modifiers");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, Rule> loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), "Apothic infusion modifier rules");
                    for (Rule rule : parseFile(entry.getKey(), object)) {
                        Rule replaced = loaded.put(rule.infusion(), rule);
                        if (replaced != null) {
                            LOGGER.warn(
                                    "Apothic infusion modifier rule {} replaced earlier rule {} for {}",
                                    rule.id(),
                                    replaced.id(),
                                    rule.infusion()
                            );
                        }
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid Apothic infusion modifier rule file {}", entry.getKey(), exception);
                }
            }
            rulesByInfusion = Map.copyOf(loaded);
            LOGGER.info("Loaded {} Better Enchanting Apothic infusion modifier rule(s)", rulesByInfusion.size());
        }
    }

    private record Rule(ResourceLocation id, ResourceLocation infusion, List<ModifierIngredient> modifiers, boolean consume) {
        private Rule {
            modifiers = List.copyOf(modifiers);
        }
    }

    public record Match(
            boolean required,
            boolean matches,
            boolean consume,
            List<Integer> matchedSlots,
            List<ModifierIngredient> missing
    ) {
        public Match {
            matchedSlots = List.copyOf(matchedSlots);
            missing = List.copyOf(missing);
        }

        private static Match unrestricted() {
            return new Match(false, true, false, List.of(), List.of());
        }

        public boolean shouldConsume() {
            return this.required && this.matches && this.consume && !this.matchedSlots.isEmpty();
        }
    }

    public record ModifierIngredient(ResourceLocation id, Item item, TagKey<Item> tag) {
        public ModifierIngredient {
            if ((item == null) == (tag == null)) {
                throw new IllegalArgumentException("Modifier ingredient must be either an item or an item tag");
            }
        }

        private static ModifierIngredient item(ResourceLocation id) {
            Item item = BuiltInRegistries.ITEM.getOptional(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown Apothic infusion modifier item: " + id));
            return new ModifierIngredient(id, item, null);
        }

        private static ModifierIngredient tag(ResourceLocation id) {
            return new ModifierIngredient(id, null, TagKey.create(Registries.ITEM, id));
        }

        private boolean matches(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            return this.item != null ? stack.is(this.item) : stack.is(this.tag);
        }
    }
}
