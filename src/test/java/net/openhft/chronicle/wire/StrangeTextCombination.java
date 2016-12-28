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

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Rob Austin.
 */
@RunWith(value = Parameterized.class)
public class StrangeTextCombination {
    private WireType wireType;
    private Bytes bytes;

    public StrangeTextCombination(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{WireType.TEXT}
                , new Object[]{WireType.BINARY}
                , new Object[]{WireType.RAW}
        );
    }

    @Test
    public void testPrependedSpace() {
        @NotNull final String prependedSpace = " hello world";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(prependedSpace);

        Assert.assertEquals(prependedSpace, wire.read().text());

    }

    @Test
    public void testPostpendedSpace() {
        @NotNull final String postpendedSpace = "hello world ";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(postpendedSpace);

        Assert.assertEquals(postpendedSpace, wire.read().text());
    }

    @Test
    public void testSlashQuoteTest() {
        @NotNull final String expected = "\\\" ";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testYaml() {
        @NotNull final String expected = "!String{chars:hello world}";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testString() {
        @NotNull final String expected = "!String";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testBinary() {
        @NotNull final String expected = "!binary";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testBinaryWithSpace() {
        @NotNull final String expected = " !binary";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testEmpty() {
        @NotNull final String expected = "";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testNull() {
        @Nullable final String expected = null;
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testNewLine() {
        @NotNull final String expected = "\n";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testUnicode() {
        @NotNull final String expected = "\u0000";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @Test
    public void testXML() {
        @NotNull final String expected = "<name>rob austin</name>";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        Assert.assertEquals(expected, wire.read().text());
    }

    @NotNull
    private Wire wireFactory() {
        bytes = Bytes.allocateElasticDirect();
        return wireType.apply(bytes);
    }
}
