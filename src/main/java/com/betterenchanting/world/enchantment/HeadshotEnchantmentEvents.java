package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModTags;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class HeadshotEnchantmentEvents {
    private static final long IMPACT_TTL_TICKS = 5L;
    private static final Map<ImpactKey, ImpactRecord> PROJECTILE_IMPACTS = new HashMap<>();

    private HeadshotEnchantmentEvents() {
    }

    public static void recordProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (!(event.getProjectile().getOwner() instanceof Player)) {
            return;
        }

        pruneOldImpacts(level);
        PROJECTILE_IMPACTS.put(
                new ImpactKey(event.getProjectile().getUUID(), target.getUUID()),
                new ImpactRecord(hit.getLocation(), level.getGameTime())
        );
    }

    public static void increaseHeadshotDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || event.getNewDamage() <= 0.0F) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player) || !(event.getSource().getDirectEntity() instanceof Projectile projectile)) {
            return;
        }

        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || weapon.isEmpty() || !weapon.is(ModTags.Items.WEAPON_RANGED)) {
            return;
        }

        int enchantmentLevel = enchantmentLevel(level, weapon);
        if (enchantmentLevel <= 0) {
            return;
        }

        double lower = EffectiveBalance.headshotLowerEyeBand();
        double upper = EffectiveBalance.headshotUpperEyeBand();
        Vec3 impact = consumeImpact(level, projectile, victim);
        boolean headshot = impact == null
                ? HeadshotDetector.isRangedHeadshot(projectile, victim, lower, upper)
                : HeadshotDetector.isHeadshot(victim, impact, lower, upper);
        if (!headshot) {
            return;
        }

        float multiplier = Math.max(0.0F, 1.0F + enchantmentLevel * EffectiveBalance.headshotDamageBonusPerLevel());
        event.setNewDamage(event.getNewDamage() * multiplier);
    }

    private static Vec3 consumeImpact(ServerLevel level, Projectile projectile, LivingEntity victim) {
        ImpactRecord record = PROJECTILE_IMPACTS.remove(new ImpactKey(projectile.getUUID(), victim.getUUID()));
        if (record == null || level.getGameTime() - record.gameTime() > IMPACT_TTL_TICKS) {
            return null;
        }
        return record.location();
    }

    private static void pruneOldImpacts(ServerLevel level) {
        long cutoff = level.getGameTime() - IMPACT_TTL_TICKS;
        PROJECTILE_IMPACTS.entrySet().removeIf(entry -> entry.getValue().gameTime() < cutoff);
    }

    private static int enchantmentLevel(ServerLevel level, ItemStack stack) {
        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.HEADSHOT)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }

    private record ImpactKey(UUID projectile, UUID target) {
    }

    private record ImpactRecord(Vec3 location, long gameTime) {
    }
}
