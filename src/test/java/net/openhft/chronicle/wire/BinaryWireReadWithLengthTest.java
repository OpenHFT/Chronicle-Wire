/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Illustrates Chronicle-Queue style copying of binary fragments into a textual representation.
 */
class BinaryWireReadWithLengthTest extends WireTestCommon {

    @Test
    @DisplayName("Copies map fragment to text wire")
    void copiesMapFragmentToText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        try (DocumentContext dc = writer.writingDocument(false)) {
            dc.wire().write("map").marshallable(m -> m.write("key").int32(1));
        }

        bytes.readPositionRemaining(0, bytes.writePosition());
        int header = bytes.readInt();
        int len = Wires.lengthOf(header);
        long bodyPos = bytes.readPosition();

        TextWire target = new TextWire(Bytes.allocateElasticOnHeap());
        BinaryWire source = new BinaryWire(bytes);
        source.bytes().readPosition(bodyPos);
        source.readWithLength(target, len);

        String text = target.bytes().toString();
        assertTrue(text.contains("key: 1"), "Expected map fragment to include key: 1, got: " + text);
    }

    @Test
    @DisplayName("Copies sequence fragment to text wire")
    void copiesSequenceFragmentToText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        try (DocumentContext dc = writer.writingDocument(false)) {
            dc.wire().write("seq").sequence(v -> {
                v.text("first");
                v.int32(2);
            });
        }

        bytes.readPositionRemaining(0, bytes.writePosition());
        int header = bytes.readInt();
        int len = Wires.lengthOf(header);
        long bodyPos = bytes.readPosition();

        TextWire target = new TextWire(Bytes.allocateElasticOnHeap());
        BinaryWire source = new BinaryWire(bytes);
        source.bytes().readPosition(bodyPos);
        source.readWithLength(target, len);

        String dump = target.bytes().toString();
        assertTrue(dump.contains("first"), "Expected sequence text to include first, got: " + dump);
        assertTrue(dump.contains("2"), "Expected sequence text to include 2, got: " + dump);
    }

    @Test
    @DisplayName("Copies entire wire into text output")
    void copyEntireWireToText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        writer.writeEventName("say").text("hello");
        writer.writeEventName("number").int32(42);

        bytes.readPositionRemaining(0, bytes.writePosition());
        Wire textWire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        new BinaryWire(bytes).copyTo(textWire);
        String output = textWire.bytes().toString();

        assertTrue(output.contains("say: hello"), "Expected output to include say: hello, got: " + output);
        assertTrue(output.contains("number: 42"), "Expected output to include number: 42, got: " + output);
    }

    @Test
    @DisplayName("Copies messages one by one into text wire")
    void copyMessagesIndividually() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        writer.writeEventName("alpha").text("one");
        writer.writeEventName("beta").int32(2);

        bytes.readPositionRemaining(0, bytes.writePosition());
        Wire textWire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        BinaryWire source = new BinaryWire(bytes);

        source.copyOne(textWire);
        String first = textWire.bytes().toString();
        assertFalse(first.isEmpty(), "Expected copyOne to write first message into text wire, got empty output");
    }
}
