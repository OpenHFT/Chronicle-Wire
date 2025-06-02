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

import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

// A2Class extends the functionality of AClass
public class A2Class extends AClass {

    // Constructor that initializes the A2Class by passing arguments to the superclass constructor
    public A2Class(int id, boolean flag, byte b, char ch, short s, int i, long l, float f, double d, String text) {
        super(id, flag, b, ch, s, i, l, f, d, text);
    }

    // Override the writeMarshallable method of the superclass
    @Override
    public void writeMarshallable(@NotNull WireOut out) {
        // Call the superclass implementation for writing marshallable data
        super.writeMarshallable(out);
    }

    // Override the readMarshallable method of the superclass
    @Override
    public void readMarshallable(@NotNull WireIn in) {
        // Call the superclass implementation for reading marshallable data
        super.readMarshallable(in);
    }
}
