package com.betterenchanting.world.level.block.entity;

import com.betterenchanting.data.EssenceDistillationRecipes;
import com.betterenchanting.data.EssenceDistillationRecipes.Recipe;
import com.betterenchanting.registry.ModBlockEntities;
import com.betterenchanting.world.inventory.ArcaneCrucibleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;

public final class ArcaneCrucibleBlockEntity extends BaseContainerBlockEntity {
    public static final int MEDIUM_SLOT = 0;
    public static final int FIRST_CATALYST_SLOT = 1;
    public static final int CATALYST_SLOT_COUNT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int CONTAINER_SIZE = 4;
    public static final int DISTILLATION_TIME = 80;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int progress;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? ArcaneCrucibleBlockEntity.this.progress : DISTILLATION_TIME;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                ArcaneCrucibleBlockEntity.this.progress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ArcaneCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_CRUCIBLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneCrucibleBlockEntity crucible) {
        Recipe recipe = EssenceDistillationRecipes.find(
                crucible.items.get(MEDIUM_SLOT),
                crucible.items.get(FIRST_CATALYST_SLOT),
                crucible.items.get(FIRST_CATALYST_SLOT + 1)
        ).orElse(null);

        if (recipe == null || !crucible.canAccept(recipe.result())) {
            if (crucible.progress != 0) {
                crucible.progress = 0;
                crucible.setChanged();
            }
            return;
        }

        crucible.progress++;
        if (crucible.progress < DISTILLATION_TIME) {
            if (crucible.progress % 4 == 0) {
                crucible.setChanged();
            }
            if (crucible.progress % 10 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.PORTAL,
                        pos.getX() + 0.5D,
                        pos.getY() + 1.08D,
                        pos.getZ() + 0.5D,
                        3,
                        0.22D,
                        0.04D,
                        0.22D,
                        0.02D
                );
            }
            return;
        }

        crucible.items.get(MEDIUM_SLOT).shrink(1);
        for (int index = 0; index < CATALYST_SLOT_COUNT; index++) {
            ItemStack catalyst = crucible.items.get(FIRST_CATALYST_SLOT + index);
            if (!catalyst.isEmpty()) {
                catalyst.shrink(1);
            }
        }
        crucible.addResult(recipe.resultCopy());
        crucible.progress = 0;
        crucible.setChanged();

        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.15F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.9D,
                    pos.getZ() + 0.5D,
                    18,
                    0.32D,
                    0.18D,
                    0.32D,
                    0.08D
            );
        }
    }

    private boolean canAccept(ItemStack result) {
        ItemStack output = this.items.get(OUTPUT_SLOT);
        return output.isEmpty()
                || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void addResult(ItemStack result) {
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }
    }

    public int progress() {
        return this.progress;
    }

    public float progressFraction() {
        return Mth.clamp((float) this.progress / DISTILLATION_TIME, 0.0F, 1.0F);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.betterenchanting.arcane_crucible");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ArcaneCrucibleMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.progress = tag.getInt("DistillationProgress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("DistillationProgress", this.progress);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
