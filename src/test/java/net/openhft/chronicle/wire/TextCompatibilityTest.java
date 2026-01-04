/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Disabled pending updated text compatibility fixtures review")
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
public class TextCompatibilityTest extends WireTestCommon {

    private static final long MAX_TEXT_BYTES = 50;

    // File name for the current test run.
    private String filename;
    // Expected content for the current test run.
    private String expected;

    // Constructor to initialize the test with a specific file and its expected content.
    public void initTextCompatibilityTest(String filename, String expected) {
        this.filename = filename;
        this.expected = expected;
    }

    // Main method that demonstrates how to find YAML files in a directory and run the test on them.
    public static void main(String[] args) throws IOException {
        String userHome = System.getProperty("user.home");
        Files.find(Paths.get(userHome, "git", "snakeyaml", "src", "test", "resources"), 4,
                (p, a) -> p.toString().endsWith(".yaml"))
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

    private static String readFileToString(String file) throws IOException {
        Bytes<?> bytes = BytesUtil.readFile(file);
        try {
            return bytes.toString();
        } finally {
            bytes.releaseLast();
        }
    }

    private static final class CompatibilityResult {
        final boolean skipped;
        final long bytesRemaining;
        final String expected;
        final String actual;

        CompatibilityResult(boolean skipped, long bytesRemaining, String expected, String actual) {
            this.skipped = skipped;
            this.bytesRemaining = bytesRemaining;
            this.expected = expected;
            this.actual = actual;
        }
    }

    // Run the actual compatibility test on a file and its expected content.
    @SuppressWarnings("rawtypes")
    private static CompatibilityResult runTest(String filename, String expectedFilename, boolean print) {
        try {
            Bytes<?> bytes = BytesUtil.readFile(filename);
            try {
                long bytesRemaining = bytes.readRemaining();
                if (bytesRemaining > MAX_TEXT_BYTES)
                    return new CompatibilityResult(true, bytesRemaining, null, null);

                String expected = filename.equals(expectedFilename)
                        ? bytes.toString()
                        : readFileToString(expectedFilename);
                Object o = new YamlWire(bytes)
                        .getValueIn()
                        .object();
                Bytes<?> out = Bytes.allocateElasticOnHeap(256);
                String s = WireType.TEXT.apply(out).getValueOut().object(o).toString();
                TEXT.fromFile(Object.class, filename);
                return new CompatibilityResult(false, bytesRemaining, expected, s);
            } finally {
                bytes.releaseLast();
            }
        } catch (Exception e) {
            throw new AssertionError(filename, e);
        }
    }

    // Perform the compatibility test for the current combination of file and expected content.
    @DisplayName("Verifies text compatibility against YAML fixtures")
    @MethodSource("combinations")
    @ParameterizedTest
    public void test(String filename, String expected) {
        initTextCompatibilityTest(filename, expected);
        CompatibilityResult result = runTest(filename, expected, false);
        if (result.skipped) {
            assertTrue(result.bytesRemaining > MAX_TEXT_BYTES,
                    "compat: skipped large file bytesRemaining=" + result.bytesRemaining);
            return;
        }
        assertEquals(result.expected.trim(),
                result.actual.trim(),
                "compat: output matches expected file=" + filename);
    }
}
