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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// Test class extending WireTestCommon to test method identification in binary wire format
public class MethodIdTest extends WireTestCommon {

    // Test method to verify serialization and deserialization of methods with various IDs
    @Test
    public void methodIdInBinary() {
        // Create a new BinaryWire instance with specified configurations
        Wire wire = new BinaryWire(new HexDumpBytes(), true, true, false, 128, "");

        // Generate a proxy instance for the Methods interface
        final Methods methods = wire.methodWriter(Methods.class);

        // Call different methods with various argument types and values
        methods.methodAt('@');
        methods.method_z('z');
        methods.methodByteMax(Byte.MAX_VALUE);
        methods.methodShortMax(Short.MAX_VALUE);
        methods.methodIntMax(Integer.MAX_VALUE);
        // methods.methodLongMax(Long.MAX_VALUE); // Not supported yet

        methods.methodByteMin(Byte.MIN_VALUE);
        methods.methodShortMin(Short.MIN_VALUE);
        methods.methodIntMin(Integer.MIN_VALUE);
        // methods.methodLongMin(Long.MIN_VALUE); // Not supported yet

        // Asserts the binary representation of the method calls
        assertEquals("" +
                        "04 00 00 00                                     # msg-length\n" +
                        "ba 40                                           # methodAt ('@')\n" +
                        "e1 40                                           # @\n" +
                        "04 00 00 00                                     # msg-length\n" +
                        "ba 7a                                           # method_z ('z')\n" +
                        "e1 7a                                           # z\n" +
                        "0b 00 00 00                                     # msg-length\n" +
                        "ba 7f                                           # methodByteMax (127)\n" +
                        "a7 7f 00 00 00 00 00 00 00                      # 127\n" +
                        "0d 00 00 00                                     # msg-length\n" +
                        "ba ff ff 01                                     # methodShortMax (32767)\n" +
                        "a7 ff 7f 00 00 00 00 00 00                      # 32767\n" +
                        "0f 00 00 00                                     # msg-length\n" +
                        "ba ff ff ff ff 07                               # methodIntMax (2147483647)\n" +
                        "a7 ff ff ff 7f 00 00 00 00                      # 2147483647\n" +
                        "0c 00 00 00                                     # msg-length\n" +
                        "ba ff 00                                        # methodByteMin (-128)\n" +
                        "a7 80 ff ff ff ff ff ff ff                      # -128\n" +
                        "0e 00 00 00                                     # msg-length\n" +
                        "ba ff ff 81 00                                  # methodShortMin (-32768)\n" +
                        "a7 00 80 ff ff ff ff ff ff                      # -32768\n" +
                        "10 00 00 00                                     # msg-length\n" +
                        "ba ff ff ff ff 87 00                            # methodIntMin (-2147483648)\n" +
                        "a7 00 00 00 80 ff ff ff ff                      # -2147483648\n",
                wire.bytes().toHexString());

        // Create a new YAML based Wire for reading
        Wire wire2 = Wire.newYamlWireOnHeap();

        // Create a method reader and a writer for the Methods interface
        final MethodReader reader = wire.methodReader(wire2.methodWriter(Methods.class));

        // Read each method call and verify the output
        for (int i = 0; i < 8; i++)
            reader.readOne();
        assertEquals("" +
                        "methodAt: \"@\"\n" +
                        "...\n" +
                        "method_z: z\n" +
                        "...\n" +
                        "methodByteMax: 127\n" +
                        "...\n" +
                        "methodShortMax: 32767\n" +
                        "...\n" +
                        "methodIntMax: 2147483647\n" +
                        "...\n" +
                        "methodByteMin: -128\n" +
                        "...\n" +
                        "methodShortMin: -32768\n" +
                        "...\n" +
                        "methodIntMin: -2147483648\n" +
                        "...\n",
                wire2.toString());
        assertFalse(reader.readOne());
    }

    // Interface defining various methods with specific method IDs using annotations
    interface Methods {
// not supported yet
//        @MethodId(Long.MIN_VALUE)
//        void methodLongMin(long a);

        @MethodId('@')
        void methodAt(char at);
        @MethodId('z')
        void method_z(char z);

        @MethodId(Integer.MIN_VALUE)
        void methodIntMin(long a);

        @MethodId(Short.MIN_VALUE)
        void methodShortMin(long a);

        @MethodId(Byte.MIN_VALUE)
        void methodByteMin(long a);

// not supported yet
//        @MethodId(Long.MAX_VALUE)
//        void methodLongMax(long a);

        @MethodId(Integer.MAX_VALUE)
        void methodIntMax(long a);

        @MethodId(Short.MAX_VALUE)
        void methodShortMax(long a);

        @MethodId(Byte.MAX_VALUE)
        void methodByteMax(long a);
    }
}
