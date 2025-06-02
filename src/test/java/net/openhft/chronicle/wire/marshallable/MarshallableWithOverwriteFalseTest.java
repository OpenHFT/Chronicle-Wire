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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

/**
 * Test class to validate behavior of marshallable objects with overwriting disabled.
 */
public class MarshallableWithOverwriteFalseTest extends WireTestCommon {

    /**
     * Test method to evaluate the behavior of the marshallable logic.
     */
    @Test
    public void test() {

        // Create instances of MyDto2 and MyDto
        MyDto2 myDto2 = new MyDto2();
        MyDto myDto1 = new MyDto();

        // Add MyDto instance to the map of MyDto2 with an empty key
        myDto2.myDto.put("", myDto1);

        // Add string values to the MyDto instance
        myDto1.strings.add("hello");
        myDto1.strings.add("world");

        // Convert MyDto2 instance to string representation
        String cs = myDto2.toString();

        // Deserialize the string representation back to a MyDto2 object
        // System.out.println(cs);
        MyDto2 o = (MyDto2) Marshallable.fromString(cs);

        // Verify the size of the strings list in the deserialized object
        assertEquals(2, o.myDto.get("").strings.size());
    }

    /**
     * Inner class representing a basic data transfer object with a list of strings.
     */
    static class MyDto extends SelfDescribingMarshallable {
        // List to store string values
        List<String> strings = new ArrayList<>();

        /**
         * Reads the data from the provided WireIn object to populate this DTO.
         * @param wire WireIn instance containing serialized data
         * @throws IORuntimeException If an IO error occurs during reading
         */
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            // Use the Wires utility to read the data
            // The following line works, but is commented out for this test
            // Wires.readMarshallable(this, wire, true);

            // WORKS
             // Wires.readMarshallable(this, wire, true);  // WORKS

            // FAILS
            Wires.readMarshallable(this, wire, false);
        }
    }

    /**
     * Inner class representing a data transfer object containing a map of MyDto objects.
     */
    static class MyDto2 extends SelfDescribingMarshallable {
        // Map to store MyDto instances with String keys
        Map<String, MyDto> myDto = new TreeMap<String, MyDto>();
    }
}
