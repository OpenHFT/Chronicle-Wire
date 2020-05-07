package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThrowableTest {
    @Test
    public void writeReadThrowable() {
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.BINARY_LIGHT}) {

            Wire wire = wireType.apply(Bytes.allocateElasticDirect());
            Throwable message = new Throwable("message");
            message.initCause(new Throwable("cause"));
            wire.getValueOut()
                    .object(message);
//        System.out.println(wire);

            Throwable t = (Throwable) wire.getValueIn().object();
            assertEquals("message", t.getMessage());
            assertTrue(t.getStackTrace()[0].toString().startsWith("net.openhft.chronicle.wire.ThrowableTest.writeReadThrowable(ThrowableTest.java"));
            wire.bytes().release();
        }
    }
}
