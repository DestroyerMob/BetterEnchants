package com.betterenchanting.mixin.client;

import com.betterenchanting.client.OverleveledGlintRenderTypes;
import com.betterenchanting.data.EnchantmentLevelRules;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Unique
    private static final ThreadLocal<ItemStack> betterenchanting$renderingStack = new ThreadLocal<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void betterenchanting$trackRenderedStack(
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight,
            int combinedOverlay,
            BakedModel model,
            CallbackInfo callbackInfo
    ) {
        betterenchanting$renderingStack.set(itemStack);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void betterenchanting$clearRenderedStack(
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight,
            int combinedOverlay,
            BakedModel model,
            CallbackInfo callbackInfo
    ) {
        betterenchanting$renderingStack.remove();
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getCompassFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer betterenchanting$getCompassFoilBuffer(
            MultiBufferSource bufferSource,
            RenderType renderType,
            PoseStack.Pose pose,
            Operation<VertexConsumer> original
    ) {
        return betterenchanting$isRenderingOverleveled()
                ? OverleveledGlintRenderTypes.getCompassFoilBuffer(bufferSource, renderType, pose, betterenchanting$currentStack())
                : original.call(bufferSource, renderType, pose);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer betterenchanting$getFoilBuffer(
            MultiBufferSource bufferSource,
            RenderType renderType,
            boolean isItem,
            boolean withGlint,
            Operation<VertexConsumer> original
    ) {
        return betterenchanting$isRenderingOverleveled()
                ? OverleveledGlintRenderTypes.getFoilBuffer(bufferSource, renderType, isItem, true, betterenchanting$currentStack())
                : original.call(bufferSource, renderType, isItem, withGlint);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer betterenchanting$getFoilBufferDirect(
            MultiBufferSource bufferSource,
            RenderType renderType,
            boolean isItem,
            boolean withGlint,
            Operation<VertexConsumer> original
    ) {
        return betterenchanting$isRenderingOverleveled()
                ? OverleveledGlintRenderTypes.getFoilBufferDirect(bufferSource, renderType, isItem, true, betterenchanting$currentStack())
                : original.call(bufferSource, renderType, isItem, withGlint);
    }

    @Inject(method = "getCompassFoilBuffer", at = @At("HEAD"), cancellable = true)
    private static void betterenchanting$useOverleveledCompassFoilBuffer(
            MultiBufferSource bufferSource,
            RenderType renderType,
            PoseStack.Pose pose,
            CallbackInfoReturnable<VertexConsumer> callbackInfo
    ) {
        if (betterenchanting$isRenderingOverleveled()) {
            callbackInfo.setReturnValue(OverleveledGlintRenderTypes.getCompassFoilBuffer(bufferSource, renderType, pose, betterenchanting$currentStack()));
        }
    }

    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true)
    private static void betterenchanting$useOverleveledFoilBuffer(
            MultiBufferSource bufferSource,
            RenderType renderType,
            boolean isItem,
            boolean withGlint,
            CallbackInfoReturnable<VertexConsumer> callbackInfo
    ) {
        if (betterenchanting$isRenderingOverleveled()) {
            callbackInfo.setReturnValue(OverleveledGlintRenderTypes.getFoilBuffer(bufferSource, renderType, isItem, true, betterenchanting$currentStack()));
        }
    }

    @Inject(method = "getFoilBufferDirect", at = @At("HEAD"), cancellable = true)
    private static void betterenchanting$useOverleveledFoilBufferDirect(
            MultiBufferSource bufferSource,
            RenderType renderType,
            boolean isItem,
            boolean withGlint,
            CallbackInfoReturnable<VertexConsumer> callbackInfo
    ) {
        if (betterenchanting$isRenderingOverleveled()) {
            callbackInfo.setReturnValue(OverleveledGlintRenderTypes.getFoilBufferDirect(bufferSource, renderType, isItem, true, betterenchanting$currentStack()));
        }
    }

    @Unique
    private static boolean betterenchanting$isRenderingOverleveled() {
        return EnchantmentLevelRules.hasOverleveledEnchantment(betterenchanting$currentStack());
    }

    @Unique
    private static ItemStack betterenchanting$currentStack() {
        ItemStack stack = betterenchanting$renderingStack.get();
        return stack == null ? ItemStack.EMPTY : stack;
    }
}
