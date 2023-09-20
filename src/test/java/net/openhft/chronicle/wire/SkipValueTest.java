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
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static net.openhft.chronicle.wire.BinaryWireCode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
/**
 * Tests the ability to skip certain values in wire formats based on the parameterized input.
 */
@SuppressWarnings("rawtypes")
@RunWith(value = Parameterized.class)
public class SkipValueTest extends net.openhft.chronicle.wire.WireTestCommon {

    private final String name; // Represents the name of the binary wire code.
    private final int code;   // Represents the binary wire code.
    private final Consumer<ValueOut> valueOutConsumer; // A consumer function to operate on a ValueOut.

    // Constructor for parameterized test.
    public SkipValueTest(String name, int code, Consumer<ValueOut> valueOutConsumer) {
        this.name = name;
        this.code = code;
        this.valueOutConsumer = valueOutConsumer;
    }

    // Helper method to return the provided consumer directly.
    static Consumer<ValueOut> wr(Consumer<ValueOut> valueOutConsumer) {
        return valueOutConsumer;
    }

    /**
     * Provides the data for parameterized testing. Each test case will be given a name, a wire code,
     * and a consumer function that indicates how to produce the value to be skipped.
     */
    @NotNull
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() throws IllegalAccessException {

        // An array that will be populated with test cases.
        Object[][] list = {
                // Here you define each test case. The array consists of:
            // { name of wire code (filled out later), the code itself, a consumer to produce the value }
                {null, BYTES_LENGTH8, wr(v -> v.object(Dto8.class, new Dto8()))},
                {null, BYTES_LENGTH16, wr(v -> v.object(Dto16.class, new Dto16()))},
                {null, BYTES_LENGTH32, wr(v -> v.object(Dto32.class, new Dto32()))},
/*                {null, BYTES_MARSHALLABLE, wr(v -> {
                    v.object(BM.class, new BM());
                    v.wireOut().bytes().readPosition(5);
                })},*/
                {null, U8_ARRAY, wr(v -> {
                    v.bytes(new byte[4]);
                    v.wireOut().bytes().readPosition(2);
                })},
                {null, I64_ARRAY, wr(v -> {
                    v.int64array(4);
                    v.wireOut().bytes().readPosition(7);
                })},
                {null, PADDING32, wr(v -> v.wireOut().addPadding(33).getValueOut().text("hi"))},
                {null, PADDING, wr(v -> v.wireOut().addPadding(3).getValueOut().text("hi"))},
                {null, FLOAT32, wr(v -> v.float32(1.2345678f))},
                {null, FLOAT64, wr(v -> v.float64(Math.PI))},
                {null, FLOAT_STOP_2, wr(v -> v.float32(1.23f))},
                {null, FLOAT_STOP_4, wr(v -> v.float32(0.0123f))},
                {null, FLOAT_STOP_6, wr(v -> v.float32(0.000123f))},
                {null, FLOAT_STOP_2, wr(v -> v.float64(1.23))},
                {null, FLOAT_STOP_4, wr(v -> v.float64(0.0123))},
                {null, FLOAT_STOP_6, wr(v -> v.float64(0.000123))},
//        public static final int FLOAT_SET_LOW_0 = 0x9A;
//        public static final int FLOAT_SET_LOW_2 = 0x9B;
//        public static final int FLOAT_SET_LOW_4 = 0x9C;
                {null, UUID, wr(v -> v.uuid(java.util.UUID.randomUUID()))},
                {null, UINT8, wr(v -> v.uint8(129))},
                {null, UINT16, wr(v -> v.uint16(65500))},
                {null, UINT32, wr(v -> v.uint32(3L << 30))},
                {null, INT8, wr(v -> v.int8(-128))},
                {null, INT16, wr(v -> v.int16(-32000))},
                {null, INT32, wr(v -> v.int32(-65555))},
                {null, INT64, wr(v -> v.int64(1234567890123456789L))},
//        public static final int SET_LOW_INT8 = 0xA8;
//        public static final int SET_LOW_INT16 = 0xA9;
                {null, INT64_0x, wr(v -> v.int64_0x(1L << 63))},
                {null, FALSE, wr(v -> v.bool(false))},
                {null, TRUE, wr(v -> v.bool(true))},
                {null, TIME, wr(v -> v.time(LocalTime.NOON))},
                {null, DATE, wr(v -> v.date(LocalDate.now()))},
                {null, DATE_TIME, wr(v -> v.dateTime(LocalDateTime.now()))},
                {null, ZONED_DATE_TIME, wr(v -> v.zonedDateTime(ZonedDateTime.now()))},
                {null, TYPE_PREFIX, wr(v -> v.object(new Dto8()))},
//        public static final int FIELD_NAME_ANY = 0xB7;
                {null, STRING_ANY, wr(v -> v.text("Long string 012345678901234567890123456789"))},
//        public static final int EVENT_NAME = 0xB9;
//        public static final int FIELD_NUMBER = 0xBA;
                {null, NULL, wr(v -> v.nu11())},
                {null, TYPE_LITERAL, wr(v -> v.typeLiteral(String.class))},
//        public static final int EVENT_OBJECT = 0xBD;
                {null, COMMENT, wr(v -> v.wireOut().writeComment("hi").getValueOut().text("hi"))},
//        public static final int HINT = 0xBF;
//        public static final int FIELD_NAME0 = 0xC0;
//        public static final int FIELD_NAME31 = 0xDF;
                {null, STRING_0, wr(v -> v.text(""))},
                {null, STRING_31, wr(v -> v.text("0123456789012345678901234567890"))},
        };

        // Map to associate the integer wire code with its name.
        Map<Integer, String> codeName = new HashMap<>();
        for (Field field : BinaryWireCode.class.getDeclaredFields()) {
            if (field.getType() == int.class)
                codeName.put(field.getInt(null), field.getName());
        }

        // Update the test case array to set the wire code name.
        for (Object[] objects : list) {
            objects[0] = codeName.get(objects[1]);
            assertNotNull(objects[0]);
        }

        // Return the populated test cases as a list.
        return Arrays.asList(list);
    }

