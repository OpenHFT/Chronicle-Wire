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

package net.openhft.chronicle.wire.method;

import org.junit.After;
import org.junit.Before;

// Test class extending MethodWriter2Test to evaluate behavior of method writers with enforced proxy generation
public class MethodWriterProxy2Test extends MethodWriter2Test {

    // Set up the environment before each test
    @Before
    public void before() {
        // Enforce proxy generation by disabling proxy code generation
        System.setProperty("disableProxyCodegen", "true");

        // Expect a warning message indicating the use of a proxy method writer
        expectException("Falling back to proxy method writer");
    }

    // Clean up and reset the environment after each test
    @After
    public void after() {
        // Clear the property to revert to the default method writer generation behavior
        System.clearProperty("disableProxyCodegen");
    }
}
