/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.bytes.MethodReader.MESSAGE_HISTORY_METHOD_ID;
import static org.junit.jupiter.api.Assertions.*;

public class MethodWriterHistoryTest extends WireTestCommon {

    interface Events {
        void event(CharSequence value);
    }

    static class RecordingTextWire extends TextWire {
        RecordingTextWire(Bytes<?> bytes) {
            super(bytes);
            useTextDocuments();
        }

        @Override
        public boolean recordHistory() {
            return true;
        }
    }

    @Test
    void historyEventIsPrependedWhenRecordingEnabled() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        Wire wire = new RecordingTextWire(bytes);
        try {
            VanillaMessageHistory suppliedHistory = new VanillaMessageHistory();
            suppliedHistory.reset(7, 11);
            suppliedHistory.addTiming(123L);
            suppliedHistory.addSourceDetails(false);
            MessageHistory.set(suppliedHistory);

            ClassAliasPool.CLASS_ALIASES.addAlias(VanillaMessageHistory.class);
            Events writer = wire.methodWriter(Events.class);
            writer.event("payload");

            bytes.readPositionRemaining(0, bytes.writePosition());
            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent());
                long historyEventId = dc.wire().readEventNumber();
                assertEquals(MESSAGE_HISTORY_METHOD_ID, historyEventId);
                VanillaMessageHistory captured = dc.wire().getValueIn().object(VanillaMessageHistory.class);
                assertEquals(suppliedHistory.sources(), captured.sources());
                assertEquals(suppliedHistory.sourceId(0), captured.sourceId(0));

                StringBuilder callName = new StringBuilder();
                ValueIn callValue = dc.wire().readEventName(callName);
                assertEquals("event", callName.toString());
                assertEquals("payload", callValue.text());
            }
        } finally {
            MessageHistory.clear();
            bytes.releaseLast();
        }
    }
}
