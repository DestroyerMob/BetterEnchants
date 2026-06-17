package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.registry.ModEffects;
import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class ShockingEnchantmentEvents {
    private ShockingEnchantmentEvents() {
    }

    public static void increaseShockedDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.SHOCKED)) {
            event.setNewDamage(event.getNewDamage() * BetterEnchantingConfig.shockedDamageMultiplier());
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
        MobEffectInstance shocked = new MobEffectInstance(ModEffects.SHOCKED, BetterEnchantingConfig.shockingDurationTicks(), 0, false, true, true);
        if (source == null) {
            target.addEffect(shocked);
        } else {
            target.addEffect(shocked, source);
        }
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
}
