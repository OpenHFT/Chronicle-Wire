/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Test class focusing on the functionality of elastic byte buffers with wire operations.
public class ElasticByteBufferTest extends WireTestCommon {

    private static String repeat(char ch, int length) {
        char[] data = new char[length];
        Arrays.fill(data, ch);
        return new String(data);
    }

    @Test
    @DisplayName("Writes text into elastic byte buffer wire")
    public void testElasticByteBufferWithWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip elastic byte buffer wire test");

        // Initialize an elastic byte buffer with initial size of 10.
        Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer(10);

        // Use binary wire type with padding enabled.
        Wire wire = WireType.BINARY.apply(byteBufferBytes);
        wire.usePadding(true);

        // Write a key-value pair into the wire document.
        try (DocumentContext documentContext = wire.writingDocument(false)) {
            documentContext.wire().write("some key").text("some value of more than ten characters");
        }

        @Nullable ByteBuffer byteBuffer = byteBufferBytes.underlyingObject();
        StringBuilder stringBuilder = new StringBuilder();
        while (byteBuffer.remaining() > 0) {
            stringBuilder.append((char) byteBuffer.get());
        }

        // Assert that the text was written correctly.
        @NotNull String s = stringBuilder.toString();
        Assertions.assertTrue(s.contains("some value of more than ten characters"),
                "buffer output should contain expected payload text, actual=" + s);

        byteBufferBytes.releaseLast();
    }

    @Test
    @DisplayName("Resizes direct elastic buffer when capacity exceeded")
    public void directElasticBufferResizesWhenCapacityIsExceeded() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip direct elastic buffer resize test");

        for (boolean padding : new boolean[]{true, false}) {
            Bytes<?> directBytes = Bytes.allocateElasticDirect(32);
            try {
                Wire wire = WireType.BINARY.apply(directBytes);
                wire.usePadding(padding);

                long initialCapacity = directBytes.realCapacity();
                String largeValue = repeat('x', (int) initialCapacity + 64);

                try (DocumentContext context = wire.writingDocument(false)) {
                    context.wire().write("payload").text(largeValue);
                }

                Assertions.assertTrue(directBytes.realCapacity() >= initialCapacity,
                        "buffer should grow when payload exceeds initial capacity, padding=" + padding);

                directBytes.readPositionRemaining(0, directBytes.writePosition());
                try (DocumentContext context = wire.readingDocument()) {
                    Assertions.assertTrue(context.isPresent(),
                            "document should be present after writing payload, padding=" + padding);
                    Assertions.assertEquals(largeValue, context.wire().read("payload").text(),
                            "payload text should round-trip for padding=" + padding);
                }
            } finally {
                directBytes.releaseLast();
            }
        }
    }
}
