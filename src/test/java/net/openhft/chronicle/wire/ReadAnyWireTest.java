/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadAnyWireTest extends WireTestCommon {

    @Test
    @DisplayName("underlyingWire returns null when fewer than 8 bytes")
    void underlyingWireReturnsNullWhenBytesTooShort() {
        Bytes<?> bytes = Bytes.from("abc");
        try {
            ReadAnyWire wire = new ReadAnyWire(bytes);
            assertNull(wire.underlyingWire(), "ReadAnyWire cannot resolve type with fewer than 8 bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("underlyingWire resolves to TextWire for ASCII input")
    void underlyingWireResolvesToTextWire() {
        Bytes<?> bytes = Bytes.from("abcdefgh");
        try {
            ReadAnyWire wire = new ReadAnyWire(bytes);
            assertTrue(wire.underlyingWire() instanceof TextWire,
                    "ReadAnyWire resolves ASCII input to TextWire");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("underlyingWire resolves to BinaryWire for high-bit input")
    void underlyingWireResolvesToBinaryWire() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(16);
        bytes.writeByte((byte) 0x80);
        bytes.writeSkip(7);
        bytes.readPosition(0);
        try {
            ReadAnyWire wire = new ReadAnyWire(bytes);
            assertTrue(wire.underlyingWire() instanceof BinaryWire,
                    "ReadAnyWire resolves high-bit input to BinaryWire");
        } finally {
            bytes.releaseLast();
        }
    }
}
