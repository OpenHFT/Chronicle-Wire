/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.internal.SingleMappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.ScopeConfined;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.VanillaReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"deprecation", "removal"})
class BinaryWireTest extends WireTestCommon {

    private int testId;
    private boolean fixed;
    private boolean numericField;
    private boolean fieldLess;
    private int compressedSize;
    @NotNull
    private final Bytes<?> bytes = new HexDumpBytes();
    private static final String[] INT8_EXPECTED = {
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a4 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a4 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a4 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a4 01                                           # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a4 02                                           # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a4 03                                           # 3\n",
            "a1 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "a1 03                                           # 3\n",
            "a4 01                                           # 1\n" +
                    "a4 02                                           # 2\n" +
                    "a4 03                                           # 3\n"
    };
    private static final String[] INT16_EXPECTED = {
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a5 01 00                                        # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a5 02 00                                        # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a5 03 00                                        # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a5 01 00                                        # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a5 02 00                                        # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a5 03 00                                        # 3\n",
            "a1 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "a1 03                                           # 3\n",
            "a5 01 00                                        # 1\n" +
                    "a5 02 00                                        # 2\n" +
                    "a5 03 00                                        # 3\n"
    };
    private static final String[] UINT8_EXPECTED = {
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a1 03                                           # 3\n",
            "a1 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "a1 03                                           # 3\n",
            "a1 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "a1 03                                           # 3\n"
    };
    private static final String[] UINT16_EXPECTED = {
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a2 01 00                                        # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a2 02 00                                        # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a2 03 00                                        # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a2 01 00                                        # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a2 02 00                                        # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a2 03 00                                        # 3\n",
            "a1 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "a1 03                                           # 3\n",
            "a2 01 00                                        # 1\n" +
                    "a2 02 00                                        # 2\n" +
                    "a2 03 00                                        # 3\n"
    };
    private static final String[] UINT32_EXPECTED = {
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a3 01 00 00 00                                  # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a3 02 00 00 00                                  # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a3 03 00 00 00                                  # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a3 01 00 00 00                                  # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a3 02 00 00 00                                  # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a3 03 00 00 00                                  # 3\n",
            "a1 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "a1 03                                           # 3\n",
            "a3 01 00 00 00                                  # 1\n" +
                    "a3 02 00 00 00                                  # 2\n" +
                    "a3 03 00 00 00                                  # 3\n"
    };
    private static final String[] INT32_EXPECTED = {
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a1 02                                           # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a6 01 00 00 00                                  # 1\n" +
                    "c6 66 69 65 6c 64 31                            # field1:\n" +
                    "a6 02 00 00 00                                  # 2\n" +
                    "c4 54 65 73 74                                  # Test:\n" +
                    "a6 03 00 00 00                                  # 3\n",
            "c0                                              # :\n" +
                    "a1 01                                           # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a1 03                                           # 3\n",
            "c0                                              # :\n" +
                    "a6 01 00 00 00                                  # 1\n" +
                    "ba 01                                           # 1\n" +
                    "a6 02 00 00 00                                  # 2\n" +
                    "ba b2 f1 9e 01                                  # 2603186\n" +
                    "a6 03 00 00 00                                  # 3\n",
            "a1 01                                           # 1\n" +
                    "a1 02                                           # 2\n" +
                    "a1 03                                           # 3\n",
            "a6 01 00 00 00                                  # 1\n" +
                    "a6 02 00 00 00                                  # 2\n" +
                    "a6 03 00 00 00                                  # 3\n"
    };

    // Constructor for initializing parameters of the test
    void initBinaryWireTest(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        this.testId = testId;
        this.fixed = fixed;
        this.numericField = numericField;
        this.fieldLess = fieldLess;
        this.compressedSize = compressedSize;
    }

    // Provides the combinations of test parameters to be used in parameterized tests
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
    void assertReferencesReleased() {
        bytes.releaseLast();
        super.assertReferencesReleased();
    }

