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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static net.openhft.chronicle.wire.WireType.YAML;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
@RunWith(Parameterized.class)
public class YamlSpecificationTextWireTest extends WireTestCommon {
    private final String input;

    public YamlSpecificationTextWireTest(String input) {
        this.input = input;
    }

    @Parameterized.Parameters(name = "case={0}")
    public static Collection<Object[]> tests() {
        return Arrays.asList(new Object[][]{
                    {"2_1_SequenceOfScalars"},
                    {"2_2_MappingScalarsToScalars"},
                    {"2_6_MappingOfMappings"},
                    // {"2_19Integers"},
                    {"2_21MiscellaneousBis"}
            });
    }

    @Test
    public void decodeAs() throws IOException {
        String snippet = new String(getBytes(input + ".yaml"), StandardCharsets.UTF_8);
        String actual = parseWithText(snippet);

        byte[] expectedBytes = getBytes(input + ".out.yaml");
        String expected;
        if (expectedBytes != null) {
            assertEquals(actual, parseWithText(actual));

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
