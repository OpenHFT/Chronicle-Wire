/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;

// Tests for handling unsupported changes in the wire format.
public class UnsupportedChangesTest extends WireTestCommon {

    // Test the behavior when trying to parse a scalar value as a Marshallable object
    @Test
    public void scalarToMarshallable() {
        Nested nested = Marshallable.fromString(Nested.class, "{\n" +
                "inner: 128\n" +
                "}\n");

        // Validate that the result is as expected when the parsing fails
        assertEquals("!net.openhft.chronicle.wire.UnsupportedChangesTest$Nested {\n" +
                "  inner: !!null \"\"\n" +
                "}\n", nested.toString());

        // Expect an exception indicating the inability to parse the scalar as a Marshallable
        expectException("Unable to parse field: inner, as a marshallable as it is 128");
    }

    // Test the behavior when trying to parse a Marshallable object as a scalar
    @Test
    public void marshallableToScalar() {
        assumeFalse(Jvm.isArm()); // Ensure the test isn't running on an ARM architecture

        Wrapper wrapper = Marshallable.fromString(Wrapper.class, "{\n" +
                "pnl: { a: 128, b: 1.0 },\n" +
                "second: 123.4," +
                "}\n");

        // Validate that the result is as expected when the parsing fails
        assertEquals("!net.openhft.chronicle.wire.UnsupportedChangesTest$Wrapper {\n" +
                "  pnl: 0.0,\n" +
                "  second: 123.4\n" +
                "}\n", wrapper.toString());

        // Expect an exception indicating the inability to parse the Marshallable as a scalar double
        expectException("Unable to read {a=128, b=1.0} as a double.");

    }

    // Test the behavior when trying to parse a Marshallable object as a scalar (specifically, a long)
    @Test
    public void marshallableToScala2r() {
        IntWrapper wrapper = Marshallable.fromString(IntWrapper.class, "{\n" +
                "pnl: { a: 128, b: 1.0 },\n" +
                "second: 1234," +
                "}\n");

        // Validate that the result is as expected when the parsing fails
        assertEquals("!net.openhft.chronicle.wire.UnsupportedChangesTest$IntWrapper {\n" +
                "  pnl: 0,\n" +
                "  second: 1234\n" +
                "}\n", wrapper.toString());

        // Expect an exception indicating the inability to parse the Marshallable as a scalar long
        expectException("Unable to read {a=128, b=1.0} as a long.");

    }

    // Test the behavior when trying to parse a Marshallable object as a boolean scalar
    @Test
    public void marshallableToScalar3() {
        // This test case checks if parsing produces a warning rather than an exception
        BooleanWrapper wrapper = Marshallable.fromString(BooleanWrapper.class, "{\n" +
                "flag: { a: 128, b: 1.0 },\n" +
                "second: 1234," +
                "}\n");

        // Validate that the wrapper object was successfully created
        assertNotNull(wrapper);
    }

    static class Wrapper extends SelfDescribingMarshallable {
        double pnl;
        double second;
    }

    static class IntWrapper extends SelfDescribingMarshallable {
        long pnl;
        long second;
    }

    static class BooleanWrapper extends SelfDescribingMarshallable {
        boolean flag;
        long second;
    }

    static class Nested extends SelfDescribingMarshallable {
        Inner inner;
    }

    static class Inner extends SelfDescribingMarshallable {
        String value;
    }
}
