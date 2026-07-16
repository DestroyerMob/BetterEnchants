package com.betterenchanting.client;

import com.betterenchanting.world.inventory.PedestalUpgradeRules;
import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/** Shows which enchanting table supplies the pedestal with a dense stream of enchanting glyphs. */
public final class PedestalTableLinkParticles {
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
        for (int index = 0; index < 4; index++) {
            double progress = Math.floorMod(tick * 4L + index * 11L, 52L) / 51.0D;
            Vec3 position = curve(start, end, progress, distance);
            Vec3 next = curve(start, end, Math.min(1.0D, progress + 0.055D), distance);
            Vec3 motion = next.subtract(position).scale(1.35D);
            double jitter = (index - 1.5D) * 0.012D;
            level.addParticle(
                    ParticleTypes.ENCHANT,
                    position.x + jitter,
                    position.y,
                    position.z - jitter,
                    motion.x,
                    motion.y,
                    motion.z
            );
        }
    }

    private static Vec3 curve(Vec3 start, Vec3 end, double progress, double distance) {
        Vec3 linear = start.lerp(end, progress);
        double arch = Math.sin(progress * Math.PI) * Math.min(0.85D, 0.18D + distance * 0.08D);
        return linear.add(0.0D, arch, 0.0D);
    }
}
