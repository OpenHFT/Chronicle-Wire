/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class JSON222Test extends WireTestCommon {

    @NotNull
    private final File file;

    // Constructor that accepts parameters for each test iteration
    public JSON222Test(@NotNull String fileName, File file) {
        this.file = file;
    }

    // Provide the test parameters from a collection of files found in a specific directory
    @NotNull
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();

        // Iterate through all the files in the given directory
        for (@NotNull final File file : OS.findFile("OpenHFT", "Chronicle-Wire", "src/test/resources/nst_files").listFiles()) {
            // Add files with a specific naming pattern (contains underscore) to the list of test inputs
            if (file.getName().contains("_")) {
                @NotNull Object[] args = {file.getName(), file};
                list.add(args);
            }
        }

        // Sort the list of files based on a specific part of their names
        list.sort(Comparator.comparingInt(f -> Integer.parseInt(((String) f[0]).split("[_.]")[1])));
        return list;
    }

    // Test the JSON content using TextWire type
    @Test
    public void testJSONAsTextWire() throws IOException {
        testJSON(WireType.TEXT);
    }

    @Test
    public void testJSONAsYamlWire() throws IOException {
        testJSON(WireType.YAML_ONLY);
    }

    private void testJSON(WireType wireType) throws IOException {
        // Read the file content into a byte array
        @NotNull byte[] bytes = Files.readAllBytes(file.toPath());
        // System.out.println(file + " " + new String(bytes, "UTF-8"));
        // Convert byte array to Bytes object for further processing
        Bytes<?> b = Bytes.wrapForRead(bytes);
        @NotNull Wire wire = new JSONWire(b);
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap();
        @NotNull Wire out = wireType.apply(bytes2);

        // Flag to determine if this test iteration is expected to fail
        boolean fail = file.getName().startsWith("n");
        Bytes<?> bytes3 = Bytes.allocateElasticOnHeap();
        try {
            @NotNull List<Object> list = new ArrayList<>();
            do {
                // Read an object from the wire
                @Nullable final Object object;
                try {
                    object = wire.getValueIn()
                            .object();
                } catch (Exception e) {
                    if (fail)
                        return;
                    throw new AssertionError(e);
                }

                // Write the read object into another wire for further comparison
                @NotNull Wire out3 = wireType.apply(bytes3);
                out3.getValueOut()
                        .object(object);

                // System.out.println("As YAML " + bytes3);
                // Validate the read object with an external YAML parser
                parseWithSnakeYaml(bytes3.toString());
                @Nullable Object object3 = out3.getValueIn()
                        .object();
                assertEquals(object, object3);

                list.add(object);
                out.getValueOut().object(object);

            } while (wire.isNotEmptyAfterPadding());

            if (fail) {
                // If the test iteration is expected to fail, we check the expected output against a reference file
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
                String expected = new String(Files.readAllBytes(file2.toPath()), StandardCharsets.UTF_8);
                if (expected.contains("\r\n"))
                    expected = expected.replaceAll("\r\n", "\n");
                String actual = bytes2.toString();
                // The snapshots record TextWire's lexical form; YamlWire legitimately canonicalises numbers.
                if (wireType == WireType.TEXT)
                    assertEquals(expected, actual);
            }
            // if (fail)
            // throw new AssertionError("Expected to fail, was " + list);
        } finally {
            // Release resources to avoid memory leaks
            bytes2.releaseLast();
            bytes3.releaseLast();
        }
    }

    // Utility method to parse a string using the SnakeYaml library
    private static void parseWithSnakeYaml(@NotNull String s) {
        try {
            @NotNull Yaml yaml = new Yaml();
            yaml.load(new StringReader(s));
        } catch (Exception e) {
            throw e;
        }
    }
}
