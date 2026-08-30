/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static net.openhft.chronicle.wire.Wires.END_OF_DATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

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
            assertEquals(eofPosition, bytes.writePosition());
            assertEquals(END_OF_DATA, bytes.readVolatileInt(eofPosition));

            //! Repeating the refusal is the observable purpose of clearing insideHeader: EOF remains the
            //! storage condition, rather than being masked by stale state from the first header attempt.
            assertThrows(WriteAfterEOFException.class, () -> wire.enterHeader(128));
            assertFalse(((AbstractWire) wire).isInsideHeader());
            assertEquals(eofPosition, bytes.writePosition());
            assertEquals(END_OF_DATA, bytes.readVolatileInt(eofPosition));
        } finally {
            bytes.releaseLast();
        }
    }
}
