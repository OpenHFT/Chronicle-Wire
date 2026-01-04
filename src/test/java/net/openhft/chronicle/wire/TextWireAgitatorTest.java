/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// This test suite is designed to test behaviors of the TextWire class
// based on random character changes, a method called "agitator testing".
public class TextWireAgitatorTest extends WireTestCommon {

    @Test
    @DisplayName("Lowercase class name should throw ClassNotFound")
    public void lowerCaseClassThrows() {
        assertThrows(ClassNotFoundRuntimeException.class, () -> {
            Wires.setGenerateTuples(false);
            Object o = Marshallable.fromString("!" + TextWireTest.MyDto.class.getName().toLowerCase() + " { }");
            fail("Class resolution should fail for " + o);
        }, "lowercase class name should throw ClassNotFoundRuntimeException");
    }

    @Test
    @DisplayName("Colon in list should throw IORuntimeException")
    public void colonInList() {
        assertThrows(IORuntimeException.class, () -> {
            assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip colon list test");

            TextWireTest.MyDto md = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireTest$MyDto {\n" +
                    "  strings: [\n" +
                    "  :\n" +
                    "  ]\n" +
                    "}\n");
            assertEquals("[hello]", md.toString(), "dto should parse with default list value");
        }, "colon in list should be rejected by the parser");
    }

    // Test to validate if an unexpected string value (i.e., not a boolean) assigned to a boolean field
    // will still be parsed without throwing an exception. The test is designed to produce a warning.
    @Test
    @DisplayName("Non-boolean string should parse with warning")
    public void notBoolean() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip non-boolean test");

        // produces a warning.
        MyFlagged mf = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireAgitatorTest$MyFlagged {\n" +
                "  flag: not-false\n" +
                "}");
        assertNotNull(mf, "marshallable should still be created for non-boolean flag");
    }

    // An inner static class designed to be marshallable, with a single boolean field named "flag".
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class MyFlagged extends SelfDescribingMarshallable {
        boolean flag;
    }
}
