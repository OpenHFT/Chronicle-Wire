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
    public void textAndTypedScalarsRetainTypes() {
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.YAML_ONLY}) {
            assertScalar(wireType, ".3", String.class, out -> out.text(".3"));
            assertScalar(wireType, 0.3, Double.class, out -> out.float64(0.3));
            assertScalar(wireType, "true", String.class, out -> out.text("true"));
            assertScalar(wireType, true, Boolean.class, out -> out.bool(true));
            assertScalar(wireType, "false", String.class, out -> out.text("false"));
            assertScalar(wireType, false, Boolean.class, out -> out.bool(false));
        }
    }

    @Test
    public void quotedJsonBooleansRemainStrings() {
        assertJsonString("true");
        assertJsonString("false");
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

    private static void assertJsonString(@NotNull String expected) {
        Bytes<?> bytes = Bytes.from('"' + expected + '"');
        try {
            assertValueAndType(expected, String.class, new JSONWire(bytes).getValueIn().object());
        } finally {
            bytes.releaseLast();
        }
    }

    private static void assertValueAndType(Object expected, @NotNull Class<?> expectedType, Object actual) {
        assertEquals(expected, actual);
        assertSame(expectedType, actual.getClass());
    }
}
