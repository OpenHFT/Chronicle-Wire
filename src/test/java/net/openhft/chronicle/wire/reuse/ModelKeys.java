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

import net.openhft.chronicle.wire.WireKey;

/**
 * ModelKeys is an enumeration representing different keys used in a model.
 * It implements the WireKey interface to facilitate serialization and deserialization
 * with Chronicle Wire, where each enum constant is a key in the wire format.
 * The `code` method, defined in the WireKey interface, is overridden to return
 * the ordinal of the enum constant, providing a unique code for each key.
 */
enum ModelKeys implements WireKey {
    // Enum constants representing different keys in the model.
    id,             // Represents an identifier
    revision,       // Represents a revision number
    properties,     // Represents a set of properties
    collections,    // Represents a collection of items
    reference,      // Represents a reference to another entity
    path,           // Represents a path
    name,           // Represents a name
    value,          // Represents a value
    key;            // Represents a key

    /**
     * Returns the code associated with each key. The code is the ordinal
     * of the enum constant, ensuring a unique code for each key.
     *
     * @return The ordinal of the enum constant.
     */
    @Override
    public int code() {
        return ordinal();
    }
}
