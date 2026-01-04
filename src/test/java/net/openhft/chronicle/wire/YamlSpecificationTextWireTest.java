/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
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

import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
public class YamlSpecificationTextWireTest extends WireTestCommon {

    // Holds the input data for each test case
    private String input;

    // Parameterized constructor that initializes the input for each test case
    public void initYamlSpecificationTextWireTest(String input) {
        this.input = input;
    }

    // Provides the parameters for the parameterized test
    public static Collection<Object[]> tests() {
        return Arrays.asList(new Object[][]{
                    // {"2_1_SequenceOfScalars"},  // Uncomment to include in the test
                    {"2_2_MappingScalarsToScalars"},
                    {"2_6_MappingOfMappings"},
                    // {"2_19Integers"},  // Uncomment to include in the test
                    {"2_21MiscellaneousBis"}
            });
    }

    // Test method to decode YAML as TextWire and validate it
    @MethodSource("tests")
    @ParameterizedTest(name = "YAML text spec case {0} should round trip")
    @DisplayName("YAML specification text wire round trip")
    public void decodeAs(String input) throws IOException {
        initYamlSpecificationTextWireTest(input);
        // Reads the YAML file and converts it to a string
        String snippet = new String(getBytes(input + ".yaml"), StandardCharsets.UTF_8);

        // Parse the YAML snippet using TextWire
        String actual = parseWithText(snippet);

        // Expected output read from a .out.yaml file
        byte[] expectedBytes = getBytes(input + ".out.yaml");
        String expected;
        if (expectedBytes != null) {
            assertEquals(actual, parseWithText(actual),
                    "Text wire should be idempotent for case: " + input);

            expected = new String(expectedBytes, StandardCharsets.UTF_8);
        } else {
            expected = snippet;
        }

        // Validate if the actual output matches the expected output
        assertEquals(Bytes.wrapForRead(expected.getBytes(StandardCharsets.UTF_8)).toString().replace("\r\n", "\n"),
                actual,
                "Text wire output should match specification case: " + input);
    }

    // Helper method to parse a given YAML string using TextWire
    @NotNull
    private String parseWithText(String snippet) {
        // Convert the snippet to an object
        Object o = TEXT.fromString(snippet);

        // Create a new TextWire object
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        TextWire tw = new TextWire(bytes);

        // Write the object to TextWire
        tw.writeObject(o);

        // Return the written bytes as a string
        return bytes.toString();
    }

    // Helper method to read the bytes of a file
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
