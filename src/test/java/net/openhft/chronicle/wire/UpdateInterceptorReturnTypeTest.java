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
import static net.openhft.chronicle.wire.WireType.BINARY;
import static org.junit.Assume.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@RunWith(Parameterized.class)
public class UpdateInterceptorReturnTypeTest extends WireTestCommon {

    // Parameterized value to determine if proxy code generation is disabled
    @Parameterized.Parameter
    public boolean disableProxyCodegen;

    // Data set for parameterized tests, providing true and false values for 'disableProxyCodegen'
    @Parameterized.Parameters(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    // Creates and returns a new Wire instance with allocated memory
    static Wire createWire() {
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());
        return wire;
    }

    // Before each test execution, set the appropriate system property and
    // expect a specific exception if proxy code generation is disabled
    @Before
    public void setUp() {
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, String.valueOf(disableProxyCodegen));
        System.setProperty(DISABLE_PROXY_REFLECTION, String.valueOf(!disableProxyCodegen));
        if (disableProxyCodegen)
            expectException("Falling back to proxy method writer");
    }

    // After each test execution, clean up by clearing the system property
    @After
    public void cleanUp() {
        System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
    }

    // Test to verify behavior with an interceptor on a method that has no return type
    @Test
    public void testUpdateInterceptorNoReturnType() {

        final Wire wire = createWire();
        wire
                .methodWriterBuilder(NoReturnType.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");
        assertEquals("" +
                        "--- !!data #binary\n" +
                        "x: hello world\n",
                Wires.fromSizePrefixedBlobs(wire));
    }

    // Test to verify behavior with an interceptor on a method that has an integer return type
    @Test
    public void testUpdateInterceptorWithIntReturnType() {
        final Wire wire = createWire();
        int value = wire
                .methodWriterBuilder(WithIntReturnType.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");
        assertEquals(0, value);
        assertEquals("" +
                        "--- !!data #binary\n" +
                        "x: hello world\n",
                Wires.fromSizePrefixedBlobs(wire));
    }

    // Test to verify behavior with an interceptor on a method that has an object return type
    @Test
    public void testUpdateInterceptorWithObjectReturnType() {
        final Wire wire = createWire();
        final WithObjectReturnType mw = wire
                .methodWriterBuilder(WithObjectReturnType.class)
                .updateInterceptor((methodName, t) -> true)
                .build();
        Object value = mw.x("hello world");
        assertSame(mw, value);
        assertEquals(disableProxyCodegen, Proxy.isProxyClass(mw.getClass()));
        assumeFalse(disableProxyCodegen);

        // Here, data is written but is on hold until the end of the message is written.
        // WireDumper no longer scans data that is written but not ready
        assertEquals("" +
                        "--- !!not-ready-data\n" +
                        "...\n" +
                        "# 15 bytes remaining\n",
                Wires.fromSizePrefixedBlobs(wire));

        mw.y("good byte");
        assertEquals("" +
                        "--- !!data #binary\n" +
                        "x: hello world\n" +
                        "y: good byte\n",
                Wires.fromSizePrefixedBlobs(wire));
    }

    // Test to verify the behavior of an interceptor on a method from the LadderByQtyListener interface
    @Test
    public void testUpdateInterceptorWithLadderByQtyListener() {
        final Wire wire = createWire();
        wire
                .methodWriterBuilder(LadderByQtyListener.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .ladderByQty("a ladder");
        assertEquals("" +
                        "--- !!data #binary\n" +
                        "ladderByQty: a ladder\n",
                Wires.fromSizePrefixedBlobs(wire));
    }

    // Interface that represents a listener for 'LadderByQty' updates
    public interface LadderByQtyListener {
        // Declares an action to perform when a ladder update is received
        void ladderByQty(String ladder);

        // Default method to provide a shorthand for 'ladderByQty' with an additional argument
        default void lbq(String name, String ladder) {
            ladderByQty(ladder);
        }

        // Default method to potentially ignore certain methods based on the first argument.
        // The current implementation does not ignore any method, but this can be customized.
        default boolean ignoreMethodBasedOnFirstArg(String methodName, String ladderDefinitionName) {
            return false;
        }
    }

    // Interface that represents an action without any return type
    interface NoReturnType {
        void x(String x);
    }

    // Interface that represents an action with an integer return type
    interface WithIntReturnType {
        int x(String x);
    }

    // Interface that represents an action with an object return type
    interface WithObjectReturnType {
        Object x(String x);

        void y(String y);
    }

    // Interface that represents an action with a 'Void' return type
    // Note: 'Void' is different from 'void'. 'Void' can be used when you need a generic type
    // that represents "no return value", while 'void' is a basic keyword indicating the absence of a return value.
    interface WithObjectVoidReturnType {
        Void x(String x);
    }
}
