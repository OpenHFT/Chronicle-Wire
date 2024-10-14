/*
 * Copyright 2016-2020 chronicle.software
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
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class BinaryWireNumbersTest extends WireTestCommon {
    private static final float VAL1 = 12345678901234567.0f;
    static int counter = 0;
    private final int len;
    private final WriteValue expected;
    private final WriteValue perform;

    // Constructor initializes values for each test iteration
    public BinaryWireNumbersTest(int len, WriteValue expected, WriteValue perform) {
        this.len = len;
        this.expected = expected;
        this.perform = perform;
    }

    // Provides a collection of parameters to run the tests with
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        // Each sub-array represents a set of parameters: length, expected write value, and actual write value
        return Arrays.asList(new Object[][]{

                {2 + 4, (WriteValue) w -> w.float32(-(1L << 48L)), (WriteValue) w -> w.int64(-(1L << 48L))},    //0
                {2 + 8, (WriteValue) w -> w.int64(Long.MIN_VALUE + 1), (WriteValue) w -> w.int64(Long.MIN_VALUE + 1)},  //1
                {2 + 4, (WriteValue) w -> w.float32(-VAL1), (WriteValue) w -> w.int64((long) -VAL1)},//2
                {2 + 4, (WriteValue) w -> w.float32(VAL1), (WriteValue) w -> w.int64((long) VAL1)},//3
                {2 + 4, (WriteValue) w -> w.int32(Integer.MIN_VALUE), (WriteValue) w -> w.int64(Integer.MIN_VALUE)},//4
                {2 + 2, (WriteValue) w -> w.int16(Short.MIN_VALUE), (WriteValue) w -> w.int64(Short.MIN_VALUE)},//5
                {2 + 1, (WriteValue) w -> w.int8(Byte.MIN_VALUE), (WriteValue) w -> w.int64(Byte.MIN_VALUE)},//6
                {2 + 1, (WriteValue) w -> w.int8(-1), (WriteValue) w -> w.int64(-1)},//7
                {2 + 1, (WriteValue) w -> w.uint8(0), (WriteValue) w -> w.int64(0)}, //8
                {2 + 1, (WriteValue) w -> w.uint8(Byte.MAX_VALUE), (WriteValue) w -> w.int64(Byte.MAX_VALUE)}, //9
                {2 + 1, (WriteValue) w -> w.uint8(0xFF), (WriteValue) w -> w.int64(0xFF)}, //10
                {2 + 2, (WriteValue) w -> w.int16(Short.MAX_VALUE), (WriteValue) w -> w.int64(Short.MAX_VALUE)},// 11
                {2 + 2, (WriteValue) w -> w.uint16(0xFFFF), (WriteValue) w -> w.int64(0xFFFF)}, //12
                {2 + 4, (WriteValue) w -> w.int32(Integer.MAX_VALUE), (WriteValue) w -> w.int64(Integer.MAX_VALUE)},//13
                {2 + 4, (WriteValue) w -> w.uint32(0xFFFFFFFFL), (WriteValue) w -> w.int64(0xFFFFFFFFL)},//14
                {2 + 4, (WriteValue) w -> w.float32(1L << 50), (WriteValue) w -> w.int64(1L << 50)},//15
                {2 + 8, (WriteValue) w -> w.int64(Long.MAX_VALUE - 1), (WriteValue) w -> w.int64(Long.MAX_VALUE - 1)}, //16
                // floating point tests
                {2 + 8, (WriteValue) w -> w.float64(Double.MIN_VALUE), (WriteValue) w -> w.float64(Double.MIN_VALUE)}, //17
                {2 + 8, (WriteValue) w -> w.float64(Double.MAX_VALUE), (WriteValue) w -> w.float64(Double.MAX_VALUE)}, //18
                {2 + 4, (WriteValue) w -> w.float32(Float.MIN_VALUE), (WriteValue) w -> w.float64(Float.MIN_VALUE)}, //19
                {2 + 4, (WriteValue) w -> w.float32(Float.MAX_VALUE), (WriteValue) w -> w.float64(Float.MAX_VALUE)}, //20
                {2 + 4, (WriteValue) w -> w.float32(-(1L << 48)), (WriteValue) w -> w.float64(-(1L << 48))},//21
                {2 + 4, (WriteValue) w -> w.int32(Integer.MIN_VALUE), (WriteValue) w -> w.float64(Integer.MIN_VALUE)},//22
                {2 + 2, (WriteValue) w -> w.int16(Short.MIN_VALUE), (WriteValue) w -> w.float64(Short.MIN_VALUE)},//23
                {2 + 1, (WriteValue) w -> w.int8(Byte.MIN_VALUE), (WriteValue) w -> w.float64(Byte.MIN_VALUE)},//24
                {2 + 1, (WriteValue) w -> w.int8(-1), (WriteValue) w -> w.float64(-1)},//25
                {2 + 1, (WriteValue) w -> w.uint8(0), (WriteValue) w -> w.float64(0)},//26
                {2 + 1, (WriteValue) w -> w.uint8(Byte.MAX_VALUE), (WriteValue) w -> w.float64(Byte.MAX_VALUE)},//27
                {2 + 1, (WriteValue) w -> w.uint8(0xFF), (WriteValue) w -> w.float64(0xFF)},//28
                {2 + 2, (WriteValue) w -> w.int16(Short.MAX_VALUE), (WriteValue) w -> w.float64(Short.MAX_VALUE)},//29
                {2 + 2, (WriteValue) w -> w.uint16(0xFFFF), (WriteValue) w -> w.float64(0xFFFF)},//30
                {2 + 4, (WriteValue) w -> w.int32(Integer.MAX_VALUE), (WriteValue) w -> w.float64(Integer.MAX_VALUE)},//31
                {2 + 4, (WriteValue) w -> w.uint32(0xFFFFFFFFL), (WriteValue) w -> w.float64(0xFFFFFFFFL)},//32
                {2 + 4, (WriteValue) w -> w.float32(1L << 50), (WriteValue) w -> w.float64(1L << 50)},//33
                {2 + 4, (WriteValue) w -> w.float32(Long.MIN_VALUE), (WriteValue) w -> w.int64(Long.MIN_VALUE)},  //34
                {2 + 4, (WriteValue) w -> w.float32(Long.MAX_VALUE), (WriteValue) w -> w.int64(Long.MAX_VALUE)},
        });
    }

    // Test the BinaryWire number serialization using the given parameters
    @Test
    public void doTest() {
        if (counter++ == 18)
            Thread.yield();
        test(expected, perform);
    }

    // Compares the serialized values from expected and perform operations
    public void test(@NotNull WriteValue expected, @NotNull WriteValue perform) {
        @SuppressWarnings("rawtypes")
        @NotNull Bytes<?> bytes1 = allocateElasticOnHeap();
        @NotNull Wire wire1 = new BinaryWire(bytes1, true, false, false, Integer.MAX_VALUE, "binary");

        // Serialize the expected value
        expected.writeValue(wire1.write());

        // Check if the length of the serialized value matches the expected length
        assertEquals("Length for fixed length doesn't match for " + TextWire.asText(wire1), len, bytes1.readRemaining());

        @SuppressWarnings("rawtypes")
        @NotNull Bytes<?> bytes2 = allocateElasticOnHeap();
        @NotNull Wire wire2 = new BinaryWire(bytes2);

        // Serialize the actual value
        perform.writeValue(wire2.write());

        // Compare the lengths of the serialized values
        assertEquals("Lengths for variable length expected " + bytes1
                        + " and actual " + bytes2 + " don't match for " + TextWire.asText(wire1),
                bytes1.readRemaining(), bytes2.readRemaining());

        // If serialized values don't match, log the mismatched values
        if (!bytes1.toString().equals(bytes2.toString()))
            System.out.println("Format doesn't match for " + TextWire.asText(wire2));
    }
}
