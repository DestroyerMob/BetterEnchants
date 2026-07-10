package com.betterenchanting.world.level.block;

import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import com.betterenchanting.data.EssenceDefinitions;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AttunementPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<AttunementPedestalBlock> CODEC = simpleCodec(AttunementPedestalBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            box(3.0D, 0.0D, 3.0D, 13.0D, 2.0D, 13.0D),
            box(6.0D, 2.0D, 6.0D, 10.0D, 9.0D, 10.0D),
            box(0.0D, 9.0D, 0.0D, 16.0D, 12.0D, 16.0D)
    );

    public AttunementPedestalBlock(BlockBehaviour.Properties properties) {
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
        return new AttunementPedestalBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AttunementPedestalBlockEntity pedestal)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int slot = insertionSlot(stack);
        if (slot < 0) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        int limit = slot == AttunementPedestalBlockEntity.TARGET_SLOT ? 1 : stack.getMaxStackSize();
        boolean inserted = InWorldMachineInteraction.insert(pedestal, slot, player, stack, limit);
        if (inserted) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.15F);
            player.displayClientMessage(Component.translatable("message.betterenchanting.machine.inserted"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.betterenchanting.machine.slot_full"), true);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AttunementPedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return pedestal.isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            int taken = InWorldMachineInteraction.takeAll(
                    pedestal,
                    player,
                    AttunementPedestalBlockEntity.TARGET_SLOT,
                    AttunementPedestalBlockEntity.CATALYST_SLOT,
                    AttunementPedestalBlockEntity.ESSENCE_SLOT
            );
            if (taken <= 0) {
                return InteractionResult.PASS;
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.65F, 0.9F);
            player.displayClientMessage(Component.translatable("message.betterenchanting.machine.cleared", taken), true);
            return InteractionResult.CONSUME;
        }
        int slot = firstOccupied(
                pedestal,
                AttunementPedestalBlockEntity.TARGET_SLOT,
                AttunementPedestalBlockEntity.CATALYST_SLOT,
                AttunementPedestalBlockEntity.ESSENCE_SLOT
        );
        if (slot < 0) {
            return InteractionResult.PASS;
        }
        ItemStack taken = InWorldMachineInteraction.take(pedestal, slot, player);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.65F, 0.9F);
        player.displayClientMessage(Component.translatable("message.betterenchanting.machine.took", taken.getHoverName()), true);
        return InteractionResult.CONSUME;
    }

    private static int insertionSlot(ItemStack stack) {
        if (EssenceDefinitions.isEssence(stack)) {
            return AttunementPedestalBlockEntity.ESSENCE_SLOT;
        }
        if (stack.is(Items.NETHER_STAR)) {
            return AttunementPedestalBlockEntity.CATALYST_SLOT;
        }
        return !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()
                ? AttunementPedestalBlockEntity.TARGET_SLOT
                : -1;
    }

    private static int firstOccupied(AttunementPedestalBlockEntity pedestal, int... slots) {
        for (int slot : slots) {
            if (!pedestal.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AttunementPedestalBlockEntity pedestal) {
                Containers.dropContents(level, pos, pedestal);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
