package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Rob Austin.
 */
@RunWith(value = Parameterized.class)
public class PrimitiveTypeWrappersTest {

    private boolean isTextWire;

    public PrimitiveTypeWrappersTest(Object isTextWire) {
        this.isTextWire = (Boolean) isTextWire;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(
                new Object[]{Boolean.FALSE}
                , new Object[]{Boolean.TRUE}

        );
    }


    @Test
    public void testNumbers() throws Exception {

        final Class[] types = new Class[]{Byte.class,
                Short.class, Float.class,
                Integer.class, Long.class, Double.class};

        for (Class type : types) {
            final Wire wire = before();

            wire.write().object(1);
            final Object object = wire.read().object(type);
            Assert.assertTrue(type.isAssignableFrom(object.getClass()));
            Assert.assertEquals(1, ((Number) object).intValue());
        }
    }


    @NotNull
    private Wire before() {
        final Bytes bytes = Bytes.allocateElasticDirect();
        return (isTextWire) ? new TextWire(bytes) : new BinaryWire(bytes);
    }


}
