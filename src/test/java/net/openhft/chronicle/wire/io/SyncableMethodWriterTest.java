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

package net.openhft.chronicle.wire.io;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.OnHeapBytes;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.Syncable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.YamlWire;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;

public class SyncableMethodWriterTest extends net.openhft.chronicle.wire.WireTestCommon {

    // A custom interface combining message sending (say) and synchronization capabilities (sync)
    interface SayAndSync extends Syncable {
        void say(String say);
    }

    // A specialized YamlWire that has synchronization capabilities
    static class SyncableYamlWire extends YamlWire implements Syncable {
        public SyncableYamlWire(@NotNull Bytes<?> bytes) {
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
        doTest(bytes);
    }

    // Core logic for testing the say and sync operations, encapsulated for reuse
    private void doTest(Bytes<?> bytes) {
        Wire wire = new SyncableYamlWire(bytes);
        SayAndSync sas = wire.methodWriter(SayAndSync.class);
        sas.say("hello");
        sas.sync();
        sas.say("world");
        sas.sync();
        assertEquals("" +
                "say: hello\n" +
                "...\n" +
                "sync: \"\"\n" +
                "# sync\n" +
                "...\n" +
                "say: world\n" +
                "...\n" +
                "sync: \"\"\n" +
                "# sync\n" +
                "...\n", wire.toString());
    }

    // Test the say and sync operations but this time with a MappedBytes instance which maps bytes to a file
    @Test
    public void sayAndSyncMappedBytes() throws FileNotFoundException {
        final File file = IOTools.createTempFile("sayAndSyncMappedBytes");
        file.deleteOnExit();
        try (MappedBytes mb = MappedBytes.mappedBytes(file, OS.pageSize())) {
            doTest(mb);
        }
    }
}
