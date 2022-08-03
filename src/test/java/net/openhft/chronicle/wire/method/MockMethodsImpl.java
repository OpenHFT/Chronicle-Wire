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

class MockMethodsImpl implements MockMethods {
    private final MockMethods out;

    public MockMethodsImpl(MockMethods out) {
        this.out = out;
    }

    @Override
    public void method1(MockDto dto) {
        out.method1(dto);
    }

    @Override
    public void method2(MockDto dto) {
        out.method2(dto);
    }

    @Override
    public void method3(List<MockDto> dtos) {
        out.method3(dtos);
    }

    @Override
    public void list(List<String> strings) {
        out.list(strings);
    }

    @Override
    public void throwException(String s) {
        throw new RuntimeException(s);
    }

    @Override
    public IMid mid(String text) {
        return out.mid(text);
    }
}
