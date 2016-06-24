package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ReadResolvable;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnumTest {

    @Test
    public void testEnum() {
        TextWire wire = new TextWire(Bytes.elasticByteBuffer());
        wire.write("test")
                .object(TestEnum.INSTANCE);
        assertEquals("test: !net.openhft.chronicle.wire.EnumTest$TestEnum INSTANCE\n", wire.toString());
        TextWire wire2 = new TextWire(Bytes.from("test: !net.openhft.chronicle.wire.EnumTest$TestEnum {\n" +
                "}\n"));
        Object enumObject = wire2.read(() -> "test")
                .object();
        Assert.assertTrue(enumObject == TestEnum.INSTANCE);
    }

    public enum TestEnum implements Marshallable, ReadResolvable<TestEnum> {
        INSTANCE;

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
        }

        @Override
        public EnumTest.TestEnum readResolve() {
            return INSTANCE;
        }
    }
}