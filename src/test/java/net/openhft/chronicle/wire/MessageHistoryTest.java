/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageHistoryTest extends WireTestCommon {

    @Test
    @DisplayName("writeHistory writes history when the document is empty")
    void writeHistoryWritesWhenEmpty() {
        MessageHistory.emptyHistory();
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        Wire wire = WireType.TEXT.apply(bytes);

        try {
            TestWriteDocumentContext context = new TestWriteDocumentContext(wire, true);
            MessageHistory.writeHistory(context);

            assertTrue(bytes.readRemaining() > 0, "MessageHistory should write into an empty document");
        } finally {
            MessageHistory.clear();
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeHistory skips writing when document is not empty")
    void writeHistorySkipsNonEmptyDocument() {
        MessageHistory.emptyHistory();
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        Wire wire = WireType.TEXT.apply(bytes);

        try {
            TestWriteDocumentContext context = new TestWriteDocumentContext(wire, false);
            MessageHistory.writeHistory(context);

            assertEquals(0, bytes.readRemaining(), "MessageHistory should leave a non-empty document unchanged");
        } finally {
            MessageHistory.clear();
            bytes.releaseLast();
        }
    }

    private static final class TestWriteDocumentContext implements WriteDocumentContext {
        private final Wire wire;
        private final boolean empty;
        private boolean chained;
        private boolean metaData;

        private TestWriteDocumentContext(Wire wire, boolean empty) {
            this.wire = wire;
            this.empty = empty;
        }

        @Override
        public void start(boolean metaData) {
            this.metaData = metaData;
        }

        @Override
        public boolean chainedElement() {
            return chained;
        }

        @Override
        public void chainedElement(boolean chainedElement) {
            this.chained = chainedElement;
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }

        @Override
        public boolean isMetaData() {
            return metaData;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public Wire wire() {
            return wire;
        }

        @Override
        public boolean isNotComplete() {
            return false;
        }

        @Override
        public void close() {
        }

        @Override
        public void reset() {
        }

        @Override
        public int sourceId() {
            return -1;
        }

        @Override
        public long index() throws IORuntimeException {
            return -1L;
        }
    }
}
