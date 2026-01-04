/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.bytesmarshallable;

// Class: BenchRefBytesMain
// A class for performance benchmarking, particularly designed to
// test the efficiency and performance of operations related to reference bytes.
class BenchRefBytesMain {

    // Main method: entry point of the application, designed for
    // running the benchmarking test related to reference bytes operations.
    public static void main(String[] args) {
        // Instantiate a PerfRegressionHolder object.
        PerfRegressionHolder main = new PerfRegressionHolder();

        // Execute the benchmark test specific to reference byte operations.
        main.doTest(main::benchRefBytes);
    }
}
