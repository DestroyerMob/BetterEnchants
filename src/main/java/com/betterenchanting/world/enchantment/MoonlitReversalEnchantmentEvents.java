package com.betterenchanting.world.enchantment;

import com.betterenchanting.compat.MobsCombatCompat;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public final class MoonlitReversalEnchantmentEvents {
    private static final Map<Player, ReversalState> STATES = new WeakHashMap<>();

    private MoonlitReversalEnchantmentEvents() {
    }

    public static void prepareAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ReversalState state = STATES.computeIfAbsent(player, ignored -> new ReversalState());
        state.targetId = -1;
        state.applied = false;
        if (enchantmentLevel(player, player.getMainHandItem()) <= 0) {
            return;
        }
        MobsCombatCompat.ParryCounter counter = MobsCombatCompat.parryCounter(player).orElse(null);
        if (counter == null || !counter.active() || counter.sequence() <= state.lastConsumedParrySequence) {
            return;
        }
        state.lastConsumedParrySequence = counter.sequence();
        if (player.getAttackStrengthScale(0.5F) >= EffectiveBalance.moonlitReversalReadyThreshold()) {
            state.targetId = event.getTarget().getId();
        }
    }

    public static void applyDamage(LivingDamageEvent.Pre event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player
                || event.getNewDamage() <= 0.0F) {
            return;
        }
        ReversalState state = STATES.get(player);
        if (state == null || state.applied || state.targetId != event.getEntity().getId()
                || enchantmentLevel(player, event.getSource().getWeaponItem()) <= 0) {
            return;
        }
        event.setNewDamage(event.getNewDamage() * EffectiveBalance.moonlitReversalDamageMultiplier());
        state.applied = true;
    }

    public static void finishAttack(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }
        ReversalState state = STATES.get(player);
        if (state == null || state.targetId != event.getEntity().getId()) {
            return;
        }
        if (state.applied && event.getNewDamage() > 0.0F) {
            if (event.getEntity() instanceof LivingEntity target) {
                MobsCombatCompat.applyFlatSlashPostureDamage(
                        player,
                        target,
                        EffectiveBalance.moonlitReversalPostureBonus()
                );
            }
            player.serverLevel().sendParticles(
                    ParticleTypes.ENCHANTED_HIT,
                    event.getEntity().getX(),
                    event.getEntity().getY(0.6D),
                    event.getEntity().getZ(),
                    12,
                    0.3D,
                    0.4D,
                    0.3D,
                    0.1D
            );
            player.serverLevel().playSound(null, event.getEntity().blockPosition(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 0.85F);
        }
        state.targetId = -1;
        state.applied = false;
    }

    private static int enchantmentLevel(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(ModTags.Items.WEAPON_KATANAS)) {
            return 0;
        }
        return player.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.MOONLIT_REVERSAL)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }

    private static final class ReversalState {
        private long lastConsumedParrySequence;
        private int targetId = -1;
        private boolean applied;
    }
}
