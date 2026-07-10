package com.betterenchanting.mixin.client;

import com.betterenchanting.client.ChainedMiningAnimationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    private ItemStack mainHandItem;

    @Inject(method = "tick", at = @At("HEAD"))
    private void betterenchanting$suppressChainedMiningReequip(CallbackInfo callbackInfo) {
        if (!ChainedMiningAnimationState.suppressesReequipAnimation()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ItemStack current = minecraft.player.getMainHandItem();
        if (ItemStack.isSameItem(mainHandItem, current)) {
            mainHandItem = current;
        }
    }
}
