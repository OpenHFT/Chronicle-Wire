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
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// This class tests the functionalities related to the QueryWire's read and write operations.
public class QueryWireTest extends WireTestCommon {

    // Byte storage to hold serialized data
    private Bytes<?> bytes;

    // Factory method to create and return a QueryWire instance.
    // Initializes the byte storage and associates it with the wire.
    @NotNull
    private QueryWire createWire() {
        bytes = allocateElasticOnHeap();
        return new QueryWire(bytes);
    }

    // Test case to verify both write and read operations of the QueryWire
    @Test
    public void readWriteQuery() {

        // Create a wire and write various data types to it
        @NotNull QueryWire wire = createWire();
        wire.write(() -> "bool").bool(true)
                .write(() -> "int").int64(12345)
                .write(() -> "text").text("Hello World")
                .write(() -> "float").float64(12.345);

        // Assert that the wire correctly serialized the data
        assertEquals("bool=true&int=12345&text=Hello World&float=12.345", bytes.toString());

        // Read from the wire and verify each data type
        wire.read(() -> "bool").bool(this, (o, b) -> assertTrue(b))
                .read(() -> "int").int64(this, (o, i) -> assertEquals(12345, i))
                .read(() -> "text").text(this, (o, s) -> assertEquals("Hello World", s))
                .read(() -> "float").float64(this, (o, f) -> assertEquals(12.345, f, 0.0));

        // Set up a WireParser to process each data type and add the values to a results list
        @NotNull WireParser wp = WireParser.wireParser((s, v) -> System.err.println(s + " " + v.text()));
        @NotNull List<Object> results = new ArrayList<>();
        wp.register(() -> "bool", (s, v) -> v.bool(results, List::add));
        wp.register(() -> "int", (s, v) -> v.int64(results, List::add));
        wp.register(() -> "text", (s, v) -> v.text(results, List::add));
        wp.register(() -> "float", (s, v) -> v.float64(results, List::add));

        // Reset the read position and use the WireParser to extract the data
        bytes.readPosition(0);
        while (bytes.readRemaining() > 0)
            wp.parseOne(wire);

        // Verify that the results list contains the correct values
        assertEquals(new ArrayList<>(Arrays.asList(true, 12345L, "Hello World", 12.345)), results);
    }
}
