package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class Issue341Test {

    private final WireType wireType;

    public Issue341Test(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                new Object[]{WireType.TEXT},
                new Object[]{WireType.JSON},
                new Object[]{WireType.BINARY},
        });
    }

    @Test
    public void instant() {
        final MyClass source = new MyClass();
        source.instant = Instant.ofEpochMilli(1_000_000_000_000L);

        final Bytes<?> bytes = new HexDumpBytes();
        final Wire wire = wireType.apply(bytes);

        wire.getValueOut().object((Class) source.getClass(), source);
        System.out.println(wireType + "\n"
                + (wire.getValueOut().isBinary() ? bytes.toHexString() : bytes.toString()));

        final MyClass target = wire.getValueIn().object(source.getClass());
        Assert.assertEquals(source, target);

    }

    static final class MyClass extends SelfDescribingMarshallable {
        Instant instant;
    }
}
