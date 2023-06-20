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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.core.onoes.LogLevel;
import net.openhft.chronicle.core.onoes.Slf4jExceptionHandler;
import net.openhft.chronicle.core.threads.CleaningThread;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WireTestCommon {
    protected ThreadDump threadDump;
    protected Map<ExceptionKey, Integer> exceptions;
    private final Map<Predicate<ExceptionKey>, String> ignoredExceptions = new LinkedHashMap<>();
    private final Map<Predicate<ExceptionKey>, String> expectedExceptions = new LinkedHashMap<>();

    private boolean gt;

    public WireTestCommon() {
        ignoreException("The incubating features are subject to change");
    }

    @Before
    public void enableReferenceTracing() {
        AbstractReferenceCounted.enableReferenceTracing();
    }

    public void assertReferencesReleased() {
        AbstractReferenceCounted.assertReferencesReleased();
    }

    // add @Before for tests that might create threads.
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    public void checkThreadDump() {
        if (threadDump != null)
            threadDump.assertNoNewThreads();
    }

    @Before
    public void recordExceptions() {
        exceptions = Jvm.recordExceptions();
    }

    public void ignoreException(String message) {
        ignoreException(k -> contains(k.message, message) || (k.throwable != null && contains(k.throwable.getMessage(), message)), message);
    }

    static boolean contains(String text, String message) {
        return text != null && text.contains(message);
    }

    public void ignoreException(Predicate<ExceptionKey> predicate, String description) {
        ignoredExceptions.put(predicate, description);
    }

    public void expectException(String message) {
        expectException(k -> contains(k.message, message) || (k.throwable != null && contains(k.throwable.getMessage(), message)), message);
    }

    public void expectException(Predicate<ExceptionKey> predicate, String description) {
        expectedExceptions.put(predicate, description);
    }

    public void checkExceptions() {
        for (Map.Entry<Predicate<ExceptionKey>, String> expectedException : expectedExceptions.entrySet()) {
            if (!exceptions.keySet().removeIf(expectedException.getKey()))
                throw new AssertionError("No error for " + expectedException.getValue());
        }
        expectedExceptions.clear();
        for (Map.Entry<Predicate<ExceptionKey>, String> ignoredException : ignoredExceptions.entrySet()) {
            if (!exceptions.keySet().removeIf(ignoredException.getKey()))
                Slf4jExceptionHandler.DEBUG.on(getClass(), "Ignored " + ignoredException.getValue());
        }
        ignoredExceptions.clear();
        for (Iterator<Map.Entry<ExceptionKey, Integer>> iterator = exceptions.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ExceptionKey, Integer> entry = iterator.next();
            LogLevel level = entry.getKey().level;
            if (level == LogLevel.DEBUG || level == LogLevel.PERF)
                iterator.remove();
        }
        if (!exceptions.isEmpty()) {
            final String msg = exceptions.size() + " exceptions were detected: " +
                    exceptions.keySet().stream()
                            .map(ek -> ek.message + " " + ek.throwable)
                            .collect(Collectors.joining(", "));
            Jvm.dumpException(exceptions);
            Jvm.resetExceptionHandlers();
            Assert.fail(msg);
        }
    }

    @After
    public void afterChecks() {
        preAfter();
        CleaningThread.performCleanup(Thread.currentThread());

        // find any discarded resources.
        AbstractCloseable.waitForCloseablesToClose(100);

        assertReferencesReleased();
        checkThreadDump();
        checkExceptions();
        MessageHistory.set(null);
    }

    protected void preAfter() {
    }

    @Before
    public void rememberGenerateTuples() {
        gt = Wires.GENERATE_TUPLES;
    }

    @After
    public void restoreGenerateTuples() {
        Wires.GENERATE_TUPLES = gt;
    }
}
