package com.betterenchanting.world.enchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class VeinMinerModeState {
    private static final Map<UUID, VeinMinerMode> MODES = new HashMap<>();

    private VeinMinerModeState() {
    }

    public static VeinMinerMode current(Player player) {
        return MODES.getOrDefault(player.getUUID(), VeinMinerMode.CONNECTED_MATCHING);
    }

    public static void cycle(ServerPlayer player) {
        VeinMinerMode mode = current(player).next();
        MODES.put(player.getUUID(), mode);
        player.displayClientMessage(
                Component.translatable(
                        "message.betterenchanting.vein_miner_mode",
                        Component.translatable(mode.translationKey()).withStyle(ChatFormatting.AQUA)
                ).withStyle(ChatFormatting.GRAY),
                true
        );
    }
}
