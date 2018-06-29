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
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.NoBytesStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.RetentionPolicy;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.*;

@RunWith(value = Parameterized.class)
public class BinaryWireTest {

    final int testId;
    final boolean fixed;
    final boolean numericField;
    final boolean fieldLess;
    final int compressedSize;
    @NotNull
    Bytes bytes = nativeBytes();

    public BinaryWireTest(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        this.testId = testId;
        this.fixed = fixed;
        this.numericField = numericField;
        this.fieldLess = fieldLess;
        this.compressedSize = compressedSize;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{0, false, false, false, 128},
                new Object[]{1, false, false, false, 32},
                new Object[]{2, true, false, false, 128},
                new Object[]{3, false, true, false, 128},
                new Object[]{4, true, true, false, 128},
                new Object[]{5, false, false, true, 128},
                new Object[]{6, true, false, true, 128}
        );
    }

    @After
    public void after() {
//        BinaryWire.SPEC = 16;
    }

    @Test
    public void testWrite() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        checkWire(wire, "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁÀÀÀ‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁÀÀÀ‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁÀÀÀ‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁÀÀÀ‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁÀÀÀ‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ǁ‡",
                "[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ǁ‡");

        assertEquals(fieldLess ? "" : "\"\": \"\": \"\": ", TextWire.asText(wire));
    }

    @NotNull
    private BinaryWire createWire() {
        bytes.clear();
        @NotNull BinaryWire wire = new BinaryWire(bytes, fixed, numericField, fieldLess, compressedSize, "lzw", false);
        assert wire.startUse();
        return wire;
    }

    private void checkWire(@NotNull Wire wire, String... expected) {
        assertEquals("id: " + testId, expected[testId], wire.toString());
    }

    @Test
    public void testWrite1() {
        @NotNull Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        checkWire(wire, "[pos: 0, rlim: 21, wlim: 8EiB, cap: 8EiB ] ǁÆfield1Æfield2Æfield3‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 21, wlim: 8EiB, cap: 8EiB ] ǁÆfield1Æfield2Æfield3‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 21, wlim: 8EiB, cap: 8EiB ] ǁÆfield1Æfield2Æfield3‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ǁº⒈º⒉º⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ǁº⒈º⒉º⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ǁ‡",
                "[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ǁ‡");
        checkAsText(wire,
                "field1: field2: field3: ",
                "\"1\": \"2\": \"3\": ",
                "");
    }

    private void checkAsText(@NotNull Wire wire, String textFieldExcepted, String numberFieldExpected, String fieldLessExpected) {
        String text = TextWire.asText(wire);
        if (fieldLess)
            assertEquals(fieldLessExpected, text);
        else if (numericField)
            assertEquals(numberFieldExpected, text);
        else
            assertEquals(textFieldExcepted, text);
    }

    @Test
    public void testWrite2() {
        @NotNull Wire wire = createWire();
        wire.write(() -> "Hello");
        wire.write(() -> "World");
        @NotNull String name = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name);
        checkWire(wire, "[pos: 0, rlim: 67, wlim: 8EiB, cap: 8EiB ] ǁÅHelloÅWorld·5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 67, wlim: 8EiB, cap: 8EiB ] ǁÅHelloÅWorld·5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 67, wlim: 8EiB, cap: 8EiB ] ǁÅHelloÅWorld·5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 17, wlim: 8EiB, cap: 8EiB ] ǁº²Ñ\\u0098!ºòÖø'º´Íýå\\u0083٠" + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 17, wlim: 8EiB, cap: 8EiB ] ǁº²Ñ\\u0098!ºòÖø'º´Íýå\\u0083٠" + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ǁ‡",
                "[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ǁ‡");
        assertEquals(numericField ? "\"69609650\": \"83766130\": \"-1019176629\": " :
                fieldLess ? "" : "Hello: World: \"" + name + "\": ", TextWire.asText(wire));
    }

    @Test
    public void testRead() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");
        checkAsText(wire, "\"\": field1: Test: ",
                "\"\": \"1\": \"2603186\": ",
                "");

        wire.read();
        wire.read();
        wire.read();
        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead1() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");
        checkAsText(wire, "\"\": field1: Test: ",
                "\"\": \"1\": \"2603186\": ",
                "");

        // ok as blank matches anything
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        // not a match
        wire.read(BWKey.field1);
        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead2() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        @NotNull String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name1);

        // ok as blank matches anything
        @NotNull StringBuilder name = new StringBuilder();
        wire.read(name);
        assertEquals(0, name.length());

        name.setLength(0);
        wire.read(name);
        assertEquals(numericField ? "1" : fieldLess ? "" : BWKey.field1.name(), name.toString());

        name.setLength(0);
        wire.read(name);
        assertEquals(numericField ? "-1019176629" : fieldLess ? "" : name1, name.toString());

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int8() {
        @NotNull Wire wire = createWire();
        wire.write().int8((byte) 1);
        wire.write(BWKey.field1).int8((byte) 2);
        wire.write(() -> "Test").int8((byte) 3);
        checkWire(wire, "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 19, wlim: 8EiB, cap: 8EiB ] ǁÀ¤⒈Æfield1¤⒉ÄTest¤⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 11, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈º⒈⒉º²ñ\\u009E⒈⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 14, wlim: 8EiB, cap: 8EiB ] ǁÀ¤⒈º⒈¤⒉º²ñ\\u009E⒈¤⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ǁ¤⒈¤⒉¤⒊‡٠٠٠٠٠٠٠٠");
        checkAsText123(wire, fixed ? "!byte " : "");

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int8(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    private void checkAsText123(@NotNull Wire wire) {
        checkAsText123(wire, "");
    }

    private void checkAsText123(@NotNull Wire wire, String type) {
        checkAsText(wire, "\"\": " + type + "1\n" +
                        "field1: " + type + "2\n" +
                        "Test: " + type + "3\n",
                "\"\": " + type + "1\n" +
                        "\"1\": " + type + "2\n" +
                        "\"2603186\": " + type + "3\n",
                "" + type + "1\n" +
                        "" + type + "2\n" +
                        "" + type + "3\n"
        );
    }

    private void checkAsText123_0(@NotNull Wire wire) {
        checkAsText(wire, "\"\": 1.0\n" +
                        "field1: 2.0\n" +
                        "Test: 3.0\n",
                "\"\": 1.0\n" +
                        "\"1\": 2.0\n" +
                        "\"2603186\": 3.0\n",
                "1.0\n" +
                        "2.0\n" +
                        "3.0\n"
        );
    }

    @Test
    public void int16() {
        @NotNull Wire wire = createWire();
        wire.write().int16((short) 1);
        wire.write(BWKey.field1).int16((short) 2);
        wire.write(() -> "Test").int16((short) 3);
        checkWire(wire, "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 22, wlim: 8EiB, cap: 8EiB ] ǁÀ¥⒈٠Æfield1¥⒉٠ÄTest¥⒊٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 11, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈º⒈⒉º²ñ\\u009E⒈⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 17, wlim: 8EiB, cap: 8EiB ] ǁÀ¥⒈٠º⒈¥⒉٠º²ñ\\u009E⒈¥⒊٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 9, wlim: 8EiB, cap: 8EiB ] ǁ¥⒈٠¥⒉٠¥⒊٠‡٠٠٠٠٠٠٠٠");
        checkAsText123(wire, fixed ? "!short " : "");

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int16(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void uint8() {
        @NotNull Wire wire = createWire();
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);
        wire.write(() -> "Test").uint8(3);
        checkWire(wire, "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 19, wlim: 8EiB, cap: 8EiB ] ǁÀ¡⒈Æfield1¡⒉ÄTest¡⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 11, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈º⒈⒉º²ñ\\u009E⒈⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 14, wlim: 8EiB, cap: 8EiB ] ǁÀ¡⒈º⒈¡⒉º²ñ\\u009E⒈¡⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ǁ¡⒈¡⒉¡⒊‡٠٠٠٠٠٠٠٠");
        checkAsText123(wire, fixed ? "!int " : "");

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint8(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void uint16() {
        @NotNull Wire wire = createWire();
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);
        wire.write(() -> "Test").uint16(3);
        checkWire(wire, "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 22, wlim: 8EiB, cap: 8EiB ] ǁÀ¢⒈٠Æfield1¢⒉٠ÄTest¢⒊٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 11, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈º⒈⒉º²ñ\\u009E⒈⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 17, wlim: 8EiB, cap: 8EiB ] ǁÀ¢⒈٠º⒈¢⒉٠º²ñ\\u009E⒈¢⒊٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 9, wlim: 8EiB, cap: 8EiB ] ǁ¢⒈٠¢⒉٠¢⒊٠‡٠٠٠٠٠٠٠٠");
        checkAsText123(wire, fixed ? "!int " : "");

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint16(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void uint32() {
        @NotNull Wire wire = createWire();
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);
        wire.write(() -> "Test").uint32(3);
        checkWire(wire, "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 28, wlim: 8EiB, cap: 8EiB ] ǁÀ£⒈٠٠٠Æfield1£⒉٠٠٠ÄTest£⒊٠٠٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 11, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈º⒈⒉º²ñ\\u009E⒈⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 23, wlim: 8EiB, cap: 8EiB ] ǁÀ£⒈٠٠٠º⒈£⒉٠٠٠º²ñ\\u009E⒈£⒊٠٠٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 15, wlim: 8EiB, cap: 8EiB ] ǁ£⒈٠٠٠£⒉٠٠٠£⒊٠٠٠‡٠٠٠٠٠٠٠٠");
        checkAsText123(wire);

        // ok as blank matches anything
        @NotNull AtomicLong i = new AtomicLong();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint32(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int32() {
        @NotNull Wire wire = createWire();
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);
        wire.write(() -> "Test").int32(3);
        checkWire(wire, "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 28, wlim: 8EiB, cap: 8EiB ] ǁÀ¦⒈٠٠٠Æfield1¦⒉٠٠٠ÄTest¦⒊٠٠٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 11, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈º⒈⒉º²ñ\\u009E⒈⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 23, wlim: 8EiB, cap: 8EiB ] ǁÀ¦⒈٠٠٠º⒈¦⒉٠٠٠º²ñ\\u009E⒈¦⒊٠٠٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 15, wlim: 8EiB, cap: 8EiB ] ǁ¦⒈٠٠٠¦⒉٠٠٠¦⒊٠٠٠‡٠٠٠٠٠٠٠٠");
        checkAsText123(wire, fixed ? "!int " : "");

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int32(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int64() {
        @NotNull Wire wire = createWire();
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write(() -> "Test").int64(3);
        checkWire(wire, "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 40, wlim: 8EiB, cap: 8EiB ] ǁÀ§⒈٠٠٠٠٠٠٠Æfield1§⒉٠٠٠٠٠٠٠ÄTest§⒊٠٠٠٠٠٠٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 11, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈º⒈⒉º²ñ\\u009E⒈⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 35, wlim: 8EiB, cap: 8EiB ] ǁÀ§⒈٠٠٠٠٠٠٠º⒈§⒉٠٠٠٠٠٠٠º²ñ\\u009E⒈§⒊٠٠٠٠٠٠٠‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 27, wlim: 8EiB, cap: 8EiB ] ǁ§⒈٠٠٠٠٠٠٠§⒉٠٠٠٠٠٠٠§⒊٠٠٠٠٠٠٠‡٠٠٠٠٠٠٠٠");
        checkAsText123(wire);

        // ok as blank matches anything
        @NotNull AtomicLong i = new AtomicLong();
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testFloat64s() {
        @NotNull Wire wire = createWire();
        for (double d : new double[]{
                2.358662e9,
                Double.POSITIVE_INFINITY, Double.NaN,
                3, 3 << 6, 3 << 7, 3 << 8, 3 << 14, 3 << 15, 1 + (3L << 29), 1 + (3L << 30), 1 + (3L << 31), 3L << 52, 3L << 53
        }) {
            wire.clear();

            wire.write("p")
                    .float64(d)
                    .write("n").float64(-d)
                    .write("t").text("hi");

            assertEquals(d, wire.read("p").float64(), 0);
            assertEquals(-d, wire.read("n").float64(), 0);
            assertEquals("hi", wire.read("t").text());
        }
    }

    @Test
    public void float64() {
        @NotNull Wire wire = createWire();
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);
        wire.write(() -> "Test").float64(3);
        checkWire(wire, "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 16, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈Æfield1⒉ÄTest⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 40, wlim: 8EiB, cap: 8EiB ] ǁÀ\\u0091٠٠٠٠٠٠ð?Æfield1\\u0091٠٠٠٠٠٠٠@ÄTest\\u0091٠٠٠٠٠٠⒏@‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 11, wlim: 8EiB, cap: 8EiB ] ǁÀ⒈º⒈⒉º²ñ\\u009E⒈⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 35, wlim: 8EiB, cap: 8EiB ] ǁÀ\\u0091٠٠٠٠٠٠ð?º⒈\\u0091٠٠٠٠٠٠٠@º²ñ\\u009E⒈\\u0091٠٠٠٠٠٠⒏@‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 27, wlim: 8EiB, cap: 8EiB ] ǁ\\u0091٠٠٠٠٠٠ð?\\u0091٠٠٠٠٠٠٠@\\u0091٠٠٠٠٠٠⒏@‡٠٠٠٠٠٠٠٠");
        if (wire.getValueOut() instanceof BinaryWire.BinaryValueOut)
            checkAsText123(wire);
        else
            checkAsText123_0(wire);
        wire.write().float64(0);

        // ok as blank matches anything
        class Floater {
            double f;

            public void set(double d) {
                f = d;
            }
        }
        @NotNull Floater n = new Floater();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().float64(n, Floater::set);
            assertEquals(e, n.f, 0.0);
        });
        assertEquals(0.0, wire.read().float64(), 0.0);

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void text() {
        @NotNull String name = "Long field name which is more than 32 characters, Bye";

        @NotNull Wire wire = createWire();
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        wire.write(() -> "Test").text(name);
        checkWire(wire, "[pos: 0, rlim: 80, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloÆfield1åworldÄTest¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 80, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloÆfield1åworldÄTest¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 80, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloÆfield1åworldÄTest¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 75, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloº⒈åworldº²ñ\\u009E⒈¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 75, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloº⒈åworldº²ñ\\u009E⒈¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 67, wlim: 8EiB, cap: 8EiB ] ǁåHelloåworld¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 67, wlim: 8EiB, cap: 8EiB ] ǁåHelloåworld¸5" + name + "‡٠٠٠٠٠٠٠٠");
        checkAsText(wire, "\"\": Hello\n" +
                        "field1: world\n" +
                        "Test: \"" + name + "\"\n",
                "\"\": Hello\n" +
                        "\"1\": world\n" +
                        "\"2603186\": \"" + name + "\"\n",
                "Hello\n" +
                        "world\n" +
                        "\"" + name + "\"\n");

        // ok as blank matches anything
        @NotNull StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name).forEach(e -> {
            wire.read().textTo(sb);
            assertEquals(e, sb.toString());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void type() {
        @NotNull Wire wire = createWire();
        wire.write().typePrefix("MyType");
        wire.write(BWKey.field1).typePrefix("AlsoMyType");
        @NotNull String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write(() -> "Test").typePrefix(name1);
        checkWire(wire, "[pos: 0, rlim: 158, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 158, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 158, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 153, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeº⒈¶⒑AlsoMyTypeº²ñ\\u009E⒈¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 153, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeº⒈¶⒑AlsoMyTypeº²ñ\\u009E⒈¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 145, wlim: 8EiB, cap: 8EiB ] ǁ¶⒍MyType¶⒑AlsoMyType¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 145, wlim: 8EiB, cap: 8EiB ] ǁ¶⒍MyType¶⒑AlsoMyType¶{" + name1 + "‡٠٠٠٠٠٠٠٠");
        checkAsText(wire, "\"\": !MyType field1: !AlsoMyType Test: !" + name1 + " ",
                "\"\": !MyType \"1\": !AlsoMyType \"2603186\": !" + name1 + " ",
                "!MyType !AlsoMyType !" + name1 + " ");

        // ok as blank matches anything
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().typePrefix(e, (expected, actual) -> Assert.assertEquals(expected, actual.toString()));
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testBool() {
        @NotNull Wire wire = createWire();
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);
        System.out.println(wire);
        wire.read().bool(false, Assert::assertEquals)
                .read().bool(true, Assert::assertEquals)
                .read().bool(null, Assert::assertEquals);
    }

    @Test
    public void testFloat32() {
        @NotNull Wire wire = createWire();
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY)
                .write().float32(Float.NEGATIVE_INFINITY)
                .write().float32(123456.0f);
        wire.read().float32(this, (o, t) -> assertEquals(0.0F, t, 0.0F))
                .read().float32(this, (o, t) -> assertTrue(Float.isNaN(t)))
                .read().float32(this, (o, t) -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(Float.NEGATIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(123456.0f, t, 0.0F));
    }

    @Test
    public void testTime() {
        @NotNull Wire wire = createWire();
        LocalTime now = LocalTime.of(12, 54, 4, 612 * 1000000);
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);
        if (testId <= 4) {
            assertEquals("00000000 c0 b2 0c 31 32 3a 35 34  3a 30 34 2e 36 31 32 c0 ···12:54 :04.612·\n" +
                            "00000010 b2 12 32 33 3a 35 39 3a  35 39 2e 39 39 39 39 39 ··23:59: 59.99999\n" +
                            "00000020 39 39 39 39 c0 b2 05 30  30 3a 30 30             9999···0 0:00    \n",
                    bytes.toHexString());
        } else {
            assertEquals("00000000 b2 0c 31 32 3a 35 34 3a  30 34 2e 36 31 32 b2 12 ··12:54: 04.612··\n" +
                            "00000010 32 33 3a 35 39 3a 35 39  2e 39 39 39 39 39 39 39 23:59:59 .9999999\n" +
                            "00000020 39 39 b2 05 30 30 3a 30  30                      99··00:0 0       \n",
                    bytes.toHexString());
        }
        wire.read().time(now, Assert::assertEquals)
                .read().time(LocalTime.MAX, Assert::assertEquals)
                .read().time(LocalTime.MIN, Assert::assertEquals);
    }

    @Test
    public void testZonedDateTime() {
        @NotNull Wire wire = createWire();
        ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        final ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());
        wire.write().zonedDateTime(now)
                .write().zonedDateTime(max)
                .write().zonedDateTime(min);
        wire.read().zonedDateTime(now, Assert::assertEquals)
                .read().zonedDateTime(max, Assert::assertEquals)
                .read().zonedDateTime(min, Assert::assertEquals);

        wire.write().object(now)
                .write().object(max)
                .write().object(min);
        wire.read().object(Object.class, now, Assert::assertEquals)
                .read().object(Object.class, max, Assert::assertEquals)
                .read().object(Object.class, min, Assert::assertEquals);
    }

    @Test
    public void testDate() {
        @NotNull Wire wire = createWire();
        LocalDate now = LocalDate.now();
        wire.write().date(now)
                .write().date(LocalDate.MAX)
                .write().date(LocalDate.MIN);
        wire.read().date(now, Assert::assertEquals)
                .read().date(LocalDate.MAX, Assert::assertEquals)
                .read().date(LocalDate.MIN, Assert::assertEquals);
    }

    @Test
    public void testUuid() {
        @NotNull Wire wire = createWire();
        UUID uuid = UUID.randomUUID();
        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));
        wire.read().uuid(uuid, Assert::assertEquals)
                .read().uuid(new UUID(0, 0), Assert::assertEquals)
                .read().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), Assert::assertEquals);
    }

    @Test
    public void testBytes() {
        @NotNull Wire wire = createWire();
        @NotNull byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;
        wire.write().bytes(NoBytesStore.NO_BYTES)
                .write().bytes(Bytes.wrapForRead("Hello".getBytes(ISO_8859_1)))
                .write().bytes(Bytes.wrapForRead("quotable, text".getBytes(ISO_8859_1)))
                .write()
                .bytes(allBytes);
        System.out.println(bytes.toDebugString());
        @NotNull NativeBytes allBytes2 = nativeBytes();
        wire.read().bytes(b -> assertEquals(0, b.readRemaining()))
                .read().bytes(b -> assertEquals("Hello", b.toString()))
                .read().bytes(b -> assertEquals("quotable, text", b.toString()))
                .read()
                .bytes(allBytes2);
        assertEquals(Bytes.wrapForRead(allBytes), allBytes2);
    }

    @Test
    public void testWriteMarshallable() {
//        BinaryWire.SPEC = 18;

        @NotNull Wire wire = createWire();
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.b = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;
        mtA.text.append("Hello World");

        wire.write(() -> "A").marshallable(mtA);

        @NotNull MyTypesCustom mtB = new MyTypesCustom();
        mtB.b = false;
        mtB.d = 123.4567;
        mtB.i = -123457890;
        mtB.s = (short) 1234;
        mtB.text.append("Bye now");
        wire.write(() -> "B").marshallable(mtB);

        //        System.out.println(wire.bytes().toDebugString(400));
        checkWire(wire,
                "[pos: 0, rlim: 134, wlim: 8EiB, cap: 8EiB ] ǁÁA\\u0082>٠٠٠ÆB_FLAG±ÅS_NUM¥90ÅD_NUM\\u0094\\u0080\u00ADKÅL_NUM٠ÅI_NUM¦C\\u009ECÿÄTEXTëHello WorldÁB\\u0082:٠٠٠ÆB_FLAG°ÅS_NUM¥Ò⒋ÅD_NUM\\u0094\\u0087\u00ADKÅL_NUM٠ÅI_NUM¦\\u009E.¤øÄTEXTçBye now‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 134, wlim: 8EiB, cap: 8EiB ] ǁÁA\\u0082>٠٠٠ÆB_FLAG±ÅS_NUM¥90ÅD_NUM\\u0094\\u0080\u00ADKÅL_NUM٠ÅI_NUM¦C\\u009ECÿÄTEXTëHello WorldÁB\\u0082:٠٠٠ÆB_FLAG°ÅS_NUM¥Ò⒋ÅD_NUM\\u0094\\u0087\u00ADKÅL_NUM٠ÅI_NUM¦\\u009E.¤øÄTEXTçBye now‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 160, wlim: 8EiB, cap: 8EiB ] ǁÁA\\u0082K٠٠٠ÆB_FLAG±ÅS_NUM¥90ÅD_NUM\\u0091w¾\\u009F\\u001A/Ý^@ÅL_NUM§٠٠٠٠٠٠٠٠ÅI_NUM¦C\\u009ECÿÄTEXTëHello WorldÁB\\u0082G٠٠٠ÆB_FLAG°ÅS_NUM¥Ò⒋ÅD_NUM\\u0091S⒌£\\u0092:Ý^@ÅL_NUM§٠٠٠٠٠٠٠٠ÅI_NUM¦\\u009E.¤øÄTEXTçBye now‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 86, wlim: 8EiB, cap: 8EiB ] ǁºA\\u0082&٠٠٠º٠±º⒈¥90º⒉\\u0094\\u0080\u00ADKº⒊٠º⒋¦C\\u009ECÿº⒌ëHello WorldºB\\u0082\"٠٠٠º٠°º⒈¥Ò⒋º⒉\\u0094\\u0087\u00ADKº⒊٠º⒋¦\\u009E.¤øº⒌çBye now‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 112, wlim: 8EiB, cap: 8EiB ] ǁºA\\u00823٠٠٠º٠±º⒈¥90º⒉\\u0091w¾\\u009F\\u001A/Ý^@º⒊§٠٠٠٠٠٠٠٠º⒋¦C\\u009ECÿº⒌ëHello WorldºB\\u0082/٠٠٠º٠°º⒈¥Ò⒋º⒉\\u0091S⒌£\\u0092:Ý^@º⒊§٠٠٠٠٠٠٠٠º⒋¦\\u009E.¤øº⒌çBye now‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 58, wlim: 8EiB, cap: 8EiB ] ǁ\\u0082\\u001A٠٠٠±¥90\\u0094\\u0080\u00ADK٠¦C\\u009ECÿëHello World\\u0082\\u0016٠٠٠°¥Ò⒋\\u0094\\u0087\u00ADK٠¦\\u009E.¤øçBye now‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 84, wlim: 8EiB, cap: 8EiB ] ǁ\\u0082'٠٠٠±¥90\\u0091w¾\\u009F\\u001A/Ý^@§٠٠٠٠٠٠٠٠¦C\\u009ECÿëHello World\\u0082#٠٠٠°¥Ò⒋\\u0091S⒌£\\u0092:Ý^@§٠٠٠٠٠٠٠٠¦\\u009E.¤øçBye now‡٠٠٠٠٠٠٠٠");
        @NotNull MyTypesCustom mt2 = new MyTypesCustom();
        wire.read(() -> "A").marshallable(mt2);
        assertEquals(mt2, mtA);

        wire.read(() -> "B").marshallable(mt2);
        assertEquals(mt2, mtB);
    }

    @Test
    public void writeNull() {
        @NotNull Wire wire = createWire();
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);

        @Nullable Object o = wire.read().object(Object.class);
        assertEquals(null, o);
        @Nullable String s = wire.read().object(String.class);
        assertEquals(null, s);
        @Nullable RetentionPolicy rp = wire.read().object(RetentionPolicy.class);
        assertEquals(null, rp);
        @Nullable Circle c = wire.read().object(Circle.class);
        assertEquals(null, c);
    }

    @Test
    public void testLongString() {
        @NotNull Wire wire = createWire();
        @NotNull char[] chars = new char[16];
        for (int i = 0; i < Character.MAX_VALUE; i++) {
            if (!Character.isValidCodePoint(i))
                continue;
            wire.clear();
            Arrays.fill(chars, (char) i);
            @NotNull String s = new String(chars);
            wire.writeDocument(false, w -> w.write(() -> "msg").text(s));

            System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));
            wire.readDocument(null, w -> w.read(() -> "msg").text(s, Assert::assertEquals));
        }
    }

    @Test
    public void testArrays() {
        @NotNull Wire wire = createWire();

        @NotNull Object[] noObjects = {};
        wire.write("a").object(noObjects);

        System.out.println(wire.asText());
        @Nullable Object[] object = wire.read()
                .object(Object[].class);
        assertEquals(0, object.length);

        @NotNull Object[] threeObjects = {"abc", "def", "ghi"};
        wire.write("b").object(threeObjects);
        System.out.println(wire.asText());

        @Nullable Object[] object2 = wire.read()
                .object(Object[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    @Test
    @Ignore("TODO FIX")
    public void testArrays2() {
        @NotNull Wire wire = createWire();
        @NotNull Object[] a1 = new Object[0];
        wire.write("empty").object(a1);
        @NotNull Object[] a2 = {1L};
        wire.write("one").object(a2);
        @NotNull Object[] a3 = {"Hello", 123, 10.1};
        wire.write("three").object(Object[].class, a3);

        @Nullable Object o1 = wire.read().object(Object[].class);
        assertArrayEquals(a1, (Object[]) o1);
        @Nullable Object o2 = wire.read().object(Object[].class);
        assertArrayEquals(a2, (Object[]) o2);
        @Nullable Object o3 = wire.read().object(Object[].class);
        assertArrayEquals(a3, (Object[]) o3);
    }

    @Test
    public void testUsingEvents() throws Exception {

        final Wire w = WireType.BINARY.apply(Bytes.elasticByteBuffer());

        try (DocumentContext dc = w.writingDocument(false)) {
            dc.wire().writeEventName("hello1").typedMarshallable(new DTO("world1"));
            dc.wire().writeEventName("hello2").typedMarshallable(new DTO("world2"));
            dc.wire().writeEventName("hello3").typedMarshallable(new DTO("world3"));
        }

        try (DocumentContext dc = w.readingDocument()) {

            System.out.println(Wires.fromSizePrefixedBlobs(dc));

            StringBuilder sb = Wires.acquireStringBuilder();

            @NotNull ValueIn valueIn1 = dc.wire().readEventName(sb);
            Assert.assertTrue("hello1".contentEquals(sb));
            valueIn1.skipValue();

            @NotNull ValueIn valueIn2 = dc.wire().readEventName(sb);
            Assert.assertTrue("hello2".contentEquals(sb));

            valueIn2.skipValue(); // if you change this to typed marshable it works

            @NotNull ValueIn valueIn3 = dc.wire().readEventName(sb);
            Assert.assertTrue("hello3".contentEquals(sb));

            @Nullable DTO o = valueIn3.typedMarshallable();
            Assert.assertEquals("world3", o.text);
        }
        w.bytes().release();
    }

    @Test
    public void testSortedSet() {
        @NotNull Wire wire = createWire();
        @NotNull SortedSet<String> set = new TreeSet<>();
        set.add("one");
        set.add("two");
        set.add("three");
        wire.write("a").object(set);

        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedSet);
        assertEquals(set, o);
    }

    @Test
    public void testSortedMap() {
        @NotNull Wire wire = createWire();
        @NotNull SortedMap<String, Long> set = new TreeMap<>();
        set.put("one", 1L);
        set.put("two", 2L);
        set.put("three", 3L);
        wire.write("a").object(set);

        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedMap);
        assertEquals(set, o);
    }

    @Test
    public void testSkipPadding() {
        @NotNull Wire wire = createWire();
        for (int i = 1; i <= 128; i *= 2) {
            wire.addPadding(i);
            wire.getValueIn().skipValue();
            assertEquals(0, wire.bytes().readRemaining());
            wire.clear();
        }
        for (int i = 1; i <= 128; i *= 2) {
            wire.addPadding(i);
            int finalI = i;
            wire.getValueOut().marshallable(w -> w.write("i").int32(finalI));
            wire.getValueIn().skipValue();
            assertEquals(0, wire.bytes().readRemaining());
            wire.clear();
        }
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    enum BWKey implements WireKey {
        field1(1), field2(2), field3(3);

        private final int code;

        BWKey(int code) {
            this.code = code;
        }

        @Override
        public int code() {
            return code;
        }

    }

    private static class DTO extends AbstractMarshallable {

        String text;

        DTO(String text) {
            this.text = text;
        }

    }

}