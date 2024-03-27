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
 * The 'DummyDataTest' class extends 'WireTestCommon' and aims to test functionality
 * related to serialization and deserialization of 'DummyData' objects using Chronicle Wire.
 */
public class DummyDataTest extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * The 'readMarshallable' test method ensures that 'DummyData' objects can be correctly
     * serialized and deserialized using the Chronicle Wire's 'BinaryWire'.
     */
    @Test
    public void readMarshallable() {
        // Instantiate and initialize a 'DummyData' object with specified field values
        DummyData dd = new DummyData()
                .timeNS(12345)
                .data(new byte[16]);

        // Create a byte buffer and a wire attached to that buffer
        final HexDumpBytes bytes = new HexDumpBytes();
        Wire wire = new BinaryWire(bytes);

        // Serialize the 'DummyData' instance to the wire
        wire.getValueOut().object(DummyData.class, dd);

        // Assert that the binary representation of the object matches the expected output
        assertEquals("" +
                        "82 1c 00 00 00 39 30 00 00 00 00 00 00 10 00 00 # DummyData\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00\n",
                bytes.toHexString());

        // Instantiate a new 'DummyData' object to verify deserialization
        DummyData dd2 = new DummyData();

        // Deserialize the data from the wire into the new 'DummyData' instance
        wire.getValueIn().object(dd2, DummyData.class);

        // Validate that the deserialized object's fields are equal to the original object's
        assertEquals(dd.timeNS(), dd2.timeNS());
        assertArrayEquals(dd.data(), dd2.data());
    }
}