    @Test
    public void test() {
        // Creates a new wire using the BINARY WireType.
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());

        // This accepts the Consumer function defined for the test instance and operates on wire's output.
        valueOutConsumer.accept(wire.getValueOut());

        // Writes the name to the wire's output.
        wire.getValueOut().text(name);

        // Checks the wire's byte content matches the expected 'code'.
        assertEquals(code, wire.bytes().peekUnsignedByte());

        // Reset the reading position of the wire's underlying bytes.
        wire.bytes().readPosition(0);

        // Skip the first value in the wire.
        wire.getValueIn().skipValue();

        // Checks that the next value read from the wire is the expected 'name'.
        assertEquals(name, wire.getValueIn().text());
    }

    // Dto8 is a data transfer object (DTO) that extends the SelfDescribingMarshallable,
    // which allows it to describe its own serialization/deserialization behavior.
    static class Dto8 extends SelfDescribingMarshallable {
        // Stores the current system time in nanoseconds when an instance of Dto8 is created.
        long time = System.nanoTime();

        @Override
        // This method returns the length of binary representation for instances of Dto8.
        // Here, it specifies that Dto8 instances will have an 8-bit binary length.
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_8BIT;
        }
    }

    // Dto16 is another DTO, similar to Dto8, but with a 16-bit binary length specification.
    static class Dto16 extends SelfDescribingMarshallable {
        // Stores the current system time in nanoseconds when an instance of Dto16 is created.
        long time = System.nanoTime();

        @Override
        // Specifies that Dto16 instances will have a 16-bit binary length.
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_16BIT;
        }
    }

    // Dto32 is yet another DTO, but with a 32-bit binary length specification.
    static class Dto32 extends SelfDescribingMarshallable {
        // Stores the current system time in nanoseconds when an instance of Dto32 is created.
        long time = System.nanoTime();

        @Override
        // Specifies that Dto32 instances will have a 32-bit binary length.
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_32BIT;
        }
    }

    // BM is a simple class that implements BytesMarshallable, allowing instances to be serialized to and
    // deserialized from Bytes directly.
    static class BM implements BytesMarshallable {
        // Stores the current system time in nanoseconds when an instance of BM is created.
        long time = System.nanoTime();
    }
}
