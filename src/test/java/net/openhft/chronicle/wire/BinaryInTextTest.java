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
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

public class BinaryInTextTest extends WireTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void testBytesFromText() {
        Bytes<?> a = Marshallable.fromString(Bytes.class, "A==");
        assertEquals("A==", a.toString());

        BytesStore a2 = Marshallable.fromString(BytesStore.class, "A==");
        assertEquals("A==", a2.toString());

        Bytes<?> b = Marshallable.fromString(Bytes.class, "!!binary BA==");
        assertEquals("[pos: 0, rlim: 1, wlim: 2147483632, cap: 2147483632 ] ǁ⒋‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", b.toDebugString());

        Bytes<?> b2 = Marshallable.fromString(Bytes.class, "!!binary A1==");
        assertEquals("[pos: 0, rlim: 1, wlim: 2147483632, cap: 2147483632 ] ǁ⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", b2.toDebugString());
    }

    @Test
    public void testReserialize() {
        BIT b = new BIT();
        byte[] a = new byte[5];
        b.b = Bytes.wrapForRead(a);
        b.c = Bytes.wrapForRead(a);
       // System.out.println(b);

        BIT bit = Marshallable.fromString(BIT.class, "{\n" +
                "b: !!binary AAAAAAA=,\n" +
                "c: !!binary CCCCCCCC,\n" +
                "}");
        String bitToString = bit.toString();
        assertTrue(bitToString.equals("!net.openhft.chronicle.wire.BinaryInTextTest$BIT {\n" +
                "  b: !!binary AAAAAAA=,\n" +
                "  c: !!binary CCCCCCCC\n" +
                "}\n") ||
                bitToString.equals("!net.openhft.chronicle.wire.BinaryInTextTest$BIT {\n" +
                        "  c: !!binary CCCCCCCC,\n" +
                        "  b: !!binary AAAAAAA=\n" +
                        "}\n"));
    }

    @SuppressWarnings("rawtypes")
    static class BIT extends SelfDescribingMarshallable {
        Bytes<?> b;
        BytesStore c;
    }
}
