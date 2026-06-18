package com.betterenchanting.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeightedSelectionTest {
    @Test
    void cappedWeightHandlesConfigEdges() {
        assertEquals(1, WeightedSelection.cappedWeight(0.0D, 1_000));
        assertEquals(1, WeightedSelection.cappedWeight(-20.0D, 1_000));
        assertEquals(1_000, WeightedSelection.cappedWeight(Double.NaN, 1_000));
        assertEquals(1_000, WeightedSelection.cappedWeight(Double.POSITIVE_INFINITY, 1_000));
        assertEquals(1_000, WeightedSelection.cappedWeight(100_000_000_000.0D, 1_000));
        assertEquals(1, WeightedSelection.cappedWeight(100.0D, 0));
    }

    @Test
    void pickIndexUsesLongTotalsWithoutOverflowing() {
        List<Integer> weights = new ArrayList<>(Collections.nCopies(10_000, 1_000_000_000));
        List<Boolean> bonuses = new ArrayList<>(Collections.nCopies(weights.size(), false));

        int picked = WeightedSelection.pickIndex(
                weights,
                bonuses,
                1_000_000_000,
                1.0D,
                bound -> bound - 1L
        );

        assertEquals(weights.size() - 1, picked);
    }

    @Test
    void comboBonusIsCappedBeforeSelection() {
        int picked = WeightedSelection.pickIndex(
                List.of(1, 1),
                List.of(false, true),
                10,
                1_000_000_000.0D,
                ignored -> 10L
        );

        assertEquals(1, picked);
    }

    @Test
    void invalidWeightShapeFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> WeightedSelection.pickIndex(
                List.of(1),
                List.of(),
                10,
                1.0D,
                ignored -> 0L
        ));
    }
}
