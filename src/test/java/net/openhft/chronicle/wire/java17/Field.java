/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.java17;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a field with the ability to describe its structure and contents.
 * Inherits the capabilities of SelfDescribingMarshallable to provide
 * self-describing marshalling and unmarshalling.
 */
class Field extends SelfDescribingMarshallable {

    // A map to maintain the required status for various field names
    private final Map<String, Required> required = new HashMap<>();

    /**
     * Sets the required status for a given field name.
     *
     * @param name     The name of the field.
     * @param required The required status of the field.
     */
    void required(String name, Required required) {
        this.required.put(name, required);
    }
}
