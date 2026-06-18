package com.betterenchanting.mixin.client;

import com.betterenchanting.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static final String RENDER_METHOD = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Unique
    private boolean betterenchanting$renderingFrozenEntity;

    @Inject(method = RENDER_METHOD, at = @At("HEAD"))
    private void betterenchanting$captureFrozenEntity(
            LivingEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        this.betterenchanting$renderingFrozenEntity = entity.hasEffect(ModEffects.FROZEN);
    }

    @Inject(method = RENDER_METHOD, at = @At("RETURN"))
    private void betterenchanting$clearFrozenEntity(
            LivingEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        this.betterenchanting$renderingFrozenEntity = false;
    }

    @ModifyArg(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            ),
            index = 4
    )
    private int betterenchanting$tintFrozenEntity(int color) {
        if (!this.betterenchanting$renderingFrozenEntity) {
            return color;
        }

        int alpha = color >>> 24;
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        return alpha << 24
                | red * 120 / 255 << 16
                | green * 210 / 255 << 8
                | blue;
    }
}
