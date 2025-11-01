/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.bytesmarshallable;

// Class: BenchNullMain
// A performance benchmarking class designed to test the efficiency
// and performance of operations with null values.
public class BenchNullMain {

    // Main method: entry point of the application, intended for
    // running the benchmarking test related to null operations.
    public static void main(String[] args) {
        // Instantiate a PerfRegressionHolder object.
        PerfRegressionHolder main = new PerfRegressionHolder();

        // Perform the benchmark test specific to operations involving null values.
        main.doTest(main::benchNull);
    }
}
