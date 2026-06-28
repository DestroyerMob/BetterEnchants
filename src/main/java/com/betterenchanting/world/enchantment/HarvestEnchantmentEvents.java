package com.betterenchanting.world.enchantment;

import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModTags;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class HarvestEnchantmentEvents {
    private HarvestEnchantmentEvents() {
    }

    public static void harvestCrops(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getUseBlock().isFalse()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isSpectator() || !player.mayBuild()) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        int harvestLevel = harvestLevel(level, tool);
        if (harvestLevel <= 0) {
            return;
        }

        int harvested = harvestArea(level, player, event.getPos(), tool, harvestLevel);
        if (harvested <= 0) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            tool.hurtAndBreak(harvested, level, player, item -> player.onEquippedItemBroken(item, EquipmentSlot.MAINHAND));
        }
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    private static int harvestArea(ServerLevel level, ServerPlayer player, BlockPos center, ItemStack tool, int harvestLevel) {
        int radius = Math.max(0, harvestLevel - 1);
        int harvested = 0;
        for (int xOffset = -radius; xOffset <= radius; xOffset++) {
            for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                BlockPos pos = center.offset(xOffset, 0, zOffset);
                if (harvestCrop(level, player, pos, tool)) {
                    harvested++;
                }
            }
        }
        return harvested;
    }

    private static boolean harvestCrop(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack tool) {
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos) || !player.mayInteract(level, pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.is(ModTags.Blocks.HARVEST_CROPS)) {
            return false;
        }

        Optional<IntegerProperty> ageProperty = ageProperty(state);
        if (ageProperty.isEmpty()) {
            return false;
        }

        IntegerProperty age = ageProperty.get();
        int matureAge = maximumAge(age);
        if (state.getValue(age) < matureAge) {
            return false;
        }

        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);
        BlockState replanted = state.setValue(age, minimumAge(age));
        level.setBlock(pos, replanted, Block.UPDATE_CLIENTS);
        level.levelEvent(2001, pos, Block.getId(state));
        boolean vacuum = VacuumEnchantmentEvents.hasVacuum(level, tool);
        for (ItemStack drop : drops) {
            ItemStack remainder = drop.copy();
            if (remainder.isEmpty()) {
                continue;
            }
            if (vacuum) {
                player.getInventory().add(remainder);
                if (remainder.isEmpty()) {
                    continue;
                }
            }
            Block.popResource(level, pos, remainder);
        }
        if (vacuum) {
            player.getInventory().setChanged();
        }
        return true;
    }

    private static Optional<IntegerProperty> ageProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && "age".equals(property.getName())) {
                return Optional.of(integerProperty);
            }
        }
        return Optional.empty();
    }

    private static int minimumAge(IntegerProperty property) {
        return property.getPossibleValues().stream().min(Comparator.naturalOrder()).orElse(0);
    }

    private static int maximumAge(IntegerProperty property) {
        return property.getPossibleValues().stream().max(Comparator.naturalOrder()).orElse(0);
    }

    private static int harvestLevel(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.HARVEST)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
