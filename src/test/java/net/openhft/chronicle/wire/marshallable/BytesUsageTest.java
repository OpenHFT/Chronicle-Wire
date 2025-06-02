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
        public Bytes<?> clOrdId() {
            return clOrdId;
        }

        // Setter for clOrdId that also allows chaining
        public BytesWrapper clOrdId(Bytes<?> clOrdId) {
            this.clOrdId = clOrdId;
            return this;
        }
    }
}
