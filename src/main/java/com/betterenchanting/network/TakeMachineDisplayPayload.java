package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TakeMachineDisplayPayload(BlockPos machinePos, int slot, boolean takeAll)
        implements CustomPacketPayload {
    public static final Type<TakeMachineDisplayPayload> TYPE =
            new Type<>(BetterEnchanting.id("take_machine_display"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TakeMachineDisplayPayload> STREAM_CODEC =
            StreamCodec.of(TakeMachineDisplayPayload::encode, TakeMachineDisplayPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, TakeMachineDisplayPayload payload) {
        buffer.writeBlockPos(payload.machinePos());
        buffer.writeVarInt(payload.slot());
        buffer.writeBoolean(payload.takeAll());
    }

    private static TakeMachineDisplayPayload decode(RegistryFriendlyByteBuf buffer) {
        return new TakeMachineDisplayPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
