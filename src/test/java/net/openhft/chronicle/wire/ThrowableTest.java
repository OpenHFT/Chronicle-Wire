package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThrowableTest extends WireTestCommon {
    @Test
    public void writeReadThrowable() {
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.BINARY_LIGHT}) {

            Wire wire = wireType.apply(Bytes.allocateElasticDirect());
            try (DocumentContext dc = wire.writingDocument()) {
                Throwable message = new Throwable("message");
                message.initCause(new Throwable("cause"));
                wire.getValueOut()
                        .object(message);
            }
/*            if (wireType == WireType.TEXT)
                System.out.println(wire);
            else
                System.out.println(wire.bytes().toHexString()+"\n"+Wires.fromSizePrefixedBlobs(wire.bytes()));*/

            try (DocumentContext dc = wire.readingDocument()) {
                Throwable t = (Throwable) wire.getValueIn().object();
                assertEquals("message", t.getMessage());
                assertTrue(t.getStackTrace()[0].toString().startsWith("net.openhft.chronicle.wire.ThrowableTest.writeReadThrowable(ThrowableTest.java"));
            }
            wire.bytes().releaseLast();
        }
    }
}
