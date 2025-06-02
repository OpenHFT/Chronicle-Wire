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
import java.util.List;

/**
 * Test for JSON wire handling of lists.
 * Related issue: https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
@RunWith(value = Parameterized.class)
public class JSONWireWithListsTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Determines whether to use types during serialization
    private final boolean useTypes;

    // Parameterized constructor for test variants
    @Parameterized.Parameters(name = "useTypes={0}")
    public static Collection<Object[]> wireTypes() {
        // Providing test parameters for both "true" and "false" values of useTypes
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    // Constructor initializes the useTypes flag
    public JSONWireWithListsTest(boolean useTypes) {
        this.useTypes = useTypes;
    }

    // Inner class representing a Formula 1 driver with surname and car number
    static class F1 {
        private String surname; // Surname of the driver
        private int car;        // Car number of the driver

        // Constructor for initializing F1 driver data
        public F1(String surname, int car) {
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
    @Test
    public void test() {
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
        Assert.assertEquals("[{surname=Hamilton, car=44}, {surname=Verstappen, car=33}]", actual);
    }
}
