package com.betterenchanting.world.enchantment;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

final class HeadshotDetector {
    private HeadshotDetector() {
    }

    static boolean isMeleeHeadshot(Player attacker, LivingEntity victim, double lowerEyeBand, double upperEyeBand) {
        double reach = attacker.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + 0.5D;
        Vec3 start = attacker.getEyePosition();
        Vec3 end = start.add(attacker.getLookAngle().scale(reach));
        return victim.getBoundingBox().inflate(0.1D).clip(start, end)
                .map(hit -> isHeadshot(victim, hit, lowerEyeBand, upperEyeBand))
                .orElse(false);
    }

    static boolean isRangedHeadshot(Projectile projectile, LivingEntity victim, double lowerEyeBand, double upperEyeBand) {
        Vec3 start = projectile.position();
        Vec3 end = start.add(projectile.getDeltaMovement());
        return victim.getBoundingBox().inflate(0.1D).clip(start, end)
                .map(hit -> isHeadshot(victim, hit, lowerEyeBand, upperEyeBand))
                .orElseGet(() -> isHeadshot(victim, projectile.position(), lowerEyeBand, upperEyeBand));
    }

    static boolean isHeadshot(LivingEntity victim, Vec3 hit, double lowerEyeBand, double upperEyeBand) {
        double lower = Math.min(lowerEyeBand, upperEyeBand);
        double upper = Math.max(lowerEyeBand, upperEyeBand);
        double relativeToEyes = (hit.y() - victim.getEyeY()) / Math.max(victim.getBbHeight(), 0.001F);
        return relativeToEyes >= lower && relativeToEyes <= upper;
    }
}
