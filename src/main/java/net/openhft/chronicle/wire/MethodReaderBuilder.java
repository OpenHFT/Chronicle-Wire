package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Created by peter on 17/05/2017.
 */
public class MethodReaderBuilder {
    private final MarshallableIn in;
    private boolean ignoreDefaults;

    // TODO add support for filtering.

    public MethodReaderBuilder(MarshallableIn in) {
        this.in = in;
    }

    public boolean ignoreDefaults() {
        return ignoreDefaults;
    }

    @NotNull
    public MethodReaderBuilder ignoreDefaults(boolean ignoreDefaults) {
        this.ignoreDefaults = ignoreDefaults;
        return this;
    }

    @NotNull
    public MethodReader build(Object... impls) {
        return new MethodReader(in, ignoreDefaults, impls);
    }
}
