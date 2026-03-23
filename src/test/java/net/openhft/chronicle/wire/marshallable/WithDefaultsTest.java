/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the WithDefaults functionality.
 * Extends the WireTestCommon to leverage utilities related to wire tests.
 */
public class WithDefaultsTest extends WireTestCommon {

    /**
     * Tests the writeMarshallable functionality of WithDefaults under various scenarios.
     */
    @Test
    public void writeMarshallable() {
        // Default test without any modification
        doTest(w -> {
        });

        // Test the scenario after clearing the bytes data
        doTest(w -> w.bytes.clear());

        // Test with changing the default text value
        doTest(w -> w.text = "bye");

        // Test with changing the flag value to false
        doTest(w -> w.flag = false);

        // Test with changing the default numerical value
        doTest(w -> w.num = 5);
    }

    /**
     * Utility function to perform tests on WithDefaults.
     * Initializes an instance, applies the consumer action, and then validates the string
     * representation and object equality.
     *
     * @param consumer Consumer action to apply on the WithDefaults instance.
     */
    private void doTest(Consumer<WithDefaults> consumer) {
        // Initialize the WithDefaults instance
        WithDefaults wd = new WithDefaults();

        // Apply the consumer action on the instance
        consumer.accept(wd);

        // Convert the instance to its string representation
        String cs = wd.toString();

        // Convert the string representation back to a WithDefaults object
        WithDefaults o = Marshallable.fromString(cs);

        // Validate the string representation remains consistent
        assertEquals(cs, o.toString());

        // Validate the original and recreated objects are equal
        assertEquals(wd, o);
    }
}
