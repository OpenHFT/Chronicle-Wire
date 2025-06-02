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
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

@RunWith(value = Parameterized.class)
public class UnicodeStringTest extends WireTestCommon {

    // Suppressing unchecked warnings as Bytes class may handle various types
    @SuppressWarnings("rawtypes")
    @NotNull
    // Static byte buffer used for wire operations
    static Bytes<?> bytes = nativeBytes();

    // Wire object to handle serialization and deserialization
    static Wire wire = createWire();

    // Char array to be used in tests
    static char[] chars = new char[128];

    // Character under test
    private final char ch;

    // Constructor initializes the character under test
    public UnicodeStringTest(char ch) {
        this.ch = ch;
    }

    // Define the parameters for the test: a collection of characters
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        List<Object[]> chars = new ArrayList<>();
        int a = 1, b = 1;
        while (a < Character.MAX_VALUE) {
            int i = a++;
            a = b;
            b += i;
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
    @AfterClass
    public static void release() {
        bytes.releaseLast();
    }

    // Test case to validate serialization and deserialization of long strings
    @Test
    public void testLongString() {
        wire.clear(); // Clear the wire for a fresh start

        // Fill the char array with the character under test
        Arrays.fill(chars, ch);

        // Create a string from the char array
        @NotNull String s = new String(chars);

        // Write the string into the wire as a document
        wire.writeDocument(false, w -> w.write(() -> "msg").text(s));

        // Uncomment below to print the representation of the serialized data
        // System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));

        // Read the string from the wire and validate it matches the original
        wire.readDocument(null, w -> w.read(() -> "msg").text(s, Assert::assertEquals));
    }
}
