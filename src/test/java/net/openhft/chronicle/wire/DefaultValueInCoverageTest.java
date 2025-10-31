/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
        direct.write("hi".getBytes(StandardCharsets.ISO_8859_1));
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
        assertArrayEquals("data".getBytes(StandardCharsets.ISO_8859_1), out);
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
        valueIn.bytes((ReadBytesMarshallable) bytesIn -> marshallable.set(bytesIn.bytesForRead()));
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
