package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModTags;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public final class DistantEdgeEnchantmentEvents {
    private static final Map<Player, StrikeState> STATES = new WeakHashMap<>();

    private DistantEdgeEnchantmentEvents() {
    }

    public static void prepareAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int level = enchantmentLevel(player, player.getMainHandItem());
        boolean empowered = level > 0
                && player.getAttackStrengthScale(0.5F) >= EffectiveBalance.distantEdgeReadyThreshold()
                && distanceToBounds(player, event.getTarget())
                >= EffectiveBalance.distantEdgeMinDistance();
        STATES.put(player, new StrikeState(empowered ? event.getTarget().getId() : -1, level));
    }

    public static void applyDamage(LivingDamageEvent.Pre event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player
                || event.getNewDamage() <= 0.0F) {
            return;
        }
        StrikeState state = STATES.get(player);
        if (state == null || state.applied || state.targetId != event.getEntity().getId()
                || enchantmentLevel(player, event.getSource().getWeaponItem()) <= 0) {
            return;
        }
        float bonus = EffectiveBalance.distantEdgeBaseDamageBonus()
                + Math.max(0, state.level - 1) * EffectiveBalance.distantEdgeDamageBonusPerLevel();
        event.setNewDamage(event.getNewDamage() * (1.0F + Math.max(0.0F, bonus)));
        state.applied = true;
    }

    public static void finishAttack(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }
        StrikeState state = STATES.get(player);
        if (state == null || state.targetId != event.getEntity().getId()) {
            return;
        }
        if (state.applied && event.getNewDamage() > 0.0F) {
            player.serverLevel().sendParticles(
                    ParticleTypes.CRIT,
                    event.getEntity().getX(),
                    event.getEntity().getY(0.55D),
                    event.getEntity().getZ(),
                    7,
                    0.25D,
                    0.35D,
                    0.25D,
                    0.08D
            );
            player.serverLevel().playSound(null, event.getEntity().blockPosition(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.7F, 1.25F);
        }
        STATES.remove(player);
    }

    private static int enchantmentLevel(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(ModTags.Items.WEAPON_KATANAS)) {
            return 0;
        }
        return player.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.DISTANT_EDGE)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }

    private static double distanceToBounds(ServerPlayer player, Entity target) {
        var point = player.getEyePosition();
        var bounds = target.getBoundingBox();
        return ReachDistance.nearestPoint(
                point.x, point.y, point.z,
                bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ
        );
    }

    private static final class StrikeState {
        private final int targetId;
        private final int level;
        private boolean applied;

        private StrikeState(int targetId, int level) {
            this.targetId = targetId;
            this.level = level;
        }
    }
}
