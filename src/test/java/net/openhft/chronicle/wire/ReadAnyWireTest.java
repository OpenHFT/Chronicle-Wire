/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.wire.WireType.*;

/**
 * @author Rob Austin.
 */
public class ReadAnyWireTest {

    @Test
    public void testReadAny() {
        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();
        final Wire wire = TEXT.apply(t);
        wire.write((() -> "hello")).text("world");
        Assert.assertEquals("world", READ_ANY.apply(t).read(() -> "hello").text());
    }

    @Test
    public void testCreateReadAnyFirstTextWire() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        TEXT.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }

    @Test
    public void testCreateReadAnyFirstBinaryWire() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }

    @Test
    public void testCreateReadAnyFirstJSONWire() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        JSON.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }

    @Test
    @Ignore("TODO FIX")
    public void testCreateReadAnyFirstFIELDLESS_BINARYWire() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        FIELDLESS_BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }
}

