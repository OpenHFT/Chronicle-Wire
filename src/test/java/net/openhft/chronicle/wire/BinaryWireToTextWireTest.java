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
 * Mirrors Chronicle-Queue behaviour where binary messages are copied into a textual representation
 * for tooling and diagnostics.
 */
public class BinaryWireToTextWireTest extends WireTestCommon {

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
        // At least one message should have been emitted
        assertTrue(first.length() > 0);
    }
}
