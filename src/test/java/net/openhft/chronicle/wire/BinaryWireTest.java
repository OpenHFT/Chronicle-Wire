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
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.NoBytesStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
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
public class BinaryWireTest extends WireTestCommon {

    final int testId;
    final boolean fixed;
    final boolean numericField;
    final boolean fieldLess;
    final int compressedSize;
    @SuppressWarnings("rawtypes")
    @NotNull
    Bytes bytes = new HexDumpBytes();

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

    @Override
    public void assertReferencesReleased() {
        bytes.releaseLast();
        super.assertReferencesReleased();
    }

    @Test
    public void testWrite() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        checkWire(wire, "" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "",
                "");

        assertEquals(fieldLess ? "" : "\"\": \"\": \"\": ", TextWire.asText(wire));
    }

    @Test
    public void readWriteString() {
        String utfCharacter = "ä";
        @NotNull Wire wire = createWire();
        wire.getValueOut()
                .writeString(utfCharacter);

        assertEquals(utfCharacter, wire.getValueIn()
                .readString());
    }

    @NotNull
    private BinaryWire createWire() {
        bytes.clear();
        @NotNull BinaryWire wire = new BinaryWire(bytes, fixed, numericField, fieldLess, compressedSize, "lzw", false);
        wire.usePadding(true);
        assert wire.startUse();
        return wire;
    }

    private void checkWire(@NotNull Wire wire, String... expected) {
        if (expected[0].startsWith("["))
            System.out.println("\"\" +\n\"" + (wire.bytes().toHexString().replaceAll("\n", "\\\\n\" +\n\"") + "\",").replace(" +\n\"\",", ","));
        else
            assertEquals("id: " + testId,
                    expected[testId],
                    wire.bytes().toHexString());
    }

    private void checkWire2(@NotNull Wire wire, String... expected) {
        assertEquals("id: " + testId,
                expected[testId].replaceAll("٠+$", ""),
                wire.bytes().toDebugString(9999).replaceAll("٠+$", ""));
    }

    @Test
    public void testWrite1() {
        @NotNull Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        checkWire(wire, "" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "c6 66 69 65 6c 64 32                            # field2:\n" +
                        "c6 66 69 65 6c 64 33                            # field3:\n",
                "" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "c6 66 69 65 6c 64 32                            # field2:\n" +
                        "c6 66 69 65 6c 64 33                            # field3:\n",
                "" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "c6 66 69 65 6c 64 32                            # field2:\n" +
                        "c6 66 69 65 6c 64 33                            # field3:\n",
                "" +
                        "ba 01                                           # 1\n" +
                        "ba 02                                           # 2\n" +
                        "ba 03                                           # 3\n",
                "" +
                        "ba 01                                           # 1\n" +
                        "ba 02                                           # 2\n" +
                        "ba 03                                           # 3\n",
                "",
                "");
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
        checkWire(wire, "" +
                        "c5 48 65 6c 6c 6f                               # Hello:\n" +
                        "c5 57 6f 72 6c 64                               # World:\n" +
                        "b7 35 4c 6f 6e 67 20 66 69 65 6c 64 20 6e 61 6d # Long field name which is more than 32 characters, Bye:\n" +
                        "65 20 77 68 69 63 68 20 69 73 20 6d 6f 72 65 20\n" +
                        "74 68 61 6e 20 33 32 20 63 68 61 72 61 63 74 65\n" +
                        "72 73 2c 20 42 79 65\n",
                "" +
                        "c5 48 65 6c 6c 6f                               # Hello:\n" +
                        "c5 57 6f 72 6c 64                               # World:\n" +
                        "b7 35 4c 6f 6e 67 20 66 69 65 6c 64 20 6e 61 6d # Long field name which is more than 32 characters, Bye:\n" +
                        "65 20 77 68 69 63 68 20 69 73 20 6d 6f 72 65 20\n" +
                        "74 68 61 6e 20 33 32 20 63 68 61 72 61 63 74 65\n" +
                        "72 73 2c 20 42 79 65\n",
                "" +
                        "c5 48 65 6c 6c 6f                               # Hello:\n" +
                        "c5 57 6f 72 6c 64                               # World:\n" +
                        "b7 35 4c 6f 6e 67 20 66 69 65 6c 64 20 6e 61 6d # Long field name which is more than 32 characters, Bye:\n" +
                        "65 20 77 68 69 63 68 20 69 73 20 6d 6f 72 65 20\n" +
                        "74 68 61 6e 20 33 32 20 63 68 61 72 61 63 74 65\n" +
                        "72 73 2c 20 42 79 65\n",
                "" +
                        "ba b2 d1 98 21                                  # 69609650\n" +
                        "ba f2 d6 f8 27                                  # 83766130\n" +
                        "ba b4 cd fd e5 83 00                            # -1019176629\n",
                "" +
                        "ba b2 d1 98 21                                  # 69609650\n" +
                        "ba f2 d6 f8 27                                  # 83766130\n" +
                        "ba b4 cd fd e5 83 00                            # -1019176629\n",
                "",
                "");
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
        checkWire(wire, "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a4 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a4 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a4 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a4 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a4 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a4 03                                           # 3\n",
                "" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "a4 01                                           # 1\n" +
                        "a4 02                                           # 2\n" +
                        "a4 03                                           # 3\n"
        );
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
        checkWire(wire, "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a5 01 00                                        # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a5 02 00                                        # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a5 03 00                                        # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a5 01 00                                        # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a5 02 00                                        # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a5 03 00                                        # 3\n",
                "" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "a5 01 00                                        # 1\n" +
                        "a5 02 00                                        # 2\n" +
                        "a5 03 00                                        # 3\n");
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
        checkWire(wire, "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n");
        checkAsText123(wire);

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
        checkWire(wire, "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a2 01 00                                        # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a2 02 00                                        # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a2 03 00                                        # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a2 01 00                                        # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a2 02 00                                        # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a2 03 00                                        # 3\n",
                "" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "a2 01 00                                        # 1\n" +
                        "a2 02 00                                        # 2\n" +
                        "a2 03 00                                        # 3\n");
        checkAsText123(wire);

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
        checkWire(wire, "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a3 01 00 00 00                                  # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a3 02 00 00 00                                  # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a3 03 00 00 00                                  # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a3 01 00 00 00                                  # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a3 02 00 00 00                                  # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a3 03 00 00 00                                  # 3\n",
                "" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "a3 01 00 00 00                                  # 1\n" +
                        "a3 02 00 00 00                                  # 2\n" +
                        "a3 03 00 00 00                                  # 3\n");
        checkAsText123(wire, fixed ? "!int " : "");

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
        checkWire(wire, "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a6 01 00 00 00                                  # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a6 02 00 00 00                                  # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a6 03 00 00 00                                  # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a6 01 00 00 00                                  # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a6 02 00 00 00                                  # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a6 03 00 00 00                                  # 3\n",
                "" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "a6 01 00 00 00                                  # 1\n" +
                        "a6 02 00 00 00                                  # 2\n" +
                        "a6 03 00 00 00                                  # 3\n");
        checkAsText123(wire);

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
        checkWire(wire, "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a7 01 00 00 00 00 00 00 00                      # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a7 02 00 00 00 00 00 00 00                      # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a7 03 00 00 00 00 00 00 00                      # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a7 01 00 00 00 00 00 00 00                      # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a7 02 00 00 00 00 00 00 00                      # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a7 03 00 00 00 00 00 00 00                      # 3\n",
                "" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "a7 01 00 00 00 00 00 00 00                      # 1\n" +
                        "a7 02 00 00 00 00 00 00 00                      # 2\n" +
                        "a7 03 00 00 00 00 00 00 00                      # 3\n");
        checkAsText123(wire, "");

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
        checkWire(wire, "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "91 00 00 00 00 00 00 f0 3f                      # 1.0\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "91 00 00 00 00 00 00 00 40                      # 2.0\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "91 00 00 00 00 00 00 08 40                      # 3.0\n",
                "" +
                        "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "c0                                              # :\n" +
                        "91 00 00 00 00 00 00 f0 3f                      # 1.0\n" +
                        "ba 01                                           # 1\n" +
                        "91 00 00 00 00 00 00 00 40                      # 2.0\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "91 00 00 00 00 00 00 08 40                      # 3.0\n",
                "" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
                "" +
                        "91 00 00 00 00 00 00 f0 3f                      # 1.0\n" +
                        "91 00 00 00 00 00 00 00 40                      # 2.0\n" +
                        "91 00 00 00 00 00 00 08 40                      # 3.0\n");
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
        checkWire2(wire, "[pos: 0, rlim: 80, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloÆfield1åworldÄTest¸5" + name + "‡٠٠٠٠٠٠٠٠",
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
        expectException("Unable to copy MyType safely will try anyway");
        expectException("Unable to copy AlsoMyType safely will try anyway");
        expectException("Unable to copy com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState safely will try anyway");

        @NotNull Wire wire = createWire();
        wire.write().typePrefix("MyType");
        wire.write(BWKey.field1).typePrefix("AlsoMyType");
        @NotNull String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write(() -> "Test").typePrefix(name1);
        checkWire2(wire, "[pos: 0, rlim: 158, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
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
        // System.out.println(wire);
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
            assertEquals("" +
                            "c0                                              # :\n" +
                            "b2 0c 31 32 3a 35 34 3a 30 34 2e 36 31 32       # 12:54:04.612\n" +
                            "c0                                              # :\n" +
                            "b2 12 32 33 3a 35 39 3a 35 39 2e 39 39 39 39 39 # 23:59:59.999999999\n" +
                            "39 39 39 39 c0                                  # :\n" +
                            "b2 05 30 30 3a 30 30                            # 00:00\n",
                    bytes.toHexString());
        } else {
            assertEquals("" +
                            "b2 0c 31 32 3a 35 34 3a 30 34 2e 36 31 32       # 12:54:04.612\n" +
                            "b2 12 32 33 3a 35 39 3a 35 39 2e 39 39 39 39 39 # 23:59:59.999999999\n" +
                            "39 39 39 39 b2 05 30 30 3a 30 30                # 00:00\n",
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
        // System.out.println(bytes.toDebugString());
        @SuppressWarnings("rawtypes")
        @NotNull NativeBytes allBytes2 = nativeBytes();
        wire.read().bytes(b -> assertEquals(0, b.readRemaining()))
                .read().bytes(b -> assertEquals("Hello", b.toString()))
                .read().bytes(b -> assertEquals("quotable, text", b.toString()))
                .read()
                .bytes(allBytes2);
        assertEquals(Bytes.wrapForRead(allBytes), allBytes2);
        allBytes2.releaseLast();
    }

    @Test
    public void testWriteMarshallable() {
        // BinaryWire.SPEC = 18;

        @NotNull Wire wire = createWire();
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;
        mtA.text.append("Hello World");

        wire.write(() -> "A").marshallable(mtA);

        @NotNull MyTypesCustom mtB = new MyTypesCustom();
        mtB.flag = false;
        mtB.d = 123.4567;
        mtB.i = -123457890;
        mtB.s = (short) 1234;
        mtB.text.append("Bye now");
        wire.write(() -> "B").marshallable(mtB);

        // System.out.println(wire.bytes().toDebugString(400));
        checkWire(wire,
                "" +
                        "c1 41                                           # A:\n" +
                        "82 3f 00 00 00                                  # MyTypesCustom\n" +
                        "c6 42 5f 46 4c 41 47 b1                         # B_FLAG:\n" +
                        "c5 53 5f 4e 55 4d                               # S_NUM:\n" +
                        "a5 39 30                                        # 12345\n" +
                        "c5 44 5f 4e 55 4d                               # D_NUM:\n" +
                        "94 80 ad 4b                                     # 1234560/1e4\n" +
                        "c5 4c 5f 4e 55 4d                               # L_NUM:\n" +
                        "a1 00                                           # 0\n" +
                        "c5 49 5f 4e 55 4d                               # I_NUM:\n" +
                        "a6 43 9e 43 ff                                  # -12345789\n" +
                        "c4 54 45 58 54                                  # TEXT:\n" +
                        "eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World\n" +
                        "c1 42                                           # B:\n" +
                        "82 3b 00 00 00                                  # MyTypesCustom\n" +
                        "c6 42 5f 46 4c 41 47 b0                         # B_FLAG:\n" +
                        "c5 53 5f 4e 55 4d                               # S_NUM:\n" +
                        "a5 d2 04                                        # 1234\n" +
                        "c5 44 5f 4e 55 4d                               # D_NUM:\n" +
                        "94 87 ad 4b                                     # 1234567/1e4\n" +
                        "c5 4c 5f 4e 55 4d                               # L_NUM:\n" +
                        "a1 00                                           # 0\n" +
                        "c5 49 5f 4e 55 4d                               # I_NUM:\n" +
                        "a6 9e 2e a4 f8                                  # -123457890\n" +
                        "c4 54 45 58 54                                  # TEXT:\n" +
                        "e7 42 79 65 20 6e 6f 77                         # Bye now\n",
                "" +
                        "c1 41                                           # A:\n" +
                        "82 3f 00 00 00                                  # MyTypesCustom\n" +
                        "c6 42 5f 46 4c 41 47 b1                         # B_FLAG:\n" +
                        "c5 53 5f 4e 55 4d                               # S_NUM:\n" +
                        "a5 39 30                                        # 12345\n" +
                        "c5 44 5f 4e 55 4d                               # D_NUM:\n" +
                        "94 80 ad 4b                                     # 1234560/1e4\n" +
                        "c5 4c 5f 4e 55 4d                               # L_NUM:\n" +
                        "a1 00                                           # 0\n" +
                        "c5 49 5f 4e 55 4d                               # I_NUM:\n" +
                        "a6 43 9e 43 ff                                  # -12345789\n" +
                        "c4 54 45 58 54                                  # TEXT:\n" +
                        "eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World\n" +
                        "c1 42                                           # B:\n" +
                        "82 3b 00 00 00                                  # MyTypesCustom\n" +
                        "c6 42 5f 46 4c 41 47 b0                         # B_FLAG:\n" +
                        "c5 53 5f 4e 55 4d                               # S_NUM:\n" +
                        "a5 d2 04                                        # 1234\n" +
                        "c5 44 5f 4e 55 4d                               # D_NUM:\n" +
                        "94 87 ad 4b                                     # 1234567/1e4\n" +
                        "c5 4c 5f 4e 55 4d                               # L_NUM:\n" +
                        "a1 00                                           # 0\n" +
                        "c5 49 5f 4e 55 4d                               # I_NUM:\n" +
                        "a6 9e 2e a4 f8                                  # -123457890\n" +
                        "c4 54 45 58 54                                  # TEXT:\n" +
                        "e7 42 79 65 20 6e 6f 77                         # Bye now\n",
                "" +
                        "c1 41                                           # A:\n" +
                        "82 4b 00 00 00                                  # MyTypesCustom\n" +
                        "c6 42 5f 46 4c 41 47 b1                         # B_FLAG:\n" +
                        "c5 53 5f 4e 55 4d                               # S_NUM:\n" +
                        "a5 39 30                                        # 12345\n" +
                        "c5 44 5f 4e 55 4d                               # D_NUM:\n" +
                        "91 77 be 9f 1a 2f dd 5e 40                      # 123.456\n" +
                        "c5 4c 5f 4e 55 4d                               # L_NUM:\n" +
                        "a7 00 00 00 00 00 00 00 00                      # 0\n" +
                        "c5 49 5f 4e 55 4d                               # I_NUM:\n" +
                        "a6 43 9e 43 ff                                  # -12345789\n" +
                        "c4 54 45 58 54                                  # TEXT:\n" +
                        "eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World\n" +
                        "c1 42                                           # B:\n" +
                        "82 47 00 00 00                                  # MyTypesCustom\n" +
                        "c6 42 5f 46 4c 41 47 b0                         # B_FLAG:\n" +
                        "c5 53 5f 4e 55 4d                               # S_NUM:\n" +
                        "a5 d2 04                                        # 1234\n" +
                        "c5 44 5f 4e 55 4d                               # D_NUM:\n" +
                        "91 53 05 a3 92 3a dd 5e 40                      # 123.4567\n" +
                        "c5 4c 5f 4e 55 4d                               # L_NUM:\n" +
                        "a7 00 00 00 00 00 00 00 00                      # 0\n" +
                        "c5 49 5f 4e 55 4d                               # I_NUM:\n" +
                        "a6 9e 2e a4 f8                                  # -123457890\n" +
                        "c4 54 45 58 54                                  # TEXT:\n" +
                        "e7 42 79 65 20 6e 6f 77                         # Bye now\n",
                "" +
                        "ba 41                                           # 65\n" +
                        "82 27 00 00 00                                  # MyTypesCustom\n" +
                        "ba 00 b1                                        # 0\n" +
                        "ba 01                                           # 1\n" +
                        "a5 39 30                                        # 12345\n" +
                        "ba 02                                           # 2\n" +
                        "94 80 ad 4b                                     # 1234560/1e4\n" +
                        "ba 03                                           # 3\n" +
                        "a1 00                                           # 0\n" +
                        "ba 04                                           # 4\n" +
                        "a6 43 9e 43 ff                                  # -12345789\n" +
                        "ba 05                                           # 5\n" +
                        "eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World\n" +
                        "ba 42                                           # 66\n" +
                        "82 23 00 00 00                                  # MyTypesCustom\n" +
                        "ba 00 b0                                        # 0\n" +
                        "ba 01                                           # 1\n" +
                        "a5 d2 04                                        # 1234\n" +
                        "ba 02                                           # 2\n" +
                        "94 87 ad 4b                                     # 1234567/1e4\n" +
                        "ba 03                                           # 3\n" +
                        "a1 00                                           # 0\n" +
                        "ba 04                                           # 4\n" +
                        "a6 9e 2e a4 f8                                  # -123457890\n" +
                        "ba 05                                           # 5\n" +
                        "e7 42 79 65 20 6e 6f 77                         # Bye now\n",
                "" +
                        "ba 41                                           # 65\n" +
                        "82 33 00 00 00                                  # MyTypesCustom\n" +
                        "ba 00 b1                                        # 0\n" +
                        "ba 01                                           # 1\n" +
                        "a5 39 30                                        # 12345\n" +
                        "ba 02                                           # 2\n" +
                        "91 77 be 9f 1a 2f dd 5e 40                      # 123.456\n" +
                        "ba 03                                           # 3\n" +
                        "a7 00 00 00 00 00 00 00 00                      # 0\n" +
                        "ba 04                                           # 4\n" +
                        "a6 43 9e 43 ff                                  # -12345789\n" +
                        "ba 05                                           # 5\n" +
                        "eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World\n" +
                        "ba 42                                           # 66\n" +
                        "82 2f 00 00 00                                  # MyTypesCustom\n" +
                        "ba 00 b0                                        # 0\n" +
                        "ba 01                                           # 1\n" +
                        "a5 d2 04                                        # 1234\n" +
                        "ba 02                                           # 2\n" +
                        "91 53 05 a3 92 3a dd 5e 40                      # 123.4567\n" +
                        "ba 03                                           # 3\n" +
                        "a7 00 00 00 00 00 00 00 00                      # 0\n" +
                        "ba 04                                           # 4\n" +
                        "a6 9e 2e a4 f8                                  # -123457890\n" +
                        "ba 05                                           # 5\n" +
                        "e7 42 79 65 20 6e 6f 77                         # Bye now\n",
                "" +
                        "82 1b 00 00 00 b1                               # MyTypesCustom\n" +
                        "a5 39 30                                        # 12345\n" +
                        "94 80 ad 4b                                     # 1234560/1e4\n" +
                        "a1 00                                           # 0\n" +
                        "a6 43 9e 43 ff                                  # -12345789\n" +
                        "eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World\n" +
                        "82 17 00 00 00 b0                               # MyTypesCustom\n" +
                        "a5 d2 04                                        # 1234\n" +
                        "94 87 ad 4b                                     # 1234567/1e4\n" +
                        "a1 00                                           # 0\n" +
                        "a6 9e 2e a4 f8                                  # -123457890\n" +
                        "e7 42 79 65 20 6e 6f 77                         # Bye now\n",
                "" +
                        "82 27 00 00 00 b1                               # MyTypesCustom\n" +
                        "a5 39 30                                        # 12345\n" +
                        "91 77 be 9f 1a 2f dd 5e 40                      # 123.456\n" +
                        "a7 00 00 00 00 00 00 00 00                      # 0\n" +
                        "a6 43 9e 43 ff                                  # -12345789\n" +
                        "eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World\n" +
                        "82 23 00 00 00 b0                               # MyTypesCustom\n" +
                        "a5 d2 04                                        # 1234\n" +
                        "91 53 05 a3 92 3a dd 5e 40                      # 123.4567\n" +
                        "a7 00 00 00 00 00 00 00 00                      # 0\n" +
                        "a6 9e 2e a4 f8                                  # -123457890\n" +
                        "e7 42 79 65 20 6e 6f 77                         # Bye now\n");
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
        assertNull(o);
        @Nullable String s = wire.read().object(String.class);
        assertNull(s);
        @Nullable RetentionPolicy rp = wire.read().object(RetentionPolicy.class);
        assertNull(rp);
        @Nullable Circle c = wire.read().object(Circle.class);
        assertNull(c);
    }

    @Test
    public void testLongString() {
        @NotNull Wire wire = createWire();
        @NotNull char[] chars = new char[64];
        for (int i = 0; i < Character.MAX_VALUE; i += chars.length) {
            for (int j = 0; j < chars.length; j++) {
                if (!Character.isValidCodePoint(i + j))
                    continue;
                chars[j] = (char) (i + j);
            }
            wire.clear();
            @NotNull String s = new String(chars);
            wire.writeDocument(false, w -> w.write(() -> "message").text(s));

            // System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));
            wire.readDocument(null, w -> w.read(() -> "message").text(s, Assert::assertEquals));
        }
    }

    @Test
    public void testArrays() {
        @NotNull Wire wire = createWire();

        @NotNull Object[] noObjects = {};
        wire.write("a").object(noObjects);

        // System.out.println(wire.asText());
        @Nullable Object[] object = wire.read()
                .object(Object[].class);
        assertEquals(0, object.length);

        @NotNull Object[] threeObjects = {"abc", "def", "ghi"};
        wire.write("b").object(threeObjects);
        // System.out.println(wire.asText());

        @Nullable Object[] object2 = wire.read()
                .object(Object[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    @Test
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
        w.usePadding(true);

        try (DocumentContext dc = w.writingDocument(false)) {
            dc.wire().writeEventName("hello1").typedMarshallable(new DTO("world1"));
            dc.wire().writeEventName("hello2").typedMarshallable(new DTO("world2"));
            dc.wire().writeEventName("hello3").typedMarshallable(new DTO("world3"));
        }

        try (DocumentContext dc = w.readingDocument()) {

            // System.out.println(Wires.fromSizePrefixedBlobs(dc));

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
        w.bytes().releaseLast();
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

    private static class DTO extends SelfDescribingMarshallable {

        String text;

        DTO(String text) {
            this.text = text;
        }
    }
}