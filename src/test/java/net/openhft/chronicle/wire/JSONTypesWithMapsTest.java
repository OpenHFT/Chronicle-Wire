/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test class is associated with an issue raised in the Chronicle-Wire repository.
 * Refer: https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
public class JSONTypesWithMapsTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Instance variable to determine if types are to be used in the JSON Wire representation.
    private boolean useTypes;

    // Provide two sets of parameters for the tests, based on whether types should be used or not.
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    // Constructor initializes the `useTypes` instance variable based on the test parameters.
    public void initJSONTypesWithMapsTest(boolean useTypes) {
        this.useTypes = useTypes;
    }

    // Static class representing Formula 1 details.
    static class F1 {
        private final String surname;  // Surname of the F1 driver.
        private final int car;         // Represents the car number.

        // Constructor for the F1 class.
        F1(String surname, int car) {
            this.surname = surname;
            this.car = car;
        }

        // Overridden toString method for a custom string representation of F1 instances.
        @Override
        public String toString() {
            return "{" +
                    "surname=" + surname +
                    ", car=" + car +
                    '}';
        }
    }

    // Test method verifies the JSON Wire representation for a map containing an F1 instance.
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void test(boolean useTypes) {

        initJSONTypesWithMapsTest(useTypes);

        // Create a new JSONWire instance and decide if it should use types based on `useTypes`.
        final JSONWire jsonWire = new JSONWire()
                .useTypes(useTypes);

        // Initialize the F1 object.
        final F1 f1 = new F1("Hamilton", 44);

        // Write a singleton map containing the F1 object to the wire.
        jsonWire.getValueOut()
                .object(singletonMap("Lewis", f1));

        // (Commented out) Printing the bytes to the console for verification.

        // Expected string representation for the object read back from the wire.
        final String expected = "{Lewis=" + f1 + "}";

        // Extract the object from the wire.
        final Object object = jsonWire.getValueIn().object();

        // Verify the object isn't null and is an instance of a map.
        assertNotNull(object);
        assertInstanceOf(Map.class, object);

        // Convert the object to its string representation.
        final String actual = object.toString();

        // Assert to verify if the actual string matches the expected string.
        Assertions.assertEquals(expected, actual);
    }
}
