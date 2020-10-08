package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

public class MethodWriterMarshallableTest {
    @Test(expected = IllegalArgumentException.class)
    public void invalidInterface() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap());
        wire.methodWriter(MyBadInterface.class);
    }

    interface MyBadInterface extends Marshallable {

    }
}