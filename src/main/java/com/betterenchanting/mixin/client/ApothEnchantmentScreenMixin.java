package com.betterenchanting.mixin.client;

import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentScreen", remap = false)
public abstract class ApothEnchantmentScreenMixin extends EnchantmentScreen {
    private static final int BETTER_ENCHANTING_APOTHIC_IMAGE_WIDTH = 201;

    private ApothEnchantmentScreenMixin(EnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void betterenchanting$useExtendedGuiWidth(EnchantmentMenu menu, Inventory playerInventory, Component title, CallbackInfo callbackInfo) {
        this.imageWidth = BETTER_ENCHANTING_APOTHIC_IMAGE_WIDTH;
    }
}
