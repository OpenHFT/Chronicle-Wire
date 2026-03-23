/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import static net.openhft.chronicle.wire.WireType.BINARY;

// Test class to ensure proper method writing for interfaces with similar inner interfaces but different outer classes
public class GenerateMethodWriterSameInterfaceDifferentOuterClassTest extends WireTestCommon {

    // Test the method writing capability for two different interfaces
    @Test
    public void test() {

        // Create a new Wire object with elastic byte buffer and BINARY settings
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());

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
