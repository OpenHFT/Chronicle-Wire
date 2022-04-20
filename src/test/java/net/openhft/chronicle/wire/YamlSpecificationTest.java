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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.wire.WireType.YAML;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
@RunWith(Parameterized.class)
public class YamlSpecificationTest extends WireTestCommon {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(String.class, "something");
        /*ClassAliasPool.CLASS_ALIASES.addAlias(Circle.class, "circle");
        ClassAliasPool.CLASS_ALIASES.addAlias(Shape.class, "shape");
        ClassAliasPool.CLASS_ALIASES.addAlias(Line.class, "line");
        ClassAliasPool.CLASS_ALIASES.addAlias(Label.class, "label");*/
    }

    private final String input;

    public YamlSpecificationTest(String input) {
        this.input = input;
    }

    @Parameterized.Parameters(name = "case={0}")
    public static Collection<Object[]> tests() {
        return Arrays.asList(new String[][]{
                {"2_1_SequenceOfScalars"},
                {"2_2_MappingScalarsToScalars"},
                {"2_3_MappingScalarsToSequences"},
                {"2_4_SequenceOfMappings"},
                {"2_5_SequenceOfSequences"},
                {"2_6_MappingOfMappings"},
                {"2_7_TwoDocumentsInAStream"},
                // {"example2_8"},
                {"2_9_SingleDocumentWithTwoComments"},
                {"2_10_NodeAppearsTwiceInThisDocument"},
                // {"2_11MappingBetweenSequences"}, // Not supported
                {"2_12CompactNestedMapping"},
                {"2_13InLiteralsNewlinesArePreserved"},
                {"2_14InThefoldedScalars"},
                // {"example2_15"}, // Not supported
                // {"example2_16"}, // Not supported
                {"2_17QuotedScalars"},
                // {"example2_18"}, // Not supported
                {"2_19Integers"},
                // {"2_20FloatingPoint"}, // TODO fix handling of .nan/.inf
                {"2_21MiscellaneousBis"},
                // {"example2_22"}, // TODO fix handling of times.
                {"2_23VariousExplicitTags"},
                // {"example2_24"}, // TODO FIx handling of anchors
                // {"example2_25"}, // TODO support set
                // {"2_26OrderedMappings"}, // TODO support omap
                // {"example2_27"}, // Not supported
                // {"example2_28"} // Not supported
        });
    }

    @Test
    public void decodeAs() throws IOException {
        String snippet = new String(getBytes(input + ".yaml"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        String actual = parseWithYaml(snippet);

        byte[] expectedBytes = getBytes(input + ".out.yaml");
        String expected;
        if (expectedBytes != null) {
            assertEquals(actual, parseWithYaml(actual));

            expected = new String(expectedBytes, StandardCharsets.UTF_8);
        } else {
            expected = snippet;
        }

        final String expectedStr = Bytes.wrapForRead(expected.getBytes(StandardCharsets.UTF_8)).toString();
        assertEquals(input,
                expectedStr
                        .replace("\r\n", "\n"),
                actual);
    }

    @NotNull
    private String parseWithYaml(String snippet) {
        Object o = YAML.fromString(snippet);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        YamlWire tw = new YamlWire(bytes);
        tw.writeObject(o);

        return bytes.toString();
    }

    @Nullable
    public byte[] getBytes(String file) throws IOException {
        InputStream is = getClass().getResourceAsStream("/yaml/spec/" + file);
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
/*
class Shape implements Marshallable {
}

class Circle implements Marshallable {
}

class Line implements Marshallable {
}

class Label implements Marshallable {
}
*/