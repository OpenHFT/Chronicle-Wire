/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import static net.openhft.chronicle.wire.WireOut.EndOfWire.*;

import static org.junit.Assert.*;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.threads.TimingPauser;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;

public class RecoveredEndOfWireTest extends WireTestCommon {

    private static class Source implements AutoCloseable {
        final Bytes<?> bytes = Bytes.allocateDirect(128);
        final BinaryWire wire = new BinaryWire(bytes);

        Source(int header) {
            bytes.writeInt(0, header);
            bytes.writePosition(0);
        }

        void timeout() throws Exception {
            TimingPauser pauser =
                    (TimingPauser)
                            Proxy.newProxyInstance(
                                    TimingPauser.class.getClassLoader(),
                                    new Class<?>[] {TimingPauser.class},
                                    (proxy, method, args) -> {
                                        if (method.getName().equals("pause")
                                                && args != null
                                                && args.length == 2)
                                            throw new TimeoutException("probe");
                                        return null;
                                    });
            Field field = AbstractWire.class.getDeclaredField("timedParser");
            field.setAccessible(true);
            field.set(wire, pauser);
        }

        public void close() {
            bytes.releaseLast();
        }
    }

    @Test
    public void readOnlyEofProbeDoesNotModifyEmptyHeader() throws Exception {
        try (Source s = new Source(0)) {
            assertEquals(NOT_PRESENT, s.wire.endOfWire(false, 1, TimeUnit.MILLISECONDS, 0));
            assertEquals(0, s.bytes.readInt(0));
        }
    }

    @Test
    public void emptyHeaderReportsEofPublication() throws Exception {
        try (Source s = new Source(0)) {
            assertEquals(PRESENT_AFTER_UPDATE, s.wire.endOfWire(true, 1, TimeUnit.MILLISECONDS, 0));
            assertEquals(Wires.END_OF_DATA, s.bytes.readInt(0));
        }
    }

    @Test
    public void existingEofIsNotRepublished() throws Exception {
        try (Source s = new Source(Wires.END_OF_DATA)) {
            assertEquals(PRESENT, s.wire.endOfWire(true, 1, TimeUnit.MILLISECONDS, 0));
            assertEquals(0, s.bytes.writePosition());
        }
    }

    @Test
    public void timedOutWriterReportsEofPublication() throws Exception {
        expectException("resetting header after timeout");
        try (Source s = new Source(Wires.NOT_COMPLETE)) {
            s.timeout();
            assertEquals(PRESENT_AFTER_UPDATE, s.wire.endOfWire(true, 1, TimeUnit.MILLISECONDS, 0));
            assertEquals(Wires.END_OF_DATA, s.bytes.readInt(0));
        }
    }

    @Test
    public void booleanEofResultReportsForcedPublication() throws Exception {
        expectException("resetting header after timeout");
        try (Source s = new Source(Wires.NOT_COMPLETE)) {
            s.timeout();
            assertTrue(s.wire.writeEndOfWire(1, TimeUnit.MILLISECONDS, 0));
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void unalignedEofAfterCompleteDocumentIsPublished() throws Exception {
        try (Source s = new Source(1)) {
            s.wire.usePadding(false);
            s.bytes.writeByte(4, (byte) 7);
            s.bytes.writeInt(5, 0);
            assertEquals(PRESENT_AFTER_UPDATE, s.wire.endOfWire(true, 1, TimeUnit.MILLISECONDS, 0));
            assertEquals(Wires.END_OF_DATA, s.bytes.readInt(5));
        }
    }

    @Test
    public void readOnlyProbeLeavesPendingWriterUntouched() throws Exception {
        try (Source s = new Source(Wires.NOT_COMPLETE)) {
            assertEquals(NOT_PRESENT, s.wire.endOfWire(false, 1, TimeUnit.MILLISECONDS, 0));
            assertEquals(Wires.NOT_COMPLETE, s.bytes.readInt(0));
        }
    }
}
