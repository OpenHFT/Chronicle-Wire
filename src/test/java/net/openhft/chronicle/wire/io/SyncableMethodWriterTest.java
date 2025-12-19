/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.io;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.OnHeapBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.Syncable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.YamlWire;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class SyncableMethodWriterTest extends net.openhft.chronicle.wire.WireTestCommon {

    private static final String EXPECTED = "say: hello\n" +
            "...\n" +
            "sync: \"\"\n" +
            "# sync\n" +
            "...\n" +
            "say: world\n" +
            "...\n" +
            "sync: \"\"\n" +
            "# sync\n" +
            "...\n";

    // A custom interface combining message sending (say) and synchronization capabilities (sync)
    interface SayAndSync extends Syncable {
        void say(String say);
    }

    // A specialized YamlWire that has synchronization capabilities
    static class SyncableYamlWire extends YamlWire implements Syncable {
        SyncableYamlWire(@NotNull Bytes<?> bytes) {
            super(bytes);
            useTextDocuments();
        }

        // Override the sync method to write a comment and then invoke the sync on the underlying bytes
        @Override
        public void sync() {
            writeComment("sync");
            Syncable.syncIfAvailable(bytes());
        }
    }

    // Test the ability to use the custom method writer to write a message and then synchronize the wire
    @Test
    public void sayAndSync() {
        final OnHeapBytes bytes = Bytes.allocateElasticOnHeap();
        try {
            assertEquals(EXPECTED, doTest(bytes), "sayAndSync: output");
        } finally {
            bytes.releaseLast();
        }
    }

    // Core logic for testing the say and sync operations, encapsulated for reuse
    private String doTest(Bytes<?> bytes) {
        Wire wire = new SyncableYamlWire(bytes);
        SayAndSync sas = wire.methodWriter(SayAndSync.class);
        sas.say("hello");
        sas.sync();
        sas.say("world");
        sas.sync();
        return wire.toString();
    }

    // Test the say and sync operations but this time with a MappedBytes instance which maps bytes to a file
    @Test
    public void sayAndSyncMappedBytes() throws FileNotFoundException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        final File file = IOTools.createTempFile("sayAndSyncMappedBytes");
        file.deleteOnExit();
        try (MappedBytes mb = MappedBytes.mappedBytes(file, OS.pageSize())) {
            assertEquals(EXPECTED, doTest(mb), "sayAndSyncMappedBytes: output");
        }
    }
}
