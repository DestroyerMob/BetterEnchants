package com.betterenchanting.network;

import com.betterenchanting.client.ResonanceHighlights;
import com.betterenchanting.compat.MobsToolForgingCompat;
import com.betterenchanting.world.enchantment.FlashStepEnchantmentEvents;
import com.betterenchanting.world.enchantment.VeinMinerModeState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "2";

    private ModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(CycleVeinMinerModePayload.TYPE, CycleVeinMinerModePayload.STREAM_CODEC, ModNetworking::handleCycleVeinMinerMode);
        registrar.playToServer(FlashStepPayload.TYPE, FlashStepPayload.STREAM_CODEC, ModNetworking::handleFlashStep);
        registrar.playToClient(ResonanceHighlightPayload.TYPE, ResonanceHighlightPayload.STREAM_CODEC, ModNetworking::handleResonanceHighlights);
        registrar.playToServer(PromoteRoutedEnchantmentPayload.TYPE, PromoteRoutedEnchantmentPayload.STREAM_CODEC, ModNetworking::handlePromoteRoutedEnchantment);
    }

    private static void handleCycleVeinMinerMode(CycleVeinMinerModePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            VeinMinerModeState.cycle(player);
        }
    }

    private static void handleFlashStep(FlashStepPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            FlashStepEnchantmentEvents.tryFlashStep(player);
        }
    }

    private static void handleResonanceHighlights(ResonanceHighlightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ResonanceHighlights.add(payload));
    }

    private static void handlePromoteRoutedEnchantment(PromoteRoutedEnchantmentPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (player.distanceToSqr(Vec3.atCenterOf(payload.stationPos())) > 64.0D) {
            return;
        }

        boolean wasActive = isStationEnchantmentActive(player, payload);
        if (MobsToolForgingCompat.promoteStationRoutedEnchantment(player.level(), payload.stationPos(), payload.partIndex(), payload.enchantment())) {
            player.containerMenu.broadcastChanges();
            if (!wasActive && isStationEnchantmentActive(player, payload)) {
                float pitch = 1.25F + player.level().random.nextFloat() * 0.15F;
                Vec3 particlePosition = activationParticlePosition(payload);
                ServerLevel level = player.serverLevel();
                level.sendParticles(
                        ParticleTypes.ENCHANT,
                        particlePosition.x,
                        particlePosition.y,
                        particlePosition.z,
                        34,
                        0.22D,
                        0.18D,
                        0.22D,
                        0.06D
                );
                player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.8F, pitch);
                player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.15F);
                player.level().playSound(
                        null,
                        payload.stationPos(),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.BLOCKS,
                        1.15F,
                        pitch
                );
                player.level().playSound(
                        null,
                        particlePosition.x,
                        particlePosition.y,
                        particlePosition.z,
                        SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS,
                        0.8F,
                        1.15F
                );
            }
        }
    }

    private static Vec3 activationParticlePosition(PromoteRoutedEnchantmentPayload payload) {
        Vec3 stationCenter = Vec3.atCenterOf(payload.stationPos());
        Vec3 position = new Vec3(payload.orbX(), payload.orbY(), payload.orbZ());
        if (!Double.isFinite(position.x)
                || !Double.isFinite(position.y)
                || !Double.isFinite(position.z)
                || position.distanceToSqr(stationCenter) > 16.0D) {
            return stationCenter.add(0.0D, 1.0D, 0.0D);
        }
        return position;
    }

    private static boolean isStationEnchantmentActive(ServerPlayer player, PromoteRoutedEnchantmentPayload payload) {
        return MobsToolForgingCompat.stationRoutedEnchantmentPreview(player.level(), payload.stationPos())
                .flatMap(preview -> preview.breakdown())
                .flatMap(breakdown -> breakdown.parts().stream()
                        .filter(part -> part.partIndex() == payload.partIndex())
                        .flatMap(part -> part.enchantments().stream())
                        .filter(enchantment -> enchantment.enchantmentId().equals(payload.enchantment()))
                        .findFirst())
                .map(enchantment -> enchantment.active())
                .orElse(false);
    }
}
