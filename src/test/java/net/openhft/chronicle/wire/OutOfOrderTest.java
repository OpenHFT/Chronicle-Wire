package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class OutOfOrderTest {
    static final String start = "{ \"a\": 1, ";
    static final String records = "\"records\":[{\"id\":1}], ";
    static final String missing = "\"missing\": 111, ";
    static final String end = "\"z\": 99 }";

    @Test
    public void outOfOrder() {
        doTest(start + end, "\"a\":1,\"b\":null,\"records\":null,\"z\":99");
        doTest(start + missing + records + end, "\"a\":1,\"b\":null,\"records\":[ {\"id\":1} ], \"z\":99");
    }

    void doTest(String input, String expected) {
        Bytes<?> from = Bytes.from(input);
        JSONWire wire = new JSONWire(from);
        OOOT ooot = wire.getValueIn().object(OOOT.class);
        from.release();

        JSONWire wire2 = new JSONWire(Bytes.elasticHeapByteBuffer(64));
        wire2.getValueOut().object(ooot);
        assertEquals(expected, wire2.toString());
    }

    static class OOOT extends SelfDescribingMarshallable {
        int a;
        String b;
        List<OOOT2> records;
        int z;
    }

    static class OOOT2 extends SelfDescribingMarshallable {
        int id;
    }
}
