package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WriteDocumentContextTest {
    static void writeThreeKeys(Wire wire) {
        try (DocumentContext dc0 = wire.acquireWritingDocument(false)) {
            for (int i = 0; i < 3; i++) {
                try (DocumentContext dc = wire.acquireWritingDocument(false)) {
                    dc.wire().write("key").int32(i);
                }
                assertTrue(dc0.isNotComplete());
            }
        }
    }

    static void writeThreeChainedKeys(Wire wire) {
        for (int i = 0; i < 3; i++) {
            try (WriteDocumentContext dc = (WriteDocumentContext) wire.acquireWritingDocument(false)) {
                dc.wire().write("key").int32(i);
                dc.chainedElement(i < 2);
            }
        }
    }

    @Test
    public void nestedPlainText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        writeThreeKeys(wire);
        assertEquals("key: 0\n" +
                "key: 1\n" +
                "key: 2\n" +
                "...\n", wire.bytes().toString());
    }

    @Test
    public void chainedPlainText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        writeThreeChainedKeys(wire);
        assertEquals("key: 0\n" +
                "key: 1\n" +
                "key: 2\n" +
                "...\n", wire.bytes().toString());
    }

    @Test
    public void nestedText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useBinaryDocuments();
        writeThreeKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("key: 0\n" +
                "key: 1\n" +
                "key: 2\n", wire.bytes().toString());
    }

    @Test
    public void chainedText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useBinaryDocuments();
        writeThreeChainedKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("key: 0\n" +
                "key: 1\n" +
                "key: 2\n", wire.bytes().toString());
    }

    @Test
    public void nestedBinary() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        writeThreeKeys(wire);
        String s = Wires.fromSizePrefixedBlobs(wire);
        assertEquals("--- !!data #binary\n" +
                "key: 0\n" +
                "key: 1\n" +
                "key: 2\n", s);
    }

    @Test
    public void chainedBinary() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        writeThreeChainedKeys(wire);
        String s = Wires.fromSizePrefixedBlobs(wire);
        assertEquals("--- !!data #binary\n" +
                "key: 0\n" +
                "key: 1\n" +
                "key: 2\n", s);
    }
}
