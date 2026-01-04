/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Tests for handling unsupported changes in the wire format.
public class UnsupportedChangesTest extends WireTestCommon {
    @BeforeEach
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip unsupported change checks");
    }

    // Test the behavior when trying to parse a scalar value as a Marshallable object
    @Test
    @DisplayName("Falls back when scalar replaces marshallable field")
    public void scalarToMarshallable() {
        Nested nested = Marshallable.fromString(Nested.class, "{\n" +
                "inner: 128\n" +
                "}\n");

        // Validate that the result is as expected when the parsing fails
        assertEquals("!net.openhft.chronicle.wire.UnsupportedChangesTest$Nested {\n" +
                "  inner: !!null \"\"\n" +
                "}\n", nested.toString(),
                "Nested string should reflect scalar to marshallable fallback");

        // Expect an exception indicating the inability to parse the scalar as a Marshallable
        expectException("Unable to parse field: inner, as a marshallable as it is 128");
    }

    // Method testing the scenario where a Marshallable object is being parsed as a scalar value.
    @Test
    @DisplayName("Falls back when marshallable replaces scalar double")
    public void marshallableToScalar() {
        // Skip this test if it's running on an ARM architecture.
        assumeFalse(Jvm.isArm(), "ARM platforms use different floating-point handling");

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
                "}\n", wrapper.toString(),
                "Wrapper string should reflect marshallable to scalar double fallback");

        // Assert that an exception is thrown indicating failure to parse a Marshallable as a scalar double.
        expectException("Unable to read {a=128, b=1.0} as a double.");
    }

    // Method testing the scenario where a Marshallable object is being parsed as a scalar long value.
    @Test
    @DisplayName("Falls back when marshallable replaces scalar long")
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
                "}\n", wrapper.toString(),
                "Wrapper string should reflect marshallable to scalar long fallback");

        // Expect an exception indicating the inability to parse the Marshallable as a scalar long
        expectException("Unable to read {a=128, b=1.0} as a long.");

    }

    // Method testing the scenario where a Marshallable object is being parsed as a scalar boolean value.
    @Test
    @DisplayName("Instantiates wrapper when marshallable replaces scalar boolean")
    public void marshallableToScalar3() {
        // Deserialize a BooleanWrapper object, providing a Marshallable instead of a scalar boolean for 'flag'.
        BooleanWrapper wrapper = Marshallable.fromString(BooleanWrapper.class, "{\n" +
                "flag: { a: 128, b: 1.0 },\n" +
                "second: 1234," +
                "}\n");

        // Validate that despite the unsupported change, the object 'wrapper' is still instantiated.
        assertNotNull(wrapper, "Wrapper instance should be created after marshallable to scalar boolean fallback");
    }

    // Definitions of the various Marshallable objects used in the above test methods.

    // Wrapper class containing double values, extending the SelfDescribingMarshallable class.
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class Wrapper extends SelfDescribingMarshallable {
        double pnl;
        double second;
    }

    // IntWrapper class containing long values, extending the SelfDescribingMarshallable class.
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class IntWrapper extends SelfDescribingMarshallable {
        long pnl;
        long second;
    }

    // BooleanWrapper class containing a boolean and a long value, extending the SelfDescribingMarshallable class.
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class BooleanWrapper extends SelfDescribingMarshallable {
        boolean flag;
        long second;
    }

    // Nested class containing an Inner object, extending the SelfDescribingMarshallable class.
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class Nested extends SelfDescribingMarshallable {
        Inner inner;
    }

    // Inner class containing a String value, extending the SelfDescribingMarshallable class.
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class Inner extends SelfDescribingMarshallable {
        String value;
    }
}
