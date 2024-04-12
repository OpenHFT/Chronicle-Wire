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

// Class: BenchArrayStringMain
// A performance benchmarking class designed to test the efficiency
// and performance of operations related to array strings.
public class BenchArrayStringMain {

    // Main method: entry point of the application, intended for
    // running the benchmarking test related to array strings.
    public static void main(String[] args) {
        // Instantiate a PerfRegressionHolder object.
        PerfRegressionHolder main = new PerfRegressionHolder();

        // Perform the benchmark test specific to array string operations.
        main.doTest(main::benchArrayString);
    }
}
