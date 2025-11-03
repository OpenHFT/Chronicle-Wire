/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class WireMarshallerDeepGraphTest extends WireTestCommon {

    public static class Child extends SelfDescribingMarshallable {
        int id;
        String name;
        public Child() {}
        public Child(int id, String name) { this.id = id; this.name = name; }
    }

    public static class Parent extends SelfDescribingMarshallable {
        String title = "p";
        List<Child> children = new ArrayList<>();
        Map<String, Long> counters = new LinkedHashMap<>();
    }

    @Test
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
            assertEquals(p, r);
        }
    }
}

