package com.betterenchanting.world.enchantment;

import com.betterenchanting.compat.MoreWeaponsIaiCompat;
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

/** Drawn Steel enhances MoreWeapons' base fully charged Iaijutsu strike. */
public final class DrawnSteelEnchantmentEvents {
    private static final Map<Player, DrawnSteelState> STATES = new WeakHashMap<>();

    private DrawnSteelEnchantmentEvents() {
    }

    public static void prepareAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !hasDrawnSteel(player, player.getMainHandItem())
                || !MoreWeaponsIaiCompat.isFullyChargedStrike(player, event.getTarget())
                || player.getAttackStrengthScale(0.5F) < EffectiveBalance.drawnSteelReadyThreshold()) {
            STATES.remove(event.getEntity());
            return;
        }
        DrawnSteelState state = new DrawnSteelState();
        state.empoweredTargetId = event.getTarget().getId();
        STATES.put(player, state);
    }

    public static void applyDamage(LivingDamageEvent.Pre event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player) || event.getNewDamage() <= 0.0F) {
            return;
        }
        DrawnSteelState state = STATES.get(player);
        if (state == null || state.damageApplied || state.empoweredTargetId != event.getEntity().getId()
                || !hasDrawnSteel(player, event.getSource().getWeaponItem())) {
            return;
        }
        event.setNewDamage(event.getNewDamage() * EffectiveBalance.drawnSteelDamageMultiplier());
        state.damageApplied = true;
    }

    public static void finishAttack(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }
        DrawnSteelState state = STATES.get(player);
        if (state == null || state.empoweredTargetId != event.getEntity().getId()) {
            return;
        }
        if (state.damageApplied && event.getNewDamage() > 0.0F) {
            player.serverLevel().sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    event.getEntity().getX(),
                    event.getEntity().getY(0.55D),
                    event.getEntity().getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
            player.serverLevel().playSound(
                    null,
                    event.getEntity().blockPosition(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS,
                    0.85F,
                    1.35F
            );
        }
        STATES.remove(player);
    }

    private static boolean hasDrawnSteel(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(ModTags.Items.WEAPON_KATANAS)) {
            return false;
        }
        return player.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.DRAWN_STEEL)
                .map(holder -> stack.getEnchantmentLevel(holder) > 0)
                .orElse(false);
    }

    private static final class DrawnSteelState {
        private int empoweredTargetId = -1;
        private boolean damageApplied;
    }
}
