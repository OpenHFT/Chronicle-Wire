/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinaryWriteDocumentContextTest extends WireTestCommon {

    @Test
    @DisplayName("start and close complete a binary document")
    void startAndCloseCompleteBinaryDocument() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryWire wire = new BinaryWire(bytes);
        TestBinaryWriteDocumentContext context = new TestBinaryWriteDocumentContext(wire);

        context.start(false);
        assertTrue(context.isNotComplete(), "Binary context is open after start");
        assertTrue(context.isEmpty(), "Binary context is empty before writes");

        wire.write("key").text("value");
        assertFalse(context.isEmpty(), "Binary context is not empty after writes");

        context.close();
        assertFalse(context.isNotComplete(), "Binary context closes after final close");
        assertTrue(bytes.writePosition() > context.positionValue(), "Binary context writes data and header");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("metadata start sets the metadata flag on the context")
    void metadataStartSetsMetadataFlag() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryWire wire = new BinaryWire(bytes);
        TestBinaryWriteDocumentContext context = new TestBinaryWriteDocumentContext(wire);

        context.start(true);

        assertTrue(context.isMetaData(), "Binary context records metadata flag");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("rollbackOnClose discards written data and resets positions")
    void rollbackOnCloseDiscardsWrittenData() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryWire wire = new BinaryWire(bytes);
        TestBinaryWriteDocumentContext context = new TestBinaryWriteDocumentContext(wire);

        context.start(false);
        wire.write("key").text("value");
        context.rollbackOnClose();
        context.close();

        assertEquals(bytes.readPosition(), bytes.writePosition(),
                "rollbackOnClose should reset write position to read position");
        assertFalse(context.isNotComplete(), "binary context should close after rollbackOnClose");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("rollbackIfNotComplete closes the context and discards data")
    void rollbackIfNotCompleteClosesAndDiscardsData() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryWire wire = new BinaryWire(bytes);
        TestBinaryWriteDocumentContext context = new TestBinaryWriteDocumentContext(wire);

        context.start(false);
        wire.write("key").text("value");
        context.rollbackIfNotComplete();

        assertEquals(bytes.readPosition(), bytes.writePosition(),
                "rollbackIfNotComplete should reset write position to read position");
        assertFalse(context.isNotComplete(), "rollbackIfNotComplete should clear the notComplete flag");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("chained element keeps the context open after close call")
    void chainedElementSkipsClose() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryWire wire = new BinaryWire(bytes);
        TestBinaryWriteDocumentContext context = new TestBinaryWriteDocumentContext(wire);

        context.start(false);
        wire.write("key").text("value");
        context.chainedElement(true);
        context.close();

        assertTrue(context.isNotComplete(), "chained element should keep the context not complete");
        assertTrue(bytes.readRemaining() > 0, "chained element should keep the written data available");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("nested start requires two closes to complete the context")
    void nestedStartRequiresTwoCloses() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryWire wire = new BinaryWire(bytes);
        TestBinaryWriteDocumentContext context = new TestBinaryWriteDocumentContext(wire);

        context.start(false);
        context.start(false);
        wire.write("key").text("value");

        context.close();
        assertTrue(context.isNotComplete(), "first close should leave the context open");

        context.close();
        assertFalse(context.isNotComplete(), "second close should complete the context");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("checkResetOpened resets the notComplete flag after start")
    void checkResetOpenedClearsNotComplete() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryWire wire = new BinaryWire(bytes);
        TestBinaryWriteDocumentContext context = new TestBinaryWriteDocumentContext(wire);

        context.start(false);
        assertTrue(context.isNotComplete(), "binary context should start in the notComplete state");

        assertFalse(context.checkResetOpened(), "checkResetOpened should return false when already open");
        assertFalse(context.isNotComplete(), "checkResetOpened should clear the notComplete flag");

        bytes.releaseLast();
    }

    private static final class TestBinaryWriteDocumentContext extends BinaryWriteDocumentContext {
        private TestBinaryWriteDocumentContext(Wire wire) {
            super(wire);
        }

        private long positionValue() {
            return position;
        }
    }
}
