package com.betterenchanting.world.inventory;

import com.betterenchanting.compat.ApothicEnchantingCompat;
import com.betterenchanting.compat.MobsToolForgingCompat;
import com.betterenchanting.data.EnchantmentLevelRules;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.world.level.block.EnchantingTablePower;
import com.betterenchanting.world.level.block.EnchantingTableStorage;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class PedestalUpgradeRules {
    public static final int TABLE_SEARCH_HORIZONTAL = 4;
    public static final int TABLE_SEARCH_VERTICAL = 2;

    private PedestalUpgradeRules() {
    }

    public static UpgradePlan plan(
            Level level,
            BlockPos pedestalPos,
            ItemStack target,
            int partIndex,
            ResourceLocation enchantmentId,
            ItemStack essence,
            ItemStack catalyst
    ) {
        Optional<Holder.Reference<Enchantment>> selected = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId));
        if (target.isEmpty() || selected.isEmpty()) {
            return invalidPlan(enchantmentId, partIndex);
        }

        Holder<Enchantment> enchantment = selected.get();
        int currentLevel = selectedLevel(level, target, partIndex, enchantmentId, enchantment);
        if (currentLevel <= 0) {
            return invalidPlan(enchantmentId, partIndex);
        }

        int maximumLevel = EnchantmentLevelRules.maxLevel(enchantment);
        boolean alreadyOverleveled = currentLevel > maximumLevel;
        boolean overlevel = currentLevel == maximumLevel && maximumLevel > 1 && !hasAnyOverlevel(level, target);
        boolean normalUpgrade = currentLevel < maximumLevel;
        int nextLevel = normalUpgrade || overlevel ? currentLevel + 1 : currentLevel;
        int essenceCost = normalUpgrade || overlevel ? nextLevel : 0;
        boolean catalystRequired = overlevel;
        boolean hasCatalyst = !catalystRequired || catalyst.is(Items.NETHER_STAR);
        Optional<BlockPos> tablePos = nearestEnchantingTable(level, pedestalPos);
        ItemStack tableEssence = tablePos.map(pos -> linkedTableEssence(level, pos)).orElse(ItemStack.EMPTY);
        ResourceLocation requiredEssenceItem = requiredEssenceItem(enchantment, essence, tableEssence);
        int availableEssence = matchingCount(essence, requiredEssenceItem)
                + matchingCount(tableEssence, requiredEssenceItem);
        boolean matchingEssence = availableEssence > 0;
        boolean enoughEssence = matchingEssence && availableEssence >= essenceCost;
        int availablePower = tablePos.map(pos -> availablePower(level, pos, target)).orElse(0);
        int requiredPower = normalUpgrade || overlevel ? Math.min(30, Math.max(1, nextLevel * 3)) : 0;
        boolean enoughPower = tablePos.isPresent() && availablePower >= requiredPower;
        boolean validSelection = normalUpgrade || overlevel;

        return new UpgradePlan(
                enchantmentId,
                partIndex,
                enchantment,
                currentLevel,
                nextLevel,
                maximumLevel,
                essenceCost,
                requiredEssenceItem,
                availableEssence,
                requiredPower,
                availablePower,
                validSelection,
                matchingEssence,
                enoughEssence,
                tablePos.isPresent(),
                enoughPower,
                catalystRequired,
                hasCatalyst,
                overlevel,
                alreadyOverleveled || !validSelection
        );
    }

    public static boolean canSelect(Level level, ItemStack target, int partIndex, ResourceLocation enchantmentId) {
        Optional<Holder.Reference<Enchantment>> selected = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId));
        return selected.map(enchantment -> selectedLevel(level, target, partIndex, enchantmentId, enchantment) > 0).orElse(false);
    }

    public static UpgradePlan invalidPlan(ResourceLocation enchantmentId, int partIndex) {
        return UpgradePlan.invalid(enchantmentId, partIndex);
    }

    private static int selectedLevel(
            Level level,
            ItemStack target,
            int partIndex,
            ResourceLocation enchantmentId,
            Holder<Enchantment> enchantment
    ) {
        if (partIndex >= 0) {
            return MobsToolForgingCompat.routedEnchantmentBreakdown(level.registryAccess(), target)
                    .flatMap(breakdown -> breakdown.parts().stream()
                            .filter(part -> part.partIndex() == partIndex)
                            .flatMap(part -> part.enchantments().stream())
                            .filter(state -> state.enchantmentId().equals(enchantmentId))
                            .findFirst())
                    .map(MobsToolForgingCompat.RoutedEnchantmentState::level)
                    .orElse(0);
        }
        return EnchantmentHelper.getEnchantmentsForCrafting(target).getLevel(enchantment);
    }

    private static boolean hasAnyOverlevel(Level level, ItemStack target) {
        if (EnchantmentLevelRules.hasOverleveledEnchantment(target)) {
            return true;
        }
        return MobsToolForgingCompat.routedEnchantmentBreakdown(level.registryAccess(), target)
                .map(breakdown -> breakdown.parts().stream()
                        .flatMap(part -> part.enchantments().stream())
                        .anyMatch(MobsToolForgingCompat.RoutedEnchantmentState::overleveled))
                .orElse(false);
    }

    public static void consumeEssence(Level level, BlockPos pedestalPos, ItemStack pedestalEssence,
                                      UpgradePlan plan) {
        int remaining = consumeFrom(pedestalEssence, plan.requiredEssenceItem(), plan.essenceCost());
        if (remaining <= 0) {
            return;
        }
        nearestEnchantingTable(level, pedestalPos).ifPresent(tablePos -> {
            if (!(level.getBlockEntity(tablePos) instanceof EnchantingTableStorage storage)) {
                return;
            }
            SimpleContainer inventory = storage.betterenchanting$getEnchantingInventory();
            ItemStack tableEssence = inventory.getItem(EnhancedEnchantingMenu.REAGENT_SLOT);
            if (consumeFrom(tableEssence, plan.requiredEssenceItem(), remaining) < remaining) {
                inventory.setChanged();
            }
        });
    }

    private static ResourceLocation requiredEssenceItem(Holder<Enchantment> enchantment,
                                                         ItemStack pedestalEssence, ItemStack tableEssence) {
        boolean pedestalMatches = EssenceDefinitions.matches(enchantment, pedestalEssence);
        boolean tableMatches = EssenceDefinitions.matches(enchantment, tableEssence);
        if (pedestalMatches && tableMatches) {
            ResourceLocation pedestalItem = BuiltInRegistries.ITEM.getKey(pedestalEssence.getItem());
            ResourceLocation tableItem = BuiltInRegistries.ITEM.getKey(tableEssence.getItem());
            if (pedestalItem.equals(tableItem) || pedestalEssence.getCount() >= tableEssence.getCount()) {
                return pedestalItem;
            }
            return tableItem;
        }
        if (pedestalMatches) {
            return BuiltInRegistries.ITEM.getKey(pedestalEssence.getItem());
        }
        if (tableMatches) {
            return BuiltInRegistries.ITEM.getKey(tableEssence.getItem());
        }
        return EssenceDefinitions.matching(enchantment)
                .map(definition -> definition.item())
                .orElse(ResourceLocation.withDefaultNamespace("air"));
    }

    private static ItemStack linkedTableEssence(Level level, BlockPos tablePos) {
        if (!(level.getBlockEntity(tablePos) instanceof EnchantingTableStorage storage)) {
            return ItemStack.EMPTY;
        }
        return storage.betterenchanting$getEnchantingInventory().getItem(EnhancedEnchantingMenu.REAGENT_SLOT);
    }

    private static int matchingCount(ItemStack stack, ResourceLocation requiredEssenceItem) {
        return stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(requiredEssenceItem)
                ? 0
                : stack.getCount();
    }

    private static int consumeFrom(ItemStack stack, ResourceLocation requiredEssenceItem, int amount) {
        int consumed = Math.min(amount, matchingCount(stack, requiredEssenceItem));
        stack.shrink(consumed);
        return amount - consumed;
    }

    public static Optional<BlockPos> nearestEnchantingTable(Level level, BlockPos pedestalPos) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -TABLE_SEARCH_HORIZONTAL; x <= TABLE_SEARCH_HORIZONTAL; x++) {
            for (int y = -TABLE_SEARCH_VERTICAL; y <= TABLE_SEARCH_VERTICAL; y++) {
                for (int z = -TABLE_SEARCH_HORIZONTAL; z <= TABLE_SEARCH_HORIZONTAL; z++) {
                    cursor.set(pedestalPos.getX() + x, pedestalPos.getY() + y, pedestalPos.getZ() + z);
                    if (!level.getBlockState(cursor).is(Blocks.ENCHANTING_TABLE)) {
                        continue;
                    }
                    double distance = pedestalPos.distSqr(cursor);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = cursor.immutable();
                    }
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static int availablePower(Level level, BlockPos tablePos, ItemStack target) {
        return ApothicEnchantingCompat.gatherTableStats(level, tablePos, target)
                .map(ApothicEnchantingCompat.TableStats::bookshelfPower)
                .orElseGet(() -> Math.max(0, Math.round(EnchantingTablePower.bookshelfPower(level, tablePos))));
    }

    public record UpgradePlan(
            ResourceLocation enchantmentId,
            int partIndex,
            Holder<Enchantment> enchantment,
            int currentLevel,
            int nextLevel,
            int maximumLevel,
            int essenceCost,
            ResourceLocation requiredEssenceItem,
            int availableEssence,
            int requiredPower,
            int availablePower,
            boolean validSelection,
            boolean matchingEssence,
            boolean enoughEssence,
            boolean linkedTable,
            boolean enoughPower,
            boolean catalystRequired,
            boolean hasCatalyst,
            boolean overlevel,
            boolean maximumReached
    ) {
        private static UpgradePlan invalid(ResourceLocation id, int partIndex) {
            return new UpgradePlan(
                    id,
                    partIndex,
                    null,
                    0,
                    0,
                    0,
                    0,
                    ResourceLocation.withDefaultNamespace("air"),
                    0,
                    0,
                    0,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true
            );
        }

        public boolean canUpgrade() {
            return validSelection && enoughEssence && linkedTable && enoughPower && hasCatalyst;
        }
    }
}
