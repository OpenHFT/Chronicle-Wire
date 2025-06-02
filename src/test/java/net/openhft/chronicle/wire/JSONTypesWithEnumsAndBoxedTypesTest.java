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

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * This test class corresponds to an issue raised in the Chronicle-Wire repository.
 * See: https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
@RunWith(value = Parameterized.class)
public class JSONTypesWithEnumsAndBoxedTypesTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Instance variable to determine if types are to be used in the JSON Wire representation.
    private final boolean useTypes;

    // Providing two sets of parameters for the tests, based on whether types should be used or not.
    @Parameterized.Parameters(name = "useTypes={0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    // Constructor to initialize the `useTypes` instance variable.
    public JSONTypesWithEnumsAndBoxedTypesTest(boolean useTypes) {
        this.useTypes = useTypes;
    }

    // Enum representing various locations in a Formula 1 race.
    enum Location {
        PITS, TRACK, GRAVEL
    }

    // Class representing Formula 1 details.
    static class F1 extends AbstractMarshallableCfg {

        private String surname;  // Surname of the F1 driver.

        // change this to and int from an Integer and, it will work !
        private Integer car;
        private Location location;  // Represents the current location of the car.

        // Constructor for the F1 class.
        public F1(String surname, int car, Location location) {
            this.surname = surname;
            this.car = car;
            this.location = location;
        }
    }

    // Test method to verify the JSON Wire representation.
    @Test
    public void test() {
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
        Assert.assertTrue(actual.contains("TRACK"));
    }
}
