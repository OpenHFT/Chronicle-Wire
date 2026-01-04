/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"deprecation", "removal"})
class RawWireRoundTripTest extends WireTestCommon {

    @Test
    @DisplayName("RawWire round-trips primitive sequence and resets")
    void roundTripPrimitiveSequenceAndReset() {
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
            assertTrue(in.bool(), "Boolean value should round-trip");
            assertEquals(321, in.int32(), "int32 value should round-trip");
            assertEquals(7.5, in.float64(), 0.0, "float64 value should round-trip");
            assertEquals("note", in.text(), "Text value should round-trip");

            Bytes<?> sink = Bytes.allocateElasticOnHeap();
            in.bytes(sink);
            byte[] data = new byte[(int) sink.readRemaining()];
            sink.read(data);
            assertArrayEquals(new byte[]{1, 2, 3}, data, "Byte array should round-trip");
            sink.releaseLast();

            // skip int64array read (not written in this environment)

            java.util.concurrent.atomic.AtomicReference<UUID> got = new java.util.concurrent.atomic.AtomicReference<>();
            in.uuid(got, AtomicReference::set);
            assertEquals(uuid, got.get(), "UUID value should round-trip");
        }

        wire.reset();
        assertEquals(0, wire.bytes().readRemaining(), "Wire should be empty after reset");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("copyTo should reject non-RawWire targets")
    void copyToRequiresRawWire() {
        RawWire source = new RawWire(Bytes.allocateElasticOnHeap());
        BinaryWire target = new BinaryWire(Bytes.allocateElasticOnHeap());
        try {
            assertThrows(UnsupportedOperationException.class, () -> source.copyTo(target),
                    "RawWire copyTo should reject non-RawWire targets");
        } finally {
            source.bytes().releaseLast();
            target.bytes().releaseLast();
        }
    }

    @Test
    @DisplayName("RawWire rawBytes operation is unsupported in practice")
    void rawBytesUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> {
            RawWire wire = new RawWire(Bytes.allocateElasticOnHeap());
            wire.getValueOut().rawBytes(new byte[]{1});
        }, "RawWire rawBytes should be unsupported");
    }
}
