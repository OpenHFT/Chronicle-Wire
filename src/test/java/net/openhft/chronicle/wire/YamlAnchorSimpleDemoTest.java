/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Demonstrates YAML anchor functionality in Chronicle Wire using shared configurations.
 */
public class YamlAnchorSimpleDemoTest extends WireTestCommon {

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "NP_UNWRITTEN_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    public static class Config extends SelfDescribingMarshallable {
        String text;
        int value;
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "NP_UNWRITTEN_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class MultiConfig extends SelfDescribingMarshallable {
        Config first;
        Config second;
        Config third;
    }

    @Test
    @DisplayName("YAML anchors share scalar field values")
    @SuppressFBWarnings(
            value = "NP_UNWRITTEN_FIELD",
            justification = "Wire marshalling populates fields without explicit setters.")
    public void shouldShareFieldValuesUsingAnchors() {
        String yaml = "!net.openhft.chronicle.wire.YamlAnchorSimpleDemoTest$MultiConfig {\n" +
                "  first: {\n" +
                "    text: &sharedText hello,\n" +
                "    value: &sharedValue 42\n" +
                "  },\n" +
                "  second: {\n" +
                "    text: *sharedText,\n" +
                "    value: 100\n" +
                "  },\n" +
                "  third: {\n" +
                "    text: world,\n" +
                "    value: *sharedValue\n" +
                "  }\n" +
                "}\n";

        MultiConfig config = WireType.YAML.fromString(MultiConfig.class, yaml);

        // Show the results
        assertEquals("hello", config.first.text, "First config text should be hello");
        assertSame(config.first.text, config.second.text, "Text anchor should reuse string instance");
        assertEquals("world", config.third.text, "Third config text should be world");

        // Verify text anchors worked (numeric anchors seem to have issues)
        assertEquals(42, config.first.value, "First config value should be 42");
        assertEquals(100, config.second.value, "Second config value should be explicit 100");
        assertEquals(42, config.third.value, "Third config value should reuse shared anchor");
    }

    @Test
    @DisplayName("YAML anchors share object instances across fields")
    @SuppressFBWarnings(
            value = "NP_UNWRITTEN_FIELD",
            justification = "Wire marshalling populates fields without explicit setters.")
    public void shouldShareObjectInstancesUsingAnchors() {
        String yaml = "first: &shared !net.openhft.chronicle.wire.YamlAnchorSimpleDemoTest$Config {\n" +
                "  text: shared config,\n" +
                "  value: 999\n" +
                "}\n" +
                "second: *shared\n" +
                "third: !net.openhft.chronicle.wire.YamlAnchorSimpleDemoTest$Config {\n" +
                "  text: different config,\n" +
                "  value: 111\n" +
                "}\n";

        MultiConfig config = WireType.YAML.fromString(MultiConfig.class, yaml);

        // Show the results
        assertEquals("shared config", config.first.text, "First config text should be shared config");
        assertSame(config.first.text, config.second.text, "Second config should share anchor instance");
        assertEquals("different config", config.third.text, "Third config text should be different config");
    }
}
