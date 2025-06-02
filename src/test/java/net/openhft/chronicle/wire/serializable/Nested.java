/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.wire.Wires;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.openhft.chronicle.wire.WireType.TEXT;

@SuppressWarnings("serial")
public class Nested implements Serializable, Validatable {
    private static final long serialVersionUID = 0L;
    // Fields of various types including a custom class, collections, and a map
    ScalarValues values;
    List<String> strings;
    Set<Integer> ints;
    Map<String, List<Double>> map;

    // Default constructor
    public Nested() {
    }

    // Constructor initializing all fields
    public Nested(ScalarValues values, List<String> strings, Set<Integer> ints, Map<String, List<Double>> map) {
        this.values = values;
        this.strings = strings;
        this.ints = ints;
        this.map = map;
    }

    // Overriding equals method for custom comparison logic
    @Override
    public boolean equals(Object obj) {
        // Check for instance equality and delegate to Wires utility for deep comparison
        return obj instanceof Nested && Wires.isEquals(this, obj);
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
        // Validate non-nullity of fields and delegate to validate method of 'values' if present
        ValidatableUtil.requireNonNull(values, "values");
        values.validate();
        ValidatableUtil.requireNonNull(strings, "strings");
        ValidatableUtil.requireNonNull(ints, "ints");
        ValidatableUtil.requireNonNull(map, "map");
    }
}
