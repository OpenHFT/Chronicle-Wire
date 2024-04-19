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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("deprecated")
public class YamlTesterAbstractMarshallableCfgTest extends WireTestCommon {
    // Test method for AbstractMarshallableCfg reset behavior
    @Test
    public void testAbstractMarshallableCfgResetTest() {
        // Running the YAML tester with specific implementation and output class
        final YamlTester yt = YamlTester.runTest(TestImpl::new, TestOut.class, "yaml-tester/tamc");

        // Asserting that the expected output from the YAML file matches the actual output
        assertEquals(yt.expected(), yt.actual());
    }
}
