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

import net.openhft.chronicle.core.Jvm;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

// Test class extending MethodWriterTest to test behavior of method writers when using proxies
public class MethodWriterProxyTest extends MethodWriterTest {

    // Method to set up the test environment before each test method
    @Before
    public void before() {
        // Disable proxy code generation for the duration of the tests
        System.setProperty("disableProxyCodegen", "true");

        // Expect a specific warning message about falling back to proxy method writer
        expectException("Falling back to proxy method writer");
    }

    // Method to clean up and reset the environment after each test method
    @After
    public void after() {
        // Clear the property to re-enable proxy code generation
        System.clearProperty("disableProxyCodegen");
    }

    // Test method inherited from the parent class but ignored due to a known issue
    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/159")
    @Test
    public void multiOut() {
        // Calls the same test method from the parent class
        super.multiOut();
    }

    // Test method for testing primitives, ignored on specific conditions and known issues
    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/159")
    @Test
    public void testPrimitives() {
        // Skip the test on Mac ARM architecture
        assumeFalse(Jvm.isMacArm());

        // Calls the test method for primitives from the parent class
        super.doTestPrimitives(true);
    }

    // Method to check the type of the writer object in the context of this test class
    @Override
    protected void checkWriterType(Object writer) {
        // Skip the check on Mac ARM architecture
        assumeFalse(Jvm.isMacArm());

        // Assert that the writer object is a proxy class
        assertTrue(Proxy.isProxyClass(writer.getClass()));
    }
}
