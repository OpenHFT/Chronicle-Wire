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
import net.openhft.chronicle.wire.utils.api.OMSOut;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(Parameterized.class)
public class OMSEventsTest extends WireTestCommon {
    static final String paths = "" +
            "yaml-tester/oms";

    final String name;
    final YamlTester tester;

    public OMSEventsTest(String name, YamlTester tester) {
        this.name = name;
        this.tester = tester;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        return new YamlTesterParametersBuilder<>(OMSImpl::new, OMSOut.class, paths).agitators(new YamlAgitator[]{YamlAgitator.messageMissing(), YamlAgitator.duplicateMessage()}).get();
    }

    @After
    public void tearDown() {
        SystemTimeProvider.CLOCK = SystemTimeProvider.INSTANCE;
    }

    @Test
    public void runTester() {
        SystemTimeProvider.CLOCK = new SetTimeProvider("2019-12-03T09:54:40")
                .autoIncrement(1, TimeUnit.MILLISECONDS);
        assertEquals(tester.expected(), tester.actual());
    }
}
