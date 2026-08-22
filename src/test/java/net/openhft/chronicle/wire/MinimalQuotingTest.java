/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MinimalQuotingTest extends WireTestCommon {

    private static final Object[][] CASES = {
            {"hello", "hello"},
            {"1st", "1st"},
            {"123abc", "123abc"},
            {"3D-model2", "3D-model2"},
            {"1234", "\"1234\""},
            {"true", "\"true\""},
            {"false", "\"false\""},
            {"null", "\"null\""},
            {"2026-08-22", "\"2026-08-22\""}
    };

    @Test
    public void minimalQuotingHasExactOutputAndPreservesDynamicStringType() {
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.YAML}) {
            for (Object[] testCase : CASES)
                assertMinimalQuoting(wireType, (String) testCase[0], (String) testCase[1]);
        }
    }

    @Test
    public void existingLeadingDigitOutputRemainsTheDefault() {
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.YAML}) {
            final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            try {
                wireType.apply(bytes).getValueOut().text("1st");
                assertEquals(wireType + " default output", "\"1st\"\n", bytes.toString());
            } finally {
                bytes.releaseLast();
            }
        }
    }

    private static void assertMinimalQuoting(WireType wireType, String value, String expectedText) {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            final Wire wire = wireType.apply(bytes);
            ((YamlWireOut<?>) wire).useMinimalQuoting(true);
            wire.getValueOut().text(value);

            assertEquals(wireType + " output for " + value, expectedText + "\n", bytes.toString());
            final Object read = wire.getValueIn().object(Object.class);
            assertEquals(wireType + " dynamic type for " + value, String.class, read.getClass());
            assertEquals(wireType + " value for " + value, value, read);
        } finally {
            bytes.releaseLast();
        }
    }
}
