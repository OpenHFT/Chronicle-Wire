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