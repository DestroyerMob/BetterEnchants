package com.betterenchanting.client;

import com.betterenchanting.network.ChainedMiningAnimationPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ChainedMiningAnimationState {
    private static final int RELEASE_GRACE_TICKS = 4;
    private static int activeChannels;
    private static int releaseGraceTicks;

    private ChainedMiningAnimationState() {
    }

    public static void update(ChainedMiningAnimationPayload payload) {
        int channel = payload.channel();
        if (channel != ChainedMiningAnimationPayload.TREE_CAPITATOR
                && channel != ChainedMiningAnimationPayload.VEIN_MINER) {
            return;
        }

        if (payload.active()) {
            activeChannels |= channel;
            releaseGraceTicks = 0;
        } else {
            activeChannels &= ~channel;
            releaseGraceTicks = Math.max(releaseGraceTicks, RELEASE_GRACE_TICKS);
        }
    }

    public static boolean suppressesReequipAnimation() {
        return activeChannels != 0 || releaseGraceTicks > 0;
    }

    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            activeChannels = 0;
            releaseGraceTicks = 0;
        } else if (activeChannels == 0 && releaseGraceTicks > 0) {
            releaseGraceTicks--;
        }
    }
}
