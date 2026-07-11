package com.betterenchanting.client;

import com.betterenchanting.world.inventory.PedestalUpgradeRules;
import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shows which enchanting table supplies the pedestal, with pulses travelling toward the pedestal. */
public final class PedestalTableLinkParticles {
    private static final DustParticleOptions LINK_PARTICLE =
            new DustParticleOptions(new Vector3f(0.58F, 0.38F, 1.0F), 0.62F);
    private static final DustParticleOptions TRAIL_PARTICLE =
            new DustParticleOptions(new Vector3f(0.35F, 0.78F, 1.0F), 0.45F);
    private static final Map<Long, Long> LAST_EMISSION_TICKS = new HashMap<>();
    private static ClientLevel cachedLevel;

    private PedestalTableLinkParticles() {
    }

    public static void emit(AttunementPedestalBlockEntity pedestal) {
        if (!(pedestal.getLevel() instanceof ClientLevel level) || pedestal.target().isEmpty()) {
            return;
        }
        if (cachedLevel != level) {
            cachedLevel = level;
            LAST_EMISSION_TICKS.clear();
        }

        long tick = level.getGameTime();
        long key = pedestal.getBlockPos().asLong();
        Long previous = LAST_EMISSION_TICKS.put(key, tick);
        if (previous != null && previous == tick) {
            return;
        }

        PedestalUpgradeRules.nearestEnchantingTable(level, pedestal.getBlockPos())
                .ifPresent(tablePos -> emitPulse(level, tablePos, pedestal.getBlockPos(), tick));
    }

    private static void emitPulse(ClientLevel level, BlockPos tablePos, BlockPos pedestalPos, long tick) {
        Vec3 start = Vec3.atCenterOf(tablePos).add(0.0D, 0.82D, 0.0D);
        Vec3 end = Vec3.atCenterOf(pedestalPos).add(0.0D, 0.78D, 0.0D);
        double distance = start.distanceTo(end);
        double progress = Math.floorMod(tick, 28L) / 27.0D;

        spawn(level, curve(start, end, progress, distance), LINK_PARTICLE);
        double trailProgress = progress - 0.055D;
        if (trailProgress >= 0.0D) {
            spawn(level, curve(start, end, trailProgress, distance), TRAIL_PARTICLE);
        }
    }

    private static Vec3 curve(Vec3 start, Vec3 end, double progress, double distance) {
        Vec3 linear = start.lerp(end, progress);
        double arch = Math.sin(progress * Math.PI) * Math.min(0.85D, 0.18D + distance * 0.08D);
        return linear.add(0.0D, arch, 0.0D);
    }

    private static void spawn(ClientLevel level, Vec3 position, DustParticleOptions particle) {
        level.addParticle(particle, position.x, position.y, position.z, 0.0D, 0.004D, 0.0D);
    }
}
