package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 09/05/16.
 */
@RunWith(value = Parameterized.class)
public class MarshallableWireTest {
    private final WireType wireType;
    private final Marshallable m;

    public MarshallableWireTest(WireType wireType, Marshallable m) {
        this.wireType = wireType;
        this.m = m;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        List<Object[]> list = new ArrayList<>();
        WireType[] wireTypes = {WireType.TEXT, WireType.BINARY};
        Marshallable[] objects = {
                new Nested(),
                new Nested(new ScalarValues(), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap()),
                new ScalarValues(),
                new ScalarValues(1),
                new ScalarValues(10)
        };
        for (WireType wt : wireTypes) {
            for (Marshallable object : objects) {
                Object[] test = {wt, object};
                list.add(test);
            }
        }
        return list;
    }

    @Test
    public void writeMarshallable() {
        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);

        wire.getValueOut().object(m);
        System.out.println(wire);

        Object m2 = wire.getValueIn()
                .object();
        assertEquals(m, m2);
    }
}
