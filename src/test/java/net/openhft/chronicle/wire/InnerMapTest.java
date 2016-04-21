package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Rob Austin.
 */
public class InnerMapTest {

    static class MyMarshable extends AbstractMarshallable {


        String name;

        Map<String, Double> commission;


        @UsedViaReflection
        public MyMarshable(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        public MyMarshable() {
            this.commission = new LinkedHashMap<>();
        }

        public String name() {
            return name;
        }

        public Map<String, Double> commission() {
            return commission;
        }

        public MyMarshable name(String name) {
            this.name = name;
            return this;
        }
    }


    @Test
    public void testMyInnnerMap() {
        MyMarshable myMarshable = new MyMarshable().name("rob");
        myMarshable.commission().put("hello", 123.4);

        String asString = myMarshable.toString();
        Assert.assertEquals("!net.openhft.chronicle.wire.InnerMapTest$MyMarshable {\n" +
                "  name: rob,\n" +
                "  commission: {\n" +
                "    hello: 123.4\n" +
                "  }\n" +
                "}\n", asString);


        Bytes b = Bytes.elasticByteBuffer();
        Wire w = new BinaryWire(b);     // works with text fails with binary

        try (DocumentContext dc = w.writingDocument(false)) {
            dc.wire().write("marshable").typedMarshallable(myMarshable);
        }


        try (DocumentContext dc = w.readingDocument()) {
            MyMarshable tm = dc.wire().read(() -> "marshable").typedMarshallable();
            Assert.assertEquals(asString, tm.toString());
        }

    }
}
