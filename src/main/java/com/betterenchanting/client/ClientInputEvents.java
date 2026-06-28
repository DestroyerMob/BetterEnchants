package com.betterenchanting.client;

import com.betterenchanting.network.CycleVeinMinerModePayload;
import com.betterenchanting.network.FlashStepPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ClientInputEvents {
    private static final int DOUBLE_TAP_WINDOW_TICKS = 7;
    private static final long NO_FORWARD_PRESS = Long.MIN_VALUE;
    private static final KeyMapping TOGGLE_VEIN_MINER_MODE = new KeyMapping(
            "key.betterenchanting.toggle_vein_miner_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.betterenchanting"
    );
    private static long clientTicks;
    private static long lastForwardPressTick = NO_FORWARD_PRESS;
    private static boolean wasForwardDown;

    private ClientInputEvents() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_VEIN_MINER_MODE);
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

        while (TOGGLE_VEIN_MINER_MODE.consumeClick()) {
            PacketDistributor.sendToServer(CycleVeinMinerModePayload.INSTANCE);
        }
    }

    private static void resetForwardState() {
        wasForwardDown = false;
        lastForwardPressTick = NO_FORWARD_PRESS;
    }
}
