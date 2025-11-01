/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;


// This class tests the performance of raw wire operations.
@Ignore("Long running test")
public class RawWirePerfTest extends WireTestCommon {

    // Test case to measure the performance of raw wire operations.
    @Test
    public void testRawPerf() {

        // Create an instance of BinaryWirePerfTest with specific parameters.
        // These parameters typically control the test conditions.
        @NotNull BinaryWirePerfTest test = new BinaryWirePerfTest(-1, true, false, true);

        // Run the performance test on wire operations.
        test.wirePerf();

        // Run the performance test specifically for integers on the wire.
        test.wirePerfInts();
    }
}
