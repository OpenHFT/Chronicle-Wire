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

import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.utils.api.TestRMIn;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(Parameterized.class)
public class YamlTesterReadMarshallableTest extends WireTestCommon {
    static final String paths = "" +
            "yaml-tester/rm,"+
            "yaml-tester/rm-indent";

    final String name;
    final YamlTester tester;

    public YamlTesterReadMarshallableTest(String name, YamlTester tester) {
        this.name = name;
        this.tester = tester;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        // ignored as duplicate
        // ignored
        // also ignored as duplicates
        return new YamlTesterParametersBuilder<>(TestRMImpl::new, TestRMIn.class, paths)
                .agitators(
                        YamlAgitator.missingFields("a", "b", "c"))
                .get();
    }

    @Test
    public void runTester() {
        assertEquals(tester.expected(), tester.actual());
    }
}
