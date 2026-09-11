/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ScalarTypePreservationTest extends WireTestCommon {

    @Test
    public void dotPrefixedNumericTextRetainsType() {
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.YAML_ONLY}) {
            assertScalar(wireType, ".3", String.class, out -> out.text(".3"));
            assertScalar(wireType, 0.3, Double.class, out -> out.float64(0.3));
        }
    }

    private static void assertScalar(@NotNull WireType wireType, Object expected, @NotNull Class<?> expectedType,
                                     @NotNull Consumer<ValueOut> writer) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            writer.accept(wireType.apply(bytes).getValueOut());
            assertValueAndType(expected, expectedType, wireType.apply(bytes).getValueIn().object());
        } finally {
            bytes.releaseLast();
        }
    }

    private static void assertValueAndType(Object expected, @NotNull Class<?> expectedType, Object actual) {
        assertEquals(expected, actual);
        assertSame(expectedType, actual.getClass());
    }
}
