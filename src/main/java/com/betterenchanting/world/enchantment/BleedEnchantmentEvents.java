package com.betterenchanting.world.enchantment;

import com.betterenchanting.registry.ModEffects;
import com.betterenchanting.registry.ModEnchantments;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class BleedEnchantmentEvents {
    private static final int BASE_DURATION_TICKS = 80;
    private static final int DURATION_TICKS_PER_LEVEL = 40;
    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final float BASE_DAMAGE = 1.0F;
    private static final float DAMAGE_PER_EXTRA_LEVEL = 0.5F;
    private static final int PARTICLE_INTERVAL_TICKS = 5;
    private static final int PARTICLE_COLOR = 0xB21725;

    private BleedEnchantmentEvents() {
    }

    public static void applyBleed(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || event.getNewDamage() <= 0.0F) {
            return;
        }

        int level = bleedLevel(target.level(), event.getSource().getWeaponItem());
        if (level <= 0) {
            return;
        }

        int duration = BASE_DURATION_TICKS + DURATION_TICKS_PER_LEVEL * (level - 1);
        MobEffectInstance bleeding = new MobEffectInstance(ModEffects.BLEEDING, duration, level - 1, false, false, true);
        Entity source = event.getSource().getEntity();
        if (source == null) {
            target.addEffect(bleeding);
        } else {
            target.addEffect(bleeding, source);
        }
    }

    public static void tickBleeding(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }

        MobEffectInstance bleeding = entity.getEffect(ModEffects.BLEEDING);
        if (bleeding == null) {
            return;
        }

        if (entity.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
            float damage = BASE_DAMAGE + DAMAGE_PER_EXTRA_LEVEL * bleeding.getAmplifier();
            entity.hurt(entity.damageSources().magic(), damage);
        }
        if (entity.tickCount % PARTICLE_INTERVAL_TICKS == 0) {
            double y = entity.getY() + entity.getBbHeight() * 0.55D;
            level.sendParticles(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, PARTICLE_COLOR),
                    entity.getX(),
                    y,
                    entity.getZ(),
                    3,
                    Math.max(entity.getBbWidth() * 0.35D, 0.15D),
                    Math.max(entity.getBbHeight() * 0.35D, 0.25D),
                    Math.max(entity.getBbWidth() * 0.35D, 0.15D),
                    0.02D
            );
        }
    }

    private static int bleedLevel(Level level, @Nullable ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || stack == null || stack.isEmpty()) {
            return 0;
        }

        return serverLevel.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.BLEED)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
