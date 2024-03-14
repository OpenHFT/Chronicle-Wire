/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

// Test class focusing on the functionality of elastic byte buffers with wire operations.
public class ElasticByteBufferTest extends WireTestCommon {

    @Test
    public void testElasticByteBufferWithWire() {

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
}
