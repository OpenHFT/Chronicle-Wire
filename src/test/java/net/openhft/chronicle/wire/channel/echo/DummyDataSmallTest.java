/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * The 'DummyDataSmallTest' class extends 'WireTestCommon' and is intended to provide
 * test coverage for functionality defined in 'DummyDataSmall'.
 */
public class DummyDataSmallTest extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * The 'readMarshallable' test method ensures that instances of 'DummyDataSmall' can
     * be correctly marshalled (serialized and deserialized) using the 'BinaryWire' class.
     */
    @Test
    public void readMarshallable() {
        // Creating a 'DummyDataSmall' instance and initializing its fields
        DummyDataSmall dd = new DummyDataSmall();
        dd.timeNS(12345)
                .data(new byte[16]);

        // Creating a byte buffer and a wire attached to that buffer
        final HexDumpBytes bytes = new HexDumpBytes();
        Wire wire = new BinaryWire(bytes);

        // Writing the 'DummyDataSmall' instance to the wire
        wire.getValueOut().object(DummyDataSmall.class, dd);

        // Asserting that the binary representation is as expected
        assertEquals("" +
                        "80 19 39 30 00 00 00 00 00 00 10 00 00 00 00 00 # DummyDataSmall\n" +
                        "00 00 00 00 00 00 00 00 00 00 00\n",
                bytes.toHexString());

        // Creating a 'DummyData' instance to test deserialization
        DummyData dd2 = new DummyData();

        // Reading the data from the wire into the 'DummyData' instance
        wire.getValueIn().object(dd2, DummyData.class);

        // Validating that the deserialized object's fields are equal to the original object's
        assertEquals(dd.timeNS(), dd2.timeNS());
        assertArrayEquals(dd.data(), dd2.data());
    }
}
