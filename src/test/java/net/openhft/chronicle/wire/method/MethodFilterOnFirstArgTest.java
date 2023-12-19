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

// only extend MethodFilterOnFirstArg for testing purposes.
// Don't do this unless you need to record your filtering choices.
@SuppressWarnings("rawtypes")
interface MockMethods2 extends MethodFilterOnFirstArg {
    void method1(MockDto dto);

    void method2(String text, MockDto dto);

    void method3(MockDto dto, MockDto dto2);
}

@SuppressWarnings("rawtypes")
public class MethodFilterOnFirstArgTest extends WireTestCommon {
    @Test
    public void ignoreMethodBasedOnFirstArg() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "ignore-method/methods-in.yaml",
                MockMethods2Impl::new,
                MockMethods2.class,
                "ignore-method/methods-out.yaml")
                .run();
        assertEquals(test.expected(), test.actual());
    }
}

@SuppressWarnings("rawtypes")
class MockMethods2Impl implements MockMethods2, MethodFilterOnFirstArg {
    private final MockMethods2 out;

    public MockMethods2Impl(MockMethods2 out) {
        this.out = out;
    }

    @Override
    public void method1(MockDto dto) {
        out.method1(dto);
    }

    @Override
    public void method2(String text, MockDto dto) {
        out.method2(text, dto);
    }

    @Override
    public void method3(MockDto dto, MockDto dto2) {
        out.method3(dto, dto2);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean ignoreMethodBasedOnFirstArg(String methodName, Object firstArg) {
        out.ignoreMethodBasedOnFirstArg(methodName, firstArg);
        switch (methodName) {
            case "method1":
            default:
                throw new AssertionError();

            case "method2":
                String text = (String) firstArg;
                return Boolean.parseBoolean(text);

            case "method3":
                MockDto dto = (MockDto) firstArg;
                return dto.field2 <= 0;
        }
    }
}
