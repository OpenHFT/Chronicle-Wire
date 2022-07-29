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

public class SyncableMethodWriterTest {
    interface SayAndSync extends Syncable {
        void say(String say);
    }

    static class SyncableYamlWire extends YamlWire implements Syncable {
        public SyncableYamlWire(@NotNull Bytes<?> bytes) {
            super(bytes);
            useTextDocuments();
        }

        @Override
        public void sync() {
            writeComment("sync");
            Syncable.syncIfAvailable(bytes());
        }
    }

    @Test
    public void sayAndSync() {
        final OnHeapBytes bytes = Bytes.allocateElasticOnHeap();
        doTest(bytes);
    }

    private void doTest(Bytes bytes) {
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

    @Test
    public void sayAndSyncMappedBytes() throws FileNotFoundException {
        final File file = IOTools.createTempFile("sayAndSyncMappedBytes");
        file.deleteOnExit();
        try (MappedBytes mb = MappedBytes.mappedBytes(file, OS.pageSize())) {
            doTest(mb);
        }
    }
}
