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
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class LongConversionTest extends WireTestCommon {

    // Static initializer to add an alias for the LongHolder class to the CLASS_ALIASES pool
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(LongHolder.class);
    }

    // Test case to verify the correct serialization and deserialization of the LongHolder object
    @Test
    public void dto() {
        // Creating a new LongHolder instance and setting values for its fields
        LongHolder lh = new LongHolder();
        lh.hex = 0XFEDCBA9876543210L;
        lh.unsigned = Long.MIN_VALUE;
        lh.timestamp = 0x05432108090a0bL;

        // Asserting the expected serialized string representation of the LongHolder object
        assertEquals("!LongHolder {\n" +
                "  unsigned: C222222222222,\n" +
                "  hex: fedcba9876543210,\n" +
                "  timestamp: 2016-12-08T08:00:31.345163\n" +
                "}\n", lh.toString());

        // Deserializing the LongHolder object from its string representation
        LongConversionTest.LongHolder lh2 = Marshallable.fromString(lh.toString());

        // Asserting the equality of the deserialized and original objects
        assertEquals(lh2, lh);
    }

    // Test case to check the method using HexadecimalLongConverter
    @Test
    public void method() {

        // Initializing a new Wire instance with an elastic heap-allocated buffer
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(64))
                .useTextDocuments();

        // Creating a method writer for the WriteWithLong interface
        LongConversionTest.WriteWithLong write = wire.methodWriter(LongConversionTest.WriteWithLong.class);
        assertSame(write, write.to(0x12345));

        // Asserting the wire's string representation
        assertEquals("to: 12345\n", wire.toString());

        // Setting up a StringWriter to capture logging output
        StringWriter sw = new StringWriter();
        LongConversionTest.WriteWithLong read = Mocker.logging(LongConversionTest.WriteWithLong.class, "", sw);
        wire.methodReader(read)
                .readOne();

        // Asserting the captured output (Note: Mocker ignores the LongConverter)
        assertEquals("to[74565]\n", sw.toString().replaceAll("\r", ""));
    }


    // Test case to check the method using OxHexadecimalLongConverter
    @Test
    public void oxmethod() {

        // Initializing a new Wire instance with an elastic heap-allocated buffer
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(64))
                .useTextDocuments();

        // Creating a method writer for the OxWriteWithLong interface
        LongConversionTest.OxWriteWithLong write = wire.methodWriter(LongConversionTest.OxWriteWithLong.class);
        assertSame(write, write.to(0x12345));

        // Asserting the wire's string representation
        assertEquals("to: 0x12345\n", wire.toString());

        // Setting up a StringWriter to capture logging output
        StringWriter sw = new StringWriter();
        LongConversionTest.OxWriteWithLong read = Mocker.logging(LongConversionTest.OxWriteWithLong.class, "", sw);
        wire.methodReader(read).readOne();

        // NOTE: Mocker which is in Core, ignores the LongConverter
        assertEquals("to[74565]\n", sw.toString().replaceAll("\r", ""));
    }

    // Interface for method writers that use HexadecimalLongConverter
    interface WriteWithLong {
        LongConversionTest.WriteWithLong to(@LongConversion(HexadecimalLongConverter.class) int x);
    }

    // Interface for method writers that use OxHexadecimalLongConverter
    interface OxWriteWithLong {
        LongConversionTest.OxWriteWithLong to(@LongConversion(OxHexadecimalLongConverter.class) int x);
    }

    // Static class representing a holder for various types of Long values
    static class LongHolder extends SelfDescribingMarshallable {
        @LongConversion(UnsignedLongConverter.class)
        long unsigned;  // Represents unsigned long value
        @LongConversion(HexadecimalLongConverter.class)
        long hex;       // Represents a hexadecimal long value
        @LongConversion(MicroTimestampLongConverter.class)
        long timestamp; // Represents a micro-timestamp
    }
}
