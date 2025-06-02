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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

// Runner to enable parameterized tests for the MarshallableWireTest class
@RunWith(value = Parameterized.class)
public class MarshallableWireTest extends WireTestCommon {

    // Type of wire to be tested
    private final WireType wireType;

    // Marshallable object for test scenarios
    private final Marshallable m;

    // Constructor initializes the WireType and Marshallable object for the test scenario
    public MarshallableWireTest(WireType wireType, Marshallable m) {
        this.wireType = wireType;
        this.m = m;
    }

    // Provide test data combinations for the parameterized test
    @NotNull
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {

        // Collection to store different test combinations
        @NotNull List<Object[]> list = new ArrayList<>();

        // Different WireTypes for test scenarios
        @NotNull WireType[] wireTypes = {WireType.TEXT, WireType.YAML_ONLY, WireType.BINARY};

        // Sample Marshallable objects for the test scenarios
        @NotNull Marshallable[] objects = {
                new Nested(),
                new Nested(new ScalarValues(), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap(), new String[0]),
                new ScalarValues(),
                new ScalarValues(1),
                new ScalarValues(10)
        };

        // Populate the test combinations list using each WireType with each Marshallable object
        for (WireType wt : wireTypes) {
            for (Marshallable object : objects) {
                @NotNull Object[] test = {wt, object};
                list.add(test);
            }
        }

        return list;
    }

    // Test method to write a Marshallable object to a wire and then read it back
    @SuppressWarnings("rawtypes")
    @Test
    public void writeMarshallable() {

        // Allocate memory for writing data
        Bytes<?> bytes = Bytes.elasticByteBuffer();

        // Apply the specific WireType to the allocated memory
        Wire wire = wireType.apply(bytes);

        // Write the Marshallable object to the wire
        wire.getValueOut().object(m);

        // Uncomment to print wire contents for debug purposes
        // System.out.println(wire);

        // Read back the object from the wire
        @Nullable Object m2 = wire.getValueIn().object();

        // Assert that the written and read objects are the same
        if (!m.equals(m2))
            assertEquals(m, m2);

        // Release the allocated memory
        bytes.releaseLast();
    }
}
