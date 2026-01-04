/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
public class UnicodeStringTest extends WireTestCommon {

    // Suppressing unchecked warnings as Bytes class may handle various types
    @SuppressWarnings("rawtypes")
    @NotNull
    // Static byte buffer used for wire operations
    private static final Bytes<?> bytes = nativeBytes();

    // Wire object to handle serialization and deserialization
    private static final Wire wire = createWire();

    // Char array to be used in tests
    private static final char[] chars = new char[128];

    // Character under test
    private char ch;

    // Constructor initializes the character under test
    public void initUnicodeStringTest(char ch) {
        this.ch = ch;
    }

    // Define the parameters for the test: a collection of characters
    public static Collection<Object[]> combinations() {
        List<Object[]> chars = new ArrayList<>();
        int a = 1;
        int b = 1;
        while (a < Character.MAX_VALUE) {
            int i = a;
            int next = a + b;
            a = b;
            b = next;
            if (!Character.isValidCodePoint(i))
                continue;
            chars.add(new Object[]{(char) i});
        }
        // Special characters to always test
        for (int ch : new int[]{0x0, 0x7F, 0x80, 0x07FF, 0x800, 0xFFFF})
            chars.add(new Object[]{(char) ch});
        return chars;
    }

    // Helper method to create a BinaryWire instance with specific configurations
    @NotNull
    private static BinaryWire createWire() {
        bytes.clear();
        final boolean fixed = true;
        final boolean numericField = false;
        final boolean fieldLess = false;
        final int compressedSize = 128;
        @NotNull BinaryWire wire = new BinaryWire(bytes, fixed, numericField, fieldLess, compressedSize, "lzw");

        return wire;
    }

    // Release the byte buffer after all tests have been executed
    @AfterAll
    public static void release() {
        bytes.releaseLast();
    }

    // Test case to validate serialization and deserialization of long strings
    @DisplayName("Round-trips long unicode strings across code points")
    @MethodSource("combinations")
    @ParameterizedTest
    public void testLongString(char ch) {
        initUnicodeStringTest(ch);
        wire.clear(); // Clear the wire for a fresh start

        // Fill the char array with the character under test
        Arrays.fill(chars, ch);

        // Create a string from the char array
        @NotNull String s = new String(chars);

        // Write the string into the wire as a document
        wire.writeDocument(false, w -> w.write(() -> "msg").text(s));

        // Uncomment below to print the representation of the serialized data

        // Read the string from the wire and validate it matches the original
        String[] actual = {null};
        wire.readDocument(null, w -> actual[0] = w.read(() -> "msg").text());
        Assertions.assertEquals(s, actual[0], "long string should round-trip for ch=0x" + Integer.toHexString(ch));
    }
}
