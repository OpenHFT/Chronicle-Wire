/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.bytesmarshallable;

// Class: BenchFieldsMain
// A performance benchmarking class designed to test the efficiency
// and performance of field-related operations.
public class BenchFieldsMain {

    // Main method: entry point of the application, intended for
    // running the benchmarking test related to fields operations.
    public static void main(String[] args) {
        // Instantiate a PerfRegressionHolder object.
        PerfRegressionHolder main = new PerfRegressionHolder();

        // Perform the benchmark test specific to field operations.
        main.doTest(main::benchFields);
    }
}
