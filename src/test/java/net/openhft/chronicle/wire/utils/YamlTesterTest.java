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

package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.time.SetTimeProvider;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.YamlMethodTester;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

@SuppressWarnings("deprecation")
public class YamlTesterTest extends WireTestCommon {
    // Test implementation instance
    private static TestImpl testImpl;

    // Setup method for each test, initializes a time provider
    @Before
    public void setUp() {
        // Set a custom time provider with a fixed start time and auto-increment
        CLOCK = new SetTimeProvider("2022-05-17T20:26:00")
                .autoIncrement(1, TimeUnit.MICROSECONDS);
    }

    // Additional checks to run before each test
    @Override
    protected void preAfter() {
        super.preAfter();
        // Ensure that the test implementation instance is closed after the test
        if (testImpl != null)
            assertTrue(testImpl.isClosed());
    }

    // Teardown method for each test, resets the time provider
    @After
    public void tearDown() {
        // Reset the CLOCK to its original system time provider
        CLOCK = SystemTimeProvider.INSTANCE;
    }

    // Test method for scenario t1
    @Test
    public void t1() {
        // Create a YamlTester instance for the t1 test
        YamlTester yt = new YamlMethodTester<>(
                "yaml-tester/t1/in.yaml", // Input YAML file
                newTestImplFunction(),    // Test implementation function
                TestOut.class,            // Test output class
                "yaml-tester/t1/out.yaml")// Expected output YAML file
                .setup("yaml-tester/t1/setup.yaml") // Setup YAML file
                .inputFunction(s -> s.replace("# Replace comment", ""));
        // Assert that the actual output matches the expected output
        assertEquals(yt.expected(), yt.actual());
    }

    // Test method for scenario t2
    @Test
    public void t2() {
        // Run the t2 test using the YamlTester utility
        final YamlTester yt = YamlTester.runTest(newTestImplFunction(), TestOut.class, "yaml-tester/t2");
        // Assert that the actual output matches the expected output
        assertEquals(yt.expected(), yt.actual());
    }

    // Test method for scenario t3
    @Test
    public void t3() {
        // Run the t3 test using the YamlTester utility
        final YamlTester yt = YamlTester.runTest(newTestImplFunction(), TestOut.class, "yaml-tester/t3");
        // Assert that the actual output matches the expected output
        assertEquals(yt.expected(), yt.actual());
    }

    // Factory method to create a new test implementation function
    @NotNull
    private static Function<TestOut, Object> newTestImplFunction() {
        return out -> testImpl = new TestImpl(out);
    }

    // Test method for scenario t2 with an error case
    @Test
    public void t2error() {
        // Print message indicating expected NullPointerException
        System.err.println("### The Following NullPointerException Are Expected ###");
        // Expect a specific exception to be thrown during the test
        expectException("java.lang.NullPointerException");
        // Set CLOCK to null to trigger the NullPointerException
        CLOCK = null;
        // Run the t2 test with an expected error using the YamlTester utility
        final YamlTester yt = YamlTester.runTest(newTestImplFunction(), TestOut.class, "yaml-tester/t2");
        // Assert that the actual output matches the expected output
        assertEquals("" +
                        "---\n" +
                        "---\n" +
                        "---",
                yt.actual());
        // Check that the test implementation is closed
        assertTrue(testImpl.isClosed());
    }

    // Test for mismatched scenarios
    @Test
    public void mismatched() {
        // Skip if REGRESS_TESTS is true
        assumeFalse(YamlTester.REGRESS_TESTS);
        // Expect an exception for a missing setup file
        expectException("setup.yaml not found");
        // Run the test with an expected mismatch in YAML files
        final YamlTester yt = YamlTester.runTest(TestImpl.class, "yaml-tester/mismatch");
        // Assert that the expected and actual outputs are not the same
        assertNotEquals("This tests an inconsistency was found, so they shouldn't be the same", yt.expected(), yt.actual());
    }

    // Test for handling comments in YAML files
    @Test
    public void comments() {
        // Note using YamlWire instead of TextWire moves comment 8
        final YamlTester yt = YamlTester.runTest(newTestImplFunction(), TestOut.class, "yaml-tester/comments");
        // Assert that the expected and actual outputs match
        assertEquals(yt.expected(), yt.actual());
    }

    // Test for direct text input and output
    @Test
    public void direct() throws IOException {
        // Skip if regress.tests property is set to true
        assumeFalse(Jvm.getBoolean("regress.tests"));
        // Create a TextMethodTester with direct string inputs and outputs
        YamlTester yt = new TextMethodTester<>(
                "=" +
                        "# comment 1\n" +
                        "---\n" +
                        "# comment 2\n" +
                        "time: 2022-05-17T20:25:02.002\n" +
                        "# comment 3\n" +
                        "...\n" +
                        "# comment 4\n" +
                        "---\n" +
                        "# comment 5\n" +
                        "testEvent: {\n" +
                        "  # comment 6\n" +
                        "  eventTime: 2022-05-17T20:25:01.001\n" +
                        "  # comment 7\n" +
                        "}\n" +
                        "# comment 8\n" +
                        "...\n" +
                        "# comment 9\n",
                newTestImplFunction(),
                TestOut.class,
                "=" +
                        "# comment 1\n" +
                        "# comment 2\n" +
                        "---\n" +
                        "# comment 3\n" +
                        "# comment 4\n" +
                        "# comment 5\n" +
                        "# comment 6\n" +
                        "# comment 7\n" +
                        "# comment 8\n" +
                        "---\n" +
                        "testEvent: {\n" +
                        "  eventTime: 2022-05-17T20:25:01.001,\n" +
                        "  processedTime: 2022-05-17T20:25:02.002,\n" +
                        "  currentTime: 2022-05-17T20:26:00\n" +
                        "}\n" +
                        "...\n" +
                        "# comment 9\n")
                .run();
        // Assert that the expected and actual outputs match
        assertEquals(yt.expected(), yt.actual());
    }

    @Test
    public void emptyDocuments() {
        final YamlTester yt = YamlTester.runTest(newTestImplFunction(), TestOut.class, "yaml-tester/empty-docs");
        assertEquals(yt.expected(), yt.actual());
    }
}
