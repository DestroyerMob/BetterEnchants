package com.betterenchanting.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TreeCapitatorDataTest {
    @Test
    void treeCapitatorRegistersBothDocumentedLevels() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(
                "/data/betterenchanting/enchantment/tree_capitator.json"
        )) {
            assertNotNull(stream, "Tree Capitator enchantment data must exist");
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.matches("(?s).*\\\"max_level\\\"\\s*:\\s*2.*"));
        }
    }
}
