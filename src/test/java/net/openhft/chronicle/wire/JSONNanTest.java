package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

public class JSONNanTest extends WireTestCommon {

    @Test
    public void writeNaN() {
        Bytes b = Bytes.elasticByteBuffer();
        try {
            Wire wire = WireType.JSON.apply(b);
            Dto value = new Dto();
            value.value = Double.NaN;
            wire.write().object(value);
            Assert.assertEquals("\"\":{\"value\":null}", wire.toString());
        } finally {
            b.releaseLast();
        }
    }

    /**
     * JSON spec says that NAN should be written as null
     */
    @Test
    public void readNan() {
        Bytes b = Bytes.from("\"\":{\"value\":null}");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assert.assertTrue(Double.isNaN(value.value));
    }

    /**
     * JSON spec says that NAN should be written as null
     */
    @Test
    public void readNanWithSpaceAteEnd() {
        Bytes b = Bytes.from("\"\":{\"value\":null }");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assert.assertTrue(Double.isNaN(value.value));
    }

    /**
     * JSON spec says that NAN should be written as null
     */
    @Test
    public void readNanWithSpaceAtStart() {
        Bytes b = Bytes.from("\"\":{\"value\": null}");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assert.assertTrue(Double.isNaN(value.value));
    }

public static class Dto extends SelfDescribingMarshallable {
    double value;
}

}
