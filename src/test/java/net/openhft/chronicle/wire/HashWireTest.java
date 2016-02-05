package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Created by peter.lawrey on 05/02/2016.
 */
public class HashWireTest {

    @Test
    public void testHash64() throws Exception {
        long h = HashWire.hash64(wire ->
                wire.write(() -> "entrySet").sequence(s -> {
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-1")
                            .write(() -> "value").text("value-1"));
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-2")
                            .write(() -> "value").text("value-2"));
                }));
        assertFalse(h == 0);
    }
}