/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for URL validation when creating file based MarshallableOut instances.
 */
@SuppressWarnings({"deprecation", "removal"})
class FileMarshallableOutValidationTest extends WireTestCommon {

    @Test
    @DisplayName("Rejects url path traversal when building marshallable out")
    void rejectsParentTraversal() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            File dir = new File(OS.getTarget(), "valtest");
            Path dirPath = dir.toPath();
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                throw new IORuntimeException("Failed to create validation directory " + dirPath, e);
            }
            @SuppressWarnings("deprecation") URL url = new URL("file://" + dir.getAbsolutePath() + "/../evil");
            MarshallableOut.builder(url).get();
        }, "path traversal should be rejected when building MarshallableOut");
    }
}
