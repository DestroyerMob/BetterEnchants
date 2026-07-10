package com.betterenchanting.client;

import com.betterenchanting.network.TakeMachineDisplayPayload;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
        if (!canInteract(minecraft)) {
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
        if (!canInteract(minecraft) || hovered.isEmpty()) {
            return;
        }
        MachineDisplayState.Display display = hovered.get();
        Component name = display.stack().getHoverName();
        Component action = Component.translatable(minecraft.player.isShiftKeyDown()
                ? "gui.betterenchanting.machine.take_all"
                : "gui.betterenchanting.machine.take_display").withStyle(ChatFormatting.GRAY);
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = Math.max(font.width(name), font.width(action)) + 20;
        int x = minecraft.getWindow().getGuiScaledWidth() / 2 + 13;
        int y = minecraft.getWindow().getGuiScaledHeight() / 2 + 12;
        graphics.fill(x, y, x + width, y + 28, 0xC0100E17);
        graphics.renderOutline(x, y, width, 28, 0xAA9FE7FF);
        graphics.drawString(font, name, x + 8, y + 5, 0xFFF1F8FF, true);
        graphics.drawString(font, action, x + 8, y + 16, 0xFFAFA7B7, false);
    }

    public static void handleInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canInteract(minecraft)) {
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
        PacketDistributor.sendToServer(new TakeMachineDisplayPayload(
                display.machinePos(),
                display.slot(),
                minecraft.player.isShiftKeyDown()
        ));
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

    private static boolean canInteract(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && minecraft.screen == null
                && !minecraft.options.hideGui && minecraft.player.getMainHandItem().isEmpty();
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
