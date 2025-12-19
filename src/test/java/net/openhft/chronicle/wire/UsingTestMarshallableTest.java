/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UsingTestMarshallableTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Test case to verify the conversion of a Marshallable object to its text representation
    @Test
    public void testConverMarshallableToTextName() {

        // Initialize a MarshallableFixture object and set its name
        @NotNull MarshallableFixture testMarshallable = new MarshallableFixture();
        testMarshallable.setName("hello world");

        // Create a ByteBuffer to hold the serialized data
        Bytes<?> byteBufferBytes = Bytes.allocateElasticOnHeap();

        // Initialize a Wire object with TEXT type
        @NotNull Wire wire = WireType.TEXT.apply(byteBufferBytes);
        wire.bytes().readPosition();

        // Write the MarshallableFixture object to the Wire
        wire.writeDocument(false, d -> d.write(() -> "any-key").marshallable(testMarshallable));

        // Deserialize the Wire's bytes to a String
        String value = Wires.fromSizePrefixedBlobs(wire.bytes());

        // Ensure the serialised output matches the expected format
        assertEquals("--- !!data\n" +
                        "any-key: {\n" +
                        "  name: hello world,\n" +
                        "  count: 0\n" +
                        "}\n",
                value);

        // Release the ByteBuffer's resources
        byteBufferBytes.releaseLast();
    }

    // Test case to check the marshalling functionality using numbers as keys in binary wire
    // This test addresses the WIRE-37 issue
    @Test
    public void testMarshall() {

        // Create a ByteBuffer to hold the serialized data
        @SuppressWarnings("rawtypes")
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

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

        // Ensure the read value matches the written one
        assertEquals("text", result.text.toString());

        // Release the ByteBuffer's resources
        bytes.releaseLast();
    }

    // Test case to check the write and read functionality for a Marshallable object
    @Test
    public void test() {

        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.BINARY.apply(bytes);
        @NotNull MarshableFilter expected = new MarshableFilter("hello", "world");

        // Write the MarshableFilter to the Wire
        {
            @NotNull SortedFilter sortedFilter = new SortedFilter();

            boolean add = sortedFilter.marshableFilters.add(expected);
            Assertions.assertTrue(add);
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

    // Class representing a Marshallable object with text data.
    static class MyMarshallable implements Marshallable {

        // Mutable sequence of characters to store textual data.
        @NotNull
        final StringBuilder text = new StringBuilder();

        // Method responsible for deserializing the object from the Wire input.
        @Override
        public void readMarshallable(@NotNull WireIn wire) {
            // Read the 'text' field from the wire with key "262".
            wire.read(() -> "262").text(text);
        }

        // Method responsible for serializing the object to the Wire output.
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            // Write the 'text' field to the wire with key "262".
            wire.write(() -> "262").text(text);
        }

        // Method to provide a string representation of the object.
        @NotNull
        @Override
        public String toString() {
            // Construct a string representation with the 'text' field value.
            return "X{" +
                    "text=" + text +
                    '}';
        }
    }

    // Class representing a filter condition for data, defined by a column name and a filter expression.
    static class MarshableFilter extends SelfDescribingMarshallable {
        // Name of the column to which the filter applies.
        @NotNull
        final String columnName;

        // Filter expression used to filter data.
        @NotNull
        final String filter;

        // Constructor to initialize column name and filter expression.
        MarshableFilter(@NotNull String columnName, @NotNull String filter) {
            this.columnName = columnName;
            this.filter = filter;
        }
    }

    // Class representing an order-by condition for data, defined by a column name and sort direction.
    static class MarshableOrderBy extends SelfDescribingMarshallable {
        // Name of the column used for ordering data.
        @NotNull
        final String column;

        // Flag indicating the sort direction: true for ascending, false for descending.
        final boolean isAscending;

        // Constructor to initialize column name and sort direction.
        public MarshableOrderBy(@NotNull String column, boolean isAscending) {
            this.column = column;
            this.isAscending = isAscending;
        }
    }

    // Class representing a filter with sorting details for processing data.
    static class SortedFilter extends SelfDescribingMarshallable {
        // Index from which the filtering should start.
        public long fromIndex;

        // List of order-by conditions specifying the columns and their respective sort directions.
        @NotNull
        public List<MarshableOrderBy> marshableOrderBy = new ArrayList<>();

        // List of filter conditions specifying the columns and their respective filter expressions.
        @NotNull
        final List<MarshableFilter> marshableFilters = new ArrayList<>();
    }
}
