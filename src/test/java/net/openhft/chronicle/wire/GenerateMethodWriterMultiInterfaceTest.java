/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateMethodWriterMultiInterfaceTest extends WireTestCommon {

    @Test
    @DisplayName("Builds writer supporting multiple interfaces safely")
    public void builderSupportsAdditionalInterfaces() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        @SuppressWarnings("unchecked")
        VanillaMethodWriterBuilder<First> builder = (VanillaMethodWriterBuilder<First>) wire.methodWriterBuilder(First.class);
        builder.addInterface(Second.class);

        First writer = builder.build();
        ((Second) writer).two("b");
        writer.one(1);

        List<String> seen = new ArrayList<>();
        MethodReader reader = wire.methodReader(new First() {
            @Override
            public void one(int v) {
                seen.add("one:" + v);
            }
        }, new Second() {
            @Override
            public void two(String s) {
                seen.add("two:" + s);
            }
        });
        while (reader.readOne()) {
            // drain
            continue;
        }
        assertEquals(2, seen.size(),
                "method reader should capture two events");
        assertTrue(seen.get(0).startsWith("two:"),
                "first event should come from Second.two call");
        assertTrue(seen.get(1).startsWith("one:"),
                "second event should come from First.one call");
    }

    interface First {
        void one(int v);
    }

    interface Second {
        void two(String s);
    }
}
