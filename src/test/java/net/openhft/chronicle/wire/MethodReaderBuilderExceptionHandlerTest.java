/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.core.util.Mocker;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

// Testing the behavior of the MethodReaderBuilder exception handler with various scenarios.
public class MethodReaderBuilderExceptionHandlerTest extends WireTestCommon {

    // Static input data for the tests
    private static final String input = "---\n" +
            "a: a1\n" +
            "...\n" +
            "---\n" +
            "b: b1\n" +
            "...\n" +
            "---\n" +
            "c: c1\n" +
            "...\n" +
            "---\n" +
            "a: a2\n" +
            "...\n" +
            "---\n" +
            "b: b2\n" +
            "...\n" +
            "---\n" +
            "c: c2\n" +
            "...\n";

    // Test where nothing is expected to happen, using non-scanning method
    @Test
    public void testNothing() {
        doTest("# true\n" +
                        "# true\n" +
                        "# true\n" +
                        "# true\n" +
                        "# true\n" +
                        "# true\n",
                ExceptionHandler.ignoresEverything(), IgnoresEverything.class, false);
    }

    // Test where nothing is expected to happen, using scanning method
    @Test
    public void testNothingScanning() {
        doTest("# false\n",
                ExceptionHandler.ignoresEverything(), IgnoresEverything.class, true);
    }

    // Test focusing on the 'a' type message, using non-scanning method
    @Test
    public void testA() {
        doTest("a[a1]\n" +
                        "# true\n" +
                        "# true\n" +
                        "# true\n" +
                        "a[a2]\n" +
                        "# true\n" +
                        "# true\n" +
                        "# true\n",
                ExceptionHandler.ignoresEverything(), IA.class, false);
    }

    // Test focusing on the 'a' type message, using scanning method
    @Test
    public void testAScanning() {
        doTest("a[a1]\n" +
                        "# true\n" +
                        "a[a2]\n" +
                        "# true\n" +
                        "# false\n",
                ExceptionHandler.ignoresEverything(), IA.class, true);
    }

    // Test focusing on both 'b' and 'c' type messages using non-scanning method
    @Test
    public void testBC() {
        doTest("# true\n" +
                        "b[b1]\n" +
                        "# true\n" +
                        "c[c1]\n" +
                        "# true\n" +
                        "# true\n" +
                        "b[b2]\n" +
                        "# true\n" +
                        "c[c2]\n" +
                        "# true\n",
                ExceptionHandler.ignoresEverything(), IBC.class, false);
    }

    // Test focusing on both 'b' and 'c' type messages using scanning method
    @Test
    public void testBCScanning() {
        doTest("b[b1]\n" +
                        "# true\n" +
                        "c[c1]\n" +
                        "# true\n" +
                        "b[b2]\n" +
                        "# true\n" +
                        "c[c2]\n" +
                        "# true\n",
                ExceptionHandler.ignoresEverything(), IBC.class, true);
    }

    // Test focusing on 'b' and 'c' type messages using non-scanning method, while expecting a warning for the 'a' type message
    @Test
    public void testBCWarn() {
        expectException("Unknown method-name='a'");
        doTest("# true\n" +
                        "b[b1]\n" +
                        "# true\n" +
                        "c[c1]\n" +
                        "# true\n" +
                        "# true\n" +
                        "b[b2]\n" +
                        "# true\n" +
                        "c[c2]\n" +
                        "# true\n",
                Jvm.warn(), IBC.class, false);
    }

    // Test focusing on 'b' and 'c' type messages using scanning method, while expecting a warning for the 'a' type message
    @Test
    public void testBCWarnScanning() {
        expectException("Unknown method-name='a'");
        doTest("b[b1]\n" +
                        "# true\n" +
                        "c[c1]\n" +
                        "# true\n" +
                        "b[b2]\n" +
                        "# true\n" +
                        "c[c2]\n" +
                        "# true\n",
                Jvm.warn(), IBC.class, true);
    }

    // A helper method for performing tests:
    // - It first creates a StringWriter to capture the output
    // - Then constructs a MethodReader using a YAML_ONLY Wire with the provided `input` data
    // - The constructed MethodReader uses the provided exception handler, and its scanning behavior is determined by the `scanning` flag.
    // - The MethodReader then processes each message in the Wire, logging its output and the result of each read to the StringWriter.
    // - Finally, it compares the StringWriter's output with the expected output to determine if the test passes or fails.
    private void doTest(String expected, ExceptionHandler eh, Class<?> type, boolean scanning) {
        @NotNull StringWriter out = new StringWriter();
        Wire wire = WireType.YAML_ONLY.apply(Bytes.from(input));
        MethodReader reader = wire
                .methodReaderBuilder()
                .scanning(scanning)
                .exceptionHandlerOnUnknownMethod(eh)
                .build(Mocker.logging(type, "", out));
        while (!wire.isEmpty()) {
            boolean read = reader.readOne();
            out.append("# ").append(Boolean.toString(read)).append("\n");
        }
        assertEquals(expected, out.toString().replace("\r", ""));
    }

    // Interface for handling 'a' type messages
    interface IA {
        void a(String text);
    }

    // Interface for handling 'b' type messages
    interface IB {
        void b(String text);
    }

    // Interface for handling 'c' type messages
    interface IC {
        void c(String text);
    }

    // Composite interface extending both _B and _C
    private interface IBC extends IB, IC {
    }
}
