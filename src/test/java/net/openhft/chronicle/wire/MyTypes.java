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

// Class representing a collection of various data types, extending SelfDescribingMarshallable for serialization support
class MyTypes extends SelfDescribingMarshallable {

    // StringBuilder instance to hold and manipulate string data efficiently
    final StringBuilder text = new StringBuilder();

    // Fields representing various data types
    boolean flag;  // Field representing a boolean flag
    byte b;        // Field to store a byte value
    short s;       // Field to store a short value
    char ch;       // Field to store a char value
    int i;         // Field to store an integer value
    float f;       // Field to store a float value
    double d;      // Field to store a double value
    long l;        // Field to store a long value

    // Method to update and return the current instance for a boolean flag
    public MyTypes flag(boolean b) {
        this.flag = b;
        return this;
    }

    // Getter for the boolean flag
    public boolean flag() {
        return this.flag;
    }

    // Getter for the byte value
    public byte b() {
        return b;
    }

    // Method to update and return the current instance for a byte value
    public MyTypes b(byte b) {
        this.b = b;
        return this;
    }

    // Method to update and return the current instance for a short value
    public MyTypes s(short s) {
        this.s = s;
        return this;
    }

    // Getter for the short value
    public short s() {
        return this.s;
    }

    // Getter for the char value
    public char ch() {
        return ch;
    }

    // Method to update and return the current instance for a char value
    public MyTypes ch(char ch) {
        this.ch = ch;
        return this;
    }

    // Getter for the float value
    public float f() {
        return f;
    }

    // Method to update and return the current instance for a float value
    public MyTypes f(float f) {
        this.f = f;
        return this;
    }

    // Method to update and return the current instance for a double value
    public MyTypes d(double d) {
        this.d = d;
        return this;
    }

    // Getter for the double value
    public double d() {
        return this.d;
    }

    // Method to update and return the current instance for a long value
    public MyTypes l(long l) {
        this.l = l;
        return this;
    }

    // Getter for the long value
    public long l() {
        return this.l;
    }

    // Method to update and return the current instance for an int value
    public MyTypes i(int i) {
        this.i = i;
        return this;
    }

    // Getter for the int value
    public int i() {
        return this.i;
    }

    // Getter for the StringBuilder 'text'
    @NotNull
    public StringBuilder text() {
        return text;
    }

    // Method to update the text field and return the current instance, while allowing for efficient string concatenation
    public MyTypes text(CharSequence value) {
        text.setLength(0);  // Resetting the length of the StringBuilder to 0
        text.append(value); // Appending the new value
        return this;        // Returning the current instance for chaining
    }
}
