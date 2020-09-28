/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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

@SuppressWarnings("rawtypes")
public abstract class AbstractFieldInfo implements FieldInfo {
    protected final String name;
    protected final Class type;
    protected final BracketType bracketType;

    public AbstractFieldInfo(Class type, BracketType bracketType, String name) {
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
        return (this == obj || Wires.isEquals(this, obj));
    }

    @Override
    public String toString() {
        return TEXT.asString(this);
    }
}
