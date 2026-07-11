package com.betterenchanting.client;

import com.betterenchanting.compat.ApothicEnchantingCompat;
import com.betterenchanting.compat.ApothicEnchantingCompat.TableStats;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import org.joml.Vector3f;

/** Draws Apothic's three table stats as a single synchronized, level-readable halo. */
public final class EnchantingTableStatParticles {
    private static final DustParticleOptions ETERNA_PARTICLE = dust(0x45E0A1, 0.68F);
    private static final DustParticleOptions QUANTA_PARTICLE = dust(0xF06483, 0.68F);
    private static final DustParticleOptions ARCANA_PARTICLE = dust(0xB47CFF, 0.72F);
    private static final DustParticleOptions TREASURE_PARTICLE = dust(0xF4D77B, 0.58F);
    private static final Map<Long, Integer> LAST_EMISSION_TICKS = new HashMap<>();
    private static final Map<Long, CachedStats> CACHED_STATS = new HashMap<>();
    private static ClientLevel cachedLevel;

    private EnchantingTableStatParticles() {
    }

    public static void emit(EnchantingTableBlockEntity table, ItemStack target, ItemStack reagent) {
        if (!ApothicEnchantingCompat.isLoaded() || !(table.getLevel() instanceof ClientLevel level)) {
            return;
        }
        if (cachedLevel != level) {
            cachedLevel = level;
            LAST_EMISSION_TICKS.clear();
            CACHED_STATS.clear();
        }

        BlockPos pos = table.getBlockPos();
        if (target.isEmpty() || reagent.isEmpty()) {
            return;
        }
        InteractiveEnchantingOverlay.ensureState(pos);
        if (!InteractiveEnchantingOverlay.hasAvailableOffer(pos)) {
            return;
        }

        long key = pos.asLong();
        int tick = table.time;
        Integer previousEmissionTick = LAST_EMISSION_TICKS.put(key, tick);
        if (previousEmissionTick != null && previousEmissionTick == tick) {
            return;
        }
        if (tick % 4 != 0) {
            return;
        }

        CachedStats cached = CACHED_STATS.get(key);
        if (cached == null || tick - cached.sampleTick() >= 10) {
            TableStats stats = ApothicEnchantingCompat.gatherTableStats(level, pos, target).orElse(null);
            cached = new CachedStats(tick, stats);
            CACHED_STATS.put(key, cached);
        }
        if (cached.stats() == null) {
            return;
        }

        TableStats stats = cached.stats();
        double rotation = tick * 0.055D;
        emitStrand(level, pos, tick, normalize(stats.eterna(), 50.0F), rotation,
                0.80D, 1.12D, ETERNA_PARTICLE);
        emitStrand(level, pos, tick, normalize(stats.quanta(), 100.0F), rotation + Math.PI * 2.0D / 3.0D,
                stats.stable() ? 0.86D : 0.86D + Math.sin(tick * 0.13D) * 0.025D,
                1.20D, QUANTA_PARTICLE);
        emitStrand(level, pos, tick, normalize(stats.arcana(), 100.0F), rotation + Math.PI * 4.0D / 3.0D,
                0.92D, 1.28D, ARCANA_PARTICLE);

        if (stats.treasure() && tick % 10 == 0) {
            double angle = rotation + Math.PI / 2.0D;
            spawn(level, pos, TREASURE_PARTICLE, angle, 0.20D,
                    1.47D + Mth.sin(tick * 0.12F) * 0.025D, 0.0D);
        }
    }

    private static void emitStrand(
            ClientLevel level,
            BlockPos pos,
            int tick,
            float normalized,
            double leadingAngle,
            double radius,
            double height,
            DustParticleOptions particle
    ) {
        if (normalized <= 0.005F) {
            return;
        }

        int particleCount = Mth.ceil(normalized * 8.0F);
        for (int index = 0; index < particleCount; index++) {
            double angle = leadingAngle + index * Math.PI * 2.0D / particleCount;
            spawn(level, pos, particle, angle, radius, height, tick * 0.035D);
        }
    }

    private static void spawn(
            ClientLevel level,
            BlockPos pos,
            DustParticleOptions particle,
            double angle,
            double radius,
            double height,
            double wavePhase
    ) {
        double wave = Math.sin(angle * 2.0D + wavePhase) * 0.025D;
        double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
        double y = pos.getY() + height + wave;
        double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
        double tangentX = -Math.sin(angle) * 0.0025D;
        double tangentZ = Math.cos(angle) * 0.0025D;
        level.addParticle(particle, x, y, z, tangentX, 0.002D, tangentZ);
    }

    private static float normalize(float value, float maximum) {
        return Mth.clamp(value / maximum, 0.0F, 1.0F);
    }

    private static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F
        ), scale);
    }

    private record CachedStats(int sampleTick, TableStats stats) {
    }
}
