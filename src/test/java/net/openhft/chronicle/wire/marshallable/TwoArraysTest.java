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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TwoArraysTest extends WireTestCommon {
    @Test
    public void testTwoArrays() {
        ignoreException("BytesMarshallable found in field which is not matching exactly");
        Bytes<?> bytes = new HexDumpBytes();
        Wire wire = new BinaryWire(bytes);
        TwoArrays ta = new TwoArrays(4, 8);
        ta.writeMarshallable(wire);
        assertEquals("" +
                        "   c2 69 61                                        # ia:\n" +
                        "   82 20 00 00 00                                  # BinaryIntArrayReference\n" +
                        "                                                # BinaryIntArrayReference\n" +
                        "   04 00 00 00 00 00 00 00                         # capacity\n" +
                        "   00 00 00 00 00 00 00 00                         # used\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # values\n" +
                        "   c2 6c 61                                        # la:\n" +
                        "   82 50 00 00 00                                  # BinaryLongArrayReference\n" +
                        "                                                # BinaryLongArrayReference\n" +
                        "   08 00 00 00 00 00 00 00                         # capacity\n" +
                        "   00 00 00 00 00 00 00 00                         # used\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # values\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n",
                bytes.toHexString());

        TwoArrays ta2 = new TwoArrays(0, 0);
        ta2.readMarshallable(wire);
        assertEquals(4, ta2.ia.getCapacity());
        assertEquals(8, ta2.la.getCapacity());
        ta2.ia.setMaxUsed(1);
        ta2.ia.setValueAt(0, 11);
        ta2.la.setMaxUsed(2);
        ta2.la.setValueAt(0, 111);
        ta2.la.setValueAt(1, 222);

        Bytes<?> bytes2 = new HexDumpBytes();
        Wire wire2 = new BinaryWire(bytes2);
        ta2.writeMarshallable(wire2);
        assertEquals("" +
                        "   c2 69 61                                        # ia:\n" +
                        "   82 20 00 00 00                                  # BinaryIntArrayReference\n" +
                        "   04 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 # BinaryIntArrayReference\n" +
                        "   0b 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # la:\n" +
                        "   c2 6c 61 82 50 00 00 00                         # BinaryLongArrayReference\n" +
                        "   08 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 # BinaryLongArrayReference\n" +
                        "   6f 00 00 00 00 00 00 00 de 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n",
                bytes2.toHexString());

        bytes.readPosition(0);
        TwoArrays ta3 = new TwoArrays(0, 0);
        ta3.readMarshallable(wire);
        assertEquals(4, ta3.ia.getCapacity());
        assertEquals(1, ta3.ia.getUsed());
        assertEquals(11, ta3.ia.getValueAt(0));
        assertEquals(8, ta3.la.getCapacity());
        assertEquals(2, ta3.la.getUsed());
        assertEquals(111, ta3.la.getValueAt(0));
        assertEquals(222, ta3.la.getValueAt(1));
        ta.close();
        ta2.close();
        ta3.close();
        bytes2.releaseLast();
        bytes.releaseLast();
    }
}
