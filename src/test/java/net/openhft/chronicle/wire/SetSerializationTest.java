package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

public class SetSerializationTest {

    public static class Dto extends SelfDescribingMarshallable {
        public Map<String, Set<String>> value;
    }

    @Test
    public void test() {
        Object o = Marshallable.fromString("!Dto {\n" +
                "  a: !!set [\n" +
                "    one,\n" +
                "    two,\n" +
                "    three\n" +
                "  ]\n," +
                "  b: !!set [\n" +
                "    one,\n" +
                "    two,\n" +
                "    three\n" +
                "  ]\n" +
                "}");
        Assert.assertNotNull(o);
        HexDumpBytes hexDumpBytes = new HexDumpBytes();
        Wire w = new BinaryWire(hexDumpBytes);
        w.getValueOut().object(o);
   //     System.out.println(hexDumpBytes.toHexString());
        Assert.assertNotNull(w.getValueOut().object(o));
    }
}
