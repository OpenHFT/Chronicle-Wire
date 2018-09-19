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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.ref.BinaryTwoLongReference;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.core.values.TwoLongValue;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 18/01/16.
 */
public class BinaryWireWithMappedBytesTest {
    @Test
    public void testRefAtStart() throws FileNotFoundException {
        @NotNull File file = new File(OS.TARGET, "testRefAtStart.map");
        file.delete();
        @NotNull MappedBytes bytes = MappedBytes.mappedBytes(file, 64 << 10);
        Wire wire = WireType.BINARY.apply(bytes);
        wire.write(() -> "int32").int32forBinding(1)
                .write(() -> "int32b").int32forBinding(2)
                .write(() -> "int64").int64forBinding(3)
                .write(() -> "int128").int128forBinding(4, 5);
        @NotNull IntValue a = wire.newIntReference();
        @NotNull IntValue b = wire.newIntReference();
        @NotNull LongValue c = wire.newLongReference();
        TwoLongValue d = new BinaryTwoLongReference();

        wire.read().int32(a, null, (o, i) -> {
        });
        wire.read().int32(b, null, (o, i) -> {
        });
        wire.read().int64(c, null, (o, i) -> {
        });
        wire.read().int128(d);

        assertEquals(4, d.getValue());
        assertEquals(5, d.getValue2());

        assertEquals("", bytes.toHexString());

        assertEquals(5, ((Byteable) a).bytesStore().refCount());

        assertEquals("value: 1 value: 2 value: 3 value: 4, value2: 5", a + " " + b + " " + c + " " + d);

        // cause the old memory to drop out.
        bytes.compareAndSwapInt(1 << 20, 1, 1);
        assertEquals(4, ((Byteable) a).bytesStore().refCount());
        System.out.println(a + " " + b + " " + c);

        bytes.compareAndSwapInt(2 << 20, 1, 1);
        assertEquals(4, ((Byteable) a).bytesStore().refCount());
        System.out.println(a + " " + b + " " + c);

        bytes.release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}
