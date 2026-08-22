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
    public void explicitRecoveryReplacesEOFAndRemainsUsable() throws StreamCorruptedException {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            final Wire wire = WireType.BINARY.apply(bytes);
            final long eofPosition = 64;
            bytes.writePosition(eofPosition);
            bytes.writeInt(eofPosition, END_OF_DATA);

            assertEquals(eofPosition, ((net.openhft.chronicle.wire.domestic.InternalWire) wire)
                    .recoverFromEndOfData());
            assertEquals(-1, ((net.openhft.chronicle.wire.domestic.InternalWire) wire)
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

    private static void writeFramed(Wire wire, String value) throws StreamCorruptedException {
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
