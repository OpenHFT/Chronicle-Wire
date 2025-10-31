/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

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
        assertTrue(dump.contains("- first"));
        assertTrue(dump.contains("- 2"));
    }
}

