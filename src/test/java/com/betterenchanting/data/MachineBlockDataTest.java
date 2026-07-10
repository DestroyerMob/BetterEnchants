package com.betterenchanting.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MachineBlockDataTest {
    @Test
    void bothMachinesArePickaxeMineable() throws IOException {
        String tag = resource("/data/minecraft/tags/block/mineable/pickaxe.json");
        assertTrue(tag.contains("\"betterenchanting:arcane_crucible\""));
        assertTrue(tag.contains("\"betterenchanting:attunement_pedestal\""));
    }

    @Test
    void bothMachinesHaveSelfDropLootTables() throws IOException {
        String crucible = resource("/data/betterenchanting/loot_table/blocks/arcane_crucible.json");
        String pedestal = resource("/data/betterenchanting/loot_table/blocks/attunement_pedestal.json");
        assertTrue(crucible.contains("\"betterenchanting:arcane_crucible\""));
        assertTrue(pedestal.contains("\"betterenchanting:attunement_pedestal\""));
    }

    @Test
    void bothMachinesRejectCarryOnAndGeneralRelocation() throws IOException {
        String carryOn = resource("/data/carryon/tags/block/block_blacklist.json");
        String relocation = resource("/data/c/tags/block/relocation_not_supported.json");
        for (String block : new String[]{"arcane_crucible", "attunement_pedestal"}) {
            String id = "\"betterenchanting:" + block + "\"";
            assertTrue(carryOn.contains(id), block + " must remain in Carry On's block blacklist");
            assertTrue(relocation.contains(id), block + " must reject generic block relocation");
        }
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing machine block resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
