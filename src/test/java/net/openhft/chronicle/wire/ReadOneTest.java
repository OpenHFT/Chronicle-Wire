/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * This class is used to test the functionality related to reading individual messages and snapshots
 * from a Wire-based data structure, ensuring they can be read in the correct sequence.
 */
class ReadOneTest extends WireTestCommon {

    // Basic test for reading without scanning the wire
    @Test
    @DisplayName("ReadOne returns snapshot without scanning enabled")
    void test() throws InterruptedException {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for readOne snapshot test");

        assertEquals("two", doTest(false), "Snapshot should return the latest value without scanning");
    }

    // Test for reading the wire using scanning
    @Test
    @DisplayName("ReadOne returns snapshot with scanning enabled")
    void testScanning() throws InterruptedException {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for readOne scanning test");

        assertEquals("two", doTest(true), "Snapshot should return the latest value with scanning enabled");
    }

    // Core testing method that simulates writing to and reading from the Wire
    private String doTest(boolean scanning) {
        ignoreException("Unknown @MethodId='history' called on interface net.openhft.chronicle.wire.ReadOneTest$SnapshotListener");
        ignoreException("Unknown method-name='myDto' called on interface net.openhft.chronicle.wire.ReadOneTest$SnapshotListener");
        // Initialization phase
        Wire wire = new YamlWire() {
            @Override
            public boolean recordHistory() {
                return true;
            }
        };

        MyDtoListener myOut = wire.methodWriterBuilder(MyDtoListener.class).build();
        final SnapshotListener snapshotOut = wire.methodWriterBuilder(SnapshotListener.class).build();

        ((VanillaMessageHistory) MessageHistory.get()).useBytesMarshallable(false);
        // Simulating different historical records and writes to the Wire
        generateHistory(1);
        myOut.myDto(new MyDto());

        generateHistory(2);
        snapshotOut.snapshot(new SnapshotDTO().data("one"));

        generateHistory(3);
        myOut.myDto(new MyDto());

        generateHistory(4);
        myOut.myDto(new MyDto());

        generateHistory(5);
        snapshotOut.snapshot(new SnapshotDTO().data("two"));

        generateHistory(6);
        myOut.myDto(new MyDto());

        // Reading phase to check the data written to the Wire
        SnapshotDTO[] q = {null};

        MethodReader reader = wire.methodReaderBuilder()
                .scanning(scanning)
                .build((SnapshotListener) d -> q[0] = d);

        if (!scanning) {
            // 1
            assertTrue(reader.readOne(), "Step 1 should read a non-snapshot event");
        }
        // 2
        assertTrue(reader.readOne(), "Step 2 should read the first snapshot");
        assertNotNull(q[0], "Snapshot should be populated at step 2");
        assertEquals("one", q[0].data, "Snapshot payload should be one");
        q[0] = null;

        if (!scanning) {
            // 3
            assertTrue(reader.readOne(), "Step 3 should read a non-snapshot event");
            // 4
            assertTrue(reader.readOne(), "Step 4 should read a non-snapshot event");
        }
        // 5
        assertTrue(reader.readOne(), "Step 5 should read the second snapshot");
        assertNotNull(q[0], "Snapshot should be populated at step 5");
        assertEquals("two", q[0].data, "Snapshot payload should be two");

        if (!scanning) {
            // 6
            assertTrue(reader.readOne(), "Step 6 should read a non-snapshot event");
        }
        assertFalse(reader.readOne(), "Reader should have no more events");
        return q[0].data;
    }

    // Utility method to simulate the history of messages
    @NotNull
    private VanillaMessageHistory generateHistory(int value) {
        VanillaMessageHistory messageHistory = (VanillaMessageHistory) MessageHistory.get();
        messageHistory.reset();
        messageHistory.addSource(value, value);
        return messageHistory;
    }

    // Listener interface for MyDto to react when a MyDto is read
    interface MyDtoListener {
        void myDto(MyDto dto);
    }

    // Listener interface for SnapshotDTO to react when a SnapshotDTO is read
    interface SnapshotListener {
        void snapshot(SnapshotDTO dto);
    }

    // Definition for MyDto class, used for testing reading data from the Wire
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class MyDto extends SelfDescribingMarshallable {
        String data;
    }

    // Definition for SnapshotDTO class, used to represent snapshots in the test
    static class SnapshotDTO extends SelfDescribingMarshallable {
        String data;

        public String data() {
            return data;
        }

        SnapshotDTO data(String data) {
            this.data = data;
            return this;
        }
    }
}
