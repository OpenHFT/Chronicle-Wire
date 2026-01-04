/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static net.openhft.chronicle.wire.VanillaMethodReaderBuilder.DISABLE_READER_PROXY_CODEGEN;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"deprecation", "removal"})
public class MethodReaderDelegationTest extends WireTestCommon {
    private boolean useMethodId;

    // Constructor to set parameter
    public void initMethodReaderDelegationTest(boolean useMethodId) {
        this.useMethodId = useMethodId;
    }

    // Define parameters for this parameterized test
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{false},
                new Object[]{true}
        );
    }

    // Testing unsuccessful call delegation with BinaryWire type
    @DisplayName("Method reader delegation Unsuccessful Call Is Delegated Binary Wire")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call Is Delegated Binary Wire uses method id {0}")
    public void testUnsuccessfulCallIsDelegatedBinaryWire(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        assertEquals("*myCall[]*myCall[]", doTestUnsuccessfulCallIsDelegated(wire, false),
                "BinaryWire delegated call log (useMethodId=" + useMethodId + ", scanning=false)");
    }

    @DisplayName("Method reader delegation Unsuccessful Call Is Delegated Binary Wire Scanning")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call Is Delegated Binary Wire Scanning uses method id {0}")
    public void testUnsuccessfulCallIsDelegatedBinaryWireScanning(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        assertEquals("*myCall[]*myCall[]", doTestUnsuccessfulCallIsDelegated(wire, true),
                "BinaryWire delegated call log (useMethodId=" + useMethodId + ", scanning=true)");
    }

    // Testing unsuccessful call delegation with TextWire type
    @DisplayName("Method reader delegation Unsuccessful Call Is Delegated Text Wire")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call Is Delegated Text Wire uses method id {0}")
    public void testUnsuccessfulCallIsDelegatedTextWire(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        assertEquals("*myCall[]*myCall[]", doTestUnsuccessfulCallIsDelegated(wire, false),
                "TextWire delegated call log (useMethodId=" + useMethodId + ", scanning=false)");
    }

    @DisplayName("Method reader delegation Unsuccessful Call Is Delegated Text Wire Scanning")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call Is Delegated Text Wire Scanning uses method id {0}")
    public void testUnsuccessfulCallIsDelegatedTextWireScanning(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        assertEquals("*myCall[]*myCall[]", doTestUnsuccessfulCallIsDelegated(wire, true),
                "TextWire delegated call log (useMethodId=" + useMethodId + ", scanning=true)");
    }

    // Testing unsuccessful call delegation with YamlWire type
    @DisplayName("Method reader delegation Unsuccessful Call Is Delegated Yaml Wire")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call Is Delegated Yaml Wire uses method id {0}")
    public void testUnsuccessfulCallIsDelegatedYamlWire(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        assertEquals("*myCall[]*myCall[]", doTestUnsuccessfulCallIsDelegated(wire, false),
                "YamlWire delegated call log (useMethodId=" + useMethodId + ", scanning=false)");
    }

    @DisplayName("Method reader delegation Unsuccessful Call Is Delegated Yaml Wire Scanning")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call Is Delegated Yaml Wire Scanning uses method id {0}")
    public void testUnsuccessfulCallIsDelegatedYamlWireScanning(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        assertEquals("*myCall[]*myCall[]", doTestUnsuccessfulCallIsDelegated(wire, true),
                "YamlWire delegated call log (useMethodId=" + useMethodId + ", scanning=true)");
    }

    // A helper method to test if unsuccessful method calls are properly delegated
    private String doTestUnsuccessfulCallIsDelegated(Wire wire, boolean scanning) {
        ignoreException("Unknown method-name='myFall' called on class");
        // Reset the wire and enable padding
        wire.reset();
        wire.usePadding(true);

        // Determine the appropriate interface class based on the useMethodId flag
        final Class<? extends MyInterface> ifaceClass = useMethodId ? MyInterfaceMethodId.class : MyInterface.class;

        // Create a method writer for the interface
        final MyInterface writer = wire.methodWriter(ifaceClass);
        // Ensure that the writer isn't a proxy class
        assertFalse(Proxy.isProxyClass(writer.getClass()),
                "Writer should be concrete for " + ifaceClass.getSimpleName() + " (useMethodId=" + useMethodId + ")");

        // Call the 'myCall' method on the writer
        writer.myCall();

        // Define the "fall" event ID and its string representation
        final int myFallId = 2;
        final String myFall = useMethodId ? Integer.toString(myFallId) : "myFall";

        // Write the "fall" event to the wire
        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            if (useMethodId) {
                Objects.requireNonNull(dc.wire()).writeEventId(myFallId).text("");
            } else {
                Objects.requireNonNull(dc.wire()).writeEventName("myFall").text("");
            }
        }

        // Call the 'myCall' method on the writer again
        writer.myCall();

        // Use AtomicReference to capture any delegated method calls and a StringBuilder to capture method logs
        AtomicReference<String> delegatedMethodCall = new AtomicReference<>();
        StringBuilder sb = new StringBuilder();

        // Set up the MethodReader to read methods from the wire
        final MethodReader reader = wire.methodReaderBuilder()
                .scanning(scanning)
                .defaultParselet((s, in) -> {
                    // Store the method name when a method is read from the wire
                    delegatedMethodCall.set(s.toString());
                    in.skipValue();
                })
                .build(Mocker.intercepting(ifaceClass, "*", sb::append));

        // Ensure the reader isn't a proxy class
        assertFalse(Proxy.isProxyClass(reader.getClass()),
                "Reader should be concrete (scanning=" + scanning + ", useMethodId=" + useMethodId + ")");

        // Read and verify the first method from the wire
        assertTrue(reader.readOne(),
                "Expected first call to be read (scanning=" + scanning + ", useMethodId=" + useMethodId + ")");
        assertNull(delegatedMethodCall.get(),
                "No delegated method should be recorded after first read (scanning=" + scanning
                        + ", useMethodId=" + useMethodId + ")");

        // Read and verify the second method from the wire
        reader.readOne();
        assertEquals(myFall, delegatedMethodCall.get(),
                "Unknown method name should be recorded for default parselet (scanning=" + scanning
                        + ", useMethodId=" + useMethodId + ")");

        // If scanning mode is enabled, verify that all methods have been read and that no unknown methods are left
        if (scanning) {
            // unknown methods are skipped
            assertFalse(reader.readOne(),
                    "Scanning should exhaust after unknown method (useMethodId=" + useMethodId + ")");
        } else {
            assertTrue(reader.readOne(),
                    "Non-scanning should read final call after unknown method (useMethodId=" + useMethodId + ")");
        }
        return sb.toString();
    }

    // Test case to ensure that unsuccessful calls are not delegated when certain conditions are met
    @DisplayName("Method reader delegation Unsuccessful Call No Delegate")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call No Delegate uses method id {0}")
    public void testUnsuccessfulCallNoDelegate(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        assertEquals("*myCall[]*myCall[]", testUnsuccessfulCallNoDelegate(false, false, false),
                "No-delegate call log (proxy=false, third=false, scanning=false, useMethodId=" + useMethodId + ")");
    }

    // Test case (with scanning) to ensure that unsuccessful calls are not delegated when certain conditions are met
    @DisplayName("Method reader delegation Unsuccessful Call No Delegate Scanning")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call No Delegate Scanning uses method id {0}")
    public void testUnsuccessfulCallNoDelegateScanning(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        assertEquals("*myCall[]*myCall[]", testUnsuccessfulCallNoDelegate(false, false, true),
                "No-delegate call log (proxy=false, third=false, scanning=true, useMethodId=" + useMethodId + ")");
    }

    // Test case (with proxy) to ensure that unsuccessful calls are not delegated when certain conditions are met
    @DisplayName("Method reader delegation Unsuccessful Call No Delegate Proxy")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call No Delegate Proxy uses method id {0}")
    public void testUnsuccessfulCallNoDelegateProxy(boolean useMethodId) {

        initMethodReaderDelegationTest(useMethodId);

        assertEquals("*myCall[]*myCall[]", testUnsuccessfulCallNoDelegate(true, true, false),
                "No-delegate call log (proxy=true, third=true, scanning=false, useMethodId=" + useMethodId + ")");
    }

    // Test case (with proxy and scanning) to ensure that unsuccessful calls are not delegated when certain conditions are met
    @DisplayName("Method reader delegation Unsuccessful Call No Delegate Proxy Scanning")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Unsuccessful Call No Delegate Proxy Scanning uses method id {0}")
    public void testUnsuccessfulCallNoDelegateProxyScanning(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        assertEquals("*myCall[]*myCall[]", testUnsuccessfulCallNoDelegate(true, true, true),
                "No-delegate call log (proxy=true, third=true, scanning=true, useMethodId=" + useMethodId + ")");
    }

    // Helper method to test that unsuccessful calls are not delegated under various configurations
    private String testUnsuccessfulCallNoDelegate(boolean proxy, boolean third, boolean scanning) {
        // If proxy is enabled, set the system property to disable reader proxy code generation
        if (proxy)
            System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            // Initialize a wire with TEXT type and allocate space on the heap
            final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

            // Create a method writer for the MyInterface
            final MyInterface writer = wire.methodWriter(MyInterface.class);
            // Call the 'myCall' method on the writer
            writer.myCall();

            // Write the "fall" event to the wire
            try (DocumentContext dc = wire.acquireWritingDocument(false)) {
                Objects.requireNonNull(dc.wire()).writeEventName("myFall").text("");
            }

            // Call the 'myCall' method on the writer again
            writer.myCall();

            // StringBuilder to capture method logs
            StringBuilder sb = new StringBuilder();
            // Set up the MethodReader to read methods from the wire
            final MethodReader reader = wire.methodReaderBuilder()
                    .scanning(scanning)
                    .exceptionHandlerOnUnknownMethod(Jvm.debug())
                    .build(Mocker.intercepting(MyInterface.class, "*", sb::append));

            // Verify that the first method can be read
            assertTrue(reader.readOne(),
                    "Expected first call to be read (proxy=" + proxy + ", scanning=" + scanning + ")");

            // Based on the scanning flag, handle the method reading logic accordingly
            if (scanning) {
                assertTrue(reader.readOne(),
                        "Expected unknown method to be skipped (proxy=" + proxy + ", scanning=true)");
                assertEquals(third, reader.readOne(),
                        "Expected third read result to match third=" + third + " (proxy=" + proxy + ", scanning=true)");
                assertFalse(reader.readOne(),
                        "Expected no more calls after third read (proxy=" + proxy + ", scanning=true)");
            } else {
                reader.readOne();
                assertTrue(reader.readOne(),
                        "Expected trailing call after unknown method (proxy=" + proxy + ", scanning=false)");
                assertFalse(reader.readOne(),
                        "Expected no more calls after trailing call (proxy=" + proxy + ", scanning=false)");
            }

            return sb.toString();
        } finally {
            // Clear the system property to reset its original state
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    // Test to ensure that user exceptions are not delegated during method calls
    @DisplayName("Method reader delegation User Exceptions Are Not Delegated")
    @MethodSource("data")
    @SuppressWarnings("deprecation")
    @ParameterizedTest(name = "Method reader delegation User Exceptions Are Not Delegated uses method id {0}")
    public void testUserExceptionsAreNotDelegated(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        // Initialize a wire with BINARY type and allocate space on the heap
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // Determine the appropriate interface class based on the useMethodId flag
        final Class<? extends MyInterface> ifaceClass = useMethodId ? MyInterfaceMethodId.class : MyInterface.class;
        // Create a method writer for the determined interface class
        final MyInterface writer = wire.methodWriter(ifaceClass);

        // Call the 'myCall' method on the writer
        writer.myCall();

        // AtomicInteger to count the number of exceptions thrown
        AtomicInteger exceptionsThrown = new AtomicInteger();

        // Create an instance of MyInterface that throws a designed exception when called
        final MyInterface myInterface = () -> {
            exceptionsThrown.incrementAndGet();

            throw new IllegalStateException("Deliberate user exception in no-arg call test");
        };

        // Set up the MethodReader to read methods from the wire and handle the designed exception
        final MethodReader reader = wire.methodReader(useMethodId ? (MyInterfaceMethodId) myInterface::myCall : myInterface);

        // Assert that an InvocationTargetRuntimeException is thrown when trying to read a method
        assertThrows(InvocationTargetRuntimeException.class, reader::readOne,
                "User exception should propagate as InvocationTargetRuntimeException (useMethodId=" + useMethodId + ")");
    }

    // TODO: test below with interceptor

    // Test to verify that code generation can be disabled via system property
    @DisplayName("Method reader delegation Code Generation Can Be Disabled")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Code Generation Can Be Disabled uses method id {0}")
    public void testCodeGenerationCanBeDisabled(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        // Set system property to disable reader proxy code generation
        System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            // Initialize a wire with BINARY type and allocate space on the heap
            final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

            // Create a MethodReader with an empty implementation of MyInterface
            final MethodReader reader = wire.methodReader((MyInterface) () -> {
            });

            // Assert that the reader is an instance of VanillaMethodReader
            assertInstanceOf(VanillaMethodReader.class, reader,
                    "DISABLE_READER_PROXY_CODEGEN should force VanillaMethodReader (useMethodId=" + useMethodId + ")");
        } finally {
            // Clear the system property to reset its original state
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    // Test that an exception is thrown from user code under standard conditions
    @DisplayName("Method reader delegation Exception Thrown From User Code")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Exception Thrown From User Code uses method id {0}")
    public void testExceptionThrownFromUserCode(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        assertFalse(isVanillaMethodReaderWhenUserExceptionThrown(false),
                "User exception should not force VanillaMethodReader (proxy=false, useMethodId=" + useMethodId + ")");
    }

    // Test that an exception is thrown from user code when a proxy is used
    @DisplayName("Method reader delegation Exception Thrown From User Code Proxy")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Exception Thrown From User Code Proxy uses method id {0}")
    public void testExceptionThrownFromUserCodeProxy(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        assertTrue(isVanillaMethodReaderWhenUserExceptionThrown(true),
                "User exception should force VanillaMethodReader when proxy disabled (useMethodId=" + useMethodId + ")");
    }

    // Helper method to test that an exception is thrown from user code
    private boolean isVanillaMethodReaderWhenUserExceptionThrown(boolean proxy) throws InvocationTargetRuntimeException {
        // If proxy is enabled, set the system property to disable reader proxy code generation
        if (proxy)
            System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            // Initialize a wire with TEXT type and allocate space on the heap
            final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

            // Determine the appropriate interface class based on the useMethodId flag
            final Class<? extends MyInterface> ifaceClass = useMethodId ? MyInterfaceMethodId.class : MyInterface.class;

            // Create a method writer for the determined interface class
            final MyInterface writer = wire.methodWriter(ifaceClass);
            // Call the 'myCall' method on the writer
            writer.myCall();

            // Create an instance of MyInterface that throws a designed exception when called
            final MyInterface myInterface = () -> {
                throw new IllegalStateException("Deliberate user exception in proxy detection test");
            };
            // Set up the MethodReader to read methods from the wire
            final MethodReader reader = wire.methodReader(useMethodId ? (MyInterfaceMethodId) myInterface::myCall : myInterface);

            boolean isVanillaMethodReader = reader instanceof VanillaMethodReader;

            // Assert that an InvocationTargetRuntimeException is thrown when trying to read a method
            assertThrows(InvocationTargetRuntimeException.class, reader::readOne,
                    "User exception should surface as InvocationTargetRuntimeException (proxy=" + proxy
                            + ", useMethodId=" + useMethodId + ")");
            return isVanillaMethodReader;
        } finally {
            // Clear the system property to reset its original state
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    // Test to verify exception handling in user code with long parameters
    @DisplayName("Method reader delegation Exception Thrown From User Code Long")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Exception Thrown From User Code Long uses method id {0}")
    public void testExceptionThrownFromUserCodeLong(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        assertFalse(isVanillaMethodReaderWhenUserExceptionThrownLong(false),
                "User exception should not force VanillaMethodReader for long call (proxy=false, useMethodId="
                        + useMethodId + ")");
    }

    // Test to verify exception handling in user code with long parameters when a proxy is used
    @DisplayName("Method reader delegation Exception Thrown From User Code Long Proxy")
    @MethodSource("data")
    @ParameterizedTest(name = "Method reader delegation Exception Thrown From User Code Long Proxy uses method id {0}")
    public void testExceptionThrownFromUserCodeLongProxy(boolean useMethodId) {
        initMethodReaderDelegationTest(useMethodId);
        assertTrue(isVanillaMethodReaderWhenUserExceptionThrownLong(true),
                "User exception should force VanillaMethodReader for long call when proxy disabled (useMethodId="
                        + useMethodId + ")");
    }

    // Helper method to test exception thrown from user code with long parameters
    private boolean isVanillaMethodReaderWhenUserExceptionThrownLong(boolean proxy) {
        // If proxy is enabled, set the system property to disable reader proxy code generation
        if (proxy)
            System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            // Initialize a wire with TEXT type and allocate space on the heap
            final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

            // Determine the appropriate interface class based on the useMethodId flag and it being a long
            final Class<? extends MyInterfaceLong> ifaceClass = useMethodId ? MyInterfaceLongMethodId.class : MyInterfaceLong.class;

            // Create a method writer for the determined interface class
            final MyInterfaceLong writer = wire.methodWriter(ifaceClass);
            // Call the 'myCall' method on the writer with a long parameter
            writer.myCall(1L);

            // Create an instance of MyInterfaceLong that throws a designed exception when called
            final MyInterfaceLong myInterface = l -> {
                throw new IllegalStateException("Deliberate user exception in long call test");
            };
            // Set up the MethodReader to read methods from the wire
            final MethodReader reader = wire.methodReader(useMethodId ? (MyInterfaceLongMethodId) myInterface::myCall : myInterface);

            boolean isVanillaMethodReader = reader instanceof VanillaMethodReader;

            // Assert that an InvocationTargetRuntimeException is thrown when trying to read a method
            assertThrows(InvocationTargetRuntimeException.class, reader::readOne,
                    "User exception should surface as InvocationTargetRuntimeException for long call (proxy=" + proxy
                            + ", useMethodId=" + useMethodId + ")");
            return isVanillaMethodReader;
        } finally {
            // Clear the system property to reset its original state
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    // Interface representing a method without parameters
    interface MyInterface {
        void myCall();
    }

    // Extension of MyInterface but with a specific method ID
    interface MyInterfaceMethodId extends MyInterface {
        @MethodId(1)
        @Override
        void myCall();
    }

    // Interface representing a method with a long parameter
    interface MyInterfaceLong {
        void myCall(long l);
    }

    // Extension of MyInterfaceLong but with a specific method ID
    interface MyInterfaceLongMethodId extends MyInterfaceLong {
        @MethodId(2)
        @Override
        void myCall(long l);
    }
}
