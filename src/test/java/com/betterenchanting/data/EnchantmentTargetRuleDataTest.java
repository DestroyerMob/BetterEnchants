package com.betterenchanting.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class EnchantmentTargetRuleDataTest {
    @Test
    void channelingRequiresTheCopperAndRangedIntersection() throws IOException {
        String rules = resource("data/betterenchanting/better_enchanting/enchantment_targets/default.json");
        int comboTarget = rules.indexOf("betterenchanting:targets/weapons/copper_ranged");
        int copper = rules.lastIndexOf("betterenchanting:materials/copper", comboTarget);
        int ranged = rules.lastIndexOf("betterenchanting:weapons/ranged", comboTarget);
        int itemTags = rules.lastIndexOf("\"item_tags\"", comboTarget);

        assertTrue(comboTarget >= 0, "Missing copper+ranged target rule");
        assertTrue(itemTags >= 0 && copper > itemTags && ranged > itemTags,
                "Copper and ranged must be required by the same item_tags rule");

        String targetTag = resource("data/betterenchanting/tags/enchantment/targets/weapons/copper_ranged.json");
        assertTrue(targetTag.contains("minecraft:channeling"));
    }

    @Test
    void conductiveRequiresTheCopperAndHelmetIntersection() throws IOException {
        String rules = resource("data/betterenchanting/better_enchanting/enchantment_targets/default.json");
        int comboTarget = rules.indexOf("betterenchanting:targets/armor/copper_helmets");
        int copper = rules.lastIndexOf("betterenchanting:materials/copper", comboTarget);
        int helmets = rules.lastIndexOf("betterenchanting:armor/helmets", comboTarget);
        int itemTags = rules.lastIndexOf("\"item_tags\"", comboTarget);

        assertTrue(comboTarget >= 0, "Missing copper+helmet target rule");
        assertTrue(itemTags >= 0 && copper > itemTags && helmets > itemTags,
                "Copper and helmet must be required by the same item_tags rule");

        String targetTag = resource("data/betterenchanting/tags/enchantment/targets/armor/copper_helmets.json");
        assertTrue(targetTag.contains("betterenchanting:conductive"));
    }

    @Test
    void backstabRoutesThroughTheKnifeTarget() throws IOException {
        String rules = resource("data/betterenchanting/better_enchanting/enchantment_targets/default.json");
        assertTrue(rules.contains("\"item_tag\": \"betterenchanting:weapons/knives\""));
        assertTrue(rules.contains("\"enchantment_tag\": \"betterenchanting:targets/weapons/knives\""));

        String targetTag = resource("data/betterenchanting/tags/enchantment/targets/weapons/knives.json");
        assertTrue(targetTag.contains("betterenchanting:backstab"));
    }

    private static String resource(String path) throws IOException {
        try (InputStream stream = EnchantmentTargetRuleDataTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing test resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
