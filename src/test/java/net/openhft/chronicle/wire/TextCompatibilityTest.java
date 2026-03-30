/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("TODO FIX")
class TextCompatibilityTest extends WireTestCommon {

    // File name for the current test run.
    private String filename;
    // Expected content for the current test run.
    private String expected;

    // Main method that demonstrates how to find YAML files in a directory and run the test on them.
    public static void main(String[] args) throws IOException {
        String base = "/home/peter/git/snakeyaml/src/test/resources";
        Files.find(Paths.get(base), 4, (p, a) -> p.toString().endsWith(".yaml"))
                .forEach(p -> runTest(p.toString(), p.toString(), true));
    }

    // Provide the combinations of files and their expected content for the tests.
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
    @ParameterizedTest
    @MethodSource("combinations")
    void test(String filename, String expected) {
        this.filename = filename;
        this.expected = expected;
        runTest(filename, expected, false);
    }
}
