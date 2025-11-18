/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

// This test class focuses on handling Throwable objects with different WireTypes.
public class ThrowableTest extends WireTestCommon {

    // Tests the writing and reading capabilities of a Throwable object with TEXT and BINARY_LIGHT WireTypes.
    @Test
    public void writeReadThrowable() {
        // Loop through TEXT and BINARY_LIGHT WireTypes for testing
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.BINARY_LIGHT}) {

            // Create the wire instance based on the current wireType
            Wire wire = wireType.apply(Bytes.allocateElasticDirect());
            try (DocumentContext dc = wire.writingDocument()) {
                // Initialize the Throwable object with a message and cause
                Throwable message = new Throwable("message");
                message.initCause(new Throwable("cause"));
            dc.wire().getValueOut()
                    .object(message);
            }
            assumeFalse(Jvm.maxDirectMemory() == 0);
            // Read the written Throwable and validate its content
            try (DocumentContext dc = wire.readingDocument()) {
                Throwable t = (Throwable) dc.wire().getValueIn().object();
                assertEquals("message", t.getMessage());
                assertTrue(t.getStackTrace()[0].toString().startsWith("net.openhft.chronicle.wire.ThrowableTest.writeReadThrowable(ThrowableTest.java"));
            }

            // Release the byte resources
            wire.bytes().releaseLast();
        }
    }
}
