package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

public class MethodWireKey extends AbstractMarshallable implements WireKey {
    private final String name;
    private final int code;

    public MethodWireKey(String name, int code) {
        this.name = name;
        this.code = code;
    }

    @NotNull
    @Override
    public String name() {
        return name == null ? Integer.toString(code) : name;
    }

    @Override
    public int code() {
        return code;
    }
}
