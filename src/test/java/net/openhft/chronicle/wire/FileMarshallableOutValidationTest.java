/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for URL validation when creating file based MarshallableOut instances.
 */
@SuppressWarnings({"deprecation", "removal"})
public class FileMarshallableOutValidationTest extends WireTestCommon {

    @Test
    public void rejectsParentTraversal() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            File dir = new File(OS.getTarget(), "valtest");
            dir.mkdirs();
            @SuppressWarnings("deprecation") URL url = new URL("file://" + dir.getAbsolutePath() + "/../evil");
            MarshallableOut.builder(url).get();
        });
    }
}
