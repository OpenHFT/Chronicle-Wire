package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class BytesMarshallableCompatibilityTest {

    @Test
    public void shouldSerialiseToBytes() throws Exception {
        final Container container = new Container();
        container.number = 17;
        container.label = "non-deterministic";
        container.truth = Boolean.TRUE;

        final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);

        container.writeMarshallable(bytes);

        final Container copy = new Container();
        copy.readMarshallable(bytes);

        assertThat(copy.number, is(container.number));
        assertThat(copy.label, is(container.label));
        assertThat(copy.truth, is(container.truth));
    }

    private static final class Container extends AbstractMarshallable {
        private int number;
        private String label;
        private Boolean truth;
    }
}
