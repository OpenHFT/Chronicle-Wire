package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.wire.TextMethodTester;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.YamlMethodTester;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TextMethodTesterTest extends WireTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void run() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "tmtt/methods-in.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "tmtt/methods-out.yaml")
                .setup("tmtt/methods-out.yaml") // calls made here are not validated in the output.
                .run();
        compareResults(test);
    }

    @Test
    public void runTestEmptyOut() throws IOException {
        TextMethodTester test = new TextMethodTester<>(
                "tmtt/methods-in.yaml",
                NoopMockMethods::new,
                MockMethods.class,
                "tmtt/methods-out-empty.yaml")
                .setup("tmtt/methods-out.yaml") // calls made here are not validated in the output.
                .run();
        compareResults(test);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void runYaml() throws IOException {
        TextMethodTester test = new YamlMethodTester<>(
                "tmtt/methods-in.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "tmtt/methods-out.yaml")
                .setup("tmtt/methods-out.yaml") // calls made here are not validated in the output.
                .run();
        compareResults(test);
    }

    @Test
    public void checkExceptionsProvidedToHandler() throws IOException {
        List<Exception> exceptions = new ArrayList<>();
        TextMethodTester test = new TextMethodTester<>(
                "tmtt/methods-in-exception.yaml",
                MockMethodsImpl::new,
                MockMethods.class,
                "tmtt/methods-out-empty.yaml")
                .onInvocationException(exceptions::add)
                .run();
        compareResults(test);
        assertEquals(4, exceptions.size());
    }

    private void compareResults(TextMethodTester test) {
        assertEquals(test.expected().replaceAll("\\s+#", " #"),
                test.actual().replaceAll("\\s+#", " #"));
    }
}

