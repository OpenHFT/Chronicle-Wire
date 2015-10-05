package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Rob Austin.
 */

@RunWith(value = Parameterized.class)
public class WireTests {

    private final WireType wireType;

    @Rule
    public TestName name = new TestName();

    public WireTests(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {

        final List<Object[]> list = new ArrayList<>();
        list.add(new Object[]{WireType.BINARY});
        list.add(new Object[]{WireType.TEXT});
        list.add(new Object[]{WireType.RAW});
        return list;
    }

    @Test
    public void testWriteNull() {

        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = wireType.apply(b);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);

        Object o = wire.read().object(Object.class);
        Assert.assertEquals(null, o);
        String s = wire.read().object(String.class);
        Assert.assertEquals(null, s);
        RetentionPolicy rp = wire.read().object(RetentionPolicy.class);
        Assert.assertEquals(null, rp);
        Circle c = wire.read().object(Circle.class);  // this fails without the check.
        Assert.assertEquals(null, c);
    }
}
