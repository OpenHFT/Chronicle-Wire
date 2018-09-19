/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;

import java.io.EOFException;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*
 * Created by Peter Lawrey on 04/05/2016.
 */
public class BinaryWireHeadersTest {

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Test
    public void testHeaderNumbers() throws TimeoutException, EOFException, StreamCorruptedException {
        @NotNull BytesStore store = NativeBytesStore.elasticByteBuffer();
        @NotNull Wire wire = new BinaryWire(store.bytesForWrite()).headerNumber(0L);
        @NotNull Wire wire2 = new BinaryWire(store.bytesForWrite()).headerNumber(0L);

        assertEquals(0, wire.headerNumber());
        assertTrue(wire.writeFirstHeader());
        wire.getValueOut().text("my header");
        wire.writeAlignTo(4, 0);

        wire.updateFirstHeader();
        assertEquals(0, wire.headerNumber()); // meta data doesn't count.

        for (int i = 0; i <= 3; i++) {
            long position = wire2.writeHeader(1, TimeUnit.MILLISECONDS, null, null);
            assertEquals(i, wire2.headerNumber());
            wire2.getValueOut().text("hello world");
            wire2.writeAlignTo(4, 0);
            wire2.updateHeader(position, false);
        }
        assertEquals(4, wire2.headerNumber());
        {
            long position = wire2.writeHeader(1, TimeUnit.MILLISECONDS, null, null);
            wire2.getValueOut().text("hello world");
            wire2.writeAlignTo(4, 0);
            wire2.updateHeader(position, true); // meta data doesn't count.
            assertEquals(4, wire2.headerNumber());
        }

        for (int i = 4; i <= 8; i += 2) {
            long position = wire.writeHeader(1, TimeUnit.MILLISECONDS, null, null);
            assertEquals(i, wire.headerNumber());
            wire.getValueOut().text("hello world");
            wire.updateHeader(position, false);

            long position2 = wire2.writeHeader(1, TimeUnit.MILLISECONDS, null, null);
            assertEquals(i + 1, wire2.headerNumber());
            wire2.getValueOut().text("hello world");
            wire2.updateHeader(position2, false);
        }
        assertEquals(10, wire2.headerNumber());

        wire.bytes().release();
        wire2.bytes().release();
    }

    @Test(timeout = 3000, expected = TimeoutException.class)
    public void testConcurrentHeaderNumbers() throws TimeoutException, EOFException, StreamCorruptedException {
        @NotNull BytesStore store = NativeBytesStore.elasticByteBuffer();
        @NotNull Wire wire = new BinaryWire(store.bytesForWrite()).headerNumber(0L);
        @NotNull Wire wire2 = new BinaryWire(store.bytesForWrite()).headerNumber(0L);
        try {
            long position = wire.writeHeader(1, TimeUnit.SECONDS, null, null);

            long position2 = wire2.writeHeader(100, TimeUnit.MILLISECONDS, null, null);

        } finally {
            wire.bytes().release();
            wire2.bytes().release();
        }
    }
}