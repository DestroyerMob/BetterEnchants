package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OreRevealerHighlightPayload(List<BlockPos> positions, int durationTicks) implements CustomPacketPayload {
    private static final int MAX_POSITIONS = 4096;
    public static final Type<OreRevealerHighlightPayload> TYPE = new Type<>(BetterEnchanting.id("ore_revealer_highlights"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OreRevealerHighlightPayload> STREAM_CODEC = StreamCodec.of(
            OreRevealerHighlightPayload::encode,
            OreRevealerHighlightPayload::decode);

    public OreRevealerHighlightPayload {
        positions = List.copyOf(positions);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, OreRevealerHighlightPayload payload) {
        int count = Math.min(payload.positions.size(), MAX_POSITIONS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeBlockPos(payload.positions.get(i));
        }
        buffer.writeVarInt(payload.durationTicks);
    }

    private static OreRevealerHighlightPayload decode(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        int storedCount = Math.min(count, MAX_POSITIONS);
        List<BlockPos> positions = new ArrayList<>(storedCount);
        for (int i = 0; i < storedCount; i++) {
            positions.add(buffer.readBlockPos());
        }
        for (int i = storedCount; i < count; i++) {
            buffer.readBlockPos();
        }
        return new OreRevealerHighlightPayload(positions, buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
