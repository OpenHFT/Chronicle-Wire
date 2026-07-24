/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

/**
 * This class is used to test the functionality related to reading individual messages and snapshots
 * from a Wire-based data structure, ensuring they can be read in the correct sequence.
 */
public class ReadOneTest extends WireTestCommon {

    // Definition for MyDto class, used for testing reading data from the Wire
    static class MyDto extends SelfDescribingMarshallable {
        String data;
    }

    // Listener interface for MyDto to react when a MyDto is read
    interface MyDtoListener {
        void myDto(MyDto dto);
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

    // Listener interface for SnapshotDTO to react when a SnapshotDTO is read
    interface SnapshotListener {
        void snapshot(SnapshotDTO dto);
    }

    // Basic test for reading without scanning the wire
    @Test
    public void test() throws InterruptedException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        doTest(false);
    }

    // Test for reading the wire using scanning
    @Test
    public void testScanning() throws InterruptedException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        doTest(true);
    }

    // Core testing method that simulates writing to and reading from the Wire
    private void doTest(boolean scanning) {
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
        SnapshotListener snapshotOut = wire.methodWriterBuilder(SnapshotListener.class).build();

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
            assertTrue(reader.readOne());
        }
        // 2
        assertTrue(reader.readOne());
        assertNotNull(q[0]);
        assertEquals("one", q[0].data);
        q[0] = null;

        if (!scanning) {
            // 3
            assertTrue(reader.readOne());
            // 4
            assertTrue(reader.readOne());
        }
        // 5
        assertTrue(reader.readOne());
        assertNotNull(q[0]);
        assertEquals("two", q[0].data);

        if (!scanning) {
            // 6
            assertTrue(reader.readOne());
        }
        assertFalse(reader.readOne());
    }

    // Utility method to simulate the history of messages
    @NotNull
    private VanillaMessageHistory generateHistory(int value) {
        VanillaMessageHistory messageHistory = (VanillaMessageHistory) MessageHistory.get();
        messageHistory.reset();
        messageHistory.addSource(value, value);
        return messageHistory;
    }
}
