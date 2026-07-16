package com.betterenchanting.client;

import com.betterenchanting.compat.MobsToolForgingCompat;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedEnchantmentBreakdown;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedEnchantmentState;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedPartBreakdown;
import com.betterenchanting.compat.MobsToolForgingCompat.StationRoutedEnchantmentPreview;
import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.data.EnchantmentLevelRules;
import com.betterenchanting.network.PromoteRoutedEnchantmentPayload;
import com.betterenchanting.registry.ModItems;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RoutedEnchantmentOrbOverlay {
    private static final double PICK_MAX_DISTANCE = 8.0D;
    private static final double PICK_RADIUS = 0.50D;
    private static final int STATION_SCAN_RADIUS = 8;
    private static final int STATION_VERTICAL_SCAN_RADIUS = 4;
    private static final int STATION_SCAN_INTERVAL_TICKS = 5;
    private static final double PART_RING_HORIZONTAL_RADIUS = 1.42D;
    private static final double PART_RING_VERTICAL_RADIUS = 0.88D;
    private static final double ENCHANTMENT_RING_RADIUS = 0.46D;
    private static final double ENCHANTMENT_RING_RADIUS_STEP = 0.08D;
    private static final double ENCHANTMENT_RING_MAX_RADIUS = 0.72D;
    private static final double TOOL_ENCHANTMENT_RING_RADIUS = 0.68D;
    private static final double TOOL_LINK_CENTER_GAP = 0.34D;
    private static final double TOOL_LINK_PART_GAP = 0.26D;
    private static final double ORB_ORBIT_SPEED = 0.020D;
    private static final double ORB_PULSE_SPEED = 0.26D;
    private static final double DORMANT_ANIMATION_SPEED = 0.15D;
    private static final double ORBIT_PAUSE_TRANSITION_TICKS = 5.0D;
    private static final double REVEAL_TRANSITION_TICKS = 10.0D;
    private static final double STATE_TRANSITION_TICKS = 6.0D;
    private static final double HOVER_TRANSITION_TICKS = 5.0D;
    private static final double VISUAL_STATE_TTL_TICKS = 80.0D;
    private static final double MIN_REVEAL_RENDER_WEIGHT = 0.02D;
    private static final double TOOL_HIDDEN_Y_OFFSET = -0.78D;
    private static final double TOOL_FLOAT_AMPLITUDE = 0.035D;
    private static final double TOOL_FLOAT_SPEED = 0.11D;
    private static final double PART_FLOAT_AMPLITUDE = 0.055D;
    private static final double PART_SWAY_AMPLITUDE = 0.025D;
    private static final double PART_FLOAT_SPEED = 0.13D;
    private static final double HOVER_COLOR_BLEND = 0.45D;
    private static final float TOOL_BILLBOARD_SCALE = 0.36F;
    private static final float PART_BILLBOARD_SCALE = 0.26F;
    private static final int ACTIVE_COLOR = 0xFFFF5CFF;
    private static final int DORMANT_COLOR = 0xFF9AA4B2;
    private static final int OVERLEVEL_BONUS_COLOR = 0xFFFFD166;
    private static final String OVERLEVEL_MARKER = "✦";
    private static final int HOVER_COLOR = 0xFFFFFFFF;
    private static final int STATUS_COLOR = 0xFFFFD166;
    private static final int TEXT_BACKGROUND = 0xAA000000;
    private static final int HUD_BACKGROUND = 0xB0000000;
    private static final int HUD_BORDER = 0x90FFFFFF;
    private static final int HUD_TEXT = 0xFFEDE7F6;
    private static final int HUD_MUTED = 0xFF9AA4B2;
    private static final float LABEL_SCALE = 0.02F;
    private static final RenderType ORB_BILLBOARD = RenderType.create(
            "betterenchanting_routed_enchantment_orb",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );
    private static List<OrbHit> lastHits = List.of();
    private static List<RenderOrb> lastOrbs = List.of();
    private static List<RenderPartCluster> lastPartClusters = List.of();
    private static Optional<OrbHit> lastHovered = Optional.empty();
    private static final Map<PartOrbitKey, PartOrbitState> partOrbitStates = new HashMap<>();
    private static final Map<OrbVisualKey, OrbVisualTransition> orbVisualTransitions = new HashMap<>();
    private static final Map<BlockPos, StationRevealState> stationRevealStates = new HashMap<>();
    private static ClientLevel cachedLevel;
    private static BlockPos cachedScanOrigin = BlockPos.ZERO;
    private static long cachedScanGameTime = Long.MIN_VALUE;
    private static List<StationPreview> cachedStations = List.of();

    private RoutedEnchantmentOrbOverlay() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui) {
            clearDisplayState();
            return;
        }

        Camera camera = event.getCamera();
        double renderTime = minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        List<StationPreview> scannedStations = nearbyStations(minecraft);
        if (!canUseRoutingOverlay(minecraft)) {
            clearDisplayState();
            return;
        }
        List<StationPreview> stations = scannedStations.stream()
                .filter(RoutedEnchantmentOrbOverlay::hasRoutedEnchantments)
                .filter(station -> isWithinInteractionRange(minecraft, camera, station.pos()))
                .toList();
        clearDisplayState();
        if (stations.isEmpty()) {
            if (scannedStations.stream().noneMatch(RoutedEnchantmentOrbOverlay::hasRoutedEnchantments)) {
                renderNearbyDiagnostics(event.getPoseStack(), camera, scannedStations);
            }
            return;
        }
        renderTuningMarkers(event.getPoseStack(), camera, stations, renderTime);
    }

    public static void renderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.options.hideGui
                || minecraft.screen != null
                || !isHoldingAttunementFocus(minecraft)) {
            return;
        }
        Optional<StationPreview> selected = selectedStation(minecraft)
                .filter(station -> station.preview().breakdown().isPresent())
                .filter(station -> isWithinInteractionRange(
                        minecraft,
                        minecraft.gameRenderer.getMainCamera(),
                        station.pos()
                ));
        if (selected.isEmpty()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Component action = Component.translatable("gui.betterenchanting.tuning.open");
        int panelWidth = Math.min(width - 16, Math.max(170, font.width(action) + 24));
        int panelHeight = 24;
        int x = (width - panelWidth) / 2;
        int y = Math.min(height - panelHeight - 12, height / 2 + 26);
        graphics.fill(x, y, x + panelWidth, y + panelHeight, HUD_BACKGROUND);
        graphics.renderOutline(x, y, panelWidth, panelHeight, HUD_BORDER);
        graphics.fill(x + 6, y + 5, x + 9, y + panelHeight - 5, ACTIVE_COLOR);
        graphics.drawCenteredString(font, action, x + panelWidth / 2 + 4, y + 8, HUD_TEXT);
    }

    public static void handleInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        if (!isHoldingAttunementFocus(minecraft)) {
            return;
        }
        selectedStation(minecraft)
                .filter(station -> station.preview().breakdown().isPresent())
                .filter(station -> isWithinInteractionRange(
                        minecraft,
                        minecraft.gameRenderer.getMainCamera(),
                        station.pos()
                ))
                .ifPresent(station -> {
                    minecraft.setScreen(new RoutedEnchantmentTuningScreen(station.pos()));
                    event.setCanceled(true);
                });
    }

    private static void renderTuningMarkers(
            PoseStack poseStack,
            Camera camera,
            List<StationPreview> stations,
            double renderTime
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(ORB_BILLBOARD);
        Optional<BlockPos> selected = selectedStation(minecraft).map(StationPreview::pos);
        List<Vec3> selectedAnchors = new ArrayList<>();

        poseStack.pushPose();
        for (StationPreview station : stations) {
            boolean targeted = selected.filter(station.pos()::equals).isPresent();
            double pulse = (Math.sin(renderTime * 0.18D + station.pos().asLong() * 0.001D) + 1.0D) * 0.5D;
            Vec3 anchor = Vec3.atCenterOf(station.pos()).add(0.0D, 1.16D, 0.0D);
            double radius = (targeted ? 0.16D : 0.11D) * (1.0D + pulse * 0.12D);
            int color = targeted ? HOVER_COLOR : ACTIVE_COLOR;
            drawBillboardDiamond(
                    poseStack,
                    camera,
                    consumer,
                    anchor,
                    radius * 1.65D,
                    red(color),
                    green(color),
                    blue(color),
                    targeted ? 0.24F : 0.13F
            );
            drawBillboardDiamond(
                    poseStack,
                    camera,
                    consumer,
                    anchor,
                    radius,
                    red(color),
                    green(color),
                    blue(color),
                    targeted ? 0.95F : 0.70F
            );
            if (targeted) {
                selectedAnchors.add(anchor);
            }
        }
        poseStack.popPose();
        buffer.endBatch(ORB_BILLBOARD);

        for (Vec3 anchor : selectedAnchors) {
            renderText(
                    poseStack,
                    camera,
                    Component.translatable("gui.betterenchanting.tuning.title"),
                    anchor.add(0.0D, 0.23D, 0.0D),
                    ACTIVE_COLOR
            );
        }
    }

    private static boolean canUseOrb(RenderOrb orb) {
        return !orb.active() || (orb.overleveled() && !orb.overlevelBonusActive());
    }

    private static boolean isHoldingAttunementFocus(Minecraft minecraft) {
        return minecraft.player != null
                && (minecraft.player.getMainHandItem().is(ModItems.ATTUNEMENT_FOCUS.get())
                || minecraft.player.getOffhandItem().is(ModItems.ATTUNEMENT_FOCUS.get()));
    }

    private static Component orbAction(RenderOrb orb) {
        if (orb.partIndex() < 0 && !orb.active()) {
            return Component.literal("Use to prioritize on final tool").withStyle(ChatFormatting.GRAY);
        }
        if (!orb.active()) {
            return Component.literal("Use to make active").withStyle(ChatFormatting.GRAY);
        }
        if (orb.overleveled() && orb.overlevelBonusActive()) {
            return Component.literal("Applying +1 bonus").withStyle(ChatFormatting.GOLD);
        }
        if (orb.overleveled()) {
            return Component.literal("Use to focus +1 bonus").withStyle(ChatFormatting.GRAY);
        }
        return Component.literal("Currently active").withStyle(ChatFormatting.GRAY);
    }

    private static void clearDisplayState() {
        lastHits = List.of();
        lastOrbs = List.of();
        lastPartClusters = List.of();
        lastHovered = Optional.empty();
        partOrbitStates.clear();
        stationRevealStates.clear();
    }

    private static List<StationRender> revealStations(List<StationPreview> stations, Minecraft minecraft, Camera camera, double renderTime, boolean overlayEnabled) {
        Set<BlockPos> seen = new HashSet<>();
        List<StationRender> renders = new ArrayList<>();
        for (StationPreview station : stations) {
            seen.add(station.pos());
            boolean inRange = isWithinInteractionRange(minecraft, camera, station.pos());
            StationRevealState state = stationRevealStates.computeIfAbsent(station.pos(), unused -> new StationRevealState(renderTime));
            boolean targetVisible = overlayEnabled && inRange;
            state.advance(renderTime, targetVisible);
            double revealWeight = state.revealWeight();
            if (targetVisible || revealWeight > MIN_REVEAL_RENDER_WEIGHT) {
                renders.add(new StationRender(station, revealWeight, targetVisible && revealWeight > 0.62D));
            }
        }
        stationRevealStates.entrySet().removeIf(entry -> !seen.contains(entry.getKey())
                || (!entry.getValue().targetVisible && entry.getValue().revealWeight() <= MIN_REVEAL_RENDER_WEIGHT));
        return renders;
    }

    private static boolean canUseRoutingOverlay(Minecraft minecraft) {
        if (BetterEnchantingConfig.alwaysShowsRoutedEnchantmentOverlay()) {
            return true;
        }
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.player.getMainHandItem().is(ModItems.ATTUNEMENT_FOCUS.get())
                || minecraft.player.getOffhandItem().is(ModItems.ATTUNEMENT_FOCUS.get());
    }

    private static boolean isWithinInteractionRange(Minecraft minecraft, Camera camera, BlockPos pos) {
        if (minecraft.player == null) {
            return false;
        }

        double range = minecraft.player.blockInteractionRange();
        Vec3 eye = camera.getPosition();
        double dx = distanceOutside(eye.x, pos.getX(), pos.getX() + 1.0D);
        double dy = distanceOutside(eye.y, pos.getY(), pos.getY() + 1.0D);
        double dz = distanceOutside(eye.z, pos.getZ(), pos.getZ() + 1.0D);
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    private static double distanceOutside(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0D;
    }

    private static List<StationPreview> nearbyStations(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            cachedLevel = null;
            cachedStations = List.of();
            return List.of();
        }

        BlockPos origin = minecraft.player.blockPosition();
        long gameTime = level.getGameTime();
        if (cachedLevel == level
                && cachedScanOrigin.equals(origin)
                && gameTime - cachedScanGameTime < STATION_SCAN_INTERVAL_TICKS) {
            return cachedStations;
        }

        List<StationPreview> stations = new ArrayList<>();
        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinBuildHeight(), origin.getY() - STATION_VERTICAL_SCAN_RADIUS);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + STATION_VERTICAL_SCAN_RADIUS);
        for (int x = origin.getX() - STATION_SCAN_RADIUS; x <= origin.getX() + STATION_SCAN_RADIUS; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = origin.getZ() - STATION_SCAN_RADIUS; z <= origin.getZ() + STATION_SCAN_RADIUS; z++) {
                    scanPos.set(x, y, z);
                    stationPreview(level, scanPos.immutable())
                            .ifPresent(stations::add);
                }
            }
        }

        Vec3 playerPosition = minecraft.player.position();
        stations.sort(Comparator.comparingDouble(station -> Vec3.atCenterOf(station.pos()).distanceToSqr(playerPosition)));
        cachedLevel = level;
        cachedScanOrigin = origin.immutable();
        cachedScanGameTime = gameTime;
        cachedStations = List.copyOf(stations);
        return cachedStations;
    }

    private static Optional<StationPreview> selectedStation(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        return stationPreview(minecraft.level, hit.getBlockPos().immutable());
    }

    private static Optional<StationPreview> stationPreview(ClientLevel level, BlockPos pos) {
        if (level == null) {
            return Optional.empty();
        }
        return MobsToolForgingCompat.stationRoutedEnchantmentPreview(level, pos)
                .map(preview -> new StationPreview(pos.immutable(), preview));
    }

    private static Vec3 stationRightVector(Vec3 anchor, Vec3 cameraPosition) {
        Vec3 toCamera = cameraPosition.subtract(anchor);
        Vec3 right = new Vec3(toCamera.z, 0.0D, -toCamera.x);
        if (right.lengthSqr() < 0.001D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return right.normalize();
    }

    private static void layoutStation(
            StationRoutedEnchantmentPreview preview,
            RoutedEnchantmentBreakdown breakdown,
            BlockPos stationPos,
            Vec3 anchor,
            Vec3 right,
            Vec3 up,
            double revealWeight,
            boolean interactive,
            double renderTime,
            List<RenderToolCluster> toolClusters,
            List<RenderPartCluster> partClusters,
            List<RenderOrb> orbs
    ) {
        double reveal = smoothStep(revealWeight);
        double hiddenWeight = 1.0D - reveal;
        if (!preview.toolStack().isEmpty()) {
            double toolBob = Math.sin(renderTime * TOOL_FLOAT_SPEED + stationPos.asLong() * 0.001D) * TOOL_FLOAT_AMPLITUDE * reveal;
            Vec3 toolPosition = anchor.add(0.0D, TOOL_HIDDEN_Y_OFFSET * hiddenWeight + toolBob, 0.0D);
            toolClusters.add(new RenderToolCluster(toolPosition, preview.toolStack().copyWithCount(1), revealWeight));

            List<RoutedEnchantmentState> toolEnchantments = breakdown.toolEnchantments().stream()
                    .sorted(Comparator.comparing(enchantment -> enchantment.enchantmentId().toString()))
                    .toList();
            boolean hasFinalConflict = toolEnchantments.stream().anyMatch(enchantment -> !enchantment.active());
            if (hasFinalConflict) {
                double toolOrbRadius = TOOL_ENCHANTMENT_RING_RADIUS * reveal;
                for (int index = 0; index < toolEnchantments.size(); index++) {
                    RoutedEnchantmentState enchantment = toolEnchantments.get(index);
                    double orbAngle = enchantmentOrbitAngle(index, toolEnchantments.size());
                    Vec3 position = toolPosition
                            .add(right.scale(Math.cos(orbAngle) * toolOrbRadius))
                            .add(up.scale(Math.sin(orbAngle) * toolOrbRadius));
                    int displayLevel = enchantment.effectiveLevel() > 0
                            ? enchantment.effectiveLevel()
                            : enchantment.level();
                    OrbHit hit = new OrbHit(position, stationPos, -1, enchantment.enchantmentId());
                    orbs.add(new RenderOrb(
                            hit,
                            toolPosition,
                            right,
                            up,
                            toolOrbRadius,
                            orbAngle,
                            orbName(enchantment, displayLevel),
                            finalToolStateDescription(enchantment),
                            enchantment.active(),
                            false,
                            false,
                            -1,
                            "assembled_tool",
                            revealWeight,
                            interactive
                    ));
                }
            }
        }

        List<RoutedPartBreakdown> parts = breakdown.parts().stream()
                .filter(part -> !part.partStack().isEmpty())
                .toList();
        for (int partSlot = 0; partSlot < parts.size(); partSlot++) {
            RoutedPartBreakdown part = parts.get(partSlot);
            double partAngle = partRingAngle(partSlot, parts.size());
            Vec3 outward = billboardVector(right, up, partAngle);
            Vec3 tangent = billboardVector(right, up, partAngle + Math.PI / 2.0D);
            double animationPhase = renderTime * PART_FLOAT_SPEED + part.partIndex() * 1.731D + stationPos.asLong() * 0.0003D;
            double bob = Math.sin(animationPhase) * PART_FLOAT_AMPLITUDE * reveal;
            double sway = Math.cos(animationPhase * 0.77D) * PART_SWAY_AMPLITUDE * reveal;
            Vec3 expandedPartPosition = anchor
                    .add(right.scale(Math.cos(partAngle) * PART_RING_HORIZONTAL_RADIUS))
                    .add(up.scale(Math.sin(partAngle) * PART_RING_VERTICAL_RADIUS))
                    .add(up.scale(bob))
                    .add(tangent.scale(sway));
            Vec3 partPosition = lerpVec(anchor, expandedPartPosition, reveal);
            partClusters.add(new RenderPartCluster(
                    partPosition,
                    anchor,
                    part.partStack().copyWithCount(1),
                    part.slotId(),
                    part.partIndex(),
                    partActiveWeight(part),
                    revealWeight
            ));
            List<RoutedEnchantmentState> enchantments = part.enchantments().stream()
                    .sorted(Comparator.comparing(enchantment -> enchantment.enchantmentId().toString()))
                    .toList();
            double orbRadius = enchantmentRingRadius(enchantments.size()) * reveal;
            for (int index = 0; index < enchantments.size(); index++) {
                RoutedEnchantmentState enchantment = enchantments.get(index);
                double orbAngle = enchantmentOrbitAngle(index, enchantments.size());
                Vec3 position = partPosition
                        .add(outward.scale(Math.cos(orbAngle) * orbRadius))
                        .add(tangent.scale(Math.sin(orbAngle) * orbRadius));
                int displayLevel = enchantment.effectiveLevel() > 0 ? enchantment.effectiveLevel() : enchantment.level();
                Component name = orbName(enchantment, displayLevel);
                Component description = stateDescription(part, enchantment);
                OrbHit hit = new OrbHit(position, stationPos, part.partIndex(), enchantment.enchantmentId());
                orbs.add(new RenderOrb(
                        hit,
                        partPosition,
                        outward,
                        tangent,
                        orbRadius,
                        orbAngle,
                        name,
                        description,
                        enchantment.active(),
                        enchantment.overleveled(),
                        enchantment.overlevelBonusActive(),
                        part.partIndex(),
                        part.slotId(),
                        revealWeight,
                        interactive
                ));
            }
        }
    }

    private static Vec3 billboardVector(Vec3 right, Vec3 up, double angle) {
        return right.scale(Math.cos(angle))
                .add(up.scale(Math.sin(angle)))
                .normalize();
    }

    private static double partRingAngle(int index, int count) {
        if (count <= 1) {
            return Math.PI / 2.0D;
        }
        if (count == 2) {
            return Math.PI + Math.PI * index;
        }
        return Math.PI - Math.PI * index / (count - 1);
    }

    private static double enchantmentRingRadius(int count) {
        return Math.min(ENCHANTMENT_RING_MAX_RADIUS, ENCHANTMENT_RING_RADIUS + Math.max(0, count - 2) * ENCHANTMENT_RING_RADIUS_STEP);
    }

    private static double enchantmentOrbitAngle(int index, int count) {
        if (count <= 1) {
            return 0.0D;
        }
        return Math.PI * 2.0D * index / count;
    }

    private static double partActiveWeight(RoutedPartBreakdown part) {
        if (part.enchantments().isEmpty()) {
            return 0.35D;
        }
        long activeEnchantments = part.enchantments().stream()
                .filter(RoutedEnchantmentState::active)
                .count();
        return activeEnchantments / (double) part.enchantments().size();
    }

    private static boolean hasRoutedEnchantments(StationPreview station) {
        return station.preview().breakdown()
                .map(RoutedEnchantmentOrbOverlay::hasRoutedEnchantments)
                .orElse(false);
    }

    private static boolean hasRoutedEnchantments(RoutedEnchantmentBreakdown breakdown) {
        return breakdown.parts().stream().anyMatch(RoutedEnchantmentOrbOverlay::hasRoutedEnchantments);
    }

    private static boolean hasRoutedEnchantments(RoutedPartBreakdown part) {
        return !part.enchantments().isEmpty();
    }

    private static Component stateDescription(RoutedPartBreakdown part, RoutedEnchantmentState enchantment) {
        String slot = readableSlot(part.slotId());
        if (enchantment.active()) {
            if (enchantment.overleveled() && enchantment.overlevelBonusActive()) {
                return Component.literal("Bonus focused on " + slot).withStyle(ChatFormatting.GOLD);
            }
            if (enchantment.overleveled()) {
                return Component.literal("Active on " + slot + ", bonus ready").withStyle(ChatFormatting.LIGHT_PURPLE);
            }
            return Component.literal("Active on " + slot).withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        return Component.literal("Dormant on " + slot).withStyle(ChatFormatting.GRAY);
    }

    private static Component finalToolStateDescription(RoutedEnchantmentState enchantment) {
        return enchantment.active()
                ? Component.literal("Active on final tool").withStyle(ChatFormatting.LIGHT_PURPLE)
                : Component.literal("Dormant on final tool (category limit)").withStyle(ChatFormatting.GRAY);
    }

    private static Component orbName(RoutedEnchantmentState enchantment, int displayLevel) {
        MutableComponent name = Enchantment.getFullname(enchantment.enchantment(), displayLevel).copy();
        if (enchantment.active()
                && !enchantment.overleveled()
                && EnchantmentLevelRules.isOverlevelPrimed(enchantment.enchantment(), displayLevel)) {
            name.withStyle(ChatFormatting.ITALIC);
        }
        if (enchantment.overleveled()) {
            name.append(Component.literal(" " + OVERLEVEL_MARKER).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        return name;
    }

    private static String readableSlot(String slotId) {
        if (slotId == null || slotId.isBlank()) {
            return "part";
        }

        String[] words = slotId.replace('_', ' ').split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.isEmpty() ? "part" : builder.toString();
    }

    private static void renderToolPartLinks(PoseStack poseStack, Camera camera, List<RenderPartCluster> partClusters, double renderTime) {
        if (partClusters.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());

        poseStack.pushPose();
        for (RenderPartCluster cluster : partClusters) {
            drawToolPartLink(poseStack, camera, lineConsumer, cluster, renderTime);
        }
        poseStack.popPose();
        buffer.endBatch(RenderType.lines());
    }

    private static void drawToolPartLink(
            PoseStack poseStack,
            Camera camera,
            VertexConsumer consumer,
            RenderPartCluster cluster,
            double renderTime
    ) {
        Vec3 direction = cluster.position().subtract(cluster.toolPosition());
        double length = direction.length();
        if (length <= TOOL_LINK_CENTER_GAP + TOOL_LINK_PART_GAP) {
            return;
        }

        Vec3 unit = direction.scale(1.0D / length);
        Vec3 start = cluster.toolPosition().add(unit.scale(TOOL_LINK_CENTER_GAP));
        Vec3 end = cluster.position().subtract(unit.scale(TOOL_LINK_PART_GAP));
        int startColor = blendColor(DORMANT_COLOR, STATUS_COLOR, 0.45D);
        int endColor = blendColor(DORMANT_COLOR, ACTIVE_COLOR, cluster.activeWeight());
        double shimmer = (Math.sin(renderTime * ORB_PULSE_SPEED * 0.5D + cluster.partIndex() * 1.618D) + 1.0D) * 0.5D;
        double reveal = smoothStep(cluster.revealWeight());
        float startAlpha = (float) (lerp(0.16D, 0.26D, cluster.activeWeight()) * reveal);
        float endAlpha = (float) ((lerp(0.34D, 0.64D, cluster.activeWeight()) + shimmer * 0.08D) * reveal);
        drawWorldGradientLine(poseStack, camera, consumer, start, end, startColor, startAlpha, endColor, endAlpha);
    }

    private static void renderToolAndPartBillboards(
            PoseStack poseStack,
            Camera camera,
            List<RenderToolCluster> toolClusters,
            List<RenderPartCluster> partClusters
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        for (RenderToolCluster cluster : toolClusters) {
            double reveal = smoothStep(cluster.revealWeight());
            renderItemBillboard(poseStack, camera, buffer, cluster.toolStack(), cluster.position(), (float) (TOOL_BILLBOARD_SCALE * lerp(0.72D, 1.0D, reveal)), 11000);
        }
        for (RenderPartCluster cluster : partClusters) {
            double reveal = smoothStep(cluster.revealWeight());
            if (reveal > 0.02D) {
                renderItemBillboard(poseStack, camera, buffer, cluster.partStack(), cluster.position(), (float) (PART_BILLBOARD_SCALE * reveal), cluster.partIndex());
            }
        }
        buffer.endBatch();
    }

    private static void renderOrbBillboards(PoseStack poseStack, Camera camera, List<RenderOrb> orbs, Optional<OrbHit> hovered, double renderTime) {
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer orbConsumer = buffer.getBuffer(ORB_BILLBOARD);
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());

        poseStack.pushPose();
        for (RenderOrb orb : orbs) {
            double reveal = smoothStep(orb.revealWeight());
            if (reveal <= 0.02D) {
                continue;
            }
            boolean isHovered = hovered.filter(orb.hit()::sameTarget).isPresent();
            OrbVisualState visual = visualStateFor(orb, renderTime, isHovered);
            int color = blendColor(visual.color(), HOVER_COLOR, HOVER_COLOR_BLEND * visual.hoverWeight());
            float red = red(color);
            float green = green(color);
            float blue = blue(color);
            double pulse = pulse(orb, renderTime, visual.activeWeight());
            Vec3 position = animatedOrbPosition(orb, renderTime);
            float alpha = (float) ((lerp(lerp(0.5D, 0.68D, visual.activeWeight()), 0.88D, visual.hoverWeight()) + pulse * 0.12D) * reveal);
            double radius = lerp(0.13D, 0.17D, visual.hoverWeight()) * (1.0D + pulse * 0.18D);
            drawBillboardDiamond(poseStack, camera, orbConsumer, position, radius * (1.45D + pulse * 0.22D), red, green, blue, (float) (lerp(0.13D, 0.24D, visual.hoverWeight()) * reveal));
            drawBillboardDiamond(poseStack, camera, orbConsumer, position, radius, red, green, blue, alpha);
            drawBillboardDiamond(poseStack, camera, orbConsumer, position, radius * 0.48D, 1.0F, 1.0F, 1.0F, (float) (lerp(0.42D, 0.9D, visual.hoverWeight()) * reveal));
            if (orb.overleveled() && orb.active()) {
                double bonusPulse = (Math.sin(renderTime * 0.18D + animationPhase(orb)) + 1.0D) * 0.5D;
                double bonusWeight = visual.bonusWeight();
                int bonusColor = blendColor(ACTIVE_COLOR, OVERLEVEL_BONUS_COLOR, Math.max(0.35D, bonusWeight));
                float bonusRed = red(bonusColor);
                float bonusGreen = green(bonusColor);
                float bonusBlue = blue(bonusColor);
                double ringRadius = radius * lerp(1.72D, 2.25D + bonusPulse * 0.16D, bonusWeight);
                float ringAlpha = (float) (lerp(0.18D, 0.82D, bonusWeight) * reveal * lerp(0.65D, 1.0D, visual.activeWeight()));
                drawBillboardDiamond(poseStack, camera, orbConsumer, position, ringRadius, bonusRed, bonusGreen, bonusBlue, ringAlpha);
                if (bonusWeight > 0.03D) {
                    float focusAlpha = (float) (bonusWeight * reveal * 0.92D);
                    double markRadius = radius * 1.42D;
                    drawBillboardLine(poseStack, camera, lineConsumer, position, -markRadius, 0.0D, markRadius, 0.0D, bonusRed, bonusGreen, bonusBlue, focusAlpha);
                    drawBillboardLine(poseStack, camera, lineConsumer, position, 0.0D, -markRadius, 0.0D, markRadius, bonusRed, bonusGreen, bonusBlue, focusAlpha);
                }
            }
            double activeWeight = visual.activeWeight();
            double dormantWeight = 1.0D - activeWeight;
            if (activeWeight > 0.01D) {
                drawBillboardLine(poseStack, camera, lineConsumer, position, -radius, 0.0D, radius, 0.0D, red, green, blue, (float) (activeWeight * reveal));
                drawBillboardLine(poseStack, camera, lineConsumer, position, 0.0D, -radius, 0.0D, radius, red, green, blue, (float) (activeWeight * reveal));
            }
            if (dormantWeight > 0.01D) {
                drawBillboardLine(poseStack, camera, lineConsumer, position, -radius, -radius, radius, radius, red, green, blue, (float) (0.92D * dormantWeight * reveal));
            }
        }
        poseStack.popPose();
        buffer.endBatch(ORB_BILLBOARD);
        buffer.endBatch(RenderType.lines());
    }

    private static OrbHit orbHitAt(RenderOrb orb, double renderTime) {
        return new OrbHit(
                animatedOrbPosition(orb, renderTime),
                orb.hit().stationPos(),
                orb.hit().partIndex(),
                orb.hit().enchantmentId()
        );
    }

    private static Vec3 animatedOrbPosition(RenderOrb orb, double renderTime) {
        double angle = orb.baseAngle() + orbitPhase(orb) + orbitTime(orb, renderTime) * ORB_ORBIT_SPEED;
        return orbitPosition(orb.partPosition(), orb.orbitX(), orb.orbitY(), orb.orbitRadius(), angle);
    }

    private static double orbitTime(RenderOrb orb, double renderTime) {
        return Optional.ofNullable(partOrbitStates.get(PartOrbitKey.from(orb)))
                .map(state -> state.orbitTime)
                .orElse(renderTime);
    }

    private static void updateOrbitStates(List<RenderOrb> orbs, Optional<PartOrbitKey> pausedPart, double renderTime) {
        for (RenderOrb orb : orbs) {
            PartOrbitKey key = PartOrbitKey.from(orb);
            PartOrbitState state = partOrbitStates.computeIfAbsent(key, unused -> new PartOrbitState(renderTime));
            state.advance(renderTime, pausedPart.filter(key::equals).isPresent());
        }
        partOrbitStates.entrySet().removeIf(entry -> renderTime - entry.getValue().lastSeenTime > VISUAL_STATE_TTL_TICKS);
    }

    private static Vec3 orbitPosition(Vec3 center, Vec3 orbitX, Vec3 orbitY, double radius, double angle) {
        return center
                .add(orbitX.scale(Math.cos(angle) * radius))
                .add(orbitY.scale(Math.sin(angle) * radius));
    }

    private static double orbitPhase(RenderOrb orb) {
        int hash = orb.partIndex();
        hash = 31 * hash + orb.slotId().hashCode();
        hash = 31 * hash + orb.hit().stationPos().hashCode();
        return (hash & 0xFFFF) / 65535.0D * Math.PI * 2.0D;
    }

    private static double pulse(RenderOrb orb, double renderTime, double activeWeight) {
        return lerp(
                pulseAtSpeed(orb, renderTime, DORMANT_ANIMATION_SPEED),
                pulseAtSpeed(orb, renderTime, 1.0D),
                activeWeight
        );
    }

    private static double pulseAtSpeed(RenderOrb orb, double renderTime, double speed) {
        return (Math.sin(renderTime * ORB_PULSE_SPEED * speed + animationPhase(orb)) + 1.0D) * 0.5D;
    }

    private static double animationPhase(RenderOrb orb) {
        int hash = orb.hit().enchantmentId().hashCode();
        hash = 31 * hash + orb.partIndex();
        hash = 31 * hash + orb.slotId().hashCode();
        return (hash & 0xFFFF) / 65535.0D * Math.PI * 2.0D;
    }

    private static OrbVisualState visualStateFor(RenderOrb orb, double renderTime, boolean hovered) {
        OrbVisualTransition transition = visualTransitionFor(orb, renderTime);
        double targetHoverWeight = hovered ? 1.0D : 0.0D;
        if (Math.abs(transition.targetHoverWeight - targetHoverWeight) > 0.001D) {
            transition.startHoverWeight = transition.hoverWeight(renderTime);
            transition.targetHoverWeight = targetHoverWeight;
            transition.hoverTransitionStartTime = renderTime;
        }

        transition.lastSeenTime = renderTime;
        double activeWeight = transition.activeWeight(renderTime);
        return new OrbVisualState(
                activeWeight,
                transition.hoverWeight(renderTime),
                transition.bonusWeight(renderTime),
                blendColor(DORMANT_COLOR, ACTIVE_COLOR, activeWeight)
        );
    }

    private static OrbVisualTransition visualTransitionFor(RenderOrb orb, double renderTime) {
        OrbVisualKey key = OrbVisualKey.from(orb.hit());
        double targetWeight = orb.active() ? 1.0D : 0.0D;
        double targetBonusWeight = orb.overlevelBonusActive() ? 1.0D : 0.0D;
        OrbVisualTransition transition = orbVisualTransitions.get(key);
        if (transition == null) {
            transition = new OrbVisualTransition(targetWeight, targetBonusWeight, 0.0D, renderTime);
            orbVisualTransitions.put(key, transition);
        } else if (Math.abs(transition.targetWeight - targetWeight) > 0.001D) {
            transition.startWeight = transition.activeWeight(renderTime);
            transition.targetWeight = targetWeight;
            transition.transitionStartTime = renderTime;
        }
        if (Math.abs(transition.targetBonusWeight - targetBonusWeight) > 0.001D) {
            transition.startBonusWeight = transition.bonusWeight(renderTime);
            transition.targetBonusWeight = targetBonusWeight;
            transition.bonusTransitionStartTime = renderTime;
        }
        transition.lastSeenTime = renderTime;
        return transition;
    }

    private static void discardStaleVisualStates(double renderTime) {
        orbVisualTransitions.entrySet().removeIf(entry -> renderTime - entry.getValue().lastSeenTime > VISUAL_STATE_TTL_TICKS);
    }

    private static Optional<RenderOrb> selectedOrb() {
        return lastHovered.flatMap(hit -> lastOrbs.stream()
                .filter(orb -> orb.hit().sameTarget(hit))
                .findFirst());
    }

    private static Optional<OrbHit> pick(List<OrbHit> hits, Minecraft minecraft) {
        if (hits.isEmpty() || minecraft.player == null) {
            return Optional.empty();
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 origin = camera.getPosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        OrbHit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (OrbHit hit : hits) {
            Vec3 toOrb = hit.position().subtract(origin);
            double alongRay = toOrb.dot(look);
            if (alongRay < 0.5D || alongRay > PICK_MAX_DISTANCE) {
                continue;
            }

            double distance = toOrb.subtract(look.scale(alongRay)).length();
            if (distance <= PICK_RADIUS && distance < bestDistance) {
                best = hit;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    private static void renderStatus(PoseStack poseStack, Camera camera, StationPreview station, Vec3 anchor) {
        String status = statusText(station);
        if (status.isBlank()) {
            return;
        }
        renderText(poseStack, camera, Component.literal(status), anchor.add(0.0D, 0.36D, 0.0D), STATUS_COLOR);
    }

    private static void renderSelectedStatus(PoseStack poseStack, Camera camera, Minecraft minecraft) {
        selectedStation(minecraft)
                .ifPresent(station -> renderStatus(poseStack, camera, station, Vec3.atCenterOf(station.pos()).add(0.0D, 0.92D, 0.0D)));
    }

    private static void renderNearbyDiagnostics(PoseStack poseStack, Camera camera, List<StationPreview> stations) {
        boolean rendered = false;
        for (StationPreview station : stations) {
            String status = station.preview().status();
            if (status == null || status.isBlank()) {
                continue;
            }
            renderStatus(poseStack, camera, station, Vec3.atCenterOf(station.pos()).add(0.0D, 0.92D, 0.0D));
            rendered = true;
        }
        if (!rendered) {
            renderSelectedStatus(poseStack, camera, Minecraft.getInstance());
        }
    }

    private static String statusText(StationPreview station) {
        String status = station.preview().status();
        if (status != null && !status.isBlank()) {
            return status;
        }
        return station.preview().breakdown().filter(breakdown -> !hasRoutedEnchantments(breakdown)).isPresent()
                ? "No routed enchantments are stored on this item"
                : "";
    }

    private static void renderText(PoseStack poseStack, Camera camera, Component text, Vec3 position, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        renderBillboardText(poseStack, camera, buffer, font, text, position, color, LABEL_SCALE, TEXT_BACKGROUND);
        buffer.endBatch();
    }

    private static void renderBillboardText(
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource buffer,
            Font font,
            Component text,
            Vec3 position,
            int color,
            float scale,
            int backgroundColor
    ) {
        Vec3 relative = position.subtract(camera.getPosition());
        poseStack.pushPose();
        poseStack.translate(relative.x, relative.y, relative.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-scale, -scale, scale);
        float x = -font.width(text) / 2.0F;
        font.drawInBatch(text, x, 0.0F, color, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, backgroundColor, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void renderItemBillboard(
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource buffer,
            ItemStack stack,
            Vec3 position,
            float scale,
            int seed
    ) {
        if (stack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Vec3 relative = position.subtract(camera.getPosition());
        poseStack.pushPose();
        poseStack.translate(relative.x, relative.y, relative.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(scale, scale, scale);
        minecraft.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                minecraft.level,
                seed
        );
        poseStack.popPose();
    }

    private static void drawBillboardDiamond(
            PoseStack poseStack,
            Camera camera,
            VertexConsumer consumer,
            Vec3 position,
            double radius,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3 relative = position.subtract(camera.getPosition());
        poseStack.pushPose();
        poseStack.translate(relative.x, relative.y, relative.z);
        poseStack.mulPose(camera.rotation());
        PoseStack.Pose pose = poseStack.last();
        float r = (float) radius;
        consumer.addVertex(pose, 0.0F, r, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, r, 0.0F, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, 0.0F, -r, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, -r, 0.0F, 0.0F).setColor(red, green, blue, alpha);
        poseStack.popPose();
    }

    private static void drawBillboardLine(
            PoseStack poseStack,
            Camera camera,
            VertexConsumer consumer,
            Vec3 position,
            double startX,
            double startY,
            double endX,
            double endY,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3 relative = position.subtract(camera.getPosition());
        poseStack.pushPose();
        poseStack.translate(relative.x, relative.y, relative.z);
        poseStack.mulPose(camera.rotation());
        Vec3 normal = new Vec3(endX - startX, endY - startY, 0.0D).normalize();
        consumer.addVertex(poseStack.last(), (float) startX, (float) startY, 0.0F)
                .setColor(red, green, blue, alpha)
                .setNormal(poseStack.last(), (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(poseStack.last(), (float) endX, (float) endY, 0.0F)
                .setColor(red, green, blue, alpha)
                .setNormal(poseStack.last(), (float) normal.x, (float) normal.y, (float) normal.z);
        poseStack.popPose();
    }

    private static void drawWorldGradientLine(
            PoseStack poseStack,
            Camera camera,
            VertexConsumer consumer,
            Vec3 start,
            Vec3 end,
            int startColor,
            float startAlpha,
            int endColor,
            float endAlpha
    ) {
        Vec3 relativeStart = start.subtract(camera.getPosition());
        Vec3 relativeEnd = end.subtract(camera.getPosition());
        Vec3 normal = relativeEnd.subtract(relativeStart).normalize();
        PoseStack.Pose pose = poseStack.last();
        consumer.addVertex(pose, (float) relativeStart.x, (float) relativeStart.y, (float) relativeStart.z)
                .setColor(red(startColor), green(startColor), blue(startColor), startAlpha)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose, (float) relativeEnd.x, (float) relativeEnd.y, (float) relativeEnd.z)
                .setColor(red(endColor), green(endColor), blue(endColor), endAlpha)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static int blendColor(int from, int to, double amount) {
        double clamped = clamp(amount);
        int alpha = (int) Math.round(lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, clamped));
        int red = (int) Math.round(lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, clamped));
        int green = (int) Math.round(lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, clamped));
        int blue = (int) Math.round(lerp(from & 0xFF, to & 0xFF, clamped));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * clamp(amount);
    }

    private static Vec3 lerpVec(Vec3 from, Vec3 to, double amount) {
        return new Vec3(
                lerp(from.x, to.x, amount),
                lerp(from.y, to.y, amount),
                lerp(from.z, to.z, amount)
        );
    }

    private static double smoothStep(double amount) {
        double clamped = clamp(amount);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    private record StationPreview(BlockPos pos, StationRoutedEnchantmentPreview preview) {
    }

    private record StationRender(StationPreview preview, double revealWeight, boolean interactive) {
    }

    private record RenderToolCluster(Vec3 position, ItemStack toolStack, double revealWeight) {
    }

    private record RenderPartCluster(Vec3 position, Vec3 toolPosition, ItemStack partStack, String slotId, int partIndex, double activeWeight, double revealWeight) {
    }

    private record RenderOrb(
            OrbHit hit,
            Vec3 partPosition,
            Vec3 orbitX,
            Vec3 orbitY,
            double orbitRadius,
            double baseAngle,
            Component name,
            Component description,
            boolean active,
            boolean overleveled,
            boolean overlevelBonusActive,
            int partIndex,
            String slotId,
            double revealWeight,
            boolean interactive
    ) {
    }

    private record OrbVisualState(double activeWeight, double hoverWeight, double bonusWeight, int color) {
    }

    private record PartOrbitKey(BlockPos stationPos, int partIndex) {
        private static PartOrbitKey from(OrbHit hit) {
            return new PartOrbitKey(hit.stationPos(), hit.partIndex());
        }

        private static PartOrbitKey from(RenderOrb orb) {
            return new PartOrbitKey(orb.hit().stationPos(), orb.partIndex());
        }

        private boolean matches(RenderOrb orb) {
            return stationPos.equals(orb.hit().stationPos())
                    && partIndex == orb.partIndex();
        }
    }

    private record OrbVisualKey(BlockPos stationPos, int partIndex, ResourceLocation enchantmentId) {
        private static OrbVisualKey from(OrbHit hit) {
            return new OrbVisualKey(hit.stationPos(), hit.partIndex(), hit.enchantmentId());
        }
    }

    private static final class StationRevealState {
        private double revealWeight;
        private double lastRenderTime;
        private boolean targetVisible;
        private double lastSeenTime;

        private StationRevealState(double renderTime) {
            this.revealWeight = 0.0D;
            this.lastRenderTime = renderTime;
            this.targetVisible = false;
            this.lastSeenTime = renderTime;
        }

        private void advance(double renderTime, boolean visible) {
            double delta = Math.max(0.0D, renderTime - lastRenderTime);
            double direction = visible ? 1.0D : -1.0D;
            revealWeight = clamp(revealWeight + direction * delta / REVEAL_TRANSITION_TICKS);
            targetVisible = visible;
            lastRenderTime = renderTime;
            lastSeenTime = renderTime;
        }

        private double revealWeight() {
            return revealWeight;
        }
    }

    private static final class PartOrbitState {
        private double orbitTime;
        private double lastRenderTime;
        private double startPauseWeight;
        private double targetPauseWeight;
        private double pauseTransitionStartTime;
        private double lastSeenTime;

        private PartOrbitState(double renderTime) {
            this.orbitTime = renderTime;
            this.lastRenderTime = renderTime;
            this.startPauseWeight = 0.0D;
            this.targetPauseWeight = 0.0D;
            this.pauseTransitionStartTime = renderTime;
            this.lastSeenTime = renderTime;
        }

        private void advance(double renderTime, boolean paused) {
            double pauseWeight = pauseWeight(renderTime);
            double delta = Math.max(0.0D, renderTime - lastRenderTime);
            orbitTime += delta * (1.0D - pauseWeight);
            lastRenderTime = renderTime;

            double target = paused ? 1.0D : 0.0D;
            if (Math.abs(targetPauseWeight - target) > 0.001D) {
                startPauseWeight = pauseWeight;
                targetPauseWeight = target;
                pauseTransitionStartTime = renderTime;
            }
            lastSeenTime = renderTime;
        }

        private double pauseWeight(double renderTime) {
            double progress = smoothStep((renderTime - pauseTransitionStartTime) / ORBIT_PAUSE_TRANSITION_TICKS);
            return lerp(startPauseWeight, targetPauseWeight, progress);
        }
    }

    private static final class OrbVisualTransition {
        private double startWeight;
        private double targetWeight;
        private double transitionStartTime;
        private double startBonusWeight;
        private double targetBonusWeight;
        private double bonusTransitionStartTime;
        private double startHoverWeight;
        private double targetHoverWeight;
        private double hoverTransitionStartTime;
        private double lastSeenTime;

        private OrbVisualTransition(double targetWeight, double targetBonusWeight, double targetHoverWeight, double renderTime) {
            this.startWeight = targetWeight;
            this.targetWeight = targetWeight;
            this.transitionStartTime = renderTime;
            this.startBonusWeight = targetBonusWeight;
            this.targetBonusWeight = targetBonusWeight;
            this.bonusTransitionStartTime = renderTime;
            this.startHoverWeight = 0.0D;
            this.targetHoverWeight = targetHoverWeight;
            this.hoverTransitionStartTime = renderTime;
            this.lastSeenTime = renderTime;
        }

        private double activeWeight(double renderTime) {
            double progress = smoothStep((renderTime - transitionStartTime) / STATE_TRANSITION_TICKS);
            return lerp(startWeight, targetWeight, progress);
        }

        private double bonusWeight(double renderTime) {
            double progress = smoothStep((renderTime - bonusTransitionStartTime) / STATE_TRANSITION_TICKS);
            return lerp(startBonusWeight, targetBonusWeight, progress);
        }

        private double hoverWeight(double renderTime) {
            double progress = smoothStep((renderTime - hoverTransitionStartTime) / HOVER_TRANSITION_TICKS);
            return lerp(startHoverWeight, targetHoverWeight, progress);
        }
    }

    private record OrbHit(Vec3 position, BlockPos stationPos, int partIndex, ResourceLocation enchantmentId) {
        private boolean sameTarget(OrbHit other) {
            return stationPos.equals(other.stationPos)
                    && partIndex == other.partIndex
                    && enchantmentId.equals(other.enchantmentId);
        }
    }
}
