/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.bytesmarshallable;

// Class: BenchUtf8StringMain
// A class for performance benchmarking, particularly designed to
// test the efficiency and performance of UTF-8 string-related operations.
class BenchUtf8StringMain {

    // Main method: entry point of the application, designed for
    // running the benchmarking test related to UTF-8 string operations.
    public static void main(String[] args) {
        // Instantiate a PerfRegressionHolder object.
        PerfRegressionHolder main = new PerfRegressionHolder();

        // Execute the benchmark test specific to UTF-8 string operations.
        main.doTest(main::benchUtf8String);
    }
}
