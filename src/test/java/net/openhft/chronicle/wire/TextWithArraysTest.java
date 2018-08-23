package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
 * Created by peter.lawrey@chronicle.software on 26/07/2017
 */
public class TextWithArraysTest {
    @Test
    public void testWithArrays() {
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
        assertEquals("!net.openhft.chronicle.wire.TextWithArraysTest$WithArrays {\n" +
                "  booleans: [ true, false ],\n" +
                "  bytes: [ -1, 0, 1 ],\n" +
                "  shorts: [ -1, 0, 1 ],\n" +
                "  chars: [ H, e, l, l, o ],\n" +
                "  ints: [ -1, 0, 1 ],\n" +
                "  longs: [ -1, 0, 1 ],\n" +
                "  floats: [ -1.0, 0.0, 1.0 ],\n" +
                "  doubles: [ -1.0, 0.0, 1.0 ],\n" +
                "  words: [ Hello, World, Bye, for, now ]\n" +
                "}\n", wa.toString());

    }

    static class WithArrays extends AbstractMarshallable {
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
