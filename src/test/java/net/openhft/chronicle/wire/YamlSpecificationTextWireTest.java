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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
@RunWith(Parameterized.class)
public class YamlSpecificationTextWireTest extends WireTestCommon {

    // Holds the input data for each test case
    private final String input;

    // Parameterized constructor that initializes the input for each test case
    public YamlSpecificationTextWireTest(String input) {
        this.input = input;
    }

    // Provides the parameters for the parameterized test
    @Parameterized.Parameters(name = "case={0}")
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
    @Test
    public void decodeAs() throws IOException {
        // Reads the YAML file and converts it to a string
        String snippet = new String(getBytes(input + ".yaml"), StandardCharsets.UTF_8);

        // Parse the YAML snippet using TextWire
        String actual = parseWithText(snippet);

        // Expected output read from a .out.yaml file
        byte[] expectedBytes = getBytes(input + ".out.yaml");
        String expected;
        if (expectedBytes != null) {
            assertEquals(actual, parseWithText(actual));

            expected = new String(expectedBytes, StandardCharsets.UTF_8);
        } else {
            expected = snippet;
        }

        // Validate if the actual output matches the expected output
        assertEquals(input, Bytes.wrapForRead(expected.getBytes(StandardCharsets.UTF_8)).toString().replace("\r\n", "\n"), actual);
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
    public byte[] getBytes(String file) throws IOException {
        // Locate the file resource
        InputStream is = getClass().getResourceAsStream("/yaml/spec/" + file);
        if (is == null) return null;

        // Read the bytes into a byte array
        int len = is.available();
        @NotNull byte[] byteArr = new byte[len];
        is.read(byteArr);
        return byteArr;
    }
}
