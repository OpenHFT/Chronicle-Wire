package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 09/05/16.
 */
@RunWith(value = Parameterized.class)
public class MapWireTest {
    private final WireType wireType;
    private final Map m;

    public MapWireTest(WireType wireType, Map m) {
        this.wireType = wireType;
        this.m = m;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        List<Object[]> list = new ArrayList<>();
        WireType[] wireTypes = {WireType.TEXT, WireType.BINARY};
        for (WireType wt : wireTypes) {
            for (int i = 0; i < Character.MAX_VALUE; i += 128) {
                Map<Integer, String> map = new LinkedHashMap<>();
                for (int ch = i; ch < i + 128; ch++) {
                    if (Character.isValidCodePoint(ch)) {
                        final String s = Character.toString((char) ch);
                        map.put(i, s);
                    }
                }
                Object[] test = {wt, map};
                list.add(test);
            }
        }
        return list;
    }

    @Test
    public void writeMap() {
        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);
        wire.getValueOut()
                .marshallable(m);
//        System.out.println(wire);

        Map m2 = wire.getValueIn()
                .marshallableAsMap(Object.class, Object.class);
        assertEquals(m, m2);
    }
}
