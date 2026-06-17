package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class VerdantRegrowthEnchantmentEvents {
    private VerdantRegrowthEnchantmentEvents() {
    }

    public static void repairNearGrowth(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) {
            return;
        }

        int baseInterval = BetterEnchantingConfig.verdantRegrowthBaseRepairIntervalTicks();
        int fastInterval = BetterEnchantingConfig.verdantRegrowthFastRepairIntervalTicks();
        if (player.tickCount % baseInterval != 0 && player.tickCount % fastInterval != 0) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        if (!isVerdantBiome(level, playerPos) && !hasNearbyGrowth(level, playerPos)) {
            return;
        }

        boolean accelerated = hasSunlight(level, playerPos) || level.isRainingAt(playerPos);
        int repairInterval = accelerated ? fastInterval : baseInterval;
        if (player.tickCount % repairInterval != 0) {
            return;
        }

        boolean repaired = repairStacks(level, player.getHandSlots()) | repairStacks(level, player.getArmorSlots());
        if (repaired) {
            player.getInventory().setChanged();
        }
    }

    private static boolean repairStacks(ServerLevel level, Iterable<ItemStack> stacks) {
        boolean repaired = false;
        for (ItemStack stack : stacks) {
            if (!stack.isDamageableItem() || !stack.isDamaged()) {
                continue;
            }

            int enchantmentLevel = verdantRegrowthLevel(level, stack);
            int repairAmount = enchantmentLevel * BetterEnchantingConfig.verdantRegrowthDurabilityRepairedPerLevel();
            if (repairAmount > 0) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - repairAmount));
                repaired = true;
            }
        }
        return repaired;
    }

    private static int verdantRegrowthLevel(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.VERDANT_REGROWTH)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }

    private static boolean isVerdantBiome(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(ModTags.Biomes.VERDANT_REGROWTH_BIOMES);
    }

    private static boolean hasSunlight(Level level, BlockPos pos) {
        return level.isDay() && level.canSeeSkyFromBelowWater(pos.above());
    }

    private static boolean hasNearbyGrowth(ServerLevel level, BlockPos center) {
        int horizontalRadius = BetterEnchantingConfig.verdantRegrowthScanHorizontalRadius();
        int verticalRadius = BetterEnchantingConfig.verdantRegrowthScanVerticalRadius();
        BlockPos min = center.offset(-horizontalRadius, -verticalRadius, -horizontalRadius);
        BlockPos max = center.offset(horizontalRadius, verticalRadius, horizontalRadius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (isGrowthBlock(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGrowthBlock(BlockState state) {
        return state.is(ModTags.Blocks.VERDANT_REGROWTH_GROWTH_BLOCKS);
    }
}
