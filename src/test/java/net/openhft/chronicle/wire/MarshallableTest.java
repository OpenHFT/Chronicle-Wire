package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

public class MarshallableTest {
    @Test
    public void testBytesMarshallable() {
        Marshallable m = new MyTypes();

        Bytes bytes = nativeBytes();
        TextWire wire = new TextWire(bytes);
        m.writeMarshallable(wire);
        wire.flip();
        m.readMarshallable(wire);
    }
}
