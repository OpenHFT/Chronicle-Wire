package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Mocker;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

interface MockMethods {
    void method1(MockDto dto);

    void method2(MockDto dto);
}

/**
 * Created by peter on 17/05/2017.
 */
public class MethodReaderTest {
    @Test
    public void readMethods() throws IOException {
        Wire wire = new TextWire(BytesUtil.readFile("methods-in.yaml")).useTextDocuments();
        Wire wire2 = new TextWire(Bytes.allocateElasticDirect());
        // expected
        Bytes expected = BytesUtil.readFile("methods-in.yaml");
        MockMethods writer = wire2.methodWriter(MockMethods.class);
        MethodReader reader = wire.methodReader(writer);
        for (int i = 0; i < 2; i++) {
            assertTrue(reader.readOne());
            while (wire2.bytes().peekUnsignedByte(wire2.bytes().writePosition() - 1) == ' ')
                wire2.bytes().writeSkip(-1);
        }
        assertFalse(reader.readOne());
        assertEquals(expected.toString().trim().replace("\r", ""), wire2.toString().trim());
    }

    @Test
    public void testSubclasses() {
        Wire wire = new TextWire(Bytes.elasticHeapByteBuffer(256));
        MRTListener writer = wire.methodWriter(MRTListener.class);
        writer.top(new MRT1("one"));
        writer.top(new MRT2("one", "two"));
        writer.mid(new MRT1("1"));
        writer.mid(new MRT2("1", "2"));

        StringWriter sw = new StringWriter();
        MethodReader reader = wire.methodReader(Mocker.logging(MRTListener.class, "subs ", sw));
        for (int i = 0; i < 4; i++) {
            assertTrue(reader.readOne());
        }
        assertFalse(reader.readOne());
        String expected = "subs top[!net.openhft.chronicle.wire.MethodReaderTest$MRT1 {\n" +
                "  field1: one\n" +
                "}\n" +
                "]\n" +
                "subs top[!net.openhft.chronicle.wire.MethodReaderTest$MRT2 {\n" +
                "  field1: one,\n" +
                "  field2: two\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.MethodReaderTest$MRT1 {\n" +
                "  field1: \"1\"\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.MethodReaderTest$MRT2 {\n" +
                "  field1: \"1\",\n" +
                "  field2: \"2\"\n" +
                "}\n" +
                "]\n";
        String actual = sw.toString().replace("\r", "");
        assertEquals(expected, actual);
    }

    interface MRTListener {
        void top(MRTInterface mrti);

        void mid(MRT1 mrt1);
    }

    interface MRTInterface {

    }

    static class MRT1 extends AbstractMarshallable implements MRTInterface {
        final String field1;

        MRT1(String field1) {
            this.field1 = field1;
        }
    }

    static class MRT2 extends MRT1 {
        final String field2;

        MRT2(String field1, String field2) {
            super(field1);
            this.field2 = field2;
        }
    }
}

class MockDto extends AbstractMarshallable {
    String field1;
    double field2;
}
