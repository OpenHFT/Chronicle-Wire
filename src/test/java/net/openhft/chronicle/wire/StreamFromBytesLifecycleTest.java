/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;


import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import java.util.stream.Stream;
import static org.junit.Assert.*;

public class StreamFromBytesLifecycleTest extends WireTestCommon {
    public static class Item extends SelfDescribingMarshallable {
        String name;
        int value;
    }
    private Bytes<?> documents() { return Bytes.from("{ name: first, value: 1 }\n---\n{ name: second, value: 2 }\n"); }
    private void cleanup(Bytes<?> b) { if (b.refCount() > 0) b.releaseLast(); }
    @Test public void closingUnconsumedStreamReleasesBytes() {
        Bytes<?> b = documents();
        try { WireType.TEXT.streamFromBytes(Item.class, b).close(); assertEquals(0, b.refCount()); }
        finally { cleanup(b); }
    }
    @Test public void findFirstThenCloseReleasesBytes() {
        Bytes<?> b = documents();
        try {
            try (Stream<Item> s = WireType.TEXT.streamFromBytes(Item.class, b)) { assertEquals("first", s.findFirst().get().name); }
            assertEquals(0, b.refCount());
        } finally { cleanup(b); }
    }
    @Test public void limitThenCloseReleasesBytes() {
        Bytes<?> b = documents();
        try {
            try (Stream<Item> s = WireType.TEXT.streamFromBytes(Item.class, b)) { assertEquals(1, s.limit(1).count()); }
            assertEquals(0, b.refCount());
        } finally { cleanup(b); }
    }
    @Test public void throwingConsumerThenCloseReleasesBytes() {
        Bytes<?> b = documents(); RuntimeException failure = new IllegalStateException("consumer failed");
        try {
            try (Stream<Item> s = WireType.TEXT.streamFromBytes(Item.class, b)) {
                assertSame(failure, assertThrows(IllegalStateException.class, () -> s.forEach(x -> { throw failure; })));
            }
            assertEquals(0, b.refCount());
        } finally { cleanup(b); }
    }
    @Test public void repeatedCloseDoesNotDoubleRelease() {
        Bytes<?> b = documents();
        try {
            Stream<Item> s = WireType.TEXT.streamFromBytes(Item.class, b); s.close(); s.close();
            assertEquals(0, b.refCount());
        } finally { cleanup(b); }
    }
    @Test public void fullConsumptionThenCloseReleasesExactlyOnce() {
        Bytes<?> b = documents();
        try {
            try (Stream<Item> s = WireType.TEXT.streamFromBytes(Item.class, b)) { assertEquals(3, s.mapToInt(x -> x.value).sum()); }
            assertEquals(0, b.refCount());
        } finally { cleanup(b); }
    }
}
