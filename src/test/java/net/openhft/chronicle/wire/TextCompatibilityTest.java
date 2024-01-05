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
import net.openhft.chronicle.bytes.BytesUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
@Ignore("TODO FIX")
public class TextCompatibilityTest extends WireTestCommon {

    // File name for the current test run.
    private final String filename;
    // Expected content for the current test run.
    private final String expected;

    // Constructor to initialize the test with a specific file and its expected content.
    public TextCompatibilityTest(String filename, String expected) {
        this.filename = filename;
        this.expected = expected;
    }

    // Main method that demonstrates how to find YAML files in a directory and run the test on them.
    public static void main(String[] args) throws IOException {
        String base = "/home/peter/git/snakeyaml/src/test/resources";
        Files.find(Paths.get(base), 4, (p, a) -> p.toString().endsWith(".yaml"))
                .forEach(p -> runTest(p.toString(), p.toString(), true));
    }

    // Provide the combinations of files and their expected content for the tests.
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() throws IOException {
        List<Object[]> list = new ArrayList<>();
        String dir = "src/test/resources/compat";
        if (new File("Chronicle-Wire").isDirectory())
            dir = "Chronicle-Wire/" + dir;
        Files.find(Paths.get(dir), 4, (p, a) -> p.toString().endsWith(".yaml"))
                .filter(p -> !p.endsWith(".out.yaml"))
                .forEach(p -> addTest(list, p.toString()));

        return list;
    }

    // Add a test case (file and expected content) to the list.
    private static void addTest(List<Object[]> list, String file) {
        String out = file.replace(".yaml", ".out.yaml");
        if (!new File(out).exists())
            out = file;
        Object[] args = {file, out};
        list.add(args);
    }

    // Run the actual compatibility test on a file and its expected content.
    @SuppressWarnings("rawtypes")
    private static void runTest(String filename, String expectedFilename, boolean print) {
        String expected = null;
        try {
            Bytes<?> bytes = BytesUtil.readFile(filename);
            if (bytes.readRemaining() > 50)
                return;
            expected = filename.equals(expectedFilename) ? bytes.toString() : BytesUtil.readFile(filename).toString();
            try {
                Object o = new YamlWire(bytes)
                        .getValueIn()
                        .object();
                Bytes<?> out = Bytes.allocateElasticOnHeap(256);
                String s = WireType.TEXT.apply(out).getValueOut().object(o).toString();
                if (s.trim().equals(expected.trim()))
                    return;
                if (print) {
                   // System.out.println("Comparison failure in " + filename);
                   // System.out.println("Expected:\n" + expected);
                   // System.out.println("Actual:\n" + s);
                } else {
                    assertEquals(expected, s);
                }
            } finally {
                bytes.releaseLast();
            }
            Object o = TEXT.fromFile(Object.class, filename);
        } catch (Exception e) {
           // System.out.println("Expected:\n" + expected);
            throw new AssertionError(filename, e);
        }
    }

    // Perform the compatibility test for the current combination of file and expected content.
    @Test
    public void test() {
        runTest(filename, expected, false);
    }
}
