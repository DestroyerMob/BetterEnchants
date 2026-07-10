package com.betterenchanting.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class MachineDisplayState {
    private static final Map<BlockPos, PublishedMachine> MACHINES = new HashMap<>();
    private static ClientLevel currentLevel;

    private MachineDisplayState() {
    }

    public static void publish(ClientLevel level, BlockPos pos, List<Display> displays) {
        if (currentLevel != level) {
            currentLevel = level;
            MACHINES.clear();
        }
        MACHINES.put(pos.immutable(), new PublishedMachine(List.copyOf(displays), level.getGameTime()));
    }

    public static List<Display> nearby(ClientLevel level, Vec3 origin, double maximumDistance) {
        if (currentLevel != level) {
            currentLevel = level;
            MACHINES.clear();
            return List.of();
        }
        long time = level.getGameTime();
        MACHINES.entrySet().removeIf(entry -> time - entry.getValue().lastSeenTick() > 2L);
        double maximumDistanceSquared = maximumDistance * maximumDistance;
        List<Display> nearby = new ArrayList<>();
        for (PublishedMachine machine : MACHINES.values()) {
            for (Display display : machine.displays()) {
                if (display.position().distanceToSqr(origin) <= maximumDistanceSquared) {
                    nearby.add(display);
                }
            }
        }
        return List.copyOf(nearby);
    }

    public record Display(BlockPos machinePos, int slot, Vec3 position, ItemStack stack, double pickRadius) {
        public Display {
            machinePos = machinePos.immutable();
            stack = stack.copy();
        }
    }

    private record PublishedMachine(List<Display> displays, long lastSeenTick) {
    }
}
