package com.betterenchanting.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RoutedPriorityOrderTest {
    @Test
    void finalToolPriorityMovesChosenMiningEnchantmentAheadOfCrossPartConflicts() {
        List<String> available = List.of("efficiency", "harvest", "tree_capitator", "unbreaking");

        List<String> ordered = RoutedPriorityOrder.apply(
                List.of("tree_capitator", "efficiency"),
                available,
                value -> value
        );

        assertEquals(List.of("tree_capitator", "efficiency", "harvest", "unbreaking"), ordered);
        assertEquals(List.of("tree_capitator", "efficiency"), ordered.stream()
                .filter(value -> !value.equals("unbreaking"))
                .limit(2)
                .toList());
    }

    @Test
    void stalePriorityEntriesDoNotRemoveAvailableEnchantments() {
        assertEquals(
                List.of("harvest", "tree_capitator", "efficiency"),
                RoutedPriorityOrder.apply(
                        List.of("missing", "harvest"),
                        List.of("tree_capitator", "efficiency", "harvest"),
                        value -> value
                )
        );
    }
}
