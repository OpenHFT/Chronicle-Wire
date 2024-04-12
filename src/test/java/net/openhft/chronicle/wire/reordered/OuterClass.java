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
package net.openhft.chronicle.wire.reordered;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * OuterClass extends SelfDescribingMarshallable to facilitate serialization and deserialization.
 * It contains multiple lists of NestedClass objects and handles custom serialization logic.
 */
public class OuterClass extends SelfDescribingMarshallable {
    // Lists for maintaining instances of NestedClass and their free pools
    final List<NestedClass> listAFree = new ArrayList<>();
    final List<NestedClass> listA = new ArrayList<>();
    final List<NestedClass> listBFree = new ArrayList<>();
    final List<NestedClass> listB = new ArrayList<>();
    String text;
    WireType wireType;

    /**
     * Custom read logic for marshalling from Wire. It specifies how fields and lists
     * of nested classes are read from the wire.
     *
     * @param wire The WireIn instance to read the data from.
     * @throws IORuntimeException If an IO error occurs during reading.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "text").text(this, (t, v) -> t.text = v)
                .read(() -> "wireType").object(WireType.class, this, (t, v) -> t.wireType = v)
                .read(() -> "listA").sequence(this, (t, v) -> {
            t.clearListA();
            while (v.hasNextSequenceItem())
                v.marshallable(addListA());
        });

        wire.read(() -> "listB").sequence(this, (t, v) -> {
            t.clearListB();
            while (v.hasNextSequenceItem())
                v.marshallable(addListB());
        });
    }

    /**
     * Custom write logic for marshalling to Wire. It specifies how fields and lists
     * of nested classes are written to the wire.
     *
     * @param wire The WireOut instance to write the data to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "text").text(text)
                .write(() -> "wireType").text(wireType.name())
                .write(() -> "listA").sequence(this, (t, v) -> {
            for (NestedClass nc : t.getListA()) {
                v.marshallable(nc);
            }
        })
                .write(() -> "listB").sequence(this, (t, v) -> {
            for (NestedClass nc : t.getListB()) {
                v.marshallable(nc);
            }
        });

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    // Getter and setter methods for 'text' and 'wireType'
    public WireType getWireType() {
        return wireType;
    }

    public void setWireType(WireType wireType) {
        this.wireType = wireType;
    }

    /**
     * Getters for lists A and B, methods to clear lists, and methods to add elements to lists
     * leveraging the free pool pattern for efficient memory management.
     */
    @NotNull
    public List<NestedClass> getListA() {
        return listA;
    }

    public void clearListA() {
        listA.clear();
    }

    public NestedClass addListA() {
        if (listAFree.size() <= listA.size())
            listAFree.add(new NestedClass());
        NestedClass nc = listAFree.get(listA.size());
        listA.add(nc);
        return nc;
    }

    @NotNull
    public List<NestedClass> getListB() {
        return listB;
    }

    public void clearListB() {
        listB.clear();
    }

    public NestedClass addListB() {
        if (listBFree.size() <= listB.size())
            listBFree.add(new NestedClass());
        NestedClass nc = listBFree.get(listB.size());
        listB.add(nc);
        return nc;
    }

    /**
     * Overrides equals method for object comparison based on Marshallable logic.
     */
    @Override
    public boolean equals(Object o) {
        return Marshallable.$equals(this, o);
    }

    /**
     * Overrides hashCode method for object hashing based on Marshallable logic.
     */
    @Override
    public int hashCode() {
        return Marshallable.$hashCode(this);
    }

    /**
     * Provides a string representation of this class, including all its fields and lists.
     */
    @Override
    public String toString() {
        return Marshallable.$toString(this);
    }
}
