/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                assertTrue(dc0.isNotComplete(),
                        "Top level document should stay incomplete for key index " + i);
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
    @DisplayName("Nested plain text writes three key entries")
    public void nestedPlainText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        writeThreeKeys(wire);
        assertEquals("key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n" +
                        "...\n",
                wire.bytes().toString(),
                "Plain text nested output should match expected document");
    }

    // Test writing chained key-value pairs in plain text format
    @Test
    @DisplayName("Chained plain text writes three key entries")
    public void chainedPlainText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        writeThreeChainedKeys(wire);
        assertEquals("key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n" +
                        "...\n",
                wire.bytes().toString(),
                "Plain text chained output should match expected document");
    }

    // Test writing nested key-value pairs in TextWire format
    @Test
    @DisplayName("Nested text wire writes three key entries")
    public void nestedText() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        writeThreeKeys(wire);
        assertEquals(21,
                wire.bytes().readInt(),
                "Text wire should prefix length before entries");
        assertEquals("key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n",
                wire.bytes().toString(),
                "Text wire nested entries should match output");
    }

    // Test writing chained key-value pairs in TextWire format
    @Test
    @DisplayName("Chained text wire writes three key entries")
    public void chainedText() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        writeThreeChainedKeys(wire);
        assertEquals(21,
                wire.bytes().readInt(),
                "Text wire should prefix length for chained entries");
        assertEquals("key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n",
                wire.bytes().toString(),
                "Text wire chained entries should match output");
    }

    // Test writing nested key-value pairs in YAML format (Currently Ignored)
    @Test
    @Disabled("TODO fix YAML length prefix handling for documents")
    @DisplayName("Nested YAML wire writes three key entries")
    public void nestedYaml() {
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());

        writeThreeKeys(wire);
        assertEquals(21,
                wire.bytes().readInt(),
                "YAML wire should prefix length before entries");
        assertEquals("key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n",
                wire.bytes().toString(),
                "YAML wire nested entries should match output");
    }

    // Test writing chained key-value pairs in YAML format (Currently Ignored)
    @Test
    @Disabled("TODO fix YAML chained length prefix handling")
    @DisplayName("Chained YAML wire writes three key entries")
    public void chainedYaml() {
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());

        writeThreeChainedKeys(wire);
        assertEquals(21,
                wire.bytes().readInt(),
                "YAML wire should prefix length for chained entries");
        assertEquals("key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n",
                wire.bytes().toString(),
                "YAML wire chained entries should match output");
    }

    // Test writing nested key-value pairs in BinaryWire format
    @Test
    @DisplayName("Nested binary wire writes three key entries")
    public void nestedBinary() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        writeThreeKeys(wire);
        String s = Wires.fromSizePrefixedBlobs(wire);
        assertEquals("--- !!data #binary\n" +
                "key: 0\n" +
                "key: 1\n" +
                "key: 2\n",
                s,
                "Binary wire should emit expected data header");
    }

    // Test writing chained key-value pairs in BinaryWire format
    @Test
    @DisplayName("Chained binary wire writes three key entries")
    public void chainedBinary() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        writeThreeChainedKeys(wire);
        String s = Wires.fromSizePrefixedBlobs(wire);
        assertEquals("--- !!data #binary\n" +
                "key: 0\n" +
                "key: 1\n" +
                "key: 2\n",
                s,
                "Binary wire should emit expected chained data header");
    }
}
