/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Exercises overwrite-false reads to ensure nested DTO list values persist after round-trip.
 */
class MarshallableWithOverwriteFalseTest extends WireTestCommon {

    @Test
    @DisplayName("Overwrite false should preserve nested strings list")
    void test() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory must be available for overwrite false test");

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

        // Deserialise the string representation back to a MyDto2 object
        MyDto2 o = Marshallable.fromString(cs);

        // Verify the size of the strings list in the deserialised object
        assertEquals(2, o.myDto.get("").strings.size(),
                "Deserialised DTO should retain both string entries");
    }

    /**
     * Inner class representing a basic data transfer object with a list of strings.
     */
    static class MyDto extends SelfDescribingMarshallable {
        // List to store string values
        final List<String> strings = new ArrayList<>();

        /**
         * Reads the data from the provided WireIn object to populate this DTO.
         *
         * @param wire WireIn instance containing serialised data
         * @throws IORuntimeException If an IO error occurs during reading
         */
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            // Use the Wires utility to read the data
            // The following line works, but is commented out for this test
            // Wires.readMarshallable(this, wire, true);

            // FAILS
            Wires.readMarshallable(this, wire, false);
        }
    }

    /**
     * Inner class representing a data transfer object containing a map of MyDto objects.
     */
    static class MyDto2 extends SelfDescribingMarshallable {
        // Map to store MyDto instances with String keys
        final Map<String, MyDto> myDto = new TreeMap<>();
    }
}
