package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;

public final class OverchargedEnchantmentEvents {
    private static final int MAX_LEVEL = 5;

    private OverchargedEnchantmentEvents() {
    }

    public static void applyOvercharged(EntityStruckByLightningEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        int enchantmentLevel = overchargedLevel(level, player.getItemBySlot(EquipmentSlot.CHEST));
        int duration = EffectiveBalance.overchargedDurationTicks();
        if (enchantmentLevel <= 0 || duration <= 0) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                duration,
                scaledAmplifier(enchantmentLevel, EffectiveBalance.overchargedStrengthMaxAmplifier()),
                false,
                true,
                true
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                duration,
                scaledAmplifier(enchantmentLevel, EffectiveBalance.overchargedRegenerationMaxAmplifier()),
                false,
                true,
                true
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                duration,
                scaledAmplifier(enchantmentLevel, EffectiveBalance.overchargedSpeedMaxAmplifier()),
                false,
                true,
                true
        ));
    }

    private static int overchargedLevel(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.OVERCHARGED)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }

    private static int scaledAmplifier(int enchantmentLevel, int maxAmplifier) {
        int clampedLevel = Mth.clamp(enchantmentLevel, 1, MAX_LEVEL);
        return Mth.clamp((int) Math.floor((double) (clampedLevel - 1) * maxAmplifier / (MAX_LEVEL - 1)), 0, maxAmplifier);
    }
}
