package net.openhft.chronicle.wire.type.conversions.binary;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.BinaryWire;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * Created by Rob Austin
 */
public class ConventionsTest {

    @Test
    public void testTypeConversionsMaxValue() throws Exception {

        for (Class type : new Class[]{String.class, Integer.class, Long.class, Short
                .class, Byte
                .class, Float.class, Double.class}) {
            Object extected;
            if (Number.class.isAssignableFrom(type)) {
                System.out.println("" + type + "");
                final Field max_value = type.getField("MAX_VALUE");
                extected = max_value.get(type);
            } else {
                extected = "123"; // small number
            }

            Assert.assertEquals("type=" + type, extected, test(extected, type));
        }
    }

    @Test
    public void testTypeConversionsMinValue() throws Exception {

        for (Class type : new Class[]{String.class, Integer.class, Long.class, Short.class, Byte
                .class, Float.class, Double.class}) {
            Object extected;
            if (Number.class.isAssignableFrom(type)) {
                System.out.println("" + type + "");
                final Field value = type.getField("MIN_VALUE");
                extected = value.get(type);
            } else {
                extected = "123";
            }

            Assert.assertEquals("type=" + type, extected, test(extected, type));
        }
    }

    @Test
    public void testTypeConversionsSmallNumber() throws Exception {

        for (Class type : new Class[]{String.class, Integer.class, Long.class, Short
                .class, Byte.class}) {

            Object extected = "123"; // small number
            Assert.assertEquals("type=" + type, extected, String.valueOf(test(extected, type)));
        }

        Assert.assertEquals(123.0, (double) (Double) test("123", Double.class), 0);
        Assert.assertEquals(123.0, (double) (Float) test("123", Float.class), 0);

    }

    @Test
    public void testTypeConversionsConvertViaString() throws Exception {

        for (Class type : new Class[]{Integer.class, Long.class, Short.class, Byte
                .class}) {
            Object extected;
            if (Number.class.isAssignableFrom(type)) {
                final Field max_value = type.getField("MAX_VALUE");
                extected = max_value.get(type);
            } else {
                extected = 123;
            }

            final Object value = test(extected, String.class);
            final Object actual = test(value, extected.getClass());

            Assert.assertEquals("type=" + type, extected, actual);
        }
    }

    @Test
    public void testTypeConversionsMaxUnsigned() throws Exception {

        for (long shift : new long[]{8}) {
            long extected = 1L << shift;
            Assert.assertEquals(extected, (long) test(extected, Long.class));
        }
    }

    @Nullable
    public <T> T test(Object source, @NotNull Class<T> destinationType) {

        final BinaryWire wire = new BinaryWire(Bytes.elasticByteBuffer());

        if (source instanceof String)
            wire.writeValue().text((String) source);
        else if (source instanceof Long)
            wire.writeValue().int64((Long) source);
        else if (source instanceof Integer)
            wire.writeValue().int32((Integer) source);
        else if (source instanceof Short)
            wire.writeValue().int16((Short) source);
        else if (source instanceof Byte)
            wire.writeValue().int8((Byte) source);
        else if (source instanceof Float)
            wire.writeValue().float32((Float) source);
        else if (source instanceof Double)
            wire.writeValue().float64((Double) source);

        wire.bytes().flip();

        if (String.class.isAssignableFrom(destinationType))
            return (T) wire.getValueIn().text();

        if (Long.class.isAssignableFrom(destinationType))
            return (T) (Long) wire.getValueIn().int64();

        if (Integer.class.isAssignableFrom(destinationType))
            return (T) (Integer) wire.getValueIn().int32();

        if (Short.class.isAssignableFrom(destinationType))
            return (T) (Short) wire.getValueIn().int16();

        if (Byte.class.isAssignableFrom(destinationType))
            return (T) (Byte) wire.getValueIn().int8();

        if (Float.class.isAssignableFrom(destinationType))
            return (T) (Float) wire.getValueIn().float32();

        if (Double.class.isAssignableFrom(destinationType))
            return (T) (Double) wire.getValueIn().float64();

        throw new UnsupportedOperationException("");
    }
}
