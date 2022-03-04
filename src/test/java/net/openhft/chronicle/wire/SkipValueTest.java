/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

@SuppressWarnings("rawtypes")
@RunWith(value = Parameterized.class)
public class SkipValueTest {

    private final String name;
    private final int code;
    private final Consumer<ValueOut> valueOutConsumer;

    public SkipValueTest(String name, int code, Consumer<ValueOut> valueOutConsumer) {
        this.name = name;
        this.code = code;
        this.valueOutConsumer = valueOutConsumer;
    }

    static Consumer<ValueOut> wr(Consumer<ValueOut> valueOutConsumer) {
        return valueOutConsumer;
    }

    @NotNull
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() throws IllegalAccessException {

        Object[][] list = {
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
        Map<Integer, String> codeName = new HashMap<>();
        for (Field field : BinaryWireCode.class.getDeclaredFields()) {
            if (field.getType() == int.class)
                codeName.put(field.getInt(null), field.getName());
        }
        for (Object[] objects : list) {
            objects[0] = codeName.get(objects[1]);
            assertNotNull(objects[0]);
        }
        return Arrays.asList(list);
    }

    @Test
    public void test() {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        valueOutConsumer.accept(wire.getValueOut());
        wire.getValueOut().text(name);
        assertEquals(code, wire.bytes().peekUnsignedByte());
        wire.bytes().readPosition(0);
        wire.getValueIn().skipValue();
        assertEquals(name, wire.getValueIn().text());
    }

    static class Dto8 extends SelfDescribingMarshallable {
        long time = System.nanoTime();

        @Override
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_8BIT;
        }
    }

    static class Dto16 extends SelfDescribingMarshallable {
        long time = System.nanoTime();

        @Override
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_16BIT;
        }
    }

    static class Dto32 extends SelfDescribingMarshallable {
        long time = System.nanoTime();

        @Override
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_32BIT;
        }
    }

    static class BM implements BytesMarshallable {
        long time = System.nanoTime();
    }
}
