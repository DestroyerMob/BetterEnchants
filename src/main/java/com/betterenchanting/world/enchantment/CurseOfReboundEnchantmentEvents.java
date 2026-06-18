package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class CurseOfReboundEnchantmentEvents {
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);

    private CurseOfReboundEnchantmentEvents() {
    }

    public static void reflectPlayerDamage(LivingDamageEvent.Post event) {
        if (REFLECTING.get() || event.getNewDamage() <= 0.0F || event.getEntity().level().isClientSide()) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        LivingEntity target = event.getEntity();
        if (!(attacker instanceof Player player) || target == player || target instanceof Player) {
            return;
        }

        ItemStack weapon = event.getSource().getWeaponItem();
        if (!hasCurseOfRebound(player, weapon)) {
            return;
        }

        float reflectedDamage = event.getNewDamage() * EffectiveBalance.curseOfReboundReflectedDamageRatio();
        if (reflectedDamage <= 0.0F) {
            return;
        }

        REFLECTING.set(true);
        try {
            player.hurt(target.damageSources().thorns(target), reflectedDamage);
        } finally {
            REFLECTING.set(false);
        }
    }

    private static boolean hasCurseOfRebound(Player player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel serverLevel) || stack == null || stack.isEmpty()) {
            return false;
        }

        return serverLevel.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.CURSE_OF_REBOUND)
                .map(holder -> stack.getEnchantmentLevel(holder) > 0)
                .orElse(false);
    }
}
