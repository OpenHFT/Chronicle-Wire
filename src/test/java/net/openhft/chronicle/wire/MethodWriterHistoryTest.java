/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.ValueIn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.bytes.MethodReader.MESSAGE_HISTORY_METHOD_ID;
import static org.junit.jupiter.api.Assertions.*;

class MethodWriterHistoryTest extends WireTestCommon {

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
    @DisplayName("History event is prepended when recording is enabled")
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
                assertTrue(dc.isPresent(), "History document should be present");
                long historyEventId = dc.wire().readEventNumber();
                assertEquals(MESSAGE_HISTORY_METHOD_ID, historyEventId,
                        "History event id should match MESSAGE_HISTORY_METHOD_ID");
                VanillaMessageHistory captured = dc.wire().getValueIn().object(VanillaMessageHistory.class);
                assertEquals(suppliedHistory.sources(), captured.sources(),
                        "Captured history should have the same source count");
                assertEquals(suppliedHistory.sourceId(0), captured.sourceId(0),
                        "Captured history should retain the source id");

                StringBuilder callName = new StringBuilder();
                ValueIn callValue = dc.wire().readEventName(callName);
                assertEquals("event", callName.toString(), "Event name should follow history");
                assertEquals("payload", callValue.text(), "Event payload should follow history");
            }
        } finally {
            MessageHistory.clear();
            bytes.releaseLast();
        }
    }
}
