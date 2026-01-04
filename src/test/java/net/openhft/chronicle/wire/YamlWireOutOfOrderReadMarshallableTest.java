/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YamlWireOutOfOrderReadMarshallableTest extends WireTestCommon {

    @Test
    @DisplayName("Flow mapping preserves manual read order")
    public void flowMappingKeepsManualReadMarshallableInOrder() {
        assertOutOfOrderManualReadMarshallable(
                flowYaml("c", "b", "a"),
                flowYaml("b", "c", "a"),
                flowYaml("c", "a", "b"));
    }

    @Test
    @DisplayName("Indented mapping preserves manual read order")
    public void indentedMappingKeepsManualReadMarshallableInOrder() {
        assertOutOfOrderManualReadMarshallable(
                indentedYaml("c", "b", "a"),
                indentedYaml("b", "c", "a"),
                indentedYaml("c", "a", "b"));
    }

    private static void assertOutOfOrderManualReadMarshallable(String... yamls) {
        for (String yaml : yamls) {
            YamlWire wire = new YamlWire(Bytes.from(yaml)).useTextDocuments();

            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent(), "Document should be present for YAML: " + yaml);
                OutOfOrderRM dto = new OutOfOrderRM();
                dc.wire().read("rm").marshallable(dto);
                assertEquals("aye2", dto.a, "Field 'a' should read correctly for YAML: " + yaml);
                assertEquals("bee2", dto.b, "Field 'b' should read correctly for YAML: " + yaml);
                assertEquals("cee2", dto.c, "Field 'c' should read correctly for YAML: " + yaml);
            }
        }
    }

    private static String flowYaml(String... order) {
        StringBuilder sb = new StringBuilder("---\nrm: {\n");
        for (int i = 0; i < order.length; i++) {
            sb.append("  ").append(order[i]).append(": ").append(valueFor(order[i]))
                    .append(i < order.length - 1 ? ",\n" : "\n");
        }
        sb.append("}\n...\n");
        return sb.toString();
    }

    private static String indentedYaml(String... order) {
        StringBuilder sb = new StringBuilder("---\nrm:\n");
        for (String key : order) {
            sb.append("  ").append(key).append(": ").append(valueFor(key)).append('\n');
        }
        sb.append("...\n");
        return sb.toString();
    }

    private static String valueFor(String key) {
        switch (key) {
            case "a":
                return "aye2";
            case "b":
                return "bee2";
            case "c":
                return "cee2";
            default:
                throw new IllegalArgumentException("Unexpected key " + key);
        }
    }

    private static final class OutOfOrderRM implements ReadMarshallable {
        String a;
        String b;
        String c;

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            a = wire.read("a").text();
            b = wire.read("b").text();
            c = wire.read("c").text();
        }
    }
}
