/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import java.util.TreeMap;

// Class ObjectWithTreeMap extends SelfDescribingMarshallable and contains a TreeMap
class ObjectWithTreeMap extends SelfDescribingMarshallable {
    // Declaration and instantiation of a TreeMap, mapping String keys to String values
    public final TreeMap<String, String> map = new TreeMap<>();
}
