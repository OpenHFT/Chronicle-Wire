/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentContextHolderTest extends WireTestCommon {

    @Test
    @DisplayName("close is safe when the holder has no active context")
    void closeIsSafeWhenNoContextIsSet() {
        DocumentContextHolder holder = new DocumentContextHolder();

        holder.close();

        assertTrue(holder.isClosed(), "Holder stays closed without a context");
    }

    @Test
    @DisplayName("close clears the holder when context is complete")
    void closeClearsCompletedContext() {
        DocumentContextHolder holder = new DocumentContextHolder();
        RecordingContext context = new RecordingContext(false);
        holder.documentContext(context);

        holder.close();

        assertTrue(context.closed, "completed context should receive a close call");
        assertTrue(holder.isClosed(), "holder should clear a completed context");
    }

    @Test
    @DisplayName("close keeps the holder when context is not complete")
    void closeKeepsNotCompleteContext() {
        DocumentContextHolder holder = new DocumentContextHolder();
        RecordingContext context = new RecordingContext(true);
        holder.documentContext(context);

        holder.close();

        assertTrue(context.closed, "not-complete context should still receive a close call");
        assertFalse(holder.isClosed(), "holder should keep the context when not complete");
        assertSame(context, holder.documentContext(), "holder should retain the active context instance");
    }

    @Test
    @DisplayName("reset clears the holder and resets the context")
    void resetClearsAndResetsContext() {
        DocumentContextHolder holder = new DocumentContextHolder();
        RecordingContext context = new RecordingContext(false);
        holder.documentContext(context);

        holder.reset();

        assertTrue(context.reset, "context reset should be invoked by the holder");
        assertTrue(holder.isClosed(), "holder should clear the context after reset");
    }

    @Test
    @DisplayName("holder delegates write-context methods to the current context")
    void delegatesWriteContextMethods() {
        DocumentContextHolder holder = new DocumentContextHolder();
        RecordingContext context = new RecordingContext(false);
        holder.documentContext(context);

        holder.start(true);
        holder.chainedElement(true);

        assertTrue(context.metaData, "holder should forward metadata start to the context");
        assertTrue(holder.chainedElement(), "holder should expose chained element state");
        assertEquals(1, context.chainedCalls, "holder should forward chained element calls");
        assertTrue(holder.isEmpty(), "holder should delegate isEmpty to the context");
    }

    @Test
    @DisplayName("delegates source metadata and exposes context")
    void delegatesSourceMetadataAndExposesContext() {
        DocumentContextHolder holder = new DocumentContextHolder();
        RecordingContext context = new RecordingContext(false);
        context.sourceId = 7;
        context.index = 42L;
        holder.documentContext(context);

        assertSame(context, holder.documentContext(), "holder should expose the current context");
        assertEquals(7, holder.sourceId(), "holder should delegate the source id value");
        assertEquals(42L, holder.index(), "holder should delegate the index value");
    }

    private static final class RecordingContext implements WriteDocumentContext {
        private final boolean notComplete;
        private boolean metaData;
        private boolean chained;
        private boolean closed;
        private boolean reset;
        private int chainedCalls;
        private int sourceId = -1;
        private long index = -1L;

        private RecordingContext(boolean notComplete) {
            this.notComplete = notComplete;
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
            this.chainedCalls++;
        }

        @Override
        public boolean isEmpty() {
            return true;
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
            return null;
        }

        @Override
        public boolean isNotComplete() {
            return notComplete;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void reset() {
            reset = true;
        }

        @Override
        public int sourceId() {
            return sourceId;
        }

        @Override
        public long index() throws IORuntimeException {
            return index;
        }
    }
}
