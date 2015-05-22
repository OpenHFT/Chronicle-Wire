package net.openhft.chronicle.wire.type.conversions.binnary;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.BinaryWire;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * Created by Rob Austin
 */
public class ConventionsTest {


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
                extected = 123;
            }
            System.out.println("testing ((" + (extected.getClass().getSimpleName()) + ")" +
                    extected + ") to a " + type.getSimpleName() + " and back to a " + extected.getClass()
                    .getSimpleName());
            Assert.assertEquals("type=" + type, extected, test(extected, type));
        }
    }


    @Test
    public void testTypeConversionsMaxUnsigned() throws Exception {

        for (long shift : new long[]{8, 16, 32}) {

            long extected = (1L << shift) - 1L;

            Assert.assertEquals(extected, test(extected, Long.class));
        }
    }

    public Object test(Object source, Class destinationType) {

        final BinaryWire wire = new BinaryWire(Bytes.elasticByteBuffer());

        if (source instanceof Long)
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

            if (source instanceof Long)
                return Long.valueOf(actual);
            else if (source instanceof Integer)
                return Integer.valueOf(actual);
            else if (source instanceof Short)
                return Short.valueOf(actual);
            else if (source instanceof Byte)
                return Byte.valueOf(actual);
            else if (source instanceof Float)
                return Float.valueOf(actual);
            else if (source instanceof Double)
                return Double.valueOf(actual);
            else if (source instanceof CharSequence)
                return actual.charAt(0);
        }

        if (Long.class.isAssignableFrom(destinationType))
            return wire.getValueIn().int64();

        if (Integer.class.isAssignableFrom(destinationType))
            return wire.getValueIn().int32();

        if (Short.class.isAssignableFrom(destinationType))
            return wire.getValueIn().int16();

        if (Byte.class.isAssignableFrom(destinationType))
            return wire.getValueIn().int8();

        if (Float.class.isAssignableFrom(destinationType))
            return wire.getValueIn().float32();


        if (Double.class.isAssignableFrom(destinationType))
            return wire.getValueIn().float64();

        throw new UnsupportedOperationException("");
    }


}
