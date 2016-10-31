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
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;
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

/**
 * Created by peter on 28/10/16.
 */
@RunWith(value = Parameterized.class)
public class JSON222Test {

    final File file;

    public JSON222Test(File file) {
        this.file = file;
    }

    @Parameterized.Parameters
    public static Collection<File[]> combinations() {
        List<File[]> list = new ArrayList<>();
        for (File file : new File(OS.getTarget(), "../src/test/resources/nst_files").listFiles()) {
            if (file.getName().contains("_")) {
                File[] args = {file};
                list.add(args);
            }
        }
        list.sort(Comparator.comparingInt(f -> Integer.parseInt(f[0].getName().split("[_.]")[1])));
        return list;
    }

    @Test//(timeout = 500)
    public void testJSON() throws IOException {
        int len = Maths.toUInt31(file.length());
        byte[] bytes = new byte[len];
        try (InputStream in = new FileInputStream(file)) {
            in.read(bytes);
        }
//        System.out.println(file + " " + new String(bytes, "UTF-8"));
        Bytes b = Bytes.wrapForRead(bytes);
        Wire wire = new JSONWire(b);
        Bytes bytes2 = Bytes.elasticByteBuffer();
        TextWire out = new TextWire(bytes2);

        boolean fail = file.getName().startsWith("n");
        try {
            List list = new ArrayList();
            do {
                final Object object = wire.getValueIn()
                        .object();

                Bytes bytes3 = Bytes.elasticByteBuffer();
                TextWire out3 = new TextWire(bytes3);
                out3.getValueOut()
                        .object(object);
//                System.out.println("As YAML " + bytes3);
                parseWithSnakeYaml(bytes3.toString());
                Object object3 = out3.getValueIn()
                        .object();
                assertEquals(object, object3);

                list.add(object);
                out.getValueOut().object(object);

            } while (wire.isNotEmptyAfterPadding());

            if (fail) {
                final File file2 = new File(this.file.getPath().replaceAll("/._", "/e-"));
/*
                System.out.println(file2 + "\n" + new String(bytes, "UTF-8") + "\n" + bytes2);
                try (OutputStream out2 = new FileOutputStream(file2)) {
                    out2.write(bytes2.toByteArray());
                }
*/
                if (!file2.exists())
                    throw new AssertionError("Expected to fail\n" + bytes2);
                byte[] bytes4 = new byte[(int) file2.length()];
                try (InputStream in = new FileInputStream(file2)) {
                    in.read(bytes4);
                }
                assertEquals(new String(bytes4, "UTF-8"), bytes2.toString());
            }
//            if (fail)
//                throw new AssertionError("Expected to fail, was " + list);
        } catch (Exception e) {
            if (!fail)
                throw new AssertionError(e);
        }
    }

    private void parseWithSnakeYaml(@NotNull String s) {
        try {
            Yaml yaml = new Yaml();
            yaml.load(new StringReader(s));
        } catch (Exception e) {
            throw e;
        }
    }
}
