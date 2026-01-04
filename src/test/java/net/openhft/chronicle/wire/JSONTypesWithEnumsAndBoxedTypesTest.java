/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * This test class corresponds to an issue raised in the Chronicle-Wire repository.
 * See: https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
class JSONTypesWithEnumsAndBoxedTypesTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Providing two sets of parameters for the tests, based on whether types should be used or not.
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }


    // Enum representing various locations in a Formula 1 race.
    enum Location {
        PITS, TRACK, GRAVEL
    }

    // Class representing Formula 1 details.
    @UsedViaReflection
    static class F1 extends AbstractMarshallableCfg {

        private final String surname;  // Surname of the F1 driver.

        // change this to and int from an Integer and, it will work !
        private final Integer car;
        private final Location location;  // Represents the current location of the car.

        // Constructor for the F1 class.
        F1(String surname, int car, Location location) {
            this.surname = surname;
            this.car = car;
            this.location = location;
        }
    }

    // Test method to verify the JSON Wire representation.
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json boxed enum values")
    @DisplayName("Writes boxed enums to json with optional types")
    void test(boolean useTypes) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip json enums test");

        // Add an alias for the F1 class for a more concise YAML representation.
        ClassAliasPool.CLASS_ALIASES.addAlias(F1.class);

        // Create a new JSONWire instance and decide if it should use types based on `useTypes`.
        final JSONWire jsonWire = new JSONWire()
                .useTypes(useTypes);

        // Write the F1 object to the wire.
        jsonWire.getValueOut()
                .object(new F1("Hamilton", 44, Location.TRACK));

        // Print the bytes to the console for verification.
        System.out.println(jsonWire.bytes());

        // Extract the object from the wire and convert it to a string.
        final String actual = jsonWire.getValueIn().object().toString();

        // Assert to verify if the string representation contains the word "TRACK".
        Assertions.assertTrue(actual.contains("TRACK"),
                actual + " should include enum value TRACK");
    }
}
