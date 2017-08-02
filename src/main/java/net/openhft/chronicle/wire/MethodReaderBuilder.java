package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/*
 * Created by peter on 17/05/2017.
 */
public class MethodReaderBuilder {
    private final MarshallableIn in;
    private boolean ignoreDefaults;
    private WireParselet defaultParselet = createDefaultParselet();
    private MethodReaderInterceptor methodReaderInterceptor;

    public MethodReaderBuilder(MarshallableIn in) {
        this.in = in;
    }

    // TODO add support for filtering.

    @NotNull
    static WireParselet createDefaultParselet() {
        return (s, v, $) -> {
            MessageHistory history = MessageHistory.get();
            long sourceIndex = history.lastSourceIndex();
            v.skipValue();
            if (s.length() == 0)
                MethodReader.LOGGER.warn(errorMsg(s, history, sourceIndex));
            else if (MethodReader.LOGGER.isDebugEnabled())
                MethodReader.LOGGER.debug(errorMsg(s, history, sourceIndex));
        };
    }

    @NotNull
    private static String errorMsg(CharSequence s, MessageHistory history, long sourceIndex) {
        return "Unknown method-name='" + s + "' from " + history.lastSourceId() + " at " + Long.toHexString(sourceIndex) + " ~ " + (int) sourceIndex;
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
        return new MethodReader(in, ignoreDefaults, defaultParselet, methodReaderInterceptor, impls);
    }

    public MethodReaderBuilder methodReaderInterceptor(MethodReaderInterceptor methodReaderInterceptor) {
        this.methodReaderInterceptor = methodReaderInterceptor;
        return this;
    }
}
