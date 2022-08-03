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

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.utils.YamlTester;

import static org.junit.Assert.assertEquals;

public final class TestUtil {
    private TestUtil() {
    }

    public static void allowCommentsOutOfOrder(YamlTester yamlTester) {
        final String e = yamlTester.expected()
                .replaceAll("---\n", "");
        final String a = yamlTester.actual()
                .replaceAll("---\n", "");
        if (!e.equals(a))
            assertEquals(
                    yamlTester.expected(), yamlTester.actual());
    }
}
