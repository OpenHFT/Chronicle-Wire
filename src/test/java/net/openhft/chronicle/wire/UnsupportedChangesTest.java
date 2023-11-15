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

    // Method testing the scenario where a Marshallable object is being parsed as a scalar value.
    @Test
    public void marshallableToScalar() {
        // Skip this test if it's running on an ARM architecture.
        assumeFalse(Jvm.isArm());

        // Deserialize a Wrapper object, providing a Marshallable instead of a scalar double for 'pnl'.
        Wrapper wrapper = Marshallable.fromString(Wrapper.class, "{\n" +
                "pnl: { a: 128, b: 1.0 },\n" +
                "second: 123.4," +
                "}\n");

        // Validate that the deserialization failure results in the expected string representation,
        // showing 'pnl' as zero.
        assertEquals("!net.openhft.chronicle.wire.UnsupportedChangesTest$Wrapper {\n" +
                "  pnl: 0.0,\n" +
                "  second: 123.4\n" +
                "}\n", wrapper.toString());

        // Assert that an exception is thrown indicating failure to parse a Marshallable as a scalar double.
        expectException("Unable to read {a=128, b=1.0} as a double.");
    }

    // Method testing the scenario where a Marshallable object is being parsed as a scalar long value.
    @Test
    public void marshallableToScala2r() {
        // Deserialize an IntWrapper object, providing a Marshallable instead of a scalar long for 'pnl'.
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

    // Method testing the scenario where a Marshallable object is being parsed as a scalar boolean value.
    @Test
    public void marshallableToScalar3() {
        // Deserialize a BooleanWrapper object, providing a Marshallable instead of a scalar boolean for 'flag'.
        BooleanWrapper wrapper = Marshallable.fromString(BooleanWrapper.class, "{\n" +
                "flag: { a: 128, b: 1.0 },\n" +
                "second: 1234," +
                "}\n");

        // Validate that despite the unsupported change, the object 'wrapper' is still instantiated.
        assertNotNull(wrapper);
    }

    // Definitions of the various Marshallable objects used in the above test methods.

    // Wrapper class containing double values, extending the SelfDescribingMarshallable class.
    static class Wrapper extends SelfDescribingMarshallable {
        double pnl;
        double second;
    }

    // IntWrapper class containing long values, extending the SelfDescribingMarshallable class.
    static class IntWrapper extends SelfDescribingMarshallable {
        long pnl;
        long second;
    }

    // BooleanWrapper class containing a boolean and a long value, extending the SelfDescribingMarshallable class.
    static class BooleanWrapper extends SelfDescribingMarshallable {
        boolean flag;
        long second;
    }

    // Nested class containing an Inner object, extending the SelfDescribingMarshallable class.
    static class Nested extends SelfDescribingMarshallable {
        Inner inner;
    }

    // Inner class containing a String value, extending the SelfDescribingMarshallable class.
    static class Inner extends SelfDescribingMarshallable {
        String value;
    }
}
