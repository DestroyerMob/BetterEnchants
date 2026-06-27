package com.betterenchanting.mixin.client;

import com.betterenchanting.client.OverleveledGlintRenderTypes;
import com.betterenchanting.data.EnchantmentLevelRules;
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
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getCompassFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer betterenchanting$getCompassFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, PoseStack.Pose pose) {
        return betterenchanting$isRenderingOverleveled()
                ? OverleveledGlintRenderTypes.getCompassFoilBuffer(bufferSource, renderType, pose)
                : ItemRenderer.getCompassFoilBuffer(bufferSource, renderType, pose);
    }

    @Redirect(
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
            boolean withGlint
    ) {
        return betterenchanting$isRenderingOverleveled()
                ? OverleveledGlintRenderTypes.getFoilBuffer(bufferSource, renderType, isItem, true)
                : ItemRenderer.getFoilBuffer(bufferSource, renderType, isItem, withGlint);
    }

    @Redirect(
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
            boolean withGlint
    ) {
        return betterenchanting$isRenderingOverleveled()
                ? OverleveledGlintRenderTypes.getFoilBufferDirect(bufferSource, renderType, isItem, true)
                : ItemRenderer.getFoilBufferDirect(bufferSource, renderType, isItem, withGlint);
    }

    @Inject(method = "getCompassFoilBuffer", at = @At("HEAD"), cancellable = true)
    private static void betterenchanting$useOverleveledCompassFoilBuffer(
            MultiBufferSource bufferSource,
            RenderType renderType,
            PoseStack.Pose pose,
            CallbackInfoReturnable<VertexConsumer> callbackInfo
    ) {
        if (betterenchanting$isRenderingOverleveled()) {
            callbackInfo.setReturnValue(OverleveledGlintRenderTypes.getCompassFoilBuffer(bufferSource, renderType, pose));
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
            callbackInfo.setReturnValue(OverleveledGlintRenderTypes.getFoilBuffer(bufferSource, renderType, isItem, true));
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
            callbackInfo.setReturnValue(OverleveledGlintRenderTypes.getFoilBufferDirect(bufferSource, renderType, isItem, true));
        }
    }

    @Unique
    private static boolean betterenchanting$isRenderingOverleveled() {
        ItemStack stack = betterenchanting$renderingStack.get();
        return stack != null && EnchantmentLevelRules.hasOverleveledEnchantment(stack);
    }
}
