/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarshallableCfgResetTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Represents an engine with a configuration whether it's electric or not
    static class Engine extends AbstractMarshallableCfg {
        boolean isItElectric;

        // Constructs an Engine instance with a given electric configuration
        Engine(boolean isItElectric) {
            this.isItElectric = isItElectric;
        }
    }

    // Represents a boat that contains an engine
    static class Boat extends SelfDescribingMarshallable {

        // Engine instance associated with the boat
        Engine engine;

        // Constructs a Boat instance with a given engine
        Boat(Engine engine) {
            this.engine = engine;
        }
    }

    /**
     * Tests that objects that extend AbstractMarshallableCfg are reset when calling {@link ValueIn#object(Object, Class)}
     */
    @Test
    void test() {
        // Create a boat instance with an engine that is not electric
        Boat k9f = new Boat(new Engine(false));

        // Instantiate a JSONWire object
        Wire w = new JSONWire();

        // Write the boat object to the wire
        w.getValueOut().object(k9f);

        // Check that 'isItElectric' isn't written as it has the default value
        assertEquals("{\"engine\":{}}", w.toString());

        // Create a boat instance with an engine that is electric
        Boat using = new Boat(new Engine(true));

        // Read from the wire into the boat object. Check if the electric field is overridden
        Boat boat = w.getValueIn().object(using, Boat.class);

        // Assert that the engine is electric after reading from wire
        assertTrue(boat.engine.isItElectric);

        // Set the engine as not electric in a new JSONWire and assert the change
        new JSONWire(Bytes.from("{\"engine\":{\"isItElectric\":false}}")).getValueIn().object(using, Boat.class);
        assertFalse(boat.engine.isItElectric);
    }
}
