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

package run.chronicle.account;

import net.openhft.chronicle.core.time.SetTimeProvider;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.converter.ShortText;
import net.openhft.chronicle.wire.utils.*;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import run.chronicle.account.api.AccountsOut;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class AccountsTest extends WireTestCommon {
    // Test scenarios located in the following paths
    static final String[] paths = {
            "account/simple",
            "account/mixed",
            "account/waterfall"
    };
    static final long VAULT = ShortText.INSTANCE.parse("vault");

    // Name of the test case and the YamlTester instance
    final String name;
    final YamlTester tester;

    // Constructor initializing test name and tester
    public AccountsTest(String name, YamlTester tester) {
        this.name = name;
        this.tester = tester;
    }

    // Parameters for the test cases
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        // Building parameters for the YamlTester
        return new YamlTesterParametersBuilder<>(out -> new AccountsImpl(out).id(VAULT), AccountsOut.class, Arrays.asList(paths))
                .agitators(
                        YamlAgitator.messageMissing(),
                        YamlAgitator.duplicateMessage(),
                        YamlAgitator.overrideFields("currency: , amount: NaN, amount: -1, target: no-vault".split(", *")),
                        YamlAgitator.missingFields("name, account, sender, target, sendingTime, from, to, currency, amount, reference".split(", *")))
                .exceptionHandlerFunction(out -> (log, msg, thrown) -> out.jvmError(thrown == null ? msg : (msg + " " + thrown)))
                .exceptionHandlerFunctionAndLog(true)
                .get();
    }

    // Method to reset the clock after each test
    @After
    public void tearDown() {
        SystemTimeProvider.CLOCK = SystemTimeProvider.INSTANCE;
    }

    // Test method to run the YamlTester
    @Test
    public void runTester() {
        // Setting the clock with a specific time and auto increment
        SystemTimeProvider.CLOCK = new SetTimeProvider("2023-01-20T10:10:00")
                .autoIncrement(1, TimeUnit.MILLISECONDS);

        // Asserting that the expected output matches the actual output
        assertEquals(tester.expected(), tester.actual());
    }
}
