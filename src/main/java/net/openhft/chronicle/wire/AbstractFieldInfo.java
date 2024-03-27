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
 * The AbstractFieldInfo class serves as an abstract foundation for field information.
 * It implements the FieldInfo interface and provides basic implementations for some of the interface's methods.
 * This class contains the core properties of a field, including its name, type, and bracket type.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractFieldInfo implements FieldInfo {

    // The name of the field
    protected final String name;

    // The type of the field
    protected final Class type;

    // The bracket type associated with the field
    protected final BracketType bracketType;

    /**
     * Constructs a new AbstractFieldInfo with the provided type, bracket type, and name.
     *
     * @param type         The class type of the field
     * @param bracketType  The bracket type associated with the field
     * @param name         The name of the field
     */
    protected AbstractFieldInfo(Class type, BracketType bracketType, String name) {
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
