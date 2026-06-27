package com.betterenchanting.mixin;

import com.betterenchanting.world.enchantment.StickyGripEnchantmentEvents;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handlePlayerAction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;drop(Z)Z"),
            cancellable = true
    )
    private void betterenchanting$preventStickyGripSelectedDrop(ServerboundPlayerActionPacket packet, CallbackInfo callbackInfo) {
        ItemStack selected = this.player.getInventory().getSelected();
        if (StickyGripEnchantmentEvents.preventsDropKey(this.player, selected)) {
            this.player.containerMenu.sendAllDataToRemote();
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = "handleContainerClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;suppressRemoteUpdates()V"
            ),
            cancellable = true
    )
    private void betterenchanting$preventStickyGripInventoryThrow(ServerboundContainerClickPacket packet, CallbackInfo callbackInfo) {
        if (packet.getClickType() != ClickType.THROW || !this.player.containerMenu.getCarried().isEmpty()) {
            return;
        }

        int slotIndex = packet.getSlotNum();
        if (slotIndex < 0 || slotIndex >= this.player.containerMenu.slots.size()) {
            return;
        }

        Slot slot = this.player.containerMenu.getSlot(slotIndex);
        if (StickyGripEnchantmentEvents.preventsDropKey(this.player, slot.getItem())) {
            this.player.containerMenu.sendAllDataToRemote();
            callbackInfo.cancel();
        }
    }
}
