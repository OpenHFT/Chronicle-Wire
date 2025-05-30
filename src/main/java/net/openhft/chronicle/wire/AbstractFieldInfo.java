/*
 * Copyright 2016-2025 chronicle.software
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

import static net.openhft.chronicle.wire.WireType.TEXT;
/**
 * The AbstractFieldInfo class serves as an abstract foundation for field information.
 * It implements the FieldInfo interface and provides basic implementations for some of the interface's methods.
 * This class contains the core properties of a field, including its name, type, and bracket type.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractFieldInfo implements FieldInfo {

    /**
     * The name of the field this {@code FieldInfo} instance represents.
     */
    protected final String name;

    /**
     * The Java type declared for the field.
     */
    protected final Class<?> type;

    /**
     * How the field is bracketed when serialised.
     */
    protected final BracketType bracketType;

    /**
     * Creates a description of a field.
     *
     * @param fieldType   class used for marshalling
     * @param bracketType placement of start and end brackets in the wire text
     * @param name        identifier of the field
     */
    protected AbstractFieldInfo(Class<?> fieldType, BracketType bracketType, String name) {
        this.type = fieldType;
        this.bracketType = bracketType;
        this.name = name;
    }

    /**
     * Returns the field name.
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the declared type.
     */
    @Override
    public Class<?> type() {
        return type;
    }

    /**
     * Returns the bracket placement style.
     */
    @Override
    public BracketType bracketType() {
        return bracketType;
    }

    /**
     * Generates a 32-bit hash of the serialised form.
     */
    @Override
    public int hashCode() {
        return HashWire.hash32(this);
    }

    /**
     * Compares the serialised content for equality.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        return (this == obj || Wires.isEquals(this, obj));
    }

    /**
     * Returns a YAML style representation of this field information.
     */
    @Override
    public String toString() {
        return TEXT.asString(this);
    }
}
