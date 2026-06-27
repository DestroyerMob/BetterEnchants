package com.betterenchanting.compat;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class SilentGearCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
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

    public static Map<ResourceLocation, Integer> materialItemTagCounts(ItemStack stack) {
        Optional<ResourceLocation> materialId = primaryMaterialId(stack);
        if (materialId.isEmpty()) {
            return Map.of();
        }

        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (ResourceLocation tag : materialItemTags(materialId.get())) {
            counts.merge(tag, 1, Integer::sum);
        }
        return Map.copyOf(counts);
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
        return MaterialTagResolver.materialItemTags(materialId);
    }

    private static List<ResourceLocation> materialTargetTags(ResourceLocation materialId) {
        return MaterialTagResolver.materialTargetTags(materialId);
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
