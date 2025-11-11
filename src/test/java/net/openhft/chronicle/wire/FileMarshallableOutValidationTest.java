/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * Tests for URL validation when creating file based MarshallableOut instances.
 */
public class FileMarshallableOutValidationTest extends WireTestCommon {

    @Test(expected = IllegalArgumentException.class)
    public void rejectsParentTraversal() throws Exception {
        File dir = new File(OS.getTarget(), "valtest");
        dir.mkdirs();
        @SuppressWarnings("deprecation")
        URL url = new URL("file://" + dir.getAbsolutePath() + "/../evil");
        MarshallableOut.builder(url).get();
    }
}
