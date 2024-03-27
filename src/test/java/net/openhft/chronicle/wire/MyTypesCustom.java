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

// Class MyTypesCustom extends MyTypes and implements Marshallable to provide custom serialization logic
class MyTypesCustom extends MyTypes implements Marshallable {

    // Override the writeMarshallable method to dictate how instances of MyTypesCustom will be serialized to wire format
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        // Writing each field to the wire with specified keys and values
        wire.write(Fields.B_FLAG).bool(flag) // serializes the boolean flag value
                .write(Fields.S_NUM).int16(s) // serializes the short value s with 16 bits
                .write(Fields.D_NUM).float64(d) // serializes the double value d with 64 bits
                .write(Fields.L_NUM).int64(l) // serializes the long value l with 64 bits
                .write(Fields.I_NUM).int32(i) // serializes the int value i with 32 bits
                .write(Fields.TEXT).text(text); // serializes the StringBuilder text
    }

    // Override the readMarshallable method to specify how instances of MyTypesCustom will be deserialized from wire format
    @Override
    public void readMarshallable(@NotNull WireIn wire) {
        // Reading each field from the wire with specified keys and assigning values to instance variables using lambdas
        wire.read(Fields.B_FLAG).bool(this, (o, x) -> o.flag = x) // deserializes a boolean value and assigns it to flag
                .read(Fields.S_NUM).int16(this, (o, x) -> o.s = x) // deserializes a 16-bit int value and assigns it to s
                .read(Fields.D_NUM).float64(this, (o, x) -> o.d = x) // deserializes a 64-bit float value and assigns it to d
                .read(Fields.L_NUM).int64(this, (o, x) -> o.l = x) // deserializes a 64-bit int value and assigns it to l
                .read(Fields.I_NUM).int32(this, (o, x) -> o.i = x) // deserializes a 32-bit int value and assigns it to i
                .read(Fields.TEXT).textTo(text); // deserializes a text value and assigns it to text
    }

    // Enum Fields implements WireKey to define keys used in serialization and deserialization
    enum Fields implements WireKey {
        B_FLAG, // Key for serializing/deserializing the boolean flag field
        S_NUM,  // Key for serializing/deserializing the short number field
        D_NUM,  // Key for serializing/deserializing the double number field
        L_NUM,  // Key for serializing/deserializing the long number field
        I_NUM,  // Key for serializing/deserializing the integer number field
        TEXT;   // Key for serializing/deserializing the text field

        // Override the code method to provide specific numerical identifiers for each key, using their ordinal number
        @Override
        public int code() {
            return ordinal();
        }
    }
}
