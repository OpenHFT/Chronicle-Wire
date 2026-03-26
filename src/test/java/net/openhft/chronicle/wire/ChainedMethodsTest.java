/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class ChainedMethodsTest extends WireTestCommon {

    // Parameter that will be set per test invocation.
    private boolean disableProxyCodegen;

    // Specifies the parameters to be used for the test runs.
    public static Collection<Object[]> data() {
        // Two sets of parameters: 'false' and 'true'
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    // Set up method that runs before each test execution.
    public void setUp() {
        // Set a system property based on the current parameter value.
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, String.valueOf(disableProxyCodegen));
    }

    // Clean up method that runs after each test execution.
    @AfterEach
    void cleanUp() {
        // Clear the system property that was set in the setup method.
        System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
    }

    // Test method for chained methods with TextWire.
    @ParameterizedTest
    @MethodSource("data")
    void chainedText(boolean disableProxyCodegen) {
        this.disableProxyCodegen = disableProxyCodegen;
        setUp();

        if (disableProxyCodegen)
            expectException("Falling back to proxy method writer");

        // Create an instance of TextWire.
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap(128))
                .useTextDocuments();
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
                "...\n", wire.toString());

        // Create a StringBuilder to collect method call representations.
        StringBuilder sb = new StringBuilder();

        // Create a method reader to read method calls.
        MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        // Validate the string representation of method calls.
        assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    // Test method for chained methods with YAML Wire.
    @ParameterizedTest
    @MethodSource("data")
    void chainedYaml(boolean disableProxyCodegen) {
        this.disableProxyCodegen = disableProxyCodegen;
        setUp();

        if (disableProxyCodegen)
            expectException("Falling back to proxy method writer");

        // Create an instance of YamlWire.
        Wire wire = Wire.newYamlWireOnHeap();
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
                "...\n", wire.toString());

        // Create a StringBuilder to collect method call representations.
        StringBuilder sb = new StringBuilder();

        // Create a method reader to read method calls.
        MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        // Validate the string representation of method calls.
        assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    // Test for chained methods with BinaryWire
    @ParameterizedTest
    @MethodSource("data")
    void chainedBinary(boolean disableProxyCodegen) {
        this.disableProxyCodegen = disableProxyCodegen;
        setUp();

        // Assume the test should not run if the condition is true.
        assumeFalse(disableProxyCodegen, "https://github.com/OpenHFT/Chronicle-Wire/issues/460");

        // Create an instance of BinaryWire.
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);
        ITop top = wire.methodWriter(ITop.class);

        // Chain method calls on the created wire instance.
        top.mid("mid")
                .next(1)
                .echo("echo-1");
        assertEquals(34, wire.bytes().writePosition());
        top.mid2("mid2")
                .next2("word")
                .echo("echo-2");

        // Validate the wire's representation using WireDumper.
        assertEquals("" +
                "--- !!data #binary\n" +
                "mid: mid\n" +
                "next: 1\n" +
                "echo: echo-1\n" +
                "# position: 36, header: 1\n" +
                "--- !!data #binary\n" +
                "mid2: mid2\n" +
                "next2: word\n" +
                "echo: echo-2\n", WireDumper.of(wire).asString());

        // Create a StringBuilder to collect method call representations.
        StringBuilder sb = new StringBuilder();

        // Create a method reader to read method calls.
        MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        // Validate the string representation of method calls.
        assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    // Test for chained methods with BinaryWire and varying argument numbers
    @ParameterizedTest
    @MethodSource("data")
    void chainedBinaryVariousArgsNumber(boolean disableProxyCodegen) {
        this.disableProxyCodegen = disableProxyCodegen;
        setUp();

        // Assume the test should not run if the condition is true.
        assumeFalse(disableProxyCodegen, "https://github.com/OpenHFT/Chronicle-Wire/issues/460");

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
        assertEquals("" +
                        "--- !!data #binary\n" +
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
                WireDumper.of(wire).asString());

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
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        // Validate the string representation of method calls.
        assertEquals("*next[1]*echo[echo-1]*next[2]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    // Test for nested return type in BinaryWire
    @ParameterizedTest
    @MethodSource("data")
    void testNestedReturnType(boolean disableProxyCodegen) {
        this.disableProxyCodegen = disableProxyCodegen;
        setUp();

        if (disableProxyCodegen)
            expectException("Falling back to proxy method writer");

        // Create an instance of BinaryWire.
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);
        final NestedStart writer = wire.methodWriter(NestedStart.class);

        // Check if the writer is a Proxy class, if the proxy codegen is disabled.
        assertEquals(disableProxyCodegen, Proxy.isProxyClass(writer.getClass()));

        // Chain method calls on the writer.
        writer.start().end();

        // Validate the wire's representation using WireDumper.
        assertEquals("--- !!data #binary\n" +
                "start: \"\"\n" +
                "end: \"\"\n", WireDumper.of(wire).asString());
    }

    // Interface defining the start of a nested call.
    interface NestedStart {
        NestedEnd start();
    }

    // Interface defining the end of a nested call.
    interface NestedEnd {
        void end();
    }
}
