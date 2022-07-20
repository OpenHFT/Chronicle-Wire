package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

public class MethodWriterMarshallableTest extends WireTestCommon {
    @Test(expected = IllegalArgumentException.class)
    public void invalidInterface() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        wire.methodWriter(MyBadInterface.class);
    }

    interface MyBadInterface extends Marshallable {
    }
}