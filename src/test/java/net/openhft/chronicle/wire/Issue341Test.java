package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class Issue341Test extends WireTestCommon {

    private final WireType wireType;

    public Issue341Test(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML},
                new Object[]{WireType.BINARY_LIGHT},
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


    @Test
    public void testComparableSerializable() {
        final MyComparableSerializable source = new MyComparableSerializable("hello");

        final Bytes<?> bytes = new HexDumpBytes();
        final Wire wire = wireType.apply(bytes);

        wire.getValueOut().object((Class) source.getClass(), source);
        System.out.println(wireType + "\n"
                + (wire.getValueOut().isBinary() ? bytes.toHexString() : bytes.toString()));

        final MyComparableSerializable target = wire.getValueIn().object(source.getClass());
        Assert.assertEquals(source.value, target.value);
    }

    static final class MyClass extends SelfDescribingMarshallable {
        Instant instant;
    }

    static final class MyComparableSerializable implements Serializable, Comparable<MyComparableSerializable> {
        final String value;

        MyComparableSerializable(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public int compareTo(@NotNull MyComparableSerializable o) {
            return value.compareTo(o.value);
        }
    }
}
