package net.openhft.chronicle.wire;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TextMethodTesterTest extends WireTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void run() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "methods-out.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "methods-out.yaml")
                .setup("methods-out.yaml") // calls made here are not validated in the output.
                .run();
        assertEquals(test.expected(), test.actual());
    }

    @Test
    public void runTestEmptyOut() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "methods-out.yaml",
                NoopMockMethods::new,
                MockMethods.class,
                "methods-out-empty.yaml")
                .setup("methods-out.yaml") // calls made here are not validated in the output.
                .run();
        assertEquals(test.expected(), test.actual());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void runYaml() throws IOException {
        TextMethodTester test = new YamlMethodTester<>(
                "methods-out.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "methods-out.yaml")
                .setup("methods-out.yaml") // calls made here are not validated in the output.
                .run();
        assertEquals(test.expected(), test.actual());
    }
}

