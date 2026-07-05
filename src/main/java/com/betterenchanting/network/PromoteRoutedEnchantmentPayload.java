package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PromoteRoutedEnchantmentPayload(BlockPos stationPos, int partIndex, ResourceLocation enchantment, double orbX, double orbY, double orbZ) implements CustomPacketPayload {
    public static final Type<PromoteRoutedEnchantmentPayload> TYPE = new Type<>(BetterEnchanting.id("promote_routed_enchantment"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PromoteRoutedEnchantmentPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PromoteRoutedEnchantmentPayload decode(RegistryFriendlyByteBuf buffer) {
            return new PromoteRoutedEnchantmentPayload(
                    buffer.readBlockPos(),
                    buffer.readVarInt(),
                    buffer.readResourceLocation(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PromoteRoutedEnchantmentPayload payload) {
            buffer.writeBlockPos(payload.stationPos());
            buffer.writeVarInt(payload.partIndex());
            buffer.writeResourceLocation(payload.enchantment());
            buffer.writeDouble(payload.orbX());
            buffer.writeDouble(payload.orbY());
            buffer.writeDouble(payload.orbZ());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
