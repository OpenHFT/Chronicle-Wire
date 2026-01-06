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

class TextWriteDocumentContextTest extends WireTestCommon {

    @Test
    @DisplayName("start and close append terminator for text wires")
    void startAndCloseAppendTerminatorForTextWires() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        TextWire wire = new TextWire(bytes);
        TextWriteDocumentContext context = new TextWriteDocumentContext(wire);

        context.start(false);
        wire.write("key").text("value");
        context.close();

        String text = bytes.toString();
        assertFalse(context.isNotComplete(), "text context should close after the final close call");
        assertTrue(text.contains("...\n"), "text output should contain '...\\n' terminator: " + text);

        bytes.releaseLast();
    }

    @Test
    @DisplayName("rollbackOnClose discards written data and resets positions")
    void rollbackOnCloseDiscardsWrittenData() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        TextWire wire = new TextWire(bytes);
        TextWriteDocumentContext context = new TextWriteDocumentContext(wire);

        context.start(false);
        wire.write("key").text("value");
        context.rollbackOnClose();
        context.close();

        assertEquals(bytes.readPosition(), bytes.writePosition(),
                "rollbackOnClose should reset write position to read position");
        assertFalse(context.isNotComplete(), "text context should close after rollbackOnClose");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("rollbackIfNotComplete closes the context and discards data")
    void rollbackIfNotCompleteClosesAndDiscardsData() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        TextWire wire = new TextWire(bytes);
        TextWriteDocumentContext context = new TextWriteDocumentContext(wire);

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
        TextWire wire = new TextWire(bytes);
        TextWriteDocumentContext context = new TextWriteDocumentContext(wire);

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
        TextWire wire = new TextWire(bytes);
        TextWriteDocumentContext context = new TextWriteDocumentContext(wire);

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
    @DisplayName("metadata start writes a meta-data comment line")
    void metadataStartWritesComment() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        TextWire wire = new TextWire(bytes);
        TextWriteDocumentContext context = new TextWriteDocumentContext(wire);

        context.start(true);
        String text = bytes.toString();

        assertTrue(text.contains("meta-data"), "text output should contain 'meta-data' comment: " + text);
        assertTrue(context.isEmpty(), "metadata comment should not count as data");

        bytes.releaseLast();
    }
}
