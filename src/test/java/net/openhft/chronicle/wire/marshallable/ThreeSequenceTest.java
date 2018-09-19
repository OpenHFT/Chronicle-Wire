package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 16/05/2017.
 */
public class ThreeSequenceTest {
    @Test
    public void testThree() {
        ThreeSequence ts = Marshallable.fromString("!" + ThreeSequence.class.getName() + " {\n" +
                "  a: [\n" +
                "    { price: 1.1, qty: 2.0 },\n" +
                "    { price: 1.2, qty: 1.0 }\n" +
                "  ]," +
                "  b: [\n" +
                "    { price: 2.1, qty: 2.0 },\n" +
                "    { price: 2.2, qty: 1.0 }\n" +
                "  ]," +
                "  c: [\n" +
                "    { price: 3.1, qty: 2.0 },\n" +
                "    { price: 3.2, qty: 1.0 }\n" +
                "  ],\n" +
                "  text: hello\n" +
                "}\n");
        assertEquals("!net.openhft.chronicle.wire.marshallable.ThreeSequence {\n" +
                "  a: [\n" +
                "    { price: 1.1, qty: 2.0 },\n" +
                "    { price: 1.2, qty: 1.0 }\n" +
                "  ],\n" +
                "  b: [\n" +
                "    { price: 2.1, qty: 2.0 },\n" +
                "    { price: 2.2, qty: 1.0 }\n" +
                "  ],\n" +
                "  c: [\n" +
                "    { price: 3.1, qty: 2.0 },\n" +
                "    { price: 3.2, qty: 1.0 }\n" +
                "  ],\n" +
                "  text: hello\n" +
                "}\n", ts.toString());
        assertEquals(ts, Marshallable.fromString(ts.toString()));
    }
}
