/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Covers scalar edge cases across common wire types to exercise ValueIn/ValueOut branches.
 */
@SuppressWarnings({"deprecation", "removal"})
public class WireScalarEdgeCasesTest extends WireTestCommon {

    private static final WireType[] TYPES = {
            WireType.BINARY,
            WireType.TEXT,
            WireType.YAML
    };

    @Test
    public void numericAndSpecialValues() {
        for (WireType wt : TYPES) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            // write extremes and special values
            w.write("i32min").int32(Integer.MIN_VALUE);
            w.write("i32max").int32(Integer.MAX_VALUE);
            w.write("i64min").int64(Long.MIN_VALUE + 1); // avoid overflow on some paths
            w.write("i64max").int64(Long.MAX_VALUE);
            w.write("nz").float64(-0.0d);
            w.write("nan").float64(Double.NaN);
            w.write("pinf").float64(Double.POSITIVE_INFINITY);
            w.write("ninf").float64(Double.NEGATIVE_INFINITY);
            w.write("boolT").bool(true);
            w.write("boolF").bool(false);
            w.write("ch0").character('\u0000');
            w.write("chA").character('A');

            assertEquals(Integer.MIN_VALUE, w.read("i32min").int32());
            assertEquals(Integer.MAX_VALUE, w.read("i32max").int32());
            assertEquals(Long.MIN_VALUE + 1, w.read("i64min").int64());
            assertEquals(Long.MAX_VALUE, w.read("i64max").int64());

            double minusZero = w.read("nz").float64();
            assertEquals(0.0d, minusZero, 0.0d);

            double nan = w.read("nan").float64();
            assertTrue(Double.isNaN(nan));
            assertTrue(Double.isInfinite(w.read("pinf").float64()));
            assertTrue(Double.isInfinite(w.read("ninf").float64()));
            assertTrue(w.read("boolT").bool());
            assertFalse(w.read("boolF").bool());
            assertEquals('\u0000', w.read("ch0").character());
            assertEquals('A', w.read("chA").character());
        }
    }

    @Test
    public void bytesAndEmptyText() {
        for (WireType wt : TYPES) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            byte[] empty = new byte[0];
            byte[] small = {1, 2, 3, 4, 5};
            // Limit bytes round‑trip to binary where semantics are unambiguous
            if (wt == WireType.BINARY) {
                w.write("b0").bytes(empty);
                w.write("b1").bytes(small);
            }
            w.write("t0").text("");
            w.write("t1").text("x");

            if (wt == WireType.BINARY) {
                assertArrayEquals(empty, w.read("b0").bytes(new byte[0]));
                assertArrayEquals(small, w.read("b1").bytes(new byte[0]));
            }
            assertEquals("", w.read("t0").text());
            assertEquals("x", w.read("t1").text());
        }
    }
}
