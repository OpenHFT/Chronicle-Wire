/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
