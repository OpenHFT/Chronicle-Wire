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

package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class BytesMarshallableTest extends WireTestCommon {
    private final WireType wireType;

    // Constructor that accepts a wireType as a parameter
    public BytesMarshallableTest(WireType wireType) {
        this.wireType = wireType;
    }

    // This method provides different WireType parameters to be used in the tests
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{WireType.TEXT},
                new Object[]{WireType.BINARY_LIGHT}
        );
    }

    // Method to create and return a Wire object with specific configurations
    // Uses the `wireType` instance variable and applies it to a newly created Bytes object
    private Wire createWire() {
        return wireType.apply(Bytes.elasticHeapByteBuffer(64));
    }

    // Test method to verify the (de)serialization of primitive data transfer objects (DTOs)
    // with the wire, also validating against expected string representations
    @SuppressWarnings("incomplete-switch")
    @Test
    public void primitiveDto() {
        // Creating a wire object using the previously defined method
        Wire wire = createWire();

        // Creating and writing two DTO objects (PrimDto and ScalarDto) to the wire
        PrimDto dto1 = PrimDto.init(1);
        wire.write("prim").marshallable(dto1);

        ScalarDto sdto1 = ScalarDto.init(1);
        wire.write("scalar").marshallable(sdto1);

        // Initialize a default expected string, then define specific expectations
        // based on the current `wireType` being tested
        String expected = "Unknown wire type";
        switch (wireType) {
            // Cases define the expected debug string output of the wire bytes
            // based on the different wire types
            case TEXT:
                expected = "[pos: 0, rlim: 159, wlim: 2147483632, cap: 2147483632 ] ǁprim: {⒑  flag: true,⒑  s8: 1,⒑  ch: \"\\x01\",⒑  s16: 1,⒑  s32: 1,⒑  s64: 1,⒑  f32: 1.0,⒑  f64: 1.0⒑}⒑scalar: {⒑  text: Hello1,⒑  buffer: bye 1,⒑  bytes: hi 1⒑}⒑‡٠٠٠٠٠٠٠٠";
                break;
            case BINARY_LIGHT:
                expected = "[pos: 0, rlim: 69, wlim: 2147483632, cap: 2147483632 ] ǁÄprim\\u0082\\u001D٠٠٠Y⒈⒈⒈٠⒈٠٠٠⒈٠٠٠٠٠٠٠٠٠\\u0080?٠٠٠٠٠٠ð?Æscalar\\u0082⒙٠٠٠⒍Hello1⒌bye 1⒋hi 1‡٠٠٠٠٠٠٠٠٠٠٠";
                break;
        }
        // Asserting that the expected string equals the debug string output of the wire bytes
        assertEquals(expected, wire.bytes().toDebugString());

        // Creating two new DTOs and populating them by reading from the wire
        // Then, asserting that they equal the original written DTOs
        PrimDto dto2 = new PrimDto();
        ScalarDto sdto2 = new ScalarDto();

        // Performing the read, populate, and assert equal operations twice
        for (int i = 0; i < 2; i++) {
            wire.bytes().readPosition(0);

            wire.read("prim").marshallable(dto2);
            assertEquals(dto1, dto2);

            wire.read("scalar").marshallable(sdto2);
            assertEquals(sdto1, sdto2);
        }
    }

    // Another test method similar to the above, but using different DTO types (PrimDto2 and ScalarDto2)
    @SuppressWarnings("incomplete-switch")
    @Test
    public void primitiveDto2() {
        // Creating a wire object using the previously defined method
        Wire wire = createWire();

        // Creating and writing two DTO objects (PrimDto2 and ScalarDto2) to the wire
        PrimDto2 dto1 = PrimDto2.init(1);
        wire.write("prim").marshallable(dto1);

        ScalarDto2 sdto1 = ScalarDto2.init(1);
        wire.write("scalar").marshallable(sdto1);

        // Similar string expectation setting and assertion as in the previous test method
        String expected = "Unknown wire type";
        switch (wireType) {
            case TEXT:
                expected = "[pos: 0, rlim: 159, wlim: 2147483632, cap: 2147483632 ] ǁprim: {⒑  flag: true,⒑  s8: 1,⒑  ch: \"\\x01\",⒑  s16: 1,⒑  s32: 1,⒑  s64: 1,⒑  f32: 1.0,⒑  f64: 1.0⒑}⒑scalar: {⒑  text: Hello1,⒑  buffer: bye 1,⒑  bytes: hi 1⒑}⒑‡٠٠٠٠٠٠٠٠";
                break;

            case BINARY_LIGHT:
                expected = "[pos: 0, rlim: 50, wlim: 2147483632, cap: 2147483632 ] ǁÄprim\\u0082⒑٠٠٠Y⒈⒈⒈⒈⒈\\u009F|\\u009F|Æscalar\\u0082⒙٠٠٠⒍Hello1⒌bye 1⒋hi 1‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠";
                break;
        }
        // Asserting that the expected string equals the debug string output of the wire bytes
        assertEquals(expected, wire.bytes().toDebugString());

        // Creating two new DTOs, reading values from the wire, and asserting they equal originals
        PrimDto2 dto2 = new PrimDto2();
        ScalarDto2 sdto2 = new ScalarDto2();

        for (int i = 0; i < 2; i++) {
            wire.bytes().readPosition(0);

            wire.read("prim").marshallable(dto2);
            assertEquals(dto1, dto2);

            wire.read("scalar").marshallable(sdto2);
            assertEquals(sdto1, sdto2);
        }

        ClassAliasPool.CLASS_ALIASES.addAlias(PrimDto2.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(ScalarDto2.class);

        assertEquals("!PrimDto2 {\n" +
                "  flag: true,\n" +
                "  s8: 1,\n" +
                "  ch: \"\\x01\",\n" +
                "  s16: 1,\n" +
                "  s32: 1,\n" +
                "  s64: 1,\n" +
                "  f32: 1.0,\n" +
                "  f64: 1.0\n" +
                "}\n", dto2.toString());

        assertEquals("!ScalarDto2 {\n" +
                "  text: Hello1,\n" +
                "  buffer: bye 1,\n" +
                "  bytes: hi 1\n" +
                "}\n", sdto2.toString());
    }

    // Class encapsulating various primitive data types and providing initialization logic.
    static class PrimDto extends BytesInBinaryMarshallable {
        boolean flag;
        byte s8;
        char ch;
        short s16;
        int s32;
        long s64;
        float f32;
        double f64;

        // Method to initialize an instance of PrimDto with certain derived values based on input integer.
        static PrimDto init(int i) {
            return init(i, new PrimDto());
        }

        // Generic method to initialize a PrimDto or its subtype, using an input integer.
        static <T extends PrimDto> T init(int i, T d) {
            d.flag = i % 2 != 0;
            d.s8 = (byte) i;
            d.ch = (char) i;
            d.s16 = (short) i;
            d.s32 = i;
            d.s64 = i * i * i;
            d.f32 = d.s32;
            d.f64 = d.s64;
            return d;
        }
    }

    // Class extending PrimDto with custom marshallable reading and writing logic.
    static class PrimDto2 extends PrimDto {
        // Method to initialize an instance of PrimDto2 with certain derived values based on input integer.
        static PrimDto2 init(int i) {
            return init(i, new PrimDto2());
        }

        // Overridden method defining custom deserialization logic for PrimDto2.
        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException {
            flag = bytes.readBoolean();
            s8 = bytes.readByte();
            ch = (char) Maths.toUInt16(bytes.readStopBit());
            s16 = Maths.toInt16(bytes.readStopBit());
            s32 = Maths.toInt32(bytes.readStopBit());
            s64 = bytes.readStopBit();
            f32 = (float) bytes.readStopBitDouble();
            f64 = bytes.readStopBitDouble();
        }

        // Overridden method defining custom serialization logic for PrimDto2.
        @Override
        public void writeMarshallable(BytesOut<?> bytes) {
            bytes.writeBoolean(flag);
            bytes.writeByte(s8);
            bytes.writeStopBit(ch);
            bytes.writeStopBit(s16);
            bytes.writeStopBit(s32);
            bytes.writeStopBit(s64);
            bytes.writeStopBit(f32);
            bytes.writeStopBit(f64);
        }
    }

    // Class encapsulating scalar values (text, buffer, bytes) with initialization logic.
    static class ScalarDto extends BytesInBinaryMarshallable {
        String text;
        StringBuilder buffer;
        Bytes<?> bytes;

        // Method to initialize an instance of ScalarDto with certain derived values based on input integer.
        static ScalarDto init(int i) {
            return init(i, new ScalarDto());
        }

        // Generic method to initialize a ScalarDto or its subtype, using an input integer.
        static <D extends ScalarDto> D init(int i, D d) {
            d.text = "Hello" + i;
            d.buffer = new StringBuilder("bye " + i);
            d.bytes = Bytes.allocateElasticOnHeap(8).append("hi ").append(i);
            return d;
        }
    }

    // Class extending ScalarDto with custom marshallable reading and writing logic.
    static class ScalarDto2 extends ScalarDto {
        // Method to initialize an instance of ScalarDto2 with certain derived values based on input integer.
        static ScalarDto2 init(int i) {
            return init(i, new ScalarDto2());
        }

        // Overridden method defining custom deserialization logic for ScalarDto2.
        @Override
        public void readMarshallable(BytesIn<?> in) throws IORuntimeException {
            text = in.read8bit();
            if (buffer == null) buffer = new StringBuilder();
            in.read8bit(buffer);
            if (bytes == null) bytes = Bytes.allocateElasticOnHeap(8);
            in.read8bit(bytes);
        }

        // Overridden method defining custom serialization logic for ScalarDto2.
        @Override
        public void writeMarshallable(BytesOut<?> out) {
            out.write8bit(text);
            out.write8bit(buffer);
            if (bytes == null) {
                out.writeStopBit(-1);
            } else {
                long offset = bytes.readPosition();
                long readRemaining = Math.min(out.writeRemaining(), bytes.readLimit() - offset);
                out.writeStopBit(readRemaining);
                try {
                    out.write(bytes, offset, readRemaining);
                } catch (BufferUnderflowException | IllegalArgumentException e) {
                    throw new AssertionError(e);
                }
            }
        }
    }
}
