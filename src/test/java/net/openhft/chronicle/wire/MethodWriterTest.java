package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

public class MethodWriterTest {
    @Test(expected = IllegalArgumentException.class)
    public void invalidInterface() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap());
        wire.methodWriter(MyBadInterface.class);
    }

    interface MyBadInterface extends Marshallable {

    }

}