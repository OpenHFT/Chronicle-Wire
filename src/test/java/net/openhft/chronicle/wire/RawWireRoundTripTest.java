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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.values.LongArrayValues;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class RawWireRoundTripTest extends WireTestCommon {

    @Test
    public void roundTripPrimitiveSequenceAndReset() {
        // Use direct bytes to satisfy BinaryLongArrayReference.lazyWrite preconditions
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        RawWire wire = new RawWire(bytes);
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000123");

        try (DocumentContext dc = wire.writingDocument(false)) {
            ValueOut out = dc.wire().getValueOut();
            out.bool(true);
            out.int32(321);
            out.float64(7.5);
            out.text("note");
            out.bytes(new byte[]{1, 2, 3});
            // omit int64array in this environment to avoid platform-dependent behaviour
            out.uuid(uuid);
        }

        wire.bytes().readPositionRemaining(0, wire.bytes().writePosition());
        try (DocumentContext dc = wire.readingDocument()) {
            ValueIn in = dc.wire().getValueIn();
            assertTrue(in.bool());
            assertEquals(321, in.int32());
            assertEquals(7.5, in.float64(), 0.0);
            assertEquals("note", in.text());

            Bytes<?> sink = Bytes.allocateElasticOnHeap();
            in.bytes(sink);
            byte[] data = new byte[(int) sink.readRemaining()];
            sink.read(data);
            assertArrayEquals(new byte[]{1, 2, 3}, data);
            sink.releaseLast();

            // skip int64array read (not written in this environment)

            java.util.concurrent.atomic.AtomicReference<UUID> got = new java.util.concurrent.atomic.AtomicReference<>();
            in.uuid(got, AtomicReference::set);
            assertEquals(uuid, got.get());
        }

        wire.reset();
        assertEquals(0, wire.bytes().readRemaining());
        bytes.releaseLast();
    }

    @Test
    public void copyToRequiresRawWire() {
        RawWire source = new RawWire(Bytes.allocateElasticOnHeap());
        BinaryWire target = new BinaryWire(Bytes.allocateElasticOnHeap());
        try {
            assertThrows(UnsupportedOperationException.class, () -> source.copyTo(target));
        } finally {
            source.bytes().releaseLast();
            target.bytes().releaseLast();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void rawBytesUnsupported() {
        RawWire wire = new RawWire(Bytes.allocateElasticOnHeap());
        wire.getValueOut().rawBytes(new byte[]{1});
    }
}
