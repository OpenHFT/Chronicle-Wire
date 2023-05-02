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
import net.openhft.chronicle.wire.converter.Base85;
import net.openhft.chronicle.wire.utils.ErrorListener;
import net.openhft.chronicle.wire.utils.YamlAgitator;
import net.openhft.chronicle.wire.utils.YamlTester;
import net.openhft.chronicle.wire.utils.YamlTesterParametersBuilder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import run.chronicle.account.api.AccountsOut;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(Parameterized.class)
public class AccountsTest extends WireTestCommon {
    static final String paths = "" +
            "account/simple," +
            "account/mixed," +
            "account/waterfall";
    static final long VAULT = Base85.INSTANCE.parse("vault");

    final String name;
    final YamlTester tester;

    public AccountsTest(String name, YamlTester tester) {
        this.name = name;
        this.tester = tester;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        return new YamlTesterParametersBuilder<>(out -> new AccountsImpl(out).id(VAULT), AccountsOut.class, paths)
                .agitators(
                        YamlAgitator.messageMissing(),
                        YamlAgitator.duplicateMessage(),
                        YamlAgitator.overrideFields("currency: , amount: NaN, amount: -1, target: no-vault".split(", *")),
                        YamlAgitator.missingFields("name, account, sender, target, sendingTime, from, to, currency, amount, reference".split(", *")))
                .exceptionHandlerFunction(out -> (log, msg, thrown) -> ((ErrorListener) out).jvmError(thrown == null ? msg : (msg + " " + thrown)))
                .exceptionHandlerFunctionAndLog(true)
                .get();
    }

    @After
    public void tearDown() {
        SystemTimeProvider.CLOCK = SystemTimeProvider.INSTANCE;
    }

    @Test
    public void runTester() {
        SystemTimeProvider.CLOCK = new SetTimeProvider("2023-01-20T10:10:00")
                .autoIncrement(1, TimeUnit.MILLISECONDS);
        assertEquals(tester.expected(), tester.actual());
    }
}
