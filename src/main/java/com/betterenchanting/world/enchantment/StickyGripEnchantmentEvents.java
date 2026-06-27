package com.betterenchanting.world.enchantment;

import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class StickyGripEnchantmentEvents {
    private StickyGripEnchantmentEvents() {
    }

    public static boolean preventsDropKey(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stickyGripLevel(player.serverLevel(), stack) > 0;
    }

    private static int stickyGripLevel(ServerLevel level, ItemStack stack) {
        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.STICKY_GRIP)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
