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

public class ElasticByteBufferTest extends WireTestCommon {

    @Test
    public void testElasticByteBufferWithWire() throws Exception {

        Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer(10);

        Wire wire = WireType.BINARY.apply(byteBufferBytes);
        wire.usePadding(true);

        try (DocumentContext documentContext = wire.writingDocument(false)) {
            documentContext.wire().write("some key").text("some value of more than ten characters");
        }

        @Nullable ByteBuffer byteBuffer = byteBufferBytes.underlyingObject();
        StringBuilder stringBuilder = Wires.acquireStringBuilder();
        while (byteBuffer.remaining() > 0) {
            stringBuilder.append((char) byteBuffer.get());
        }

        @NotNull String s = stringBuilder.toString();
        Assert.assertTrue(s.contains("some value of more than ten characters"));

        byteBufferBytes.releaseLast();
    }
}
