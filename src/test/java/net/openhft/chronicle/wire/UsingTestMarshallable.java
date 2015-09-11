package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * Created by Rob Austin
 */
public class UsingTestMarshallable {

    @Test
    public void testConverMarshallableToTextName() {

        TestMarshallable testMarshallable = new TestMarshallable();
        StringBuilder name = new StringBuilder("hello world");
        testMarshallable.setName(name);

        Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer();

        ByteBuffer byteBuffer = byteBufferBytes.underlyingObject();
        System.out.println(byteBuffer.getClass());

        Wire textWire = new TextWire(byteBufferBytes);
        textWire.bytes().readPosition();

        textWire.writeDocument(false, d -> d.write(() -> "any-key").marshallable(testMarshallable));

        String value = WireInternal.fromSizePrefixedBinaryToText(textWire.bytes());

        //String replace = value.replace("\n", "\\n");

        System.out.println(byteBufferBytes.toHexString());
        System.out.println(value);

      //  Assert.assertTrue(replace.length() > 1);
    }
}
