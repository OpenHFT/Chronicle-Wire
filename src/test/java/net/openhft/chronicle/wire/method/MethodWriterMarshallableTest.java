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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

// Test class extending WireTestCommon to verify behavior of method writer with invalid interface
public class MethodWriterMarshallableTest extends WireTestCommon {

    // Test method expecting an IllegalArgumentException when using an invalid interface with method writer
    @Test(expected = IllegalArgumentException.class)
    public void invalidInterface() {
        // Create a new wire instance with the TEXT wire type
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        // Attempt to create a method writer for a bad interface, expecting an exception
        wire.methodWriter(MyBadInterface.class);
    }

    // Interface declared invalid for method writer usage due to it extending Marshallable
    interface MyBadInterface extends Marshallable {
    }
}
