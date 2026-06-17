package com.betterenchanting.data;

import com.betterenchanting.BetterEnchanting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class TagSimplifier {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile List<TagGroup> groups = List.of();

    private TagSimplifier() {
    }

    public static List<ResourceLocation> simplify(List<ResourceLocation> tags) {
        List<ResourceLocation> simplified = new ArrayList<>(new LinkedHashSet<>(tags));
        boolean changed;
        do {
            changed = false;
            for (TagGroup group : groups) {
                if (simplified.containsAll(group.children())) {
                    int parentIndex = simplified.indexOf(group.parent());
                    int insertIndex = parentIndex >= 0 ? parentIndex : firstChildIndex(simplified, group);
                    boolean removedChildren = simplified.removeIf(group.children()::contains);
                    if (!simplified.contains(group.parent())) {
                        simplified.add(Math.min(insertIndex, simplified.size()), group.parent());
                    }
                    changed |= removedChildren;
                }
            }
        } while (changed);
        return List.copyOf(simplified);
    }

    private static int firstChildIndex(List<ResourceLocation> tags, TagGroup group) {
        int index = tags.size();
        for (ResourceLocation child : group.children()) {
            int childIndex = tags.indexOf(child);
            if (childIndex >= 0) {
                index = Math.min(index, childIndex);
            }
        }
        return index;
    }

    private static TagGroup parseGroup(JsonObject object) {
        ResourceLocation parent = parseTagId(GsonHelper.getAsString(object, "parent"));
        JsonArray childArray = GsonHelper.getAsJsonArray(object, "children");
        List<ResourceLocation> children = new ArrayList<>();
        for (JsonElement element : childArray) {
            children.add(parseTagId(GsonHelper.convertToString(element, "child tag")));
        }
        if (children.isEmpty()) {
            throw new IllegalArgumentException("Tag simplification group " + parent + " must have at least one child");
        }
        return new TagGroup(parent, List.copyOf(children));
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
            super(GSON, "better_enchanting/tag_simplification");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, TagGroup> loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), "tag simplification rules");
                    JsonArray groupArray = GsonHelper.getAsJsonArray(object, "groups");
                    for (JsonElement groupElement : groupArray) {
                        TagGroup group = parseGroup(GsonHelper.convertToJsonObject(groupElement, "tag simplification group"));
                        loaded.put(group.parent(), group);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Skipping invalid tag simplification rules {}", entry.getKey(), exception);
                }
            }
            groups = List.copyOf(loaded.values());
            LOGGER.info("Loaded {} Better Enchanting tag simplification group(s)", groups.size());
        }
    }

    private record TagGroup(ResourceLocation parent, List<ResourceLocation> children) {
    }
}
