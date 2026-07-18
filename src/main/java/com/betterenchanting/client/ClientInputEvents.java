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
    private static final int CONTROLLER_ACTION_GRACE_TICKS = 8;
    private static final long NO_FORWARD_PRESS = Long.MIN_VALUE;
    private static final KeyMapping TOGGLE_VEIN_MINER_MODE = new KeyMapping(
            "key.betterenchanting.toggle_vein_miner_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.betterenchanting"
    );
    private static final KeyMapping ACTIVATE_FLASH_STEP = new KeyMapping(
            "key.betterenchanting.activate_flash_step",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.betterenchanting"
    );
    private static long clientTicks;
    private static long lastForwardPressTick = NO_FORWARD_PRESS;
    private static boolean wasForwardDown;
    private static int pendingFlashStepTicks;
    private static int pendingVeinModeTicks;

    private ClientInputEvents() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_VEIN_MINER_MODE);
        event.register(ACTIVATE_FLASH_STEP);
    }

    public static void detectFlashStep(ClientTickEvent.Post event) {
        clientTicks++;

        Minecraft minecraft = Minecraft.getInstance();
        while (ACTIVATE_FLASH_STEP.consumeClick()) {
            pendingFlashStepTicks = CONTROLLER_ACTION_GRACE_TICKS;
        }
        while (TOGGLE_VEIN_MINER_MODE.consumeClick()) {
            pendingVeinModeTicks = CONTROLLER_ACTION_GRACE_TICKS;
        }
        if (minecraft.player != null && minecraft.level != null && minecraft.screen == null && !minecraft.isPaused()) {
            if (pendingFlashStepTicks > 0) {
                PacketDistributor.sendToServer(FlashStepPayload.INSTANCE);
                pendingFlashStepTicks = 0;
            }
            if (pendingVeinModeTicks > 0) {
                PacketDistributor.sendToServer(CycleVeinMinerModePayload.INSTANCE);
                pendingVeinModeTicks = 0;
            }
        } else {
            pendingFlashStepTicks = Math.max(0, pendingFlashStepTicks - 1);
            pendingVeinModeTicks = Math.max(0, pendingVeinModeTicks - 1);
        }

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
