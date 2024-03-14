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

package net.openhft.chronicle.wire;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static net.openhft.chronicle.wire.WireType.BINARY;

// Test class to ensure proper method writing for interfaces with similar inner interfaces but different outer classes
public class GenerateMethodWriterSameInterfaceDifferentOuterClassTest extends TestCase {

    // Test the method writing capability for two different interfaces
    @Test
    public void test() {

        // Create a new Wire object with elastic byte buffer and BINARY settings
        final Wire wire = BINARY.apply(Bytes.elasticByteBuffer());

        // Activate padding for the wire object
        wire.usePadding(true);

        // Write the method for the InnerInterface of Outer1
        wire
                .methodWriterBuilder(Outer1.InnerInterface.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");

        // Write the method for the InnerInterface of Outer2
        wire
                .methodWriterBuilder(Outer2.InnerInterface.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .y("hello world");

        // Assert the expected output from the wire
        assertEquals("" +
                        "--- !!data #binary\n" +
                        "x: hello world\n" +
                        "# position: 20, header: 1\n" +
                        "--- !!data #binary\n" +
                        "y: hello world\n",
                Wires.fromSizePrefixedBlobs(wire));

    }
}
