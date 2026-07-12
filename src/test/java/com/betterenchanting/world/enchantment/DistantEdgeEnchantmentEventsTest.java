package com.betterenchanting.world.enchantment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DistantEdgeEnchantmentEventsTest {
    @Test
    void measuresToNearestHitboxPoint() {
        assertEquals(3.0D, distance(0.0D, 1.0D, 0.5D), 0.0001D);
    }

    @Test
    void returnsZeroInsideHitbox() {
        assertEquals(0.0D, distance(3.5D, 1.0D, 0.5D), 0.0001D);
    }

    @Test
    void includesDiagonalSeparation() {
        assertEquals(Math.sqrt(18.0D),
                distance(0.0D, 1.0D, -3.0D),
                0.0001D);
    }

    private static double distance(double x, double y, double z) {
        return ReachDistance.nearestPoint(
                x, y, z,
                3.0D, 0.0D, 0.0D,
                4.0D, 2.0D, 1.0D
        );
    }
}
