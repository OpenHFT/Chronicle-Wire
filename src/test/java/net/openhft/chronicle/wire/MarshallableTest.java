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

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarshallableTest {
    @Test
    public void testBytesMarshallable() {
        Marshallable m = new MyTypesCustom();

        Bytes bytes = nativeBytes();
        assertTrue(bytes.isElastic());
        TextWire wire = new TextWire(bytes);
        m.writeMarshallable(wire);

        m.readMarshallable(wire);
    }

    @Test
    public void testWriteDelta() {
        Wire wire = new TextWire(Bytes.elasticByteBuffer());
        AClass a1 = new AClass(1, true, (byte) 1, '1', (short) 1, 1, 1L, 1.0f, 1.0, "one");
        wire.writeDocument(false, a1);
        AClass a2 = new AClass(1, true, (byte) 1, '1', (short) 2, 1, 1L, 2.0f, 1.0, "one");
        wire.writeDocument(false, w -> Wires.writeMarshallable(a2, w, a1, true));
        AClass a3 = new AClass(1, false, (byte) 1, '2', (short) 2, 2, 2L, 2.0f, 1.0, "two");
        wire.writeDocument(false, w -> Wires.writeMarshallable(a3, w, a1, true));
        AClass a4 = new AClass(2, false, (byte) 2, '2', (short) 2, 2, 2L, 2.0f, 2.0, "two");
        wire.writeDocument(false, w -> Wires.writeMarshallable(a4, w, a1, true));
        assertEquals("--- !!data\n" +
                "id: 1\n" +
                "flag: true\n" +
                "b: 1\n" +
                "ch: \"1\"\n" +
                "s: 1\n" +
                "i: 1\n" +
                "l: 1\n" +
                "f: 1.0\n" +
                "d: 1.0\n" +
                "text: one\n" +
                "# position: 73\n" +
                "--- !!data\n" +
                "s: 2\n" +
                "f: 2.0\n" +
                "# position: 89\n" +
                "--- !!data\n" +
                "flag: false\n" +
                "ch: \"2\"\n" +
                "i: 2\n" +
                "l: 2\n" +
                "text: two\n" +
                "# position: 133\n" +
                "--- !!data\n" +
                "id: 2\n" +
                "b: 2\n" +
                "d: 2.0\n", Wires.fromSizePrefixedBlobs(wire));

    }
}

