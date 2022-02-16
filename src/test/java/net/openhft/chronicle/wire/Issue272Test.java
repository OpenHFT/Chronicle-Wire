package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class Issue272Test {
    @Test
    public void test() {
        Bytes buffer = Bytes.allocateElasticDirect(10 * 4);
        int[] one = {1, 2, 3, 4, 5};
        long[] two = {101, 102, 103, 104, 105};
        double[] three = {1.01, 1.02, 1.03, 1.04, 1.05};

        // Create wire for saving two int arrays
        Wire wire = new YamlWire(buffer);
        wire.write("one").array(one, 5);
        wire.write("two").array(two, 5);
        wire.write("three").array(three, 5);

        assertEquals("" +
                        "one: [ 1, 2, 3, 4, 5 ],\n" +
                        "two: [ 101, 102, 103, 104, 105 ],\n" +
                        "three: [ 1.01, 1.02, 1.03, 1.04, 1.05 ]",
                wire.toString());

        int[] ints = new int[5];
        int amtRead = wire.read("one").array(ints);
        assertEquals(Arrays.toString(one), Arrays.toString(ints));
        assertEquals(amtRead, one.length);

        int[] longs = new int[5];
        int amtRead2 = wire.read("two").array(longs);
        assertEquals(Arrays.toString(two), Arrays.toString(longs));
        assertEquals(amtRead2, two.length);

        double[] doubles = new double[5];
        int amtRead3 = wire.read("three").array(doubles);
        assertEquals(Arrays.toString(three), Arrays.toString(doubles));
        assertEquals(amtRead3, three.length);
    }
}
