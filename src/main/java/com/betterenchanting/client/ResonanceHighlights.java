package com.betterenchanting.client;

import com.betterenchanting.network.ResonanceHighlightPayload;
import com.betterenchanting.network.GeodeSearchPayload;
import com.betterenchanting.registry.ModTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class ResonanceHighlights {
    private static final double TAU = Math.PI * 2.0D;
    private static final double WAVE_DELAY_TICKS_PER_BLOCK = 0.85D;
    private static final double REVEAL_TICKS = 5.0D;
    private static final double FADE_TICKS = 20.0D;
    private static final double TWINKLE_PERIOD_TICKS = 46.0D;
    private static final double SEARCH_PULSE_TICKS = 18.0D;
    private static final int MAX_ACTIVE_PULSES = 24;
    private static final Map<BlockPos, Highlight> HIGHLIGHTS = new LinkedHashMap<>();
    private static final List<SearchPulse> SEARCH_PULSES = new ArrayList<>();
    private static ResourceKey<Level> dimension;

    private ResonanceHighlights() {
    }

    public static void add(ResonanceHighlightPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        ensureDimension(minecraft.level);

        double now = minecraft.level.getGameTime();
        ResonanceColor color = colorFor(payload.ore());
        int radius = Math.max(1, payload.searchRadius());
        addSearchPulse(
                payload.origin(),
                now,
                now + SEARCH_PULSE_TICKS,
                new AABB(
                        payload.origin().getX() - radius,
                        payload.origin().getY() - radius,
                        payload.origin().getZ() - radius,
                        payload.origin().getX() + radius + 1,
                        payload.origin().getY() + radius + 1,
                        payload.origin().getZ() + radius + 1
                ),
                color,
                false
        );

        int durationTicks = Math.max(1, payload.durationTicks());
        for (BlockPos pos : payload.positions()) {
            double distance = Math.sqrt(payload.origin().distSqr(pos));
            double startsAt = now + Math.min(12.0D, distance * WAVE_DELAY_TICKS_PER_BLOCK);
            double expiresAt = startsAt + durationTicks;
            HIGHLIGHTS.put(pos.immutable(), new Highlight(
                    payload.ore(),
                    startsAt,
                    expiresAt,
                    animationPhase(pos),
                    color,
                    true
            ));
        }
    }

    public static void add(GeodeSearchPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        ensureDimension(minecraft.level);
        double now = minecraft.level.getGameTime();
        ResourceLocation buddingAmethyst = ResourceLocation.withDefaultNamespace("budding_amethyst");
        ResonanceColor color = colorFor(buddingAmethyst);
        BlockPos min = payload.searchMin();
        BlockPos max = payload.searchMax();
        addSearchPulse(
                payload.origin(),
                now,
                now + SEARCH_PULSE_TICKS * 1.65D,
                new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1.0D, max.getY() + 1.0D, max.getZ() + 1.0D),
                color,
                true
        );

        int durationTicks = Math.max(1, payload.durationTicks());
        for (BlockPos pos : payload.buddingAmethyst()) {
            double distance = Math.sqrt(payload.origin().distSqr(pos));
            double startsAt = now + Math.min(16.0D, distance * 0.22D);
            HIGHLIGHTS.put(pos.immutable(), new Highlight(
                    buddingAmethyst,
                    startsAt,
                    startsAt + durationTicks,
                    animationPhase(pos),
                    color,
                    false
            ));
        }
    }

    private static void ensureDimension(Level level) {
        ResourceKey<Level> currentDimension = level.dimension();
        if (dimension == null || dimension != currentDimension) {
            HIGHLIGHTS.clear();
            SEARCH_PULSES.clear();
            dimension = currentDimension;
        }
    }

    private static void addSearchPulse(
            BlockPos origin,
            double startsAt,
            double expiresAt,
            AABB bounds,
            ResonanceColor color,
            boolean accented
    ) {
        if (SEARCH_PULSES.size() >= MAX_ACTIVE_PULSES) {
            SEARCH_PULSES.removeFirst();
        }
        SEARCH_PULSES.add(new SearchPulse(origin.immutable(), startsAt, expiresAt, bounds, color, accented));
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || (HIGHLIGHTS.isEmpty() && SEARCH_PULSES.isEmpty())) {
            return;
        }
        if (dimension != null && dimension != minecraft.level.dimension()) {
            HIGHLIGHTS.clear();
            SEARCH_PULSES.clear();
            return;
        }

        double renderTime = minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        pruneInvalid(minecraft.level, renderTime);
        if (HIGHLIGHTS.isEmpty() && SEARCH_PULSES.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lineConsumer = bufferSource.getBuffer(ResonanceRenderTypes.lines());
        poseStack.pushPose();
        for (SearchPulse pulse : SEARCH_PULSES) {
            renderSearchPulse(poseStack, cameraPosition, lineConsumer, pulse, renderTime);
        }
        for (Map.Entry<BlockPos, Highlight> entry : HIGHLIGHTS.entrySet()) {
            renderHighlight(
                    poseStack,
                    cameraPosition,
                    lineConsumer,
                    entry.getKey(),
                    entry.getValue(),
                    renderTime
            );
        }
        poseStack.popPose();
        bufferSource.endBatch(ResonanceRenderTypes.lines());
    }

    private static void renderSearchPulse(
            PoseStack poseStack,
            Vec3 cameraPosition,
            VertexConsumer lineConsumer,
            SearchPulse pulse,
            double renderTime
    ) {
        double progress = pulse.progress(renderTime);
        if (progress < 0.0D || progress >= 1.0D) {
            return;
        }

        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
        float alpha = (float) (Math.sin(Math.PI * progress) * 0.48D);
        Vec3 center = Vec3.atCenterOf(pulse.origin);
        AABB box = new AABB(
                lerp(center.x - 0.16D, pulse.bounds.minX, eased),
                lerp(center.y - 0.16D, pulse.bounds.minY, eased),
                lerp(center.z - 0.16D, pulse.bounds.minZ, eased),
                lerp(center.x + 0.16D, pulse.bounds.maxX, eased),
                lerp(center.y + 0.16D, pulse.bounds.maxY, eased),
                lerp(center.z + 0.16D, pulse.bounds.maxZ, eased)
        ).move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                box,
                pulse.color.red,
                pulse.color.green,
                pulse.color.blue,
                alpha
        );
        if (pulse.accented && box.getXsize() > 0.08D && box.getYsize() > 0.08D && box.getZsize() > 0.08D) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineConsumer,
                    box.inflate(-0.035D),
                    0.96F,
                    0.92F,
                    1.00F,
                    alpha * 0.34F
            );
        }
    }

    private static void renderHighlight(
            PoseStack poseStack,
            Vec3 cameraPosition,
            VertexConsumer lineConsumer,
            BlockPos pos,
            Highlight highlight,
            double renderTime
    ) {
        double visibility = highlight.visibility(renderTime);
        if (visibility <= 0.001D) {
            return;
        }

        double twinkle = 0.5D + 0.5D * Math.sin(renderTime * TAU / TWINKLE_PERIOD_TICKS + highlight.phase);
        double coreRadius = lerp(0.075D, 0.115D, twinkle);
        Vec3 center = Vec3.atCenterOf(pos);
        AABB halo = centeredBox(center, coreRadius + 0.055D, cameraPosition);
        AABB core = centeredBox(center, coreRadius, cameraPosition);
        ResonanceColor color = highlight.color;
        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                halo,
                color.red,
                color.green,
                color.blue,
                (float) (visibility * lerp(0.08D, 0.16D, twinkle))
        );
        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                core,
                color.red,
                color.green,
                color.blue,
                (float) (visibility * lerp(0.42D, 0.68D, twinkle))
        );
    }

    private static AABB centeredBox(Vec3 center, double radius, Vec3 cameraPosition) {
        return new AABB(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        ).move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
    }

    private static void pruneInvalid(Level level, double now) {
        for (Iterator<Map.Entry<BlockPos, Highlight>> iterator = HIGHLIGHTS.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<BlockPos, Highlight> entry = iterator.next();
            BlockPos pos = entry.getKey();
            Highlight highlight = entry.getValue();
            if (highlight.expiresAt <= now
                    || !level.isLoaded(pos)) {
                iterator.remove();
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(highlight.ore)
                    || highlight.requiresOreTag && !state.is(ModTags.Blocks.ORES)) {
                iterator.remove();
            }
        }
        SEARCH_PULSES.removeIf(pulse -> pulse.expiresAt <= now);
    }

    private static ResonanceColor colorFor(ResourceLocation ore) {
        String path = ore.getPath();
        if (path.contains("diamond")) {
            return new ResonanceColor(0.36F, 0.91F, 0.88F);
        }
        if (path.contains("emerald")) {
            return new ResonanceColor(0.30F, 0.88F, 0.48F);
        }
        if (path.contains("redstone")) {
            return new ResonanceColor(0.94F, 0.24F, 0.24F);
        }
        if (path.contains("lapis")) {
            return new ResonanceColor(0.28F, 0.48F, 0.96F);
        }
        if (path.contains("gold")) {
            return new ResonanceColor(1.00F, 0.78F, 0.22F);
        }
        if (path.contains("copper")) {
            return new ResonanceColor(0.92F, 0.43F, 0.24F);
        }
        if (path.contains("iron")) {
            return new ResonanceColor(0.89F, 0.70F, 0.55F);
        }
        if (path.contains("coal")) {
            return new ResonanceColor(0.60F, 0.64F, 0.70F);
        }
        if (path.contains("quartz")) {
            return new ResonanceColor(0.95F, 0.88F, 0.82F);
        }
        if (path.contains("amethyst")) {
            return new ResonanceColor(0.74F, 0.46F, 0.98F);
        }
        if (path.contains("netherite") || path.contains("debris")) {
            return new ResonanceColor(0.66F, 0.42F, 0.36F);
        }
        return new ResonanceColor(0.48F, 0.75F, 1.00F);
    }

    private static double animationPhase(BlockPos pos) {
        long hash = pos.asLong() * 0x9E3779B97F4A7C15L;
        return (hash & 0xFFFFL) / 65535.0D * TAU;
    }

    private static double smoothStep(double value) {
        double clamped = Math.max(0.0D, Math.min(1.0D, value));
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * Math.max(0.0D, Math.min(1.0D, amount));
    }

    private record ResonanceColor(float red, float green, float blue) {
    }

    private record SearchPulse(
            BlockPos origin,
            double start,
            double expiresAt,
            AABB bounds,
            ResonanceColor color,
            boolean accented
    ) {
        private double progress(double now) {
            return (now - start) / Math.max(1.0D, expiresAt - start);
        }
    }

    private record Highlight(
            ResourceLocation ore,
            double start,
            double expiresAt,
            double phase,
            ResonanceColor color,
            boolean requiresOreTag
    ) {
        private double visibility(double now) {
            if (now < start || now >= expiresAt) {
                return 0.0D;
            }
            double reveal = smoothStep((now - start) / REVEAL_TICKS);
            double fade = smoothStep((expiresAt - now) / FADE_TICKS);
            return reveal * fade;
        }
    }
}
