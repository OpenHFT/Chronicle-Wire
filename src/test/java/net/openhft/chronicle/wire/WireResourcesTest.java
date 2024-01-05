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

    // Test the process of closing mapped bytes and ensuring their resources are properly released.
    @Test
    public void testMappedBytesClose() throws Exception {
        // Create a temporary file for the test and ensure it's deleted afterwards.
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        // Initialize and verify initial reference counts.
        MappedBytes mb0;
        @NotNull MappedBytes mb = MappedBytes.mappedBytes(tmp, 64 * 1024);
        assertEquals(1, mb.mappedFile().refCount());
        assertEquals(1, mb.refCount());

        // Apply text wire type to the mapped bytes.
        Wire wire = WireType.TEXT.apply(mb);

        wire.headerNumber(0);

        // Check reference counts after setting the header and updating it.
        assertEquals(1, mb.mappedFile().refCount());
        wire.writeFirstHeader(); // This operation first touches the file.
        assertEquals(1, mb.mappedFile().refCount());

        wire.updateFirstHeader();

        // Verify the reference counts after header update.
        assertEquals(1, mb.mappedFile().refCount());
        assertEquals(1, mb.refCount());

        // Release the mapped bytes and verify that all resources are released.
        mb0 = mb;
        mb.releaseLast();
        BackgroundResourceReleaser.releasePendingResources();

        assertEquals(0, mb0.mappedFile().refCount());
        assertEquals(0, mb0.refCount());
    }

    // Test the process of releasing mapped bytes via wire and ensuring their resources are properly released.
    @Test
    public void testMappedBytesWireRelease() throws Exception {
        // Create a temporary file for the test and ensure it's deleted afterwards.
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        // Initialize the mapped bytes and verify the initial reference counts.
        Wire wire;
        @NotNull MappedBytes mb = MappedBytes.mappedBytes(tmp, 64 * 1024);
        assertEquals(1, mb.mappedFile().refCount());
        assertEquals(1, mb.refCount());

        // Apply text wire type to the mapped bytes and reserve additional references.
        wire = WireType.TEXT.apply(mb);
        ReferenceOwner test = ReferenceOwner.temporary("test");
        wire.bytes().reserve(test);

        // Verify reference counts after reservation.
        assertEquals(1, mb.mappedFile().refCount());
        assertEquals(2, mb.refCount());
        mb.release(INIT);

        // Verify the reference count of wire's bytes.
        assertEquals(1, wire.bytes().refCount());

        // Set and update the header.
        wire.headerNumber(1);
        wire.writeFirstHeader();
        wire.updateFirstHeader();

        // Release the wire's bytes and ensure all resources are released.
        wire.bytes().releaseLast(test);
        BackgroundResourceReleaser.releasePendingResources();
        assertEquals(0, wire.bytes().refCount());
    }

    @Test
    // Test the process of releasing mapped bytes with multiple message writings and ensuring their resources are properly released.
    public void testMappedBytesWireRelease2() throws Exception {
        // Create a temporary file for the test and ensure it's deleted afterwards.
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        // Initialize the mapped bytes with a size of 256 KB and verify the initial reference counts.
        @NotNull MappedBytes t = MappedBytes.mappedBytes(tmp, 256 * 1024);
        assertEquals(1, t.refCount());
        assertEquals(1, t.mappedFile().refCount());
        Wire wire = WireType.TEXT.apply(t);

        // Check reference counts after initializing the wire.
        assertEquals(1, t.refCount());
        assertEquals(1, t.mappedFile().refCount());

        wire.headerNumber(1);
        assertEquals(1, t.refCount());
        assertEquals(1, t.mappedFile().refCount());

        // Set the header of the wire and verify reference counts.
        wire.writeFirstHeader();
        assertEquals(1, wire.bytes().refCount());
        assertEquals(1, t.mappedFile().refCount()); // now there is a mapping used as well as use
        // in MappedBytes

        assertEquals(1, wire.bytes().refCount());
        assertEquals(1, mappedFile(wire).refCount());

        wire.bytes().writeSkip(128000);
        wire.updateFirstHeader();

        writeMessage(wire);

        assertEquals(1, mappedFile(wire).refCount());

        writeMessage(wire);

        // new block
        assertEquals(1, mappedFile(wire).refCount());

        writeMessage(wire);

        assertEquals(1, mappedFile(wire).refCount());

        writeMessage(wire);

        assertEquals(1, mappedFile(wire).refCount());

        // Release resources associated with the wire.
        wire.bytes().releaseLast();
        BackgroundResourceReleaser.releasePendingResources();
        // the MappedFile was created by MappedBytes
        // so when it is fully released, the MappedFile is close()d
        assertEquals(0, wire.bytes().refCount());
        assertEquals(0, t.refCount());
        assertEquals(0, mappedFile(wire).refCount());
    }

    // Helper method to write a message into the given wire.
    private static void writeMessage(@NotNull Wire wire) throws StreamCorruptedException {
        try (DocumentContext dc = wire.writingDocument()) {
            final Bytes<?> bytes = dc.wire().bytes();
            bytes.writeSkip(128000);
            bytes.writeLong(1L);
        }
    }

    // Helper method to retrieve the MappedFile associated with the given wire.
    @NotNull
    protected MappedFile mappedFile(@NotNull Wire wire) {
        return ((MappedBytes) wire.bytes()).mappedFile();
    }
}
