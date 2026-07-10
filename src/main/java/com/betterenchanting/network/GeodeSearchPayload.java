package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GeodeSearchPayload(
        BlockPos origin,
        BlockPos searchMin,
        BlockPos searchMax,
        List<BlockPos> buddingAmethyst,
        int durationTicks
) implements CustomPacketPayload {
    private static final int MAX_POSITIONS = 512;
    public static final Type<GeodeSearchPayload> TYPE = new Type<>(BetterEnchanting.id("geode_search"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GeodeSearchPayload> STREAM_CODEC = StreamCodec.of(
            GeodeSearchPayload::encode,
            GeodeSearchPayload::decode
    );

    public GeodeSearchPayload {
        buddingAmethyst = List.copyOf(buddingAmethyst);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, GeodeSearchPayload payload) {
        buffer.writeBlockPos(payload.origin);
        buffer.writeBlockPos(payload.searchMin);
        buffer.writeBlockPos(payload.searchMax);
        int count = Math.min(payload.buddingAmethyst.size(), MAX_POSITIONS);
        buffer.writeVarInt(count);
        for (int index = 0; index < count; index++) {
            buffer.writeBlockPos(payload.buddingAmethyst.get(index));
        }
        buffer.writeVarInt(payload.durationTicks);
    }

    private static GeodeSearchPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos origin = buffer.readBlockPos();
        BlockPos searchMin = buffer.readBlockPos();
        BlockPos searchMax = buffer.readBlockPos();
        int count = Math.min(buffer.readVarInt(), MAX_POSITIONS);
        List<BlockPos> positions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            positions.add(buffer.readBlockPos());
        }
        return new GeodeSearchPayload(origin, searchMin, searchMax, positions, buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
