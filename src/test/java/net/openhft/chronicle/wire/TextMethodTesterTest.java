package net.openhft.chronicle.wire;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TextMethodTesterTest extends WireTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void run() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "methods-in.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "methods-in.yaml")
                .setup("methods-in.yaml") // calls made here are not validated in the output.
                .run();
        assertEquals(test.expected(), test.actual());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void runYaml() throws IOException {
        TextMethodTester test = new YamlMethodTester<>(
                "methods-in.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "methods-in.yaml")
                .setup("methods-in.yaml") // calls made here are not validated in the output.
                .run();
        assertEquals(test.expected(), test.actual());
    }
}

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
}