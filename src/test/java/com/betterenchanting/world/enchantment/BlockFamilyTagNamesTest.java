package com.betterenchanting.world.enchantment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlockFamilyTagNamesTest {
    @Test
    void recognisesSpecificLogFamilyTagsWithoutTreatingGlobalLogTagsAsOneTree() {
        assertTrue(BlockFamilyTagNames.isLogFamilyPath("oak_logs"));
        assertTrue(BlockFamilyTagNames.isLogFamilyPath("redwood_logs"));
        assertTrue(BlockFamilyTagNames.isLogFamilyPath("logs/silverwood"));

        assertFalse(BlockFamilyTagNames.isLogFamilyPath("logs"));
        assertFalse(BlockFamilyTagNames.isLogFamilyPath("logs_that_burn"));
        assertFalse(BlockFamilyTagNames.isLogFamilyPath("overworld_natural_logs"));
        assertFalse(BlockFamilyTagNames.isLogFamilyPath("stripped_logs"));
    }

    @Test
    void recognisesOreMaterialTagsWithoutTreatingEveryOreAsMatching() {
        assertTrue(BlockFamilyTagNames.isOreFamilyPath("ores/sapphire"));
        assertTrue(BlockFamilyTagNames.isOreFamilyPath("ruby_ores"));

        assertFalse(BlockFamilyTagNames.isOreFamilyPath("ores"));
        assertFalse(BlockFamilyTagNames.isOreFamilyPath("ores_in_ground/deepslate"));
        assertFalse(BlockFamilyTagNames.isOreFamilyPath("ore_rates/dense"));
    }
}
