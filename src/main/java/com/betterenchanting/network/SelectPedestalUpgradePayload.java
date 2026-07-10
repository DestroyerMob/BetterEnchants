package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectPedestalUpgradePayload(
        BlockPos pedestalPos,
        int partIndex,
        ResourceLocation enchantment
) implements CustomPacketPayload {
    public static final Type<SelectPedestalUpgradePayload> TYPE =
            new Type<>(BetterEnchanting.id("select_pedestal_upgrade"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectPedestalUpgradePayload> STREAM_CODEC =
            StreamCodec.of(SelectPedestalUpgradePayload::encode, SelectPedestalUpgradePayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, SelectPedestalUpgradePayload payload) {
        buffer.writeBlockPos(payload.pedestalPos());
        buffer.writeVarInt(payload.partIndex());
        buffer.writeResourceLocation(payload.enchantment());
    }

    private static SelectPedestalUpgradePayload decode(RegistryFriendlyByteBuf buffer) {
        return new SelectPedestalUpgradePayload(
                buffer.readBlockPos(),
                buffer.readVarInt(),
                buffer.readResourceLocation()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
