package com.betterenchanting.mixin.client;

import com.betterenchanting.client.EnchantingTableItemDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantTableRenderer.class)
public abstract class EnchantTableRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void betterenchanting$renderStoredItems(
            EnchantingTableBlockEntity table,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            CallbackInfo callbackInfo
    ) {
        EnchantingTableItemDisplay.render(table, partialTick, poseStack, buffers, packedLight, packedOverlay);
    }

    @Inject(method = "getRenderBoundingBox", at = @At("RETURN"), cancellable = true, remap = false)
    private void betterenchanting$includeStoredItems(
            EnchantingTableBlockEntity table,
            CallbackInfoReturnable<AABB> callbackInfo
    ) {
        callbackInfo.setReturnValue(new AABB(table.getBlockPos()).inflate(0.65D, 1.55D, 0.65D));
    }
}
