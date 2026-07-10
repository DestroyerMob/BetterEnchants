package com.betterenchanting.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AffinityRoleDataTest {
    private static final Pattern TAG_VALUE = Pattern.compile(
            "\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"|^\\s*\\\"([^\\\"]+)\\\"\\s*,?\\s*$",
            Pattern.MULTILINE
    );
    private static final List<String> AFFINITIES = List.of(
            "fire", "frost", "lightning", "curse", "physical",
            "mining", "defensive", "vitality", "mobility", "void"
    );

    @Test
    void everyClassifiedEnchantmentHasExactlyOnePrimaryAffinity() throws IOException {
        Map<String, Set<String>> affinitiesByEnchantment = new HashMap<>();
        Map<String, Set<String>> primaryByEnchantment = new HashMap<>();

        for (String affinity : AFFINITIES) {
            Set<String> members = loadTag("betterenchanting", affinity, new HashSet<>());
            Set<String> primaryMembers = loadTag("betterenchanting", "primary/" + affinity, new HashSet<>());
            assertTrue(members.containsAll(primaryMembers), "Primary " + affinity + " entries must remain in the ordinary affinity tag");
            members.forEach(enchantment -> affinitiesByEnchantment
                    .computeIfAbsent(enchantment, unused -> new HashSet<>())
                    .add(affinity));
            primaryMembers.forEach(enchantment -> primaryByEnchantment
                    .computeIfAbsent(enchantment, unused -> new HashSet<>())
                    .add(affinity));
        }

        assertEquals(affinitiesByEnchantment.keySet(), primaryByEnchantment.keySet());
        for (Map.Entry<String, Set<String>> entry : affinitiesByEnchantment.entrySet()) {
            assertTrue(entry.getValue().size() >= 2, entry.getKey() + " must retain its secondary affinity");
            assertEquals(1, primaryByEnchantment.get(entry.getKey()).size(), entry.getKey() + " must have one primary affinity");
        }

        assertEquals(Set.of("fire"), primaryByEnchantment.get("betterenchanting:auto_smelt"));
        assertEquals(Set.of("mining"), primaryByEnchantment.get("minecraft:efficiency"));
        assertEquals(Set.of("mining"), primaryByEnchantment.get("minecraft:fortune"));
        assertEquals(Set.of("mining"), primaryByEnchantment.get("betterenchanting:resonance"));
    }

    private static Set<String> loadTag(String namespace, String path, Set<String> resolving) throws IOException {
        String key = namespace + ":" + path;
        assertTrue(resolving.add(key), "Cyclic enchantment tag reference: " + key);
        String resourcePath = "data/" + namespace + "/tags/enchantment/" + path + ".json";
        try (InputStream stream = AffinityRoleDataTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "Missing enchantment tag " + key);
            Set<String> members = new HashSet<>();
            Matcher values = TAG_VALUE.matcher(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            while (values.find()) {
                String id = values.group(1) != null ? values.group(1) : values.group(2);
                if (id.startsWith("#")) {
                    String referenced = id.substring(1);
                    int separator = referenced.indexOf(':');
                    String referencedNamespace = separator >= 0 ? referenced.substring(0, separator) : "minecraft";
                    String referencedPath = separator >= 0 ? referenced.substring(separator + 1) : referenced;
                    members.addAll(loadTag(referencedNamespace, referencedPath, resolving));
                } else {
                    members.add(id);
                }
            }
            return members;
        } finally {
            resolving.remove(key);
        }
    }
}
