package net.openhft.chronicle.wire;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

interface Flow1 {
    Flow2 first(String args);
}

interface Flow2 {
    Flow3 second(long num);
}

interface Flow3 {
    void third(List<String> list);
}

@Ignore
public class FlowMethodTest extends WireTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void runYaml() throws IOException {
        TextMethodTester test = new YamlMethodTester<>(
                "flow-in.yaml",
                out -> out,
                Flow1.class,
                "flow-in.yaml")
                .setup("flow-in.yaml") // calls made here are not validated in the output.
                .run();
        assertEquals(test.expected(), test.actual());
    }
}