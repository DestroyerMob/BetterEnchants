package com.betterenchanting.compat;

import com.betterenchanting.BetterEnchanting;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

final class MaterialTagResolver {
    private static final String MATERIALS_PREFIX = "materials/";
    private static final String TARGET_MATERIALS_PREFIX = "targets/materials/";
    private static final Set<String> WOOD_MATERIAL_PATHS = woodMaterialPaths();

    private MaterialTagResolver() {
    }

    static List<ResourceLocation> materialItemTags(ResourceLocation materialId) {
        if (isVanillaWoodMaterial(materialId)) {
            return List.of(BetterEnchanting.id(MATERIALS_PREFIX + "wood"));
        }

        Set<ResourceLocation> tags = new LinkedHashSet<>();
        tags.add(BetterEnchanting.id(MATERIALS_PREFIX + materialId.getPath()));
        tags.add(BetterEnchanting.id(MATERIALS_PREFIX + materialId.getNamespace() + "/" + materialId.getPath()));
        return List.copyOf(tags);
    }

    static List<ResourceLocation> materialTargetTags(ResourceLocation materialId) {
        if (isVanillaWoodMaterial(materialId)) {
            return List.of(BetterEnchanting.id(TARGET_MATERIALS_PREFIX + "wood"));
        }

        Set<ResourceLocation> tags = new LinkedHashSet<>();
        tags.add(BetterEnchanting.id(TARGET_MATERIALS_PREFIX + materialId.getPath()));
        tags.add(BetterEnchanting.id(TARGET_MATERIALS_PREFIX + materialId.getNamespace() + "/" + materialId.getPath()));
        return List.copyOf(tags);
    }

    private static boolean isVanillaWoodMaterial(ResourceLocation materialId) {
        return WOOD_MATERIAL_PATHS.contains(materialId.getPath());
    }

    private static Set<String> woodMaterialPaths() {
        Set<String> paths = new LinkedHashSet<>();
        paths.add("wood");
        paths.add("wooden");
        for (String family : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo")) {
            paths.add(family);
            paths.add(family + "_planks");
            paths.add(family + "_log");
            paths.add(family + "_wood");
            paths.add("stripped_" + family + "_log");
            paths.add("stripped_" + family + "_wood");
        }
        for (String family : List.of("crimson", "warped")) {
            paths.add(family);
            paths.add(family + "_planks");
            paths.add(family + "_stem");
            paths.add(family + "_hyphae");
            paths.add("stripped_" + family + "_stem");
            paths.add("stripped_" + family + "_hyphae");
        }
        paths.addAll(List.of("bamboo_block", "stripped_bamboo_block", "bamboo_mosaic"));
        return Set.copyOf(paths);
    }
}
