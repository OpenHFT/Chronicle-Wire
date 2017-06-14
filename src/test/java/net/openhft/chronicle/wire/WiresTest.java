package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rob on 14/06/2017.
 */
public class WiresTest {


    public static class Base {
        Bytes base = Bytes.elasticByteBuffer();
    }

    @Test
    public void test() {

        Base base1 = new Base();
        base1.base.clear().append("value1");

        Base base2 = new Base();
        base2.base.clear().append("value2");

        Wires.reset(base1);
        Wires.reset(base2);

        base1.base.clear().append("value1");

        Assert.assertEquals("", base2.base.toString());

    }


}
