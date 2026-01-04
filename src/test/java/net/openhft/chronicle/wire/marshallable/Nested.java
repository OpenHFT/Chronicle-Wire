/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a nested structure containing various data types.
 * This class can be serialised/deserialised as it extends SelfDescribingMarshallable.
 */
@SuppressWarnings("serial")
class Nested extends SelfDescribingMarshallable {

    // Holds scalar values
    private ScalarValues values;

    // A list of strings
    private List<String> strings;

    // A set of integers
    private Set<Integer> ints;

    // A map with string keys and lists of doubles as values
    private Map<String, List<Double>> map;

    // An array of strings
    private String[] array;

    public Nested() {
    }

    /**
     * Parameterised constructor to initialise the object with specified values.
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

    int fieldFingerprint() {
        return Objects.hash(values, strings, ints, map, Arrays.hashCode(array));
    }
}
