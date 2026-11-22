/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MappedBytes;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.channels.FileLock;

import static org.junit.Assert.*;

public class MappedEmptyValueLookaheadTest {
    @Rule
    public TemporaryFolder temporaryFolder = TemporaryFolder.builder().assureDeletion().build();

    @Test
    public void emptyMappedTableDoesNotScanPastZeroPadding() throws Exception {
        checkLookahead(false);
    }

    @Test
    public void emptyFieldLookaheadStopsAtMappedReadLimit() throws Exception {
        checkLookahead(true);
    }

    private void checkLookahead(boolean fieldAtEnd) throws Exception {
        File file = temporaryFolder.newFile();
        MappedBytes bytes = MappedBytes.mappedBytes(file, 4096, 0);
        try {
            bytes.writeByte(0, (byte) 0);
            long capacity = bytes.realCapacity();
            if (fieldAtEnd) {
                for (long position = 0; position < capacity; position++)
                    bytes.writeByte(position, (byte) ' ');
                bytes.append("\"empty\":");
            }
            bytes.readPosition(0);
            bytes.readLimit(capacity);
            long originalLength = file.length();
            try (FileLock lock = bytes.mappedFile().tryLock(0, Long.MAX_VALUE, true)) {
                assertNotNull(lock);
                StringBuilder name = new StringBuilder();
                new JSONWire(bytes).readEventName(name);
                assertEquals(fieldAtEnd ? "empty" : "", name.toString());
                assertEquals(originalLength, file.length());
            }
        } finally {
            bytes.releaseLast();
        }
    }
}
