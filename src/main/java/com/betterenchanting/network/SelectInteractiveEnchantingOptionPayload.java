package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SelectInteractiveEnchantingOptionPayload(BlockPos tablePos, int option) implements CustomPacketPayload {
    public static final Type<SelectInteractiveEnchantingOptionPayload> TYPE =
            new Type<>(BetterEnchanting.id("select_interactive_enchanting_option"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectInteractiveEnchantingOptionPayload> STREAM_CODEC =
            StreamCodec.of(SelectInteractiveEnchantingOptionPayload::encode, SelectInteractiveEnchantingOptionPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, SelectInteractiveEnchantingOptionPayload payload) {
        buffer.writeBlockPos(payload.tablePos());
        buffer.writeVarInt(payload.option());
    }

    private static SelectInteractiveEnchantingOptionPayload decode(RegistryFriendlyByteBuf buffer) {
        return new SelectInteractiveEnchantingOptionPayload(buffer.readBlockPos(), buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
