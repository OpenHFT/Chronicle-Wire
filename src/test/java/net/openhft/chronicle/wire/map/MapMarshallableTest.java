package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.wire.AbstractMarshallable;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class MapMarshallableTest {

    @Test
    public void test() {
        @NotNull final Map<String, Object> map = new LinkedHashMap<>();
        map.put("one", 10);
        map.put("two", 20);
        map.put("three", 30);

        @NotNull MyDto usingInstance = new MyDto();
        @NotNull MyDto result = Wires.copyTo(map, usingInstance);
        assertEquals(10, result.one);
        assertEquals(20, result.two);
        assertEquals(30, result.three);

        @NotNull Map<String, Object> map2 = Wires.copyTo(result, new LinkedHashMap<>());
        assertEquals("{one=10, two=20, three=30}", map2.toString());

        @NotNull Map<String, Object> map3 = Wires.copyTo(map, new TreeMap<>());
        assertEquals("{one=10, three=30, two=20}", map3.toString());
    }

    private static class MyDto extends AbstractMarshallable {
        int one;
        int two;
        int three;
    }
}