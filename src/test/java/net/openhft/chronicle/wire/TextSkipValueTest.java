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

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class TextSkipValueTest extends WireTestCommon {

    // This will store the input string for each run of the test.
    final String input;

    // Constructor that initializes the 'input' member variable.
    public TextSkipValueTest(String input) {
        this.input = input;
    }

    // This method provides the parameters (inputs) for the test.
    @NotNull
    @Parameterized.Parameters
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
    @Test
    public void skipValue() {
        // Create a TextWire from the input string.
        Wire wire = TextWire.from(input);
        // Try to skip the value in the input string.
        wire.getValueIn()
            .skipValue();
        // Consume any padding in the wire.
        wire.consumePadding();
        // After skipping the value and consuming padding,
        // the next value in the wire should be "end". Assert this expectation.
        assertEquals("end", wire.bytes().toString());
    }
}
