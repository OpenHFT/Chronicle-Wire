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

import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.wire.IMid;

import java.util.List;

/**
 * A no-operation (noop) implementation of the MockMethods interface, intended for use in scenarios
 * where the actual method implementations are not needed, such as testing or stubbing.
 * This class also implements the IgnoresEverything interface, indicating that it ignores all method calls.
 */
public class NoopMockMethods implements MockMethods, IgnoresEverything {
    private final MockMethods mockMethods; // Reference to an instance of MockMethods, potentially for delegation

    // Constructor to initialize with a MockMethods instance
    public NoopMockMethods(MockMethods mockMethods) {
        this.mockMethods = mockMethods;
    }

    // No-operation implementation of method1
    @Override
    public void method1(MockDto dto) {
        // Intentionally left empty
    }

    // No-operation implementation of method2
    @Override
    public void method2(MockDto dto) {
        // Intentionally left empty
    }

    // No-operation implementation of method3
    @Override
    public void method3(List<MockDto> dtos) {
        // Intentionally left empty
    }

    // No-operation implementation of list method
    @Override
    public void list(List<String> strings) {
        // Intentionally left empty
    }

    // No-operation implementation of throwException
    @Override
    public void throwException(String s) {
        // Intentionally left empty
    }

    // Returns a lambda for IMid interface, which in turn returns a no-operation lambda for IMid methods
    @Override
    public IMid mid(String text) {
        return x -> t -> {
            // Intentionally left empty
        };
    }
}
