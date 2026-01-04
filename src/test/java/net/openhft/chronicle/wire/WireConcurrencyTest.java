/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for concurrency and thread safety in Wire operations.
 * These tests verify correct behaviour under concurrent access.
 */
@SuppressWarnings({"deprecation", "removal"})
class WireConcurrencyTest extends WireTestCommon {

    private static final int THREAD_COUNT = 4;
    private static final int ITERATIONS_PER_THREAD = 100;

    // ========== Independent Wire Instance Tests ==========

    @Test
    @DisplayName("Multiple threads with independent BinaryWire instances should work")
    void testIndependentBinaryWireInstances() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            executor.execute(() -> {
                try {
                    runBinaryThreadIterations(threadId, successCount);
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "Independent binary wire threads should complete within timeout");
        executor.shutdown();

        assertFalse(failed.get(), "No thread should fail during independent binary wire access");
        assertEquals(THREAD_COUNT * ITERATIONS_PER_THREAD, successCount.get(),
                "Binary wire loop should record all iterations");
    }

    @Test
    @DisplayName("Multiple threads with independent TextWire instances should work")
    void testIndependentTextWireInstances() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            executor.execute(() -> {
                try {
                    runTextThreadIterations(threadId, successCount);
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "Independent text wire threads should complete within timeout");
        executor.shutdown();

        assertFalse(failed.get(), "No thread should fail during independent text wire access");
        assertEquals(THREAD_COUNT * ITERATIONS_PER_THREAD, successCount.get(),
                "Text wire loop should record all iterations");
    }

    @Test
    @DisplayName("Multiple threads with independent YamlWire instances should work")
    void testIndependentYamlWireInstances() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            executor.execute(() -> {
                try {
                    runYamlThreadIterations(threadId, successCount);
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "Independent Yaml wire threads should complete within timeout");
        executor.shutdown();

        assertFalse(failed.get(), "No thread should fail during independent Yaml wire access");
        assertEquals(THREAD_COUNT * ITERATIONS_PER_THREAD, successCount.get(),
                "Yaml wire loop should record all iterations");
    }

    // ========== Concurrent Object Serialization Tests ==========

    public static class ThreadSafeDto extends SelfDescribingMarshallable {
        public int threadId;
        public int iteration;
        public String data;

        public ThreadSafeDto() {
        }

        public ThreadSafeDto(int threadId, int iteration, String data) {
            this.threadId = threadId;
            this.iteration = iteration;
            this.data = data;
        }
    }

    @Test
    @DisplayName("Concurrent object serialization should work with independent wires")
    void testConcurrentObjectSerialization() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            Future<Boolean> future = executor.submit(() -> {
                try {
                    for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
                        BinaryWire wire = new BinaryWire(bytes);

                        ThreadSafeDto original = new ThreadSafeDto(
                                threadId, i, "data-" + threadId + "-" + i);

                        wire.write("dto").object(original);

                        bytes.readPosition(0);

                        ThreadSafeDto read = wire.read("dto").object(ThreadSafeDto.class);

                        if (read.threadId != threadId ||
                                read.iteration != i ||
                                !read.data.equals("data-" + threadId + "-" + i)) {
                            return false;
                        }
                    }
                    return true;
                } catch (Exception e) {
                    failed.set(true);
                    return false;
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "Concurrent serialisation threads should complete within timeout");
        executor.shutdown();

        assertFalse(failed.get(), "No thread should fail during concurrent object serialisation");
        for (Future<Boolean> future : futures) {
            assertTrue(future.get(), "Concurrent serialisation future should report success");
        }
    }

    // ========== WireType Factory Thread Safety Tests ==========

    @Test
    @DisplayName("WireType.apply should be thread-safe")
    void testWireTypeApplyThreadSafety() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);

        WireType[] wireTypes = {WireType.BINARY, WireType.TEXT, WireType.YAML};

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            executor.execute(() -> {
                try {
                    runWireTypeApplyIterations(threadId, wireTypes);
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "WireType.apply threads should complete within timeout");
        executor.shutdown();

        assertFalse(failed.get(), "No thread should fail during WireType.apply concurrency");
    }

    // ========== Elastic Buffer Resize Under Concurrency ==========

    @Test
    @DisplayName("Elastic buffer resize should work with independent instances")
    void testConcurrentElasticResize() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            executor.execute(() -> {
                try {
                    for (int i = 0; i < ITERATIONS_PER_THREAD / 10; i++) {
                        // Start with small buffer, force many resizes
                        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
                        BinaryWire wire = new BinaryWire(bytes);

                        // Write enough data to trigger multiple resizes
                        for (int j = 0; j < 50; j++) {
                            wire.write("key" + j).text("value" + threadId + "-" + i + "-" + j);
                        }

                        bytes.readPosition(0);

                        // Verify all data
                        for (int j = 0; j < 50; j++) {
                            String expected = "value" + threadId + "-" + i + "-" + j;
                            assertEquals(expected, wire.read("key" + j).text(),
                                    "Elastic resize should read key " + j + " for thread " + threadId);
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS),
                "Elastic resize threads should complete within timeout");
        executor.shutdown();

        assertFalse(failed.get(), "No thread should fail during elastic resize");
    }

    // ========== Rapid Create/Dispose Cycles ==========

    @Test
    @DisplayName("Rapid wire creation and disposal should not leak")
    void testRapidCreateDispose() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger totalCreated = new AtomicInteger(0);

        for (int t = 0; t < THREAD_COUNT; t++) {
            executor.execute(() -> {
                try {
                    for (int i = 0; i < ITERATIONS_PER_THREAD * 10; i++) {
                        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
                        BinaryWire wire = new BinaryWire(bytes);
                        wire.write("x").int32(i);
                        bytes.readPosition(0);
                        wire.read("x").int32();
                        // Let bytes be garbage collected
                        totalCreated.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS),
                "Rapid creation threads should complete within timeout");
        executor.shutdown();

        assertEquals(THREAD_COUNT * ITERATIONS_PER_THREAD * 10, totalCreated.get(),
                "Rapid creation loop should record every created wire");
    }

    // ========== Mixed Read/Write Patterns ==========

    @Test
    @DisplayName("Mixed wire types under concurrency should work")
    void testMixedWireTypesConcurrent() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            executor.execute(() -> {
                try {
                    runMixedWireIterations(threadId);
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "Mixed wire type threads should complete within timeout");
        executor.shutdown();

        assertFalse(failed.get(), "No thread should fail with mixed wire types");
    }

    private void runBinaryThreadIterations(int threadId, AtomicInteger successCount) {
        for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            BinaryWire wire = new BinaryWire(bytes);

            // Write thread-specific data
            wire.write("thread").int32(threadId);
            wire.write("iteration").int32(i);
            wire.write("data").text("value-" + threadId + "-" + i);

            bytes.readPosition(0);

            // Read and verify
            assertEquals(threadId, wire.read("thread").int32(),
                    "Binary wire thread id should round-trip for thread " + threadId + " iteration " + i);
            assertEquals(i, wire.read("iteration").int32(),
                    "Binary wire iteration should round-trip for thread " + threadId + " iteration " + i);
            assertEquals("value-" + threadId + "-" + i, wire.read("data").text(),
                    "Binary wire data should round-trip for thread " + threadId + " iteration " + i);

            successCount.incrementAndGet();
        }
    }

    private void runTextThreadIterations(int threadId, AtomicInteger successCount) {
        for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            TextWire wire = new TextWire(bytes);

            wire.write("thread").int32(threadId);
            wire.write("iteration").int32(i);

            bytes.readPosition(0);

            assertEquals(threadId, wire.read("thread").int32(),
                    "Text wire thread id should round-trip for thread " + threadId + " iteration " + i);
            assertEquals(i, wire.read("iteration").int32(),
                    "Text wire iteration should round-trip for thread " + threadId + " iteration " + i);

            successCount.incrementAndGet();
        }
    }

    private void runYamlThreadIterations(int threadId, AtomicInteger successCount) {
        for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            YamlWire wire = new YamlWire(bytes);

            wire.write("thread").int32(threadId);
            wire.write("iteration").int32(i);

            bytes.readPosition(0);

            assertEquals(threadId, wire.read("thread").int32(),
                    "Yaml wire thread id should round-trip for thread " + threadId + " iteration " + i);
            assertEquals(i, wire.read("iteration").int32(),
                    "Yaml wire iteration should round-trip for thread " + threadId + " iteration " + i);

            successCount.incrementAndGet();
        }
    }

    private void runWireTypeApplyIterations(int threadId, WireType[] wireTypes) {
        for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
            WireType wt = wireTypes[i % wireTypes.length];
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            assertNotNull(wire, "WireType.apply should return a non-null wire for thread " + threadId + " iteration " + i);

            wire.write("val").int32(threadId * 1000 + i);
            bytes.readPosition(0);
            assertEquals(threadId * 1000 + i, wire.read("val").int32(),
                    "WireType.apply should read back value " + (threadId * 1000 + i) + " for thread " + threadId + " iteration " + i);
        }
    }

    private void runMixedWireIterations(int threadId) {
        for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
            // Each thread uses different wire type based on iteration
            Wire wire;
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();

            switch (i % 3) {
                case 0:
                    wire = new BinaryWire(bytes);
                    break;
                case 1:
                    wire = new TextWire(bytes);
                    break;
                default:
                    wire = new YamlWire(bytes);
                    break;
            }

            // Write various types
            wire.write("int").int32(threadId);
            wire.write("long").int64(i * 1000L);
            wire.write("double").float64(threadId + 0.5);
            wire.write("string").text("t" + threadId + "i" + i);

            bytes.readPosition(0);

            // Read and verify
            assertEquals(threadId, wire.read("int").int32(),
                    "Mixed wire int field should match thread " + threadId + " iteration " + i);
            assertEquals(i * 1000L, wire.read("long").int64(),
                    "Mixed wire long field should match thread " + threadId + " iteration " + i);
            assertEquals(threadId + 0.5, wire.read("double").float64(), 0.001,
                    "Mixed wire double field should match thread " + threadId + " iteration " + i);
            assertEquals("t" + threadId + "i" + i, wire.read("string").text(),
                    "Mixed wire string field should match thread " + threadId + " iteration " + i);
        }
    }
}
