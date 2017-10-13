package net.openhft.chronicle.wire;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

// only extend MethodFilterOnFirstArg for testing purposes.
// Don't do this unless you need to record your filtering choices.
interface MockMethods2 extends MethodFilterOnFirstArg {
    void method1(MockDto dto);

    void method2(String text, MockDto dto);

    void method3(MockDto dto, MockDto dto2);
}

public class MethodFilterOnFirstArgTest {
    @Test
    public void ignoreMethodBasedOnFirstArg() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "ignore-method/methods-in.yaml",
                net.openhft.chronicle.wire.MockMethods2Impl::new,
                MockMethods2.class,
                "ignore-method/methods-out.yaml")
                .run();
        assertEquals(test.expected(), test.actual());
    }
}

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