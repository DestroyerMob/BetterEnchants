package com.betterenchanting.compat;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class MobsToolForgingCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SWORD_BLADE = "sword_blade";
    private static final String SWORD_GUARD = "sword_guard";
    private static final String SHOVEL_HEAD = "shovel_head";
    private static final String PICKAXE_HEAD = "pickaxe_head";
    private static final String AXE_HEAD = "axe_head";
    private static final String HOE_HEAD = "hoe_head";
    private static volatile boolean reflectionAttempted;
    private static volatile boolean runtimeWarningLogged;
    private static volatile Reflection reflection;

    private MobsToolForgingCompat() {
    }

    public static List<ResourceLocation> materialItemTags(ItemStack stack) {
        Set<ResourceLocation> tags = new LinkedHashSet<>(partItemTags(stack));
        tags.addAll(materialItemTagCounts(stack).keySet());
        return List.copyOf(tags);
    }

    public static Map<ResourceLocation, Integer> materialItemTagCounts(ItemStack stack) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (ResourceLocation materialId : materialIds(stack)) {
            for (ResourceLocation tag : MaterialTagResolver.materialItemTags(materialId)) {
                counts.merge(tag, 1, Integer::sum);
            }
        }
        return Map.copyOf(counts);
    }

    public static List<ResourceLocation> materialTargetTags(ItemStack stack) {
        Set<ResourceLocation> tags = new LinkedHashSet<>(partTargetTags(stack));
        for (ResourceLocation materialId : materialIds(stack)) {
            tags.addAll(MaterialTagResolver.materialTargetTags(materialId));
        }
        return List.copyOf(tags);
    }

    public static boolean blocksFinishedToolEnchanting(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Reflection access = reflection();
        if (access == null) {
            return false;
        }

        try {
            return stack.get(access.toolConstructionComponent()) != null && !access.allowFinishedToolEnchanting();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return false;
        }
    }

    public static Optional<ResourceLocation> primaryMaterialId(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Reflection access = reflection();
        if (access == null) {
            return Optional.empty();
        }

        try {
            Object construction = stack.get(access.toolConstructionComponent());
            if (construction != null) {
                Object id = access.headMaterial().invoke(construction);
                return id instanceof ResourceLocation location ? Optional.of(location) : Optional.empty();
            }

            Object part = stack.get(access.toolPartComponent());
            if (part != null) {
                Object id = access.partMaterial().invoke(part);
                return id instanceof ResourceLocation location ? Optional.of(location) : Optional.empty();
            }

            return Optional.empty();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return Optional.empty();
        }
    }

    private static List<ResourceLocation> materialIds(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        Reflection access = reflection();
        if (access == null) {
            return List.of();
        }

        try {
            List<ResourceLocation> materials = new ArrayList<>();
            Object construction = stack.get(access.toolConstructionComponent());
            if (construction != null) {
                addLocation(materials, access.headMaterial().invoke(construction));
                addLocation(materials, access.handleMaterial().invoke(construction));
                addOptionalLocation(materials, access.bindingMaterial().invoke(construction));
                addOptionalLocation(materials, access.wrapMaterial().invoke(construction));
                addOptionalLocation(materials, access.focusMaterial().invoke(construction));
                addOptionalLocation(materials, access.treatment().invoke(construction));
                return List.copyOf(materials);
            }

            Object part = stack.get(access.toolPartComponent());
            if (part != null) {
                addLocation(materials, access.partMaterial().invoke(part));
                return List.copyOf(materials);
            }

            return List.of();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return List.of();
        }
    }

    private static void addLocation(List<ResourceLocation> materials, Object value) {
        if (value instanceof ResourceLocation location) {
            materials.add(location);
        }
    }

    private static void addOptionalLocation(List<ResourceLocation> materials, Object value) {
        if (value instanceof Optional<?> optional) {
            optional.ifPresent(entry -> addLocation(materials, entry));
            return;
        }

        addLocation(materials, value);
    }

    private static List<ResourceLocation> partTargetTags(ItemStack stack) {
        Optional<String> partType = partType(stack);
        if (partType.isEmpty()) {
            return List.of();
        }

        List<String> tagPaths = switch (partType.get()) {
            case SWORD_BLADE, SWORD_GUARD -> List.of("targets/durability", "targets/weapons", "targets/weapons/melee", "targets/weapons/swords");
            case PICKAXE_HEAD -> List.of("targets/durability", "targets/tools", "targets/tools/harvesters", "targets/tools/pickaxes");
            case AXE_HEAD -> List.of("targets/durability", "targets/tools", "targets/tools/harvesters", "targets/tools/axes", "targets/weapons", "targets/weapons/melee");
            case SHOVEL_HEAD -> List.of("targets/durability", "targets/tools", "targets/tools/harvesters", "targets/tools/shovels");
            case HOE_HEAD -> List.of("targets/durability", "targets/tools", "targets/tools/harvesters", "targets/tools/hoes");
            default -> List.of();
        };

        List<ResourceLocation> tags = new ArrayList<>(tagPaths.size());
        tagPaths.forEach(path -> tags.add(ResourceLocation.fromNamespaceAndPath("betterenchanting", path)));
        return List.copyOf(tags);
    }

    private static List<ResourceLocation> partItemTags(ItemStack stack) {
        Optional<String> partType = partType(stack);
        if (partType.isEmpty()) {
            return List.of();
        }

        List<String> tagPaths = switch (partType.get()) {
            case SWORD_BLADE, SWORD_GUARD -> List.of("durability", "weapons", "weapons/all", "weapons/melee", "weapons/swords");
            case PICKAXE_HEAD -> List.of("durability", "tools", "tools/all", "harvestable", "harvesters", "tools/harvesters", "tools/pickaxes");
            case AXE_HEAD -> List.of("durability", "tools", "tools/all", "harvestable", "harvesters", "tools/harvesters", "tools/axes", "weapons", "weapons/all", "weapons/melee");
            case SHOVEL_HEAD -> List.of("durability", "tools", "tools/all", "harvestable", "harvesters", "tools/harvesters", "tools/shovels");
            case HOE_HEAD -> List.of("durability", "tools", "tools/all", "harvestable", "harvesters", "tools/harvesters", "tools/hoes");
            default -> List.of();
        };

        List<ResourceLocation> tags = new ArrayList<>(tagPaths.size());
        tagPaths.forEach(path -> tags.add(ResourceLocation.fromNamespaceAndPath("betterenchanting", path)));
        return List.copyOf(tags);
    }

    private static Optional<String> partType(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Reflection access = reflection();
        if (access == null) {
            return Optional.empty();
        }

        try {
            Object part = stack.get(access.toolPartComponent());
            if (part == null) {
                return Optional.empty();
            }
            Object value = access.partType().invoke(part);
            return value instanceof String type ? Optional.of(type) : Optional.empty();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return Optional.empty();
        }
    }

    private static Reflection reflection() {
        if (reflectionAttempted) {
            return reflection;
        }

        synchronized (MobsToolForgingCompat.class) {
            if (reflectionAttempted) {
                return reflection;
            }

            try {
                Class<?> modDataComponentsClass = Class.forName("org.destroyermob.mobstoolforging.registry.ModDataComponents");
                Class<?> constructionClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolConstructionData");
                Class<?> partClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolPartData");

                Field toolConstructionField = modDataComponentsClass.getField("TOOL_CONSTRUCTION");
                Object toolConstructionHolder = toolConstructionField.get(null);
                if (!(toolConstructionHolder instanceof Supplier<?> toolConstructionSupplier)) {
                    throw new IllegalStateException("Mobs Tool Forging TOOL_CONSTRUCTION did not resolve to a component holder");
                }

                Object toolConstructionComponent = toolConstructionSupplier.get();
                if (!(toolConstructionComponent instanceof DataComponentType<?> toolConstructionComponentType)) {
                    throw new IllegalStateException("Mobs Tool Forging TOOL_CONSTRUCTION did not resolve to a data component type");
                }

                Field toolPartField = modDataComponentsClass.getField("TOOL_PART");
                Object toolPartHolder = toolPartField.get(null);
                if (!(toolPartHolder instanceof Supplier<?> toolPartSupplier)) {
                    throw new IllegalStateException("Mobs Tool Forging TOOL_PART did not resolve to a component holder");
                }

                Object toolPartComponent = toolPartSupplier.get();
                if (!(toolPartComponent instanceof DataComponentType<?> toolPartComponentType)) {
                    throw new IllegalStateException("Mobs Tool Forging TOOL_PART did not resolve to a data component type");
                }

                Object finishedToolEnchantingValue = null;
                Method configValueGetter = null;
                try {
                    Class<?> configClass = Class.forName("org.destroyermob.mobstoolforging.MobsToolForgingConfig");
                    Field finishedToolEnchantingField = configClass.getField("ALLOW_FINISHED_TOOL_ENCHANTING");
                    finishedToolEnchantingValue = finishedToolEnchantingField.get(null);
                    configValueGetter = finishedToolEnchantingValue.getClass().getMethod("get");
                } catch (ClassNotFoundException | NoSuchFieldException exception) {
                    LOGGER.debug("Mobs Tool Forging finished-tool enchanting config not detected; direct enchanting remains allowed");
                }


                reflection = new Reflection(
                        toolConstructionComponentType,
                        toolPartComponentType,
                        constructionClass.getMethod("headMaterial"),
                        constructionClass.getMethod("handleMaterial"),
                        constructionClass.getMethod("bindingMaterial"),
                        constructionClass.getMethod("wrapMaterial"),
                        constructionClass.getMethod("focusMaterial"),
                        constructionClass.getMethod("treatment"),
                        partClass.getMethod("partType"),
                        partClass.getMethod("materialId"),
                        finishedToolEnchantingValue,
                        configValueGetter
                );
                LOGGER.info("Enabled Mobs Tool Forging material compatibility");
            } catch (ClassNotFoundException exception) {
                LOGGER.debug("Mobs Tool Forging not detected; material compatibility disabled");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                LOGGER.warn("Mobs Tool Forging was detected, but Better Enchanting could not enable material compatibility", exception);
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
        LOGGER.warn("Failed to read Mobs Tool Forging material data for an item stack; material compatibility will be skipped for that stack", exception);
    }

    private record Reflection(
            DataComponentType<?> toolConstructionComponent,
            DataComponentType<?> toolPartComponent,
            Method headMaterial,
            Method handleMaterial,
            Method bindingMaterial,
            Method wrapMaterial,
            Method focusMaterial,
            Method treatment,
            Method partType,
            Method partMaterial,
            Object allowFinishedToolEnchantingConfig,
            Method allowFinishedToolEnchantingGetter
    ) {
        private boolean allowFinishedToolEnchanting() throws ReflectiveOperationException {
            if (allowFinishedToolEnchantingConfig == null || allowFinishedToolEnchantingGetter == null) {
                return true;
            }
            Object value = allowFinishedToolEnchantingGetter.invoke(allowFinishedToolEnchantingConfig);
            return !(value instanceof Boolean booleanValue) || booleanValue;
        }
    }
}
