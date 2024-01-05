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
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The OuterClass class implements the Marshallable interface to allow serialization
 * and deserialization using Chronicle Wire. It contains lists of NestedClass objects
 * and some basic properties.
 */
public class OuterClass implements Marshallable {
    // Lists for storing NestedClass instances. Separate lists are maintained
    // for free and used instances to optimize object reuse.
    final List<NestedClass> listAFree = new ArrayList<>();
    final List<NestedClass> listA = new ArrayList<>();
    final List<NestedClass> listBFree = new ArrayList<>();
    final List<NestedClass> listB = new ArrayList<>();
    String text; // Text property of the class.
    WireType wireType; // The type of Wire (serialization format) used.

    /**
     * Deserializes data from Wire format into an OuterClass instance.
     *
     * @param wire The wire input to read data from.
     * @throws IORuntimeException If an I/O error occurs.
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
     * Serializes an OuterClass instance into Wire format.
     *
     * @param wire The wire output to write data to.
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

    // Getter and setter methods for the text property.
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the WireType of this instance.
     *
     * @return The WireType.
     */
    public WireType getWireType() {
        return wireType;
    }

    /**
     * Sets the WireType of this instance.
     *
     * @param wireType The new WireType.
     */
    public void setWireType(WireType wireType) {
        this.wireType = wireType;
    }

    /**
     * Gets the list of NestedClass instances in listA.
     *
     * @return A list of NestedClass instances.
     */
    @NotNull
    public List<NestedClass> getListA() {
        return listA;
    }

    /**
     * Clears the listA of NestedClass instances.
     */
    public void clearListA() {
        listA.clear();
    }

    /**
     * Adds a new NestedClass instance to listA and returns it.
     * Reuses instances from a free list to optimize object creation.
     *
     * @return The newly added NestedClass instance.
     */
    public NestedClass addListA() {
        if (listAFree.size() <= listA.size())
            listAFree.add(new NestedClass());
        NestedClass nc = listAFree.get(listA.size());
        listA.add(nc);
        return nc;
    }

    /**
     * Gets the list of NestedClass instances in listB.
     *
     * @return A list of NestedClass instances.
     */
    @NotNull
    public List<NestedClass> getListB() {
        return listB;
    }

    /**
     * Clears the listB of NestedClass instances.
     */
    public void clearListB() {
        listB.clear();
    }

    /**
     * Adds a new NestedClass instance to listB and returns it.
     * Reuses instances from a free list to optimize object creation.
     *
     * @return The newly added NestedClass instance.
     */
    public NestedClass addListB() {
        if (listBFree.size() <= listB.size())
            listBFree.add(new NestedClass());
        NestedClass nc = listBFree.get(listB.size());
        listB.add(nc);
        return nc;
    }

    /**
     * Provides a string representation of the OuterClass instance.
     *
     * @return A string describing the OuterClass instance.
     */
    @NotNull
    @Override
    public String toString() {
        return "OuterClass{" +
                "text='" + text + '\'' +
                ", wireType=" + wireType +
                ", listA=" + listA +
                ", listB=" + listB +
                '}';
    }
}
