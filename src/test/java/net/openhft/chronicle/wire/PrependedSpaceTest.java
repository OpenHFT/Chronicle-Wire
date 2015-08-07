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
public class PrependedSpaceTest {


    private boolean isTextWire;
    private Bytes bytes;

    public PrependedSpaceTest(Object isTextWire) {
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
    public void testPrependedSpace() throws Exception {
        final String prependedSpace = " hello world";
        final Wire wire = wireFactory();
        wire.write().text(prependedSpace);

        Assert.assertEquals(prependedSpace, wire.read().text());

    }

    @Test
    public void testPostpendedSpace() throws Exception {
        final String postpendedSpace = "hello world ";
        final Wire wire = wireFactory();
        wire.write().text(postpendedSpace);

        Assert.assertEquals(postpendedSpace, wire.read().text());


    }


    @NotNull
    private Wire wireFactory() {
        bytes = Bytes.allocateElasticDirect();
        return (isTextWire) ? new TextWire(bytes) : new BinaryWire(bytes);
    }

}
