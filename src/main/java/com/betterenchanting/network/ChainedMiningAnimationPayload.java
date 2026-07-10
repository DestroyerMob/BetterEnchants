package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChainedMiningAnimationPayload(int channel, boolean active) implements CustomPacketPayload {
    public static final int TREE_CAPITATOR = 1;
    public static final int VEIN_MINER = 2;
    public static final Type<ChainedMiningAnimationPayload> TYPE = new Type<>(BetterEnchanting.id("chained_mining_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainedMiningAnimationPayload> STREAM_CODEC = StreamCodec.of(
            ChainedMiningAnimationPayload::encode,
            ChainedMiningAnimationPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, ChainedMiningAnimationPayload payload) {
        buffer.writeVarInt(payload.channel);
        buffer.writeBoolean(payload.active);
    }

    private static ChainedMiningAnimationPayload decode(RegistryFriendlyByteBuf buffer) {
        return new ChainedMiningAnimationPayload(buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
