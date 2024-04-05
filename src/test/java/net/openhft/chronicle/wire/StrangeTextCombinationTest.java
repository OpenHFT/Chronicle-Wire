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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

// A parameterized test class that tests various string serialization behaviors for different WireTypes.
@RunWith(value = Parameterized.class)
public class StrangeTextCombinationTest extends net.openhft.chronicle.wire.WireTestCommon {
    private WireType wireType;
    @SuppressWarnings("rawtypes")
    private Bytes<?> bytes;

    // Constructor initializes the WireType for this instance of the test.
    public StrangeTextCombinationTest(WireType wireType) {
        this.wireType = wireType;
    }

    // Parameterized test data. Each WireType will be tested.
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] list = {
                {WireType.BINARY},
                {WireType.RAW},
                {WireType.TEXT},
                {WireType.JSON}
        };
        return Arrays.asList(list);
    }

    // Tests that a string with a leading space is serialized and deserialized correctly.
    @Test
    public void testPrependedSpace() {
        @NotNull final String prependedSpace = " hello world";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(prependedSpace);

        Assert.assertEquals(prependedSpace, wire.read().text());

    }

    // Tests that a string with a trailing space is serialized and deserialized correctly.
    @Test
    public void testPostpendedSpace() {
        @NotNull final String postpendedSpace = "hello world ";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(postpendedSpace);

        Assert.assertEquals(postpendedSpace, wire.read().text());
    }

    // Tests that a string with escape characters is serialized and deserialized correctly.
    @Test
    public void testSlashQuoteTest() {
        @NotNull final String expected = "\\\" ";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Tests that a string with specific YAML syntax is serialized and deserialized correctly.
    @Test
    public void testYaml() {
        @NotNull final String expected = "!String{chars:hello world}";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Test class to ensure various string values are correctly serialized and deserialized using
    // Chronicle-Wire. The class contains multiple test cases, each focused on a specific string value
    // or format.

    // Tests that a string "!String" is serialized and deserialized correctly.
    @Test
    public void testString() {
        @NotNull final String expected = "!String";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Tests that a string "!binary" is serialized and deserialized correctly.
    @Test
    public void testBinary() {
        @NotNull final String expected = "!binary";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Tests that a string " !binary" with a leading space is serialized and deserialized correctly.
    @Test
    public void testBinaryWithSpace() {
        @NotNull final String expected = " !binary";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Tests that an empty string is serialized and deserialized correctly.
    @Test
    public void testEmpty() {
        @NotNull final String expected = "";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Tests that a null string value is serialized and deserialized correctly.
    @Test
    public void testNull() {
        @Nullable final String expected = null;
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Tests that a string with a newline character is serialized and deserialized correctly.
    @Test
    public void testNewLine() {
        @NotNull final String expected = "\n";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Tests that a string with a Unicode null character is serialized and deserialized correctly.
    @Test
    public void testUnicode() {
        @NotNull final String expected = "\u0000";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Tests that an XML formatted string is serialized and deserialized correctly.
    @Test
    public void testXML() {
        @NotNull final String expected = "<name>rob austin</name>";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    // Helper method to create a new Wire instance using the given WireType.
    // The Wire is backed by an on-heap elastic byte buffer.
    @NotNull
    private Wire wireFactory() {
        bytes = Bytes.allocateElasticOnHeap(64);
        return wireType.apply(bytes);
    }
}
