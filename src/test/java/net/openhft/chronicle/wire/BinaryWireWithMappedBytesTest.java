/*
 *     Copyright (C) 2016  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 18/01/16.
 */
public class BinaryWireWithMappedBytesTest {
    @Test
    public void testRefAtStart() throws FileNotFoundException {
        MappedBytes bytes = MappedBytes.mappedBytes(new File(OS.TARGET, "testRefAtStart.map"), 64 << 10);
        Wire wire = WireType.BINARY.apply(bytes);
        wire.write(() -> "int32").int32forBinding(1)
                .write(() -> "int32b").int32forBinding(2)
                .write(() -> "int64").int64forBinding(3);
        IntValue a = wire.newIntReference();
        IntValue b = wire.newIntReference();
        LongValue c = wire.newLongReference();
        wire.read().int32(a, null, (o, i) -> {
        });
        wire.read().int32(b, null, (o, i) -> {
        });
        wire.read().int64(c, null, (o, i) -> {
        });
        assertEquals(2 + 3, ((Byteable) a).bytesStore().refCount());

        System.out.println(a + " " + b + " " + c);

        // cause the old memory to drop out.
        bytes.compareAndSwapInt(1 << 20, 1, 1);
        assertEquals(1 + 3, ((Byteable) a).bytesStore().refCount());
        System.out.println(a + " " + b + " " + c);

        bytes.compareAndSwapInt(2 << 20, 1, 1);
        assertEquals(1 + 3, ((Byteable) a).bytesStore().refCount());
        System.out.println(a + " " + b + " " + c);

        bytes.close();
    }
}
