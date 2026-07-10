package com.betterenchanting.mixin;

import com.betterenchanting.world.inventory.EnhancedEnchantingMenu;
import com.betterenchanting.world.level.block.EnchantingTableStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantingTableBlockEntity.class)
public abstract class EnchantingTableBlockEntityMixin extends BlockEntity implements EnchantingTableStorage {
    @Unique
    private static final String BETTERENCHANTING_INVENTORY_TAG = "BetterEnchantingInventory";

    @Unique
    private final SimpleContainer betterenchanting$enchantingInventory = new SimpleContainer(EnhancedEnchantingMenu.ENCHANTING_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnchantingTableBlockEntityMixin.this.setChanged();
            if (EnchantingTableBlockEntityMixin.this.level != null && !EnchantingTableBlockEntityMixin.this.level.isClientSide) {
                EnchantingTableBlockEntityMixin.this.level.sendBlockUpdated(
                        EnchantingTableBlockEntityMixin.this.worldPosition,
                        EnchantingTableBlockEntityMixin.this.getBlockState(),
                        EnchantingTableBlockEntityMixin.this.getBlockState(),
                        3
                );
            }
        }
    };

    protected EnchantingTableBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public SimpleContainer betterenchanting$getEnchantingInventory() {
        return this.betterenchanting$enchantingInventory;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void betterenchanting$saveInventory(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo callbackInfo
    ) {
        CompoundTag inventoryTag = new CompoundTag();
        ContainerHelper.saveAllItems(inventoryTag, this.betterenchanting$enchantingInventory.getItems(), registries);
        tag.put(BETTERENCHANTING_INVENTORY_TAG, inventoryTag);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void betterenchanting$loadInventory(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo callbackInfo
    ) {
        this.betterenchanting$enchantingInventory.getItems().clear();
        if (tag.contains(BETTERENCHANTING_INVENTORY_TAG, CompoundTag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(
                    tag.getCompound(BETTERENCHANTING_INVENTORY_TAG),
                    this.betterenchanting$enchantingInventory.getItems(),
                    registries
            );
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((EnchantingTableBlockEntity) (Object) this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
