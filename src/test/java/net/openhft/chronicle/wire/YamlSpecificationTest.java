/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static net.openhft.chronicle.wire.WireType.YAML;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
@RunWith(Parameterized.class)
public class YamlSpecificationTest extends WireTestCommon {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(String.class, "something");
        ClassAliasPool.CLASS_ALIASES.addAlias(Circle.class, "circle");
        ClassAliasPool.CLASS_ALIASES.addAlias(Shape.class, "shape");
        ClassAliasPool.CLASS_ALIASES.addAlias(Line.class, "line");
        ClassAliasPool.CLASS_ALIASES.addAlias(Label.class, "label");
    }

    private final String input;
    private final boolean textWire;

    public YamlSpecificationTest(String input, boolean textWire) {
        this.input = input;
        this.textWire = textWire;
    }

    @Parameterized.Parameters(name = "case={0}, textWire={1}")
    public static Collection<Object[]> tests() {
        ArrayList<Object[]> result = new ArrayList<>();
        for (boolean textWire : new boolean[] {true, false}) {
            result.addAll(Arrays.asList(new Object[][]{
                    {"example2_1", textWire},
                    {"example2_2", textWire},
                    {"example2_3", false},
                    {"example2_4", false},
                    {"example2_5", false},
                    {"example2_6", textWire},
                    {"example2_7", false},
                    //{"example2_8", textWire},
                    {"example2_9", false},
                    {"example2_10", false},
                    //{"example2_11", textWire}, // Not supported
                    // {"example2_12"}, // Not supported
                    // {"example2_13"}, // Not supported
                    // {"example2_14"}, // Not supported
                    // {"example2_15"}, // Not supported
                    // {"example2_16"}, // Not supported
                    // {"example2_17"}, // TODO Fix handling of double single quote.
                    // {"example2_18"}, // Not supported
                    // {"example2_19"}, // TODO fix handling of times.
                    // {"example2_20"}, // TODO fix handling of times.
                    {"example2_21", textWire},
                    // {"example2_22"}, // TODO fix handling of times.
                    // {"example2_23"}, // Not supported
                    // {"example2_24"}, // TODO FIx handling of anchors
                    // {"example2_25"}, // TODO support set
                    // {"example2_26"}, // TODO support omap
                    // {"example2_27"}, // Not supported
                    // {"example2_28"} // Not supported
            }));
        }

        return result;
    }

    @Test
    public void decodeAs() throws IOException {
        String snippet = new String(getBytes(input + ".yaml"), StandardCharsets.UTF_8);
        String actual;
        if (textWire) {
            actual = parseWithText(snippet);
        } else {
            actual = parseWithYaml(snippet);
        }

        byte[] expectedBytes = getBytes(input + ".out.yaml");
        String expected;
        if (expectedBytes != null) {
            assertEquals(actual, textWire ? parseWithText(actual) : parseWithYaml(actual));

            expected = new String(expectedBytes, StandardCharsets.UTF_8);
        } else {
            expected = snippet;
        }

        assertEquals(input, Bytes.wrapForRead(expected.getBytes(StandardCharsets.UTF_8)).toString().replace("\r\n", "\n"), actual);
    }

    @NotNull
    private String parseWithText(String snippet) {
        Object o = TEXT.fromString(snippet);
        Bytes bytes = Bytes.allocateElasticOnHeap();

        TextWire tw = new TextWire(bytes);
        tw.writeObject(o);

        return bytes.toString();
    }

    @NotNull
    private String parseWithYaml(String snippet) {
        Object o = YAML.fromString(snippet);
        Bytes bytes = Bytes.allocateElasticOnHeap();

        YamlWire tw = new YamlWire(bytes);
        tw.writeObject(o);

        return bytes.toString();
    }

    @Nullable
    public byte[] getBytes(String file) throws IOException {
        InputStream is = getClass().getResourceAsStream("/specification/" + file);
        if (is == null) return null;
        int len = is.available();
        @NotNull byte[] byteArr = new byte[len];
        is.read(byteArr);
        return byteArr;
    }
}
/*
--- !shape
  # Use the ! handle for presenting
  # tag:clarkevans.com,2002:circle
- !circle
  center: &ORIGIN {x: 73, y: 129}
  radius: 7
- !line
  start: *ORIGIN
  finish: { x: 89, y: 102 }
- !label
  start: *ORIGIN
  color: 0xFFEEBB
  text: Pretty vector drawing.
 */

class Shape implements Marshallable {
}

class Circle implements Marshallable {
}

class Line implements Marshallable {
}

class Label implements Marshallable {
}
