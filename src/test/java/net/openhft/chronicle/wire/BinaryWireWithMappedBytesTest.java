/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.ref.BinaryTwoLongReference;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.core.values.TwoLongValue;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * This class tests BinaryWire behaviour with mapped bytes, reference counts, and read paths in file storage.
 */
@SuppressWarnings({"deprecation", "removal"})
public class BinaryWireWithMappedBytesTest extends WireTestCommon {

    // Defines if the MappedFile should retain its contents
    private static final boolean RETAIN = Jvm.getBoolean("mappedFile.retain");

    /**
     * Test to verify the reference management at the start of a binary wire
     * when using MappedBytes.
     *
     * @throws FileNotFoundException if the file for mapping bytes is not found
     */
    @Test
    @SuppressWarnings("rawtypes")
    @DisplayName("Reads references correctly at start of mapped bytes")
    public void testRefAtStart() throws FileNotFoundException {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip mapped bytes test");

        // Define the file for the test and ensure its deletion if it already exists
        @NotNull File file = new File(OS.getTarget(), "testRefAtStart.map");
        Path filePath = file.toPath();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new IORuntimeException("Failed to delete mapped bytes test file " + filePath, e);
        }

        // Create a mapped byte instance and use it to instantiate a binary wire
        @NotNull MappedBytes bytes = MappedBytes.mappedBytes(file, 64 << 10);
        Wire wire = WireType.BINARY.apply(bytes);

        // Write several integer and long values to the wire
        wire.write(() -> "int32").int32forBinding(1)
                .write(() -> "int32b").int32forBinding(2)
                .write(() -> "int64").int64forBinding(3)
                .write(() -> "int128").int128forBinding(4, 5);

        // Read the values from the wire and assert the retrieved data
        try (@NotNull IntValue a = wire.newIntReference();
             @NotNull IntValue b = wire.newIntReference();
             @NotNull LongValue c = wire.newLongReference();
             TwoLongValue d = new BinaryTwoLongReference()) {

            wire.read().int32(a, null, (o, i) -> {});
            wire.read().int32(b, null, (o, i) -> {});
            wire.read().int64(c, null, (o, i) -> {});
            wire.read().int128(d);

            // Assertions for the values read
            assertEquals(4, d.getValue(), "first int128 value should match written data");
            assertEquals(5, d.getValue2(), "second int128 value should match written data");

            assertEquals("", bytes.toHexString(), "mapped bytes should be fully read");

            int expected = RETAIN ? 2 : 1;
            assertEquals(expected + 4, ((Byteable) a).bytesStore().refCount(),
                    "refCount should match initial reads");

            // Generate a descriptive string from the read values
            assertEquals("value: 1 value: 2 value: 3 value: 4, value2: 5", a + " " + b + " " + c + " " + d,
                    "combined value string should match read values");

            // Force the old memory to be released and assert the reference count after
            bytes.compareAndSwapInt(1 << 20, 1, 1);
            assertEquals(expected + 3, ((Byteable) a).bytesStore().refCount(),
                    "refCount should match after first compareAndSwap");

            bytes.compareAndSwapInt(2 << 20, 1, 1);
            assertEquals(expected + 3, ((Byteable) a).bytesStore().refCount(),
                    "refCount should match after second compareAndSwap");

        }

        // Release the last byte reference
        bytes.releaseLast();
    }
}
