package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharSequenceObjectMapTest {
    @Test
    public void put() {
        CharSequenceObjectMap<String> map = new CharSequenceObjectMap<>(10);
        for (int i = 10; i < 20; i++) {
            map.put("" + i, "" + i);
        }
        for (int i = 10; i < 20; i++) {
            assertEquals("" + i, map.get("" + i));
        }
    }

}