package com.betterenchanting.client;

import com.betterenchanting.client.MachineDisplayState.Display;
import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AttunementPedestalRenderer implements BlockEntityRenderer<AttunementPedestalBlockEntity> {
    private static final Map<AttunementPedestalBlockEntity, AnimationState> ANIMATIONS = new WeakHashMap<>();

    public AttunementPedestalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AttunementPedestalBlockEntity pedestal, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!(pedestal.getLevel() instanceof ClientLevel level)) {
            return;
        }
        double time = level.getGameTime() + partialTick;
        AnimationState state = ANIMATIONS.computeIfAbsent(pedestal, unused -> new AnimationState());
        state.advance(pedestal, time);
        List<Display> published = new ArrayList<>();

        double readyPulse = 1.0D + state.ready * (0.025D + Math.sin(time * 0.16D) * 0.018D);
        Vec3 targetPosition = new Vec3(0.5D, 1.25D + Math.sin(time * 0.085D) * 0.04D, 0.5D);
        renderAnimatedStack(pedestal, state.items, AttunementPedestalBlockEntity.TARGET_SLOT,
                poses, buffers, packedLight, packedOverlay, targetPosition, time * 1.35D,
                (float) (0.78D * readyPulse), 0);
        publishCurrent(pedestal, AttunementPedestalBlockEntity.TARGET_SLOT, targetPosition, 0.34D, published);

        double radius = 0.50D - state.ready * 0.05D;
        Vec3 essencePosition = orbitPosition(state.orbitAngle, radius, 1.57D, time, 1.1D);
        renderAnimatedStack(pedestal, state.items, AttunementPedestalBlockEntity.ESSENCE_SLOT,
                poses, buffers, packedLight, packedOverlay, essencePosition, -time * 2.0D,
                0.49F, 1);
        publishCurrent(pedestal, AttunementPedestalBlockEntity.ESSENCE_SLOT, essencePosition, 0.25D, published);

        Vec3 catalystPosition = orbitPosition(state.orbitAngle + Math.PI, radius, 1.57D, time, 3.0D);
        renderAnimatedStack(pedestal, state.items, AttunementPedestalBlockEntity.CATALYST_SLOT,
                poses, buffers, packedLight, packedOverlay, catalystPosition, time * 2.0D,
                0.49F, 2);
        publishCurrent(pedestal, AttunementPedestalBlockEntity.CATALYST_SLOT, catalystPosition, 0.25D, published);

        MachineDisplayState.publish(level, pedestal.getBlockPos(), published);
    }

    private static Vec3 orbitPosition(double angle, double radius, double y, double time, double phase) {
        return new Vec3(
                0.5D + Math.cos(angle) * radius,
                y + Math.sin(time * 0.11D + phase) * 0.045D,
                0.5D + Math.sin(angle) * radius
        );
    }

    private static void renderAnimatedStack(AttunementPedestalBlockEntity pedestal, MachineItemAnimation animation,
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
                pedestal.getLevel(), (int) pedestal.getBlockPos().asLong() + seedOffset);
        poses.popPose();
    }

    private static void publishCurrent(AttunementPedestalBlockEntity pedestal, int slot, Vec3 localPosition,
                                       double pickRadius, List<Display> displays) {
        ItemStack current = pedestal.getItem(slot);
        if (current.isEmpty()) {
            return;
        }
        displays.add(new Display(
                pedestal.getBlockPos(),
                slot,
                localPosition.add(Vec3.atLowerCornerOf(pedestal.getBlockPos())),
                current,
                pickRadius
        ));
    }

    @Override
    public AABB getRenderBoundingBox(AttunementPedestalBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D, 2.0D, 2.0D);
    }

    private static final class AnimationState {
        private final MachineItemAnimation items = new MachineItemAnimation(AttunementPedestalBlockEntity.CONTAINER_SIZE);
        private float ready;
        private double orbitAngle;

        private void advance(AttunementPedestalBlockEntity pedestal, double time) {
            double delta = this.items.advance(pedestal, time);
            float targetReady = pedestal.selectedEnchantment() != null && pedestal.upgradePlan().canUpgrade() ? 1.0F : 0.0F;
            this.ready = Mth.lerp(MachineItemAnimation.smoothing(delta, 0.30D), this.ready, targetReady);
            this.orbitAngle = (this.orbitAngle + delta * (0.035D + this.ready * 0.025D)) % (Math.PI * 2.0D);
        }
    }
}
