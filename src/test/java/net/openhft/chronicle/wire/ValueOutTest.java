package net.openhft.chronicle.wire;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Rob Austin.
 */
@RunWith(value = Parameterized.class)
public class ValueOutTest extends TestCase {


    private final WireType wireType;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    public ValueOutTest(WireType wireType) {
        this.wireType = wireType;
    }


    @Test
    public void test() {

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());

        final byte[] expected = "this is my byte array".getBytes();
        wire.writeDocument(false, w ->
                w.write().object(expected)

        );

        System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));

        wire.readDocument(null, w -> {
            final byte[] actual = (byte[]) w.read().object();
            Assert.assertArrayEquals(expected, actual);

        });

    }

    @Test
    public void testRequestedType() {

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());

        final byte[] expected = "this is my byte array".getBytes();
        wire.writeDocument(false, w -> w.write().object(expected));

        System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));

        wire.readDocument(null, w -> {
            final byte[] actual = w.read().object(byte[].class);
            Assert.assertArrayEquals(expected, actual);
        });

    }


    @Test
    public void testAllBytes() {

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());

        for (int i = -128; i < 127; i++) {

            final byte[] expected = {(byte) i};
            wire.writeDocument(false, w ->
                    w.write().object(expected)
            );

            System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));

            wire.readDocument(null, w -> {
                final byte[] actual = (byte[]) w.read().object();
                Assert.assertArrayEquals(expected, actual);
            });

        }
    }


}