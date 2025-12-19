/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Test class to validate the serialization behavior of special Unicode characters using Wires.
 * It specifically tests the serialization and deserialization of characters '\uFFFF' and '\uFFFE'.
 * It extends WireTestCommon for utility behaviors related to Wire tests.
 */
public class Issue344Test extends WireTestCommon {

    /**
     * Tests the serialization and deserialization of the character '\uFFFF'.
     */
    @Test
    public void testFFFF() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        char testChar = '\uFFFF';
        Assertions.assertEquals(testChar, roundTrip(testChar), "issue344: roundtrip testChar=0xFFFF");
    }

    /**
     * Tests the serialization and deserialization of the character '\uFFFE'.
     */
    @Test
    public void testFFFE() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        char testChar = '\uFFFE';
        Assertions.assertEquals(testChar, roundTrip(testChar), "issue344: roundtrip testChar=0xFFFE");
    }

    /**
     * Helper method to run the serialization and deserialization test with a given character.
     *
     * @param test The character to be tested for serialization and deserialization.
     */
    private char roundTrip(char test) {
        // Create an instance of SampleData and set its testChar field to the provided character.
        final SampleData data = new SampleData();
        data.testChar = test;

        // Create another instance to store the deserialized data.
        final SampleData copyData = new SampleData();

        // Perform serialization from `data` and deserialization to `copyData`.
        Wires.copyTo(data, copyData);

        return copyData.testChar;
    }

    /**
     * Test data class used for the serialization and deserialization tests.
     * It contains a single field testChar of type char.
     */

    private static class SampleData implements Marshallable {
        char testChar;
    }
}
