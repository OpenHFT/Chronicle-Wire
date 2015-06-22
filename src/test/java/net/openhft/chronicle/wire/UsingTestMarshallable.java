package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * Created by Rob Austin
 */
public class UsingTestMarshallable {

    @Test
    public void testConverMarshallableToTextName() throws Exception {

        TestMarshallable testMarshallable = new TestMarshallable();
        StringBuilder name = new StringBuilder("hello world");
        testMarshallable.setName(name);

        Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer();

        TextWire textWire = new TextWire(byteBufferBytes);

        textWire.writeDocument(false, d -> d.write(() -> "any-key").marshallable(testMarshallable));

        String value = Wires.fromSizePrefixedBlobs(textWire.bytes());

        String replace = value.replace("\n", "\\n");

        System.out.println(replace);

        Assert.assertTrue(replace.length() > 1);
    }
}
