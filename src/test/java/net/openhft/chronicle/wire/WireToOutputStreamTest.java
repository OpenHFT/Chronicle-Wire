package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class WireToOutputStreamTest extends WireTestCommon {

    public static class AnObject implements Serializable {
        long value;
        String text;

        Timestamp timestamp = new Timestamp(1234567890000L);

        @Override
        public String toString() {
            return "AnObject{" +
                    "value=" + value +
                    ", text='" + text + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
    private WireType currentWireType;

    public WireToOutputStreamTest(WireType currentWireType) {
        this.currentWireType = currentWireType;
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<WireType> data() {
        List<WireType> wireTypes = new ArrayList<>();

        for (WireType wireType : WireType.values()) {
            if (wireType.isAvailable()
                    && wireType != WireType.RAW // Serializable objects are not support for RAW binary
                    && wireType != WireType.CSV // type literals not supported in CSV files
                    && wireType != WireType.READ_ANY // cannot write to a READ_ANY until it knows what type to use.
            ) {
                wireTypes.add(wireType);
            }
        }

        return wireTypes;
    }

    @Test
    public void testTimestamp() {
        Wire wire = currentWireType.apply(Bytes.allocateElasticOnHeap(128));
        Timestamp ts = new Timestamp(1234567890000L);
        wire.write().object(ts);
       // System.out.println(wire);

        Timestamp ts2 = wire.read()
                .object(Timestamp.class);
        assertEquals(ts.toString(), ts2.toString());
    }

    @Test
    public void testNoSocket() {
        Wire wire = currentWireType.apply(Bytes.allocateElasticOnHeap(128));
        AnObject ao = writeAnObject(wire);
       // System.out.println(wire);

        Object ao2 = readAnObject(wire);
        assertEquals(ao.toString(), ao2.toString());
    }

    @Test
    public void testVisSocket() throws IOException {
        try (ServerSocket ss = new ServerSocket(0);
             Socket s = new Socket("localhost", ss.getLocalPort());
             Socket s2 = ss.accept()) {
            WireToOutputStream wtos = new WireToOutputStream(currentWireType, s.getOutputStream());

            Wire wire = wtos.getWire();
            AnObject ao = writeAnObject(wire);
            wtos.flush();

            InputStreamToWire istw = new InputStreamToWire(currentWireType, s2.getInputStream());
            Wire wire2 = istw.readOne();
            Object ao2 = readAnObject(wire2);
           // System.out.println(ao2);
            assertEquals(ao.toString(), ao2.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @NotNull
    public Object readAnObject(Wire wire2) {
        Class type = wire2.getValueIn().typeLiteral();
        Object ao2 = ObjectUtils.newInstance(type);
        Wires.readMarshallable(ao2, wire2, true);
        return ao2;
    }

    @NotNull
    public AnObject writeAnObject(Wire wire) {
        AnObject ao = new AnObject();
        ao.value = 12345;
        ao.text = "Hello";
        //ao.timestamp1 = new Timestamp(1234567890);
        // write the type is needed.
        wire.getValueOut().typeLiteral(AnObject.class);
        Wires.writeMarshallable(ao, wire);
        return ao;
    }
}