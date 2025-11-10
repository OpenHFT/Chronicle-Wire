//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

// This test suite is designed to test behaviors of the TextWire class
// based on random character changes, a method called "agitator testing".
public class TextWireAgitatorTest extends WireTestCommon {

    @Test(expected = ClassNotFoundRuntimeException.class)
    public void lowerCaseClassThrows() {
        Wires.GENERATE_TUPLES = false;
        Object o = Marshallable.fromString("!" + TextWireTest.MyDto.class.getName().toLowerCase() + " { }");
        fail("" + o);
    }

    @Test(expected = IORuntimeException.class)
    public void colonInList() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        TextWireTest.MyDto md = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireTest$MyDto {\n" +
                "  strings: [\n" +
                "  :\n" +
                "  ]\n" +
                "}\n");
        assertEquals("[hello]", md.toString());
    }

    // Test to validate if an unexpected string value (i.e., not a boolean) assigned to a boolean field
    // will still be parsed without throwing an exception. The test is designed to produce a warning.
    @Test
    public void notBoolean() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // produces a warning.
        MyFlagged mf = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireAgitatorTest$MyFlagged {\n" +
                "  flag: not-false\n" +
                "}");
        assertNotNull(mf);
    }

    // An inner static class designed to be marshallable, with a single boolean field named "flag".
    private static class MyFlagged extends SelfDescribingMarshallable {
        boolean flag;
    }
}
