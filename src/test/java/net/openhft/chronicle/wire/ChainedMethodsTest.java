/*
 * Copyright 2016-2022 chronicle.software
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
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_PROXY_REFLECTION;
import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

@RunWith(Parameterized.class)
public class ChainedMethodsTest extends WireTestCommon {

    // Parameter that will be injected by the Parameterized runner.
    @Parameterized.Parameter
    public boolean disableProxyCodegen;

    // Specifies the parameters to be used for the test runs.
    @Parameterized.Parameters(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    public static Collection<Object[]> data() {
        // Two sets of parameters: 'false' and 'true'
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    // Set up method that runs before each test execution.
    @Before
    public void setUp() {
        // Set a system property based on the current parameter value.
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, String.valueOf(disableProxyCodegen));
    }

    // Clean up method that runs after each test execution.
    @After
    public void cleanUp() {
        // Clear the system property that was set in the setup method.
        System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
    }

    // Test method for chained methods with TextWire.
    @Test
    public void chainedText() {
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
    @Test
    public void chainedYaml() {
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
    @Test
    public void chainedBinary() {
        // Assume the test should not run if the condition is true.
        assumeFalse("https://github.com/OpenHFT/Chronicle-Wire/issues/460", disableProxyCodegen);

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
    @Test
    public void chainedBinaryVariousArgsNumber() {
        // Assume the test should not run if the condition is true.
        assumeFalse("https://github.com/OpenHFT/Chronicle-Wire/issues/460", disableProxyCodegen);

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
    @Test
    public void testNestedReturnType() {
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