    // Test case to verify the write operation of the Wire
    @DisplayName("Binary wire writes empty fields for each entry")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Write fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testWrite(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();

        // Check the expected wire output against actual wire representation
        checkWire(wire, "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "c0                                              # :\n" +
                        "c0                                              # :\n" +
                        "c0                                              # :\n",
                "",
                "");

        // Assert the text representation of the wire based on the fieldLess parameter
        assertEquals(fieldLess ? "" : "\"\": \"\": \"\": ", TextWire.asText(wire), "text representation should match fieldless mode configuration");
    }

    // Test case to verify the reading and writing of a String with special characters
    @DisplayName("Binary wire reads and writes utf-8 string values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire read Write String fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void readWriteString(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        String utfCharacter = "ä";
        @NotNull Wire wire = createWire();
        wire.getValueOut()
                .writeString(utfCharacter);

        // Verify if the string read from the wire is the same as written
        assertEquals(utfCharacter, wire.getValueIn()
                .readString(), "binary wire should correctly serialize and deserialize utf-8 characters");
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
            assertEquals(expected[testId],
                    wire.bytes().toHexString(),
                    "wire hex output should match expected for testId=" + testId);
    }

    // A variation of checkWire to compare the wire's debug string output
    private void checkWire2(@NotNull Wire wire, String... expected) {
        assertEquals(expected[testId].replaceAll("٠+$", ""),
                wire.bytes().toDebugString(9999).replaceAll("٠+$", ""),
                "wire debug output should match expected for testId=" + testId);
    }

    // Test case to verify writing fields to the wire and checking their representation
    @DisplayName("Binary wire writes field keys with expected encoding")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Write 1 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testWrite1(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);

        // Check the expected wire output against actual wire representation
        checkWire(wire, "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "c6 66 69 65 6c 64 32                            # field2:\n" +
                        "c6 66 69 65 6c 64 33                            # field3:\n",
                "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "c6 66 69 65 6c 64 32                            # field2:\n" +
                        "c6 66 69 65 6c 64 33                            # field3:\n",
                "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "c6 66 69 65 6c 64 32                            # field2:\n" +
                        "c6 66 69 65 6c 64 33                            # field3:\n",
                "ba 01                                           # 1\n" +
                        "ba 02                                           # 2\n" +
                        "ba 03                                           # 3\n",
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
        if (fieldLess || numericField)
            return;
        assertEquals(textFieldExcepted, text, "text representation should match expected format for binary wire");
    }

    // Test writing data to a Wire
    @DisplayName("Binary wire writes string field names and values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Write 2 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testWrite2(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write some strings to the wire
        wire.write(() -> "Hello");
        wire.write(() -> "World");

        // Define and write a long string to the wire
        @NotNull String name = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name);

