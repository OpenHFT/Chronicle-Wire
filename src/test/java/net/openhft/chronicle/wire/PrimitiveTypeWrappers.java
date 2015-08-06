package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Rob Austin.
 */
public class PrimitiveTypeWrappers {

    @Test
    public void testInteger() throws Exception {

        final Bytes bytes = Bytes.allocateElasticDirect();
        final TextWire textWire = new TextWire(bytes);
        textWire.write().object(1);


        final Object object = textWire.read().object(Integer.class);
        Assert.assertTrue(object instanceof Integer);
        Assert.assertEquals((Integer) 1, object);

    }

    @Test
    public void testFloat() throws Exception {

        final Bytes bytes = Bytes.allocateElasticDirect();
        final TextWire textWire = new TextWire(bytes);
        textWire.write().object(1);


        final Object object = textWire.read().object(Float.class);
        Assert.assertTrue(object instanceof Float);
        Assert.assertEquals((float) 1.0, (float) object, 0);

    }

    @Test
    public void testShort() throws Exception {

        final Bytes bytes = Bytes.allocateElasticDirect();
        final TextWire textWire = new TextWire(bytes);
        textWire.write().object(1);


        final Object object = textWire.read().object(Short.class);
        Assert.assertTrue(object instanceof Short);
        Assert.assertEquals((short) 1.0, (short) object);

    }


    @Test
    public void testLong() throws Exception {

        final Bytes bytes = Bytes.allocateElasticDirect();
        final TextWire textWire = new TextWire(bytes);
        textWire.write().object(1);


        final Object object = textWire.read().object(Long.class);
        Assert.assertTrue(object instanceof Long);
        Assert.assertEquals((long) 1, (long) object);

    }
}
