package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesMarshallable;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DeserializeFromNakedFileTest {
    @Test(expected = IllegalArgumentException.class)
    public void testPOJO() throws IOException {
        PlainOldJavaClass res = Marshallable.fromFile(PlainOldJavaClass.class, "naked.yaml");

        assertEquals(20, res.heartBtInt);
    }

    @Test
    public void testSelfDescribing() throws IOException {
        SelfDescribingClass res = Marshallable.fromFile(SelfDescribingClass.class, "naked.yaml");

        assertEquals(20, res.heartBtInt);
    }

    @Test
    public void testBytes() throws IOException {
        BytesClass res = Marshallable.fromFile(BytesClass.class, "naked.yaml");

        // The result of parsing first 4 bytes as integer value
        assertEquals(0x72616548, res.heartBtInt);
    }

    private static class PlainOldJavaClass {
        public int heartBtInt;
    }

    private static class SelfDescribingClass extends SelfDescribingMarshallable {
        public int heartBtInt;
    }

    private static class BytesClass implements BytesMarshallable {
        public int heartBtInt;
    }
}
