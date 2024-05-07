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

import java.time.ZoneId;

// Define a test class named 'TestMarshallableZoneId' that tests
// serialization and deserialization of objects containing ZoneId fields
public class TestMarshallableZoneId {

    // Define a static nested class named 'MySelfDescribingMarshallable',
    // which extends 'SelfDescribingMarshallable' and includes a 'ZoneId' field
    public static class MySelfDescribingMarshallable extends SelfDescribingMarshallable {
        ZoneId zoneId; // Declare a ZoneId field named 'zoneId'
    }

    // Define a test method named 'testMySelfDescribingMarshallable'
    // to test serialization and deserialization of 'MySelfDescribingMarshallable' objects
    @Test
    public void testMySelfDescribingMarshallable() {

        // Create and initialize an instance of 'MySelfDescribingMarshallable',
        // setting its 'zoneId' field to "UTC"
        final MySelfDescribingMarshallable expected = new MySelfDescribingMarshallable();
        expected.zoneId = ZoneId.of("UTC");

        // Instantiate a JSONWire object and enable the usage of types
        JSONWire jsonWire = new JSONWire().useTypes(true);

        // Serialize the 'expected' object into the JSON wire
        jsonWire.getValueOut().object(expected);

        // Deserialize the object from the JSON wire into a new instance
        // and store it in the 'actual' variable
        final MySelfDescribingMarshallable actual = jsonWire.getValueIn().object(MySelfDescribingMarshallable.class);

        // Assert that the 'expected' and 'actual' objects are equal
        Assert.assertEquals(expected, actual);
    }

    // Define a static nested class named 'MyAbstractMarshallableCfg',
    // which extends 'AbstractMarshallableCfg' and includes a 'ZoneId' field
    public static class MyAbstractMarshallableCfg extends AbstractMarshallableCfg {
        ZoneId zoneId; // Declare a ZoneId field named 'zoneId'
    }

    // Define a test method named 'testMyAbstractMarshallableCfg'
    // to test serialization and deserialization of 'MyAbstractMarshallableCfg' objects
    @Test
    public void testMyAbstractMarshallableCfg() {

        // Create and initialize an instance of 'MyAbstractMarshallableCfg',
        // setting its 'zoneId' field to "UTC"
        final MyAbstractMarshallableCfg expected = new MyAbstractMarshallableCfg();
        expected.zoneId = ZoneId.of("UTC");

        // Instantiate a JSONWire object and enable the usage of types
        JSONWire jsonWire = new JSONWire().useTypes(true);

        // Serialize the 'expected' object into the JSON wire
        jsonWire.getValueOut().object(expected);

        // Deserialize the object from the JSON wire into a new instance
        // and store it in the 'actual' variable
        final MyAbstractMarshallableCfg actual = jsonWire.getValueIn().object(MyAbstractMarshallableCfg.class);

        // Assert that the 'expected' and 'actual' objects are equal
        Assert.assertEquals(expected, actual);
    }
}
