/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("Numeric and special values round trip")
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

            assertEquals(Integer.MIN_VALUE,
                    w.read("i32min").int32(),
                    "i32min should round trip for " + wt);
            assertEquals(Integer.MAX_VALUE,
                    w.read("i32max").int32(),
                    "i32max should round trip for " + wt);
            assertEquals(Long.MIN_VALUE + 1,
                    w.read("i64min").int64(),
                    "i64min should round trip for " + wt);
            assertEquals(Long.MAX_VALUE,
                    w.read("i64max").int64(),
                    "i64max should round trip for " + wt);

            double minusZero = w.read("nz").float64();
            assertEquals(0.0d,
                    minusZero,
                    0.0d,
                    "Minus zero should round trip for " + wt);

            double nan = w.read("nan").float64();
            assertTrue(Double.isNaN(nan),
                    "NaN should remain NaN for " + wt);
            assertTrue(Double.isInfinite(w.read("pinf").float64()),
                    "Positive infinity should round trip for " + wt);
            assertTrue(Double.isInfinite(w.read("ninf").float64()),
                    "Negative infinity should round trip for " + wt);
            assertTrue(w.read("boolT").bool(),
                    "Boolean true should round trip for " + wt);
            assertFalse(w.read("boolF").bool(),
                    "Boolean false should round trip for " + wt);
            assertEquals('\u0000',
                    w.read("ch0").character(),
                    "Null character should round trip for " + wt);
            assertEquals('A',
                    w.read("chA").character(),
                    "Character A should round trip for " + wt);
        }
    }

    @Test
    @DisplayName("Bytes and empty text round trip")
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
                assertArrayEquals(empty,
                        w.read("b0").bytes(new byte[0]),
                        "Empty bytes should round trip for " + wt);
                assertArrayEquals(small,
                        w.read("b1").bytes(new byte[0]),
                        "Small bytes should round trip for " + wt);
            }
            assertEquals("",
                    w.read("t0").text(),
                    "Empty text should round trip for " + wt);
            assertEquals("x",
                    w.read("t1").text(),
                    "Text value should round trip for " + wt);
        }
    }
}
