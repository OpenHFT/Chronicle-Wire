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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Class representing an outer data structure that is self-describing.
class DMOuterClass extends SelfDescribingMarshallable {

    // Attributes of the outer class.
    String text;
    boolean b;
    byte bb;
    short s;
    float f;
    double d;
    long l;
    int i;

    // List of nested classes associated with the outer class.
    @NotNull
    List<DMNestedClass> nested = new ArrayList<>();

    // Map of string keys to nested class values.
    @NotNull
    Map<String, DMNestedClass> map = new LinkedHashMap<>();

    // Default constructor for the outer class.
    DMOuterClass() {

    }

    // Constructor to initialize the outer class with given values.
    public DMOuterClass(String text, boolean b, byte bb, double d, float f, int i, long l, short s) {
        this.text = text;
        this.b = b;
        this.bb = bb;
        this.d = d;
        this.f = f;
        this.i = i;
        this.l = l;
        this.s = s;
    }

    // Method to compare this object to another for equality.
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
