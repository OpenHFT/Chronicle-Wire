/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

// Define a test class named 'TestMarshallableZoneId' that tests
// serialization and deserialization of objects containing ZoneId fields
class TestMarshallableZoneId {

    // Define a static nested class named 'MySelfDescribingMarshallable',
    // which extends 'SelfDescribingMarshallable' and includes a 'ZoneId' field
    static class MySelfDescribingMarshallable extends SelfDescribingMarshallable {
        ZoneId zoneId; // Declare a ZoneId field named 'zoneId'
    }

    // Define a test method named 'testMySelfDescribingMarshallable'
    // to test serialization and deserialization of 'MySelfDescribingMarshallable' objects
    @Test
    void testMySelfDescribingMarshallable() {

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
        assertEquals(expected, actual);
    }

    // Define a static nested class named 'MyAbstractMarshallableCfg',
    // which extends 'AbstractMarshallableCfg' and includes a 'ZoneId' field
    static class MyAbstractMarshallableCfg extends AbstractMarshallableCfg {
        ZoneId zoneId; // Declare a ZoneId field named 'zoneId'
    }

    // Define a test method named 'testMyAbstractMarshallableCfg'
    // to test serialization and deserialization of 'MyAbstractMarshallableCfg' objects
    @Test
    void testMyAbstractMarshallableCfg() {

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
        assertEquals(expected, actual);
    }
}
