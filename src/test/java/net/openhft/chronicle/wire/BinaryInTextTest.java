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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(value = Parameterized.class)
public class BinaryInTextTest extends WireTestCommon {
    private final WireType wireType;

    public BinaryInTextTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML});
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testBytesFromText() {
        Bytes<?> a = wireType.fromString(Bytes.class, "A==");
        assertEquals("A==", a.toString());

        BytesStore a2 = wireType.fromString(BytesStore.class, "A==");
        assertEquals("A==", a2.toString());

        Bytes<?> b = wireType.fromString(Bytes.class, "!!binary BA==");
        assertEquals("00000000 04", b.toHexString().substring(0, 58).trim());

        Bytes<?> b2 = wireType.fromString(Bytes.class, "!!binary A1==");
        assertEquals("00000000 03", b2.toHexString().substring(0, 58).trim());
    }

    @Test
    public void testReserialize() {
        BIT bit = wireType.fromString(BIT.class, "{\n" +
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
