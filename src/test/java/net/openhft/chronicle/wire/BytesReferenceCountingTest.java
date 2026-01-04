/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.ReferenceCountedTracer;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Validates that Bytes reference counts remain stable when multiple {@link ReferenceOwner}s
 * reserve and release handles on different threads.
 */
class BytesReferenceCountingTest extends WireTestCommon {

    @Test
    @DisplayName("Maintains reference counts for heap bytes across owners")
    void heapBytesMaintainReferenceCountsAcrossOwners() throws InterruptedException {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        assertEquals(1, bytes.refCount(), "newly allocated heap bytes should have initial reference count of 1");
        try {
            exerciseReferenceCountingAcrossThreads(bytes);
            assertEquals(1, bytes.refCount(), "heap bytes reference count should return to 1 after all concurrent owners release their references");
        } finally {
            if (bytes.refCount() > 0) {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("Warns when owner leaks and force releases bytes")
    void warnLoggedWhenOwnerLeakedAndForceReleased() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        assertEquals(1, bytes.refCount(), "newly allocated bytes should have initial reference count of 1 before any reservations");
        try {
            ReferenceOwner leaky = ReferenceOwner.temporary("leaky-owner");
            bytes.reserve(leaky);
            long afterReserve = bytes.refCount();
            assertTrue(afterReserve > 1, "Expected afterReserve > 1 but was " + afterReserve);
            ((ReferenceCountedTracer) bytes).warnAndReleaseIfNotReleased();
            long afterRelease = bytes.refCount();
            assertTrue(afterRelease < afterReserve,
                    "Expected refCount to drop below " + afterReserve + " but was " + afterRelease);
        } finally {
            if (bytes.refCount() > 0) {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("Maintains reference counts for direct bytes across owners")
    void directBytesMaintainReferenceCountsAcrossOwners() throws InterruptedException {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip direct bytes test");
        Bytes<?> bytes = Bytes.allocateElasticDirect(64);
        assertEquals(1, bytes.refCount(), "newly allocated direct bytes should have initial reference count of 1");
        try {
            exerciseReferenceCountingAcrossThreads(bytes);
            assertEquals(1, bytes.refCount(), "direct bytes reference count should return to 1 after all concurrent owners release their references");
        } finally {
            if (bytes.refCount() > 0) {
                bytes.releaseLast();
            }
        }
    }

    private void exerciseReferenceCountingAcrossThreads(Bytes<?> bytes) throws InterruptedException {
        assertEquals(1, bytes.refCount(), "bytes should have reference count of 1 before concurrent access test begins");

        int ownersCount = 4;
        int iterationsPerOwner = 64;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(ownersCount);
        AtomicReference<AssertionError> failure = new AtomicReference<>();
        List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < ownersCount; i++) {
            ReferenceOwner owner = ReferenceOwner.temporary("bytes-owner-" + i);
            Thread thread = new Thread(() -> {
                try {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        failure.compareAndSet(null, new AssertionError("worker thread failed to receive start signal within 5 second timeout - potential deadlock in concurrent reference counting test"));
                        return;
                    }
                    for (int iteration = 0; iteration < iterationsPerOwner; iteration++) {
                        bytes.reserve(owner);
                        long afterReserve = bytes.refCount();
                        if (afterReserve < 2) {
                            failure.compareAndSet(null, new AssertionError(
                                    "reference count must be at least 2 after reserve() call (1 for initial owner + 1 for reserved owner), but was " + afterReserve +
                                            " after iteration " + iteration + " - indicates concurrent release bug or missing atomic increment"));
                            break;
                        }
                        bytes.release(owner);
                        long afterRelease = bytes.refCount();
                        if (afterRelease < 1) {
                            failure.compareAndSet(null, new AssertionError(
                                    "reference count dropped to " + afterRelease + " after release() on iteration " + iteration +
                                            " - indicates over-release bug causing premature deallocation of off-heap memory still in use"));
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failure.compareAndSet(null, new AssertionError("worker thread interrupted during reserve/release cycle - concurrent reference counting test integrity compromised", e));
                } finally {
                    done.countDown();
                }
            }, "bytes-refcount-" + i);
            thread.start();
            workers.add(thread);
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "all worker threads should complete reserve/release cycles within timeout");
        for (Thread worker : workers) {
            worker.join(TimeUnit.SECONDS.toMillis(1));
        }
        AssertionError error = failure.get();
        if (error != null) {
            throw error;
        }

        assertEquals(1, bytes.refCount(), "reference count should return to 1 after all concurrent owners complete their reserve/release cycles");

        ReferenceOwner finalOwner = ReferenceOwner.temporary("final-owner");
        bytes.reserve(finalOwner);
        assertEquals(2, bytes.refCount(), "reference count should increase to 2 when final owner reserves bytes");
        bytes.release(finalOwner);
        assertEquals(1, bytes.refCount(), "reference count should return to 1 after final owner releases bytes");
    }
}
