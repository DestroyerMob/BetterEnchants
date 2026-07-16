package com.betterenchanting.client;

import com.betterenchanting.network.TakeMachineDisplayPayload;
import com.betterenchanting.world.level.block.InWorldMachineInteraction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MachineDisplayInteraction {
    private static final double PICK_DISTANCE = 8.0D;
    private static final RenderType HIGHLIGHT = RenderType.create(
            "betterenchanting_machine_display_highlight",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            128,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );
    private static Optional<MachineDisplayState.Display> hovered = Optional.empty();

    private MachineDisplayInteraction() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canInspect(minecraft)) {
            hovered = Optional.empty();
            return;
        }
        List<MachineDisplayState.Display> displays = MachineDisplayState.nearby(
                minecraft.level,
                event.getCamera().getPosition(),
                PICK_DISTANCE
        );
        hovered = pick(displays, minecraft);
        if (hovered.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(HIGHLIGHT);
        double time = minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double radius = hovered.get().pickRadius() * (1.0D + Math.sin(time * 0.18D) * 0.06D);
        drawDiamond(event.getPoseStack(), event.getCamera(), consumer, hovered.get().position(), radius, 0.66F);
        drawDiamond(event.getPoseStack(), event.getCamera(), consumer, hovered.get().position(), radius * 1.35D, 0.14F);
        buffers.endBatch(HIGHLIGHT);
    }

    public static void renderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canInspect(minecraft) || hovered.isEmpty()) {
            return;
        }
        MachineDisplayState.Display display = hovered.get();
        List<Component> enchantments = FloatingItemTooltip.enchantmentLines(minecraft.level, display.stack());
        Component action = machineAction(minecraft, display);
        FloatingItemTooltip.renderCard(
                event.getGuiGraphics(),
                minecraft.font,
                display.stack(),
                enchantments,
                action,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight()
        );
    }

    public static void handleInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canInspect(minecraft)) {
            return;
        }
        Optional<MachineDisplayState.Display> hit = pick(
                MachineDisplayState.nearby(minecraft.level, minecraft.gameRenderer.getMainCamera().getPosition(), PICK_DISTANCE),
                minecraft
        );
        if (hit.isEmpty()) {
            return;
        }
        MachineDisplayState.Display display = hit.get();
        if (InWorldMachineInteraction.canReceive(minecraft.player, display.stack())) {
            PacketDistributor.sendToServer(new TakeMachineDisplayPayload(
                    display.machinePos(),
                    display.slot(),
                    minecraft.player.isShiftKeyDown()
            ));
        }
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    private static Optional<MachineDisplayState.Display> pick(List<MachineDisplayState.Display> displays, Minecraft minecraft) {
        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        MachineDisplayState.Display best = null;
        double bestAlong = Double.MAX_VALUE;
        for (MachineDisplayState.Display display : displays) {
            Vec3 delta = display.position().subtract(origin);
            double along = delta.dot(look);
            if (along < 0.25D || along > PICK_DISTANCE) {
                continue;
            }
            double distance = delta.subtract(look.scale(along)).length();
            if (distance <= display.pickRadius() && along < bestAlong) {
                best = display;
                bestAlong = along;
            }
        }
        return Optional.ofNullable(best);
    }

    private static Component machineAction(Minecraft minecraft, MachineDisplayState.Display display) {
        if (!InWorldMachineInteraction.canReceive(minecraft.player, display.stack())) {
            return Component.translatable("gui.betterenchanting.machine.inventory_full")
                    .withStyle(ChatFormatting.DARK_GRAY);
        }
        return Component.translatable(minecraft.player.isShiftKeyDown()
                ? "gui.betterenchanting.machine.take_all"
                : "gui.betterenchanting.machine.take_display").withStyle(ChatFormatting.GRAY);
    }

    private static boolean canInspect(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && minecraft.screen == null
                && !minecraft.options.hideGui;
    }

    private static void drawDiamond(PoseStack poses, Camera camera, VertexConsumer consumer,
                                    Vec3 position, double radius, float alpha) {
        Vec3 relative = position.subtract(camera.getPosition());
        poses.pushPose();
        poses.translate(relative.x, relative.y, relative.z);
        poses.mulPose(camera.rotation());
        PoseStack.Pose pose = poses.last();
        float r = (float) radius;
        consumer.addVertex(pose, 0.0F, r, 0.0F).setColor(0.55F, 0.88F, 1.0F, alpha);
        consumer.addVertex(pose, r, 0.0F, 0.0F).setColor(0.55F, 0.88F, 1.0F, alpha);
        consumer.addVertex(pose, 0.0F, -r, 0.0F).setColor(0.55F, 0.88F, 1.0F, alpha);
        consumer.addVertex(pose, -r, 0.0F, 0.0F).setColor(0.55F, 0.88F, 1.0F, alpha);
        poses.popPose();
    }
}
