package com.betterenchanting.network;

import com.betterenchanting.client.ResonanceHighlights;
import com.betterenchanting.world.enchantment.FlashStepEnchantmentEvents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(FlashStepPayload.TYPE, FlashStepPayload.STREAM_CODEC, ModNetworking::handleFlashStep);
        registrar.playToClient(ResonanceHighlightPayload.TYPE, ResonanceHighlightPayload.STREAM_CODEC, ModNetworking::handleResonanceHighlights);
    }

    private static void handleFlashStep(FlashStepPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            FlashStepEnchantmentEvents.tryFlashStep(player);
        }
    }

    private static void handleResonanceHighlights(ResonanceHighlightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ResonanceHighlights.add(payload));
    }
}
