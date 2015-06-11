package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.NativeBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by peter on 28/05/15.
 */
public class QueryWireTest {
    private NativeBytes<Void> bytes;

    @NotNull
    private QueryWire createWire() {
        bytes = nativeBytes();
        return new QueryWire(bytes);
    }

    @Test
    public void readWriteQuery() {
        QueryWire wire = createWire();
        wire.write(() -> "bool").bool(true)
                .write(() -> "int").int64(12345)
                .write(() -> "text").text("Hello World")
                .write(() -> "float").float64(12.345);
        bytes.flip();
        assertEquals("bool=true&int=12345&text=Hello World&float=12.345", bytes.toString());
        wire.read(() -> "bool").bool(b -> assertTrue(b))
                .read(() -> "int").int64(i -> assertEquals(12345, i))
                .read(() -> "text").text((Consumer<String>) s -> assertEquals("Hello World", s))
                .read(() -> "float").float64(f -> assertEquals(12.345, f, 0.0));
        WireParser wp = WireParser.wireParser();
        List<Object> results = new ArrayList<>();
        wp.register(() -> "bool", v -> v.bool(results::add));
        wp.register(() -> "int", v -> v.int64(results::add));
        wp.register(() -> "text", v -> v.text((Consumer<String>) s -> results.add(s)));
        wp.register(() -> "float", v -> v.float64(results::add));
        bytes.position(0);
        while (bytes.remaining() > 0)
            wp.parse(wire);
        assertEquals(new ArrayList<>(Arrays.asList(true, 12345L, "Hello World", 12.345)), results);
    }
}
