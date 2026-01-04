/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.ZoneId;

// Define a test class named 'TestMarshallableZoneId' that tests
// serialization and deserialization of objects containing ZoneId fields
public class MarshallableFixtureZoneId {

    // Define a static nested class named 'MySelfDescribingMarshallable',
    // which extends 'SelfDescribingMarshallable' and includes a 'ZoneId' field
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class MySelfDescribingMarshallable extends SelfDescribingMarshallable {
        ZoneId zoneId; // Declare a ZoneId field named 'zoneId'
    }

    // Define a test method named 'testMySelfDescribingMarshallable'
    // to test serialization and deserialization of 'MySelfDescribingMarshallable' objects
    @Test
    @DisplayName("ZoneId should round-trip for SelfDescribingMarshallable instances")
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
        Assertions.assertEquals(expected, actual, "ZoneId should round-trip in SelfDescribingMarshallable instances");
    }

    // Define a static nested class named 'MyAbstractMarshallableCfg',
    // which extends 'AbstractMarshallableCfg' and includes a 'ZoneId' field
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class MyAbstractMarshallableCfg extends AbstractMarshallableCfg {
        ZoneId zoneId; // Declare a ZoneId field named 'zoneId'
    }

    // Define a test method named 'testMyAbstractMarshallableCfg'
    // to test serialization and deserialization of 'MyAbstractMarshallableCfg' objects
    @Test
    @DisplayName("ZoneId should round-trip for AbstractMarshallableCfg instances")
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
        Assertions.assertEquals(expected, actual, "ZoneId should round-trip in AbstractMarshallableCfg instances");
    }
}
