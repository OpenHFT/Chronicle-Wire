/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MethodWriterReaderDispatchTest extends WireTestCommon {

    interface Api {
        void a(int i);
        void b(String s);
    }

    @Test
    public void dispatchKnownMethodsAndIgnoreUnknown() {
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));
        Api writer = w.methodWriter(Api.class);
        writer.a(1);
        writer.b("two");

        // inject an unknown method name to exercise the warning path
        w.writeEventName("unknown").int32(99);

        List<String> seen = new ArrayList<>();
        MethodReader r = w.methodReader(new Api() {
            @Override public void a(int i) { seen.add("a:" + i); }
            @Override public void b(String s) { seen.add("b:" + s); }
        });
        while (r.readOne()) {
            // drain
        }
        assertEquals(2, seen.size());
        assertTrue(seen.get(0).startsWith("a:"));
        assertTrue(seen.get(1).startsWith("b:"));
    }
}

