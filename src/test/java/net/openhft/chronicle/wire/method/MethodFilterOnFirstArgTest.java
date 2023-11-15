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

import net.openhft.chronicle.wire.MethodFilterOnFirstArg;
import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

// Interface extending MethodFilterOnFirstArg for testing method filtering based on the first argument.
// It's used to record filtering choices and should be used only for testing purposes.
@SuppressWarnings("rawtypes")
interface MockMethods2 extends MethodFilterOnFirstArg {
    void method1(MockDto dto);

    void method2(String text, MockDto dto);

    void method3(MockDto dto, MockDto dto2);
}

// Test class for MethodFilterOnFirstArg functionality, extending WireTestCommon for thread and exception monitoring.
@SuppressWarnings("rawtypes")
public class MethodFilterOnFirstArgTest extends WireTestCommon {

    // Test method to verify ignoring methods based on the first argument
    @Test
    public void ignoreMethodBasedOnFirstArg() throws IOException {
        // Sets up a method tester with input and output YAML files, and the MockMethods2 interface
        TextMethodTester test = new TextMethodTester<>(
                "ignore-method/methods-in.yaml",
                MockMethods2Impl::new,
                MockMethods2.class,
                "ignore-method/methods-out.yaml")
                .run();

        // Asserts that the expected output matches the actual output
        assertEquals(test.expected(), test.actual());
    }
}

// Implementation of MockMethods2 and MethodFilterOnFirstArg for testing.
// It contains logic to decide which methods to ignore based on the first argument.
@SuppressWarnings("rawtypes")
class MockMethods2Impl implements MockMethods2, MethodFilterOnFirstArg {
    private final MockMethods2 out;

    // Constructor initializing with an instance of MockMethods2
    public MockMethods2Impl(MockMethods2 out) {
        this.out = out;
    }

    // Implementation of method1, delegating call to the out instance
    @Override
    public void method1(MockDto dto) {
        out.method1(dto);
    }

    // Implementation of method2, delegating call to the out instance
    @Override
    public void method2(String text, MockDto dto) {
        out.method2(text, dto);
    }

    // Implementation of method3, delegating call to the out instance
    @Override
    public void method3(MockDto dto, MockDto dto2) {
        out.method3(dto, dto2);
    }

    // Implements the logic to ignore methods based on the first argument
    @SuppressWarnings("unchecked")
    @Override
    public boolean ignoreMethodBasedOnFirstArg(String methodName, Object firstArg) {
        // Delegates the call to the out instance
        out.ignoreMethodBasedOnFirstArg(methodName, firstArg);

        // Logic for ignoring methods based on the first argument's value
        switch (methodName) {
            case "method1":
            default:
                // Throws AssertionError for method1 as it's not expected to be filtered here
                throw new AssertionError();

            case "method2":
                // Parses the first argument as a String and returns a boolean based on its value
                String text = (String) firstArg;
                return Boolean.parseBoolean(text);

            case "method3":
                // Casts the first argument to MockDto and returns true if a certain condition is met
                MockDto dto = (MockDto) firstArg;
                return dto.field2 <= 0;
        }
    }
}
