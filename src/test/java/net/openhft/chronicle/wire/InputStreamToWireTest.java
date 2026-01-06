/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"deprecation", "removal"})
class InputStreamToWireTest extends WireTestCommon {

    @Test
    @DisplayName("Negative length frames reject with StreamCorruptedException")
    void negativeLengthRejects() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(buffer)) {
            dos.writeInt(-1);
        }

        InputStreamToWire reader = new InputStreamToWire(
                WireType.BINARY,
                new ByteArrayInputStream(buffer.toByteArray()));
        assertThrows(StreamCorruptedException.class, reader::readOne,
                "negative frame length fails fast");
    }
}
