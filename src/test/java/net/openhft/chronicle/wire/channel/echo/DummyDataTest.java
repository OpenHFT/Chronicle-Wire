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
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DummyDataTest extends net.openhft.chronicle.wire.WireTestCommon {

    @Test
    public void readMarshallable() {
        DummyData dd = new DummyData()
                .timeNS(12345)
                .data(new byte[16]);
        final HexDumpBytes bytes = new HexDumpBytes();
        Wire wire = new BinaryWire(bytes);
        wire.getValueOut().object(DummyData.class, dd);
        assertEquals("" +
                        "82 1c 00 00 00 39 30 00 00 00 00 00 00 10 00 00 # DummyData\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00\n",
                bytes.toHexString());
        DummyData dd2 = new DummyData();
        wire.getValueIn().object(dd2, DummyData.class);
        assertEquals(dd.timeNS(), dd2.timeNS());
        assertArrayEquals(dd.data(), dd2.data());
    }
}