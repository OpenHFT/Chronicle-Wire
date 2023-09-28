/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WriteDocumentContextTest extends WireTestCommon {
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
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n" +
                        "...\n",
                wire.bytes().toString());
    }

    @Test
    public void chainedPlainText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        writeThreeChainedKeys(wire);
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n" +
                        "...\n",
                wire.bytes().toString());
    }

    @Test
    public void nestedText() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        writeThreeKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n",
                wire.bytes().toString());
    }

    @Test
    public void chainedText() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        writeThreeChainedKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n",
                wire.bytes().toString());
    }

    @Ignore(/* TODO FIX */)
    @Test
    public void nestedYaml() {
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());

        writeThreeKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n",
                wire.bytes().toString());
    }

    @Ignore(/* TODO FIX */)
    @Test
    public void chainedYaml() {
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());

        writeThreeChainedKeys(wire);
        assertEquals(21, wire.bytes().readInt());
        assertEquals("" +
                        "key: 0\n" +
                        "key: 1\n" +
                        "key: 2\n",
                wire.bytes().toString());
    }

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
