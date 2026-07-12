package com.betterenchanting.world.enchantment;

final class ReachDistance {
    private ReachDistance() {
    }

    static double nearestPoint(
            double pointX,
            double pointY,
            double pointZ,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        double x = Math.max(Math.max(minX - pointX, 0.0D), pointX - maxX);
        double y = Math.max(Math.max(minY - pointY, 0.0D), pointY - maxY);
        double z = Math.max(Math.max(minZ - pointZ, 0.0D), pointZ - maxZ);
        return Math.sqrt(x * x + y * y + z * z);
    }
}
