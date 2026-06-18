package com.betterenchanting.data;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.compat.SilentGearCompat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;

public final class TagDisplayRules {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final TextColor DEFAULT_COLOR = TextColor.fromRgb(0xA0A0A0);
    private static final TextColor DEFAULT_AFFINITY_COLOR = TextColor.fromRgb(0xFFFFFF);
    private static volatile List<DisplayTag> itemTags = List.of();
    private static volatile List<DisplayTag> enchantmentTags = List.of();
    private static volatile List<DisplayTag> enchantmentTargetTags = List.of();

    private TagDisplayRules() {
    }

    public static List<TagLabel> itemLabels(ItemStack stack) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        Map<ResourceLocation, TagLabel> labelsById = new LinkedHashMap<>();
        List<ResourceLocation> virtualMaterialTags = SilentGearCompat.materialItemTags(stack);
        boolean displayedVirtualMaterial = false;
        for (DisplayTag tag : itemTags) {
            labelsById.put(tag.id(), tag.label());
            boolean virtualMatch = virtualMaterialTags.contains(tag.id());
            if (stack.is(TagKey.create(Registries.ITEM, tag.id())) || virtualMatch) {
                ids.add(tag.id());
                displayedVirtualMaterial |= virtualMatch;
            }
        }
        if (!displayedVirtualMaterial && !virtualMaterialTags.isEmpty()) {
            ResourceLocation primaryVirtualTag = virtualMaterialTags.getFirst();
            labelsById.putIfAbsent(primaryVirtualTag, labelFor(primaryVirtualTag));
            ids.add(primaryVirtualTag);
        }
        return labelsFor(List.copyOf(ids), labelsById);
    }

    public static List<TagLabel> enchantmentLabels(ItemStack stack) {
        return matchingEnchantmentLabels(stack, enchantmentTags);
    }

    public static List<TagLabel> enchantmentTargetLabels(ItemStack stack) {
        return matchingEnchantmentLabels(stack, enchantmentTargetTags);
    }

    public static TagLabel labelFor(ResourceLocation tag) {
        for (DisplayTag candidate : itemTags) {
            if (candidate.id().equals(tag)) {
                return candidate.label();
            }
        }
        for (DisplayTag candidate : enchantmentTags) {
            if (candidate.id().equals(tag)) {
                return candidate.label();
            }
        }
        for (DisplayTag candidate : enchantmentTargetTags) {
            if (candidate.id().equals(tag)) {
                return candidate.label();
            }
        }
        return new TagLabel(titleCase(EssenceDefinitions.compactTagName(tag)), DEFAULT_COLOR);
    }

    public static TextColor dominantAffinityColor(Holder<Enchantment> enchantment) {
        for (DisplayTag tag : enchantmentTags) {
            if (enchantment.is(TagKey.create(Registries.ENCHANTMENT, tag.id()))) {
                return tag.label().color();
            }
        }
        return DEFAULT_AFFINITY_COLOR;
    }

    private static List<TagLabel> matchingEnchantmentLabels(ItemStack stack, List<DisplayTag> candidates) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        Map<ResourceLocation, TagLabel> labelsById = new LinkedHashMap<>();
        for (DisplayTag candidate : candidates) {
            labelsById.put(candidate.id(), candidate.label());
        }
        addEnchantmentTags(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), candidates, ids);
        addEnchantmentTags(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY), candidates, ids);
        return labelsFor(List.copyOf(ids), labelsById);
    }

    private static void addEnchantmentTags(ItemEnchantments enchantments, List<DisplayTag> candidates, Set<ResourceLocation> ids) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            for (DisplayTag candidate : candidates) {
                if (enchantment.is(TagKey.create(Registries.ENCHANTMENT, candidate.id()))) {
                    ids.add(candidate.id());
                }
            }
        }
    }

    private static List<TagLabel> labelsFor(List<ResourceLocation> ids, Map<ResourceLocation, TagLabel> labelsById) {
        List<TagLabel> labels = new ArrayList<>();
        for (ResourceLocation id : TagSimplifier.simplify(ids)) {
            TagLabel label = labelsById.get(id);
            labels.add(label == null ? labelFor(id) : label);
        }
        return labels;
    }

    private static DisplayTag parseTag(JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "display tag");
        ResourceLocation tag = parseTagId(GsonHelper.getAsString(object, "tag"));
        String label = GsonHelper.getAsString(object, "label", titleCase(EssenceDefinitions.compactTagName(tag)));
        TextColor color = parseColor(GsonHelper.getAsString(object, "color", "gray"));
        return new DisplayTag(tag, new TagLabel(label, color));
    }

    private static ResourceLocation parseTagId(String value) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.indexOf(':') >= 0) {
            return ResourceLocation.parse(normalized);
        }
        return BetterEnchanting.id(normalized);
    }

    private static TextColor parseColor(String value) {
        String color = value.trim().toLowerCase(Locale.ROOT);
        if (color.startsWith("#")) {
            return TextColor.fromRgb(Integer.parseInt(color.substring(1), 16));
        }
        if (color.startsWith("0x")) {
            return TextColor.fromRgb(Integer.parseInt(color.substring(2), 16));
        }

        ChatFormatting formatting = ChatFormatting.getByName(color);
        if (formatting != null && formatting.getColor() != null) {
            return TextColor.fromRgb(formatting.getColor());
        }
        return DEFAULT_COLOR;
    }

    private static String titleCase(String value) {
        if (value.isEmpty()) {
            return value;
        }
        String[] words = value.replace('_', ' ').split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, "better_enchanting/tag_display");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            List<DisplayTag> loadedItemTags = new ArrayList<>();
            List<DisplayTag> loadedEnchantmentTags = new ArrayList<>();
            List<DisplayTag> loadedTargetTags = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), "tag display rules");
                    loadTags(object, "item_tags", loadedItemTags);
                    loadTags(object, "enchantment_tags", loadedEnchantmentTags);
                    loadTags(object, "enchantment_target_tags", loadedTargetTags);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid tag display rules {}", entry.getKey(), exception);
                }
            }
            itemTags = List.copyOf(loadedItemTags);
            enchantmentTags = List.copyOf(loadedEnchantmentTags);
            enchantmentTargetTags = List.copyOf(loadedTargetTags);
            LOGGER.info(
                    "Loaded Better Enchanting tag display data: {} item tag(s), {} enchantment tag(s), {} target tag(s)",
                    itemTags.size(),
                    enchantmentTags.size(),
                    enchantmentTargetTags.size()
            );
        }

        private static void loadTags(JsonObject object, String memberName, List<DisplayTag> output) {
            if (!object.has(memberName)) {
                return;
            }
            JsonArray tags = GsonHelper.getAsJsonArray(object, memberName);
            for (JsonElement tag : tags) {
                output.add(parseTag(tag));
            }
        }
    }

    private record DisplayTag(ResourceLocation id, TagLabel label) {
    }

    public record TagLabel(String text, TextColor color) {
    }
}
