package com.betterenchanting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Shared motion and rendering rules for magical items displayed above in-world stations. */
final class FloatingItemVisuals {
    private static final double ROTATION_DEGREES_PER_TICK = 0.22D;
    private static final double BOB_RADIANS_PER_TICK = 0.075D;

    private FloatingItemVisuals() {
    }

    static float slowRotation(double time, double phaseDegrees) {
        return (float) ((phaseDegrees + time * ROTATION_DEGREES_PER_TICK) % 360.0D);
    }

    static double bob(double time, double phase, double amplitude) {
        return Mth.sin((float) ((time * BOB_RADIANS_PER_TICK + phase) % Mth.TWO_PI)) * amplitude;
    }

    static void render(
            BlockEntity owner,
            ItemStack stack,
            PoseStack poses,
            MultiBufferSource buffers,
            int packedOverlay,
            Vec3 position,
            float scale,
            float rotationDegrees,
            int seedOffset
    ) {
        if (stack.isEmpty() || scale <= 0.001F) {
            return;
        }
        poses.pushPose();
        poses.translate(position.x, position.y, position.z);
        poses.mulPose(Axis.YP.rotationDegrees(rotationDegrees));
        poses.scale(scale, scale, scale);
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        renderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                packedOverlay,
                poses,
                buffers,
                owner.getLevel(),
                (int) owner.getBlockPos().asLong() + seedOffset
        );
        poses.popPose();
    }
}
