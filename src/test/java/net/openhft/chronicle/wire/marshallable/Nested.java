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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a nested structure containing various data types.
 * This class can be serialized/deserialized as it extends SelfDescribingMarshallable.
 */
public class Nested extends SelfDescribingMarshallable {

    // Holds scalar values
    ScalarValues values;

    // A list of strings
    List<String> strings;

    // A set of integers
    Set<Integer> ints;

    // A map with string keys and lists of doubles as values
    Map<String, List<Double>> map;

    // An array of strings
    String[] array;

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
