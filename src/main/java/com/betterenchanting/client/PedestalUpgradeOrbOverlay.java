package com.betterenchanting.client;

import com.betterenchanting.registry.ModItems;
import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Focus marker that opens the screen-space pedestal upgrade view. */
public final class PedestalUpgradeOrbOverlay {
    private static final int SCAN_RADIUS = 8;
    private static final int VERTICAL_RADIUS = 4;
    private static final int SCAN_INTERVAL = 5;
    private static final double PICK_DISTANCE = 8.0D;
    private static final double PICK_RADIUS = 0.36D;
    private static final RenderType MARKER = RenderType.create(
            "betterenchanting_pedestal_attunement_marker",
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

    private static ClientLevel cachedLevel;
    private static BlockPos cachedOrigin = BlockPos.ZERO;
    private static long lastScan = Long.MIN_VALUE;
    private static List<AttunementPedestalBlockEntity> cachedPedestals = List.of();
    private static Optional<AttunementPedestalBlockEntity> hovered = Optional.empty();

    private PedestalUpgradeOrbOverlay() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canDisplay(minecraft)) {
            hovered = Optional.empty();
            return;
        }
        List<AttunementPedestalBlockEntity> pedestals = nearbyPedestals(minecraft);
        hovered = pick(pedestals, minecraft);
        if (pedestals.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        double time = minecraft.level.getGameTime()
                + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(MARKER);
        for (AttunementPedestalBlockEntity pedestal : pedestals) {
            boolean selected = hovered.filter(value -> value.getBlockPos().equals(pedestal.getBlockPos())).isPresent();
            double pulse = 1.0D + Math.sin(time * 0.16D + pedestal.getBlockPos().asLong() * 0.001D) * 0.08D;
            Vec3 position = anchor(pedestal);
            int color = selected ? AttunementUiTheme.BONUS_COLOR : AttunementUiTheme.ACTIVE_COLOR;
            drawDiamond(event.getPoseStack(), camera, consumer, position,
                    (selected ? 0.15D : 0.11D) * pulse, color, selected ? 0.96F : 0.74F);
            drawDiamond(event.getPoseStack(), camera, consumer, position,
                    (selected ? 0.24D : 0.18D) * pulse, color, selected ? 0.22F : 0.12F);
        }
        buffers.endBatch(MARKER);
    }

    public static void renderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canDisplay(minecraft) || hovered.isEmpty()) {
            return;
        }
        Component action = Component.translatable("gui.betterenchanting.pedestal.screen.open");
        int width = Math.min(minecraft.getWindow().getGuiScaledWidth() - 16,
                Math.max(174, minecraft.font.width(action) + 24));
        int height = 24;
        int x = (minecraft.getWindow().getGuiScaledWidth() - width) / 2;
        int y = Math.min(minecraft.getWindow().getGuiScaledHeight() - height - 12,
                minecraft.getWindow().getGuiScaledHeight() / 2 + 26);
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(x, y, x + width, y + height, AttunementUiTheme.HOVER_FILL);
        graphics.renderOutline(x, y, width, height, AttunementUiTheme.PANEL_BORDER);
        graphics.fill(x + 6, y + 5, x + 9, y + height - 5, AttunementUiTheme.ACTIVE_COLOR);
        graphics.drawCenteredString(minecraft.font, action, x + width / 2 + 4, y + 8,
                AttunementUiTheme.TEXT_COLOR);
    }

    public static void handleInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canDisplay(minecraft)) {
            return;
        }
        pick(nearbyPedestals(minecraft), minecraft).ifPresent(pedestal -> {
            minecraft.setScreen(new PedestalUpgradeScreen(pedestal.getBlockPos()));
            event.setSwingHand(true);
            event.setCanceled(true);
        });
    }

    private static List<AttunementPedestalBlockEntity> nearbyPedestals(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        BlockPos origin = minecraft.player.blockPosition();
        long time = level.getGameTime();
        if (level == cachedLevel && origin.equals(cachedOrigin) && time - lastScan < SCAN_INTERVAL) {
            return cachedPedestals;
        }
        List<AttunementPedestalBlockEntity> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -VERTICAL_RADIUS; y <= VERTICAL_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (level.getBlockEntity(cursor) instanceof AttunementPedestalBlockEntity pedestal
                            && !pedestal.target().isEmpty()) {
                        found.add(pedestal);
                    }
                }
            }
        }
        cachedLevel = level;
        cachedOrigin = origin.immutable();
        lastScan = time;
        cachedPedestals = List.copyOf(found);
        return cachedPedestals;
    }

    private static Optional<AttunementPedestalBlockEntity> pick(
            List<AttunementPedestalBlockEntity> pedestals,
            Minecraft minecraft
    ) {
        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        AttunementPedestalBlockEntity best = null;
        double bestAlong = Double.MAX_VALUE;
        for (AttunementPedestalBlockEntity pedestal : pedestals) {
            Vec3 delta = anchor(pedestal).subtract(origin);
            double along = delta.dot(look);
            if (along < 0.35D || along > PICK_DISTANCE) {
                continue;
            }
            if (delta.subtract(look.scale(along)).length() <= PICK_RADIUS && along < bestAlong) {
                best = pedestal;
                bestAlong = along;
            }
        }
        return Optional.ofNullable(best);
    }

    private static Vec3 anchor(AttunementPedestalBlockEntity pedestal) {
        return Vec3.atCenterOf(pedestal.getBlockPos()).add(0.0D, 1.95D, 0.0D);
    }

    private static boolean canDisplay(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && minecraft.screen == null
                && !minecraft.options.hideGui
                && (minecraft.player.getMainHandItem().is(ModItems.ATTUNEMENT_FOCUS.get())
                || minecraft.player.getOffhandItem().is(ModItems.ATTUNEMENT_FOCUS.get()));
    }

    private static void drawDiamond(PoseStack poses, Camera camera, VertexConsumer consumer,
                                    Vec3 position, double radius, int color, float alpha) {
        Vec3 relative = position.subtract(camera.getPosition());
        poses.pushPose();
        poses.translate(relative.x, relative.y, relative.z);
        poses.mulPose(camera.rotation());
        PoseStack.Pose pose = poses.last();
        float r = (float) radius;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        consumer.addVertex(pose, 0.0F, r, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, r, 0.0F, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, 0.0F, -r, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, -r, 0.0F, 0.0F).setColor(red, green, blue, alpha);
        poses.popPose();
    }
}
