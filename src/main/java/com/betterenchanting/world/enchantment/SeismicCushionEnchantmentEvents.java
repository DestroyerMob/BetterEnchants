package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.registry.ModGameRules;
import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

public final class SeismicCushionEnchantmentEvents {
    private SeismicCushionEnchantmentEvents() {
    }

    public static void explodeOnCrouchLanding(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        int enchantmentLevel = seismicCushionLevel(level, player.getItemBySlot(EquipmentSlot.FEET));
        if (enchantmentLevel <= 0) {
            return;
        }

        float radius = enchantmentLevel * BetterEnchantingConfig.seismicCushionExplosionRadiusPerLevel();
        if (radius > 0.0F) {
            Level.ExplosionInteraction interaction = level.getGameRules().getBoolean(ModGameRules.RULE_PLAYER_GRIEFING)
                    ? Level.ExplosionInteraction.TNT
                    : Level.ExplosionInteraction.NONE;
            level.explode(player, player.getX(), player.getY(), player.getZ(), radius, false, interaction);
        }
        event.setDamageMultiplier(0.0F);
    }

    public static int seismicCushionLevel(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.SEISMIC_CUSHION)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
