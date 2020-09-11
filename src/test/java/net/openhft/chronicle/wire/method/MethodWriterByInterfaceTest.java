package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.MicroTimestampLongConverter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.TextWire;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MethodWriterByInterfaceTest {
    @Before
    public void setup() {
        ObjectUtils.defaultObjectForInterface(c -> Class.forName(c.getName() + "mpl"));
    }

    @After
    public void teardown() {
        ObjectUtils.defaultObjectForInterface(c -> c);
    }

    @Test
    public void writeReadViaImplementation() {
        TextWire tw = new TextWire(Bytes.allocateElasticOnHeap());
        MWBI0 mwbi0 = tw.methodWriter(MWBI0.class);
        mwbi0.method(new MWBImpl("name", 1234567890123456L));
        assertEquals("method: {\n" +
                "  name: name,\n" +
                "  time: 2009-02-13T23:31:30.123456\n" +
                "}\n" +
                "...\n", tw.toString());
        StringWriter sw = new StringWriter();
        MethodReader reader = tw.methodReader(Mocker.logging(MWBI0.class, "", sw));
        assertTrue(reader.readOne());
        assertEquals("method[!net.openhft.chronicle.wire.method.MethodWriterByInterfaceTest$MWBImpl {\n" +
                "  name: name,\n" +
                "  time: 2009-02-13T23:31:30.123456\n" +
                "}\n" +
                "]\n", sw.toString());
    }

    interface MWBI {
        String name();

        long time();
    }

    interface MWBI0 {
        void method(MWBI mwbi);
    }

    public static class MWBImpl extends SelfDescribingMarshallable implements MWBI {
        String name;
        @LongConversion(MicroTimestampLongConverter.class)
        long time;

        public MWBImpl(String name, long time) {
            this.name = name;
            this.time = time;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long time() {
            return time;
        }
    }
}
