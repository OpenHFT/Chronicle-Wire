/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentContextDefaultMethodsTest extends WireTestCommon {

    @Test
    @DisplayName("isData reflects presence and metadata flags")
    void isDataReflectsPresentAndMetaData() {
        assertTrue(new FixedDocumentContext(true, false).isData(),
                "isData is true for present non-metadata entries");
        assertFalse(new FixedDocumentContext(false, false).isData(),
                "isData is false when the entry is not present");
        assertFalse(new FixedDocumentContext(true, true).isData(),
                "isData is false for metadata entries");
    }

    private static final class FixedDocumentContext implements DocumentContext {
        private final boolean present;
        private final boolean metaData;

        private FixedDocumentContext(boolean present, boolean metaData) {
            this.present = present;
            this.metaData = metaData;
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
            return 0;
        }
    }
}
