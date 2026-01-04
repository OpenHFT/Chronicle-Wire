/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
class TextSkipValueTest extends WireTestCommon {

    // This will store the input string for each run of the test.
    private String input;

    // Constructor that initializes the 'input' member variable.
    void initTextSkipValueTest(String input) {
        this.input = input;
    }

    // This method provides the parameters (inputs) for the test.
    @NotNull
    public static Collection<Object[]> combinations() {
        List<Object[]> list = new ArrayList<>();
        // Here are the different inputs we are testing:
        for (String s : new String[]{
                "data: {\n" +
                        "  a: 123\n" +
                        "  b: 1.1\n" +
                        "  c: \"hi\"\n" +
                        "},\n" +
                        "end",
                "cluster1: {\n" +
                        "  context:  !EngineClusterContext  { }\n" +
                        "  host1: {\n" +
                        "     hostId: 1\n" +
                        "  },\n" +
                        "},\n" +
                        "end",
                "? { MyField: parent }: {\n" +
                        "  ? !sometype { MyField: key1 }: value1,\n" +
                        "  ? !sometype { MyField: key2 }: value2\n" +
                        "},\n" +
                        "end",
                "example: {\n" +
                        "  ? { MyField: aKey }: { MyField: aValue },\n" +
                        "  ? { MyField: aKey2 }: { MyField: aValue2 }\n" +
                        "},\n" +
                        "end",
                "a: [ !Type { b: 'a, a', bb: aa }, !Type { c: 1.0, d: x } ]\n" +
                        "end",
                "a: [ { b: 'a, a', bb: aa }, { c: 1.0, d: x } ]\n" +
                        "end",
                "a: [ { b: a, bb: aa }, { c: 1.0, d: x } ]\n" +
                        "end",
                "a: [ { b: a }, { c: 1.0 } ]\n" +
                        "end",
                "a: { b: a },\n" +
                        "end",
                "a: [ a ],\n" +
                        "end",
                "a: a,\n" +
                        "end",
                "a,\n" +
                        "end"
        }) {
            // Add each input string wrapped in an Object array to the list.
            list.add(new Object[]{s});
        }
        return list;
    }

    // This is the actual test that will run once for each input string provided by combinations() method.
    @DisplayName("Skips value and leaves end marker")
    @MethodSource("combinations")
    @ParameterizedTest
    void skipValue(String input) {
        initTextSkipValueTest(input);
        // Create a TextWire from the input string.
        Wire wire = TextWire.from(input);
        // Try to skip the value in the input string.
        wire.getValueIn()
            .skipValue();
        // Consume any padding in the wire.
        wire.consumePadding();
        // After skipping the value and consuming padding,
        // the next value in the wire should be "end". Assert this expectation.
        assertEquals("end", wire.bytes().toString(),
                "skipValue should leave the end marker for input=" + input);
    }
}
