package com.betterenchanting.world.enchantment;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModTags;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class PerfectStrikeEnchantmentEvents {
    private static final ResourceLocation COOLDOWN_VARIANCE_ID = BetterEnchanting.id("perfect_strike_cooldown_variance");
    private static final Map<Player, PerfectStrikeState> STATES = new WeakHashMap<>();

    private PerfectStrikeEnchantmentEvents() {
    }

    public static void trackAttackTarget(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PerfectStrikeState state = STATES.computeIfAbsent(player, ignored -> new PerfectStrikeState());
        if (!hasPerfectStrikeWeapon(player, player.getMainHandItem())) {
            clearState(player, state);
            STATES.remove(player);
            return;
        }

        state.pendingCooldownReset = true;
        state.attackTargetId = event.getTarget().getId();
    }

    public static void openPerfectStrikeWindow(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PerfectStrikeState state = STATES.get(player);
        if (!hasPerfectStrikeWeapon(player, player.getMainHandItem())) {
            if (state != null) {
                clearState(player, state);
                STATES.remove(player);
            }
            return;
        }

        state = STATES.computeIfAbsent(player, ignored -> new PerfectStrikeState());
        float readyThreshold = BetterEnchantingConfig.perfectStrikeReadyThreshold();
        float attackStrength = player.getAttackStrengthScale(0.0F);

        if (state.pendingCooldownReset && attackStrength < readyThreshold) {
            state.pendingCooldownReset = false;
            state.canOpenWindow = true;
            state.windowExpiresTick = -1;
            state.attackTargetId = -1;
        }

        if (state.windowExpiresTick >= 0 && player.tickCount > state.windowExpiresTick) {
            state.windowExpiresTick = -1;
            state.canOpenWindow = false;
            removeCooldownVariance(player, state);
        }

        if (state.canOpenWindow && attackStrength >= readyThreshold) {
            int windowTicks = BetterEnchantingConfig.perfectStrikeWindowTicks();
            state.canOpenWindow = false;
            state.windowExpiresTick = windowTicks > 0 ? player.tickCount + windowTicks : -1;
        }
    }

    public static void applyPerfectStrikeDamage(LivingDamageEvent.Pre event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player) || event.getNewDamage() <= 0.0F) {
            return;
        }

        PerfectStrikeState state = STATES.get(player);
        if (state == null || !state.isWindowOpen(player.tickCount) || event.getEntity().getId() != state.attackTargetId) {
            return;
        }
        if (!hasPerfectStrikeWeapon(player, event.getSource().getWeaponItem())) {
            return;
        }

        event.setNewDamage(event.getNewDamage() * BetterEnchantingConfig.perfectStrikeDamageMultiplier());
        state.windowExpiresTick = -1;
    }

    public static void randomizeNextCooldown(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player) || event.getNewDamage() <= 0.0F) {
            return;
        }

        PerfectStrikeState state = STATES.get(player);
        if (state == null || event.getEntity().getId() != state.attackTargetId) {
            return;
        }
        if (!hasPerfectStrikeWeapon(player, event.getSource().getWeaponItem())) {
            clearAttack(state);
            return;
        }

        state.canOpenWindow = false;
        state.windowExpiresTick = -1;
        applyCooldownVariance(player, state);
        clearAttack(state);
    }

    private static boolean hasPerfectStrikeWeapon(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer) || stack == null || stack.isEmpty()) {
            return false;
        }
        if (!stack.is(ModTags.Items.WEAPON_SWORDS) && !stack.is(ModTags.Items.TOOL_AXES)) {
            return false;
        }

        return serverPlayer.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.PERFECT_STRIKE)
                .map(holder -> stack.getEnchantmentLevel(holder) > 0)
                .orElse(false);
    }

    private static void applyCooldownVariance(ServerPlayer player, PerfectStrikeState state) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }

        double min = Math.min(BetterEnchantingConfig.perfectStrikeMinCooldownVariance(), BetterEnchantingConfig.perfectStrikeMaxCooldownVariance());
        double max = Math.max(BetterEnchantingConfig.perfectStrikeMinCooldownVariance(), BetterEnchantingConfig.perfectStrikeMaxCooldownVariance());
        double variance = min == max ? min : min + player.getRandom().nextDouble() * (max - min);
        attackSpeed.addOrUpdateTransientModifier(new AttributeModifier(
                COOLDOWN_VARIANCE_ID,
                variance,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
        state.hasCooldownVariance = true;
    }

    private static void clearState(ServerPlayer player, PerfectStrikeState state) {
        state.canOpenWindow = true;
        state.pendingCooldownReset = false;
        state.windowExpiresTick = -1;
        clearAttack(state);
        removeCooldownVariance(player, state);
    }

    private static void clearAttack(PerfectStrikeState state) {
        state.attackTargetId = -1;
    }

    private static void removeCooldownVariance(ServerPlayer player, PerfectStrikeState state) {
        if (!state.hasCooldownVariance) {
            return;
        }

        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(COOLDOWN_VARIANCE_ID);
        }
        state.hasCooldownVariance = false;
    }

    private static final class PerfectStrikeState {
        private boolean canOpenWindow = true;
        private boolean pendingCooldownReset;
        private boolean hasCooldownVariance;
        private long windowExpiresTick = -1;
        private int attackTargetId = -1;

        private boolean isWindowOpen(int tickCount) {
            return this.windowExpiresTick >= tickCount;
        }
    }
}
