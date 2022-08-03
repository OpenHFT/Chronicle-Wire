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

    final String input;

    public TextSkipValueTest(String input) {
        this.input = input;
    }

    @NotNull
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        List<Object[]> list = new ArrayList<>();
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
            list.add(new Object[]{s});
        }
        return list;
    }

    @Test
    public void skipValue() {
        Wire wire = TextWire.from(input);
        wire.getValueIn()
                .skipValue();
        wire.consumePadding();
        assertEquals("end", wire.bytes().toString());
    }
}
