package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ResonanceHighlightPayload(
        BlockPos origin,
        ResourceLocation ore,
        int searchRadius,
        List<BlockPos> positions,
        int durationTicks
) implements CustomPacketPayload {
    private static final int MAX_POSITIONS = 4096;
    public static final Type<ResonanceHighlightPayload> TYPE = new Type<>(BetterEnchanting.id("resonance_highlights"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResonanceHighlightPayload> STREAM_CODEC = StreamCodec.of(
            ResonanceHighlightPayload::encode,
            ResonanceHighlightPayload::decode);

    public ResonanceHighlightPayload {
        positions = List.copyOf(positions);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ResonanceHighlightPayload payload) {
        buffer.writeBlockPos(payload.origin);
        buffer.writeResourceLocation(payload.ore);
        buffer.writeVarInt(payload.searchRadius);
        int count = Math.min(payload.positions.size(), MAX_POSITIONS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeBlockPos(payload.positions.get(i));
        }
        buffer.writeVarInt(payload.durationTicks);
    }

    private static ResonanceHighlightPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos origin = buffer.readBlockPos();
        ResourceLocation ore = buffer.readResourceLocation();
        int searchRadius = buffer.readVarInt();
        int count = buffer.readVarInt();
        int storedCount = Math.min(count, MAX_POSITIONS);
        List<BlockPos> positions = new ArrayList<>(storedCount);
        for (int i = 0; i < storedCount; i++) {
            positions.add(buffer.readBlockPos());
        }
        for (int i = storedCount; i < count; i++) {
            buffer.readBlockPos();
        }
        return new ResonanceHighlightPayload(origin, ore, searchRadius, positions, buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
