package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class Issue886Test {
    private final WireType wireType;

    public Issue886Test(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        return Arrays.asList(new Object[][]{
                {WireType.YAML_ONLY},
                {WireType.JSON_ONLY},
        });
    }

    @Test
    public void test() throws IOException {
        String data = "{\n" +
                "  \"a\": 1.234,\n" +
                "  \"a1\": '1.234'\n" +
                "  \"a2\": \"1.234\"\n" +
                "  \"b\": 128,\n" +
                "  \"b1\": '128'\n" +
                "  \"b2\": \"128\"\n" +
                "}";
        Wire wire = wireType.apply(Bytes.from(data));
        assertEquals(1.234, wire.read(() -> "a").float64(), 0.0);
        assertEquals(1.234, wire.read(() -> "a1").float64(), 0.0);
        assertEquals(1.234, wire.read(() -> "a2").float64(), 0.0);
        assertEquals(128L, wire.read(() -> "b").int64());
        assertEquals(128L, wire.read(() -> "b1").int64());
        assertEquals(128L, wire.read(() -> "b2").int64());

        Wire wire2 = wireType.apply(Bytes.from(data));
        assertEquals(1.234f, wire2.read(() -> "a").float32(), 0.0f);
        assertEquals(1.234f, wire2.read(() -> "a1").float32(), 0.0f);
        assertEquals(1.234f, wire2.read(() -> "a2").float32(), 0.0f);
        assertEquals(128, wire2.read(() -> "b").int32());
        assertEquals(128, wire2.read(() -> "b1").int32());
        assertEquals(128, wire2.read(() -> "b2").int32());
    }
}
