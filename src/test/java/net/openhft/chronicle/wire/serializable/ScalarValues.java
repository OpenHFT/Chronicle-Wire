/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.wire.FieldInfo;
import net.openhft.chronicle.wire.Wires;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

import static net.openhft.chronicle.wire.WireType.TEXT;

@SuppressWarnings({"rawtypes","deprecation"})
@UsedViaReflection
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS", justification = "Serializable variant keeps the name for compatibility with existing uses.")
public class ScalarValues extends net.openhft.chronicle.wire.marshallable.ScalarValues implements Serializable, Validatable {
    private static final long serialVersionUID = 0L;

    public static ScalarValues fromMarshallable(int i) {
        return new ScalarValues(i);
    }

    public ScalarValues() {
        super();
    }

    public ScalarValues(int i) {
        super(i);
    }

    // Overriding equals method for custom comparison logic
    @Override
    public boolean equals(Object obj) {
        // Check for instance equality and delegate to Wires utility for deep comparison
        return obj instanceof ScalarValues && Wires.isEquals(this, obj);
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    // Overriding toString method to provide a string representation of the object
    @Override
    public String toString() {
        // Utilize TEXT Wire format for string representation
        return TEXT.asString(this);
    }

    // Implementing validate method from Validatable interface
    @Override
    public void validate() throws InvalidMarshallableException {
        // Validate all non-primitive fields to ensure they are not null
        for (FieldInfo fieldInfo : Wires.fieldInfos(getClass())) {
            if (!fieldInfo.type().isPrimitive()) {
                String name = fieldInfo.name();
                Object o = fieldInfo.get(this);
                ValidatableUtil.requireNonNull(o, name);
            }
        }
    }
}
