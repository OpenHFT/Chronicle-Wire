/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TupleInvocationHandlerTest extends WireTestCommon {

    private final boolean originalGenerateTuples = Wires.GENERATE_TUPLES;

    @AfterEach
    void restoreTuplesFlag() {
        Wires.setGenerateTuples(originalGenerateTuples);
    }

    @Test
    @DisplayName("Supports tuple field API and deep copy behaviour")
    void tupleSupportsFieldApiAndDeepCopy() throws InvalidMarshallableException, NoSuchFieldException {
        Wires.setGenerateTuples(true);
        SampleTuple tuple = Wires.tupleFor(SampleTuple.class, "sampleType");
        assertNotNull(tuple, "tuple instance should be created for sampleType conversion");

        tuple.setField("alpha", "one");
        tuple.setField("beta", 2L);
        assertEquals("one", tuple.getField("alpha", String.class), "alpha field should read back as string");
        assertEquals(2L, tuple.getField("beta", Long.class), "beta field should read back as long");
        assertEquals("sampleType", tuple.className(), "className should match tuple type");
        assertTrue(tuple.usesSelfDescribingMessage(), "tuple should use self-describing messages");

        int hash = tuple.hashCode();
        assertNotEquals(0, hash, "tuple hashCode should not be zero");
        assertEquals(tuple, tuple, "tuple should be equal to itself");

        SampleTuple copy = tuple.deepCopy();
        assertNotNull(copy, "deep copy should return a tuple instance");
        assertEquals(tuple.getField("alpha", String.class), copy.getField("alpha", String.class),
                "deep copy should preserve alpha field");

        List<FieldInfo> infos = tuple.$fieldInfos();
        assertEquals(2, infos.size(), "tuple should report two field infos");

        Bytes<?> buffer = Bytes.allocateElasticOnHeap();
        Wire textWire = WireType.TEXT.apply(buffer);
        tuple.writeMarshallable(textWire);

        buffer.readPositionRemaining(0, buffer.writePosition());
        SampleTuple readBack = Wires.tupleFor(SampleTuple.class, "sampleType");
        readBack.readMarshallable(textWire);
        assertEquals("one", readBack.getField("alpha", String.class),
                "readBack alpha field should match written value");
        buffer.releaseLast();
    }

    interface SampleTuple extends Marshallable {
        @Override
        void setField(String name, Object value);

        @Override
        <T> T getField(String name, Class<T> type);

        @Override
        List<FieldInfo> $fieldInfos();
    }
}
