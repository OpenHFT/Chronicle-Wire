/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.threads.NamedThreadFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests the efficiency, correctness, and stability of BinaryWire string interning across concurrent readers.
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
    @BeforeEach
    public void threadDump() {
        super.threadDump();
    }

    // Prepares test data before the test runs
    @BeforeEach
    public void createTestData() {
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
    @DisplayName("Interns existing strings across concurrent readers")
    public void shouldInternExistingStringsAlright() throws Exception {
        // List to capture exceptions during the execution of concurrent tasks
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        // Create a thread pool for concurrent testing
        final ExecutorService executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory("test"));

        // Submit multiple tasks to the executor
        for (int i = 0; i < (Jvm.isArm() || Jvm.isCodeCoverage() ? 12 : 200); i++) {
            executorService.execute(new BinaryTextReaderWriter(capturedExceptions::add,
                    () -> BinaryWire.binaryOnly(Bytes.allocateElasticOnHeap(4096))));
        }

        // Randomly pick strings from testData, serialize and compare with previously interned strings
        for (int i = 0; i < 50000; i++) {
            wire.clear();
            final int dataPointIndex = random.nextInt(DATA_SET_SIZE);
            wire.getFixedBinaryValueOut(true).text(testData[dataPointIndex]);

            final String inputData = wire.read().text();
            assertEquals(internedStrings[dataPointIndex], inputData,
                    "interned string should match input at iteration " + i
                            + ", data index=" + dataPointIndex + ", value=" + inputData);
        }

        // Shutdown the executor and ensure all tasks are complete
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(60, TimeUnit.SECONDS),
                "Interning tasks did not complete in time");
        assertTrue(capturedExceptions.isEmpty(),
                "interning tasks should not raise exceptions, saw " + capturedExceptions.size());
    }

    /**
     * Test to ensure that when each thread has its own BinaryWire instance,
     * there should be no concurrency issues.
     */
    @Test
    @DisplayName("Uses per-thread BinaryWire without concurrency faults")
    public void multipleThreadsUsingBinaryWiresShouldNotCauseProblems() throws Exception {
        // List to capture exceptions during the execution of concurrent tasks
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        // Create a thread pool with number of threads equal to available processors
        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Submit multiple tasks to the executor. The number of tasks depends on the environment (ARM or Code Coverage).
        for (int i = 0; i < (Jvm.isArm() || Jvm.isCodeCoverage() ? 4 : 200); i++) {
            executorService.execute(new BinaryTextReaderWriter(capturedExceptions::add,
                    () -> BinaryWire.binaryOnly(Bytes.allocateElasticOnHeap(4096))));
        }

        // Shutdown the executor and ensure all tasks are complete
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(60, TimeUnit.SECONDS),
                "Per-thread wire tasks did not complete in time");
        assertTrue(capturedExceptions.isEmpty(),
                "per-thread wire tasks should not raise exceptions, saw " + capturedExceptions.size());
    }

    /**
     * Test to demonstrate potential errors that can arise when threads share the same BinaryWire instance.
     */
    @Test
    @Disabled("used to demonstrate errors that can occur when buffers are shared between threads")
    @DisplayName("Shared BinaryWire across threads can trigger failures")
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
            executorService.execute(new BinaryTextReaderWriter(capturedExceptions::add, () -> sharedMutableWire));
        }

        // Shutdown the executor and ensure all tasks are complete
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(30L, TimeUnit.SECONDS),
                "Shared wire tasks did not complete in time");

        // Print exceptions of type BufferUnderflowException
        capturedExceptions.stream().filter(e -> e instanceof BufferUnderflowException).forEach(RuntimeException::printStackTrace);
        assertTrue(capturedExceptions.isEmpty(),
                "shared wire tasks should not raise exceptions, saw " + capturedExceptions.size());
    }

    // Runnable class to read/write from/to BinaryWire
    private static final class BinaryTextReaderWriter implements Runnable {
        private final ThreadLocal<BinaryWire> wire;
        private final Random random = new Random(System.nanoTime());
        private final Consumer<RuntimeException> exceptionConsumer;

        private BinaryTextReaderWriter(final Consumer<RuntimeException> exceptionConsumer,
                                       final Supplier<BinaryWire> binaryWireSupplier) {
            this.exceptionConsumer = exceptionConsumer;

            // Each thread gets its own BinaryWire instance
            wire = ThreadLocal.withInitial(
                    binaryWireSupplier);
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
