package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 10/06/16.
 */
@RunWith(value = Parameterized.class)
public class PrimArraysTest {

    private final WireType wireType;
    private final Object array;
    private final String asText;

    public PrimArraysTest(WireType wireType, Object array, String asText) {
        this.wireType = wireType;
        this.array = array;
        this.asText = asText;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        List<Object[]> list = new ArrayList<>();
        for (WireType wt : new WireType[]{
                WireType.TEXT,
                WireType.BINARY
        }) {
            final Object[] objects = {
                    new boolean[]{true, false},
                    "test: !boolean[] [true, false]",
                    "test: !boolean[] []",
                    new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE},
                    "test: !byte[] !!binary gAB/\n\n",
                    "test: !byte[] !!binary \n\n",
                    new char[]{Character.MIN_VALUE, '?', Character.MAX_VALUE},
                    "test: !char[] [\"\\0\", \"?\", ï¿¿]",
                    "test: !char[] []",
                    new short[]{Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE},
                    "test: !short[] [-32768, -1, 0, 1, 32767]",
                    "test: !short[] []",
                    new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE},
                    "test: !int[] [-2147483648, -1, 0, 1, 2147483647]",
                    "test: !int[] []",
                    new long[]{Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE},
                    "test: !long[] [-9223372036854775808, -1, 0, 1, 9223372036854775807]",
                    "test: !long[] []",
                    new float[]{Float.MIN_VALUE, -1, 0, 1, Float.MAX_VALUE},
                    "test: !float[] [0.000000000000000000000000000000000000000000001401298464324817, -1.0, 0.0, 1.0, 340282346638528858800000000000000000000]",
                    "test: !float[] []",
                    new double[]{Double.MIN_VALUE, -1, 0, 1, Double.MAX_VALUE},
                    "test: !double[] [0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003, -1.0, 0.0, 1.0, 179769313486231563200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000]",
                    "test: !double[] []"
            };
            for (int i = 0; i < objects.length; i += 3) {
                Object array = objects[i];
                list.add(new Object[]{wt, array, objects[i + 1]});
                final Object emptyArray = Array.newInstance(array.getClass().getComponentType(), 0);
                list.add(new Object[]{wt, emptyArray, objects[i + 2]});
            }
        }
        return list;
    }

    @Test
    public void testPrimArray() {
        Wire wire = createWire();
        wire.write("test")
                .object(array);
        System.out.println(wire);
        if (wireType == WireType.TEXT)
            assertEquals(asText, wire.toString());

        Object array2 = wire.read().object();
        assertEquals(array.getClass(), array2.getClass());
        assertEquals(Array.getLength(array), Array.getLength(array));
        for (int i = 0, len = Array.getLength(array); i < len; i++)
            assertEquals(Array.get(array, i), Array.get(array2, i));
    }

    private Wire createWire() {
        return wireType.apply(Bytes.elasticByteBuffer());
    }
}
