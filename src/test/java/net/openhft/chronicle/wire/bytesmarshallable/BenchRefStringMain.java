/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.bytesmarshallable;

// Class: BenchRefStringMain
// A performance benchmarking class designed to test the efficiency
// and performance of operations related to reference strings.
public class BenchRefStringMain {

    // Main method: entry point of the application, designed for
    // running the benchmarking test related to reference strings operations.
    public static void main(String[] args) {
        // Instantiate a PerfRegressionHolder object.
        PerfRegressionHolder main = new PerfRegressionHolder();

        // Execute the benchmark test specific to reference string operations.
        main.doTest(main::benchRefString);
    }
}
