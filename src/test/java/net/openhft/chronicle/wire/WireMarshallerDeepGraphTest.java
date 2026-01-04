/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WireMarshallerDeepGraphTest extends WireTestCommon {

    @Test
    @DisplayName("Deep graph round trips in binary and text")
    public void deepGraphBinaryAndText() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT}) {
            Parent p = new Parent();
            p.children.add(new Child(1, "a"));
            p.children.add(new Child(2, "b"));
            p.counters.put("x", 10L);
            p.counters.put("y", 20L);

            Wire w = wt.apply(Bytes.allocateElasticOnHeap(512));
            w.write("o").object(p);
            Parent r = w.read("o").object(Parent.class);
            assertEquals(p,
                    r,
                    "Deep graph should round trip for " + wt);
            assertEquals("p", r.title, "Parent title should round trip for " + wt);
            assertEquals(2, r.children.size(), "Child count should round trip for " + wt);
            assertEquals(1, r.children.get(0).id, "First child id should round trip for " + wt);
            assertEquals("a", r.children.get(0).name, "First child name should round trip for " + wt);
            assertEquals(2, r.children.get(1).id, "Second child id should round trip for " + wt);
            assertEquals("b", r.children.get(1).name, "Second child name should round trip for " + wt);
        }
    }

    public static class Child extends SelfDescribingMarshallable {
        int id;
        String name;

        public Child() {
        }

        public Child(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class Parent extends SelfDescribingMarshallable {
        String title = "p";
        final List<Child> children = new ArrayList<>();
        final Map<String, Long> counters = new LinkedHashMap<>();
    }
}
