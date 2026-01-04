/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Test class to validate the serialisation behaviour of special Unicode characters using Wires.
 * It specifically tests the serialisation and deserialisation of characters '\uFFFF' and '\uFFFE'.
 * It extends WireTestCommon for utility behaviours related to wire tests.
 */
class Issue344Test extends WireTestCommon {

    /**
     * Tests the serialisation and deserialisation of the character '\uFFFF'.
     */
    @Test
    @DisplayName("Wire should round-trip U+FFFF character value")
    void testFFFF() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for U+FFFF round-trip test");

        char testChar = '\uFFFF';
        Assertions.assertEquals(testChar, roundTrip(testChar), "U+FFFF should round-trip through wire");
    }

    /**
     * Tests the serialisation and deserialisation of the character '\uFFFE'.
     */
    @Test
    @DisplayName("Wire should round-trip U+FFFE character value")
    void testFFFE() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for U+FFFE round-trip test");

        char testChar = '\uFFFE';
        Assertions.assertEquals(testChar, roundTrip(testChar), "U+FFFE should round-trip through wire");
    }

    /**
     * Helper method to run the serialisation and deserialisation test with a given character.
     *
     * @param test The character to be tested for serialisation and deserialisation.
     */
    private char roundTrip(char test) {
        // Create an instance of SampleData and set its testChar field to the provided character.
        final SampleData data = new SampleData();
        data.testChar = test;

        // Create another instance to store the deserialized data.
        final SampleData copyData = new SampleData();

        // Perform serialisation from `data` and deserialisation to `copyData`.
        Wires.copyTo(data, copyData);

        return copyData.testChar;
    }

    /**
     * Test data class used for the serialisation and deserialisation tests.
     * It contains a single field testChar of type char.
     */

    private static class SampleData implements Marshallable {
        char testChar;
    }
}
