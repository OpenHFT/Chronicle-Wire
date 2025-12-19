/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives YamlValueOut formatting paths (strings needing quoting, multi-line, sequences, maps)
 * and validates via round-trip parsing.
 */
@SuppressWarnings({"deprecation", "removal"})
public class YamlValueOutFormattingBranchesTest extends WireTestCommon {

    @Test
    public void writeAndReadComplexYaml() {
        Wire w = WireType.YAML.apply(Bytes.allocateElasticOnHeap(512));

        // Values likely to exercise quoting and multi-line formatting branches
        w.write("quoted").text("needs: quoting [brackets]");
        w.write("multiline").text("line1\nline2");
        w.write("empty").text("");
        w.write("seq").sequence(v -> {
            v.text("x");
            v.text("y");
        });
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("k1", "v1");
        m.put("k2", 2L);
        w.write("map").map(m);

        String yaml = w.toString();

        // Parse back and assert values
        YamlWire r = YamlWire.from(yaml);
        assertEquals("needs: quoting [brackets]", r.read("quoted").text());
        assertEquals("line1\nline2", r.read("multiline").text());
        assertEquals("", r.read("empty").text());
        final Object[] seq = new Object[2];
        r.read("seq").sequence(seq, (arr, in) -> {
            arr[0] = in.text();
            arr[1] = in.text();
        });
        assertArrayEquals(new Object[]{"x", "y"}, seq);
        Map<?, ?> out = r.read("map").marshallableAsMap(String.class, Object.class);
        assertEquals(m, out);
    }

    @Test
    public void writesQuotedAndMultilineValues() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        wire.write("quoted").text("needs: quoting");
        wire.write("multiline").text("first\nsecond");
        wire.write("flag").bool(true);

        String yaml = bytes.toString();
        assertTrue(yaml.contains("quoted: \"needs: quoting\""));

        // Read the whole document as a map (avoids relying on ValueIn#marshallable for root documents)
        Map<String, Object> values = YamlWire.from(yaml)
                .readAllAsMap(String.class, Object.class, new java.util.LinkedHashMap<>());
        assertEquals("needs: quoting", values.get("quoted"));
        assertEquals("first\nsecond", values.get("multiline"));
        assertEquals(true, values.get("flag"));
    }
}
