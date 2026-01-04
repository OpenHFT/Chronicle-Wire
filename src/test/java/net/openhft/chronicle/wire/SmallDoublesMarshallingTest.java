/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// A test class to ensure small double values are marshaled and unmarshaled correctly using
// Chronicle-Wire.
// See: https://github.com/OpenHFT/Chronicle-Wire/issues/240
class SmallDoublesMarshallingTest extends WireTestCommon {

    // An example class containing a single double value to be marshaled and unmarshaled.
    public static class Example extends SelfDescribingMarshallable {
        private double doubleVal;

        double doubleVal() {
            return doubleVal;
        }

        Example doubleVal(double doubleVal) {
            this.doubleVal = doubleVal;
            return this;
        }
    }

    // A test to ensure that a specific small double value is marshaled and unmarshaled correctly.
    @Test
    @DisplayName("Marshals and unmarshals small double value in text")
    void marshallingTest() {
        final Example example = new Example().doubleVal(1.104326320059551E-14);
        final String textRepr = example.toString();
        final Example demarshalled = WireType.TEXT.fromString(Example.class, textRepr);

        Assertions.assertTrue(textRepr.contains("1.104326320059551E-14"),
                "text representation should contain 1.104326320059551E-14, actual=" + textRepr);
        Assertions.assertEquals(example.doubleVal(), demarshalled.doubleVal(), 1e-14,
                "demarshalled value should match original within tolerance, actual=" + demarshalled.doubleVal());
    }
}
