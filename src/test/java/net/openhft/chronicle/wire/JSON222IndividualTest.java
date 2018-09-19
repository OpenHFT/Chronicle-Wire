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
import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 31/10/16.
 */
public class JSON222IndividualTest {
    @Test
    public void testEmptyBrackets() {
        checkSerialized("{}", new LinkedHashMap<>());
    }

    @Test
    public void testTab() {
        checkSerialized("\"hello\\tworld\"\n", "hello\tworld");
        checkDeserialized("hello\tworld", "\"hello\\tworld\"");
    }

    @Test
    public void testSpecial() {
        checkSerialized("\"\\u1000\"\n", "\u1000");
        checkDeserialized("\u1000", "\"\\u1000\"");
    }

    @Test
    public void nestedSeq() {
        @NotNull List list = Arrays.asList(3L, Arrays.asList(4L));
        checkSerialized("[\n" +
                "  3,\n" +
                "  [\n" +
                "    4\n" +
                "  ]\n" +
                "]", list);
    }

    @Test
    public void parseArrayKey() {
        checkDeserialized("{5=[6], [7]=}", "{ '5': [ 6 ], [ 7 ] }\n");
    }

    void checkSerialized(@NotNull String expected, Object o) {
        @NotNull Wire wire = new TextWire(Bytes.elasticByteBuffer());
        try {
            wire.getValueOut()
                    .object(o);

            try {
                @NotNull Yaml yaml = new Yaml();
                yaml.load(new StringReader(expected));
            } catch (Exception e) {
                throw e;
            }

            assertEquals(expected, wire.toString());

        } finally {
            wire.bytes().release();
        }
    }

    void checkDeserialized(String expected, @NotNull String input) {
        @NotNull Wire wire = new TextWire(Bytes.from(input));

        try {
            @NotNull Yaml yaml = new Yaml();
            Object o = yaml.load(new StringReader(input));
            System.out.println(o);
        } catch (Exception e) {
            throw e;
        }
        @Nullable Object o = wire.getValueIn()
                .object();

        assertEquals(expected, o.toString());

        wire.bytes().release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}
