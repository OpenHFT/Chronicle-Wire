package net.openhft.chronicle.wire.type.conversions.binnary;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.BinaryWire;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * Created by Rob Austin
 */
public class ConventionsTest {

    @Ignore("todo fix")
    @Test
    public void testTypeConversions() throws Exception {

        for (Class type : new Class[]{String.class, Integer.class, Long.class, Short.class, Byte
                .class}) {
            Object extected;
            if (Number.class.isAssignableFrom(type)) {
                System.out.println("" + type + "");
                final Field max_value = type.getField("MAX_VALUE");
                extected = max_value.get(type);
            } else {
                extected = "123";
            }

            Assert.assertEquals("type=" + type, extected, test(extected, type));
        }
    }

    @Ignore("todo fix")
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

    @Ignore("todo fix")
    @Test(timeout = 10000)
    public void testTypeConversions2() throws Exception {

        for (Class type : new Class[]{String.class, Integer.class, Long.class, Short.class, Byte
                .class}) {
            Object extected;
            if (Number.class.isAssignableFrom(type)) {
                System.out.println("" + type + "");
                final Field max_value = type.getField("MAX_VALUE");
                extected = max_value.get(type);
            } else {
                extected = "123";
            }
            Assert.assertEquals("type=" + type, extected, test(extected, type));
        }
    }


    @Test
    public void testTypeConversionsMaxUnsigned() throws Exception {

        for (long shift : new long[]{8, 16, 32}) {

            long extected = (1L << shift) - 1L;

            Assert.assertEquals(extected, (long) test(extected, Long.class));
        }
    }

    @Test
    public void testLargeLongToString() throws Exception {
        long extected = Long.MAX_VALUE;
        Assert.assertEquals(extected, (long) Long.valueOf(test(extected, String.class)));
    }

    @Ignore("todo fix")
    @Test
    public void testSmallLongToString() throws Exception {
        long extected = Long.MIN_VALUE;
        Assert.assertEquals(extected, Long.parseLong(test(extected, String.class)));
    }

    @Test
    public void testStringWithNumber() throws Exception {
        String extected = Long.toString(Long.MAX_VALUE);
        Assert.assertEquals(extected, String.valueOf(test(extected, String.class)));
    }


    @Test
    public void testString() throws Exception {
        String extected = "some text";
        Assert.assertEquals(extected, test(extected, String.class));
    }


    public <T> T test(Object source, Class<T> destinationType) {

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

        wire.bytes().flip();

        if (String.class.isAssignableFrom(destinationType)) {
            final String actual = wire.getValueIn().text();
            return (T) (String) actual;
        }


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
