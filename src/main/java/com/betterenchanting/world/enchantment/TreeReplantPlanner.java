package com.betterenchanting.world.enchantment;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TreeReplantPlanner {
    private TreeReplantPlanner() {
    }

    public static List<Position> plantingPositions(Collection<Position> logs, Position origin) {
        if (logs.isEmpty()) {
            return List.of(origin);
        }

        int baseY = logs.stream().mapToInt(Position::y).min().orElse(origin.y());
        Set<Position> baseLogs = new HashSet<>();
        logs.stream().filter(log -> log.y() == baseY).forEach(baseLogs::add);
        List<Position> orderedBase = baseLogs.stream()
                .sorted(Comparator.comparingInt(Position::x).thenComparingInt(Position::z))
                .toList();

        for (Position northWest : orderedBase) {
            Position northEast = northWest.offset(1, 0);
            Position southWest = northWest.offset(0, 1);
            Position southEast = northWest.offset(1, 1);
            if (baseLogs.contains(northEast) && baseLogs.contains(southWest) && baseLogs.contains(southEast)) {
                return List.of(northWest, northEast, southWest, southEast);
            }
        }

        return orderedBase.stream()
                .min(Comparator.comparingLong((Position position) -> position.horizontalDistanceSquared(origin))
                        .thenComparingInt(Position::x)
                        .thenComparingInt(Position::z))
                .map(List::of)
                .orElseGet(() -> List.of(origin));
    }

    public record Position(int x, int y, int z) {
        private Position offset(int xOffset, int zOffset) {
            return new Position(x + xOffset, y, z + zOffset);
        }

        private long horizontalDistanceSquared(Position other) {
            long xDistance = x - (long) other.x;
            long zDistance = z - (long) other.z;
            return xDistance * xDistance + zDistance * zDistance;
        }
    }
}
