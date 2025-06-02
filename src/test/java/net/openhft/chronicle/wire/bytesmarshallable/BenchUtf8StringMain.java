/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.bytesmarshallable;

// Class: BenchUtf8StringMain
// A class for performance benchmarking, particularly designed to
// test the efficiency and performance of UTF-8 string-related operations.
public class BenchUtf8StringMain {

    // Main method: entry point of the application, designed for
    // running the benchmarking test related to UTF-8 string operations.
    public static void main(String[] args) {
        // Instantiate a PerfRegressionHolder object.
        PerfRegressionHolder main = new PerfRegressionHolder();

        // Execute the benchmark test specific to UTF-8 string operations.
        main.doTest(main::benchUtf8String);
    }
}
