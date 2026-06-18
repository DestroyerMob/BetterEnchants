package com.betterenchanting.world.enchantment;

import com.betterenchanting.registry.ModEnchantments;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class VacuumEnchantmentEvents {
    private VacuumEnchantmentEvents() {
    }

    public static void vacuumBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player) || !hasVacuum(event.getLevel(), event.getTool())) {
            return;
        }

        vacuumDrops(player, event.getDrops());
    }

    public static void vacuumLivingDrops(LivingDropsEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }

        if (!sourceHasVacuum(player, event.getSource().getDirectEntity(), event.getSource().getWeaponItem())) {
            return;
        }

        vacuumDrops(player, event.getDrops());
    }

    private static boolean sourceHasVacuum(Player player, @Nullable Entity directEntity, @Nullable ItemStack weapon) {
        if (hasVacuum(player.level(), weapon)) {
            return true;
        }
        if (weapon != null && !weapon.isEmpty()) {
            return false;
        }

        if (!(directEntity instanceof Projectile)) {
            return false;
        }

        return hasVacuum(player.level(), player.getMainHandItem()) || hasVacuum(player.level(), player.getOffhandItem());
    }

    private static boolean hasVacuum(net.minecraft.world.level.Level level, @Nullable ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || stack == null || stack.isEmpty()) {
            return false;
        }

        return serverLevel.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.VACUUM)
                .map(holder -> stack.getEnchantmentLevel(holder) > 0)
                .orElse(false);
    }

    private static void vacuumDrops(Player player, Collection<ItemEntity> drops) {
        Iterator<ItemEntity> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemEntity drop = iterator.next();
            ItemStack original = drop.getItem();
            if (original.isEmpty()) {
                iterator.remove();
                continue;
            }

            ItemStack remainder = original.copy();
            player.getInventory().add(remainder);
            if (remainder.isEmpty()) {
                iterator.remove();
                drop.discard();
            } else {
                drop.setItem(remainder);
                moveToPlayerFeet(player, drop);
            }
        }
        player.getInventory().setChanged();
    }

    private static void moveToPlayerFeet(Player player, ItemEntity drop) {
        drop.setPos(player.getX(), player.getY() + 0.1D, player.getZ());
        drop.setDeltaMovement(0.0D, 0.0D, 0.0D);
        drop.setDefaultPickUpDelay();
    }
}
