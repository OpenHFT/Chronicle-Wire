/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.Assume.assumeFalse;

// Test class focusing on the functionality of elastic byte buffers with wire operations.
public class ElasticByteBufferTest extends WireTestCommon {

    @Test
    public void testElasticByteBufferWithWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

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
        Assert.assertTrue(s.contains("some value of more than ten characters"));

        byteBufferBytes.releaseLast();
    }

    @Test
    public void directElasticBufferResizesWhenCapacityIsExceeded() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

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

                Assert.assertTrue("buffer should grow when payload exceeds initial capacity",
                        directBytes.realCapacity() >= initialCapacity);

                directBytes.readPositionRemaining(0, directBytes.writePosition());
                try (DocumentContext context = wire.readingDocument()) {
                    Assert.assertTrue(context.isPresent());
                    Assert.assertEquals("padding=" + padding, largeValue,
                            context.wire().read("payload").text());
                }
            } finally {
                directBytes.releaseLast();
            }
        }
    }

    private static String repeat(char ch, int length) {
        char[] data = new char[length];
        Arrays.fill(data, ch);
        return new String(data);
    }
}
