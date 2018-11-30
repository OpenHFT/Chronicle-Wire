package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.ObjectUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertEquals;

public class WireToOutputStreamTest {
    @Test
    public void testVisSocket() throws IOException {
        ServerSocket ss = new ServerSocket(0);
        Socket s = new Socket("localhost", ss.getLocalPort());
        Socket s2 = ss.accept();
        WireToOutputStream wtos = new WireToOutputStream(WireType.RAW, s.getOutputStream());

        Wire wire = wtos.getWire();
        AnObject ao = new AnObject();
        ao.value = 12345;
        ao.text = "Hello";
        // write the type is needed.
        wire.getValueOut().typeLiteral(AnObject.class);
        Wires.writeMarshallable(ao, wire);
        wtos.flush();

        InputStreamToWire istw = new InputStreamToWire(WireType.RAW, s2.getInputStream());
        Wire wire2 = istw.readOne();
        Class type = wire2.getValueIn().typeLiteral();
        Object ao2 = ObjectUtils.newInstance(type);
        Wires.readMarshallable(ao2, wire2, true);
        System.out.println(ao2);
        ss.close();
        s.close();
        s2.close();
        assertEquals(ao.toString(), ao2.toString());
    }

    public static class AnObject implements Serializable {
        long value;
        String text;

        @Override
        public String toString() {
            return "AnObject{" +
                    "value=" + value +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

}