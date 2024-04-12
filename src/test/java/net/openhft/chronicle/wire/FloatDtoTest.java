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
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FloatDtoTest extends WireTestCommon {

    // Test method to serialize and deserialize a 'Value' object using a wire
    @Test
    public void test() {
        // Creating a 'Value' instance with specific values
        @NotNull final Value value = new Value(99, 2000f);
        final Bytes<?> bytes = Bytes.elasticByteBuffer();
        final Wire w = WireType.BINARY.apply(bytes);

        // Serializing the 'value' object to the wire
        w.write().marshallable(value);

        // Initializing another 'Value' instance with default values
        @NotNull Value object1 = new Value(0, 0.0f);

        // Deserializing data from the wire into the 'object1' instance
        w.read().marshallable(object1);

        // Asserting that the original 'value' and the deserialized 'object1' are equal
        assertEquals(value, object1);
        bytes.releaseLast();
    }

    // Inner static class 'Key' with a unique ID attribute
    static class Key extends SelfDescribingMarshallable implements KeyedMarshallable {
        // Suppress unused warning as the field may be used for serialization/deserialization purposes
        @SuppressWarnings("unused")
        int uiid;

        // Constructor to initialize the 'Key' with a unique ID
        Key(int uiid) {
            this.uiid = uiid;
        }
    }

    // Inner static class 'Value' that extends 'Key' and has an additional float attribute
    static class Value extends Key implements Marshallable {
        // Suppress unused warning as the field may be used for serialization/deserialization purposes
        @SuppressWarnings("unused")
        final float myFloat;

        // Constructor to initialize the 'Value' with a unique ID and a float
        Value(int uiid,
              float myFloat) {
            super(uiid);
            this.myFloat = myFloat;
        }
    }
}
