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

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class HashWireTest extends WireTestCommon {

    // Test the hashing capability for a sequence of marshallable entries
    @Test
    public void testHash64() {

        // Calculate a 64-bit hash value for the given wire data structure
        long h = HashWire.hash64(wire ->
                wire.write(() -> "entrySet").sequence(s -> {

                    // First marshallable entry with key and value
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-1")
                            .write(() -> "value").text("value-1"));

                    // Second marshallable entry with key and value
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-2")
                            .write(() -> "value").text("value-2"));
                }));

        // Ensure the computed hash is not 0
        assertNotEquals(0, h);
    }

    // Test the hashing capability for the Field object with a given name
    @Test
    public void testHashWithMap() {
        assertEquals(428977857, new Field("hi").hashCode());
    }

    // Simple enumeration for required fields
    enum Required {
        A
    }

    // Simple enumeration for values
    enum EnumValue {
        A
    }

    // A static inner class representing a Field with properties and behaviors
    static class Field extends SelfDescribingMarshallable implements Cloneable {
        private final String name; // The name of the field
        private final Map<String, Required> required = new HashMap<>(); // Map to store required field information
        private final List<EnumValue> values = new ArrayList<>(); // List to store enum values
        private boolean used = false; // Flag to check if the field is used

        // Constructor to initialize a Field with a given name
        public Field(String name) {
            this.name = name;
        }
    }
}
