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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Test;

import java.nio.ByteOrder;

import static junit.framework.TestCase.assertEquals;

public class HexDumpTest extends WireTestCommon {
    @Test
    public void testEndian() {
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN)
            return;

        Bytes<?> b = new HexDumpBytes();
        b.writeInt(0x0a0b0c0d);
        assertEquals("0d 0c 0b 0a\n", b.toHexString());
        b.releaseLast();
    }
}
