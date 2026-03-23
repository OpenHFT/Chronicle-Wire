/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WriteDocumentContextTest extends WireTestCommon {

    // Writes three key-value pairs to the given Wire using nested DocumentContexts
    private static void writeThreeKeys(Wire wire) {
        // Acquire a top-level writing document
        try (DocumentContext dc0 = wire.acquireWritingDocument(false)) {
            // Write three key-value pairs using nested DocumentContexts
            for (int i = 0; i < 3; i++) {
                try (DocumentContext dc = wire.acquireWritingDocument(false)) {
                    dc.wire().write("key").int32(i);
                }
                // Validate that the top-level document is not complete yet
                assertTrue(dc0.isNotComplete());
            }
        }
    }

    // Writes three key-value pairs to the given Wire using chained DocumentContexts
    private static void writeThreeChainedKeys(Wire wire) {
        // Write three key-value pairs and mark each as a chained element except the last
        for (int i = 0; i < 3; i++) {
            try (WriteDocumentContext dc = (WriteDocumentContext) wire.acquireWritingDocument(false)) {
                dc.wire().write("key").int32(i);
                dc.chainedElement(i < 2);
            }
        }
    }

    // Test writing nested key-value pairs in plain text format
    @Test
    public void nestedPlainText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        writeThreeKeys(wire);
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n" +
                        "...\n", wire.bytes().toString());
    }

    // Test writing chained key-value pairs in plain text format
    @Test
    public void chainedPlainText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        writeThreeChainedKeys(wire);
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n" +
                        "...\n", wire.bytes().toString());
    }

    // Test writing nested key-value pairs in TextWire format
    @Test
    public void nestedText() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        writeThreeKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n", wire.bytes().toString());
    }

    // Test writing chained key-value pairs in TextWire format
    @Test
    public void chainedText() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        writeThreeChainedKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n", wire.bytes().toString());
    }

    // Test writing nested key-value pairs in YAML format (Currently Ignored)
    @Disabled(/* TODO FIX */)
    @Test
    public void nestedYaml() {
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());

        writeThreeKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n", wire.bytes().toString());
    }

    // Test writing chained key-value pairs in YAML format (Currently Ignored)
    @Disabled(/* TODO FIX */)
    @Test
    public void chainedYaml() {
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());

        writeThreeChainedKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n", wire.bytes().toString());
    }

    // Test writing nested key-value pairs in BinaryWire format
    @Test
    public void nestedBinary() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        writeThreeKeys(wire);
        String s = Wires.fromSizePrefixedBlobs(wire);
        assertEquals("" +
                "--- !!data #binary\n" +
                "key: 0\n" +
                "key: 1\n" +
                "key: 2\n", s);
    }

    // Test writing chained key-value pairs in BinaryWire format
    @Test
    public void chainedBinary() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        writeThreeChainedKeys(wire);
        String s = Wires.fromSizePrefixedBlobs(wire);
        assertEquals("" +
                "--- !!data #binary\n" +
                "key: 0\n" +
                "key: 1\n" +
                "key: 2\n", s);
    }
}
