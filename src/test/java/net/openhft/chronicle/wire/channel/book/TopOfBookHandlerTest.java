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

package net.openhft.chronicle.wire.channel.book;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.utils.YamlAgitator;
import net.openhft.chronicle.wire.utils.YamlTester;
import net.openhft.chronicle.wire.utils.YamlTesterParametersBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static org.junit.Assume.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(Parameterized.class)
public class TopOfBookHandlerTest extends WireTestCommon {
    static final String paths = "" +
            "echo-tob";

    final String name;
    final YamlTester tester;

    public TopOfBookHandlerTest(String name, YamlTester tester) {
        this.name = name;
        this.tester = tester;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        return new YamlTesterParametersBuilder<>(out -> new EchoTopOfBookHandler().out(out), TopOfBookListener.class, paths)
                .agitators(
                        YamlAgitator.messageMissing(),
                        YamlAgitator.duplicateMessage(),
                        YamlAgitator.overrideFields("ecn: RFX"),
                        YamlAgitator.missingFields("bidPrice"))
                .get();
    }

    @Test
    public void runTester() {
        // uses trivially copyable objects
        assumeFalse(Jvm.isAzulZing());

        assertEquals(tester.expected(), tester.actual());
    }
}