/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN;
import static net.openhft.chronicle.wire.WireType.BINARY;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UpdateInterceptorReturnTypeTest extends WireTestCommon {

    // Data set for parameterized tests, providing true and false values for 'disableProxyCodegen'
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    // Creates and returns a new Wire instance with allocated memory
    private static Wire createWire() {
        return BINARY.apply(Bytes.allocateElasticOnHeap());
    }

    // Test to verify behavior with an interceptor on a method that has no return type
    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    @DisplayName("Interceptor writes data for void return type")
    void testUpdateInterceptorNoReturnType(boolean disableProxyCodegen) {
        withDisableProxyCodegen(disableProxyCodegen, () -> {
            final Wire wire = createWire();
            wire
                    .methodWriterBuilder(NoReturnType.class)
                    .updateInterceptor((methodName, t) -> true)
                    .build()
                    .x("hello world");
            assertEquals("--- !!data #binary\n" +
                            "x: hello world\n",
                    Wires.fromSizePrefixedBlobs(wire),
                    "Expected wire output for void return type, disableProxyCodegen=" + disableProxyCodegen);
        });
    }

    // Test to verify behavior with an interceptor on a method that has an integer return type
    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    @DisplayName("Interceptor returns default int and writes data")
    void testUpdateInterceptorWithIntReturnType(boolean disableProxyCodegen) {
        withDisableProxyCodegen(disableProxyCodegen, () -> {
            final Wire wire = createWire();
            int value = wire
                    .methodWriterBuilder(WithIntReturnType.class)
                    .updateInterceptor((methodName, t) -> true)
                    .build()
                    .x("hello world");
            assertEquals(0, value, "Expected default int return for disableProxyCodegen=" + disableProxyCodegen);
            assertEquals("--- !!data #binary\n" +
                            "x: hello world\n",
                    Wires.fromSizePrefixedBlobs(wire),
                    "Expected wire output for int return type, disableProxyCodegen=" + disableProxyCodegen);
        });
    }

    // Test to verify behavior with an interceptor on a method that has an object return type
    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    @DisplayName("Interceptor returns writer for object return type")
    void testUpdateInterceptorWithObjectReturnType(boolean disableProxyCodegen) {
        withDisableProxyCodegen(disableProxyCodegen, () -> {
            final Wire wire = createWire();
            final WithObjectReturnType mw = wire
                    .methodWriterBuilder(WithObjectReturnType.class)
                    .updateInterceptor((methodName, t) -> true)
                    .build();
            Object value = mw.x("hello world");
            assertSame(mw, value, "Expected object return to be the writer instance");
            assertEquals(disableProxyCodegen, Proxy.isProxyClass(mw.getClass()),
                    "Expected proxy status to match disableProxyCodegen=" + disableProxyCodegen);
            assumeFalse(disableProxyCodegen, "Proxy codegen disabled; skip not-ready-data assertion");

            // Here, data is written but is on hold until the end of the message is written.
            // WireDumper no longer scans data that is written but not ready
            assertEquals("--- !!not-ready-data\n" +
                            "...\n" +
                            "# 15 bytes remaining\n",
                    Wires.fromSizePrefixedBlobs(wire),
                    "Expected not-ready-data marker before second call");

            mw.y("good byte");
            assertEquals("--- !!data #binary\n" +
                            "x: hello world\n" +
                            "y: good byte\n",
                    Wires.fromSizePrefixedBlobs(wire),
                    "Expected wire output after second call with x and y fields");
        });
    }

    // Test to verify the behavior of an interceptor on a method from the LadderByQtyListener interface
    @MethodSource("data")
    @ParameterizedTest(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    @DisplayName("Interceptor writes ladderByQty updates for listener")
    void testUpdateInterceptorWithLadderByQtyListener(boolean disableProxyCodegen) {
        withDisableProxyCodegen(disableProxyCodegen, () -> {
            final Wire wire = createWire();
            wire
                    .methodWriterBuilder(LadderByQtyListener.class)
                    .updateInterceptor((methodName, t) -> true)
                    .build()
                    .ladderByQty("a ladder");
            assertEquals("--- !!data #binary\n" +
                            "ladderByQty: a ladder\n",
                    Wires.fromSizePrefixedBlobs(wire),
                    "Expected wire output for ladderByQty, disableProxyCodegen=" + disableProxyCodegen);
        });
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

    private void withDisableProxyCodegen(boolean disableProxyCodegen, Runnable action) {
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, String.valueOf(disableProxyCodegen));
        try {
            if (disableProxyCodegen)
                expectException("Falling back to proxy method writer");
            action.run();
        } finally {
            System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
        }
    }
}
