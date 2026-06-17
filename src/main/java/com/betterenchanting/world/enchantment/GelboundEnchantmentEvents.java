package com.betterenchanting.world.enchantment;

import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

public final class GelboundEnchantmentEvents {
    private GelboundEnchantmentEvents() {
    }

    public static void negateFallDamage(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSuppressingBounce()) {
            return;
        }
        if (hasGelbound(player)) {
            event.setDamageMultiplier(0.0F);
        }
    }

    public static boolean hasGelbound(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        return gelboundLevel(level, boots) > 0 && SeismicCushionEnchantmentEvents.seismicCushionLevel(level, boots) <= 0;
    }

    private static int gelboundLevel(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.GELBOUND)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
