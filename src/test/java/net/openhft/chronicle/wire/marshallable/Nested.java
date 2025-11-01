/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a nested structure containing various data types.
 * This class can be serialized/deserialized as it extends SelfDescribingMarshallable.
 */
@SuppressWarnings("serial")
public class Nested extends SelfDescribingMarshallable {

    // Holds scalar values
    public ScalarValues values;

    // A list of strings
    public List<String> strings;

    // A set of integers
    public Set<Integer> ints;

    // A map with string keys and lists of doubles as values
    public Map<String, List<Double>> map;

    // An array of strings
    public String[] array;

    /**
     * Default constructor.
     */
    public Nested() {
    }

    /**
     * Parameterized constructor to initialize the object with specified values.
     *
     * @param values   Scalar values
     * @param strings  A list of strings
     * @param ints     A set of integers
     * @param map      A map with string keys and lists of doubles as values
     * @param array    An array of strings
     */
    public Nested(ScalarValues values, List<String> strings, Set<Integer> ints, Map<String, List<Double>> map, String[] array) {
        this.values = values;
        this.strings = strings;
        this.ints = ints;
        this.map = map;
        this.array = array;
    }
}
