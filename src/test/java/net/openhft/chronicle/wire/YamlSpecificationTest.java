/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.wire.WireType.YAML;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
class YamlSpecificationTest extends WireTestCommon {

    // Register class aliases for String, Circle, Shape, Line, and Label
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(String.class, "something");
        /*ClassAliasPool.CLASS_ALIASES.addAlias(Circle.class, "circle");
        ClassAliasPool.CLASS_ALIASES.addAlias(Shape.class, "shape");
        ClassAliasPool.CLASS_ALIASES.addAlias(Line.class, "line");
        ClassAliasPool.CLASS_ALIASES.addAlias(Label.class, "label");*/
    }

    // Input string used for tests
    private String input;

    // Parameterized constructor
    void initYamlSpecificationTest(String input) {
        this.input = input;
    }

    // Defining parameterized test cases
    public static Collection<Object[]> tests() {
        return Arrays.asList(new Object[][]{
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

    // Test to decode YAML snippets based on various specifications
    @MethodSource("tests")
    @ParameterizedTest(name = "YAML spec case {0} should round trip")
    @DisplayName("YAML specification snippets round trip correctly")
    void decodeAs(String input) throws IOException {
        initYamlSpecificationTest(input);
        String snippet = new String(getBytes(input + ".yaml"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        String actual = parseWithYaml(snippet);

        byte[] expectedBytes = getBytes(input + ".out.yaml");
        String expected;
        if (expectedBytes != null) {
            assertEquals(actual, parseWithYaml(actual), "parsed YAML should be idempotent when re-parsed");

            expected = new String(expectedBytes, StandardCharsets.UTF_8);
        } else {
            expected = snippet;
        }

        final String expectedStr = Bytes.wrapForRead(expected.getBytes(StandardCharsets.UTF_8)).toString();
        assertEquals(expectedStr
                        .replace("\r\n", "\n"),
                actual,
                "YAML output should match specification for test case: " + input);
    }

    // Helper method to parse input string using YamlWire
    @NotNull
    private String parseWithYaml(String snippet) {
        Object o = YAML.fromString(snippet);

        YamlWire tw = (YamlWire) Wire.newYamlWireOnHeap();
        tw.getValueOut().object(o);

        return tw.toString();
    }

    // Helper method to get bytes from a given file
    @Nullable
    private byte[] getBytes(String file) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/yaml/spec/" + file)) {
            if (is == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}

// Commented-out example YAML and classes
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
