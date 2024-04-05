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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

// This test suite is designed to verify compatibility behaviors of the TextWire class,
// especially when fields are added or modified.
public class TextWireCompatibilityTest extends WireTestCommon {

    // Test to check the behavior when fields are added in the middle of a marshallable object.
    // The main purpose is to ensure that compatibility is maintained during such changes.
    @Test
    public void testAddFieldsInTheMiddle() {
        // Create a new TextWire instance with an elastic heap allocated buffer
        @NotNull Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(100));

        // Write an instance of SubIncompatibleObject to the wire
        wire.getValueOut().object(new SubIncompatibleObject());

        // Uncomment the below line to debug and view the wire output
        // System.out.println(wire.toString());

        // Read an object from the wire and ensure it's not null
        Assert.assertNotNull(wire.getValueIn().object());
    }

    // A superclass designed to be marshallable with basic incompatibility checks.
    // It primarily checks for the presence and value of the "a" field, and absence of the "c" field.
    public static class SuperIncompatibleObject implements Marshallable {
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            // Verify the value of the "a" field
            Assert.assertEquals(1, wire.read("a").int32());

            // Check if the "c" field is missing, and log an error if present
            @Nullable String missingValue = wire.read("c").text();
            if (missingValue != null) {
                System.err.println("expected null, had: <" + missingValue + ">");
            }
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            // Write the "a" field with value 1
            wire.write(() -> "a").int32(1);
        }
    }

    // A subclass extending the SuperIncompatibleObject class.
    // It checks and writes additional fields to the wire to test compatibility.
    public static class SubIncompatibleObject extends SuperIncompatibleObject {
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            super.readMarshallable(wire);

            // Verify the value of the "b" field and the presence of "object" and "object2" fields
            Assert.assertEquals(TextWireCompatibilityTest.class, wire.read("b").typeLiteral());
            Assert.assertNotNull(wire.read(() -> "object").object());
            Assert.assertNotNull(wire.read(() -> "object2").object());
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            super.writeMarshallable(wire);

            // Write additional fields "b", "object", and "object2" to the wire
            wire.write(() -> "b").typeLiteral(TextWireCompatibilityTest.class);
            wire.write(() -> "object").object(new SimpleObject());
            wire.write(() -> "object2").object(new SimpleObject());
        }
    }

    // A simple marshallable class used as a placeholder object in the SubIncompatibleObject class.
    public static class SimpleObject implements Marshallable {
    }
}
