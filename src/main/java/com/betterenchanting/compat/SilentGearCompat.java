package com.betterenchanting.compat;

import com.betterenchanting.BetterEnchanting;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class SilentGearCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MATERIALS_PREFIX = "materials/";
    private static final String TARGET_MATERIALS_PREFIX = "targets/materials/";
    private static final Set<String> WOOD_MATERIAL_PATHS = woodMaterialPaths();
    private static volatile boolean reflectionAttempted;
    private static volatile boolean runtimeWarningLogged;
    private static volatile Reflection reflection;

    private SilentGearCompat() {
    }

    public static List<ResourceLocation> materialItemTags(ItemStack stack) {
        Optional<ResourceLocation> materialId = primaryMaterialId(stack);
        if (materialId.isEmpty()) {
            return List.of();
        }
        return materialItemTags(materialId.get());
    }

    public static boolean hasMaterialItemTag(ItemStack stack, TagKey<Item> tag) {
        return hasMaterialItemTag(stack, tag.location());
    }

    public static boolean hasMaterialItemTag(ItemStack stack, ResourceLocation tag) {
        return materialItemTags(stack).contains(tag);
    }

    public static List<ResourceLocation> materialTargetTags(ItemStack stack) {
        Optional<ResourceLocation> materialId = primaryMaterialId(stack);
        if (materialId.isEmpty()) {
            return List.of();
        }
        return materialTargetTags(materialId.get());
    }

    public static Optional<ResourceLocation> primaryMaterialId(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Reflection access = reflection();
        if (access == null || !access.gearItemClass().isInstance(stack.getItem())) {
            return Optional.empty();
        }

        try {
            Object construction = access.getConstruction().invoke(null, stack);
            Object primaryPart = access.getPrimaryPart().invoke(construction);
            if (primaryPart == null) {
                return Optional.empty();
            }

            Object primaryMaterial = access.getPrimaryMaterial().invoke(primaryPart);
            if (primaryMaterial == null) {
                return Optional.empty();
            }

            Object valid = access.materialIsValid().invoke(primaryMaterial);
            if (valid instanceof Boolean isValid && !isValid) {
                return Optional.empty();
            }

            Object id = access.materialGetId().invoke(primaryMaterial);
            return id instanceof ResourceLocation location ? Optional.of(location) : Optional.empty();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return Optional.empty();
        }
    }

    private static List<ResourceLocation> materialItemTags(ResourceLocation materialId) {
        if (isVanillaWoodMaterial(materialId)) {
            return List.of(BetterEnchanting.id(MATERIALS_PREFIX + "wood"));
        }

        Set<ResourceLocation> tags = new LinkedHashSet<>();
        tags.add(BetterEnchanting.id(MATERIALS_PREFIX + materialId.getPath()));
        tags.add(BetterEnchanting.id(MATERIALS_PREFIX + materialId.getNamespace() + "/" + materialId.getPath()));
        return List.copyOf(tags);
    }

    private static List<ResourceLocation> materialTargetTags(ResourceLocation materialId) {
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

    private static Reflection reflection() {
        if (reflectionAttempted) {
            return reflection;
        }

        synchronized (SilentGearCompat.class) {
            if (reflectionAttempted) {
                return reflection;
            }

            try {
                Class<?> gearItemClass = Class.forName("net.silentchaos512.gear.api.item.GearItem");
                Class<?> gearDataClass = Class.forName("net.silentchaos512.gear.util.GearData");
                Class<?> constructionClass = Class.forName("net.silentchaos512.gear.core.component.GearConstructionData");
                Class<?> partInstanceClass = Class.forName("net.silentchaos512.gear.gear.part.PartInstance");
                Class<?> materialInstanceClass = Class.forName("net.silentchaos512.gear.gear.material.MaterialInstance");

                reflection = new Reflection(
                        gearItemClass,
                        gearDataClass.getMethod("getConstruction", ItemStack.class),
                        constructionClass.getMethod("getPrimaryPart"),
                        partInstanceClass.getMethod("getPrimaryMaterial"),
                        materialInstanceClass.getMethod("isValid"),
                        materialInstanceClass.getMethod("getId")
                );
                LOGGER.info("Enabled Silent Gear material compatibility");
            } catch (ClassNotFoundException exception) {
                LOGGER.debug("Silent Gear not detected; material compatibility disabled");
            } catch (ReflectiveOperationException | LinkageError exception) {
                LOGGER.warn("Silent Gear was detected, but Better Enchanting could not enable material compatibility", exception);
            } finally {
                reflectionAttempted = true;
            }

            return reflection;
        }
    }

    private static void logRuntimeWarning(Throwable exception) {
        if (runtimeWarningLogged) {
            return;
        }
        runtimeWarningLogged = true;
        LOGGER.warn("Failed to read Silent Gear material data for an item stack; material compatibility will be skipped for that stack", exception);
    }

    private record Reflection(
            Class<?> gearItemClass,
            Method getConstruction,
            Method getPrimaryPart,
            Method getPrimaryMaterial,
            Method materialIsValid,
            Method materialGetId
    ) {
    }
}
