/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.io.StreamCorruptedException;

// This class tests the performance of raw wire operations.
@Ignore("Long running test")
public class RawWirePerfTest extends WireTestCommon {

    // Test case to measure the performance of raw wire operations.
    @Test
    public void testRawPerf() throws StreamCorruptedException {

        // Create an instance of BinaryWirePerfTest with specific parameters.
        // These parameters typically control the test conditions.
        @NotNull BinaryWirePerfTest test = new BinaryWirePerfTest(-1, true, false, true);

        // Run the performance test on wire operations.
        test.wirePerf();

        // Run the performance test specifically for integers on the wire.
        test.wirePerfInts();
    }
}
