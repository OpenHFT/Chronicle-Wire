/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// This test class focuses on TextWire's ability to handle arrays of various primitive and object types.
public class TextWithArraysTest extends WireTestCommon {

    // Test the behavior of TextWire with arrays of different types.
    @Test
    @DisplayName("Serialises arrays of primitives and objects in text wire")
    public void testWithArrays() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip text array test");

        // Check the string representation of an uninitialized WithArrays object
        assertEquals("!net.openhft.chronicle.wire.TextWithArraysTest$WithArrays {\n" +
                "  booleans: !!null \"\",\n" +
                "  bytes: !!null \"\",\n" +
                "  shorts: !!null \"\",\n" +
                "  chars: !!null \"\",\n" +
                "  ints: !!null \"\",\n" +
                "  longs: !!null \"\",\n" +
                "  floats: !!null \"\",\n" +
                "  doubles: !!null \"\",\n" +
                "  words: !!null \"\"\n" +
                "}\n", new WithArrays().toString(),
                "empty arrays should serialise as nulls in text wire");

        // Initialize the arrays with sample values
        WithArrays wa = new WithArrays();
        wa.booleans = new boolean[]{true, false};
        wa.bytes = new byte[]{-1, 0, 1};
        wa.shorts = new short[]{-1, 0, 1};
        wa.chars = "Hello".toCharArray();
        wa.ints = new int[]{-1, 0, 1};
        wa.longs = new long[]{-1L, 0L, 1L};
        wa.floats = new float[]{-1.0f, 0.0f, 1.0f};
        wa.doubles = new double[]{-1.0d, 0.0d, 1.0d};
        wa.words = "Hello World Bye for now".split(" ");

        // Validate the string representation of the initialized object
        assertEquals("!net.openhft.chronicle.wire.TextWithArraysTest$WithArrays {\n" +
                "  booleans: [ true, false ],\n" +
                "  bytes: !!binary /wAB,\n" +
                "  shorts: [ -1, 0, 1 ],\n" +
                "  chars: [ H, e, l, l, o ],\n" +
                "  ints: [ -1, 0, 1 ],\n" +
                "  longs: [ -1, 0, 1 ],\n" +
                "  floats: [ -1.0, 0.0, 1.0 ],\n" +
                "  doubles: [ -1.0, 0.0, 1.0 ],\n" +
                "  words: [ Hello, World, Bye, for, now ]\n" +
                "}\n", wa.toString(),
                "populated arrays should serialise with expected values");

    }

    // Inner class that holds arrays of different data types for testing.
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class WithArrays extends SelfDescribingMarshallable {
        boolean[] booleans;
        byte[] bytes;
        short[] shorts;
        char[] chars;
        int[] ints;
        long[] longs;
        float[] floats;
        double[] doubles;
        String[] words;
    }
}
