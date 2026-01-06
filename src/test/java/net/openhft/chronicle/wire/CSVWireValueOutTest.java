/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CSVWireValueOutTest extends WireTestCommon {

    @Test
    @DisplayName("CSV ignores null type literals when writing values")
    void typeLiteralNullReturnsWire() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.appendUtf8("header\n");
        bytes.readPosition(0);
        try {
            CSVWire wire = new CSVWire(bytes);
            ValueOut out = wire.getValueOut();
            assertSame(wire, out.typeLiteral((CharSequence) null),
                    "Null type literals should be ignored for CSV");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("CSV rejects non-null type literals for value output")
    void typeLiteralRejectsNonNull() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.appendUtf8("header\n");
        bytes.readPosition(0);
        try {
            CSVWire wire = new CSVWire(bytes);
            ValueOut out = wire.getValueOut();
            assertThrows(UnsupportedOperationException.class,
                    () -> out.typeLiteral("type"),
                    "CSV should reject non-null type literals");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("CSV rejects serialisable objects for marshalling")
    void marshallableRejectsSerializable() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.appendUtf8("header\n");
        bytes.readPosition(0);
        try {
            CSVWire wire = new CSVWire(bytes);
            ValueOut out = wire.getValueOut();
            assertThrows(UnsupportedOperationException.class,
                    () -> out.marshallable(new DummySerializable()),
                    "CSV should reject serializable objects");
        } finally {
            bytes.releaseLast();
        }
    }

    private static final class DummySerializable implements Serializable {
        private static final long serialVersionUID = 0L;
    }
}
