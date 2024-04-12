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
import net.openhft.chronicle.bytes.Invocation;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class MethodReaderInterceptorReturnsTest extends WireTestCommon {

    // Setting up before the tests
    @Override
    @Before
    public void threadDump() {
        super.threadDump();
    }

    /**
     * This test ensures that the standard MethodReaderInterceptorReturns works with
     * the generated method reader.
     */
    @Test
    public void testInterceptorSupportedInGeneratedCode() {
        doTestInterceptorSupportedInGeneratedCode(new CountDownLatch(1), false);
    }

    /**
     * This test checks the behavior when multiple intercepting method readers
     * are created at the same time.
     */
    @Test
    public void testInterceptingReaderConcurrentCreation() throws ExecutionException, InterruptedException, TimeoutException {
        int concurrencyLevel = 5;

        // Creating a fixed-size thread pool to simulate concurrency
        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);

        try {
            final CountDownLatch readerCreateLatch = new CountDownLatch(concurrencyLevel);

            List<Future<?>> futureList = new ArrayList<>();

            // Submitting tasks for concurrent execution
            for (int i = 0; i < concurrencyLevel; i++) {
                futureList.add(executor.submit(() -> doTestInterceptorSupportedInGeneratedCode(
                        readerCreateLatch, true)));
            }

            // Waiting for all tasks to finish
            for (Future<?> future : futureList) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private void doTestInterceptorSupportedInGeneratedCode(CountDownLatch readerCreateLatch, boolean addDummyInstance) {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        InterceptedInterface writer = wire.methodWriter(InterceptedInterface.class);

        final StringBuilder interceptedMethodNames = new StringBuilder();

        final AtomicReference<Exception> interceptorError = new AtomicReference<>();

        // Defining the interceptor
        final MethodReaderInterceptorReturns interceptor = (m, o, args, invocation) -> {
            try {
                interceptedMethodNames.append(m.getName()).append(Arrays.toString(args)).append("*");

                return invocation.invoke(m, o, args);
            } catch (Exception e) {
                interceptorError.set(e);

                e.printStackTrace();

                return null;
            }
        };

        final InterceptedInterface impl = new InterceptedInterfaceImpl(new StringBuilder(), false);

        readerCreateLatch.countDown();
        try {
            readerCreateLatch.await();
        } catch (InterruptedException e) {
            fail("Failed to wait for reader create latch");
        }

        final MethodReader reader;
        if (addDummyInstance) { // To ensure recompilation in different tests
            reader = wire.methodReaderBuilder()
                    .methodReaderInterceptorReturns(interceptor)
                    .build(impl, (Supplier<String>) () -> null);
        } else {
            reader = wire.methodReaderBuilder()
                    .methodReaderInterceptorReturns(interceptor)
                    .build(impl);
        }

        assertFalse(reader instanceof VanillaMethodReader);

        writer.noArgs();
        writer.oneArg(2);
        writer.twoArgs("dd", 3).end();

        // Reading from the wire and asserting that all reads are successful
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        // If the interceptor encounters any error, the test fails
        if (interceptorError.get() != null)
            fail("Failed to execute interceptor code: " + interceptorError.get().toString());

        // Asserting that the intercepted methods are as expected
        assertEquals("noArgs[]*oneArg[2]*twoArgs[dd, 3]*end[]*", interceptedMethodNames.toString());
    }

    /**
     * Covers {@link GeneratingMethodReaderInterceptorReturns}, which allows intervening in method reader's logic
     * without resorting to reflective calls.
     */
    @Test
    public void testGeneratingAggregatingInfoInterceptor() {
        // Create a wire with a buffer of size 128 and padding enabled
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        // Obtain a writer instance for the InterceptedInterface
        InterceptedInterface writer = wire.methodWriter(InterceptedInterface.class);

        // Instantiate the interceptor responsible for aggregating information
        final AggregatingInfoInterceptor interceptor = new AggregatingInfoInterceptor();

        // Create an implementation instance of InterceptedInterface
        final InterceptedInterfaceImpl impl = new InterceptedInterfaceImpl(new StringBuilder(), false);

        // Build the MethodReader with the specified interceptor
        final MethodReader reader = wire.methodReaderBuilder()
                .methodReaderInterceptorReturns(interceptor)
                .build(impl);

        // Assert that the reader is not of type VanillaMethodReader
        assertFalse(reader instanceof VanillaMethodReader);

        // Write sample data to the writer
        writer.noArgs();
        writer.oneArg(2);
        writer.twoArgs("dd", 3).end();

        // Read the data written above
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        // Verify the aggregated information from the interceptor
        assertEquals("someCall*twoArgs[dd, 3]*someCall*", impl.info());
    }

    /**
     * Covers {@link GeneratingMethodReaderInterceptorReturns}, which allows intervening in method reader's logic
     * without resorting to reflective calls.
     */
    @Test
    public void testGeneratingSkippingInterceptor() {
        // Create a wire with a buffer of size 128 and padding enabled
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        // Obtain a writer instance for the InterceptedInterface
        InterceptedInterface writer = wire.methodWriter(InterceptedInterface.class);

        // Instantiate the interceptor responsible for skipping certain calls
        final SkippingInterceptor interceptor = new SkippingInterceptor();

        // Create an implementation instance of InterceptedInterface with skipping enabled
        final InterceptedInterfaceImpl impl = new InterceptedInterfaceImpl(new StringBuilder(), true);

        // Build the MethodReader with the specified interceptor
        final MethodReader reader = wire.methodReaderBuilder()
                .methodReaderInterceptorReturns(interceptor)
                .build(impl);

        // Assert that the reader is not of type VanillaMethodReader
        assertFalse(reader instanceof VanillaMethodReader);

        // Write sample data to the writer, including one with a null argument
        writer.twoArgs("dd", 3).end();
        writer.twoArgs(null, 4).end();

        // Read the data written above
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        // Assert that the call with a null argument was skipped
        // as per the logic of SkippingInterceptor
        assertEquals("twoArgs[dd, 3]*", impl.info());
    }

    // Interfaces defining the behavior for the test
    interface InterceptedInterface {
        void noArgs();

        void oneArg(int x);

        InterceptedChained twoArgs(String s, int x);
    }

    interface InterceptedChained {
        void end();
    }

    interface AggregatingInfo {
        void appendInfo(String info);

        String info();
    }

    // Implementation of InterceptedInterface and AggregatingInfo to test the interception process
    static class InterceptedInterfaceImpl implements InterceptedInterface, AggregatingInfo {
        private final StringBuilder info; // Storage for aggregated information
        private final boolean addInfoOnTwoArgs; // Flag to determine if info should be added when twoArgs method is called

        // Constructor initializes the StringBuilder for info and flag for twoArgs method
        public InterceptedInterfaceImpl(StringBuilder info, boolean addInfoOnTwoArgs) {
            this.info = info;
            this.addInfoOnTwoArgs = addInfoOnTwoArgs;
        }

        // Append additional information to the StringBuilder
        @Override
        public void appendInfo(String info) {
            this.info.append(info);
        }

        // Return the aggregated information as a string
        @Override
        public String info() {
            return info.toString();
        }

        // Implementation for noArgs, does nothing in this mock
        @Override
        public void noArgs() {
        }

        // Implementation for oneArg, does nothing in this mock
        @Override
        public void oneArg(int x) {
        }

        // Implementation for twoArgs, may append info based on the flag
        @Override
        public InterceptedChained twoArgs(String s, int x) {
            if (addInfoOnTwoArgs)
                info.append("twoArgs[").append(s).append(", ").append(x).append("]*");

            return new InterceptedChainedImpl(info);
        }
    }

    // Implementation of InterceptedChained and AggregatingInfo, acts as the next step in the chained calls
    static class InterceptedChainedImpl implements InterceptedChained, AggregatingInfo {
        private final StringBuilder info; // Storage for aggregated information

        // Constructor initializes the StringBuilder for info
        public InterceptedChainedImpl(StringBuilder info) {
            this.info = info;
        }

        // Append additional information to the StringBuilder
        @Override
        public void appendInfo(String info) {
            this.info.append(info);
        }

        // Return the aggregated information as a string
        @Override
        public String info() {
            return info.toString();
        }

        // Implementation for end, does nothing in this mock
        @Override
        public void end() {
        }
    }

    // Interceptor class that adds aggregated information to the intercepted methods
    static class AggregatingInfoInterceptor implements GeneratingMethodReaderInterceptorReturns {

        // Return the unique identifier for this generator
        @Override
        public String generatorId() {
            return "aggregatingInfo";
        }

        // Inject code before the actual method call
        // Currently, it checks if the method is "oneArg" and has an argument value of 2, then breaks
        @Override
        public String codeBeforeCall(Method m, String objectName, String[] argumentNames) {
            if (m.getName().equals("oneArg")) {
                return String.format("if (%s == 2)\n" +
                        "break;\n", argumentNames[0]);
            }

            return null;
        }

        // Inject code after the actual method call
        // Adds specific info for the method "twoArgs" or a general "someCall*" otherwise
        @Override
        public String codeAfterCall(Method m, String objectName, String[] argumentNames) {
            if (m.getName().equals("twoArgs")) {
                String infoCode = String.format("\"%s[\" + %s + \", \" + %s + \"]*\"",
                        m.getName(), argumentNames[0], argumentNames[1]);

                return "((net.openhft.chronicle.wire.MethodReaderInterceptorReturnsTest.AggregatingInfo)" +
                        objectName + ")." + "appendInfo(" + infoCode + ");";
            } else {
                return "((net.openhft.chronicle.wire.MethodReaderInterceptorReturnsTest.AggregatingInfo)" +
                        objectName + ")." + "appendInfo(\"someCall*\");";
            }
        }

        // Default interceptor which is not supposed to be called in this implementation
        @Override
        public Object intercept(Method m, Object o, Object[] args, Invocation invocation) {
            throw new UnsupportedOperationException("intercept shouldn't be called in generating interceptor");
        }
    }

    // Interceptor class that skips processing if certain conditions are met
    static class SkippingInterceptor implements GeneratingMethodReaderInterceptorReturns {

        // Return the unique identifier for this generator
        @Override
        public String generatorId() {
            return "skipping";
        }

        // Inject code before the actual method call
        // It skips processing for method "twoArgs" if the first argument is null
        @Override
        public String codeBeforeCall(Method m, String objectName, String[] argumentNames) {
            if (m.getName().equals("twoArgs"))
                return "if (" + argumentNames[0] + " != null) {";
            else
                return "";
        }

        // Inject code after the actual method call
        // Closes the conditional block for method "twoArgs"
        @Override
        public String codeAfterCall(Method m, String objectName, String[] argumentNames) {
            if (m.getName().equals("twoArgs"))
                return "}";
            else
                return "";
        }

        // Default interceptor which is not supposed to be called in this implementation
        @Override
        public Object intercept(Method m, Object o, Object[] args, Invocation invocation) {
            throw new UnsupportedOperationException("intercept shouldn't be called in generating interceptor");
        }
    }
}
