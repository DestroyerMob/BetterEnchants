package com.betterenchanting.world.enchantment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.betterenchanting.world.enchantment.TreeReplantPlanner.Position;
import java.util.List;
import org.junit.jupiter.api.Test;

class TreeReplantPlannerTest {
    @Test
    void singleTrunkReplantsAtItsLowestLog() {
        assertEquals(
                List.of(new Position(4, 63, 8)),
                TreeReplantPlanner.plantingPositions(
                        List.of(new Position(4, 65, 8), new Position(4, 64, 8), new Position(4, 63, 8)),
                        new Position(4, 65, 8)
                )
        );
    }

    @Test
    void twoByTwoTrunkReplantsTheWholeFootprint() {
        List<Position> logs = List.of(
                new Position(10, 70, 20), new Position(11, 70, 20),
                new Position(10, 70, 21), new Position(11, 70, 21),
                new Position(10, 71, 20), new Position(11, 71, 20),
                new Position(10, 71, 21), new Position(11, 71, 21)
        );

        assertEquals(
                List.of(
                        new Position(10, 70, 20), new Position(11, 70, 20),
                        new Position(10, 70, 21), new Position(11, 70, 21)
                ),
                TreeReplantPlanner.plantingPositions(logs, new Position(11, 71, 21))
        );
    }

    @Test
    void irregularRootsDoNotBecomeAnAccidentalTwoByTwoTree() {
        assertEquals(
                List.of(new Position(0, 50, 0)),
                TreeReplantPlanner.plantingPositions(
                        List.of(
                                new Position(-1, 50, 0),
                                new Position(0, 50, 0),
                                new Position(1, 50, 0),
                                new Position(0, 50, 1)
                        ),
                        new Position(0, 53, 0)
                )
        );
    }
}
