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

import net.openhft.chronicle.wire.IMid;

import java.util.List;

/**
 * Implementation of the MockMethods interface, primarily used to delegate method calls
 * to another instance of MockMethods, potentially for mocking or proxying behaviors.
 */
class MockMethodsImpl implements MockMethods {
    private final MockMethods out; // Target instance to which method calls are delegated

    // Constructor to initialize with an instance of MockMethods
    public MockMethodsImpl(MockMethods out) {
        this.out = out;
    }

    // Delegate the call of method1 to the 'out' instance
    @Override
    public void method1(MockDto dto) {
        out.method1(dto);
    }

    // Delegate the call of method2 to the 'out' instance
    @Override
    public void method2(MockDto dto) {
        out.method2(dto);
    }

    // Delegate the call of method3 to the 'out' instance
    @Override
    public void method3(List<MockDto> dtos) {
        out.method3(dtos);
    }

    // Delegate the call of list method to the 'out' instance
    @Override
    public void list(List<String> strings) {
        out.list(strings);
    }

    // Implementation of throwException that throws a runtime exception
    @Override
    public void throwException(String s) {
        throw new RuntimeException(s);
    }

    // Delegate the call of mid method to the 'out' instance and return the result
    @Override
    public IMid mid(String text) {
        return out.mid(text);
    }
}
