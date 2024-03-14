/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.YamlWire;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * Parameterized test class extending WireTestCommon to validate the consistency
 * between YamlWire and TextWire in processing YAML formatted text.
 */
@RunWith(value = Parameterized.class)
public class YamlTextWireTest extends WireTestCommon {

    // Static block to register class alias for Fields
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Fields.class);
    }

    final String name;  // Name of the test scenario
    final String s;     // YAML formatted text to be tested

    /**
     * Constructor for parameterized test instances.
     *
     * @param name Name of the test scenario.
     * @param text YAML formatted text to be used in the test.
     */
    public YamlTextWireTest(String name, String text) {
        this.name = name;
        this.s = text;
    }

    /**
     * Provides the parameters for the parameterized test.
     * Each parameter set includes a scenario name and corresponding YAML formatted text.
     *
     * @return Collection of Object arrays, each containing a scenario name and YAML text.
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        // Stream of various YAML formatted texts to be tested
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

    /**
     * Tests the order and structure of fields processed by YamlWire and TextWire to ensure they are consistent.
     * The test verifies that both wires produce the same object representation from the given YAML text.
     */
    @Test
    public void orderTest() {
        // Parse the text using YamlWire and TextWire, and create Fields objects
        Fields yw = YamlWire.from(s).getValueIn().object(Fields.class);
        Fields tw = TextWire.from(s).getValueIn().object(Fields.class);

        // Assert that the objects created from both wires are equal
        assertEquals(tw, yw);
    }

    /**
     * Class representing a data structure with various fields.
     * Used for testing serialization and deserialization in YamlWire and TextWire.
     */
    @SuppressWarnings("unused")
    static class Fields extends SelfDescribingMarshallable {
        String a;
        Fields b;
        String c;
        Fields d;
    }
}
