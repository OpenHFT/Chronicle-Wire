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
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class JSON222Test extends WireTestCommon {

    @NotNull
    final File file;

    public JSON222Test(@NotNull String fileName, File file) {
        this.file = file;
    }

    @NotNull
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        for (@NotNull final File file : OS.findFile("OpenHFT", "Chronicle-Wire", "src/test/resources/nst_files").listFiles()) {
            if (file.getName().contains("_")) {
                @NotNull Object[] args = {file.getName(), file};
                list.add(args);
            }
        }
        list.sort(Comparator.comparingInt(f -> Integer.parseInt(((String) f[0]).split("[_.]")[1])));
        return list;
    }

    @Test
    public void testJSON() throws IOException {
        int len = Maths.toUInt31(file.length());
        @NotNull byte[] bytes = new byte[len];
        try (@NotNull InputStream in = new FileInputStream(file)) {
            in.read(bytes);
        }
        // System.out.println(file + " " + new String(bytes, "UTF-8"));
        Bytes b = Bytes.wrapForRead(bytes);
        @NotNull Wire wire = new JSONWire(b);
        Bytes bytes2 = Bytes.elasticByteBuffer();
        @NotNull TextWire out = new TextWire(bytes2);

        boolean fail = file.getName().startsWith("n");
        Bytes bytes3 = Bytes.elasticByteBuffer();
        try {
            @NotNull List list = new ArrayList();
            do {
                @Nullable final Object object = wire.getValueIn()
                        .object();

                @NotNull TextWire out3 = new TextWire(bytes3);
                out3.getValueOut()
                        .object(object);
                // System.out.println("As YAML " + bytes3);
                parseWithSnakeYaml(bytes3.toString());
                @Nullable Object object3 = out3.getValueIn()
                        .object();
                assertEquals(object, object3);

                list.add(object);
                out.getValueOut().object(object);

            } while (wire.isNotEmptyAfterPadding());

            if (fail) {
                @NotNull String path = file.getPath();
                @NotNull final File file2 = new File(path.replaceAll("\\b._", "e-").replaceAll("\\.json", ".yaml"));

/*
               // System.out.println(file2 + "\n" + new String(bytes, "UTF-8") + "\n" + bytes2);
                try (OutputStream out2 = new FileOutputStream(file2)) {
                    out2.write(bytes2.toByteArray());
                }
*/

                if (!file2.exists())
                    throw new AssertionError("Expected to fail\n" + bytes2);
                @NotNull byte[] bytes4 = new byte[(int) file2.length()];
                try (@NotNull InputStream in = new FileInputStream(file2)) {
                    in.read(bytes4);
                }
                String expected = new String(bytes4, "UTF-8");
                if (expected.contains("\r\n"))
                    expected = expected.replaceAll("\r\n", "\n");
                String actual = bytes2.toString();
                // TODO FIX Reduce redundant padding around [ ] in JSON #392
                actual = actual.replaceAll("\\[ ", "[");
                assertEquals(expected, actual);
            }
            // if (fail)
            // throw new AssertionError("Expected to fail, was " + list);
        } catch (Exception e) {
            if (!fail)
                throw new AssertionError(e);
        } finally {
            bytes2.releaseLast();
            bytes3.releaseLast();
        }
    }

    static void parseWithSnakeYaml(@NotNull String s) {
        try {
            @NotNull Yaml yaml = new Yaml();
            yaml.load(new StringReader(s));
        } catch (Exception e) {
            throw e;
        }
    }
}
