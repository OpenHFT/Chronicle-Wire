/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TupleInvocationHandlerTest extends WireTestCommon {

    private final boolean originalGenerateTuples = Wires.GENERATE_TUPLES;

    @After
    public void restoreTuplesFlag() {
        Wires.GENERATE_TUPLES = originalGenerateTuples;
    }

    @Test
    public void tupleSupportsFieldApiAndDeepCopy() throws InvalidMarshallableException, NoSuchFieldException {
        Wires.GENERATE_TUPLES = true;
        SampleTuple tuple = Wires.tupleFor(SampleTuple.class, "sampleType");
        assertNotNull(tuple);

        tuple.setField("alpha", "one");
        tuple.setField("beta", 2L);
        assertEquals("one", tuple.getField("alpha", String.class));
        assertEquals(Long.valueOf(2L), tuple.getField("beta", Long.class));
        assertEquals("sampleType", tuple.className());
        assertTrue(tuple.usesSelfDescribingMessage());

        int hash = tuple.hashCode();
        assertTrue(hash != 0);
        assertEquals(tuple, tuple);
        assertNotEquals("other", tuple);

        SampleTuple copy = tuple.deepCopy();
        assertNotNull(copy);
        assertEquals(tuple.getField("alpha", String.class), copy.getField("alpha", String.class));

        List<FieldInfo> infos = tuple.$fieldInfos();
        assertEquals(2, infos.size());

        Bytes<?> buffer = Bytes.allocateElasticOnHeap();
        Wire textWire = WireType.TEXT.apply(buffer);
        tuple.writeMarshallable(textWire);

        buffer.readPositionRemaining(0, buffer.writePosition());
        SampleTuple readBack = Wires.tupleFor(SampleTuple.class, "sampleType");
        readBack.readMarshallable(textWire);
        assertEquals("one", readBack.getField("alpha", String.class));
        buffer.releaseLast();
    }

    interface SampleTuple extends Marshallable {
        void setField(String name, Object value);

        <T> T getField(String name, Class<T> type);

        List<FieldInfo> $fieldInfos();
    }
}
