/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static net.openhft.chronicle.core.io.ReferenceOwner.INIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class WireResourcesTest extends WireTestCommon {

    // Helper method to write a message into the given wire.
    private static void writeMessage(@NotNull Wire wire) {
        try (DocumentContext dc = wire.writingDocument()) {
            final Bytes<?> bytes = dc.wire().bytes();
            bytes.writeSkip(128000);
            bytes.writeLong(1L);
        }
    }

    @BeforeEach
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for resource tests");
    }

    // Test the process of closing mapped bytes and ensuring their resources are properly released.
    @Test
    @DisplayName("MappedBytes close releases resources and ref counts")
    public void testMappedBytesClose() throws Exception {
        // Create a temporary file for the test and ensure it's deleted afterwards.
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        // Initialize and verify initial reference counts.
        final MappedBytes mb0;
        @NotNull MappedBytes mb = MappedBytes.mappedBytes(tmp, 64 * 1024);
        assertEquals(1, mb.mappedFile().refCount(),
                "close test: mapped file ref count should start at 1");
        assertEquals(1, mb.refCount(),
                "close test: mapped bytes ref count should start at 1");

        // Apply text wire type to the mapped bytes.
        Wire wire = WireType.TEXT.apply(mb);

        wire.headerNumber(0);

        // Check reference counts after setting the header and updating it.
        assertEquals(1, mb.mappedFile().refCount(),
                "close test: mapped file ref count should remain 1 after headerNumber");
        wire.writeFirstHeader(); // This operation first touches the file.
        assertEquals(1, mb.mappedFile().refCount(),
                "close test: mapped file ref count should remain 1 after writeFirstHeader");

        wire.updateFirstHeader();

        // Verify the reference counts after header update.
        assertEquals(1, mb.mappedFile().refCount(),
                "close test: mapped file ref count should remain 1 after updateFirstHeader");
        assertEquals(1, mb.refCount(),
                "close test: mapped bytes ref count should remain 1 after updateFirstHeader");

        // Release the mapped bytes and verify that all resources are released.
        mb0 = mb;
        mb.releaseLast();
        BackgroundResourceReleaser.releasePendingResources();

        assertEquals(0, mb0.mappedFile().refCount(),
                "close test: mapped file ref count should be 0 after release");
        assertEquals(0, mb0.refCount(),
                "close test: mapped bytes ref count should be 0 after release");
    }

    // Test the process of releasing mapped bytes via wire and ensuring their resources are properly released.
    @Test
    @DisplayName("Wire release drops mapped bytes references")
    public void testMappedBytesWireRelease() throws Exception {
        // Create a temporary file for the test and ensure it's deleted afterwards.
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        // Initialize the mapped bytes and verify the initial reference counts.
        final Wire wire;
        @NotNull MappedBytes mb = MappedBytes.mappedBytes(tmp, 64 * 1024);
        assertEquals(1, mb.mappedFile().refCount(),
                "wire release test: mapped file ref count should start at 1");
        assertEquals(1, mb.refCount(),
                "wire release test: mapped bytes ref count should start at 1");

        // Apply text wire type to the mapped bytes and reserve additional references.
        wire = WireType.TEXT.apply(mb);
        ReferenceOwner test = ReferenceOwner.temporary("test");
        wire.bytes().reserve(test);

        // Verify reference counts after reservation.
        assertEquals(1, mb.mappedFile().refCount(),
                "wire release test: mapped file ref count should remain 1 after reserve");
        assertEquals(2, mb.refCount(),
                "wire release test: mapped bytes ref count should include reserved reference");
        mb.release(INIT);

        // Verify the reference count of wire's bytes.
        assertEquals(1, wire.bytes().refCount(),
                "wire release test: wire bytes ref count should remain 1 after init release");

        // Set and update the header.
        wire.headerNumber(1);
        wire.writeFirstHeader();
        wire.updateFirstHeader();

        // Release the wire's bytes and ensure all resources are released.
        wire.bytes().releaseLast(test);
        BackgroundResourceReleaser.releasePendingResources();
        assertEquals(0, wire.bytes().refCount(),
                "wire release test: wire bytes ref count should be 0 after release");
    }

    @Test
    // Test the process of releasing mapped bytes with multiple message writings and ensuring their resources are properly released.
    @DisplayName("Wire release after multiple writes releases resources")
    public void testMappedBytesWireRelease2() throws Exception {
        // Create a temporary file for the test and ensure it's deleted afterwards.
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        // Initialize the mapped bytes with a size of 256 KB and verify the initial reference counts.
        @NotNull MappedBytes t = MappedBytes.mappedBytes(tmp, 256 * 1024);
        assertEquals(1, t.refCount(),
                "multi write test: mapped bytes ref count should start at 1");
        assertEquals(1, t.mappedFile().refCount(),
                "multi write test: mapped file ref count should start at 1");
        Wire wire = WireType.TEXT.apply(t);

        // Check reference counts after initializing the wire.
        assertEquals(1, t.refCount(),
                "multi write test: mapped bytes ref count should remain 1 after wire apply");
        assertEquals(1, t.mappedFile().refCount(),
                "multi write test: mapped file ref count should remain 1 after wire apply");

        wire.headerNumber(1);
        assertEquals(1, t.refCount(),
                "multi write test: mapped bytes ref count should remain 1 after headerNumber");
        assertEquals(1, t.mappedFile().refCount(),
                "multi write test: mapped file ref count should remain 1 after headerNumber");

        // Set the header of the wire and verify reference counts.
        wire.writeFirstHeader();
        assertEquals(1, wire.bytes().refCount(),
                "multi write test: wire bytes ref count should remain 1 after writeFirstHeader");
        assertEquals(1, t.mappedFile().refCount(),
                "multi write test: mapped file ref count should remain 1 after writeFirstHeader"); // now there is a mapping used as well as use
        // in MappedBytes

        assertEquals(1, wire.bytes().refCount(),
                "multi write test: wire bytes ref count should remain 1 after mapping");
        assertEquals(1, mappedFile(wire).refCount(),
                "multi write test: mapped file ref count should remain 1 after mapping");

        wire.bytes().writeSkip(128000);
        wire.updateFirstHeader();

        writeMessage(wire);

        assertEquals(1, mappedFile(wire).refCount(),
                "multi write test: mapped file ref count should remain 1 after first writeMessage");

        writeMessage(wire);

        // new block
        assertEquals(1, mappedFile(wire).refCount(),
                "multi write test: mapped file ref count should remain 1 after second writeMessage");

        writeMessage(wire);

        assertEquals(1, mappedFile(wire).refCount(),
                "multi write test: mapped file ref count should remain 1 after third writeMessage");

        writeMessage(wire);

        assertEquals(1, mappedFile(wire).refCount(),
                "multi write test: mapped file ref count should remain 1 after fourth writeMessage");

        // Release resources associated with the wire.
        wire.bytes().releaseLast();
        BackgroundResourceReleaser.releasePendingResources();
        // the MappedFile was created by MappedBytes
        // so when it is fully released, the MappedFile is close()d
        assertEquals(0, wire.bytes().refCount(),
                "multi write test: wire bytes ref count should be 0 after release");
        assertEquals(0, t.refCount(),
                "multi write test: mapped bytes ref count should be 0 after release");
        assertEquals(0, mappedFile(wire).refCount(),
                "multi write test: mapped file ref count should be 0 after release");
    }

    // Helper method to retrieve the MappedFile associated with the given wire.
    @NotNull
    private MappedFile mappedFile(@NotNull Wire wire) {
        return ((MappedBytes) wire.bytes()).mappedFile();
    }
}
