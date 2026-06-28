package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CycleVeinMinerModePayload() implements CustomPacketPayload {
    public static final CycleVeinMinerModePayload INSTANCE = new CycleVeinMinerModePayload();
    public static final Type<CycleVeinMinerModePayload> TYPE = new Type<>(BetterEnchanting.id("cycle_vein_miner_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CycleVeinMinerModePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
