/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.PointerBytesStore;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.*;

public class DefaultValueInCoverageTest extends WireTestCommon {

    @Test
    public void handlesBytesPointerAndBooleanBranches() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        DefaultValueIn valueIn = new DefaultValueIn(wire);
        PointerBytesStore pointer = new PointerBytesStore();

        valueIn.defaultValue = null;
        assertSame(wire, valueIn.bytesSet(pointer));
        assertEquals(0, pointer.safeLimit());

        Bytes<?> direct = Bytes.allocateDirect(32);
        direct.write("hi".getBytes(ISO_8859_1));
        valueIn.defaultValue = direct.bytesStore();
        valueIn.bytesSet(pointer);
        assertTrue("pointer should point at direct store", pointer.safeLimit() >= 2);
        direct.releaseLast();
    }

    @Test
    public void suppliesDefaultValuesAcrossReaders() throws InvalidMarshallableException {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        DefaultValueIn valueIn = new DefaultValueIn(wire);

        valueIn.defaultValue = Boolean.TRUE;
        AtomicBoolean flag = new AtomicBoolean();
        valueIn.bool(flag, AtomicBoolean::set);
        assertTrue(flag.get());
        assertTrue(valueIn.bool());

        valueIn.defaultValue = 123L;
        assertEquals(123, valueIn.int32());
        assertEquals(123L, valueIn.int64());
        assertEquals(123.0, valueIn.float64(), 0.0);

        valueIn.defaultValue = UUID.fromString("00000000-0000-0000-0000-000000000007");
        UUID uuid = valueIn.uuid();
        assertEquals("00000000-0000-0000-0000-000000000007", uuid.toString());

        Bytes<?> data = Bytes.from("data");
        Bytes<?> sink = Bytes.allocateElasticOnHeap();
        valueIn.defaultValue = data.bytesStore();
        valueIn.bytes(sink);
        byte[] out = new byte[(int) sink.readRemaining()];
        sink.read(out);
        assertArrayEquals("data".getBytes(ISO_8859_1), out);
        data.releaseLast();
        sink.releaseLast();
    }

    @Test
    public void sequenceAndMarshallableDelegatesInvocations() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        DefaultValueIn valueIn = new DefaultValueIn(wire);
        valueIn.defaultValue = null;

        AtomicReference<ValueIn> seen = new AtomicReference<>();
        valueIn.sequence("holder", "ignored", (h, k, v) -> seen.set(v));
        assertSame(valueIn, seen.get());

        AtomicReference<Bytes<?>> marshallable = new AtomicReference<>();
        valueIn.bytes(bytesIn -> marshallable.set(bytesIn.bytesForRead()));
        // Identity of the returned Bytes view is not guaranteed; verify emptiness instead
        assertNotNull(marshallable.get());
        assertEquals(0L, marshallable.get().readRemaining());
    }

    @Test
    public void objectConversionFollowsClassLookup() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        DefaultValueIn valueIn = new DefaultValueIn(wire);

        assertSame(wire.classLookup(), valueIn.classLookup());

        try (BinaryLongArrayReference values = new BinaryLongArrayReference(2)) {
            valueIn.defaultValue = values;
            assertSame(values, valueIn.applyToMarshallable(in -> values));
        }
    }
}
