/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ChainedMethodsTest extends WireTestCommon {

    // Specifies the parameters to be used for the test runs.
    public static Collection<Object[]> data() {
        // Two sets of parameters: 'false' and 'true'
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    private void verifyChainedTextual(Wire wire, boolean disableProxyCodegen) {
        if (disableProxyCodegen)
            expectException("Falling back to proxy method writer");

        ITop top = wire.methodWriter(ITop.class);

        // Chain method calls on the created wire instance.
        top.mid("mid")
                .next(1)
                .echo("echo-1");
        top.mid2("mid2")
                .next2("word")
                .echo("echo-2");

        // Validate the wire's string representation.
        assertEquals("mid: mid\n" +
                "next: 1\n" +
                "echo: echo-1\n" +
                "...\n" +
                "mid2: mid2\n" +
                "next2: word\n" +
                "echo: echo-2\n" +
                "...\n", wire.toString(),
                "Expected chained textual output for disableProxyCodegen=" + disableProxyCodegen);

        // Create a StringBuilder to collect method call representations.
        StringBuilder sb = new StringBuilder();

        // Create a method reader to read method calls.
        MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
        assertTrue(reader.readOne(), "Expected reader to return first chained method call");
        assertTrue(reader.readOne(), "Expected reader to return second chained method call");

        // Validate the string representation of method calls.
        assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString(),
                "Expected method call trace from reader");
        assertFalse(reader.readOne(), "Expected no additional method calls");
    }

    // Test method for chained methods with TextWire.
    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    @DisplayName("Chains methods in TextWire without errors")
    public void chainedText(boolean disableProxyCodegen) {
        withDisableProxyCodegen(disableProxyCodegen, () -> {
            TextWire wire = new TextWire(Bytes.allocateElasticOnHeap(128))
                    .useTextDocuments();
            verifyChainedTextual(wire, disableProxyCodegen);
        });
    }

    // Test method for chained methods with YAML Wire.
    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    @DisplayName("Chains methods in YamlWire without errors")
    public void chainedYaml(boolean disableProxyCodegen) {
        withDisableProxyCodegen(disableProxyCodegen, () -> {
            Wire wire = Wire.newYamlWireOnHeap();
            verifyChainedTextual(wire, disableProxyCodegen);
        });
    }

    // Test for chained methods with BinaryWire
    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    @DisplayName("Chains methods in BinaryWire without errors")
    public void chainedBinary(boolean disableProxyCodegen) {
        withDisableProxyCodegen(disableProxyCodegen, () -> {
            // Assume the test should not run if the condition is true.
            assumeFalse(disableProxyCodegen,
                    "Proxy codegen disabled; skip chained binary test (see https://github.com/OpenHFT/Chronicle-Wire/issues/460)");

            // Create an instance of BinaryWire.
            Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
            wire.usePadding(true);
            ITop top = wire.methodWriter(ITop.class);

            // Chain method calls on the created wire instance.
            top.mid("mid")
                    .next(1)
                    .echo("echo-1");
            assertEquals(34, wire.bytes().writePosition(),
                    "Expected write position after first chain");
            top.mid2("mid2")
                    .next2("word")
                    .echo("echo-2");

            // Validate the wire's representation using WireDumper.
            assertEquals("--- !!data #binary\n" +
                    "mid: mid\n" +
                    "next: 1\n" +
                    "echo: echo-1\n" +
                    "# position: 36, header: 1\n" +
                    "--- !!data #binary\n" +
                    "mid2: mid2\n" +
                    "next2: word\n" +
                    "echo: echo-2\n", WireDumper.of(wire).asString(),
                    "Expected wire output for chained binary calls");

            // Create a StringBuilder to collect method call representations.
            StringBuilder sb = new StringBuilder();

            // Create a method reader to read method calls.
            MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
            assertTrue(reader.readOne(), "Expected reader to return first binary method call");
            assertTrue(reader.readOne(), "Expected reader to return second binary method call");

            // Validate the string representation of method calls.
            assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString(),
                    "Expected binary method call trace");
            assertFalse(reader.readOne(), "Expected no additional binary method calls");
        });
    }

    // Test for chained methods with BinaryWire and varying argument numbers
    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    @DisplayName("Chains binary methods with varying argument counts")
    public void chainedBinaryVariousArgsNumber(boolean disableProxyCodegen) {
        withDisableProxyCodegen(disableProxyCodegen, () -> {
            // Assume the test should not run if the condition is true.
            assumeFalse(disableProxyCodegen,
                    "Proxy codegen disabled; skip binary args test (see https://github.com/OpenHFT/Chronicle-Wire/issues/460)");

            // Create an instance of BinaryWire.
            Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
            wire.usePadding(true);
            ITop top = wire.methodWriter(ITop.class);

            // Chain method calls on the created wire instance.
            top.midNoArg()
                    .next(1)
                    .echo("echo-1");

            top.midTwoArgs(5, -7L)
                    .next(2)
                    .echo("echo-2");

            // Validate the wire's representation using WireDumper.
            assertEquals("--- !!data #binary\n" +
                            "midNoArg: \"\"\n" +
                            "next: 1\n" +
                            "echo: echo-1\n" +
                            "# position: 36, header: 1\n" +
                            "--- !!data #binary\n" +
                            "midTwoArgs: [\n" +
                            "  5,\n" +
                            "  !byte -7\n" +
                            "]\n" +
                            "next: 2\n" +
                            "echo: echo-2\n",
                    WireDumper.of(wire).asString(),
                    "Expected wire output for varying argument counts");

            // Create a StringBuilder to collect method call representations.
            StringBuilder sb = new StringBuilder();

            ITop implementingOnlyITop = new ITop() {
                @Override
                public IMid mid(String name) {
                    throw new UnsupportedOperationException("not supported");
                }

                @Override
                public IMid2 mid2(String name) {
                    throw new UnsupportedOperationException("not supported");
                }

                @Override
                public IMid midNoArg() {
                    return Mocker.intercepting(IMid.class, "*", sb::append);
                }

                @Override
                public IMid midTwoArgs(int i, long l) {
                    return Mocker.intercepting(IMid.class, "*", sb::append);
                }
            };

            // Create a method reader to read method calls.
            MethodReader reader = wire.methodReader(implementingOnlyITop);
            assertTrue(reader.readOne(), "Expected first varied-args call");
            assertTrue(reader.readOne(), "Expected second varied-args call");

            // Validate the string representation of method calls.
            assertEquals("*next[1]*echo[echo-1]*next[2]*echo[echo-2]", sb.toString(),
                    "Expected varied-args call trace");
            assertFalse(reader.readOne(), "Expected no additional varied-args calls");
        });
    }

    // Test for nested return type in BinaryWire
    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    @DisplayName("Creates nested return type calls in BinaryWire")
    public void testNestedReturnType(boolean disableProxyCodegen) {
        withDisableProxyCodegen(disableProxyCodegen, () -> {
            if (disableProxyCodegen)
                expectException("Falling back to proxy method writer");

            // Create an instance of BinaryWire.
            Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
            wire.usePadding(true);
            final NestedStart writer = wire.methodWriter(NestedStart.class);

            // Check if the writer is a Proxy class, if the proxy codegen is disabled.
            assertEquals(disableProxyCodegen, Proxy.isProxyClass(writer.getClass()),
                    "Expected proxy status to match disableProxyCodegen=" + disableProxyCodegen);

            // Chain method calls on the writer.
            writer.start().end();

            // Validate the wire's representation using WireDumper.
            assertEquals("--- !!data #binary\n" +
                    "start: \"\"\n" +
                    "end: \"\"\n", WireDumper.of(wire).asString(),
                    "Expected nested start/end call output");
        });
    }

    // Interface defining the start of a nested call.
    interface NestedStart {
        NestedEnd start();
    }

    // Interface defining the end of a nested call.
    interface NestedEnd {
        void end();
    }

    private void withDisableProxyCodegen(boolean disableProxyCodegen, Runnable action) {
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, String.valueOf(disableProxyCodegen));
        try {
            action.run();
        } finally {
            System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
        }
    }
}
