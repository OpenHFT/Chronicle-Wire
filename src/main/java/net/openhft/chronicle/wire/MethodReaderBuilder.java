package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Created by peter on 17/05/2017.
 */
public class MethodReaderBuilder {
    private final MarshallableIn in;
    private boolean ignoreDefaults;
    private WireParselet defaultParselet = (s, v, $) -> {
        MessageHistory history = MessageHistory.get();
        long sourceIndex = history.lastSourceIndex();
        MethodReader.LOGGER.debug("Unknown method-name='" + s + "' " + v.text() + " from " + history.lastSourceId() + " at " + Long.toHexString(sourceIndex) + " ~ " + (int) sourceIndex);
    };

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

    public WireParselet defaultParselet() {
        return defaultParselet;
    }

    public MethodReaderBuilder defaultParselet(WireParselet defaultParselet) {
        this.defaultParselet = defaultParselet;
        return this;
    }

    @NotNull
    public MethodReader build(Object... impls) {
        return new MethodReader(in, ignoreDefaults, defaultParselet, impls);
    }
}
