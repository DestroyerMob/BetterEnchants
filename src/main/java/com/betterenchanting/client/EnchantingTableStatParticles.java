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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import org.joml.Vector3f;

/**
 * Draws restrained, level-readable particle arcs around visible enchanting tables.
 * Arc length and sample density both track the corresponding Apothic table stat.
 */
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

    public static void emit(EnchantingTableBlockEntity table, ItemStack target) {
        if (!ApothicEnchantingCompat.isLoaded() || !(table.getLevel() instanceof ClientLevel level)) {
            return;
        }
        if (cachedLevel != level) {
            cachedLevel = level;
            LAST_EMISSION_TICKS.clear();
            CACHED_STATS.clear();
        }

        BlockPos pos = table.getBlockPos();
        long key = pos.asLong();
        int tick = table.time;
        Integer previousEmissionTick = LAST_EMISSION_TICKS.put(key, tick);
        if (previousEmissionTick != null && previousEmissionTick == tick) {
            return;
        }
        if ((tick & 1) != 0) {
            return;
        }

        CachedStats cached = CACHED_STATS.get(key);
        if (cached == null || tick - cached.sampleTick() >= 10) {
            ItemStack statsTarget = target.isEmpty() ? new ItemStack(Items.BOOK) : target;
            TableStats stats = ApothicEnchantingCompat.gatherTableStats(level, pos, statsTarget).orElse(null);
            cached = new CachedStats(tick, stats);
            CACHED_STATS.put(key, cached);
        }
        if (cached.stats() == null) {
            return;
        }

        TableStats stats = cached.stats();
        emitArc(level, pos, tick, normalize(stats.eterna(), 50.0F), 0.70D, 1.02D, 0.0D, ETERNA_PARTICLE);
        emitArc(level, pos, tick, normalize(stats.quanta(), 100.0F), 0.84D, 1.18D, Math.PI * 0.66D, QUANTA_PARTICLE);
        emitArc(level, pos, tick, normalize(stats.arcana(), 100.0F), 0.98D, 1.34D, Math.PI * 1.33D, ARCANA_PARTICLE);

        if (stats.treasure() && tick % 8 == 0) {
            double angle = tick * 0.075D;
            spawn(
                    level,
                    pos,
                    TREASURE_PARTICLE,
                    angle,
                    1.06D,
                    1.50D + Mth.sin(tick * 0.12F) * 0.025D,
                    0.0D
            );
        }
    }

    private static void emitArc(
            ClientLevel level,
            BlockPos pos,
            int tick,
            float normalized,
            double radius,
            double height,
            double rotation,
            DustParticleOptions particle
    ) {
        if (normalized <= 0.005F) {
            return;
        }

        int samples = 3 + Mth.ceil(normalized * 17.0F);
        int cursor = Math.floorMod(tick / 2, samples);
        double span = Math.max(0.16D, normalized * Math.PI * 2.0D);
        double firstAngle = rotation - Math.PI / 2.0D + span * cursor / Math.max(1, samples - 1);
        spawn(level, pos, particle, firstAngle, radius, height, tick * 0.055D);

        if (samples >= 7) {
            int opposite = Math.floorMod(cursor + samples / 2, samples);
            double secondAngle = rotation - Math.PI / 2.0D + span * opposite / Math.max(1, samples - 1);
            spawn(level, pos, particle, secondAngle, radius, height, tick * 0.055D + 1.4D);
        }

        if (tick % 6 == 0) {
            double endpoint = rotation - Math.PI / 2.0D + span;
            spawn(level, pos, particle, endpoint, radius, height + 0.018D, tick * 0.04D);
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
