package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModEffects;
import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class CinderstepEnchantmentEvents {
    private CinderstepEnchantmentEvents() {
    }

    public static void applyCinderstep(LivingDamageEvent.Post event) {
        LivingEntity wearer = event.getEntity();
        if (!(wearer.level() instanceof ServerLevel level) || event.getNewDamage() <= 0.0F || !event.getSource().is(DamageTypeTags.IS_FIRE)) {
            return;
        }

        int enchantmentLevel = cinderstepLevel(level, wearer.getItemBySlot(EquipmentSlot.FEET));
        int duration = EffectiveBalance.cinderstepDurationTicks();
        if (enchantmentLevel <= 0 || duration <= 0) {
            return;
        }

        wearer.addEffect(new MobEffectInstance(ModEffects.CINDERSTEP, duration, enchantmentLevel - 1, false, false, true));
    }

    private static int cinderstepLevel(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.CINDERSTEP)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
