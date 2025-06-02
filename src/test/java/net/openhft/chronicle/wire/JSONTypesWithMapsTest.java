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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This test class is associated with an issue raised in the Chronicle-Wire repository.
 * Refer: https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
@RunWith(value = Parameterized.class)
public class JSONTypesWithMapsTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Instance variable to determine if types are to be used in the JSON Wire representation.
    private final boolean useTypes;

    // Provide two sets of parameters for the tests, based on whether types should be used or not.
    @Parameterized.Parameters(name = "useTypes={0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    // Constructor initializes the `useTypes` instance variable based on the test parameters.
    public JSONTypesWithMapsTest(boolean useTypes) {
        this.useTypes = useTypes;
    }

    // Static class representing Formula 1 details.
    static class F1 {
        private String surname;  // Surname of the F1 driver.
        private int car;         // Represents the car number.

        // Constructor for the F1 class.
        public F1(String surname, int car) {
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
    @Test
    public void test() {

        // Create a new JSONWire instance and decide if it should use types based on `useTypes`.
        final JSONWire jsonWire = new JSONWire()
                .useTypes(useTypes);

        // Initialize the F1 object.
        final F1 f1 = new F1("Hamilton", 44);

        // Write a singleton map containing the F1 object to the wire.
        jsonWire.getValueOut()
                .object(singletonMap("Lewis", f1));

        // (Commented out) Printing the bytes to the console for verification.
        // System.out.println(jsonWire.bytes());

        // Expected string representation for the object read back from the wire.
        final String expected = "{Lewis=" + f1 + "}";

        // Extract the object from the wire.
        final Object object = jsonWire.getValueIn().object();

        // Verify the object isn't null and is an instance of a map.
        assertNotNull(object);
        assertTrue(object instanceof Map);

        // Convert the object to its string representation.
        final String actual = object.toString();

        // Assert to verify if the actual string matches the expected string.
        Assert.assertEquals(expected, actual);
    }
}
