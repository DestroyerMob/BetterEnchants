package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModEffects;
import com.betterenchanting.registry.ModEnchantments;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class FrostbiteEnchantmentEvents {
    private FrostbiteEnchantmentEvents() {
    }

    public static void preventFrozenEffectDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.FROZEN) && event.getSource().is(DamageTypes.FREEZE)) {
            event.setNewDamage(0.0F);
        }
    }

    public static void applyFrostbite(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || event.getNewDamage() <= 0.0F || target.hasEffect(ModEffects.FROZEN) || !target.canFreeze()) {
            return;
        }

        int level = frostbiteLevel(target.level(), event.getSource().getWeaponItem());
        if (level <= 0) {
            return;
        }

        int frostTicks = EffectiveBalance.frostbiteFrostTicksPerLevel() * level;
        if (frostTicks <= 0) {
            return;
        }

        int threshold = Math.max(1, target.getTicksRequiredToFreeze());
        int nextFrozenTicks = Math.min(threshold, target.getTicksFrozen() + frostTicks);
        target.setTicksFrozen(nextFrozenTicks);
        if (nextFrozenTicks >= threshold) {
            applyFrozen(target, event.getSource().getEntity());
        }
    }

    public static void keepFrozenEntitiesFrozen(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !entity.hasEffect(ModEffects.FROZEN)) {
            return;
        }

        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), entity.getTicksRequiredToFreeze()));
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    private static void applyFrozen(LivingEntity target, @Nullable Entity source) {
        int duration = EffectiveBalance.frostbiteFrozenDurationTicks();
        if (duration <= 0) {
            return;
        }

        MobEffectInstance frozen = new MobEffectInstance(ModEffects.FROZEN, duration, 0, false, false, true);
        if (source == null) {
            target.addEffect(frozen);
        } else {
            target.addEffect(frozen, source);
        }
    }

    private static int frostbiteLevel(Level level, @Nullable ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || stack == null || stack.isEmpty()) {
            return 0;
        }

        return serverLevel.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.FROSTBITE)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
