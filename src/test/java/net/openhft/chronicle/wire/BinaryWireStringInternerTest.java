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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.threads.NamedThreadFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class tests the efficiency and correctness of the `BinaryWire` when interning strings.
 */
public final class BinaryWireStringInternerTest extends WireTestCommon {

    // Constants for test configuration
    private static final int DATA_SET_SIZE = 1_000;
    private static final long SEED_WITHOUT_COLLISIONS = 0x982374EADL;

    // Test setup and data storage
    private final Random random = new Random(SEED_WITHOUT_COLLISIONS);
    private final String[] testData = new String[DATA_SET_SIZE];
    private final String[] internedStrings = new String[DATA_SET_SIZE];
    private final Bytes<?> heapBytes = Bytes.allocateElasticOnHeap(4096);
    private final BinaryWire wire = BinaryWire.binaryOnly(heapBytes);

    // Helper method to create a meaningful message for assertions
    private static String message(final int index, final String inputData) {
        return String.format("At index %d for string %s",
                index, inputData);
    }

    // Generates a random string of given length
    private static String makeString(final int length, final Random random) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append((char) ('a' + random.nextInt('z' - 'a')));
        }
        return builder.toString();
    }

    // Thread dump for debugging and logging purposes, from WireTestCommon
    @Override
    @Before
    public void threadDump() {
        super.threadDump();
    }

    // Prepares test data before the test runs
    @Before
    public void createTestData() throws Exception {
        // Populate testData with random strings
        for (int i = 0; i < DATA_SET_SIZE; i++) {
            testData[i] = makeString(random.nextInt(250) + 32, random);
        }

        // Serialize the test data and store the resulting interned strings
        for (int i = 0; i < DATA_SET_SIZE; i++) {
            wire.getFixedBinaryValueOut(true).text(testData[i]);
            internedStrings[i] = wire.read().text();
        }
        wire.clear();
    }

    // Test to ensure the interning of existing strings works correctly
    @Test
    public void shouldInternExistingStringsAlright() throws Exception {
        // List to capture exceptions during the execution of concurrent tasks
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        // Create a thread pool for concurrent testing
        final ExecutorService executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory("test"));

        // Submit multiple tasks to the executor
        for (int i = 0; i < (Jvm.isArm() || Jvm.isCodeCoverage() ? 12 : 200); i++) {
            executorService.submit(new BinaryTextReaderWriter(capturedExceptions::add, () -> BinaryWire.binaryOnly(Bytes.allocateElasticOnHeap(4096))));
        }

        // Randomly pick strings from testData, serialize and compare with previously interned strings
        for (int i = 0; i < 50000; i++) {
            wire.clear();
            final int dataPointIndex = random.nextInt(DATA_SET_SIZE);
            wire.getFixedBinaryValueOut(true).text(testData[dataPointIndex]);

            final String inputData = wire.read().text();
            assertEquals(message(i, inputData), internedStrings[dataPointIndex], inputData);
        }

        // Shutdown the executor and ensure all tasks are complete
        executorService.shutdown();
        assertTrue("jobs did not complete in time", executorService.awaitTermination(60, TimeUnit.SECONDS));
        assertTrue(capturedExceptions.isEmpty());
    }

    /**
     * Test to ensure that when each thread has its own BinaryWire instance,
     * there should be no concurrency issues.
     */
    @Test
    public void multipleThreadsUsingBinaryWiresShouldNotCauseProblems() throws Exception {
        // List to capture exceptions during the execution of concurrent tasks
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        // Create a thread pool with number of threads equal to available processors
        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Submit multiple tasks to the executor. The number of tasks depends on the environment (ARM or Code Coverage).
        for (int i = 0; i < (Jvm.isArm() || Jvm.isCodeCoverage() ? 4 : 200); i++) {
            executorService.submit(new BinaryTextReaderWriter(capturedExceptions::add, () -> BinaryWire.binaryOnly(Bytes.allocateElasticOnHeap(4096))));
        }

        // Shutdown the executor and ensure all tasks are complete
        executorService.shutdown();
        assertTrue("jobs did not complete in time",
                executorService.awaitTermination(60, TimeUnit.SECONDS));
        assertTrue(capturedExceptions.isEmpty());
    }

    /**
     * Test to demonstrate potential errors that can arise when threads share the same BinaryWire instance.
     */
    @Ignore("used to demonstrate errors that can occur when buffers are shared between threads")
    @Test
    public void multipleThreadsSharingBinaryWireShouldCauseProblems() throws Exception {
        // List to capture exceptions during the execution of concurrent tasks
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        // Create a thread pool with custom thread factory
        final ExecutorService executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory("test"));

        // Create a shared BinaryWire instance
        final BinaryWire sharedMutableWire = BinaryWire.binaryOnly(Bytes.allocateElasticOnHeap(4096));

        // Submit tasks to the executor, all sharing the same BinaryWire instance
        for (int i = 0; i < 1_000; i++) {
            executorService.submit(new BinaryTextReaderWriter(capturedExceptions::add, () -> sharedMutableWire));
        }

        // Shutdown the executor and ensure all tasks are complete
        executorService.shutdown();
        assertTrue("jobs did not complete in time", executorService.awaitTermination(30L, TimeUnit.SECONDS));

        // Print exceptions of type BufferUnderflowException
        capturedExceptions.stream().filter(e -> e instanceof BufferUnderflowException).forEach(RuntimeException::printStackTrace);
        assertTrue(capturedExceptions.isEmpty());
    }

    // Runnable class to read/write from/to BinaryWire
    private static final class BinaryTextReaderWriter implements Runnable {
        private final Supplier<BinaryWire> binaryWireSupplier;
        private final ThreadLocal<BinaryWire> wire;
        private final Random random = new Random(System.nanoTime());
        private final Consumer<RuntimeException> exceptionConsumer;

        private BinaryTextReaderWriter(final Consumer<RuntimeException> exceptionConsumer,
                                       final Supplier<BinaryWire> binaryWireSupplier) throws IOException {
            this.exceptionConsumer = exceptionConsumer;
            this.binaryWireSupplier = binaryWireSupplier;

            // Each thread gets its own BinaryWire instance
            wire = ThreadLocal.withInitial(
                    this.binaryWireSupplier);
        }

        @Override
        public void run() {
            try {
                // Write to the BinaryWire
                for (int i = 0; i < 2_000; i++) {
                    wire.get().getFixedBinaryValueOut(true).text(makeString(250, random));
                }

                // Read from the BinaryWire and raise an exception if the read value is null
                for (int i = 0; i < 2_000; i++) {
                    if (wire.get().read().text() == null) {
                        exceptionConsumer.accept(new IllegalStateException("text was null"));
                    }
                }
            } catch (RuntimeException e) {
                exceptionConsumer.accept(e);
            }
        }
    }
}
