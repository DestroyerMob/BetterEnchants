package com.betterenchanting.compat;

import com.betterenchanting.data.EnchantmentFusionRecipes;
import com.betterenchanting.data.EnchantmentLevelRules;
import com.betterenchanting.data.EnchantmentLimitRules;
import com.betterenchanting.data.PartEnchantmentRoutes;
import com.betterenchanting.data.PartEnchantmentRoutes.SlotRule;
import com.betterenchanting.registry.ModDataComponents;
import com.betterenchanting.world.EnchantmentActivationEvents;
import com.betterenchanting.world.EnchantmentActivationEvents.InactiveReason;
import com.betterenchanting.world.EnchantmentTargetTags;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public final class MobsToolForgingCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SWORD_BLADE = "sword_blade";
    private static final String SWORD_GUARD = "sword_guard";
    private static final String SHOVEL_HEAD = "shovel_head";
    private static final String PICKAXE_HEAD = "pickaxe_head";
    private static final String AXE_HEAD = "axe_head";
    private static final String HOE_HEAD = "hoe_head";
    private static final String HELMET_CHAINMAIL = "helmet_chainmail";
    private static final String HELMET_PLATE = "helmet_plate";
    private static final String CHESTPLATE_CHAINMAIL = "chestplate_chainmail";
    private static final String CHESTPLATE_BODY = "chestplate_body";
    private static final String LEGGINGS_CHAINMAIL = "leggings_chainmail";
    private static final String LEGGINGS_PLATE = "leggings_plate";
    private static final String BOOTS_CHAINMAIL = "boots_chainmail";
    private static final String BOOTS_PLATE = "boots_plate";
    private static final ResourceLocation OAK = material("oak");
    private static final ResourceLocation BLAZE = material("blaze");
    private static final ResourceLocation BREEZE = material("breeze");
    private static final ResourceLocation HANDLE_PART_TAG = ResourceLocation.fromNamespaceAndPath("mobstoolforging", "parts/handle");
    private static final String BETTER_ENCHANTING_NAMESPACE = "betterenchanting";
    private static final String TARGET_TAG_PREFIX = "targets/";
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

    public static boolean blocksDirectPartEnchanting(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Reflection access = reflection();
        if (access == null) {
            return false;
        }

        try {
            return stack.get(access.toolPartComponent()) != null
                    || stack.is(TagKey.create(Registries.ITEM, HANDLE_PART_TAG));
        } catch (LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return false;
        }
    }

    public static boolean hasRoutedParts(ItemStack stack) {
        return routedTool(stack).isPresent();
    }

    public static OptionalInt routedMaxEnchantments(ItemStack stack) {
        Optional<RoutedTool> routed = routedTool(stack);
        if (routed.isEmpty()) {
            return OptionalInt.empty();
        }

        int limit = 0;
        for (RoutedSlot slot : routed.get().slots()) {
            limit += effectiveSlotLimit(slot);
        }
        return OptionalInt.of(Math.max(0, limit));
    }

    public static List<ResourceLocation> routedTargetTags(ItemStack stack) {
        Optional<RoutedTool> routed = routedTool(stack);
        if (routed.isEmpty()) {
            return List.of();
        }

        Set<ResourceLocation> tags = new LinkedHashSet<>();
        for (RoutedSlot slot : routed.get().slots()) {
            tags.addAll(EnchantmentTargetTags.resolveForRouting(slot.stack()));
        }
        return List.copyOf(tags);
    }

    public static Set<Holder<Enchantment>> storedRoutedEnchantments(ItemStack stack) {
        Optional<RoutedTool> routed = routedTool(stack);
        if (routed.isEmpty()) {
            return Set.of();
        }

        Set<Holder<Enchantment>> enchantments = new LinkedHashSet<>();
        for (RoutedSlot slot : routed.get().slots()) {
            addEnchantments(enchantments, EnchantmentHelper.getEnchantmentsForCrafting(slot.stack()));
        }
        return Set.copyOf(enchantments);
    }

    public static boolean canApplyRoutedEnchantments(RegistryAccess registryAccess, ItemStack target, Iterable<Holder<Enchantment>> additions) {
        if (target.isEmpty()) {
            return false;
        }

        ItemStack simulated = target.copy();
        Optional<RoutedTool> routed = routedTool(simulated);
        if (routed.isEmpty()) {
            return false;
        }

        for (Holder<Enchantment> addition : additions) {
            if (!assignToBestSlot(routed.get(), addition, 1, false)) {
                return false;
            }
        }
        return true;
    }

    public static Optional<ItemStack> applyRoutedEnchantments(RegistryAccess registryAccess, ItemStack target, List<EnchantmentInstance> additions) {
        if (target.isEmpty() || additions.isEmpty()) {
            return Optional.empty();
        }

        ItemStack result = target.copy();
        Optional<RoutedTool> routed = routedTool(result);
        if (routed.isEmpty()) {
            return Optional.empty();
        }

        RoutedTool routedTool = routed.get();
        for (EnchantmentInstance addition : additions) {
            if (!assignToBestSlot(routedTool, addition.enchantment, addition.level, false)) {
                return Optional.empty();
            }
        }
        routedTool.writeParts(result);
        syncRoutedToolEnchantments(registryAccess, result);
        return Optional.of(result);
    }

    public static Optional<ItemStack> overlevelRoutedEnchantment(RegistryAccess registryAccess, ItemStack target, Holder<Enchantment> enchantment) {
        if (target.isEmpty()) {
            return Optional.empty();
        }

        ItemStack result = target.copy();
        Optional<RoutedTool> routed = routedTool(result);
        if (routed.isEmpty()) {
            return Optional.empty();
        }

        if (!assignToBestSlot(routed.get(), enchantment, EnchantmentLevelRules.overlevelMaxLevel(enchantment), true)) {
            return Optional.empty();
        }
        routed.get().writeParts(result);
        syncRoutedToolEnchantments(registryAccess, result);
        return Optional.of(result);
    }

    public static Optional<ItemStack> upgradeRoutedEnchantment(
            RegistryAccess registryAccess,
            ItemStack target,
            int partIndex,
            ResourceLocation enchantmentId,
            int requestedLevel
    ) {
        if (target.isEmpty() || partIndex < 0 || requestedLevel <= 0) {
            return Optional.empty();
        }

        ItemStack result = target.copy();
        Optional<RoutedTool> routed = routedTool(result);
        if (routed.isEmpty()) {
            return Optional.empty();
        }
        Optional<Holder.Reference<Enchantment>> enchantment = registryAccess
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId));
        if (enchantment.isEmpty()) {
            return Optional.empty();
        }

        for (RoutedSlot slot : routed.get().slots()) {
            if (slot.partIndex() != partIndex || !slotCanCarry(slot, enchantment.get())) {
                continue;
            }
            int currentLevel = EnchantmentHelper.getEnchantmentsForCrafting(slot.stack()).getLevel(enchantment.get());
            if (currentLevel <= 0 || requestedLevel <= currentLevel
                    || requestedLevel > EnchantmentLevelRules.overlevelMaxLevel(enchantment.get())) {
                return Optional.empty();
            }
            if (!setPartEnchantment(slot.stack(), enchantment.get(), requestedLevel)) {
                return Optional.empty();
            }
            routed.get().writeParts(result);
            syncRoutedToolEnchantments(registryAccess, result);
            return Optional.of(result);
        }
        return Optional.empty();
    }

    public static boolean reconcileRoutedEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Optional<RoutedTool> routed = routedTool(stack);
        if (routed.isEmpty()) {
            return false;
        }

        ItemEnchantments requested = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        ItemEnchantments routedVisible = visibleEnchantments(registryAccess, stack, routed.get());
        boolean changed = false;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : requested.entrySet()) {
            int routedLevel = routedVisible.getLevel(entry.getKey());
            if (entry.getIntValue() > routedLevel) {
                changed |= assignToBestSlot(routed.get(), entry.getKey(), entry.getIntValue(), false);
            }
        }
        if (changed) {
            routed.get().writeParts(stack);
        }
        return syncRoutedToolEnchantments(registryAccess, stack) || changed;
    }

    public static boolean syncRoutedToolEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Optional<RoutedTool> routed = routedTool(stack);
        if (routed.isEmpty()) {
            return false;
        }

        ItemEnchantments before = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        ItemEnchantments visible = visibleEnchantments(registryAccess, stack, routed.get());
        if (!before.equals(visible)) {
            EnchantmentHelper.setEnchantments(stack, visible);
        }
        EnchantmentLevelRules.clampEnchantments(stack);
        return !before.equals(EnchantmentHelper.getEnchantmentsForCrafting(stack));
    }

    public static boolean removeNonCurseRoutedEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Optional<RoutedTool> routed = routedTool(stack);
        if (routed.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (RoutedSlot slot : routed.get().slots()) {
            boolean partChanged = EnchantmentLevelRules.removeNonCurseEnchantments(slot.stack());
            if (partChanged) {
                slot.stack().remove(ModDataComponents.ROUTED_ENCHANTMENT_PRIORITY.get());
                changed = true;
            } else if (EnchantmentHelper.getEnchantmentsForCrafting(slot.stack()).isEmpty()
                    && slot.stack().has(ModDataComponents.ROUTED_ENCHANTMENT_PRIORITY.get())) {
                slot.stack().remove(ModDataComponents.ROUTED_ENCHANTMENT_PRIORITY.get());
                changed = true;
            }
        }

        if (changed) {
            routed.get().writeParts(stack);
            stack.remove(ModDataComponents.ROUTED_OVERLEVEL_BONUS_PRIORITY.get());
        }
        return syncRoutedToolEnchantments(registryAccess, stack) || changed;
    }

    public static Optional<RoutedEnchantmentBreakdown> routedEnchantmentBreakdown(RegistryAccess registryAccess, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Optional<RoutedTool> routed = routedTool(stack);
        if (routed.isEmpty()) {
            return Optional.empty();
        }

        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        Map<Holder<Enchantment>, Integer> activeEnchantments = activeRoutedEnchantments(registry, routed.get());
        Optional<ResourceLocation> selectedOverlevelBonus = selectedOverlevelBonus(stack, activeEnchantments);
        List<RoutedPartBreakdown> parts = new ArrayList<>();
        boolean hasStoredEnchantments = false;
        for (RoutedSlot slot : routed.get().slots()) {
            List<RoutedEnchantmentState> enchantments = new ArrayList<>();
            int carried = 0;
            int limit = effectiveSlotLimit(slot);
            for (EnchantmentEntry entry : orderedEntries(slot.stack(), registry)) {
                boolean canCarry = slotCanCarry(slot, entry.enchantment());
                boolean active = canCarry && carried < limit;
                if (canCarry) {
                    carried++;
                }
                boolean overleveled = EnchantmentLevelRules.isOverleveled(entry.enchantment(), entry.level());
                boolean bonusActive = active
                        && overleveled
                        && selectedOverlevelBonus.filter(enchantmentId(entry.enchantment())::equals).isPresent();
                int effectiveLevel = active
                        ? effectiveRoutedLevel(entry.enchantment(), entry.level(), bonusActive)
                        : 0;
                enchantments.add(new RoutedEnchantmentState(
                        entry.enchantment(),
                        enchantmentId(entry.enchantment()),
                        entry.level(),
                        effectiveLevel,
                        active,
                        overleveled,
                        bonusActive,
                        active ? Optional.empty() : Optional.of(canCarry ? "slot_limit" : "incompatible")
                ));
            }
            hasStoredEnchantments |= !enchantments.isEmpty();
            parts.add(new RoutedPartBreakdown(
                    slot.partIndex(),
                    slot.rule().id().or(() -> slot.rule().partType()).orElse("part_" + slot.partIndex()),
                    slot.rule().partType(),
                    limit,
                    slot.stack().copyWithCount(1),
                    List.copyOf(enchantments)
            ));
        }

        if (parts.isEmpty() || !hasStoredEnchantments) {
            return Optional.empty();
        }
        ItemEnchantments visibleEnchantments = visibleEnchantments(registryAccess, stack, routed.get());
        List<RoutedEnchantmentState> toolEnchantments = finalToolEnchantmentStates(
                registryAccess,
                stack,
                registry,
                visibleEnchantments
        );
        return Optional.of(new RoutedEnchantmentBreakdown(
                List.copyOf(parts),
                visibleEnchantments,
                toolEnchantments
        ));
    }

    public static boolean promoteRoutedEnchantment(RegistryAccess registryAccess, ItemStack stack, int partIndex, ResourceLocation enchantmentId) {
        if (stack.isEmpty()) {
            return false;
        }

        Optional<RoutedTool> routed = routedTool(stack);
        if (routed.isEmpty()) {
            return false;
        }

        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> enchantment = registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId));
        if (enchantment.isEmpty()) {
            return false;
        }

        for (RoutedSlot slot : routed.get().slots()) {
            if (slot.partIndex() != partIndex) {
                continue;
            }
            if (!slotCanCarry(slot, enchantment.get()) || EnchantmentHelper.getEnchantmentsForCrafting(slot.stack()).getLevel(enchantment.get()) <= 0) {
                return false;
            }

            Optional<EnchantmentEntryState> clicked = routedEntryState(slot, registry, enchantmentId);
            if (clicked.isEmpty()) {
                return false;
            }
            if (clicked.get().active()) {
                if (EnchantmentLevelRules.isOverleveled(clicked.get().entry().enchantment(), clicked.get().entry().level())) {
                    return selectRoutedOverlevelBonus(registryAccess, stack, routed.get(), enchantmentId);
                }
                return false;
            }

            List<ResourceLocation> priority = new ArrayList<>();
            priority.add(enchantmentId);
            for (ResourceLocation existing : routedPriority(slot.stack())) {
                if (!existing.equals(enchantmentId) && !priority.contains(existing)) {
                    priority.add(existing);
                }
            }
            for (EnchantmentEntry entry : orderedEntries(slot.stack(), registry)) {
                ResourceLocation existing = enchantmentId(entry.enchantment());
                if (!existing.equals(enchantmentId) && !priority.contains(existing)) {
                    priority.add(existing);
                }
            }

            slot.stack().set(ModDataComponents.ROUTED_ENCHANTMENT_PRIORITY.get(), List.copyOf(priority));
            routed.get().writeParts(stack);
            syncRoutedToolEnchantments(registryAccess, stack);
            return true;
        }
        return false;
    }

    public static Optional<RoutedEnchantmentBreakdown> stationRoutedEnchantmentBreakdown(Level level, BlockPos pos) {
        return stationRoutedEnchantmentPreview(level, pos)
                .flatMap(StationRoutedEnchantmentPreview::breakdown);
    }

    public static Optional<StationRoutedEnchantmentPreview> stationRoutedEnchantmentPreview(Level level, BlockPos pos) {
        if (level == null) {
            return Optional.empty();
        }

        Reflection access = reflection();
        if (access == null) {
            return Optional.empty();
        }

        Object station = level.getBlockEntity(pos);
        if (!access.toolForgeBlockEntityClass().isInstance(station)) {
            return Optional.empty();
        }

        List<ItemStack> stacks = stationBenchStacks(access, station);
        Optional<ItemStack> preview = stationFinishedTool(access, stacks);
        if (preview.isEmpty()) {
            return Optional.empty();
        }

        ItemStack toolStack = preview.get().copyWithCount(1);
        Optional<RoutedEnchantmentBreakdown> breakdown = routedEnchantmentBreakdown(level.registryAccess(), preview.get());
        if (breakdown.isPresent()) {
            return Optional.of(new StationRoutedEnchantmentPreview(toolStack, breakdown, ""));
        }

        if (toolAssemblyParts(access, preview.get()).isEmpty()) {
            return Optional.of(new StationRoutedEnchantmentPreview(toolStack, Optional.empty(), "This item has no stored part data"));
        }
        if (PartEnchantmentRoutes.routeFor(preview.get()).isEmpty()) {
            return Optional.of(new StationRoutedEnchantmentPreview(toolStack, Optional.empty(), "No enchantment route is defined for this item"));
        }
        return Optional.of(new StationRoutedEnchantmentPreview(toolStack, Optional.empty(), "No enchantments are stored on these parts"));
    }

    public static boolean promoteStationRoutedEnchantment(Level level, BlockPos pos, int partIndex, ResourceLocation enchantmentId) {
        if (level == null || level.isClientSide) {
            return false;
        }

        Reflection access = reflection();
        if (access == null) {
            return false;
        }

        Object station = level.getBlockEntity(pos);
        if (!access.toolForgeBlockEntityClass().isInstance(station)) {
            return false;
        }

        List<ItemStack> stationStacks = stationBenchStacks(access, station);
        ItemStack preview = stationFinishedTool(access, stationStacks).orElse(ItemStack.EMPTY);
        boolean promoted = partIndex < 0
                ? promoteFinalRoutedEnchantment(level.registryAccess(), preview, enchantmentId)
                : promoteRoutedEnchantment(level.registryAccess(), preview, partIndex, enchantmentId);
        if (preview.isEmpty() || !promoted) {
            return false;
        }

        return replaceToolmakerStack(access, station, 0, preview);
    }

    private static boolean replaceToolmakerStack(Reflection access, Object station, int stackIndex, ItemStack replacement) {
        try {
            Object result = access.replaceToolmakerStack().invoke(station, stackIndex, replacement);
            return result instanceof Boolean replaced && replaced;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return false;
        }
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

    private static Optional<RoutedTool> routedTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Reflection access = reflection();
        if (access == null) {
            return Optional.empty();
        }

        try {
            Object construction = stack.get(access.toolConstructionComponent());
            Object armorConstruction = stack.get(access.armorConstructionComponent());
            Object assembly = stack.get(access.toolAssemblyPartsComponent());
            if ((construction == null && armorConstruction == null) || assembly == null) {
                return Optional.empty();
            }

            Optional<PartEnchantmentRoutes.Route> route = PartEnchantmentRoutes.routeFor(stack);
            if (route.isEmpty()) {
                return Optional.empty();
            }

            List<ItemStack> parts = copyAssemblyStacks(access, assembly);
            if (parts.isEmpty()) {
                return Optional.empty();
            }

            List<RoutedSlot> slots = matchSlots(route.get(), parts);
            if (slots.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new RoutedTool(access, route.get(), parts, slots));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return Optional.empty();
        }
    }

    private static Optional<ItemStack> stationPreviewTool(Level level, BlockPos pos) {
        if (level == null) {
            return Optional.empty();
        }

        Reflection access = reflection();
        if (access == null) {
            return Optional.empty();
        }

        Object station = level.getBlockEntity(pos);
        if (!access.toolForgeBlockEntityClass().isInstance(station)) {
            return Optional.empty();
        }
        return stationPreviewTool(access, stationBenchStacks(access, station), level.registryAccess());
    }

    private static Optional<ItemStack> stationPreviewTool(Reflection access, List<ItemStack> stacks, RegistryAccess registryAccess) {
        return stationFinishedTool(access, stacks);
    }

    private static Optional<ItemStack> stationFinishedTool(Reflection access, List<ItemStack> stacks) {
        if (stacks.size() != 1) {
            return Optional.empty();
        }

        return finishedToolStack(access, stacks.get(0));
    }

    private static Optional<ItemStack> finishedToolStack(Reflection access, ItemStack stack) {
        try {
            return stack.get(access.toolConstructionComponent()) == null && stack.get(access.armorConstructionComponent()) == null
                    ? Optional.empty()
                    : Optional.of(stack.copyWithCount(1));
        } catch (LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return Optional.empty();
        }
    }

    private static List<ItemStack> stationBenchStacks(Reflection access, Object station) {
        try {
            Object value = access.toolForgeBenchStacks().invoke(station);
            if (!(value instanceof List<?> list)) {
                return List.of();
            }

            List<ItemStack> stacks = new ArrayList<>(list.size());
            for (Object entry : list) {
                if (entry instanceof ItemStack stack && !stack.isEmpty()) {
                    stacks.add(stack.copyWithCount(1));
                }
            }
            return List.copyOf(stacks);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return List.of();
        }
    }

    private static List<ItemStack> assemblyStacksFromTool(Reflection access, ItemStack stack) {
        Object assembly = toolAssemblyParts(access, stack).orElse(null);
        if (assembly == null) {
            return List.of();
        }
        try {
            return copyAssemblyStacks(access, assembly);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return List.of();
        }
    }

    private static Optional<Object> toolAssemblyParts(Reflection access, ItemStack stack) {
        try {
            Object assembly = stack.get(access.toolAssemblyPartsComponent());
            return assembly == null ? Optional.empty() : Optional.of(assembly);
        } catch (LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return Optional.empty();
        }
    }

    private static List<ItemStack> copyAssemblyStacks(Reflection access, Object assembly) throws ReflectiveOperationException {
        Object value = access.assemblyCopyStacks().invoke(assembly);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<ItemStack> stacks = new ArrayList<>(list.size());
        for (Object entry : list) {
            if (entry instanceof ItemStack stack && !stack.isEmpty()) {
                stacks.add(stack.copyWithCount(1));
            }
        }
        return stacks;
    }

    private static List<RoutedSlot> matchSlots(PartEnchantmentRoutes.Route route, List<ItemStack> parts) {
        List<RoutedSlot> slots = new ArrayList<>();
        boolean[] used = new boolean[parts.size()];
        for (SlotRule rule : route.slots()) {
            for (int index = 0; index < parts.size(); index++) {
                if (used[index]) {
                    continue;
                }

                ItemStack part = parts.get(index);
                if (rule.matches(part, partType(part))) {
                    used[index] = true;
                    slots.add(new RoutedSlot(rule, part, index));
                    break;
                }
            }
        }
        return List.copyOf(slots);
    }

    private static boolean assignToBestSlot(RoutedTool routed, Holder<Enchantment> enchantment, int level, boolean requireExisting) {
        if (level <= 0) {
            return true;
        }

        RoutedSlot existing = null;
        for (RoutedSlot slot : routed.slots()) {
            if (slotCanCarry(slot, enchantment) && EnchantmentHelper.getEnchantmentsForCrafting(slot.stack()).getLevel(enchantment) > 0) {
                existing = slot;
                break;
            }
        }
        if (existing != null) {
            return setPartEnchantment(existing.stack(), enchantment, level);
        }
        if (requireExisting) {
            return false;
        }

        for (RoutedSlot slot : routed.slots()) {
            int limit = effectiveSlotLimit(slot);
            if (limit > 0
                    && slotCanCarry(slot, enchantment)
                    && slotEnchantmentCount(slot) < limit
                    && slotFitsPrimaryTagLimits(slot, enchantment)) {
                return setPartEnchantment(slot.stack(), enchantment, level);
            }
        }
        return false;
    }

    private static boolean slotFitsPrimaryTagLimits(RoutedSlot slot, Holder<Enchantment> addition) {
        Set<Holder<Enchantment>> enchantments = new LinkedHashSet<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper
                .getEnchantmentsForCrafting(slot.stack())
                .entrySet()) {
            if (entry.getIntValue() > 0 && slotCanCarry(slot, entry.getKey())) {
                enchantments.add(entry.getKey());
            }
        }
        enchantments.add(addition);
        return EnchantmentLimitRules.fitsPrimaryTagLimits(enchantments);
    }

    private static boolean setPartEnchantment(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        int clampedLevel = EnchantmentLevelRules.clampLevel(enchantment, level);
        if (level > enchantment.value().getMaxLevel()) {
            clampedLevel = Math.min(level, EnchantmentLevelRules.overlevelMaxLevel(enchantment));
        }
        if (clampedLevel <= 0) {
            return false;
        }

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(stack));
        int existingLevel = mutable.getLevel(enchantment);
        int newLevel = Math.max(existingLevel, clampedLevel);
        if (existingLevel >= newLevel) {
            return false;
        }

        mutable.set(enchantment, newLevel);
        EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
        EnchantmentLevelRules.clampEnchantments(stack);
        return true;
    }

    private static int slotEnchantmentCount(RoutedSlot slot) {
        int count = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(slot.stack()).entrySet()) {
            if (entry.getIntValue() > 0 && slotCanCarry(slot, entry.getKey())) {
                count++;
            }
        }
        return count;
    }

    private static int effectiveSlotLimit(RoutedSlot slot) {
        return Math.max(0, slot.rule().limit() + EnchantmentLimitRules.materialCapacityBonus(slot.stack()));
    }

    private static boolean slotCanCarry(RoutedSlot slot, Holder<Enchantment> enchantment) {
        Set<ResourceLocation> enchantmentTargets = betterEnchantingTargetTags(enchantment);
        if (!enchantmentTargets.isEmpty()) {
            List<ResourceLocation> partTargets = EnchantmentTargetTags.resolveForRouting(slot.stack());
            for (ResourceLocation target : enchantmentTargets) {
                if (partTargets.contains(target)) {
                    return true;
                }
            }
            return false;
        }
        return slot.stack().supportsEnchantment(enchantment);
    }

    private static Set<ResourceLocation> betterEnchantingTargetTags(Holder<Enchantment> enchantment) {
        return enchantment.tags()
                .map(TagKey::location)
                .filter(MobsToolForgingCompat::isBetterEnchantingTargetTag)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static boolean isBetterEnchantingTargetTag(ResourceLocation tagId) {
        return BETTER_ENCHANTING_NAMESPACE.equals(tagId.getNamespace())
                && tagId.getPath().startsWith(TARGET_TAG_PREFIX);
    }

    private static ItemEnchantments visibleEnchantments(RegistryAccess registryAccess, ItemStack target, RoutedTool routed) {
        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        Map<Holder<Enchantment>, Integer> active = activeRoutedEnchantments(registry, routed);
        Optional<ResourceLocation> selectedOverlevelBonus = selectedOverlevelBonus(target, active);

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        active.forEach((enchantment, level) -> mutable.set(
                enchantment,
                effectiveRoutedLevel(enchantment, level, selectedOverlevelBonus.filter(enchantmentId(enchantment)::equals).isPresent())
        ));
        ItemStack visible = target.copy();
        EnchantmentHelper.setEnchantments(visible, mutable.toImmutable());
        EnchantmentFusionRecipes.apply(registryAccess, visible);
        EnchantmentLevelRules.clampEnchantments(visible);
        return EnchantmentHelper.getEnchantmentsForCrafting(visible);
    }

    private static List<RoutedEnchantmentState> finalToolEnchantmentStates(
            RegistryAccess registryAccess,
            ItemStack target,
            Registry<Enchantment> registry,
            ItemEnchantments visibleEnchantments
    ) {
        ItemStack evaluated = target.copy();
        EnchantmentHelper.setEnchantments(evaluated, visibleEnchantments);
        List<RoutedEnchantmentState> states = new ArrayList<>();
        for (EnchantmentEntry entry : orderedEntries(evaluated, registry)) {
            EnchantmentActivationEvents.Status status = EnchantmentActivationEvents.status(
                    evaluated,
                    entry.enchantment(),
                    registryAccess.lookupOrThrow(Registries.ENCHANTMENT)
            );
            Optional<String> inactiveReason = status.has(InactiveReason.WRONG_TAG)
                    ? Optional.of("incompatible")
                    : status.has(InactiveReason.OVER_LIMIT) ? Optional.of("item_limit") : Optional.empty();
            boolean active = status.active();
            states.add(new RoutedEnchantmentState(
                    entry.enchantment(),
                    enchantmentId(entry.enchantment()),
                    entry.level(),
                    active ? entry.level() : 0,
                    active,
                    EnchantmentLevelRules.isOverleveled(entry.enchantment(), entry.level()),
                    false,
                    inactiveReason
            ));
        }
        return List.copyOf(states);
    }

    private static boolean promoteFinalRoutedEnchantment(
            RegistryAccess registryAccess,
            ItemStack stack,
            ResourceLocation enchantmentId
    ) {
        Optional<RoutedTool> routed = routedTool(stack);
        if (routed.isEmpty()) {
            return false;
        }
        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> enchantment = registry.getHolder(
                ResourceKey.create(Registries.ENCHANTMENT, enchantmentId)
        );
        if (enchantment.isEmpty()) {
            return false;
        }
        ItemEnchantments visible = visibleEnchantments(registryAccess, stack, routed.get());
        if (visible.getLevel(enchantment.get()) <= 0) {
            return false;
        }

        List<ResourceLocation> priority = new ArrayList<>();
        priority.add(enchantmentId);
        for (ResourceLocation existing : routedPriority(stack)) {
            if (!existing.equals(enchantmentId) && !priority.contains(existing)) {
                priority.add(existing);
            }
        }
        ItemStack orderedVisible = stack.copy();
        EnchantmentHelper.setEnchantments(orderedVisible, visible);
        for (EnchantmentEntry entry : orderedEntries(orderedVisible, registry)) {
            ResourceLocation existing = enchantmentId(entry.enchantment());
            if (!priority.contains(existing)) {
                priority.add(existing);
            }
        }
        stack.set(ModDataComponents.ROUTED_ENCHANTMENT_PRIORITY.get(), List.copyOf(priority));
        return true;
    }

    private static Map<Holder<Enchantment>, Integer> activeRoutedEnchantments(Registry<Enchantment> registry, RoutedTool routed) {
        Map<Holder<Enchantment>, Integer> active = new LinkedHashMap<>();
        for (RoutedSlot slot : routed.slots()) {
            int slotCount = 0;
            int slotLimit = effectiveSlotLimit(slot);
            for (EnchantmentEntry entry : orderedEntries(slot.stack(), registry)) {
                if (!slotCanCarry(slot, entry.enchantment())) {
                    continue;
                }
                if (slotCount++ >= slotLimit) {
                    continue;
                }
                active.merge(entry.enchantment(), entry.level(), Math::max);
            }
        }
        return active;
    }

    private static Optional<ResourceLocation> selectedOverlevelBonus(ItemStack stack, Map<Holder<Enchantment>, Integer> active) {
        List<ResourceLocation> candidates = active.entrySet().stream()
                .filter(entry -> EnchantmentLevelRules.isOverleveled(entry.getKey(), entry.getValue()))
                .map(entry -> enchantmentId(entry.getKey()))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        for (ResourceLocation priority : routedOverlevelBonusPriority(stack)) {
            if (candidates.contains(priority)) {
                return Optional.of(priority);
            }
        }
        return Optional.of(candidates.getFirst());
    }

    private static int effectiveRoutedLevel(Holder<Enchantment> enchantment, int level, boolean overlevelBonusActive) {
        if (!EnchantmentLevelRules.isOverleveled(enchantment, level)) {
            return EnchantmentLevelRules.clampLevel(enchantment, level);
        }
        return overlevelBonusActive
                ? EnchantmentLevelRules.effectiveLevel(enchantment, level)
                : EnchantmentLevelRules.maxLevel(enchantment);
    }

    private static boolean selectRoutedOverlevelBonus(RegistryAccess registryAccess, ItemStack stack, RoutedTool routed, ResourceLocation enchantmentId) {
        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        Map<Holder<Enchantment>, Integer> active = activeRoutedEnchantments(registry, routed);
        if (active.entrySet().stream().noneMatch(entry -> enchantmentId(entry.getKey()).equals(enchantmentId)
                && EnchantmentLevelRules.isOverleveled(entry.getKey(), entry.getValue()))) {
            return false;
        }

        Optional<ResourceLocation> current = selectedOverlevelBonus(stack, active);
        if (current.filter(enchantmentId::equals).isPresent()) {
            return false;
        }

        List<ResourceLocation> priority = new ArrayList<>();
        priority.add(enchantmentId);
        for (ResourceLocation existing : routedOverlevelBonusPriority(stack)) {
            if (!existing.equals(enchantmentId) && !priority.contains(existing)) {
                priority.add(existing);
            }
        }
        active.entrySet().stream()
                .filter(entry -> EnchantmentLevelRules.isOverleveled(entry.getKey(), entry.getValue()))
                .map(entry -> enchantmentId(entry.getKey()))
                .filter(existing -> !existing.equals(enchantmentId) && !priority.contains(existing))
                .forEach(priority::add);

        stack.set(ModDataComponents.ROUTED_OVERLEVEL_BONUS_PRIORITY.get(), List.copyOf(priority));
        syncRoutedToolEnchantments(registryAccess, stack);
        return true;
    }

    private static Optional<EnchantmentEntryState> routedEntryState(RoutedSlot slot, Registry<Enchantment> registry, ResourceLocation enchantmentId) {
        int carried = 0;
        int limit = effectiveSlotLimit(slot);
        for (EnchantmentEntry entry : orderedEntries(slot.stack(), registry)) {
            boolean canCarry = slotCanCarry(slot, entry.enchantment());
            boolean active = canCarry && carried < limit;
            if (canCarry) {
                carried++;
            }
            if (enchantmentId(entry.enchantment()).equals(enchantmentId)) {
                return canCarry ? Optional.of(new EnchantmentEntryState(entry, active)) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static List<EnchantmentEntry> orderedEntries(ItemStack stack, Registry<Enchantment> registry) {
        ItemEnchantments source = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (source.isEmpty()) {
            return List.of();
        }

        List<EnchantmentEntry> entries = new ArrayList<>();
        Set<Holder<Enchantment>> added = new HashSet<>();
        for (ResourceLocation priority : routedPriority(stack)) {
            registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, priority))
                    .ifPresent(enchantment -> addOrderedEntry(entries, added, source, enchantment));
        }
        Optional<HolderSet.Named<Enchantment>> ordered = registry.getTag(EnchantmentTags.TOOLTIP_ORDER);
        if (ordered.isPresent()) {
            for (Holder<Enchantment> enchantment : ordered.get()) {
                addOrderedEntry(entries, added, source, enchantment);
            }
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : source.entrySet()) {
            if (entry.getIntValue() > 0 && added.add(entry.getKey())) {
                entries.add(new EnchantmentEntry(entry.getKey(), entry.getIntValue()));
            }
        }
        return List.copyOf(entries);
    }

    private static void addOrderedEntry(List<EnchantmentEntry> entries, Set<Holder<Enchantment>> added, ItemEnchantments source, Holder<Enchantment> enchantment) {
        int level = source.getLevel(enchantment);
        if (level > 0 && added.add(enchantment)) {
            entries.add(new EnchantmentEntry(enchantment, level));
        }
    }

    private static List<ResourceLocation> routedPriority(ItemStack stack) {
        List<ResourceLocation> priority = stack.get(ModDataComponents.ROUTED_ENCHANTMENT_PRIORITY.get());
        return priority == null ? List.of() : priority;
    }

    private static List<ResourceLocation> routedOverlevelBonusPriority(ItemStack stack) {
        List<ResourceLocation> priority = stack.get(ModDataComponents.ROUTED_OVERLEVEL_BONUS_PRIORITY.get());
        return priority == null ? List.of() : priority;
    }

    private static ResourceLocation enchantmentId(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey()
                .map(ResourceKey::location)
                .orElseGet(() -> ResourceLocation.fromNamespaceAndPath(BETTER_ENCHANTING_NAMESPACE, "unknown"));
    }

    private static void addEnchantments(Set<Holder<Enchantment>> output, ItemEnchantments source) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : source.entrySet()) {
            if (entry.getIntValue() > 0) {
                output.add(entry.getKey());
            }
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

        Optional<ResourceLocation> handleMaterial = handleMaterialId(stack);
        if (handleMaterial.isPresent()) {
            return handleMaterial;
        }

        try {
            Object construction = stack.get(access.toolConstructionComponent());
            if (construction != null) {
                Object id = access.headMaterial().invoke(construction);
                return id instanceof ResourceLocation location ? Optional.of(location) : Optional.empty();
            }

            Object armorConstruction = stack.get(access.armorConstructionComponent());
            if (armorConstruction != null) {
                Object id = access.armorChainmailMaterial().invoke(armorConstruction);
                return id instanceof ResourceLocation location ? Optional.of(location) : Optional.empty();
            }

            Object part = stack.get(access.toolPartComponent());
            if (part != null) {
                Object id = access.toolPartMaterial().invoke(part);
                return id instanceof ResourceLocation location ? Optional.of(location) : Optional.empty();
            }

            Object armorPart = stack.get(access.armorPartComponent());
            if (armorPart != null) {
                Object id = access.armorPartMaterial().invoke(armorPart);
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

        Optional<ResourceLocation> handleMaterial = handleMaterialId(stack);
        if (handleMaterial.isPresent()) {
            return List.of(handleMaterial.get());
        }

        try {
            List<ResourceLocation> materials = new ArrayList<>();
            Object construction = stack.get(access.toolConstructionComponent());
            if (construction != null) {
                boolean addedAssemblyParts = addAssemblyPartMaterialIds(materials, access, stack);
                if (!addedAssemblyParts) {
                    addOptionalLocation(materials, access.headBaseMaterial(), construction);
                    addLocation(materials, access.headMaterial().invoke(construction));
                    addOptionalLocation(materials, access.guardMaterial(), construction);
                }
                addLocation(materials, access.handleMaterial().invoke(construction));
                addOptionalLocation(materials, access.bindingMaterial().invoke(construction));
                addOptionalLocation(materials, access.wrapMaterial().invoke(construction));
                addOptionalLocation(materials, access.focusMaterial().invoke(construction));
                addOptionalLocation(materials, access.treatment().invoke(construction));
                return List.copyOf(materials);
            }

            Object armorConstruction = stack.get(access.armorConstructionComponent());
            if (armorConstruction != null) {
                boolean addedAssemblyParts = addAssemblyPartMaterialIds(materials, access, stack);
                if (!addedAssemblyParts) {
                    addLocation(materials, access.armorChainmailMaterial().invoke(armorConstruction));
                    addOptionalLocation(materials, access.armorOverlayBaseMaterial(), armorConstruction);
                    addOptionalLocation(materials, access.armorOverlayMaterial().invoke(armorConstruction));
                }
                return List.copyOf(materials);
            }

            Object part = stack.get(access.toolPartComponent());
            if (part != null) {
                addOptionalLocation(materials, access.toolPartCoatingBaseMaterial(), part);
                addLocation(materials, access.toolPartMaterial().invoke(part));
                return List.copyOf(materials);
            }

            Object armorPart = stack.get(access.armorPartComponent());
            if (armorPart != null) {
                addOptionalLocation(materials, access.armorPartCoatingBaseMaterial(), armorPart);
                addLocation(materials, access.armorPartMaterial().invoke(armorPart));
                return List.copyOf(materials);
            }

            return List.of();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return List.of();
        }
    }

    private static boolean addAssemblyPartMaterialIds(List<ResourceLocation> materials, Reflection access, ItemStack stack) throws ReflectiveOperationException {
        Object assembly = stack.get(access.toolAssemblyPartsComponent());
        if (assembly == null) {
            return false;
        }
        boolean added = false;
        for (ItemStack partStack : copyAssemblyStacks(access, assembly)) {
            Object part = partStack.get(access.toolPartComponent());
            if (part != null) {
                addOptionalLocation(materials, access.toolPartCoatingBaseMaterial(), part);
                addLocation(materials, access.toolPartMaterial().invoke(part));
                added = true;
                continue;
            }

            Object armorPart = partStack.get(access.armorPartComponent());
            if (armorPart != null) {
                addOptionalLocation(materials, access.armorPartCoatingBaseMaterial(), armorPart);
                addLocation(materials, access.armorPartMaterial().invoke(armorPart));
                added = true;
            }
        }
        return added;
    }

    private static Optional<ResourceLocation> handleMaterialId(ItemStack stack) {
        if (stack.is(Items.STICK)) {
            return Optional.of(OAK);
        }
        if (stack.is(Items.BLAZE_ROD)) {
            return Optional.of(BLAZE);
        }
        if (stack.is(Items.BREEZE_ROD)) {
            return Optional.of(BREEZE);
        }
        return Optional.empty();
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

    private static void addOptionalLocation(List<ResourceLocation> materials, Method method, Object target) throws ReflectiveOperationException {
        if (method != null) {
            addOptionalLocation(materials, method.invoke(target));
        }
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
            case HELMET_CHAINMAIL, HELMET_PLATE -> List.of("targets/durability", "targets/armor", "targets/armor/helmets");
            case CHESTPLATE_CHAINMAIL, CHESTPLATE_BODY -> List.of("targets/durability", "targets/armor", "targets/armor/body_armor");
            case LEGGINGS_CHAINMAIL, LEGGINGS_PLATE -> List.of("targets/durability", "targets/armor", "targets/armor/leggings");
            case BOOTS_CHAINMAIL, BOOTS_PLATE -> List.of("targets/durability", "targets/armor", "targets/armor/boots");
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
            case HELMET_CHAINMAIL, HELMET_PLATE -> List.of("durability", "armor", "armour", "armor/all", "armour/all", "armor/helmets", "armour/helmets");
            case CHESTPLATE_CHAINMAIL, CHESTPLATE_BODY -> List.of("durability", "armor", "armour", "armor/all", "armour/all", "armor/body_armor", "armour/body_armor", "armor/chestplates");
            case LEGGINGS_CHAINMAIL, LEGGINGS_PLATE -> List.of("durability", "armor", "armour", "armor/all", "armour/all", "armor/leggings", "armour/leggings");
            case BOOTS_CHAINMAIL, BOOTS_PLATE -> List.of("durability", "armor", "armour", "armor/all", "armour/all", "armor/boots", "armour/boots");
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
            if (part != null) {
                Object value = access.toolPartType().invoke(part);
                return value instanceof String type ? Optional.of(type) : Optional.empty();
            }

            Object armorPart = stack.get(access.armorPartComponent());
            if (armorPart == null) {
                return Optional.empty();
            }
            Object value = access.armorPartType().invoke(armorPart);
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
                Class<?> assemblyPartsClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolAssemblyParts");
                Class<?> partClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolPartData");
                Class<?> armorConstructionClass = Class.forName("org.destroyermob.mobstoolforging.world.ArmorConstructionData");
                Class<?> armorPartClass = Class.forName("org.destroyermob.mobstoolforging.world.ArmorPartData");
                Class<?> toolForgeBlockEntityClass = Class.forName("org.destroyermob.mobstoolforging.world.ToolForgeBlockEntity");

                Field toolConstructionField = modDataComponentsClass.getField("TOOL_CONSTRUCTION");
                Object toolConstructionHolder = toolConstructionField.get(null);
                if (!(toolConstructionHolder instanceof Supplier<?> toolConstructionSupplier)) {
                    throw new IllegalStateException("Mobs Tool Forging TOOL_CONSTRUCTION did not resolve to a component holder");
                }

                Object toolConstructionComponent = toolConstructionSupplier.get();
                if (!(toolConstructionComponent instanceof DataComponentType<?> toolConstructionComponentType)) {
                    throw new IllegalStateException("Mobs Tool Forging TOOL_CONSTRUCTION did not resolve to a data component type");
                }

                Field toolAssemblyPartsField = modDataComponentsClass.getField("TOOL_ASSEMBLY_PARTS");
                Object toolAssemblyPartsHolder = toolAssemblyPartsField.get(null);
                if (!(toolAssemblyPartsHolder instanceof Supplier<?> toolAssemblyPartsSupplier)) {
                    throw new IllegalStateException("Mobs Tool Forging TOOL_ASSEMBLY_PARTS did not resolve to a component holder");
                }

                Object toolAssemblyPartsComponent = toolAssemblyPartsSupplier.get();
                if (!(toolAssemblyPartsComponent instanceof DataComponentType<?> toolAssemblyPartsComponentType)) {
                    throw new IllegalStateException("Mobs Tool Forging TOOL_ASSEMBLY_PARTS did not resolve to a data component type");
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

                Field armorConstructionField = modDataComponentsClass.getField("ARMOR_CONSTRUCTION");
                Object armorConstructionHolder = armorConstructionField.get(null);
                if (!(armorConstructionHolder instanceof Supplier<?> armorConstructionSupplier)) {
                    throw new IllegalStateException("Mobs Tool Forging ARMOR_CONSTRUCTION did not resolve to a component holder");
                }

                Object armorConstructionComponent = armorConstructionSupplier.get();
                if (!(armorConstructionComponent instanceof DataComponentType<?> armorConstructionComponentType)) {
                    throw new IllegalStateException("Mobs Tool Forging ARMOR_CONSTRUCTION did not resolve to a data component type");
                }

                Field armorPartField = modDataComponentsClass.getField("ARMOR_PART");
                Object armorPartHolder = armorPartField.get(null);
                if (!(armorPartHolder instanceof Supplier<?> armorPartSupplier)) {
                    throw new IllegalStateException("Mobs Tool Forging ARMOR_PART did not resolve to a component holder");
                }

                Object armorPartComponent = armorPartSupplier.get();
                if (!(armorPartComponent instanceof DataComponentType<?> armorPartComponentType)) {
                    throw new IllegalStateException("Mobs Tool Forging ARMOR_PART did not resolve to a data component type");
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
                        toolAssemblyPartsComponentType,
                        toolPartComponentType,
                        armorConstructionComponentType,
                        armorPartComponentType,
                        assemblyPartsClass.getMethod("copyStacks"),
                        assemblyPartsClass.getMethod("from", List.class),
                        constructionClass.getMethod("headMaterial"),
                        optionalMethod(constructionClass, "headBaseMaterial"),
                        constructionClass.getMethod("handleMaterial"),
                        optionalMethod(constructionClass, "guardMaterial"),
                        constructionClass.getMethod("bindingMaterial"),
                        constructionClass.getMethod("wrapMaterial"),
                        constructionClass.getMethod("focusMaterial"),
                        constructionClass.getMethod("treatment"),
                        partClass.getMethod("partType"),
                        partClass.getMethod("materialId"),
                        optionalMethod(partClass, "coatingBaseMaterial"),
                        armorConstructionClass.getMethod("chainmailMaterial"),
                        armorConstructionClass.getMethod("overlayMaterial"),
                        optionalMethod(armorConstructionClass, "overlayBaseMaterial"),
                        armorPartClass.getMethod("partType"),
                        armorPartClass.getMethod("materialId"),
                        optionalMethod(armorPartClass, "coatingBaseMaterial"),
                        toolForgeBlockEntityClass,
                        toolForgeBlockEntityClass.getMethod("benchStacks"),
                        toolForgeBlockEntityClass.getMethod("replaceToolmakerAssemblyStack", int.class, ItemStack.class),
                        toolForgeBlockEntityClass.getMethod("replaceToolmakerStack", int.class, ItemStack.class),
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

    private static Method optionalMethod(Class<?> owner, String name) {
        try {
            return owner.getMethod(name);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static void logRuntimeWarning(Throwable exception) {
        if (runtimeWarningLogged) {
            return;
        }
        runtimeWarningLogged = true;
        LOGGER.warn("Failed to read Mobs Tool Forging material data for an item stack; material compatibility will be skipped for that stack", exception);
    }

    private static ResourceLocation material(String path) {
        return ResourceLocation.fromNamespaceAndPath("mobstoolforging", path);
    }

    private record RoutedTool(
            Reflection access,
            PartEnchantmentRoutes.Route route,
            List<ItemStack> parts,
            List<RoutedSlot> slots
    ) {
        private void writeParts(ItemStack stack) {
            try {
                Object assemblyParts = access.assemblyFromStacks().invoke(null, parts);
                setComponent(stack, access.toolAssemblyPartsComponent(), assemblyParts);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                logRuntimeWarning(exception);
            }
        }
    }

    private record RoutedSlot(SlotRule rule, ItemStack stack, int partIndex) {
    }

    private record EnchantmentEntry(Holder<Enchantment> enchantment, int level) {
    }

    private record EnchantmentEntryState(EnchantmentEntry entry, boolean active) {
    }

    public record RoutedEnchantmentBreakdown(
            List<RoutedPartBreakdown> parts,
            ItemEnchantments visibleEnchantments,
            List<RoutedEnchantmentState> toolEnchantments
    ) {
    }

    public record StationRoutedEnchantmentPreview(ItemStack toolStack, Optional<RoutedEnchantmentBreakdown> breakdown, String status) {
    }

    public record RoutedPartBreakdown(int partIndex, String slotId, Optional<String> partType, int limit, ItemStack partStack, List<RoutedEnchantmentState> enchantments) {
    }

    public record RoutedEnchantmentState(
            Holder<Enchantment> enchantment,
            ResourceLocation enchantmentId,
            int level,
            int effectiveLevel,
            boolean active,
            boolean overleveled,
            boolean overlevelBonusActive,
            Optional<String> inactiveReason
    ) {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setComponent(ItemStack stack, DataComponentType<?> component, Object value) {
        stack.set((DataComponentType) component, value);
    }

    private record Reflection(
            DataComponentType<?> toolConstructionComponent,
            DataComponentType<?> toolAssemblyPartsComponent,
            DataComponentType<?> toolPartComponent,
            DataComponentType<?> armorConstructionComponent,
            DataComponentType<?> armorPartComponent,
            Method assemblyCopyStacks,
            Method assemblyFromStacks,
            Method headMaterial,
            Method headBaseMaterial,
            Method handleMaterial,
            Method guardMaterial,
            Method bindingMaterial,
            Method wrapMaterial,
            Method focusMaterial,
            Method treatment,
            Method toolPartType,
            Method toolPartMaterial,
            Method toolPartCoatingBaseMaterial,
            Method armorChainmailMaterial,
            Method armorOverlayMaterial,
            Method armorOverlayBaseMaterial,
            Method armorPartType,
            Method armorPartMaterial,
            Method armorPartCoatingBaseMaterial,
            Class<?> toolForgeBlockEntityClass,
            Method toolForgeBenchStacks,
            Method replaceToolmakerAssemblyStack,
            Method replaceToolmakerStack,
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
