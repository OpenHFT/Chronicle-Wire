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

import static org.junit.Assert.assertEquals;

/**
 * TestUtil contains utility methods that aid in testing,
 * particularly those involving YAML testing.
 */
@SuppressWarnings("deprecation")
public final class TestUtil {

    /**
     * Private constructor to prevent instantiation of the utility class.
     */
    private TestUtil() {
    }

    /**
     * Allow comments out of order - however, based on the method
     * implementation it seems like it's handling a YAML document separator "---"
     * and ensuring that expected and actual YAML strings are equal
     * after stripping the document separators.
     *
     * @param yamlTester a tester object presumably holding actual and expected YAML strings.
     */
    public static void allowCommentsOutOfOrder(net.openhft.chronicle.wire.utils.YamlTester yamlTester) {
        // Removing YAML document separators from expected and actual strings.
        final String e = yamlTester.expected()
                .replaceAll("---\n", "");
        final String a = yamlTester.actual()
                .replaceAll("---\n", "");

        // Asserting equality, reverting to full original strings for output if assertion fails.
        if (!e.equals(a))
            assertEquals(
                    yamlTester.expected(), yamlTester.actual());
    }
}
