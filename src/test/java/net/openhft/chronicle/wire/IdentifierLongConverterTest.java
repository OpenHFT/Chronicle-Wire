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

    // Defining constants for the test class
    public static final String MAX_SMALL_POSITIVE_STR = "^^^^^^^^^^";

    // Test the parsing functionality for the minimum values
    @Test
    public void parseMin() {
        assertEquals(0, INSTANCE.parse(""));
        assertEquals(0, INSTANCE.parse("000", 1, 2));
    }

    // Test parsing functionality for the maximum small positive value
    @Test
    public void parseMaxSmallPositive() {
        assertEquals(MAX_SMALL_ID,
                INSTANCE.parse(MAX_SMALL_POSITIVE_STR));
    }

    // Test parsing functionality for one more than the max small positive value
    @Test
    public void parseMaxSmallPositivePlus1() {
        assertEquals(MAX_SMALL_ID + 1,
                INSTANCE.parse(MIN_DATE));
    }

    // Test the string representation for zero
    @Test
    public void asString0() {
        assertEquals("",
                INSTANCE.asString(0));
    }

    // Test the string representation for the max small value
    @Test
    public void asStringMaxSmall() {
        assertEquals(MAX_SMALL_POSITIVE_STR,
                INSTANCE.asString(MAX_SMALL_ID));
    }

    // Test the string representation for one more than the max small value
    @Test
    public void asStringMaxSmallPlus1() {
        assertEquals(MIN_DATE,
                INSTANCE.asString(MAX_SMALL_ID + 1));
    }

    // Test the string representation for the maximum DateTime value
    @Test
    public void asStringMaxDateTime() {
        assertEquals(MAX_DATE,
                INSTANCE.asString(Long.MAX_VALUE));
    }

    // Test using the TextWire format with safe characters
    @Test
    public void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    // Test using the YamlWire format with safe characters
    @Test
    public void allSafeCharsYamlWire() {
        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    // Helper function to test all safe characters for both TextWire and YamlWire formats
    private void allSafeChars(Wire wire) {
        final LongConverter converter = IdentifierLongConverter.INSTANCE;

        // Loop through the first 32 numbers to validate the conversion logic
        for (long i = 0; i < 32; i++) {
            wire.clear();

            // Write long values to the wire with the provided converter
            wire.write("a").writeLong(converter, i);

            // Write sequence values to the wire with the provided converter
            wire.write("b").sequence(i, (i2, v) -> {
                v.writeLong(converter, i2);
                v.writeLong(converter, i2);
            });

            // Assert that the written values match the expected ones
            assertEquals(wire.toString(),
                    i, wire.read("a").readLong(converter));
            wire.read("b").sequence(i, (i2, v) -> {
                assertEquals((long) i2, v.readLong(converter));
                assertEquals((long) i2, v.readLong(converter));
            });
        }
    }
}
