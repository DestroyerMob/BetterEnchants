package com.betterenchanting.client;

import com.betterenchanting.network.OreRevealerHighlightPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class OreRevealerHighlights {
    private static final Map<BlockPos, Highlight> HIGHLIGHTS = new LinkedHashMap<>();
    private static ResourceKey<Level> dimension;

    private OreRevealerHighlights() {
    }

    public static void add(OreRevealerHighlightPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        ResourceKey<Level> currentDimension = minecraft.level.dimension();
        if (dimension == null || dimension != currentDimension) {
            HIGHLIGHTS.clear();
            dimension = currentDimension;
        }

        int durationTicks = Math.max(1, payload.durationTicks());
        long now = minecraft.level.getGameTime();
        long expiresAt = now + durationTicks;
        for (BlockPos pos : payload.positions()) {
            HIGHLIGHTS.put(pos.immutable(), new Highlight(now, expiresAt));
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || HIGHLIGHTS.isEmpty()) {
            return;
        }
        if (dimension != null && dimension != minecraft.level.dimension()) {
            HIGHLIGHTS.clear();
            return;
        }

        long now = minecraft.level.getGameTime();
        pruneExpired(now);
        if (HIGHLIGHTS.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(OreRevealerRenderTypes.lines());
        for (Map.Entry<BlockPos, Highlight> entry : HIGHLIGHTS.entrySet()) {
            float alpha = entry.getValue().alpha(now);
            AABB box = new AABB(entry.getKey()).inflate(0.01D).move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            LevelRenderer.renderLineBox(poseStack, consumer, box, 0.55F, 0.25F, 1.0F, alpha);
        }
        bufferSource.endBatch(OreRevealerRenderTypes.lines());
    }

    private static void pruneExpired(long now) {
        for (Iterator<Highlight> iterator = HIGHLIGHTS.values().iterator(); iterator.hasNext(); ) {
            if (iterator.next().expiresAt <= now) {
                iterator.remove();
            }
        }
    }

    private record Highlight(long start, long expiresAt) {
        private float alpha(long now) {
            long duration = Math.max(1L, expiresAt - start);
            float remaining = Math.max(0.0F, Math.min(1.0F, (expiresAt - now) / (float) duration));
            return 0.25F + 0.65F * remaining;
        }
    }
}
