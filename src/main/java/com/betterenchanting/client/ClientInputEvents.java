package com.betterenchanting.client;

import com.betterenchanting.network.FlashStepPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientInputEvents {
    private static final int DOUBLE_TAP_WINDOW_TICKS = 7;
    private static final long NO_FORWARD_PRESS = Long.MIN_VALUE;
    private static long clientTicks;
    private static long lastForwardPressTick = NO_FORWARD_PRESS;
    private static boolean wasForwardDown;

    private ClientInputEvents() {
    }

    public static void detectFlashStep(ClientTickEvent.Post event) {
        clientTicks++;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || minecraft.isPaused()) {
            resetForwardState();
            return;
        }

        boolean forwardDown = minecraft.options.keyUp.isDown();
        if (forwardDown && !wasForwardDown) {
            if (lastForwardPressTick != NO_FORWARD_PRESS && clientTicks - lastForwardPressTick <= DOUBLE_TAP_WINDOW_TICKS) {
                PacketDistributor.sendToServer(FlashStepPayload.INSTANCE);
                lastForwardPressTick = NO_FORWARD_PRESS;
            } else {
                lastForwardPressTick = clientTicks;
            }
        }
        wasForwardDown = forwardDown;
    }

    private static void resetForwardState() {
        wasForwardDown = false;
        lastForwardPressTick = NO_FORWARD_PRESS;
    }
}
