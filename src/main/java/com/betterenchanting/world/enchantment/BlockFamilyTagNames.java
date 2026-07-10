package com.betterenchanting.world.enchantment;

final class BlockFamilyTagNames {
    private BlockFamilyTagNames() {
    }

    static boolean isLogFamilyPath(String path) {
        if (path.equals("logs")
                || path.equals("logs_that_burn")
                || path.equals("overworld_natural_logs")
                || path.equals("stripped_logs")) {
            return false;
        }
        return path.endsWith("_logs") || path.startsWith("logs/") || path.contains("/logs/");
    }

    static boolean isOreFamilyPath(String path) {
        if (path.equals("ores")) {
            return false;
        }
        return path.startsWith("ores/") || path.contains("/ores/") || path.endsWith("_ores");
    }
}
