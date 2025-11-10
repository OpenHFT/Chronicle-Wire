//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import static net.openhft.chronicle.wire.WireType.TEXT;
/**
 * The AbstractFieldInfo class serves as an abstract foundation for field information.
 * It implements the FieldInfo interface and provides basic implementations for some of the interface's methods.
 * This class contains the core properties of a field, including its name, type, and bracket type.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractFieldInfo implements FieldInfo {

    // The name of the field
    protected final String name;

    // The type of the field
    protected final Class<?> type;

    // The bracket type associated with the field
    protected final BracketType bracketType;

    /**
     * Constructs a new AbstractFieldInfo with the provided type, bracket type, and name.
     *
     * @param type         The class type of the field
     * @param bracketType  The bracket type associated with the field
     * @param name         The name of the field
     */
    protected AbstractFieldInfo(Class<?> type, BracketType bracketType, String name) {
        this.type = type;
        this.bracketType = bracketType;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<?> type() {
        return type;
    }

    @Override
    public BracketType bracketType() {
        return bracketType;
    }

    @Override
    public int hashCode() {
        return HashWire.hash32(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        return (this == obj || Wires.isEquals(this, obj));
    }

    @Override
    public String toString() {
        return TEXT.asString(this);
    }
}
