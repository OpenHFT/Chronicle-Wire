package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

import java.util.function.Function;

public class YamlMethodTester<T> extends TextMethodTester<T> {
    public YamlMethodTester(String input, Function<T, Object> componentFunction, Class<T> outputClass, String output) {
        super(input, componentFunction, outputClass, output);
    }

    @Override
    protected Wire createWire(Bytes bytes) {
        return new YamlWire(bytes).useTextDocuments();
    }
}
