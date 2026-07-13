package com.betterenchanting.world.enchantment;

import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModEffects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ConductiveEnchantmentEvents {
    private static final String REDIRECTED_LIGHTNING_TAG = "betterenchanting.conductive_redirect";
    private static final Set<UUID> ARMED_PLAYERS = new HashSet<>();
    private static final Map<UUID, UUID> LAST_CHARGING_LIGHTNING = new HashMap<>();

    private ConductiveEnchantmentEvents() {
    }

    public static void chargeFromLightning(EntityStruckByLightningEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getLightning().getTags().contains(REDIRECTED_LIGHTNING_TAG)) {
            return;
        }
        if (conductiveLevel(player.serverLevel(), player.getItemBySlot(EquipmentSlot.HEAD)) <= 0) {
            return;
        }
        UUID playerId = player.getUUID();
        UUID lightningId = event.getLightning().getUUID();
        if (lightningId.equals(LAST_CHARGING_LIGHTNING.get(playerId))) {
            return;
        }

        LAST_CHARGING_LIGHTNING.put(playerId, lightningId);
        ARMED_PLAYERS.add(playerId);
        player.addEffect(new MobEffectInstance(
                ModEffects.CONDUCTIVE_CHARGE,
                MobEffectInstance.INFINITE_DURATION,
                0,
                false,
                false,
                false
        ));
        player.serverLevel().sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                player.getX(),
                player.getY(0.65D),
                player.getZ(),
                24,
                0.45D,
                0.65D,
                0.45D,
                0.08D
        );
    }

    public static void redirectLightning(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (event.getSource().getDirectEntity() != attacker
                || !ARMED_PLAYERS.contains(attacker.getUUID())
                || !attacker.hasEffect(ModEffects.CONDUCTIVE_CHARGE)) {
            return;
        }
        if (conductiveLevel(attacker.serverLevel(), attacker.getItemBySlot(EquipmentSlot.HEAD)) <= 0) {
            clearCharge(attacker);
            return;
        }

        clearCharge(attacker);
        summonLightning(attacker.serverLevel(), attacker, event.getEntity());
    }

    public static void tickChargedPlayer(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean armed = ARMED_PLAYERS.contains(player.getUUID());
        boolean hasStatus = player.hasEffect(ModEffects.CONDUCTIVE_CHARGE);
        if (!armed || !hasStatus
                || conductiveLevel(player.serverLevel(), player.getItemBySlot(EquipmentSlot.HEAD)) <= 0) {
            if (armed || hasStatus) {
                clearCharge(player);
            }
            return;
        }
        if (player.tickCount % 20 == 0) {
            player.serverLevel().sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    player.getX(),
                    player.getY(0.8D),
                    player.getZ(),
                    2,
                    0.3D,
                    0.45D,
                    0.3D,
                    0.025D
            );
        }
    }

    public static void clearOnDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerState(player);
        }
    }

    public static void clearOnLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerState(player);
        }
    }

    private static void clearCharge(ServerPlayer player) {
        ARMED_PLAYERS.remove(player.getUUID());
        player.removeEffect(ModEffects.CONDUCTIVE_CHARGE);
    }

    private static void clearPlayerState(ServerPlayer player) {
        clearCharge(player);
        LAST_CHARGING_LIGHTNING.remove(player.getUUID());
    }

    private static void summonLightning(ServerLevel level, ServerPlayer attacker, Entity target) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning == null) {
            return;
        }

        lightning.moveTo(Vec3.atBottomCenterOf(target.blockPosition()));
        lightning.setCause(attacker);
        lightning.addTag(REDIRECTED_LIGHTNING_TAG);
        level.addFreshEntity(lightning);
    }

    private static int conductiveLevel(ServerLevel level, ItemStack helmet) {
        if (helmet.isEmpty()) {
            return 0;
        }
        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.CONDUCTIVE)
                .map(helmet::getEnchantmentLevel)
                .orElse(0);
    }
}
