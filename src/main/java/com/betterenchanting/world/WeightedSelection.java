package com.betterenchanting.world;

import java.util.List;

final class WeightedSelection {
    private WeightedSelection() {
    }

    static int pickIndex(
            List<Integer> baseWeights,
            List<Boolean> receivesComboBonus,
            int maxWeight,
            double comboMultiplier,
            LongBoundedRandom random
    ) {
        if (baseWeights.size() != receivesComboBonus.size()) {
            throw new IllegalArgumentException("Weight and bonus lists must have the same size");
        }

        long totalWeight = 0L;
        int[] adjustedWeights = new int[baseWeights.size()];
        for (int index = 0; index < baseWeights.size(); index++) {
            int adjusted = Math.max(0, baseWeights.get(index));
            if (receivesComboBonus.get(index)) {
                adjusted = cappedWeight((double) adjusted * comboMultiplier, maxWeight);
            }
            adjustedWeights[index] = adjusted;
            totalWeight += adjusted;
        }

        if (totalWeight <= 0L) {
            return -1;
        }

        long roll = random.nextLong(totalWeight);
        for (int index = 0; index < adjustedWeights.length; index++) {
            roll -= adjustedWeights[index];
            if (roll < 0L) {
                return index;
            }
        }
        return adjustedWeights.length - 1;
    }

    static int cappedWeight(double weight, int maxWeight) {
        int safeMax = Math.max(1, maxWeight);
        if (!Double.isFinite(weight) || weight >= safeMax) {
            return safeMax;
        }
        return Math.max(1, (int) Math.round(weight));
    }

    @FunctionalInterface
    interface LongBoundedRandom {
        long nextLong(long bound);
    }
}
