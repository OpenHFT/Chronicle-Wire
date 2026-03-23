/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.io.StreamCorruptedException;

import static org.junit.jupiter.api.Assertions.*;

public class RawWirePerfMain extends WireTestCommon {

    public static void main(String[] args) throws StreamCorruptedException {
        // Create an instance of BinaryWirePerfTest with specific parameters.
        // These parameters typically control the test conditions.
        @NotNull BinaryWirePerfTest test = new BinaryWirePerfTest(-1, true, false, true);

        // Run the performance test on wire operations.
        test.wirePerf();

        // Run the performance test specifically for integers on the wire.
        test.wirePerfInts();
    }
}
