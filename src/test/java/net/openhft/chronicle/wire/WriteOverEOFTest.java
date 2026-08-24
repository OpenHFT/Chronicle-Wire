/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.io.StreamCorruptedException;

import static net.openhft.chronicle.wire.Wires.END_OF_DATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class WriteOverEOFTest extends WireTestCommon {

    @Test
    public void ordinaryWriteRemainsSealedAtEOF() {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            final Wire wire = WireType.BINARY.apply(bytes);
            wire.usePadding(true);
            final long eofPosition = 64;
            bytes.writePosition(eofPosition);
            bytes.writeInt(eofPosition, END_OF_DATA);

            assertThrows(WriteAfterEOFException.class, () -> wire.enterHeader(128));
            assertFalse(((AbstractWire) wire).isInsideHeader());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void explicitRecoveryReplacesEOFAndRemainsUsable()
            throws StreamCorruptedException, WriteAfterEOFException {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            final Wire wire = WireType.BINARY.apply(bytes);
            wire.usePadding(true);
            final long eofPosition = 64;
            bytes.writePosition(eofPosition);
            bytes.writeInt(eofPosition, END_OF_DATA);

            assertTrue(((net.openhft.chronicle.wire.domestic.InternalWire) wire)
                    .recoverFromEndOfData());
            assertFalse(((net.openhft.chronicle.wire.domestic.InternalWire) wire)
                    .recoverFromEndOfData());
            writeFramed(wire, "recovered");
            writeFramed(wire, "after-recovery");

            bytes.readPosition(eofPosition);
            assertEquals("recovered", read(wire));
            assertEquals("after-recovery", read(wire));
            try (DocumentContext context = wire.readingDocument()) {
                assertFalse(context.isPresent());
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void recoveryDoesNotSearchPastTheCurrentWritePosition() {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            final Wire wire = WireType.BINARY.apply(bytes);
            wire.usePadding(true);
            final long currentPosition = 64;
            final long laterEofPosition = 72;
            bytes.writeInt(currentPosition, 4);
            bytes.writeInt(currentPosition + Integer.BYTES, 0x12345678);
            bytes.writeInt(laterEofPosition, END_OF_DATA);
            bytes.writePosition(currentPosition);

            assertFalse(((net.openhft.chronicle.wire.domestic.InternalWire) wire)
                    .recoverFromEndOfData());
            assertEquals(END_OF_DATA, bytes.readVolatileInt(laterEofPosition));
            assertEquals(currentPosition, bytes.writePosition());
        } finally {
            bytes.releaseLast();
        }
    }

    private static void writeFramed(Wire wire, String value)
            throws StreamCorruptedException, WriteAfterEOFException {
        final long headerPosition = wire.enterHeader(128);
        wire.write("value").text(value);
        wire.updateHeader(headerPosition, false, 0);
    }

    private static String read(Wire wire) {
        try (DocumentContext context = wire.readingDocument()) {
            assertTrue(context.isPresent());
            return context.wire().read("value").text();
        }
    }
}
