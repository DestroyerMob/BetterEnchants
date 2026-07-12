package com.betterenchanting.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class EnchantmentDescriptionDataTest {
    private static final Pattern DESCRIPTION_TRANSLATION = Pattern.compile(
            "\\\"description\\\"\\s*:\\s*\\{\\s*\\\"translate\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
    );
    private static final Pattern LANGUAGE_ENTRY = Pattern.compile(
            "\\\"([^\\\"]+)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\""
    );

    @Test
    void everyEnchantmentHasAnEnchantmentDescriptionsEntry() throws IOException, URISyntaxException {
        URL enchantmentDirectory = getClass().getResource("/data/betterenchanting/enchantment");
        assertNotNull(enchantmentDirectory, "Better Enchanting enchantment data directory must exist");

        List<Path> enchantments;
        try (Stream<Path> files = Files.list(Path.of(enchantmentDirectory.toURI()))) {
            enchantments = files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        assertFalse(enchantments.isEmpty(), "Better Enchanting must define at least one enchantment");

        Map<String, String> languageEntries = loadEnglishLanguageEntries();
        for (Path enchantment : enchantments) {
            String fileName = enchantment.getFileName().toString();
            String enchantmentId = fileName.substring(0, fileName.length() - ".json".length());
            String translationKey = "enchantment.betterenchanting." + enchantmentId;

            Matcher description = DESCRIPTION_TRANSLATION.matcher(Files.readString(enchantment));
            assertTrue(description.find(), fileName + " must define a translatable description");
            assertEquals(translationKey, description.group(1), fileName + " must use its standard translation key");

            assertNonBlankEntry(languageEntries, translationKey);
            assertNonBlankEntry(languageEntries, translationKey + ".desc");
        }
    }

    private static Map<String, String> loadEnglishLanguageEntries() throws IOException {
        URL languageFile = EnchantmentDescriptionDataTest.class.getResource(
                "/assets/betterenchanting/lang/en_us.json"
        );
        assertNotNull(languageFile, "English language file must exist");

        String json;
        try (var stream = languageFile.openStream()) {
            json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        Map<String, String> entries = new HashMap<>();
        Matcher matcher = LANGUAGE_ENTRY.matcher(json);
        while (matcher.find()) {
            entries.put(matcher.group(1), matcher.group(2));
        }
        return entries;
    }

    private static void assertNonBlankEntry(Map<String, String> entries, String key) {
        assertTrue(entries.containsKey(key), "Missing English language entry: " + key);
        assertFalse(entries.get(key).isBlank(), "English language entry must not be blank: " + key);
    }
}
