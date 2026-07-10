package com.betterenchanting.client;

import com.betterenchanting.client.MachineDisplayState.Display;
import com.betterenchanting.world.level.block.entity.ArcaneCrucibleBlockEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ArcaneCrucibleRenderer implements BlockEntityRenderer<ArcaneCrucibleBlockEntity> {
    private static final RenderType RUNES = RenderType.create(
            "betterenchanting_crucible_runes",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );
    private static final Map<ArcaneCrucibleBlockEntity, AnimationState> ANIMATIONS = new WeakHashMap<>();

    public ArcaneCrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArcaneCrucibleBlockEntity crucible, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!(crucible.getLevel() instanceof ClientLevel level)) {
            return;
        }
        double time = level.getGameTime() + partialTick;
        AnimationState state = ANIMATIONS.computeIfAbsent(crucible, unused -> new AnimationState());
        state.advance(crucible, time);
        List<Display> published = new ArrayList<>();

        Vec3 mediumPosition = new Vec3(0.5D, 1.20D + Math.sin(time * 0.09D) * 0.035D, 0.5D);
        renderAnimatedStack(crucible, state.items, ArcaneCrucibleBlockEntity.MEDIUM_SLOT, poses, buffers,
                packedLight, packedOverlay, mediumPosition, time * 1.35D, 0.62F, 0);
        publishCurrent(crucible, ArcaneCrucibleBlockEntity.MEDIUM_SLOT, mediumPosition, 0.27D, published);

        double radius = 0.49D - state.progress * 0.22D;
        for (int index = 0; index < ArcaneCrucibleBlockEntity.CATALYST_SLOT_COUNT; index++) {
            int slot = ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + index;
            double angle = state.catalystAngle + index * Math.PI;
            Vec3 position = new Vec3(
                    0.5D + Math.cos(angle) * radius,
                    1.48D + Math.sin(time * 0.12D + index * 2.4D) * 0.045D,
                    0.5D + Math.sin(angle) * radius
            );
            renderAnimatedStack(crucible, state.items, slot, poses, buffers, packedLight, packedOverlay,
                    position, -time * 2.1D + index * 180.0D, 0.43F, 1 + index);
            publishCurrent(crucible, slot, position, 0.24D, published);
        }

        float outputReveal = state.items.reveal(ArcaneCrucibleBlockEntity.OUTPUT_SLOT);
        Vec3 outputPosition = new Vec3(
                0.5D,
                1.73D + outputReveal * 0.20D + Math.sin(time * 0.075D + 1.5D) * 0.055D,
                0.5D
        );
        renderAnimatedStack(crucible, state.items, ArcaneCrucibleBlockEntity.OUTPUT_SLOT, poses, buffers,
                packedLight, packedOverlay, outputPosition, time, 0.72F, 3);
        publishCurrent(crucible, ArcaneCrucibleBlockEntity.OUTPUT_SLOT, outputPosition, 0.31D, published);

        if (state.progress > 0.002F) {
            renderProgressRunes(poses, buffers.getBuffer(RUNES), state.progress, time);
        }
        MachineDisplayState.publish(level, crucible.getBlockPos(), published);
    }

    private static void renderAnimatedStack(ArcaneCrucibleBlockEntity crucible, MachineItemAnimation animation,
                                            int slot, PoseStack poses, MultiBufferSource buffers,
                                            int packedLight, int packedOverlay, Vec3 position,
                                            double rotation, float baseScale, int seedOffset) {
        ItemStack stack = animation.displayed(slot);
        float reveal = animation.reveal(slot);
        if (stack.isEmpty() || reveal <= 0.001F) {
            return;
        }
        float scale = baseScale * reveal;
        poses.pushPose();
        poses.translate(position.x, position.y, position.z);
        poses.mulPose(Axis.YP.rotationDegrees((float) rotation));
        poses.scale(scale, scale, scale);
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        renderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poses, buffers,
                crucible.getLevel(), (int) crucible.getBlockPos().asLong() + seedOffset);
        poses.popPose();
    }

    private static void publishCurrent(ArcaneCrucibleBlockEntity crucible, int slot, Vec3 localPosition,
                                       double pickRadius, List<Display> displays) {
        ItemStack current = crucible.getItem(slot);
        if (current.isEmpty()) {
            return;
        }
        displays.add(new Display(
                crucible.getBlockPos(),
                slot,
                localPosition.add(Vec3.atLowerCornerOf(crucible.getBlockPos())),
                current,
                pickRadius
        ));
    }

    private static void renderProgressRunes(PoseStack poses, VertexConsumer consumer, float progress, double time) {
        float rotation = (float) (time * 0.012D);
        PoseStack.Pose pose = poses.last();
        for (int index = 0; index < 16; index++) {
            float fill = Mth.clamp(progress * 16.0F - index, 0.0F, 1.0F);
            float angle = rotation + index * Mth.TWO_PI / 16.0F;
            float cx = 0.5F + Mth.cos(angle) * 0.43F;
            float cz = 0.5F + Mth.sin(angle) * 0.43F;
            float size = Mth.lerp(fill, 0.024F, 0.043F);
            float alpha = Mth.lerp(fill, 0.07F, 0.84F);
            float red = Mth.lerp(fill, 0.34F, 0.80F);
            float green = Mth.lerp(fill, 0.16F, 0.40F);
            float blue = Mth.lerp(fill, 0.48F, 1.0F);
            consumer.addVertex(pose, cx, 1.055F, cz - size).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, cx + size, 1.055F, cz).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, cx, 1.055F, cz + size).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, cx - size, 1.055F, cz).setColor(red, green, blue, alpha);
        }
    }

    @Override
    public AABB getRenderBoundingBox(ArcaneCrucibleBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.5D, 2.0D, 1.5D);
    }

    private static final class AnimationState {
        private final MachineItemAnimation items = new MachineItemAnimation(ArcaneCrucibleBlockEntity.CONTAINER_SIZE);
        private float progress;
        private double catalystAngle;

        private void advance(ArcaneCrucibleBlockEntity crucible, double time) {
            double delta = this.items.advance(crucible, time);
            float blend = MachineItemAnimation.smoothing(delta, 0.34D);
            this.progress = Mth.lerp(blend, this.progress, crucible.progressFraction());
            this.catalystAngle = (this.catalystAngle
                    + delta * (0.045D + this.progress * 0.075D)) % (Math.PI * 2.0D);
        }
    }
}
