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

import static org.junit.Assert.assertEquals;

@SuppressWarnings({"this-escape"})
public class WireTestCommon {

    // A thread dump to monitor thread states and detect unwanted thread creation
    protected ThreadDump threadDump;

    // Collection to record exceptions
    protected Map<ExceptionKey, Integer> exceptions;

    // Collection of exceptions that should be ignored during tests
    private final Map<Predicate<ExceptionKey>, String> ignoredExceptions = new LinkedHashMap<>();

    // Collection of exceptions that are expected during tests
    private final Map<Predicate<ExceptionKey>, String> expectedExceptions = new LinkedHashMap<>();

    private boolean gt;

    // Default constructor initializes ignored exceptions
    public WireTestCommon() {
        // Ignore exceptions with incubating feature warnings
        ignoreException("The incubating features are subject to change");
        ignoreException("NamedThreadFactory created here");
        ignoreException("Unable to find suitable cleaner service, falling back to using reflection");
    }

    // Activates the reference tracing before executing tests
    @Before
    public void enableReferenceTracing() {
        AbstractReferenceCounted.enableReferenceTracing();
    }

    // Verifies if all references were released after the tests
    public void assertReferencesReleased() {
        AbstractReferenceCounted.assertReferencesReleased();
    }

    // Intended to be used with @Before for tests that might create threads
    // Captures a snapshot of all threads before test execution
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    // Checks if any new threads have been created after test execution
    public void checkThreadDump() {
        if (threadDump != null)
            threadDump.assertNoNewThreads();
    }

    // Records exceptions before the test runs
    @Before
    public void recordExceptions() {
        exceptions = Jvm.recordExceptions();
    }

    // Adds an exception with a particular message to the ignore list
    public void ignoreException(String message) {
        ignoreException(k -> contains(k.message, message) || (k.throwable != null && contains(k.throwable.getMessage(), message)), message);
    }

    // Utility method to check if a text contains a particular message
    static boolean contains(String text, String message) {
        return text != null && text.contains(message);
    }

    // Ignores a specific exception based on a given predicate and description
    public void ignoreException(Predicate<ExceptionKey> predicate, String description) {
        ignoredExceptions.put(predicate, description);
    }

    // Expects an exception with a particular message during the tests
    public void expectException(String message) {
        expectException(k -> contains(k.message, message) || (k.throwable != null && contains(k.throwable.getMessage(), message)), message);
    }

    // Expect an exception based on a given predicate and description during the tests
    public void expectException(Predicate<ExceptionKey> predicate, String description) {
        expectedExceptions.put(predicate, description);
    }

    // Checks if the exceptions thrown during tests match the expected and ignored exceptions
    public void checkExceptions() {
        // Validate expected exceptions were thrown
        for (Map.Entry<Predicate<ExceptionKey>, String> expectedException : expectedExceptions.entrySet()) {
            if (!exceptions.keySet().removeIf(expectedException.getKey()))
                throw new AssertionError("No error for " + expectedException.getValue());
        }
        expectedExceptions.clear();

        // Remove ignored exceptions from the recorded list
        for (Map.Entry<Predicate<ExceptionKey>, String> ignoredException : ignoredExceptions.entrySet()) {
            if (!exceptions.keySet().removeIf(ignoredException.getKey()))
                Slf4jExceptionHandler.DEBUG.on(getClass(), "Ignored " + ignoredException.getValue());
        }
        ignoredExceptions.clear();

        // Remove DEBUG and PERF log level exceptions
        for (Iterator<Map.Entry<ExceptionKey, Integer>> iterator = exceptions.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ExceptionKey, Integer> entry = iterator.next();
            LogLevel level = entry.getKey().level;
            if (level == LogLevel.DEBUG || level == LogLevel.PERF)
                iterator.remove();
        }

        // Assert that no unexpected exceptions were thrown
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

    /**
     * Parses and round-trips each of provided strings delimited and trailed by comma with a specified converter.
     */
    protected static void subStringParseLoop(String s, LongConverter c, int comparisons) {
        int oldPos = 0;
        int newPos;
        while ((newPos = s.indexOf(',', oldPos)) >= 0) {
            long v = c.parse(s, oldPos, newPos);
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s.substring(oldPos, newPos), sb.toString());
            oldPos = newPos + 1;
            comparisons--;
        }
        assertEquals(0, comparisons);
    }

    @After
    public void afterChecks() {
        preAfter(); // Any custom operations before the default cleanup
        CleaningThread.performCleanup(Thread.currentThread());

        // Check for any lingering resources
        AbstractCloseable.waitForCloseablesToClose(100);

        // Verify if all references were released, no new threads were created and exceptions match expectations
        assertReferencesReleased();
        checkThreadDump();
        checkExceptions();
        MessageHistory.clear();
    }

    // Placeholder for subclasses to include additional operations before afterChecks
    protected void preAfter() {
    }

    // Store the current value of GENERATE_TUPLES before test execution
    @Before
    public void rememberGenerateTuples() {
        gt = Wires.GENERATE_TUPLES;
    }

    // Restore the original value of GENERATE_TUPLES after the test execution
    @After
    public void restoreGenerateTuples() {
        Wires.GENERATE_TUPLES = gt;
    }

    @Before
    public void throwCNFRE() {
    }
}
