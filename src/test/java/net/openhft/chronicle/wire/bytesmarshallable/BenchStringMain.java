//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.bytesmarshallable;

// Class: BenchStringMain
// A class for performance benchmarking, particularly designed to
// test the efficiency and performance of string-related operations.
public class BenchStringMain {

    // Main method: entry point of the application, designed for
    // running the benchmarking test related to string operations.
    public static void main(String[] args) {
        // Instantiate a PerfRegressionHolder object.
        PerfRegressionHolder main = new PerfRegressionHolder();

        // Execute the benchmark test specific to string operations.
        main.doTest(main::benchString);
    }
}
