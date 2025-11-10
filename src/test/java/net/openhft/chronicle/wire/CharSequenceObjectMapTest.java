//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharSequenceObjectMapTest extends WireTestCommon {

    // Test the put operation of the CharSequenceObjectMap
    @Test
    public void put() {
        // Initialize a new CharSequenceObjectMap with a capacity of 10
        CharSequenceObjectMap<String> map = new CharSequenceObjectMap<>(10);

        // Add key-value pairs to the map for values from 10 to 19
        for (int i = 10; i < 20; i++) {
            map.put("" + i, "" + i);
        }

        // Verify that the added key-value pairs can be successfully retrieved from the map
        for (int i = 10; i < 20; i++) {
            assertEquals("" + i, map.get("" + i));
        }
    }
}
