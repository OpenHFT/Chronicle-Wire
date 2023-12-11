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
import org.junit.Test;

import static net.openhft.chronicle.wire.IdentifierLongConverter.*;
import static org.junit.Assert.assertEquals;

public class IdentifierLongConverterTest extends net.openhft.chronicle.wire.WireTestCommon {

    public static final String MAX_SMALL_POSITIVE_STR = "^^^^^^^^^^";

    @Test
    public void parseMin() {
        assertEquals(0, INSTANCE.parse(""));
        assertEquals(0, INSTANCE.parse("0"));
    }

    @Test
    public void parseMaxSmallPositive() {
        assertEquals(MAX_SMALL_ID,
                INSTANCE.parse(MAX_SMALL_POSITIVE_STR));
    }

    @Test
    public void parseMaxSmallPositivePlus1() {
        assertEquals(MAX_SMALL_ID + 1,
                INSTANCE.parse(MIN_DATE));
    }

    @Test
    public void asString0() {
        assertEquals("",
                INSTANCE.asString(0));
    }

    @Test
    public void asStringMaxSmall() {
        assertEquals(MAX_SMALL_POSITIVE_STR,
                INSTANCE.asString(MAX_SMALL_ID));
    }

    @Test
    public void asStringMaxSmallPlus1() {
        assertEquals(MIN_DATE,
                INSTANCE.asString(MAX_SMALL_ID + 1));
    }

    @Test
    public void asStringMaxDateTime() {
        assertEquals(MAX_DATE,
                INSTANCE.asString(Long.MAX_VALUE));
    }

    @Test
    public void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    @Test
    public void allSafeCharsYamlWire() {
        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    private void allSafeChars(Wire wire) {
        final LongConverter converter = IdentifierLongConverter.INSTANCE;
        for (long i = 0; i < 32; i++) {
            wire.clear();
            wire.write("a").writeLong(converter, i);
            wire.write("b").sequence(i, (i2, v) -> {
                v.writeLong(converter, i2);
                v.writeLong(converter, i2);
            });
            assertEquals(wire.toString(),
                    i, wire.read("a").readLong(converter));
            wire.read("b").sequence(i, (i2, v) -> {
                assertEquals((long) i2, v.readLong(converter));
                assertEquals((long) i2, v.readLong(converter));
            });
        }
    }
}
