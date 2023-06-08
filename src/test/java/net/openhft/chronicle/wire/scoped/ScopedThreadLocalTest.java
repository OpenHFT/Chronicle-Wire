package net.openhft.chronicle.wire.scoped;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.threads.CleaningThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopedThreadLocalTest {

    private ScopedThreadLocal<AtomicLong> scopedThreadLocal;

    @BeforeEach
    void setUp() {
        scopedThreadLocal = new ScopedThreadLocal<>(AtomicLong::new, al -> al.set(0), 3);
    }

    @Test
    void nestedCallsWillGetDifferentResources() {
        try (ScopedResource<AtomicLong> l1 = scopedThreadLocal.get()) {
            l1.get().set(123);
            try (ScopedResource<AtomicLong> l2 = scopedThreadLocal.get()) {
                l2.get().set(456);
                try (ScopedResource<AtomicLong> l3 = scopedThreadLocal.get()) {
                    l3.get().set(789);
                }
                assertEquals(456L, l2.get().get());
            }
            assertEquals(123L, l1.get().get());
        }
    }

    @Test
    void differentThreadsWillGetDifferentResources() throws InterruptedException {
        Set<Integer> instanceObjectIDs = new HashSet<>();
        final int numThreads = 10;
        for (int i = 0; i < numThreads; i++) {
            final Thread thread = new Thread(() -> {
                try (final ScopedResource<AtomicLong> resource = scopedThreadLocal.get()) {
                    if (resource == null) {
                        throw new IllegalStateException();
                    }
                    final int e = System.identityHashCode(resource.get());
                    instanceObjectIDs.add(e);
                } catch (Exception e) {
                    Jvm.error().on(ScopedThreadLocalTest.class, e);
                }
            });
            thread.start();
            thread.join();
        }
        assertEquals(numThreads, instanceObjectIDs.size());
    }

    @Test
    void onAcquireIsPerformedBeforeEachAcquisition() {
        int objectId;
        try (ScopedResource<AtomicLong> l1 = scopedThreadLocal.get()) {
            l1.get().set(123);
            objectId = System.identityHashCode(l1.get());
        }
        try (ScopedResource<AtomicLong> l1 = scopedThreadLocal.get()) {
            assertEquals(0L, l1.get().get());
            assertEquals(objectId, System.identityHashCode(l1.get()));
        }
    }

    @Test
    void cleaningThreadWillCloseResources() throws InterruptedException {
        List<CloseableResource> allResources = new ArrayList<>();
        ScopedThreadLocal<CloseableResource> stl = new ScopedThreadLocal<>(() -> {
            CloseableResource cr = new CloseableResource();
            allResources.add(cr);
            return cr;
        }, 2);
        final CleaningThread cleaningThread = new CleaningThread(() -> {
            try (final ScopedResource<CloseableResource> cr1 = stl.get();
                 final ScopedResource<CloseableResource> cr2 = stl.get()) {
                // Create two resources, do nothing
            }
            // None should be closed
            assertTrue(allResources.stream().noneMatch(cr -> cr.closed));
        });
        cleaningThread.start();
        cleaningThread.join();
        // All should be closed
        assertEquals(2, allResources.size());
        assertTrue(allResources.stream().allMatch(cr -> cr.closed));
    }

    private static class CloseableResource implements Closeable {

        private boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }
}