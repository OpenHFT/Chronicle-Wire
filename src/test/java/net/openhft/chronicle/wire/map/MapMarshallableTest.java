package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class MapMarshallableTest extends WireTestCommon {

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
        String string2 = map2.toString();
        assertTrue(string2.equals("{one=10, two=20, three=30}") || string2.equals("{one=10, three=30, two=20}")
        || string2.equals("{two=20, one=10, three=30}")||string2.equals("{two=30, three=30, one=10}")
        || string2.equals("{three=30, one=10, two=20}") || string2.equals("{three=30, two=20, one=10}"));
    }

    private static class MyDto extends SelfDescribingMarshallable {
        int one;
        int two;
        int three;
    }
}