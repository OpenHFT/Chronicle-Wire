/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.ReferenceCountedTracer;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Validates that Bytes reference counts remain stable when multiple {@link ReferenceOwner}s
 * reserve and release handles on different threads.
 */
public class BytesReferenceCountingTest extends WireTestCommon {

    @Test
    public void heapBytesMaintainReferenceCountsAcrossOwners() throws InterruptedException {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            exerciseReferenceCountingAcrossThreads(bytes);
        } finally {
            if (bytes.refCount() > 0) {
                bytes.releaseLast();
            }
        }
    }

    @Test
    public void warnLoggedWhenOwnerLeakedAndForceReleased() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            ReferenceOwner leaky = ReferenceOwner.temporary("leaky-owner");
            bytes.reserve(leaky);
            ((ReferenceCountedTracer) bytes).warnAndReleaseIfNotReleased();
        } finally {
            if (bytes.refCount() > 0) {
                bytes.releaseLast();
            }
        }
    }

    @Test
    public void directBytesMaintainReferenceCountsAcrossOwners() throws InterruptedException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        Bytes<?> bytes = Bytes.allocateElasticDirect(64);
        try {
            exerciseReferenceCountingAcrossThreads(bytes);
        } finally {
            if (bytes.refCount() > 0) {
                bytes.releaseLast();
            }
        }
    }

    private void exerciseReferenceCountingAcrossThreads(Bytes<?> bytes) throws InterruptedException {
        assertEquals(1, bytes.refCount(), "fresh Bytes should start with refCount=1");

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
                        failure.compareAndSet(null, new AssertionError("start latch timed out"));
                        return;
                    }
                    for (int iteration = 0; iteration < iterationsPerOwner; iteration++) {
                        bytes.reserve(owner);
                        long afterReserve = bytes.refCount();
                        if (afterReserve < 2) {
                            failure.compareAndSet(null, new AssertionError("refCount did not increase on reserve"));
                            break;
                        }
                        bytes.release(owner);
                        long afterRelease = bytes.refCount();
                        if (afterRelease < 1) {
                            failure.compareAndSet(null, new AssertionError("refCount underflow after release"));
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failure.compareAndSet(null, new AssertionError("worker interrupted", e));
                } finally {
                    done.countDown();
                }
            }, "bytes-refcount-" + i);
            thread.start();
            workers.add(thread);
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "workers did not finish in time");
        for (Thread worker : workers) {
            worker.join(TimeUnit.SECONDS.toMillis(1));
        }
        AssertionError error = failure.get();
        if (error != null) {
            throw error;
        }

        assertEquals(1, bytes.refCount(), "all temporary owners released");

        ReferenceOwner finalOwner = ReferenceOwner.temporary("final-owner");
        bytes.reserve(finalOwner);
        assertEquals(2, bytes.refCount(), "final owner should increment refCount");
        bytes.release(finalOwner);
        assertEquals(1, bytes.refCount(), "reference count returns to baseline");
    }
}
