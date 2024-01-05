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

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
                wire.getValueOut()
                        .object(message);
            }
/*            if (wireType == WireType.TEXT)
                System.out.println(wire);
            else
                System.out.println(wire.bytes().toHexString()+"\n"+Wires.fromSizePrefixedBlobs(wire.bytes()));*/

            // Read the written Throwable and validate its content
            try (DocumentContext dc = wire.readingDocument()) {
                Throwable t = (Throwable) wire.getValueIn().object();
                assertEquals("message", t.getMessage());
                assertTrue(t.getStackTrace()[0].toString().startsWith("net.openhft.chronicle.wire.ThrowableTest.writeReadThrowable(ThrowableTest.java"));
            }

            // Release the byte resources
            wire.bytes().releaseLast();
        }
    }
}
