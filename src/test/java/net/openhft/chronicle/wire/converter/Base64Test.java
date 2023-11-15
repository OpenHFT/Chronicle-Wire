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

package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The Base64Test class tests the Base64 encoding and decoding functionalities
 * using the facilities provided by the WireTestCommon.
 */
public class Base64Test extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * Test the Base64 encoding for various field types and validate against expected outputs.
     */
    @Test
    public void onAnField() {
        // Create a new YAML wire with memory allocated on the heap
        Wire wire = Wire.newYamlWireOnHeap();

        // Get a writer for the UsesBase64 interface
        final UsesBase64 writer = wire.methodWriter(UsesBase64.class);

        // Convert strings "HelloWorld" and "Bye_Now" to long values using Base64 encoding
        final long helloWorld = Base64.INSTANCE.parse("HelloWorld");
        final long byeNow = Base64.INSTANCE.parse("Bye_Now");

        // Write the max values of various data types in Base64 encoding using the writer
        writer.asByte(Byte.MAX_VALUE);
        writer.asShort(Short.MAX_VALUE);
        writer.asInt(Integer.MAX_VALUE);
        writer.asLong(helloWorld);
        writer.send(new Data64(Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, byeNow));

        // Define the expected YAML output
        final String expected = "" +
                "asByte: A_\n" +
                "...\n" +
                "asShort: G__\n" +
                "...\n" +
                "asInt: A_____\n" +
                "...\n" +
                "asLong: HelloWorld\n" +
                "...\n" +
                "send: {\n" +
                "  b: A_,\n" +
                "  s: G__,\n" +
                "  i: A_____,\n" +
                "  data: Bye_Now\n" +
                "}\n" +
                "...\n";

        // Validate the wire's output against the expected output
        assertEquals(expected, wire.toString());

        // Create another YAML wire for reading the encoded data
        Wire wire2 = Wire.newYamlWireOnHeap();
        final MethodReader reader = wire.methodReader(wire2.methodWriter(UsesBase64.class));

        // Read and validate the data from the wire
        for (int i = 0; i <= 5; i++)
            assertEquals(i < 5, reader.readOne());

        // Ensure the read wire's content matches the expected output
        assertEquals(expected, wire2.toString());
    }

    /**
     * UsesBase64 interface defines the methods to demonstrate the usage of Base64 encoding.
     */
    interface UsesBase64 {
        void asByte(@Base64 byte base64);

        void asShort(@Base64 short base64);

        void asInt(@Base64 int base64);

        void asLong(@Base64 long base64);

        void send(Data64 data64);
    }

    /**
     * The Data64 class represents a set of data fields to be serialized using Base64 encoding.
     */
    static class Data64 extends SelfDescribingMarshallable {
        @Base64
        byte b;
        @Base64
        short s;
        @Base64
        int i;
        @Base64
        long data;

        /**
         * Constructor to initialize the data fields.
         *
         * @param b    Byte value.
         * @param s    Short value.
         * @param i    Int value.
         * @param data Long value.
         */
        public Data64(byte b, short s, int i, long data) {
            this.b = b;
            this.s = s;
            this.i = i;
            this.data = data;
        }
    }
}
