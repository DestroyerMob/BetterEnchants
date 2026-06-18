package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModEffects;
import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class ShockingEnchantmentEvents {
    private ShockingEnchantmentEvents() {
    }

    public static void increaseShockedDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.SHOCKED)) {
            event.setNewDamage(event.getNewDamage() * EffectiveBalance.shockedDamageMultiplier());
        }
    }

    public static void applyShocked(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || event.getNewDamage() <= 0.0F) {
            return;
        }

        ItemStack weapon = event.getSource().getWeaponItem();
        if (!hasShocking(target.level(), weapon)) {
            return;
        }

        Entity source = event.getSource().getEntity();
        MobEffectInstance shocked = new MobEffectInstance(ModEffects.SHOCKED, EffectiveBalance.shockingDurationTicks(), 0, false, false, true);
        if (source == null) {
            target.addEffect(shocked);
        } else {
            target.addEffect(shocked, source);
        }
    }

    public static void emitShockedParticles(EntityTickEvent.Post event) {
        if (!EffectiveBalance.shockedParticlesEnabled() || !(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level) || !entity.hasEffect(ModEffects.SHOCKED)) {
            return;
        }
        if (entity.tickCount % EffectiveBalance.shockedParticleIntervalTicks() != 0) {
            return;
        }

        double y = entity.getY() + entity.getBbHeight() * 0.55D;
        level.sendParticles(
                shockedParticle(),
                entity.getX(),
                y,
                entity.getZ(),
                EffectiveBalance.shockedParticleCount(),
                Math.max(entity.getBbWidth() * 0.5D, EffectiveBalance.shockedParticleHorizontalSpread()),
                Math.max(entity.getBbHeight() * 0.35D, EffectiveBalance.shockedParticleVerticalSpread()),
                Math.max(entity.getBbWidth() * 0.5D, EffectiveBalance.shockedParticleHorizontalSpread()),
                EffectiveBalance.shockedParticleSpeed()
        );
    }

    private static boolean hasShocking(Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || stack == null || stack.isEmpty()) {
            return false;
        }

        return serverLevel.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.SHOCKING)
                .map(holder -> stack.getEnchantmentLevel(holder) > 0)
                .orElse(false);
    }

    private static ParticleOptions shockedParticle() {
        try {
            ResourceLocation id = ResourceLocation.parse(EffectiveBalance.shockedParticleType());
            if (BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null) instanceof SimpleParticleType particle) {
                return particle;
            }
        } catch (RuntimeException ignored) {
        }
        return ParticleTypes.ELECTRIC_SPARK;
    }
}
