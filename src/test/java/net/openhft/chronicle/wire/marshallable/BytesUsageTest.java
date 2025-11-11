/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * This class tests the usage of Bytes in various operations, emphasizing the importance of garbage-free operations.
 */
public class BytesUsageTest extends WireTestCommon {

    /**
     * Test the operations and manipulations on Bytes.
     * It showcases creating Bytes from a string and then appending it to other Bytes instances.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testBytes() {
        // Initialize a BytesStore instance from a string
        BytesStore<?, ?> value = Bytes.from("helloWorld");

        // Simple usage of BytesWrapper and setting its clOrdId value
        {
            BytesWrapper bw = new BytesWrapper();
            bw.clOrdId(Bytes.from("A" + value));
            assertEquals(Bytes.from("AhelloWorld"), bw.clOrdId());
        }

        // Garbage-free replacement of Bytes in BytesWrapper
        // This demonstrates how to avoid garbage creation by reusing objects
        BytesWrapper bw = new BytesWrapper();  // this instance should be recycled to avoid garbage
        bw.clOrdId().clear().append("A").append(value); // Direct manipulation of the Bytes
        assertEquals(Bytes.from("AhelloWorld"), bw.clOrdId());

        // Release any resources held by the Bytes instance
        value.releaseLast();
    }

    /**
     * A utility class to wrap Bytes. Allows operations to be performed on Bytes and demonstrates
     * a typical pattern for encapsulating Bytes in other objects.
     */
    @SuppressWarnings("rawtypes")
    static class BytesWrapper extends SelfDescribingMarshallable {

        // Holds an instance of Bytes which can be dynamically resized based on content
        Bytes<?> clOrdId = Bytes.allocateElasticOnHeap();

        // Getter for clOrdId
        Bytes<?> clOrdId() {
            return clOrdId;
        }

        // Setter for clOrdId that also allows chaining
        BytesWrapper clOrdId(Bytes<?> clOrdId) {
            this.clOrdId = clOrdId;
            return this;
        }
    }
}
