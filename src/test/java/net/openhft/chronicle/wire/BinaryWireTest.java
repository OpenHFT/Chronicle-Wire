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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.internal.NoBytesStore;
import net.openhft.chronicle.bytes.internal.SingleMappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.ScopeConfined;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.VanillaReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
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
    @NotNull
    Bytes<?> bytes = new HexDumpBytes();

    // Constructor for initializing parameters of the test
    public BinaryWireTest(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        this.testId = testId;
        this.fixed = fixed;
        this.numericField = numericField;
        this.fieldLess = fieldLess;
        this.compressedSize = compressedSize;
    }

    // Provides the combinations of test parameters to be used in parameterized tests
    @Parameterized.Parameters(name = "fixed: {1}, numeric: {2}, fieldless: {3}, compressed: {4}")
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

    // Override to release resources and ensure no references are left
    @Override
    public void assertReferencesReleased() {
        bytes.releaseLast();
        super.assertReferencesReleased();
    }

    // Test case to verify the write operation of the Wire
    @Test
    public void testWrite() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();

        // Check the expected wire output against actual wire representation
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

        // Assert the text representation of the wire based on the fieldLess parameter
        assertEquals(fieldLess ? "" : "\"\": \"\": \"\": ", TextWire.asText(wire));
    }

    // Test case to verify the reading and writing of a String with special characters
    @Test
    public void readWriteString() {
        String utfCharacter = "ä";
        @NotNull Wire wire = createWire();
        wire.getValueOut()
                .writeString(utfCharacter);

        // Verify if the string read from the wire is the same as written
        assertEquals(utfCharacter, wire.getValueIn()
                .readString());
    }

    // Create a BinaryWire with pre-defined properties set during initialization
    @SuppressWarnings("deprecation")
    @NotNull
    private BinaryWire createWire() {
        bytes.clear();
        @NotNull BinaryWire wire = new BinaryWire(bytes, fixed, numericField, fieldLess, compressedSize, "lzw");
        wire.usePadding(true);
        return wire;
    }

    // Check the wire's hex string output against the expected strings provided as arguments
    private void checkWire(@NotNull Wire wire, String... expected) {
        if (expected[0].startsWith("["))
            System.out.println("\"\" +\n\"" + (wire.bytes().toHexString().replaceAll("\n", "\\\\n\" +\n\"") + "\",").replace(" +\n\"\",", ","));
        else
            assertEquals("id: " + testId,
                    expected[testId],
                    wire.bytes().toHexString());
    }

    // A variation of checkWire to compare the wire's debug string output
    private void checkWire2(@NotNull Wire wire, String... expected) {
        assertEquals("id: " + testId,
                expected[testId].replaceAll("٠+$", ""),
                wire.bytes().toDebugString(9999).replaceAll("٠+$", ""));
    }

    // Test case to verify writing fields to the wire and checking their representation
    @Test
    public void testWrite1() {
        @NotNull Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);

        // Check the expected wire output against actual wire representation
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

        // Check wire's textual representation
        checkAsText(wire,
                "field1: field2: field3: ",
                "\"1\": \"2\": \"3\": ",
                "");
    }

    // Validate the wire's text representation against expected values
    private void checkAsText(@NotNull Wire wire, String textFieldExcepted, String numberFieldExpected, String fieldLessExpected) {
        String text = TextWire.asText(wire);
        if (fieldLess)
            assertEquals(fieldLessExpected, text);
        else if (numericField)
            assertEquals(numberFieldExpected, text);
        else
            assertEquals(textFieldExcepted, text);
    }

    // Test writing data to a Wire
    @Test
    public void testWrite2() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write some strings to the wire
        wire.write(() -> "Hello");
        wire.write(() -> "World");

        // Define and write a long string to the wire
        @NotNull String name = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name);

        // Check the wire's byte representation against expected values
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

    // Test reading data from a Wire
    @Test
    public void testRead() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write some values to the wire
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");

        // Validate the wire's text representation against expected values
        checkAsText(wire, "\"\": field1: Test: ",
                "\"\": \"1\": \"2603186\": ",
                "");

        // Read values from the wire
        wire.read();
        wire.read();
        wire.read();

        // Ensure no bytes remain after reading
        assertEquals(0, bytes.readRemaining());

        // Confirm no errors occur when trying to read past the end
        wire.read();
    }

    // Testing a basic reading scenario
    @Test
    public void testRead1() {
        // Setup
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");

        // Checking the wire's current textual representation
        checkAsText(wire, "\"\": field1: Test: ",
                "\"\": \"1\": \"2603186\": ",
                "");

        // ok as blank matches anything
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        // not a match
        wire.read(BWKey.field1);

        // Ensuring all bytes were read
        assertEquals(0, bytes.readRemaining());

        // Safety check: additional read shouldn't cause problems
        wire.read();
    }

    // Testing reading into a StringBuilder
    @Test
    public void testRead2() {
        // Setup
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        @NotNull String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name1);

        // Reading into a StringBuilder (capturing data)
        @NotNull StringBuilder name = new StringBuilder();
        wire.read(name);
        assertEquals(0, name.length());  // First read captures nothing

        name.setLength(0);
        wire.read(name);  // Second read captures a field or a numeric value
        assertEquals(numericField ? "1" : fieldLess ? "" : BWKey.field1.name(), name.toString());

        name.setLength(0);
        wire.read(name);  // Third read captures a long field name or a numeric value
        assertEquals(numericField ? "-1019176629" : fieldLess ? "" : name1, name.toString());

        // Ensuring all bytes were read
        assertEquals(0, bytes.readRemaining());

        // Safety check: additional read shouldn't cause problems
        wire.read();
    }

    // Testing the writing and reading of 8-bit integers
    @Test
    public void int8() {
        // Setup
        @NotNull Wire wire = createWire();
        wire.write().int8((byte) 1);
        wire.write(BWKey.field1).int8((byte) 2);
        wire.write(() -> "Test").int8((byte) 3);

        // Checking the wire's current byte representation
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
        checkAsText123(wire, fixed ? "!byte " : ""); // Additional validation for textual representation

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int8(i, AtomicInteger::set);
            assertEquals(e, i.get());  // Validate that the read value matches the expected integer
        });

        // Ensuring all bytes were read
        assertEquals(0, bytes.readRemaining());

        // Safety check: additional read shouldn't cause problems
        wire.read();
    }

    // Checks the textual representation of the Wire with the default type.
    private void checkAsText123(@NotNull Wire wire) {
        checkAsText123(wire, "");
    }

    // Checks the textual representation of the Wire for a given type.
    private void checkAsText123(@NotNull Wire wire, String type) {
        checkAsText(wire, "\"\": " + type + "1\n" +
                        "field1: " + type + "2\n" +
                        "Test: " + type + "3\n",
                "\"\": " + type + "1\n" +
                        "\"1\": " + type + "2\n" +
                        "\"2603186\": " + type + "3\n",
                type + "1\n" +
                        type + "2\n" +
                        type + "3\n"
        );
    }

    // Checks the fixed textual representation of the Wire.
    private void checkAsText123Fixed(@NotNull Wire wire) {
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

    // Variation of the checkAsText method with different textual representation.
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

    // Test for writing and reading 16-bit integers to/from Wire.
    @Test
    public void int16() {
        // Initialize a Wire instance.
        @NotNull Wire wire = createWire();

        // Write 16-bit integers to Wire.
        wire.write().int16((short) 1);
        wire.write(BWKey.field1).int16((short) 2);
        wire.write(() -> "Test").int16((short) 3);

        // Validate the Wire content against different representations.
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

        // Read and validate integers from the Wire.
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int16(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Assert no remaining bytes to read.
        assertEquals(0, bytes.readRemaining());

        // Ensure it's safe to perform additional reads.
        wire.read();
    }

    // Test for writing and reading 8-bit unsigned integers to/from Wire.
    @Test
    public void uint8() {
        // Initialize a new Wire instance.
        @NotNull Wire wire = createWire();

        // Write 8-bit unsigned integers to the Wire.
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);
        wire.write(() -> "Test").uint8(3);

        // Validate the content of the Wire against multiple representations.
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
        // Check the textual representation of the Wire.
        checkAsText123(wire);

        // Read and validate the unsigned integers from the Wire.
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint8(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Assert that no more bytes remain to be read from the Wire.
        assertEquals(0, bytes.readRemaining());

        // Ensure it's safe to attempt additional reads from the Wire.
        wire.read();
    }

    // Test case to validate writing and reading of unsigned 16-bit integers using Wire
    @Test
    public void uint16() {
        // Create a wire instance for testing
        @NotNull Wire wire = createWire();

        // Write unsigned 16-bit integers to the wire
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);
        wire.write(() -> "Test").uint16(3);

        // Check the serialized format of the wire against expected values
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

        // Validate textual representation of the wire's content
        checkAsText123(wire);

        // Read the values from the wire and validate them
        // Ensure that blank matches any value
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint16(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Validate that no unread data remains in the byte buffer
        assertEquals(0, bytes.readRemaining());

        // Ensure that reading beyond available data doesn't result in errors
        wire.read();
    }

    // Test case to validate writing and reading of unsigned 32-bit integers using Wire
    @Test
    public void uint32() {
        // Create a wire instance for testing
        @NotNull Wire wire = createWire();

        // Write unsigned 32-bit integers to the wire
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);
        wire.write(() -> "Test").uint32(3);

        // Check the serialized format of the wire against expected values
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

        // Validate textual representation of the wire's content
        checkAsText123(wire);

        // Read the values from the wire and validate them
        // Ensure that blank matches any value
        @NotNull AtomicLong i = new AtomicLong();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint32(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        // Validate that no unread data remains in the byte buffer
        assertEquals(0, bytes.readRemaining());

        // Ensure that reading beyond available data doesn't result in errors
        wire.read();
    }

    // Test the writing and reading of 32-bit integers using the Wire API
    @Test
    public void int32() {
        // Create a new Wire instance and write int32 values
        @NotNull Wire wire = createWire();
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);
        wire.write(() -> "Test").int32(3);

        // Check the binary format of the written values
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

        // Read back the int32 values and verify their integrity
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int32(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Ensure no bytes remain to be read
        assertEquals(0, bytes.readRemaining());
        // Test to ensure over-reading is handled gracefully
        wire.read();
    }

    // Test the writing and reading of 64-bit integers using the Wire API
    @Test
    public void int64() {
        // Create a new Wire instance and write int64 values
        @NotNull Wire wire = createWire();
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write(() -> "Test").int64(3);

        // Check the binary format of the written values
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

        // Read back the int64 values and verify their integrity
        @NotNull AtomicLong i = new AtomicLong();
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        // Ensure no bytes remain to be read
        assertEquals(0, bytes.readRemaining());
        // Test to ensure over-reading is handled gracefully
        wire.read();
    }

    // Test for ensuring correct storage and retrieval of float64 values in Wire
    @Test
    public void testFloat64s() {
        // Create a Wire instance
        @NotNull Wire wire = createWire();

        // Iterate over a range of predefined float64 values
        for (double d : new double[]{
                2.358662e9,
                Double.POSITIVE_INFINITY, Double.NaN,
                3, 3 << 6, 3 << 7, 3 << 8, 3 << 14, 3 << 15, 1 + (3L << 29), 1 + (3L << 30), 1 + (3L << 31), 3L << 52, 3L << 53
        }) {
            wire.clear();

            // Write the current value to Wire under keys "p", "n", and "t"
            wire.write("p")
                    .float64(d)
                    .write("n").float64(-d)
                    .write("t").text("hi");

            // Ensure correct retrieval of written values
            assertEquals(d, wire.read("p").float64(), 0);
            assertEquals(-d, wire.read("n").float64(), 0);
            assertEquals("hi", wire.read("t").text());
        }
    }

    // Test for checking various float64 serialization scenarios in Wire
    @Test
    public void float64() {
        // Create a Wire instance
        @NotNull Wire wire = createWire();

        // Write float64 values to Wire
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);
        wire.write(() -> "Test").float64(3);

        // Check Wire contents against predefined representations
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

        // Depending on the binary state of Wire, execute different check functions
        if (wire.isBinary())
            if (fixed)
                checkAsText123Fixed(wire);
            else
                checkAsText123(wire);
        else
            checkAsText123_0(wire);

        // Write zero as a float64 value to Wire
        wire.write().float64(0);

        // Using a helper class to read float64 values from Wire
        class Floater {
            double f;

            // Setter for the floating-point value
            public void set(double d) {
                f = d;
            }
        }
        @NotNull Floater n = new Floater();

        // Read float64 values from Wire using IntStream and ensure correct retrieval
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().float64(n, Floater::set);
            assertEquals(e, n.f, 0.0);
        });

        // Ensure next float64 value read is 0
        assertEquals(0.0, wire.read().float64(), 0.0);

        // Ensure no remaining bytes in the underlying storage
        assertEquals(0, bytes.readRemaining());

        // Check that Wire can safely handle reading beyond available data
        wire.read();
    }

    @Test
    public void text() {
        // A long field name used for testing
        @NotNull String name = "Long field name which is more than 32 characters, Bye";

        // Create a wire instance for testing
        @NotNull Wire wire = createWire();
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        wire.write(() -> "Test").text(name);

        // Checking the wire's content with various expected outputs
        checkWire2(wire, "[pos: 0, rlim: 80, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloÆfield1åworldÄTest¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 80, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloÆfield1åworldÄTest¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 80, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloÆfield1åworldÄTest¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 75, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloº⒈åworldº²ñ\\u009E⒈¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 75, wlim: 8EiB, cap: 8EiB ] ǁÀåHelloº⒈åworldº²ñ\\u009E⒈¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 67, wlim: 8EiB, cap: 8EiB ] ǁåHelloåworld¸5" + name + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 67, wlim: 8EiB, cap: 8EiB ] ǁåHelloåworld¸5" + name + "‡٠٠٠٠٠٠٠٠");

        // Check the wire's content as text
        checkAsText(wire, "\"\": Hello\n" +
                        "field1: world\n" +
                        "Test: \"" + name + "\"\n",
                "\"\": Hello\n" +
                        "\"1\": world\n" +
                        "\"2603186\": \"" + name + "\"\n",
                "Hello\n" +
                        "world\n" +
                        "\"" + name + "\"\n");

        // Use a StringBuilder to read from the wire and verify the contents
        @NotNull StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name).forEach(e -> {
            wire.read().textTo(sb);
            assertEquals(e, sb.toString());
        });

        // Ensure no more bytes are left to read from the wire
        assertEquals(0, bytes.readRemaining());

        // Safeguard: Check if it's safe to read more from the wire even if there's nothing left
        wire.read();
    }

    @Test
    public void type() {
        // Ignore specific exception for the sake of this test
        ignoreException("Unable to copy object safely, message will not be repeated: net.openhft.chronicle.core.util.ClassNotFoundRuntimeException");

        // Create a wire instance for testing
        @NotNull Wire wire = createWire();

        // Write various types to the wire
        wire.write().typePrefix("MyType");
        wire.write(BWKey.field1).typePrefix("AlsoMyType");
        @NotNull String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write(() -> "Test").typePrefix(name1);

        // Checking the wire's content with various expected outputs
        checkWire2(wire, "[pos: 0, rlim: 158, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 158, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 158, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeÆfield1¶⒑AlsoMyTypeÄTest¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 153, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeº⒈¶⒑AlsoMyTypeº²ñ\\u009E⒈¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 153, wlim: 8EiB, cap: 8EiB ] ǁÀ¶⒍MyTypeº⒈¶⒑AlsoMyTypeº²ñ\\u009E⒈¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 145, wlim: 8EiB, cap: 8EiB ] ǁ¶⒍MyType¶⒑AlsoMyType¶{" + name1 + "‡٠٠٠٠٠٠٠٠",
                "[pos: 0, rlim: 145, wlim: 8EiB, cap: 8EiB ] ǁ¶⒍MyType¶⒑AlsoMyType¶{" + name1 + "‡٠٠٠٠٠٠٠٠");

        // Check the wire's content as text
        checkAsText(wire, "\"\": !MyType field1: !AlsoMyType Test: !" + name1 + " ",
                "\"\": !MyType \"1\": !AlsoMyType \"2603186\": !" + name1 + " ",
                "!MyType !AlsoMyType !" + name1 + " ");

        // Reading from the wire and verifying the type prefixes
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().typePrefix(e, (expected, actual) -> Assert.assertEquals(expected, actual.toString()));
        });

        // Ensure no more bytes are left to read from the wire
        assertEquals(0, bytes.readRemaining());

        // Safeguard: Check if it's safe to read more from the wire even if there's nothing left
        wire.read();
    }

    // Testing the boolean write and read functionality of the wire
    @Test
    public void testBool() {
        @NotNull Wire wire = createWire(); // Create a wire instance

        // Write boolean values (false, true, and null) to the wire
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);
        // System.out.println(wire);

        // Read the boolean values from the wire and assert they match the written values
        wire.read().bool(false, Assert::assertEquals)
                .read().bool(true, Assert::assertEquals)
                .read().bool(null, Assert::assertEquals);
    }

    // Testing the float32 (i.e., single precision float) write and read functionality of the wire
    @Test
    public void testFloat32() {
        @NotNull Wire wire = createWire(); // Create a wire instance

        // Write various float32 values to the wire
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY)
                .write().float32(Float.NEGATIVE_INFINITY)
                .write().float32(123456.0f);

        // Read the float32 values from the wire and assert they match the written values
        wire.read().float32(this, (o, t) -> assertEquals(0.0F, t, 0.0F))
                .read().float32(this, (o, t) -> assertTrue(Float.isNaN(t)))
                .read().float32(this, (o, t) -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(Float.NEGATIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(123456.0f, t, 0.0F));
    }

    // Testing the LocalTime write and read functionality of the wire
    @Test
    public void testTime() {
        @NotNull Wire wire = createWire(); // Create a wire instance
        LocalTime now = LocalTime.of(12, 54, 4, 612 * 1000000); // Create a LocalTime instance

        // Write various LocalTime values to the wire
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);

        // An assertion for byte representation, it seems to be related to some internal functionality
        // (the details of which would depend on the context in which this test is used)
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

        // Read the LocalTime values from the wire and assert they match the written values
        wire.read().time(now, Assert::assertEquals)
                .read().time(LocalTime.MAX, Assert::assertEquals)
                .read().time(LocalTime.MIN, Assert::assertEquals);
    }

    // Testing the ZonedDateTime write and read functionality of the wire
    @Test
    public void testZonedDateTime() {
        @NotNull Wire wire = createWire(); // Create a wire instance
        ZonedDateTime now = ZonedDateTime.now(); // Get the current ZonedDateTime
        final ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        final ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());

        // Write various ZonedDateTime values to the wire
        wire.write().zonedDateTime(now)
                .write().zonedDateTime(max)
                .write().zonedDateTime(min);

        // Read the ZonedDateTime values from the wire and assert they match the written values
        wire.read().zonedDateTime(now, Assert::assertEquals)
                .read().zonedDateTime(max, Assert::assertEquals)
                .read().zonedDateTime(min, Assert::assertEquals);

        // Write the same ZonedDateTime values but this time as generic objects
        wire.write().object(now)
                .write().object(max)
                .write().object(min);

        // Read the ZonedDateTime values (stored as Objects) from the wire and assert they match the written values
        wire.read().object(Object.class, now, Assert::assertEquals)
                .read().object(Object.class, max, Assert::assertEquals)
                .read().object(Object.class, min, Assert::assertEquals);
    }

    // Testing the LocalDate write and read functionality of the wire
    @Test
    public void testDate() {
        @NotNull Wire wire = createWire(); // Create a wire instance
        LocalDate now = LocalDate.now();   // Get the current date

        // Write various LocalDate values to the wire
        wire.write().date(now)
                .write().date(LocalDate.MAX)
                .write().date(LocalDate.MIN);

        // Read the LocalDate values from the wire and assert they match the written values
        wire.read().date(now, Assert::assertEquals)
                .read().date(LocalDate.MAX, Assert::assertEquals)
                .read().date(LocalDate.MIN, Assert::assertEquals);
    }

    // Testing the UUID write and read functionality of the wire
    @Test
    public void testUuid() {
        @NotNull Wire wire = createWire(); // Create a wire instance
        UUID uuid = UUID.randomUUID();     // Generate a random UUID

        // Write various UUID values to the wire
        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));

        // Read the UUID values from the wire and assert they match the written values
        wire.read().uuid(uuid, Assert::assertEquals)
                .read().uuid(new UUID(0, 0), Assert::assertEquals)
                .read().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), Assert::assertEquals);
    }

    // Testing the byte array write and read functionality of the wire
    @Test
    public void testBytes() {
        @NotNull Wire wire = createWire();  // Create a wire instance
        @NotNull byte[] allBytes = new byte[256];  // Initialize a byte array of length 256

        // Fill the byte array with byte values from 0 to 255
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;

        // Write various byte arrays to the wire
        wire.write().bytes(NoBytesStore.NO_BYTES)
                .write().bytes(Bytes.wrapForRead("Hello".getBytes(ISO_8859_1)))
                .write().bytes(Bytes.wrapForRead("quotable, text".getBytes(ISO_8859_1)))
                .write()
                .bytes(allBytes);

        // System.out.println(bytes.toDebugString()); // Debugging line commented out

        // Read the byte arrays from the wire and assert they match the written values
        @SuppressWarnings("rawtypes")
        @NotNull NativeBytes allBytes2 = nativeBytes();
        wire.read().bytes(b -> assertEquals(0, b.readRemaining()))
                .read().bytes(b -> assertEquals("Hello", b.toString()))
                .read().bytes(b -> assertEquals("quotable, text", b.toString()))
                .read()
                .bytes(allBytes2);

        // Assert that the byte array read matches the initial byte array
        assertEquals(Bytes.wrapForRead(allBytes), allBytes2);

        allBytes2.releaseLast();  // Release the last referenced resource
    }

    @Test
    public void testWriteMarshallable() {
        // Test the write marshallable functionality for BinaryWire

        // Uncomment the line below to set the BinaryWire.SPEC to 18.
        // BinaryWire.SPEC = 18;

        // Create a new wire instance.
        @NotNull Wire wire = createWire();

        // Initialize a MyTypesCustom instance (mtA) with specific values.
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;
        mtA.text.append("Hello World");

        // Write the above initialized instance (mtA) to the wire.
        wire.write(() -> "A").marshallable(mtA);

        // Initialize another MyTypesCustom instance (mtB) with different values.
        @NotNull MyTypesCustom mtB = new MyTypesCustom();
        mtB.flag = false;
        mtB.d = 123.4567;
        mtB.i = -123457890;
        mtB.s = (short) 1234;
        mtB.text.append("Bye now");

        // Write the second initialized instance (mtB) to the wire.
        wire.write(() -> "B").marshallable(mtB);

        // Uncomment the line below to print the wire bytes in debug format.
        // System.out.println(wire.bytes().toDebugString(400));

        // Check the wire content against expected values.
        checkWire(wire,
                // Expected representation 1
                "" +
                        "c1 41                                           # A:\n" +
                        "82 3f 00 00 00                                  # MyTypesCustom\n" +
                        "c6 42 5f 46 4c 41 47                            # B_FLAG:\n" +
                        "b1                                              # true\n" +
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
                        "c6 42 5f 46 4c 41 47                            # B_FLAG:\n" +
                        "b0                                              # false\n" +
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
                // Expected representation 2
                "" +
                        "c1 41                                           # A:\n" +
                        "82 3f 00 00 00                                  # MyTypesCustom\n" +
                        "c6 42 5f 46 4c 41 47                            # B_FLAG:\n" +
                        "b1                                              # true\n" +
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
                        "c6 42 5f 46 4c 41 47                            # B_FLAG:\n" +
                        "b0                                              # false\n" +
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
                // Expected representation 3
                "" +
                        "c1 41                                           # A:\n" +
                        "82 4b 00 00 00                                  # MyTypesCustom\n" +
                        "c6 42 5f 46 4c 41 47                            # B_FLAG:\n" +
                        "b1                                              # true\n" +
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
                        "c6 42 5f 46 4c 41 47                            # B_FLAG:\n" +
                        "b0                                              # false\n" +
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
                // Expected representation 4
                "" +
                        "ba 41                                           # 65\n" +
                        "82 27 00 00 00                                  # MyTypesCustom\n" +
                        "ba 00                                           # 0\n" +
                        "b1                                              # true\n" +
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
                        "ba 00                                           # 0\n" +
                        "b0                                              # false\n" +
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
                // Expected representation 5
                "" +
                        "ba 41                                           # 65\n" +
                        "82 33 00 00 00                                  # MyTypesCustom\n" +
                        "ba 00                                           # 0\n" +
                        "b1                                              # true\n" +
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
                        "ba 00                                           # 0\n" +
                        "b0                                              # false\n" +
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
                // Expected representation 6
                "" +
                        "82 1b 00 00 00                                  # MyTypesCustom\n" +
                        "b1                                              # true\n" +
                        "a5 39 30                                        # 12345\n" +
                        "94 80 ad 4b                                     # 1234560/1e4\n" +
                        "a1 00                                           # 0\n" +
                        "a6 43 9e 43 ff                                  # -12345789\n" +
                        "eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World\n" +
                        "82 17 00 00 00                                  # MyTypesCustom\n" +
                        "b0                                              # false\n" +
                        "a5 d2 04                                        # 1234\n" +
                        "94 87 ad 4b                                     # 1234567/1e4\n" +
                        "a1 00                                           # 0\n" +
                        "a6 9e 2e a4 f8                                  # -123457890\n" +
                        "e7 42 79 65 20 6e 6f 77                         # Bye now\n",
                // Expected representation 7
                "" +
                        "82 27 00 00 00                                  # MyTypesCustom\n" +
                        "b1                                              # true\n" +
                        "a5 39 30                                        # 12345\n" +
                        "91 77 be 9f 1a 2f dd 5e 40                      # 123.456\n" +
                        "a7 00 00 00 00 00 00 00 00                      # 0\n" +
                        "a6 43 9e 43 ff                                  # -12345789\n" +
                        "eb 48 65 6c 6c 6f 20 57 6f 72 6c 64             # Hello World\n" +
                        "82 23 00 00 00                                  # MyTypesCustom\n" +
                        "b0                                              # false\n" +
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
        // Creating a wire instance and ensuring it's not null
        @NotNull Wire wire = createWire();

        // Writing null objects to the wire
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);

        // Reading back the objects from the wire and asserting their nullity
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
        // Creating a wire instance and a character array
        @NotNull Wire wire = createWire();
        @NotNull char[] chars = new char[64];

        // Iterating through the character values and filling the char array
        for (int i = 0; i < Character.MAX_VALUE; i += chars.length) {
            for (int j = 0; j < chars.length; j++) {
                if (!Character.isValidCodePoint(i + j))
                    continue;
                chars[j] = (char) (i + j);
            }

            // Clear the wire, write the characters to the wire and then read and assert
            wire.clear();
            @NotNull String s = new String(chars);
            wire.writeDocument(false, w -> w.write(() -> "message").text(s));

            // System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));
            wire.readDocument(null, w -> w.read(() -> "message").text(s, Assert::assertEquals));
        }
    }

    @Test
    public void testArrays() {
        // Creating a wire instance
        @NotNull Wire wire = createWire();

        // Writing an empty array to the wire
        @NotNull Object[] noObjects = {};
        wire.write("a").object(noObjects);

        // System.out.println(wire.asText());
        // Reading the array back from the wire and asserting its length
        @Nullable Object[] object = wire.read()
                .object(Object[].class);
        assertEquals(0, object.length);

        // Writing an array of three strings to the wire
        @NotNull Object[] threeObjects = {"abc", "def", "ghi"};
        wire.write("b").object(threeObjects);
        // System.out.println(wire.asText());

        // Reading the string array back from the wire and asserting its contents
        @Nullable Object[] object2 = wire.read()
                .object(Object[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    @Test
    public void testArrays2() {
        // Creating a wire instance
        @NotNull Wire wire = createWire();

        // Writing an empty array to the wire
        @NotNull Object[] a1 = new Object[0];
        wire.write("empty").object(a1);

        // Writing an array with one long element to the wire
        @NotNull Object[] a2 = {1L};
        wire.write("one").object(a2);

        // Writing an array with mixed type elements to the wire
        @NotNull Object[] a3 = {"Hello", 123, 10.1};
        wire.write("three").object(Object[].class, a3);

        // Reading arrays back from the wire and asserting their contents
        @Nullable Object o1 = wire.read().object(Object[].class);
        assertArrayEquals(a1, (Object[]) o1);
        @Nullable Object o2 = wire.read().object(Object[].class);
        assertArrayEquals(a2, (Object[]) o2);
        @Nullable Object o3 = wire.read().object(Object[].class);
        assertArrayEquals(a3, (Object[]) o3);
    }

    @Test
    public void testUsingEvents() throws Exception {
        // Creating a wire instance with binary format
        final Wire w = WireType.BINARY.apply(Bytes.elasticByteBuffer());
        w.usePadding(true);

        // Writing three events with DTOs to the wire
        try (DocumentContext dc = w.writingDocument(false)) {
            dc.wire().writeEventName("hello1").typedMarshallable(new DTO("world1"));
            dc.wire().writeEventName("hello2").typedMarshallable(new DTO("world2"));
            dc.wire().writeEventName("hello3").typedMarshallable(new DTO("world3"));
        }

        // Reading events back from the wire and asserting their correctness
        try (DocumentContext dc = w.readingDocument()) {

            // System.out.println(Wires.fromSizePrefixedBlobs(dc));

            StringBuilder sb = new StringBuilder();

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
        // Creating a wire instance and a sorted set of strings
        @NotNull Wire wire = createWire();
        @NotNull SortedSet<String> set = new TreeSet<>();
        set.add("one");
        set.add("two");
        set.add("three");

        // Writing the sorted set to the wire
        wire.write("a").object(set);

        // Reading back from the wire and asserting the type and content
        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedSet);
        assertEquals(set, o);
    }

    @Test
    public void testSortedMap() {
        // Creating a wire instance and a sorted map
        @NotNull Wire wire = createWire();
        @NotNull SortedMap<String, Long> set = new TreeMap<>();
        set.put("one", 1L);
        set.put("two", 2L);
        set.put("three", 3L);

        // Writing the sorted map to the wire
        wire.write("a").object(set);

        // Reading back from the wire and asserting the type and content
        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedMap);
        assertEquals(set, o);
    }

    @Test
    public void testSkipPadding() {
        @NotNull Wire wire = createWire();

        // Testing skipping padding for increasing padding values
        for (int i = 1; i <= 128; i *= 2) {
            wire.addPadding(i);
            wire.getValueIn().skipValue();
            assertEquals(0, wire.bytes().readRemaining());
            wire.clear();
        }

        // Testing marshallable values with increasing padding
        for (int i = 1; i <= 128; i *= 2) {
            wire.addPadding(i);
            int finalI = i;
            wire.getValueOut().marshallable(w -> w.write("i").int32(finalI));
            wire.getValueIn().skipValue();
            assertEquals(0, wire.bytes().readRemaining());
            wire.clear();
        }
    }

    @SuppressWarnings("try")
    @Test
    public void readsComment() {
        StringBuilder sb = new StringBuilder();
        Wire wire = createWire();

        // Writing a document with comments and a DTO object
        try (DocumentContext dc = wire.writingDocument()) {
            wire.writeComment("one\n");
            wire.writeEventId("dto", 1);
            wire.writeComment("two\n");
            wire.getValueOut().object(new DTO("text"));
            wire.writeComment("three\n");

            // Setting a listener to accumulate comments into StringBuilder
            wire.commentListener(sb::append);
        }

        // Reading using a method reader and processing the DTO
        final MethodReader reader = wire.methodReader((IDTO) dto -> sb.append("dto: " + dto + "\n"));
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("one\n" +
                "two\n" +
                "dto: !net.openhft.chronicle.wire.BinaryWireTest$DTO {\n" +
                "  text: text\n" +
                "}\n" +
                "\n" +
                "three\n", sb.toString());
    }

    @Test
    public void writeEndOfWireDoesNotUpdateModifiedTimeOnNoOpWhenUnderlyingBytesIsFile() throws IOException {
        // Create a temporary file for the test
        final File tempFile = IOTools.createTempFile("test-lastModified-endOfWire");
        final AtomicLong endOfWirePosition = new AtomicLong();

        // Create a wire from the temporary file and write a test entry
        createWireFromFileAnd(tempFile, wire -> {
            wire.write("testing-testing").int8(123);
            endOfWirePosition.set(wire.bytes().writePosition());
            assertTrue(wire.writeEndOfWire(100, TimeUnit.MILLISECONDS, endOfWirePosition.get()));
        });

        // this will wait until any pending resources have been closed
        BackgroundResourceReleaser.releasePendingResources();

        long lastModified = tempFile.lastModified();
        Jvm.pause(10);

        // Create a wire from the temporary file and attempt a no-op
        createWireFromFileAnd(tempFile, wire -> {
            // This should be a no-op and not result in an update to lastModifiedTime
            assertFalse(wire.writeEndOfWire(100, TimeUnit.MILLISECONDS, endOfWirePosition.get()));
        });

        long lastModified2 = tempFile.lastModified();
        if (OS.isMacOSX() && lastModified2 - lastModified == 1)
            return;

        assertEquals(lastModified, lastModified2);
    }

    private void createWireFromFileAnd(File file, Consumer<@ScopeConfined Wire> wireConsumer) throws IOException {
        VanillaReferenceOwner owner = new VanillaReferenceOwner("test");
        try (MappedFile mappedFile = SingleMappedFile.mappedFile(file, 10_240)) {
            final Bytes<?> bytes = mappedFile.acquireBytesForWrite(owner, 0);
            Wire wire = WireType.BINARY.apply(bytes);
            wireConsumer.accept(wire);
            @SuppressWarnings("unchecked")
            MappedBytesStore mappedBytesStore = (MappedBytesStore) bytes.bytesStore();
            mappedBytesStore.syncUpTo(8192);
            bytes.releaseLast(owner);
        }
    }

    // Enum representing potential keys for wire entries
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

    // Interface to demonstrate a DTO with a single method
    interface IDTO {
        @MethodId(1)
        void dto(DTO dto);
    }

    // A basic DTO class extending the self-describing marshallable class
    static class DTO extends SelfDescribingMarshallable {

        String text;

        DTO(String text) {
            this.text = text;
        }
    }

    // A simple class representing a Circle
    class Circle implements Marshallable {
    }
}
