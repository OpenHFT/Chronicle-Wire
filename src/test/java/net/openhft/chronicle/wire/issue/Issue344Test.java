/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assume.assumeFalse;

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

        runWith('\uFFFF');
    }

    /**
     * Tests the serialization and deserialization of the character '\uFFFE'.
     */
    @Test
    public void testFFFE() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        runWith('\uFFFE');
    }

    /**
     * Helper method to run the serialization and deserialization test with a given character.
     *
     * @param test The character to be tested for serialization and deserialization.
     */
    private void runWith(char test) {
        // Create an instance of TestData and set its testChar field to the provided character.
        final TestData data = new TestData();
        data.testChar = test;

        // Create another instance to store the deserialized data.
        final TestData copyData = new TestData();

        // Perform serialization from `data` and deserialization to `copyData`.
        Wires.copyTo(data, copyData);

        // Assert that the character in the deserialized data matches the original character.
        Assert.assertEquals(data.testChar, copyData.testChar);
    }

    /**
     * Test data class used for the serialization and deserialization tests.
     * It contains a single field testChar of type char.
     */
    private static class TestData implements Marshallable {
        char testChar;
    }
}
