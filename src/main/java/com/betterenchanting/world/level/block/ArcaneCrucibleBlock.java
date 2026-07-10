package com.betterenchanting.world.level.block;

import com.betterenchanting.registry.ModBlockEntities;
import com.betterenchanting.data.EssenceDistillationRecipes;
import com.betterenchanting.world.level.block.entity.ArcaneCrucibleBlockEntity;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ArcaneCrucibleBlock extends BaseEntityBlock {
    public static final MapCodec<ArcaneCrucibleBlock> CODEC = simpleCodec(ArcaneCrucibleBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            box(0.0D, 2.0D, 0.0D, 16.0D, 14.0D, 16.0D),
            box(0.0D, 14.0D, 0.0D, 3.0D, 16.0D, 13.0D),
            box(13.0D, 14.0D, 3.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 14.0D, 13.0D, 13.0D, 16.0D, 16.0D),
            box(3.0D, 14.0D, 0.0D, 16.0D, 16.0D, 3.0D),
            box(0.0D, 0.0D, 0.0D, 4.0D, 2.0D, 2.0D),
            box(0.0D, 0.0D, 2.0D, 2.0D, 2.0D, 4.0D),
            box(12.0D, 0.0D, 0.0D, 16.0D, 2.0D, 2.0D),
            box(14.0D, 0.0D, 2.0D, 16.0D, 2.0D, 4.0D),
            box(0.0D, 0.0D, 14.0D, 4.0D, 2.0D, 16.0D),
            box(0.0D, 0.0D, 12.0D, 2.0D, 2.0D, 14.0D),
            box(12.0D, 0.0D, 14.0D, 16.0D, 2.0D, 16.0D),
            box(14.0D, 0.0D, 12.0D, 16.0D, 2.0D, 14.0D)
    );

    public ArcaneCrucibleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneCrucibleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, ModBlockEntities.ARCANE_CRUCIBLE.get(), ArcaneCrucibleBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ArcaneCrucibleBlockEntity crucible)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int slot = insertionSlot(crucible, stack);
        if (slot < 0) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        boolean inserted = InWorldMachineInteraction.insert(crucible, slot, player, stack, stack.getMaxStackSize());
        if (inserted) {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.55F, 1.25F);
            player.displayClientMessage(Component.translatable("message.betterenchanting.machine.inserted"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.betterenchanting.machine.slot_full"), true);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ArcaneCrucibleBlockEntity crucible)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return hasStoredItems(crucible) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (player.isSecondaryUseActive()) {
            int taken = InWorldMachineInteraction.takeAll(
                    crucible,
                    player,
                    ArcaneCrucibleBlockEntity.OUTPUT_SLOT,
                    ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + 1,
                    ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT,
                    ArcaneCrucibleBlockEntity.MEDIUM_SLOT
            );
            if (taken > 0) {
                retrievalFeedback(level, pos, player, taken);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        int slot = firstOccupied(
                crucible,
                ArcaneCrucibleBlockEntity.OUTPUT_SLOT,
                ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + 1,
                ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT,
                ArcaneCrucibleBlockEntity.MEDIUM_SLOT
        );
        if (slot < 0) {
            return InteractionResult.PASS;
        }
        ItemStack taken = InWorldMachineInteraction.take(crucible, slot, player);
        retrievalFeedback(level, pos, player, 1);
        player.displayClientMessage(Component.translatable("message.betterenchanting.machine.took", taken.getHoverName()), true);
        return InteractionResult.CONSUME;
    }

    private static int insertionSlot(ArcaneCrucibleBlockEntity crucible, ItemStack stack) {
        if (EssenceDistillationRecipes.isMedium(stack)) {
            return ArcaneCrucibleBlockEntity.MEDIUM_SLOT;
        }
        if (!EssenceDistillationRecipes.isCatalyst(stack)) {
            return -1;
        }
        for (int index = 0; index < ArcaneCrucibleBlockEntity.CATALYST_SLOT_COUNT; index++) {
            int slot = ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + index;
            ItemStack stored = crucible.getItem(slot);
            if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, stack)
                    && stored.getCount() < stored.getMaxStackSize()) {
                return slot;
            }
        }
        for (int index = 0; index < ArcaneCrucibleBlockEntity.CATALYST_SLOT_COUNT; index++) {
            int slot = ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + index;
            if (crucible.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT;
    }

    private static boolean hasStoredItems(ArcaneCrucibleBlockEntity crucible) {
        return firstOccupied(crucible, 0, 1, 2, 3) >= 0;
    }

    private static int firstOccupied(ArcaneCrucibleBlockEntity crucible, int... slots) {
        for (int slot : slots) {
            if (!crucible.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static void retrievalFeedback(Level level, BlockPos pos, Player player, int stacks) {
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.65F, 0.9F);
        if (stacks > 1) {
            player.displayClientMessage(Component.translatable("message.betterenchanting.machine.cleared", stacks), true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ArcaneCrucibleBlockEntity crucible) {
                Containers.dropContents(level, pos, crucible);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
