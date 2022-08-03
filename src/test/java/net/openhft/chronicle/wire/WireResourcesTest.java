/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.StreamCorruptedException;
import java.nio.file.Files;

import static net.openhft.chronicle.core.io.ReferenceOwner.INIT;
import static org.junit.Assert.assertEquals;

public class WireResourcesTest extends WireTestCommon {

    @Test
    public void testMappedBytesClose() throws Exception {
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        MappedBytes mb0;
        @NotNull MappedBytes mb = MappedBytes.mappedBytes(tmp, 64 * 1024);
        assertEquals(1, mb.mappedFile().refCount());
        assertEquals(1, mb.refCount());

        Wire wire = WireType.TEXT.apply(mb);

        assert wire.startUse();
        wire.headerNumber(0);

        assertEquals(1, mb.mappedFile().refCount());
        wire.writeFirstHeader(); // first touches the file.
        assertEquals(1, mb.mappedFile().refCount());

        wire.updateFirstHeader();
        assert wire.endUse();

        assertEquals(1, mb.mappedFile().refCount());
        assertEquals(1, mb.refCount());

        mb0 = mb;
        mb.releaseLast();
        BackgroundResourceReleaser.releasePendingResources();

        assertEquals(0, mb0.mappedFile().refCount());
        assertEquals(0, mb0.refCount());
    }

    @Test
    public void testMappedBytesWireRelease() throws Exception {
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        Wire wire;
        @NotNull MappedBytes mb = MappedBytes.mappedBytes(tmp, 64 * 1024);
        assertEquals(1, mb.mappedFile().refCount());
        assertEquals(1, mb.refCount());

        wire = WireType.TEXT.apply(mb);
        ReferenceOwner test = ReferenceOwner.temporary("test");
        wire.bytes().reserve(test);

        assertEquals(1, mb.mappedFile().refCount());
        assertEquals(2, mb.refCount());
        mb.release(INIT);

        assertEquals(1, wire.bytes().refCount());

        assert wire.startUse();
        wire.headerNumber(1);
        wire.writeFirstHeader();
        wire.updateFirstHeader();
        assert wire.endUse();

        wire.bytes().releaseLast(test);
        BackgroundResourceReleaser.releasePendingResources();
        assertEquals(0, wire.bytes().refCount());
    }

    @Test
    public void testMappedBytesWireRelease2() throws Exception {
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        @NotNull MappedBytes t = MappedBytes.mappedBytes(tmp, 256 * 1024);
        assertEquals(1, t.refCount());
        assertEquals(1, t.mappedFile().refCount());
        Wire wire = WireType.TEXT.apply(t);

        assertEquals(1, t.refCount()); // not really using it yet
        assertEquals(1, t.mappedFile().refCount());

        assert wire.startUse();
        wire.headerNumber(1);
        assertEquals(1, t.refCount());
        assertEquals(1, t.mappedFile().refCount()); // not really using it yet

        wire.writeFirstHeader();
        assertEquals(1, wire.bytes().refCount());
        assertEquals(1, t.mappedFile().refCount()); // now there is a mapping used as well as use
        // in MappedBytes

        assertEquals(1, wire.bytes().refCount());
        assertEquals(1, mappedFile(wire).refCount());

        wire.bytes().writeSkip(128000);
        wire.updateFirstHeader();
        assert wire.endUse();

        writeMessage(wire);

        assertEquals(1, mappedFile(wire).refCount());

        writeMessage(wire);

        // new block
        assertEquals(1, mappedFile(wire).refCount());

        writeMessage(wire);

        assertEquals(1, mappedFile(wire).refCount());

        writeMessage(wire);

        assertEquals(1, mappedFile(wire).refCount());

        wire.bytes().releaseLast();
        BackgroundResourceReleaser.releasePendingResources();
        // the MappedFile was created by MappedBytes
        // so when it is fully released, the MappedFile is close()d
        assertEquals(0, wire.bytes().refCount());
        assertEquals(0, t.refCount());
        assertEquals(0, mappedFile(wire).refCount());
    }

    private static void writeMessage(@NotNull Wire wire) throws StreamCorruptedException {
        try (DocumentContext dc = wire.writingDocument()) {
            final Bytes<?> bytes = dc.wire().bytes();
            bytes.writeSkip(128000);
            bytes.writeLong(1L);
        }
    }

    @NotNull
    protected MappedFile mappedFile(@NotNull Wire wire) {
        return ((MappedBytes) wire.bytes()).mappedFile();
    }
}
