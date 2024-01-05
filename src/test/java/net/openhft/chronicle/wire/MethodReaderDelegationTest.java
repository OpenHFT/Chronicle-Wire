/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.TestCase.assertFalse;
import static net.openhft.chronicle.wire.VanillaMethodReaderBuilder.DISABLE_READER_PROXY_CODEGEN;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class MethodReaderDelegationTest extends WireTestCommon {
    private final boolean useMethodId;

    // Constructor to set parameter
    public MethodReaderDelegationTest(boolean useMethodId) {
        this.useMethodId = useMethodId;
    }

    // Define parameters for this parameterized test
    @Parameterized.Parameters(name = "useMethodId={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{false},
                new Object[]{true}
        );
    }

    // Testing unsuccessful call delegation with BinaryWire type
    @Test
    public void testUnsuccessfulCallIsDelegatedBinaryWire() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, false);
    }

    @Test
    public void testUnsuccessfulCallIsDelegatedBinaryWireScanning() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, true);
    }

    // Testing unsuccessful call delegation with TextWire type
    @Test
    public void testUnsuccessfulCallIsDelegatedTextWire() {
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, false);
    }

    @Test
    public void testUnsuccessfulCallIsDelegatedTextWireScanning() {
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, true);
    }

    // Testing unsuccessful call delegation with YamlWire type
    @Test
    public void testUnsuccessfulCallIsDelegatedYamlWire() {
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, false);
    }

    @Test
    public void testUnsuccessfulCallIsDelegatedYamlWireScanning() {
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, true);
    }

    // A helper method to test if unsuccessful method calls are properly delegated
    private void doTestUnsuccessfulCallIsDelegated(Wire wire, boolean scanning) {
        // Reset the wire and enable padding
        wire.reset();
        wire.usePadding(true);

        // Determine the appropriate interface class based on the useMethodId flag
        final Class<? extends MyInterface> ifaceClass = useMethodId ? MyInterfaceMethodId.class : MyInterface.class;

        // Create a method writer for the interface
        final MyInterface writer = wire.methodWriter(ifaceClass);
        // Ensure that the writer isn't a proxy class
        assertFalse(Proxy.isProxyClass(writer.getClass()));

        // Call the 'myCall' method on the writer
        writer.myCall();

        // Define the "fall" event ID and its string representation
        final int myFallId = 2;
        final String myFall = useMethodId ? Integer.toString(myFallId) : "myFall";

        // Write the "fall" event to the wire
        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            if (useMethodId) {
                Objects.requireNonNull(dc.wire()).writeEventId(myFallId).text("");
            } else
                Objects.requireNonNull(dc.wire()).writeEventName("myFall").text("");
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
        assertFalse(Proxy.isProxyClass(reader.getClass()));

        // Read and verify the first method from the wire
        assertTrue(reader.readOne());
        assertNull(delegatedMethodCall.get());

        // Read and verify the second method from the wire
        reader.readOne();
        assertEquals(myFall, delegatedMethodCall.get());

        // If scanning mode is enabled, verify that all methods have been read and that no unknown methods are left
        if (scanning) {
            assertEquals("*myCall[]*myCall[]", sb.toString());
            // unknown methods are skipped
            assertFalse(reader.readOne());
        } else {
            assertTrue(reader.readOne());
            assertEquals("*myCall[]*myCall[]", sb.toString());
        }

    }

    // Test case to ensure that unsuccessful calls are not delegated when certain conditions are met
    @Test
    public void testUnsuccessfulCallNoDelegate() {
        testUnsuccessfulCallNoDelegate(false, false, false);
    }

    // Test case (with scanning) to ensure that unsuccessful calls are not delegated when certain conditions are met
    @Test
    public void testUnsuccessfulCallNoDelegateScanning() {
        testUnsuccessfulCallNoDelegate(false, false, true);
    }

    // Test case (with proxy) to ensure that unsuccessful calls are not delegated when certain conditions are met
    @Test
    public void testUnsuccessfulCallNoDelegateProxy() {

        testUnsuccessfulCallNoDelegate(true, true, false);
    }

    // Test case (with proxy and scanning) to ensure that unsuccessful calls are not delegated when certain conditions are met
    @Test
    public void testUnsuccessfulCallNoDelegateProxyScanning() {
        testUnsuccessfulCallNoDelegate(true, true, true);
    }

    // Helper method to test that unsuccessful calls are not delegated under various configurations
    private void testUnsuccessfulCallNoDelegate(boolean proxy, boolean third, boolean scanning) {
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
                    .build(Mocker.intercepting(MyInterface.class, "*", sb::append));

            // Verify that the first method can be read
            assertTrue(reader.readOne());

            // Based on the scanning flag, handle the method reading logic accordingly
            if (scanning) {
                assertTrue(reader.readOne());
                assertEquals(third, reader.readOne());
                assertEquals("*myCall[]*myCall[]", sb.toString());
                assertFalse(reader.readOne());
            } else {
                reader.readOne();
                assertTrue(reader.readOne());
                assertFalse(reader.readOne());

                assertEquals("*myCall[]*myCall[]", sb.toString());
            }

        } finally {
            // Clear the system property to reset its original state
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    // Test to ensure that user exceptions are not delegated during method calls
    @Test
    public void testUserExceptionsAreNotDelegated() {
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

            throw new IllegalStateException("This is an exception by design");
        };

        // Set up the MethodReader to read methods from the wire and handle the designed exception
        final MethodReader reader = wire.methodReader(useMethodId ? (MyInterfaceMethodId) () -> myInterface.myCall() : myInterface);

        // Assert that an InvocationTargetRuntimeException is thrown when trying to read a method
        assertThrows(InvocationTargetRuntimeException.class, () -> reader.readOne());
    }

    // TODO: test below with interceptor

    // Test to verify that code generation can be disabled via system property
    @Test
    public void testCodeGenerationCanBeDisabled() {
        // Set system property to disable reader proxy code generation
        System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            // Initialize a wire with BINARY type and allocate space on the heap
            final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

            // Create a MethodReader with an empty implementation of MyInterface
            final MethodReader reader = wire.methodReader((MyInterface) () -> {
            });

            // Assert that the reader is an instance of VanillaMethodReader
            assertTrue(reader instanceof VanillaMethodReader);
        } finally {
            // Clear the system property to reset its original state
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    // Test that an exception is thrown from user code under standard conditions
    @Test
    public void testExceptionThrownFromUserCode() {
        testExceptionThrownFromUserCode(false);
    }

    // Test that an exception is thrown from user code when a proxy is used
    @Test
    public void testExceptionThrownFromUserCodeProxy() {
        testExceptionThrownFromUserCode(true);
    }

    // Helper method to test that an exception is thrown from user code
    private void testExceptionThrownFromUserCode(boolean proxy) throws InvocationTargetRuntimeException {
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
                throw new IllegalStateException("This is an exception by design");
            };
            // Set up the MethodReader to read methods from the wire
            final MethodReader reader = wire.methodReader(useMethodId ? (MyInterfaceMethodId) () -> myInterface.myCall() : myInterface);

            // Check if the reader is of type VanillaMethodReader when proxy is enabled
            assertEquals(proxy, reader instanceof VanillaMethodReader);

            // Assert that an InvocationTargetRuntimeException is thrown when trying to read a method
            assertThrows(InvocationTargetRuntimeException.class, () -> reader.readOne());
        } finally {
            // Clear the system property to reset its original state
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    // Test to verify exception handling in user code with long parameters
    @Test
    public void testExceptionThrownFromUserCodeLong() {
        testExceptionThrownFromUserCodeLong(false);
    }

    // Test to verify exception handling in user code with long parameters when a proxy is used
    @Test
    public void testExceptionThrownFromUserCodeLongProxy() {
        testExceptionThrownFromUserCodeLong(true);
    }

    // Helper method to test exception thrown from user code with long parameters
    private void testExceptionThrownFromUserCodeLong(boolean proxy) {
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
            final MyInterfaceLong myInterface = (l) -> {
                throw new IllegalStateException("This is an exception by design");
            };
            // Set up the MethodReader to read methods from the wire
            final MethodReader reader = wire.methodReader(useMethodId ? (MyInterfaceLongMethodId) (l) -> myInterface.myCall(l) : myInterface);

            // Check if the reader is of type VanillaMethodReader when proxy is enabled
            assertEquals(proxy, reader instanceof VanillaMethodReader);

            // Assert that an InvocationTargetRuntimeException is thrown when trying to read a method
            assertThrows(InvocationTargetRuntimeException.class, () -> reader.readOne());
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
        void myCall();
    }

    // Interface representing a method with a long parameter
    interface MyInterfaceLong {
        void myCall(long l);
    }

    // Extension of MyInterfaceLong but with a specific method ID
    interface MyInterfaceLongMethodId extends MyInterfaceLong {
        @MethodId(2)
        void myCall(long l);
    }
}
