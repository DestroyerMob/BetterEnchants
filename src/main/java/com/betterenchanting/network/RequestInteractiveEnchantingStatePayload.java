package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestInteractiveEnchantingStatePayload(BlockPos tablePos) implements CustomPacketPayload {
    public static final Type<RequestInteractiveEnchantingStatePayload> TYPE =
            new Type<>(BetterEnchanting.id("request_interactive_enchanting_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestInteractiveEnchantingStatePayload> STREAM_CODEC =
            StreamCodec.of(RequestInteractiveEnchantingStatePayload::encode, RequestInteractiveEnchantingStatePayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, RequestInteractiveEnchantingStatePayload payload) {
        buffer.writeBlockPos(payload.tablePos());
    }

    private static RequestInteractiveEnchantingStatePayload decode(RegistryFriendlyByteBuf buffer) {
        return new RequestInteractiveEnchantingStatePayload(buffer.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
