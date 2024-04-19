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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(value = Parameterized.class)
public class SerializableWireTest extends WireTestCommon {
    // Wire type for the test
    private final WireType wireType;

    // Serializable object to be tested
    private final Serializable m;

    // Indicates whether to expect an InvalidMarshallableException
    private final boolean ime;

    // Constructor initializing wire type, serializable object, and exception expectation flag
    public SerializableWireTest(WireType wireType, Serializable m, boolean ime) {
        this.wireType = wireType;
        this.m = m;
        this.ime = ime;
    }

    // Parameterized tests with various combinations of wire types and serializable objects
    @NotNull
    @Parameterized.Parameters(name = "wt: {0}, object: {1}, IME: {2}") // toString() implicility called here
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        // Wire types for testing
        @NotNull WireType[] wireTypes = {WireType.TEXT /*, WireType.YAML_ONLY, WireType.BINARY*/};
        // Serializable objects for testing
        @NotNull Serializable[] objects = {
            // Various serializable objects to test
            new Nested(),
            new ScalarValues(),
            new Nested(new ScalarValues(), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap()),
            new Nested(new ScalarValues(1), null, Collections.emptySet(), Collections.emptyMap()),
            new Nested(new ScalarValues(1), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap()),
            new ScalarValues(1),
            new ScalarValues(10)
        };
        // Generate combinations of wire types and serializable objects
        for (WireType wt : wireTypes) {
            for (Serializable object : objects) {
                @NotNull Object[] test = {wt, object, list.size() < 4};
                list.add(test);
            }
        }
        return list;
    }

    // Test method to write and read serializable objects using different wire types
    @SuppressWarnings("rawtypes")
    @Test
    public void writeMarshallable() {
        // Ignore exceptions for certain test cases
        if (ime) // TODO Fix to be expected
            ignoreException(ek -> ek.throwable instanceof InvalidMarshallableException, "IME");
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            // Apply wire type to bytes
            Wire wire = wireType.apply(bytes);

            // Write the serializable object to wire
            wire.getValueOut().object(m);
            // System.out.println(wire);

            // Read the object back from wire
            @Nullable Object m2 = wire.getValueIn().object();
            // Assert that the written and read objects are equal
            assertEquals(m, m2);
            // Fail if an exception was expected but not thrown
            if (ime)
                fail();
        } catch (InvalidMarshallableException e) {
            // Throw exception if it was not expected
            if (!ime)
                throw e;
        } finally {
            // Release bytes
            bytes.releaseLast();
        }
    }
}
