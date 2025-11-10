//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

// This test class focuses on TextWire's ability to handle arrays of various primitive and object types.
public class TextWithArraysTest extends WireTestCommon {

    // Test the behavior of TextWire with arrays of different types.
    @Test
    public void testWithArrays() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

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
                "}\n", new WithArrays().toString());

        // Initialize the arrays with sample values
        WithArrays wa = new WithArrays();
        wa.booleans = new boolean[]{true, false};
        wa.bytes = new byte[]{-1, 0, 1};
        wa.shorts = new short[]{-1, 0, 1};
        wa.chars = "Hello".toCharArray();
        wa.ints = new int[]{-1, 0, 1};
        wa.longs = new long[]{-1, 0, 1};
        wa.floats = new float[]{-1, 0, 1};
        wa.doubles = new double[]{-1, 0, 1};
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
                "}\n", wa.toString());

    }

    // Inner class that holds arrays of different data types for testing.
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
