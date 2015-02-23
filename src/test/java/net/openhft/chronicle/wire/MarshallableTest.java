package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeStore;
import org.junit.Test;

public class MarshallableTest {
    @Test
    public void testBytesMarshallable() {
        Marshallable m = new MyTypes();

        Bytes bytes = NativeStore.of(1024).bytes();
        TextWire wire = new TextWire(bytes);
        m.writeMarshallable(wire);
        wire.flip();
        m.readMarshallable(wire);
    }
}