        // Check the wire's byte representation against expected values
        checkWire(wire, "c5 48 65 6c 6c 6f                               # Hello:\n" +
                        "c5 57 6f 72 6c 64                               # World:\n" +
                        "b7 35 4c 6f 6e 67 20 66 69 65 6c 64 20 6e 61 6d # Long field name which is more than 32 characters, Bye:\n" +
                        "65 20 77 68 69 63 68 20 69 73 20 6d 6f 72 65 20\n" +
                        "74 68 61 6e 20 33 32 20 63 68 61 72 61 63 74 65\n" +
                        "72 73 2c 20 42 79 65\n",
                "c5 48 65 6c 6c 6f                               # Hello:\n" +
                        "c5 57 6f 72 6c 64                               # World:\n" +
                        "b7 35 4c 6f 6e 67 20 66 69 65 6c 64 20 6e 61 6d # Long field name which is more than 32 characters, Bye:\n" +
                        "65 20 77 68 69 63 68 20 69 73 20 6d 6f 72 65 20\n" +
                        "74 68 61 6e 20 33 32 20 63 68 61 72 61 63 74 65\n" +
                        "72 73 2c 20 42 79 65\n",
                "c5 48 65 6c 6c 6f                               # Hello:\n" +
                        "c5 57 6f 72 6c 64                               # World:\n" +
                        "b7 35 4c 6f 6e 67 20 66 69 65 6c 64 20 6e 61 6d # Long field name which is more than 32 characters, Bye:\n" +
                        "65 20 77 68 69 63 68 20 69 73 20 6d 6f 72 65 20\n" +
                        "74 68 61 6e 20 33 32 20 63 68 61 72 61 63 74 65\n" +
                        "72 73 2c 20 42 79 65\n",
                "ba b2 d1 98 21                                  # 69609650\n" +
                        "ba f2 d6 f8 27                                  # 83766130\n" +
                        "ba b4 cd fd e5 83 00                            # -1019176629\n",
                "ba b2 d1 98 21                                  # 69609650\n" +
                        "ba f2 d6 f8 27                                  # 83766130\n" +
                        "ba b4 cd fd e5 83 00                            # -1019176629\n",
                "",
                "");
    }

    // Test reading data from a Wire
    @DisplayName("Binary wire reads standard fields and values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Read fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testRead(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write some values to the wire
        WireReadTestSupport.writeStandardFields(wire);

        // Validate the wire's text representation against expected values
        checkAsText(wire, "\"\": field1: Test: ",
                "\"\": \"1\": \"2603186\": ",
                "");

        // Read values from the wire
        WireReadTestSupport.exerciseRead(wire, 0);
    }

    // Testing a basic reading scenario
    @DisplayName("Binary wire reads placeholders and named fields")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Read 1 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testRead1(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
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
        WireReadTestSupport.exerciseReadWithKey(wire, 0);
    }

    // Testing reading into a StringBuilder
    @DisplayName("Binary wire reads field names into StringBuilder")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Read 2 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testRead2(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Setup
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        @NotNull String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name1);

        @NotNull StringBuilder name = new StringBuilder();
        wire.read(name);
        assertEquals(0, name.length(), "first read of name should be empty placeholder");

        name.setLength(0);
        wire.read(name);
        String expectedSecond = numericField ? "1" : fieldLess ? "" : BWKey.field1.name();
        assertEquals(expectedSecond, name.toString(),
                "second read of name should match field1, numeric, or empty mode");

        name.setLength(0);
        wire.read(name);
        String expectedThird = numericField ? "-1019176629" : fieldLess ? "" : name1;
        assertEquals(expectedThird, name.toString(),
                "third read of name should match long field name, numeric, or empty mode");

        assertEquals(0, wire.bytes().readRemaining(), "all bytes should be consumed after three name reads");
        wire.read();

        // Safety check: additional read shouldn't cause problems
        wire.read();
    }

    // Testing the writing and reading of 8-bit integers
    @DisplayName("Binary wire round-trips int8 triplet values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire int 8 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void int8(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        assertSmallIntTriplet(WireSmallIntTestSupport::writeInt8Triplet,
                WireSmallIntTestSupport::readInt8Triplet,
                INT8_EXPECTED,
                wire -> checkAsText123(wire, fixed ? "!byte " : ""));
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

    private void assertSmallIntTriplet(Consumer<Wire> writer, Consumer<Wire> reader, String[] expectedHex, Consumer<Wire> textChecker) {
        @NotNull Wire wire = createWire();

        writer.accept(wire);
        if (testId <= 2) {
            checkWire(wire, expectedHex);
        }
        textChecker.accept(wire);
        reader.accept(wire);

        assertEquals(0, bytes.readRemaining(), "all bytes should be consumed after reading int triplet");
        wire.read();
    }

    // Test for writing and reading 16-bit integers to/from Wire.
    @DisplayName("Binary wire round-trips int16 triplet values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire int 16 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void int16(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        assertSmallIntTriplet(WireSmallIntTestSupport::writeInt16Triplet,
                WireSmallIntTestSupport::readInt16Triplet,
                INT16_EXPECTED,
                wire -> checkAsText123(wire, fixed ? "!short " : ""));
    }

    // Test for writing and reading 8-bit unsigned integers to/from Wire.
    @DisplayName("Binary wire round-trips uint8 triplet values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire uint 8 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void uint8(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        assertSmallIntTriplet(WireSmallIntTestSupport::writeUint8Triplet,
                WireSmallIntTestSupport::readUint8Triplet,
                UINT8_EXPECTED,
                this::checkAsText123);
    }

    // Test case to validate writing and reading of unsigned 16-bit integers using Wire
    @DisplayName("Binary wire round-trips uint16 triplet values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire uint 16 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void uint16(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        assertSmallIntTriplet(WireSmallIntTestSupport::writeUint16Triplet,
                WireSmallIntTestSupport::readUint16Triplet,
                UINT16_EXPECTED,
                this::checkAsText123);
    }

    // Test case to validate writing and reading of unsigned 32-bit integers using Wire
    @DisplayName("Binary wire round-trips uint32 triplet values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire uint 32 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void uint32(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        assertSmallIntTriplet(WireSmallIntTestSupport::writeUint32Triplet,
                WireSmallIntTestSupport::readUint32Triplet,
                UINT32_EXPECTED,
                this::checkAsText123);
    }

    // Test the writing and reading of 32-bit integers using the Wire API
    @DisplayName("Binary wire round-trips int32 triplet values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire int 32 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void int32(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        assertSmallIntTriplet(WireSmallIntTestSupport::writeInt32Triplet,
                WireSmallIntTestSupport::readInt32Triplet,
                INT32_EXPECTED,
                this::checkAsText123);
    }

    // Test the writing and reading of 64-bit integers using the Wire API
    @DisplayName("Binary wire round-trips int64 fields and values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire int 64 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void int64(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Create a new Wire instance and write int64 values
        @NotNull Wire wire = createWire();
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write(() -> "Test").int64(3);

        // Check the binary format of the written values
        checkWire(wire, "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "c0                                              # :\n" +
                        "a7 01 00 00 00 00 00 00 00                      # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a7 02 00 00 00 00 00 00 00                      # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a7 03 00 00 00 00 00 00 00                      # 3\n",
                "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "c0                                              # :\n" +
                        "a7 01 00 00 00 00 00 00 00                      # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a7 02 00 00 00 00 00 00 00                      # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a7 03 00 00 00 00 00 00 00                      # 3\n",
                "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
                "a7 01 00 00 00 00 00 00 00                      # 1\n" +
                        "a7 02 00 00 00 00 00 00 00                      # 2\n" +
                        "a7 03 00 00 00 00 00 00 00                      # 3\n");
        checkAsText123(wire, "");

        // Read back the int64 values and verify their integrity
        @NotNull AtomicLong i = new AtomicLong();
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i, AtomicLong::set);
            assertEquals(e, i.get(), "int64 value should deserialize correctly from binary wire");
        });

        // Ensure no bytes remain to be read
        assertEquals(0, bytes.readRemaining(), "all bytes should be consumed after reading int64 values");
        // Test to ensure over-reading is handled gracefully
        wire.read();
    }

    // Test for ensuring correct storage and retrieval of float64 values in Wire
    @DisplayName("Binary wire round-trips float64 values across ranges")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Float 64 s fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testFloat64s(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
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
            assertEquals(d, wire.read("p").float64(), 0, "positive float64 value should round-trip for d=" + d);
            assertEquals(-d, wire.read("n").float64(), 0, "negative float64 value should round-trip for d=" + d);
            assertEquals("hi", wire.read("t").text(), "text value should be preserved for d=" + d);
        }
    }

    // Test for checking various float64 serialization scenarios in Wire
    @DisplayName("Binary wire round-trips float64 fields with text")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire float 64 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void float64(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Create a Wire instance
        @NotNull Wire wire = createWire();

        // Write float64 values to Wire
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);
        wire.write(() -> "Test").float64(3);

        // Check Wire contents against predefined representations
        checkWire(wire, "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "a1 02                                           # 2\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "a1 03                                           # 3\n",
                "c0                                              # :\n" +
                        "91 00 00 00 00 00 00 f0 3f                      # 1.0\n" +
                        "c6 66 69 65 6c 64 31                            # field1:\n" +
                        "91 00 00 00 00 00 00 00 40                      # 2.0\n" +
                        "c4 54 65 73 74                                  # Test:\n" +
                        "91 00 00 00 00 00 00 08 40                      # 3.0\n",
                "c0                                              # :\n" +
                        "a1 01                                           # 1\n" +
                        "ba 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "a1 03                                           # 3\n",
                "c0                                              # :\n" +
                        "91 00 00 00 00 00 00 f0 3f                      # 1.0\n" +
                        "ba 01                                           # 1\n" +
                        "91 00 00 00 00 00 00 00 40                      # 2.0\n" +
                        "ba b2 f1 9e 01                                  # 2603186\n" +
                        "91 00 00 00 00 00 00 08 40                      # 3.0\n",
                "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n",
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
        }
        @NotNull Floater n = new Floater();

        // Read float64 values from Wire using IntStream and ensure correct retrieval
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().float64(n, (target, value) -> target.f = value);
            assertEquals(e, n.f, 0.0, "float64 value should deserialize correctly with consumer");
        });

        // Ensure next float64 value read is 0
        assertEquals(0.0, wire.read().float64(), 0.0, "zero float64 value should deserialize correctly from binary wire");

        // Ensure no remaining bytes in the underlying storage
        assertEquals(0, bytes.readRemaining(), "all bytes should be consumed after reading float64 values");

        // Check that Wire can safely handle reading beyond available data
        wire.read();
    }

    @DisplayName("Binary wire round-trips text fields and names")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire text fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void text(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
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
            assertEquals(e, sb.toString(), "text value should deserialize correctly into stringbuilder from binary wire");
        });

        // Ensure no more bytes are left to read from the wire
        assertEquals(0, bytes.readRemaining(), "all bytes should be consumed after reading text values");

        // Safeguard: Check if it's safe to read more from the wire even if there's nothing left
        wire.read();
    }

    @DisplayName("Binary wire round-trips type prefixes for fields")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire type fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void type(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
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
        Stream.of("MyType", "AlsoMyType", name1).forEach(e ->
                wire.read().typePrefix(e, (expected, actual) -> Assertions.assertEquals(expected, actual.toString(), "type prefix should match expected value in binary wire")));

        // Ensure no more bytes are left to read from the wire
        assertEquals(0, bytes.readRemaining(), "all bytes should be consumed after reading type prefixes");

        // Safeguard: Check if it's safe to read more from the wire even if there's nothing left
        wire.read();
    }

    // Testing the boolean write and read functionality of the wire
    @DisplayName("Binary wire round-trips boolean values and fields")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Bool fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testBool(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire(); // Create a wire instance

        WirePrimitiveTestSupport.assertBooleanRoundTrip(wire);
    }

    // Testing the float32 (i.e., single precision float) write and read functionality of the wire
    @DisplayName("Binary wire round-trips float32 values and fields")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Float 32 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testFloat32(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire(); // Create a wire instance

        WirePrimitiveTestSupport.assertFloat32RoundTrip(wire, this);
    }

    // Testing the LocalTime write and read functionality of the wire
    @DisplayName("Binary wire round-trips local time values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Time fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testTime(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire(); // Create a wire instance
        LocalTime now = LocalTime.of(12, 54, 4, 612 * 1000000); // Create a LocalTime instance

        WirePrimitiveTestSupport.writeTimes(wire, now);

        // An assertion for byte representation, it seems to be related to some internal functionality
        // (the details of which would depend on the context in which this test is used)
        if (testId <= 4) {
            assertEquals("c0                                              # :\n" +
                            "b2 0c 31 32 3a 35 34 3a 30 34 2e 36 31 32       # 12:54:04.612\n" +
                            "c0                                              # :\n" +
                            "b2 12 32 33 3a 35 39 3a 35 39 2e 39 39 39 39 39 # 23:59:59.999999999\n" +
                            "39 39 39 39 c0                                  # :\n" +
                            "b2 05 30 30 3a 30 30                            # 00:00\n",
                    bytes.toHexString(), "binary wire should serialize localtime with field names included");
        } else {
            assertEquals("b2 0c 31 32 3a 35 34 3a 30 34 2e 36 31 32       # 12:54:04.612\n" +
                            "b2 12 32 33 3a 35 39 3a 35 39 2e 39 39 39 39 39 # 23:59:59.999999999\n" +
                            "39 39 39 39 b2 05 30 30 3a 30 30                # 00:00\n",
                    bytes.toHexString(), "fieldless binary wire should serialize localtime without field names");
        }

        WirePrimitiveTestSupport.assertTimes(wire, now);
    }

    // Testing the ZonedDateTime write and read functionality of the wire
    @DisplayName("Binary wire round-trips zoned date time values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Zoned Date Time fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testZonedDateTime(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire(); // Create a wire instance
        ZonedDateTime now = ZonedDateTime.now(); // Get the current ZonedDateTime
        final ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        final ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());

        WireTemporalTestSupport.assertZonedDateTimes(wire);

        // Write the same ZonedDateTime values but this time as generic objects
        wire.write().object(now)
                .write().object(max)
                .write().object(min);

        // Read the ZonedDateTime values (stored as Objects) from the wire and assert they match the written values
        wire.read().object(Object.class, now, (expected, actual) -> Assertions.assertEquals(expected, actual, "zoneddatetime now should deserialize correctly as object from binary wire"))
                .read().object(Object.class, max, (expected, actual) -> Assertions.assertEquals(expected, actual, "zoneddatetime max should deserialize correctly as object from binary wire"))
                .read().object(Object.class, min, (expected, actual) -> Assertions.assertEquals(expected, actual, "zoneddatetime min should deserialize correctly as object from binary wire"));
    }

    // Testing the LocalDate write and read functionality of the wire
    @DisplayName("Binary wire round-trips local date values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Date fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testDate(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire(); // Create a wire instance

        WireTemporalTestSupport.assertLocalDates(wire);
    }

    // Testing the UUID write and read functionality of the wire
    @DisplayName("Binary wire round-trips UUID values and fields")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Uuid fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testUuid(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire(); // Create a wire instance

        WireTemporalTestSupport.assertUuids(wire);
    }

    // Testing the byte array write and read functionality of the wire
    @DisplayName("Binary wire round-trips byte arrays and bytes")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Bytes fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testBytes(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire();  // Create a wire instance
        @NotNull byte[] allBytes = new byte[256];  // Initialize a byte array of length 256

        // Fill the byte array with byte values from 0 to 255
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;

        WireBytesTestSupport.exerciseBytesRoundTrip(wire, WireBytesTestSupport.helloBytes(), WireBytesTestSupport.quoteBytes(), allBytes);


        // Read the byte arrays from the wire and assert they match the written values
        @SuppressWarnings("rawtypes")
        @NotNull NativeBytes allBytes2 = nativeBytes();
        WireBytesTestSupport.assertBytesRoundTrip(wire, allBytes, allBytes2);

        allBytes2.releaseLast();  // Release the last referenced resource
    }

    @DisplayName("Binary wire writes marshallable objects with fields")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Write Marshallable fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testWriteMarshallable(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Test the write marshallable functionality for BinaryWire

        // Uncomment the line below to set the BinaryWire.SPEC to 18.
        // BinaryWire.SPEC = 18;

        // Create a new wire instance.
        final Wire wire = createWire();

        // Initialize a MyTypesCustom instance (mtA) with specific values.
        @NotNull MyTypesCustom mtA = MyTypesCustomTestSupport.createA();

        // Write the above initialized instance (mtA) to the wire.
        wire.write(() -> "A").marshallable(mtA);

        // Initialize another MyTypesCustom instance (mtB) with different values.
        @NotNull MyTypesCustom mtB = MyTypesCustomTestSupport.createB();

        // Write the second initialized instance (mtB) to the wire.
        wire.write(() -> "B").marshallable(mtB);

        // Uncomment the line below to print the wire bytes in debug format.

        // Check the wire content against expected values.
        checkWire(wire,
                // Expected representation 1
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
        assertEquals(mt2, mtA, "marshallable object A should deserialize correctly from binary wire");

        wire.read(() -> "B").marshallable(mt2);
        assertEquals(mt2, mtB, "marshallable object B should deserialize correctly from binary wire");
    }

    @DisplayName("Binary wire writes null values and markers")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire write Null fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void writeNull(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Creating a wire instance and ensuring it's not null
        @NotNull Wire wire = createWire();

        String text = WireNullTestSupport.writeNulls(wire, w -> w.write().object(null), Circle.class);
        assertFalse(text.isEmpty(), "null write should produce non-empty output");
        assertEquals(0, wire.bytes().readRemaining(), "null write should consume all bytes after read");
    }

    @DisplayName("Binary wire round-trips long string documents")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Long String fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testLongString(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
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
            String[] actual = {null};
            assertTrue(wire.readDocument(null, w -> actual[0] = w.read(() -> "message").text()),
                    "long string should read document for i=" + i);
            assertEquals(s, actual[0], "long string should round-trip for i=" + i);
        }
    }

    @DisplayName("Binary wire round-trips object arrays and strings")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Arrays fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testArrays(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Creating a wire instance
        @NotNull Wire wire = createWire();

        WireArrayTestSupport.assertEmptyArrayRoundTrip(wire, false);

        // Writing an array of three strings to the wire
        @NotNull Object[] threeObjects = {"abc", "def", "ghi"};
        wire.write("b").object(threeObjects);

        // Reading the string array back from the wire and asserting its contents
        @Nullable Object[] object2 = wire.read()
                .object(Object[].class);
        assertEquals(3, object2.length, "object array length should be preserved after serialization in binary wire");
        assertEquals("[abc, def, ghi]", Arrays.toString(object2), "object array contents should match after round-trip through binary wire");

        wire.clear();
        WireArrayTestSupport.assertSimpleStringArrayRoundTrip(this::createWire, false);
    }

    @DisplayName("Binary wire round-trips empty and mixed arrays")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Arrays 2 fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testArrays2(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Creating a wire instance
        @NotNull Wire wire = createWire();

        WireArrayTestSupport.assertMixedArraysRoundTrip(wire);
    }

    @DisplayName("Binary wire reads event names and typed DTOs")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Using Events fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testUsingEvents(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Creating a wire instance with binary format
        final Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        w.usePadding(true);

        // Writing three events with DTOs to the wire
        try (DocumentContext dc = w.writingDocument(false)) {
            dc.wire().writeEventName("hello1").typedMarshallable(new DTO("world1"));
            dc.wire().writeEventName("hello2").typedMarshallable(new DTO("world2"));
            dc.wire().writeEventName("hello3").typedMarshallable(new DTO("world3"));
        }

        // Reading events back from the wire and asserting their correctness
        try (DocumentContext dc = w.readingDocument()) {


            StringBuilder sb = new StringBuilder();

            @NotNull ValueIn valueIn1 = dc.wire().readEventName(sb);
            Assertions.assertTrue("hello1".contentEquals(sb), "first event name should match 'hello1' in binary wire");
            valueIn1.skipValue();

            @NotNull ValueIn valueIn2 = dc.wire().readEventName(sb);
            Assertions.assertTrue("hello2".contentEquals(sb), "second event name should match 'hello2' in binary wire");

            valueIn2.skipValue(); // if you change this to typed marshable it works

            @NotNull ValueIn valueIn3 = dc.wire().readEventName(sb);
            Assertions.assertTrue("hello3".contentEquals(sb), "third event name should match 'hello3' in binary wire");

            @Nullable DTO o = valueIn3.typedMarshallable();
            Assertions.assertEquals("world3", o.text, "typed marshallable dto text field should deserialize correctly from binary wire");
        }
        w.bytes().releaseLast();
    }

    @DisplayName("Binary wire round-trips sorted set values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Sorted Set fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testSortedSet(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Creating a wire instance and a sorted set of strings
        final Wire wire = createWire();
        @NotNull SortedSet<String> set = new TreeSet<>();
        set.add("one");
        set.add("two");
        set.add("three");

        // Writing the sorted set to the wire
        wire.write("a").object(set);

        // Reading back from the wire and asserting the type and content
        @Nullable Object o = wire.read().object();
        assertInstanceOf(SortedSet.class, o, "deserialized object should be instance of sortedset from binary wire");
        assertEquals(set, o, "sortedset contents should match after round-trip through binary wire");
    }

    @DisplayName("Binary wire round-trips sorted map values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Sorted Map fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testSortedMap(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        // Creating a wire instance and a sorted map
        final Wire wire = createWire();
        @NotNull SortedMap<String, Long> set = new TreeMap<>();
        set.put("one", 1L);
        set.put("two", 2L);
        set.put("three", 3L);

        // Writing the sorted map to the wire
        wire.write("a").object(set);

        // Reading back from the wire and asserting the type and content
        @Nullable Object o = wire.read().object();
        assertInstanceOf(SortedMap.class, o, "deserialized object should be instance of sortedmap from binary wire");
        assertEquals(set, o, "sortedmap contents should match after round-trip through binary wire");
    }

    @DisplayName("Binary wire skips padding around marshallable values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire Skip Padding fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void testSkipPadding(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        @NotNull Wire wire = createWire();

        // Testing skipping padding for increasing padding values
        for (int i = 1; i <= 128; i *= 2) {
            wire.addPadding(i);
            wire.getValueIn().skipValue();
            assertEquals(0, wire.bytes().readRemaining(),
                    "all padding bytes should be consumed after skipping value for i=" + i);
            wire.clear();
        }

        // Testing marshallable values with increasing padding
        for (int i = 1; i <= 128; i *= 2) {
            wire.addPadding(i);
            int finalI = i;
            wire.getValueOut().marshallable(w -> w.write("i").int32(finalI));
            wire.getValueIn().skipValue();
            assertEquals(0, wire.bytes().readRemaining(),
                    "all bytes including padding should be consumed after skipping marshallable for i=" + i);
            wire.clear();
        }
    }

    @DisplayName("Binary wire reads comments and method events")
    @MethodSource("combinations")
    @SuppressWarnings("try")
    @ParameterizedTest(name = "Binary wire reads Comment fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void readsComment(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
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
        final MethodReader reader = wire.methodReader((IDTO) dto -> sb.append("dto: ").append(dto).append('\n'));
        assertTrue(reader.readOne(), "method reader should successfully read first document from binary wire");
        assertFalse(reader.readOne(), "method reader should return false when no more documents in binary wire");
        assertEquals("one\n" +
                "two\n" +
                "dto: !net.openhft.chronicle.wire.BinaryWireTest$DTO {\n" +
                "  text: text\n" +
                "}\n" +
                "\n" +
                "three\n", sb.toString(), "comments and dto content should be captured correctly from binary wire");
    }

    @DisplayName("Binary wire write End Of Wire Does Not Update Modified Time On No Op When Underlying Bytes Is File")
    @MethodSource("combinations")
    @ParameterizedTest(name = "Binary wire write End Of Wire Does Not Update Modified Time On No Op When Underlying Bytes Is File fixed={1}, numeric={2}, fieldless={3}, compressed={4}")
    void writeEndOfWireDoesNotUpdateModifiedTimeOnNoOpWhenUnderlyingBytesIsFile(int testId, boolean fixed, boolean numericField, boolean fieldLess, int compressedSize) throws IOException {
        initBinaryWireTest(testId, fixed, numericField, fieldLess, compressedSize);
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip end-of-wire file update test");

        // Create a temporary file for the test
        final File tempFile = IOTools.createTempFile("test-lastModified-endOfWire");
        final AtomicLong endOfWirePosition = new AtomicLong();

        // Create a wire from the temporary file and write a test entry
        createWireFromFileAnd(tempFile, wire -> {
            wire.write("testing-testing").int8(123);
            endOfWirePosition.set(wire.bytes().writePosition());
            assertTrue(wire.writeEndOfWire(100, TimeUnit.MILLISECONDS, endOfWirePosition.get()), "initial write end of wire should succeed for binary wire backed by file");
        });

        // this will wait until any pending resources have been closed
        BackgroundResourceReleaser.releasePendingResources();

        long lastModified = tempFile.lastModified();
        Jvm.pause(10);

        // Create a wire from the temporary file and attempt a no-op
        createWireFromFileAnd(tempFile, wire ->
                // This should be a no-op and not result in an update to lastModifiedTime
                assertFalse(wire.writeEndOfWire(100, TimeUnit.MILLISECONDS, endOfWirePosition.get()), "no-op write end of wire should return false for binary wire backed by file"));

        long lastModified2 = tempFile.lastModified();
        if (OS.isMacOSX() && lastModified2 - lastModified == 1)
            return;

        assertEquals(lastModified, lastModified2, "file modification time should not change for no-op write end of wire in binary wire");
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

        final String text;

        DTO(String text) {
            this.text = text;
        }
    }

    // A simple class representing a Circle
    private static class Circle implements Marshallable {
    }
}
