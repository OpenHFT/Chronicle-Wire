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

import net.openhft.chronicle.core.StackTrace;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecated")
@RunWith(Parameterized.class)
public class YamlTesterErrorTest extends WireTestCommon {
    // Path to the YAML test files
    static final String paths = "" +
            "yaml-tester/errors";

    // Name of the test and the YamlTester instance
    final String name;
    final YamlTester tester;

    // Constructor assigns name and tester instance
    public YamlTesterErrorTest(String name, YamlTester tester) {
        this.name = name;
        this.tester = tester;
    }

    // Generates parameters for the test cases
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        // Builds parameters for the test using YamlTesterParametersBuilder
        return new YamlTesterParametersBuilder<>(ErrorsImpl::new, ErrorsOut.class, paths)
                .exceptionHandlerFunction(out -> (log, msg, thrown) -> ((ErrorListener) out).jvmError(thrown == null ? msg : (msg + " " + thrown)))
                .exceptionHandlerFunctionAndLog(true)
                .addOutputClass(ErrorListener.class)
                .agitators(YamlAgitator.duplicateMessage())
                .get();
    }

    // Resets the SystemTimeProvider after each test
    @After
    public void tearDown() {
        SystemTimeProvider.CLOCK = SystemTimeProvider.INSTANCE;
    }

    // Test method to run the YAML tester
    @Test
    public void runTester() {
        // Ignoring and expecting specific exceptions during the test
        ignoreException(" to the classpath");
        expectException("Unknown method-name='unknownMethod'");
        expectException((ExceptionKey ek) -> ek.throwable instanceof StackTrace, "StackTrace");
        expectException((ExceptionKey ek) -> ek.throwable instanceof ErrorsImpl.MyAssertionError, "MyAssertionError");

        // Expecting specific error and warning messages
        expectException("warning one");
        expectException("error one");
        expectException("exception one");
        expectException("warnings done");

        // Asserting that the expected output matches the actual output
        assertEquals(tester.expected(), tester.actual());
    }
}
