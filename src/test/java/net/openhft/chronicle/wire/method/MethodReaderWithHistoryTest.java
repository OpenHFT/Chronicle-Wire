/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.utils.RecordHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test class extending WireTestCommon to test MethodReader functionality with history recording
class MethodReaderWithHistoryTest extends WireTestCommon {

    // Test with text wire type
    @Test
    @DisplayName("Method reader preserves history on TEXT wire")
    void text() {
        assertTrue(doTest(WireType.TEXT), "method reader with history: wireType=TEXT");
    }

    // Test with YAML-only wire type
    @Test
    @DisplayName("Method reader preserves history on YAML_ONLY wire")
    void yaml() {
        assertTrue(doTest(WireType.YAML_ONLY), "method reader with history: wireType=YAML_ONLY");
    }

    // Test with binary wire type
    @Test
    @DisplayName("Method reader preserves history on BINARY wire")
    void binary() {
        assertTrue(doTest(WireType.BINARY), "method reader with history: wireType=BINARY");
    }

    // Helper method to perform tests with different wire types
    private boolean doTest(WireType wireType) {
        // Create a new wire instance of the specified wire type
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());

        // Create a method writer for the RecordHistorySays interface
        RecordHistorySays historySays = wire.methodWriter(RecordHistorySays.class);

        // Initialize a message history instance
        net.openhft.chronicle.wire.VanillaMessageHistory history = new net.openhft.chronicle.wire.VanillaMessageHistory();

        // Record and write the first history and message
        history.reset();
        history.addSource(1, 11);
        historySays.history(history).say("hello");

        // Record and write the second history and message
        history.reset();
        history.addSource(2, 22);
        historySays.history(history).say("bye");

        String[] says = {null};
        MethodReader reader = wire.methodReader((RecordHistorySays) h -> {
            assertEquals(1, h.sourceId(0), "First history source id should be 1");
            assertEquals(11, h.sourceIndex(0), "First history source index should be 11");
            return (Saying) s -> says[0] = s;
        });
        assertTrue(reader.readOne(), "Reader should process first history message");
        assertEquals("hello", says[0], "First message payload should be hello");

        // Read and validate the second message and its history
        String[] says2 = {null};
        MethodReader reader2 = wire.methodReader((RecordHistorySays) h -> {
            assertEquals(2, h.sourceId(0), "Second history source id should be 2");
            assertEquals(22, h.sourceIndex(0), "Second history source index should be 22");
            return (Saying) s -> says2[0] = s;
        });

        assertTrue(reader2.readOne(), "Reader should process second history message");
        assertEquals("bye", says2[0], "Second message payload should be bye");
        return true;
    }

    // Interface for a saying action
    interface Saying {
        void say(String say);
    }

    // Interface extending RecordHistory for the Saying interface
    public interface RecordHistorySays extends RecordHistory<Saying> {
    }
}
