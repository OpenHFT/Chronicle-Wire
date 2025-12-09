/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;

/**
 * The OuterClass class implements the Marshallable interface to allow serialization
 * and deserialization using Chronicle Wire. It contains lists of NestedClass objects
 * and some basic properties.
 */
@SuppressWarnings({"deprecation", "removal"})
class OuterClass extends AbstractPooledOuterClass<NestedClass> {

    public OuterClass() {
        super(NestedClass::new);
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
                "text='" + getText() + '\'' +
                ", wireType=" + getWireType() +
                ", listA=" + getListA() +
                ", listB=" + getListB() +
                '}';
    }
}
