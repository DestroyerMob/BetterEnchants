package com.betterenchanting.client;

import com.betterenchanting.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ConductiveChargeLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation CHARGED_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    private final HumanoidModel<AbstractClientPlayer> chargeModel;

    public ConductiveChargeLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer,
            ModelPart modelRoot
    ) {
        super(renderer);
        this.chargeModel = new HumanoidModel<>(modelRoot);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!player.hasEffect(ModEffects.CONDUCTIVE_CHARGE)) {
            return;
        }

        float animationTime = player.tickCount + partialTicks;
        chargeModel.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(chargeModel);
        chargeModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vertices = buffer.getBuffer(RenderType.energySwirl(
                CHARGED_TEXTURE,
                animationTime * 0.01F % 1.0F,
                animationTime * 0.01F % 1.0F
        ));
        chargeModel.renderToBuffer(
                poseStack,
                vertices,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                -8355712
        );
    }
}
