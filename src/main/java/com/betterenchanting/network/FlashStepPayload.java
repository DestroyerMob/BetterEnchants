package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FlashStepPayload() implements CustomPacketPayload {
    public static final FlashStepPayload INSTANCE = new FlashStepPayload();
    public static final Type<FlashStepPayload> TYPE = new Type<>(BetterEnchanting.id("flash_step"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlashStepPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
