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
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(value = Parameterized.class)
public class SequenceTest extends WireTestCommon {

    // Instance variable to hold the WireType.
    private final WireType wireType;

    // Constructor to initialize the WireType.
    public SequenceTest(WireType wireType) {
        this.wireType = wireType;
    }

    // Parameterized test setup to use different WireTypes.
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> wireTypes() {
        Object[][] list = {
                {WireType.BINARY},
                {WireType.TEXT},
                {WireType.JSON}
        };
        return Arrays.asList(list);
    }

    // Test method to check serialization and deserialization functionality.
    @Test
    public void test() {

        // Create a new My object.
        My m1 = new My();

        // Create an elastic ByteBuffer to hold serialized data.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        // Create a Wire instance using the WireType.
        Wire w1 = wireType.apply(bytes);

        // Populate the stuff list and serialize it.
        m1.stuff.addAll(Arrays.asList("one", "two", "three"));
        m1.writeMarshallable(w1);

        // Clear the stuff list, repopulate, and serialize again.
        m1.stuff.clear();
        m1.stuff.addAll(Arrays.asList("four", "five", "six"));
        m1.writeMarshallable(w1);

        // Repeat the above step with new values.
        m1.stuff.clear();
        m1.stuff.addAll(Arrays.asList("seven", "eight"));
        m1.writeMarshallable(w1);

        {
            // Create a new My object for deserialization.
            My m2 = new My();

            // Create another Wire instance for reading the serialized data.
            Wire w2 = wireType.apply(bytes);

            // Deserialize the serialized data and assert its content.
            m2.readMarshallable(w2);

            assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    one,\n" +
                    "    two,\n" +
                    "    three\n" +
                    "  ]\n" +
                    "}\n", m2.toString());

            // Deserialize the next set of serialized data and assert its content.
            m2.readMarshallable(w2);

            assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    four,\n" +
                    "    five,\n" +
                    "    six\n" +
                    "  ]\n" +
                    "}\n", m2.toString());

            // Deserialize the final set of serialized data and assert its content.
            m2.readMarshallable(w2);

            assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    seven,\n" +
                    "    eight\n" +
                    "  ]\n" +
                    "}\n", m2.toString());
        }

        // Release the resources held by the ByteBuffer.
        bytes.releaseLast();
    }

    // Test to read a Set as an object.
    @Test
    public void readSetAsObject() {
        // Allocate an elastic buffer on heap.
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Create a Wire instance using the WireType.
        Wire w1 = wireType.apply(bytes);

        // Define a value set.
        Set<String> value = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));

        // Write the set to the wire.
        try (DocumentContext dc = w1.writingDocument()) {
            dc.wire().write("list").object(value);
        }

        // Print the serialized data.
        System.out.println(Wires.fromSizePrefixedBlobs(w1));

        // Read the set back from the wire.
        try (DocumentContext dc = w1.readingDocument()) {
            Object o = dc.wire().read("list").object();
            if (wireType == WireType.JSON)
                o = new LinkedHashSet<>((Collection<?>) o);
            assertEquals(value, o);
        }
    }

    // Test to read a List as an object.
    @Test
    public void readListAsObject() {
        // Allocate an elastic buffer on heap.
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Create a Wire instance using the WireType.
        Wire w1 = wireType.apply(bytes);

        // Define a value list.
        List<String> value = Arrays.asList("a", "b", "c");

        // Write the list to the wire.
        try (DocumentContext dc = w1.writingDocument()) {
            dc.wire().write("list").object(value);
        }

        // Print the serialized data.
        System.out.println(Wires.fromSizePrefixedBlobs(w1));

        // Read the list back from the wire.
        try (DocumentContext dc = w1.readingDocument()) {
            Object o = dc.wire().read("list").object();
            assertEquals(value, o);
        }
    }

    // Test to read a Map as an object.
    @Test
    public void readMapAsObject() {
        // Ensure that the wire type isn't RAW.
        assumeFalse(wireType == WireType.RAW);

        // Allocate an elastic buffer on heap.
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Create a Wire instance using the WireType.
        Wire w1 = wireType.apply(bytes);

        // Define a value map.
        Map<String, String> value = new LinkedHashMap<>();
        value.put("a", "aya");
        value.put("b", "bee");

        // Write the map to the wire.
        try (DocumentContext dc = w1.writingDocument()) {
            dc.wire().write("map").object(value);
        }

        // Print the serialized data.
        System.out.println(Wires.fromSizePrefixedBlobs(w1));

        // Read the map back from the wire.
        try (DocumentContext dc = w1.readingDocument()) {
            Object o = dc.wire().read("map").object();
            assertEquals(value, o);
        }
    }

    // Static class My extending the SelfDescribingMarshallable class.
    static class My extends SelfDescribingMarshallable {
        List<CharSequence> stuff = new ArrayList<>(); // Instance variable to hold data.
        transient List<CharSequence> stuffBuffer = new ArrayList<>(); // Buffer for intermediate operations.

        // Method to read marshallable data from the wire.
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            wire.read("stuff").sequence(stuff, stuffBuffer, StringBuilder::new); // Read a sequence from the wire.
        }
    }
}
