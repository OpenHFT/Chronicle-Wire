package net.openhft.chronicle.wire;

import java.util.List;

interface MockMethods {
    void method1(MockDto dto);

    void method2(MockDto dto);

    void method3(List<MockDto> dtos);

    void list(List<String> strings);
}
