package com.betterenchanting.mixin;

import com.betterenchanting.world.level.block.EnchantingTableStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin extends BaseEntityBlock {
    protected EnchantingTableBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnchantingTableStorage storage) {
                Containers.dropContents(level, pos, storage.betterenchanting$getEnchantingInventory());
                storage.betterenchanting$getEnchantingInventory().clearContent();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
