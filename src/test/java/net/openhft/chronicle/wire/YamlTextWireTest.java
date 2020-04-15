/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class YamlTextWireTest {

    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Fields.class);
    }

    final String name;
    final String s;

    public YamlTextWireTest(String name, String text) {
        this.name = name;
        this.s = text;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Stream.of(
                "{}",
                "a: hi",
                "b: {}",
                "c: hi",
                "d: {}",
                "a: hi,\n" +
                        "b: {}",
                "a: hi,\n" +
                        "c: hi",
                "a: hi,\n" +
                        "b: {},\n" +
                        "c: hi,",
                "a: hi,\n" +
                        "c: hi,\n" +
                        "d: {},",
                "b: {},\n" +
                        "d: {}",
                "b: {},\n" +
                        "c: hi,\n",
                "a: hi,\n" +
                        "d: {},\n" +
                        "c: hi,\n" +
                        "b: {}",
                "c: hi,\n" +
                        "b: {},\n" +
                        "a: hi,\n" +
                        "d: {}",
                "a: hi,\n" +
                        "b: {},\n" +
                        "c: hi,\n" +
                        "d: {}",
                "e: [ hi ],\n" +
                        "f: { hi: there },\n" +
                        "b: { a: hi },\n" +
                        "c: hi,\n" +
                        "d: { c: bye }"
        )
                .map(o -> new Object[]{o.replaceAll("\n", " "), o})
                .collect(Collectors.toList());
    }

    @Test
    public void orderTest() {
        Fields yw = YamlWire.from(s).getValueIn()
                .object(Fields.class);
        Fields tw = TextWire.from(s).getValueIn()
                .object(Fields.class);
        assertEquals(tw, yw);
    }

    @SuppressWarnings("unused")
    static class Fields extends SelfDescribingMarshallable {
        String a;
        Fields b;
        String c;
        Fields d;
    }
}
