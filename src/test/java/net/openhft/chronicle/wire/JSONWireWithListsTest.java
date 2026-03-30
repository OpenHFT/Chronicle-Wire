/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for JSON wire handling of lists.
 * Related issue: https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
class JSONWireWithListsTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Determines whether to use types during serialization
    private boolean useTypes;

    // Parameterized constructor for test variants
    public static Collection<Object[]> wireTypes() {
        // Providing test parameters for both "true" and "false" values of useTypes
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    // Inner class representing a Formula 1 driver with surname and car number
    static class F1 {
        private String surname; // Surname of the driver
        private int car;        // Car number of the driver

        // Constructor for initializing F1 driver data
        F1(String surname, int car) {
            this.surname = surname;
            this.car = car;
        }

        // Overrides the toString method to format the driver data
        @Override
        public String toString() {
            return "{" +
                    "surname=" + surname +
                    ", car=" + car +
                    '}';
        }
    }

    // Test case for validating the serialization and deserialization of a list of drivers
    @ParameterizedTest
    @MethodSource("wireTypes")
    void test(boolean useTypes) {
        this.useTypes = useTypes;
        // Instantiating a JSON wire with or without types based on test parameter
        final JSONWire jsonWire = new JSONWire()
                .useTypes(useTypes);

        // Creating a list of F1 drivers
        final List<F1> drivers = Arrays.asList(new F1("Hamilton", 44), new F1("Verstappen", 33));

        // Serializing the list of drivers using the JSON wire
        jsonWire.getValueOut().object(drivers);

        // Deserializing the list of drivers and converting to string
        final String actual = jsonWire.getValueIn().object().toString();

        // Asserting the deserialized value against the expected format
        assertEquals("[{surname=Hamilton, car=44}, {surname=Verstappen, car=33}]", actual);
    }
}
