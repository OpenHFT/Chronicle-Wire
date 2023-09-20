/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.ref.BinaryTwoLongReference;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.core.values.TwoLongValue;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;

/**
 * This class tests the behavior of BinaryWire with mapped bytes.
 */
public class BinaryWireWithMappedBytesTest extends WireTestCommon {

    // Defines if the MappedFile should retain its contents
    static final boolean RETAIN = Jvm.getBoolean("mappedFile.retain");

    /**
     * Test to verify the reference management at the start of a binary wire
     * when using MappedBytes.
     *
     * @throws FileNotFoundException if the file for mapping bytes is not found
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRefAtStart() throws FileNotFoundException {

        // Define the file for the test and ensure its deletion if it already exists
        @NotNull File file = new File(OS.getTarget(), "testRefAtStart.map");
        file.delete();

        // Create a mapped byte instance and use it to instantiate a binary wire
        @NotNull MappedBytes bytes = MappedBytes.mappedBytes(file, 64 << 10);
        Wire wire = WireType.BINARY.apply(bytes);

        // Write several integer and long values to the wire
        wire.write(() -> "int32").int32forBinding(1)
                .write(() -> "int32b").int32forBinding(2)
                .write(() -> "int64").int64forBinding(3)
                .write(() -> "int128").int128forBinding(4, 5);

        // Read the values from the wire and assert the retrieved data
        try (@NotNull IntValue a = wire.newIntReference();
             @NotNull IntValue b = wire.newIntReference();
             @NotNull LongValue c = wire.newLongReference();
             TwoLongValue d = new BinaryTwoLongReference()) {

            wire.read().int32(a, null, (o, i) -> {});
            wire.read().int32(b, null, (o, i) -> {});
            wire.read().int64(c, null, (o, i) -> {});
            wire.read().int128(d);

            // Assertions for the values read
            assertEquals(4, d.getValue());
            assertEquals(5, d.getValue2());

            assertEquals("", bytes.toHexString());

            int expected = RETAIN ? 2 : 1;
            assertEquals(expected + 4, ((Byteable) a).bytesStore().refCount());

            // Generate a descriptive string from the read values
            assertEquals("value: 1 value: 2 value: 3 value: 4, value2: 5", a + " " + b + " " + c + " " + d);

            // Force the old memory to be released and assert the reference count after
            bytes.compareAndSwapInt(1 << 20, 1, 1);
            assertEquals(expected + 3, ((Byteable) a).bytesStore().refCount());
            // System.out.println(a + " " + b + " " + c);

            bytes.compareAndSwapInt(2 << 20, 1, 1);
            assertEquals(expected + 3, ((Byteable) a).bytesStore().refCount());
            // System.out.println(a + " " + b + " " + c);

        }

        // Release the last byte reference
        bytes.releaseLast();
    }
}
