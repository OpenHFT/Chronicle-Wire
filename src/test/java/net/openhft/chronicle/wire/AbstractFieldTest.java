package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class AbstractFieldTest extends WireTestCommon {

    private final WireType wireType;

    public AbstractFieldTest(WireType wireType) {
        this.wireType = wireType;

    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.BINARY},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.JSON}
        );
    }

    @Test
    public void abstractField() {
        MSDMHolder holder = new MSDMHolder();
        holder.marshallable = new MySelfDescribingMarshallable("Hello World");

        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(MSDMHolder.class, holder);

        MSDMHolder result = wire.getValueIn().object(MSDMHolder.class);
        assertEquals(holder, result);
    }

    @Test
    public void abstractField2() {
        MSDMHolder2 holder = new MSDMHolder2();
        holder.marshallable = new MySelfDescribingMarshallable("Hello World");

        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(MSDMHolder2.class, holder);

        MSDMHolder2 result = wire.getValueIn().object(MSDMHolder2.class);
        assertEquals(holder, result);
    }

    static class MSDMHolder extends SelfDescribingMarshallable {
        SelfDescribingMarshallable marshallable;
    }

    static class MSDMHolder2 extends SelfDescribingMarshallable {
        MySelfDescribingMarshallable marshallable;
    }

    static class MySelfDescribingMarshallable extends SelfDescribingMarshallable {
        String text;

        public MySelfDescribingMarshallable(String s) {
            text = s;
        }
    }
}
