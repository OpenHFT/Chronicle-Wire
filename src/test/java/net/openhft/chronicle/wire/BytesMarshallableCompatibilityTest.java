package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public final class BytesMarshallableCompatibilityTest extends WireTestCommon {

    @Test
    public void shouldSerialiseToBytes() {
        final Container container = new Container();
        container.number = 17;
        container.label = "non-deterministic";
        container.truth = Boolean.TRUE;

        final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);

        container.writeMarshallable(bytes);

        final Container copy = new Container();
        copy.readMarshallable(bytes);

        assertEquals(container.number, copy.number);
        assertEquals(container.label, copy.label);
        assertEquals(container.truth, copy.truth);
    }

    private static final class Container extends BytesInBinaryMarshallable {
        private int number;
        private String label;
        private Boolean truth;
    }
}
