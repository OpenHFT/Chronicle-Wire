package net.openhft.chronicle.wire;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BinaryWireHighCodeTest {
    @Test
    public void testUnique() throws IllegalAccessException {
        assertEquals(0, BinaryWireHighCode.values().length);
        Set<Integer> values = new HashSet<>();
        for (Field field : BinaryWireHighCode.class.getFields()) {
            int value = (Integer) field.get(null);
            assertTrue(values.add(value));
        }
    }

}