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

import net.openhft.chronicle.core.time.SetTimeProvider;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecated")
@RunWith(Parameterized.class)
public class YamlTesterParametersTest extends WireTestCommon {
    // Paths to the YAML test files
    static final String[] paths = {
            "yaml-tester/t1",
            "yaml-tester/t2",
            "yaml-tester/t3",
            "yaml-tester/comments-new"};

    // Name of the test and the YamlTester instance
    final String name;
    final YamlTester tester;

    // Constructor that assigns name and tester
    public YamlTesterParametersTest(String name, YamlTester tester) {
        this.name = name;
        this.tester = tester;
    }

    // Generates parameters for the test cases
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        // Builds parameters for the test using YamlTesterParametersBuilder
        // Some agitators are ignored as duplicates
        return new YamlTesterParametersBuilder<>(TestImpl::new, TestOut.class, Arrays.asList(paths))
                .agitators(
                        YamlAgitator.messageMissing(),
                        YamlAgitator.messageMissing(),
                        // Duplicate messageMissing agitator is ignored
                        YamlAgitator.duplicateMessage(),
                        YamlAgitator.duplicateMessage(),
                        YamlAgitator.duplicateMessage(),
                        // Duplicate duplicateMessage agitators are ignored
                        YamlAgitator.missingFields("eventTime"),
                        YamlAgitator.overrideFields("eventTime: 1999-01-01T01:01:01"),
                        YamlAgitator.replaceAll("5 to 6", "[5]", "6"))
                .inputFunction(s -> s.replace("# Replace comment", ""))
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
        // Setting a custom time provider for the test
        SystemTimeProvider.CLOCK = new SetTimeProvider("2022-05-17T20:26:00")
                .autoIncrement(1, TimeUnit.MICROSECONDS);

        // Asserting that the expected output matches the actual output
        assertEquals(tester.expected(), tester.actual());
    }
}
