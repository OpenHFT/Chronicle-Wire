/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.internal.stream.StreamsUtil;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ExcerptIteratorContractTest extends WireTestCommon {

    private static final class Source implements AutoCloseable {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        final Wire wire = new BinaryWire(bytes);

        Source(double... values) {
            for (double value : values)
                wire.writeDocument(false, w -> w.write("v").float64(value));
        }

        PrimitiveIterator.OfDouble doubles() {
            return new StreamsUtil.ExcerptIteratorOfDouble(wire, (w, i) -> w.read("v").float64());
        }

        @Override
        public void close() {
            bytes.releaseLast();
        }
    }

    @Test
    public void emptyDoubleIteratorIsExhausted() {
        try (Source s = new Source()) {
            assertFalse(s.doubles().hasNext());
        }
    }

    @Test
    public void emptyDoubleNextThrows() {
        try (Source s = new Source()) {
            PrimitiveIterator.OfDouble iterator = s.doubles();
            assertThrows(NoSuchElementException.class, iterator::nextDouble);
        }
    }

    @Test
    public void doubleNextReadsFirstDocument() {
        try (Source s = new Source(12.5)) {
            assertEquals(12.5, s.doubles().nextDouble(), 0);
        }
    }

    @Test
    public void doubleHasNextCachesWithoutSkipping() {
        try (Source s = new Source(12.5, 25)) {
            AtomicInteger reads = new AtomicInteger();
            PrimitiveIterator.OfDouble it = new StreamsUtil.ExcerptIteratorOfDouble(s.wire, (w, i) -> {
                reads.incrementAndGet();
                return w.read("v").float64();
            });
            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
            assertEquals(1, reads.get());
            assertEquals(12.5, it.nextDouble(), 0);
            assertEquals(25, it.nextDouble(), 0);
        }
    }

    @Test
    public void doubleIterationEndsAfterLastDocument() {
        try (Source s = new Source(1, 2)) {
            PrimitiveIterator.OfDouble it = s.doubles();
            assertEquals(1, it.nextDouble(), 0);
            assertEquals(2, it.nextDouble(), 0);
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::nextDouble);
        }
    }

    @Test
    public void doubleNaNSentinelSkipsDocument() {
        try (Source s = new Source(Double.NaN, 3)) {
            PrimitiveIterator.OfDouble it = s.doubles();
            assertEquals(3, it.nextDouble(), 0);
            assertFalse(it.hasNext());
        }
    }

    @Test
    public void doubleExtractorFailureClosesDocument() {
        try (Source s = new Source(1, 2)) {
            AtomicInteger calls = new AtomicInteger();
            PrimitiveIterator.OfDouble it = new StreamsUtil.ExcerptIteratorOfDouble(s.wire, (w,i) -> {
                if (calls.getAndIncrement() == 0)
                    throw new IllegalArgumentException("probe");
                return w.read("v").float64();
            });
            assertThrows(IllegalArgumentException.class, it::hasNext);
            assertEquals(2, it.nextDouble(), 0);
            assertFalse(it.hasNext());
        }
    }

    @Test
    public void doubleIteratorPreservesSpecialValues() {
        try (Source s = new Source(1, 2, 3)) {
            double[] values = {-0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
            AtomicInteger calls = new AtomicInteger();
            PrimitiveIterator.OfDouble it = new StreamsUtil.ExcerptIteratorOfDouble(s.wire, (w, i) -> {
                w.read("v").float64();
                return values[calls.getAndIncrement()];
            });
            assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(it.nextDouble()));
            assertEquals(Double.POSITIVE_INFINITY, it.nextDouble(), 0);
            assertEquals(Double.NEGATIVE_INFINITY, it.nextDouble(), 0);
        }
    }

    @Test
    public void objectNullSentinelSkipsDocument() {
        try (Source s = new Source(1, 2)) {
            Iterator<String> it = new StreamsUtil.ExcerptIterator<>(s.wire, (w,i) -> w.read("v").float64() == 1 ? null : "two");
            assertEquals("two", it.next());
            assertFalse(it.hasNext());
        }
    }

    @Test
    public void objectExtractorFailureClosesDocument() {
        try (Source s = new Source(1, 2)) {
            Iterator<String> it = new StreamsUtil.ExcerptIterator<>(s.wire, (w,i) -> {
                if (w.read("v").float64() == 1)
                    throw new IllegalArgumentException("probe");
                return "two";
            });
            assertThrows(IllegalArgumentException.class, it::hasNext);
            assertEquals("two", it.next());
            assertFalse(it.hasNext());
        }
    }

    @Test
    public void longSentinelSkipsDocument() {
        try (Source s = new Source(1, 2)) {
            PrimitiveIterator.OfLong it = new StreamsUtil.ExcerptIteratorOfLong(s.wire, (w,i) -> w.read("v").float64() == 1 ? Long.MIN_VALUE : 2);
            assertEquals(2, it.nextLong());
            assertFalse(it.hasNext());
        }
    }

    @Test
    public void longHasNextCachesWithoutSkipping() {
        try (Source s = new Source(1, 2)) {
            PrimitiveIterator.OfLong it = new StreamsUtil.ExcerptIteratorOfLong(s.wire, (w,i) -> (long) w.read("v").float64());
            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
            assertEquals(1, it.nextLong());
            assertEquals(2, it.nextLong());
        }
    }
}
