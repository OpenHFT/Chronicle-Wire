package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.wire.IMid;

import java.util.List;

public class NoopMockMethods implements MockMethods, IgnoresEverything {
    private final MockMethods mockMethods;

    public NoopMockMethods(MockMethods mockMethods) {
        this.mockMethods = mockMethods;
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

    @Override
    public IMid mid(String text) {
        return x -> t -> {
        };
    }
}
