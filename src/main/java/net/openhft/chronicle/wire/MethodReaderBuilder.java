package net.openhft.chronicle.wire;

/**
 * Created by peter on 17/05/2017.
 */
public class MethodReaderBuilder {
    final MarshallableIn in;
    boolean ignoreDefaults;

    // TODO add support for filtering.

    public MethodReaderBuilder(MarshallableIn in) {
        this.in = in;
    }

    public boolean ignoreDefaults() {
        return ignoreDefaults;
    }

    public MethodReaderBuilder ignoreDefaults(boolean ignoreDefaults) {
        this.ignoreDefaults = ignoreDefaults;
        return this;
    }

    public MethodReader build(Object... impls) {
        return new MethodReader(in, ignoreDefaults, impls);
    }
}
