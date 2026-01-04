/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.PointerBytesStore;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultValueInCoverageTest extends WireTestCommon {

    @Test
    @DisplayName("Handles bytes pointer and boolean branches with defaults")
    public void handlesBytesPointerAndBooleanBranches() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        DefaultValueIn valueIn = new DefaultValueIn(wire);
        PointerBytesStore pointer = new PointerBytesStore();

        valueIn.defaultValue = null;
        assertSame(wire, valueIn.bytesSet(pointer),
                "bytesSet should return wire for null default value");
        assertEquals(0, pointer.safeLimit(),
                "pointer safe limit should be zero when default value is null");

        Bytes<?> direct = Bytes.allocateDirect(32);
        direct.write("hi".getBytes(ISO_8859_1));
        valueIn.defaultValue = direct.bytesStore();
        valueIn.bytesSet(pointer);
        assertTrue(pointer.safeLimit() >= 2, "pointer should point at direct store");
        direct.releaseLast();
    }

    @Test
    @DisplayName("Supplies default values across readers and primitives")
    public void suppliesDefaultValuesAcrossReaders() throws InvalidMarshallableException {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        DefaultValueIn valueIn = new DefaultValueIn(wire);

        valueIn.defaultValue = Boolean.TRUE;
        AtomicBoolean flag = new AtomicBoolean();
        valueIn.bool(flag, AtomicBoolean::set);
        assertTrue(flag.get(), "boolean default should set AtomicBoolean");
        assertTrue(valueIn.bool(), "bool() should return true when default value is true");

        valueIn.defaultValue = 123L;
        assertEquals(123, valueIn.int32(), "int32 should return default numeric value");
        assertEquals(123L, valueIn.int64(), "int64 should return default numeric value");
        assertEquals(123.0, valueIn.float64(), 0.0, "float64 should return default numeric value");

        valueIn.defaultValue = UUID.fromString("00000000-0000-0000-0000-000000000007");
        UUID uuid = valueIn.uuid();
        assertEquals("00000000-0000-0000-0000-000000000007", uuid.toString(),
                "uuid should match default value");

        Bytes<?> data = Bytes.from("data");
        Bytes<?> sink = Bytes.allocateElasticOnHeap();
        valueIn.defaultValue = data.bytesStore();
        valueIn.bytes(sink);
        byte[] out = new byte[(int) sink.readRemaining()];
        sink.read(out);
        assertArrayEquals("data".getBytes(ISO_8859_1), out,
                "bytes should copy default bytes store contents");
        data.releaseLast();
        sink.releaseLast();
    }

    @Test
    @DisplayName("Delegates sequence and marshallable invocations correctly")
    public void sequenceAndMarshallableDelegatesInvocations() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        DefaultValueIn valueIn = new DefaultValueIn(wire);
        valueIn.defaultValue = null;

        AtomicReference<ValueIn> seen = new AtomicReference<>();
        valueIn.sequence("holder", "ignored", (h, k, v) -> seen.set(v));
        assertSame(valueIn, seen.get(),
                "sequence should provide DefaultValueIn instance");

        AtomicReference<Bytes<?>> marshallable = new AtomicReference<>();
        valueIn.bytes(bytesIn -> marshallable.set(bytesIn.bytesForRead()));
        // Identity of the returned Bytes view is not guaranteed; verify emptiness instead
        assertNotNull(marshallable.get(),
                "bytes callback should supply bytes view");
        assertEquals(0L, marshallable.get().readRemaining(),
                "bytes view should be empty for null default");
    }

    @Test
    @DisplayName("Uses class lookup when converting default object")
    public void objectConversionFollowsClassLookup() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        DefaultValueIn valueIn = new DefaultValueIn(wire);

        assertSame(wire.classLookup(), valueIn.classLookup(),
                "class lookup should match wire class lookup");

        try (BinaryLongArrayReference values = new BinaryLongArrayReference(2)) {
            valueIn.defaultValue = values;
            assertSame(values, valueIn.applyToMarshallable(in -> values),
                    "applyToMarshallable should return provided default marshallable");
        }
    }
}
