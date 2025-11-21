/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Illustrates Chronicle-Queue style copying of binary fragments into a textual representation.
 */
public class BinaryWireReadWithLengthTest extends WireTestCommon {

    @Test
    public void copiesMapFragmentToText() {
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

        assertTrue(target.bytes().toString().contains("key: 1"));
    }

    @Test
    public void copiesSequenceFragmentToText() {
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
        assertTrue(dump.contains("first"));
        assertTrue(dump.contains("2"));
    }

    @Test
    public void copyEntireWireToText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        writer.writeEventName("say").text("hello");
        writer.writeEventName("number").int32(42);

        bytes.readPositionRemaining(0, bytes.writePosition());
        Wire textWire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        new BinaryWire(bytes).copyTo(textWire);
        String output = textWire.bytes().toString();

        assertTrue(output.contains("say: hello"));
        assertTrue(output.contains("number: 42"));
    }

    @Test
    public void copyMessagesIndividually() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        writer.writeEventName("alpha").text("one");
        writer.writeEventName("beta").int32(2);

        bytes.readPositionRemaining(0, bytes.writePosition());
        Wire textWire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        BinaryWire source = new BinaryWire(bytes);

        source.copyOne(textWire);
        String first = textWire.bytes().toString();
        assertFalse(first.isEmpty());
    }
}
