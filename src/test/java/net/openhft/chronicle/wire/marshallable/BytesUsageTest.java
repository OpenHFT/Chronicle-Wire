package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.wire.AbstractMarshallable;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class BytesUsageTest {

    @SuppressWarnings("rawtypes")
    @Test
    public void testBytes() {
        BytesStore value = Bytes.fromString("helloWorld");
        {
            BytesWrapper bw = new BytesWrapper();
            bw.clOrdId(Bytes.fromString("A" + value));
            assertEquals(Bytes.fromString("AhelloWorld"), bw.clOrdId());
        }

        // gc free replacement
        BytesWrapper bw = new BytesWrapper(); // this should be recycled to avoid garbage

        bw.clOrdId().clear().append("A").append(value);
        assertEquals(Bytes.fromString("AhelloWorld"), bw.clOrdId());
    }

    @SuppressWarnings("rawtypes")
    static class BytesWrapper extends AbstractMarshallable {
        Bytes clOrdId = Bytes.allocateElasticDirect();

        public Bytes clOrdId() {
            return clOrdId;
        }

        public BytesWrapper clOrdId(Bytes clOrdId) {
            this.clOrdId = clOrdId;
            return this;
        }
    }
}
