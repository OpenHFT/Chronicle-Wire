package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class MethodWriterByInterfaceTest extends WireTestCommon {
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
        checkWriteReadViaImplementation();
    }

    @Test
    public void writeReadViaImplementationGenerateTuples() {
        Wires.GENERATE_TUPLES = true;
        checkWriteReadViaImplementation();
    }

    private void checkWriteReadViaImplementation() {
        Wire tw = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        MWBI0 mwbi0 = tw.methodWriter(MWBI0.class);
        mwbi0.method(new MWBImpl("name", 1234567890123456L));
        assertFalse(Proxy.isProxyClass(mwbi0.getClass()));
        assertEquals("method: {\n" +
                "  name: name,\n" +
                "  time: 2009-02-13T23:31:30.123456\n" +
                "}\n" +
                "...\n", tw.toString());
        StringWriter sw = new StringWriter();
        MethodReader reader = tw.methodReader(Mocker.logging(MWBI0.class, "", sw));
        assertFalse(Proxy.isProxyClass(reader.getClass()));
        assertTrue(reader.readOne());
        assertEquals("method[!net.openhft.chronicle.wire.method.MethodWriterByInterfaceTest$MWBImpl {\n" +
                "  name: name,\n" +
                "  time: 2009-02-13T23:31:30.123456\n" +
                "}\n" +
                "]\n", sw.toString().replace("\r", ""));
    }

    interface MWBI {
        String name();

        long time();
    }

    interface MWBI0 {
        void method(MWBI mwbi);
    }

    static class MWBImpl extends SelfDescribingMarshallable implements MWBI {
        String name;
        @LongConversion(MicroTimestampLongConverter.class)
        long time;

        MWBImpl(String name, long time) {
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
