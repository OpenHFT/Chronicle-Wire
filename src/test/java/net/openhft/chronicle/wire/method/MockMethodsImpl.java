package net.openhft.chronicle.wire.method;

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
}
