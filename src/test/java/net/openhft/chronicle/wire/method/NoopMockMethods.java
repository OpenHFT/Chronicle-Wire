package net.openhft.chronicle.wire.method;

import java.util.List;

public class NoopMockMethods implements MockMethods {
    public NoopMockMethods(MockMethods mockMethods) {
    }

    @Override
    public void method1(MockDto dto) {
    }

    @Override
    public void method2(MockDto dto) {
    }

    @Override
    public void method3(List<MockDto> dtos) {
    }

    @Override
    public void list(List<String> strings) {
    }

    @Override
    public void throwException(String s) {
    }
}
