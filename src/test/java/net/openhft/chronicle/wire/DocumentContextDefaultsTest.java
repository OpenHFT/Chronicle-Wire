/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentContextDefaultsTest extends WireTestCommon {

    @Test
    @DisplayName("isData returns true only when document context is present and not metadata")
    void isDataRequiresPresentAndNonMetadata() {
        assertTrue(new SimpleContext(true, false, false).isData(),
                "DocumentContext is data when present and not metadata");
        assertFalse(new SimpleContext(false, false, false).isData(),
                "DocumentContext is not data when not present");
        assertFalse(new SimpleContext(true, true, false).isData(),
                "DocumentContext is not data when metadata");
    }

    @Test
    @DisplayName("isOpen reports the notComplete state for the context")
    void isOpenMirrorsNotComplete() {
        assertTrue(new SimpleContext(true, false, true).isOpen(),
                "DocumentContext is open when not complete");
        assertFalse(new SimpleContext(true, false, false).isOpen(),
                "DocumentContext is not open when complete");
    }

    @Test
    @DisplayName("rollbackIfNotComplete throws UnsupportedOperationException by default in context")
    void rollbackIfNotCompleteThrowsByDefault() {
        SimpleContext context = new SimpleContext(true, false, false);
        assertThrows(UnsupportedOperationException.class, context::rollbackIfNotComplete,
                "DocumentContext rollbackIfNotComplete is unsupported by default");
    }

    @Test
    @DisplayName("rollbackOnClose is a no-op for the default context")
    void rollbackOnCloseIsNoOpByDefault() {
        SimpleContext context = new SimpleContext(true, false, false);
        context.rollbackOnClose();
        assertTrue(context.isPresent(), "DocumentContext rollbackOnClose does not alter state");
    }

    private static final class SimpleContext implements DocumentContext {
        private final boolean present;
        private final boolean metaData;
        private final boolean notComplete;

        private SimpleContext(boolean present, boolean metaData, boolean notComplete) {
            this.present = present;
            this.metaData = metaData;
            this.notComplete = notComplete;
        }

        @Override
        public boolean isMetaData() {
            return metaData;
        }

        @Override
        public boolean isPresent() {
            return present;
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
