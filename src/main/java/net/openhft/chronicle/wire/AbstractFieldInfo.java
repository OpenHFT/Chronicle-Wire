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

import static net.openhft.chronicle.wire.WireType.TEXT;
/**
 * Abstract foundation for {@link FieldInfo} implementations.  It stores the
 * immutable characteristics of a field and supplies basic implementations of
 * common methods.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractFieldInfo implements FieldInfo {

    /** name of the field this instance represents */
    protected final String name;

    /** declared type of the field */
    protected final Class<?> type;

    /** how the field is represented on the wire */
    protected final BracketType bracketType;

    /**
     * Create an instance describing the given field.
     *
     * @param type        declared type of the field
     * @param bracketType wire representation style
     * @param name        name of the field
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
    /**
     * Compute a hash code based on the field metadata.
     */
    public int hashCode() {
        return HashWire.hash32(this);
    }

    @Override
    /**
     * Equality based on the YAML representation of the field info.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        return (this == obj || Wires.isEquals(this, obj));
    }

    @Override
    /**
     * Return a YAML representation of this field info.
     */
    public String toString() {
        return TEXT.asString(this);
    }
}
