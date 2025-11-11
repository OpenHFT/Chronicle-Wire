/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.bytesmarshallable;

// Class: BenchBytesMain
// A performance benchmarking class designed to test the efficiency
// and performance of byte-related operations.
public class BenchBytesMain {

    // Main method: entry point of the application, intended for
    // running the benchmarking test related to bytes operations.
    public static void main(String[] args) {
        // Instantiate a PerfRegressionHolder object.
        PerfRegressionHolder main = new PerfRegressionHolder();

        // Perform the benchmark test specific to byte operations.
        main.doTest(main::benchBytes);
    }
}
