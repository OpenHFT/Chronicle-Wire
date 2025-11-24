/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class YamlWireOutOfOrderReadMarshallableTest extends WireTestCommon {

    @Test
    public void flowMappingKeepsManualReadMarshallableInOrder() {
        assertOutOfOrderManualReadMarshallable(
                flowYaml("c", "b", "a"),
                flowYaml("b", "c", "a"),
                flowYaml("c", "a", "b"));
    }

    @Test
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
                assertTrue(dc.isPresent());
                OutOfOrderRM dto = new OutOfOrderRM();
                dc.wire().read("rm").marshallable(dto);
                assertEquals("aye2", dto.a);
                assertEquals("bee2", dto.b);
                assertEquals("cee2", dto.c);
            }
        }
    }

    private static String flowYaml(String... order) {
        StringBuilder sb = new StringBuilder("---\nrm: {\n");
        for (int i = 0; i < order.length; i++) {
            sb.append("  ").append(order[i]).append(": ").append(valueFor(order[i]));
            if (i < order.length - 1)
                sb.append(",\n");
            else
                sb.append("\n");
        }
        sb.append("}\n...\n");
        return sb.toString();
    }

    private static String indentedYaml(String... order) {
        StringBuilder sb = new StringBuilder("---\nrm:\n");
        for (String key : order) {
            sb.append("  ").append(key).append(": ").append(valueFor(key)).append("\n");
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
