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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UsingTestMarshallableTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Test case to verify the conversion of a Marshallable object to its text representation
    @Test
    public void testConverMarshallableToTextName() {

        // Initialize a TestMarshallable object and set its name
        @NotNull TestMarshallable testMarshallable = new TestMarshallable();
        testMarshallable.setName("hello world");

        // Create a ByteBuffer to hold the serialized data
        Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer();

        // Initialize a Wire object with TEXT type
        @NotNull Wire wire = WireType.TEXT.apply(byteBufferBytes);
        wire.bytes().readPosition();

        // Write the TestMarshallable object to the Wire
        wire.writeDocument(false, d -> d.write(() -> "any-key").marshallable(testMarshallable));

        // Deserialize the Wire's bytes to a String
        String value = Wires.fromSizePrefixedBlobs(wire.bytes());

        //String replace = value.replace("\n", "\\n");

       // System.out.println(byteBufferBytes.toHexString());

        // Ensure the serialized output matches the expected format
        assertEquals("" +
                "--- !!data\n" +
                "any-key: {\n" +
                "  name: hello world,\n" +
                "  count: 0\n" +
                "}\n",
                value);

         // Assert.assertTrue(replace.length() > 1);
        // Release the ByteBuffer's resources
        byteBufferBytes.releaseLast();
    }

    // Test case to check the marshalling functionality using numbers as keys in binary wire
    // This test addresses the WIRE-37 issue
    @Test
    public void testMarshall() {

        // Create a ByteBuffer to hold the serialized data
        @SuppressWarnings("rawtypes")
        Bytes<?> bytes = Bytes.elasticByteBuffer();

        // Initialize a Wire object with BINARY type
        @NotNull Wire wire = new BinaryWire(bytes);

        // Create and initialize an instance of MyMarshallable
        @NotNull MyMarshallable x = new MyMarshallable();
        x.text.append("text");

        // Write the MyMarshallable object to the Wire
        wire.write(() -> "key").typedMarshallable(x);

        // Read back the MyMarshallable object from the Wire
        @NotNull final ValueIn read = wire.read(() -> "key");
        @Nullable final MyMarshallable result = read.typedMarshallable();

       // System.out.println(result.toString());

        // Ensure the read value matches the written one
        assertEquals("text", result.text.toString());

        // Release the ByteBuffer's resources
        bytes.releaseLast();
    }

    // Test case to check the write and read functionality for a Marshallable object
    @Test
    public void test() {

        Bytes<?> bytes = Bytes.elasticByteBuffer();
        Wire wire = WireType.BINARY.apply(bytes);
        @NotNull MarshableFilter expected = new MarshableFilter("hello", "world");

        // Write the MarshableFilter to the Wire
        {
            @NotNull SortedFilter sortedFilter = new SortedFilter();

            boolean add = sortedFilter.marshableFilters.add(expected);
            Assert.assertTrue(add);
            wire.write().marshallable(sortedFilter);
        }

        // Read back the MarshableFilter from the Wire
        {
            @NotNull SortedFilter sortedFilter = new SortedFilter();
            wire.read().marshallable(sortedFilter);
            assertEquals(1, sortedFilter.marshableFilters.size());
            assertEquals(expected, sortedFilter.marshableFilters.get(0));
        }
        bytes.releaseLast();
    }

    public static class MyMarshallable implements Marshallable {

        @NotNull
        public StringBuilder text = new StringBuilder();

        @Override
        public void readMarshallable(@NotNull WireIn wire) {
            wire.read(() -> "262").text(text);
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "262").text(text);
        }

        @NotNull
        @Override
        public String toString() {
            return "X{" +
                    "text=" + text +
                    '}';
        }
    }

    static class MarshableFilter extends SelfDescribingMarshallable {
        @NotNull
        public final String columnName;
        @NotNull
        public final String filter;

        public MarshableFilter(@NotNull String columnName, @NotNull String filter) {
            this.columnName = columnName;
            this.filter = filter;
        }
    }

    static class MarshableOrderBy extends SelfDescribingMarshallable {
        @NotNull
        public final String column;
        public final boolean isAscending;

        public MarshableOrderBy(@NotNull String column, boolean isAscending) {
            this.column = column;
            this.isAscending = isAscending;
        }
    }

    static class SortedFilter extends SelfDescribingMarshallable {
        public long fromIndex;
        @NotNull
        public List<MarshableOrderBy> marshableOrderBy = new ArrayList<>();
        @NotNull
        public List<MarshableFilter> marshableFilters = new ArrayList<>();
    }
}
