package com.betterenchanting.network;

import com.betterenchanting.BetterEnchanting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record InteractiveEnchantingStatePayload(BlockPos tablePos, int reagentCount, List<Option> options)
        implements CustomPacketPayload {
    public static final Type<InteractiveEnchantingStatePayload> TYPE =
            new Type<>(BetterEnchanting.id("interactive_enchanting_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InteractiveEnchantingStatePayload> STREAM_CODEC =
            StreamCodec.of(InteractiveEnchantingStatePayload::encode, InteractiveEnchantingStatePayload::decode);

    public InteractiveEnchantingStatePayload {
        tablePos = tablePos.immutable();
        options = List.copyOf(options);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, InteractiveEnchantingStatePayload payload) {
        buffer.writeBlockPos(payload.tablePos());
        buffer.writeVarInt(payload.reagentCount());
        buffer.writeVarInt(payload.options().size());
        for (Option option : payload.options()) {
            buffer.writeVarInt(option.requirement());
            buffer.writeVarInt(option.cost());
            buffer.writeVarInt(option.disabledFlags());
            buffer.writeBoolean(option.overlevel());
            buffer.writeBoolean(option.infusion());
            buffer.writeBoolean(option.allCluesRevealed());
            buffer.writeVarInt(option.clues().size());
            for (Clue clue : option.clues()) {
                buffer.writeVarInt(clue.enchantmentId());
                buffer.writeVarInt(clue.level());
            }
        }
    }

    private static InteractiveEnchantingStatePayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos tablePos = buffer.readBlockPos();
        int reagentCount = buffer.readVarInt();
        int optionCount = buffer.readVarInt();
        List<Option> options = new ArrayList<>(optionCount);
        for (int index = 0; index < optionCount; index++) {
            int requirement = buffer.readVarInt();
            int cost = buffer.readVarInt();
            int disabledFlags = buffer.readVarInt();
            boolean overlevel = buffer.readBoolean();
            boolean infusion = buffer.readBoolean();
            boolean allCluesRevealed = buffer.readBoolean();
            int clueCount = buffer.readVarInt();
            List<Clue> clues = new ArrayList<>(clueCount);
            for (int clueIndex = 0; clueIndex < clueCount; clueIndex++) {
                clues.add(new Clue(buffer.readVarInt(), buffer.readVarInt()));
            }
            options.add(new Option(requirement, cost, disabledFlags, overlevel, infusion,
                    allCluesRevealed, clues));
        }
        return new InteractiveEnchantingStatePayload(tablePos, reagentCount, options);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Option(int requirement, int cost, int disabledFlags, boolean overlevel,
                         boolean infusion, boolean allCluesRevealed, List<Clue> clues) {
        public Option {
            clues = List.copyOf(clues);
        }

        public boolean available(int reagentCount) {
            return this.requirement > 0 && this.cost > 0 && this.disabledFlags == 0 && reagentCount > 0;
        }
    }

    public record Clue(int enchantmentId, int level) {
    }
}
