package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wrapper for handling method reader events with generated code.
 * Attempts to perform best-effort processing with a generated parsing function {@link #parseOneFunction}.
 * In case it is unsuccessful, processing is delegated to a lazy-initialized {@link #delegate}.
 */
public class LazyDelegatingWireParser implements WireParser {
    private final Function<WireIn, Boolean> parseOneFunction;
    private final Supplier<WireParser> delegateSupplier;
    private WireParser delegate;

    public LazyDelegatingWireParser(Function<WireIn, Boolean> parseOneFunction,
                                    Supplier<WireParser> delegateSupplier) {
        this.parseOneFunction = parseOneFunction;
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public WireParselet getDefaultConsumer() {
        return delegate().getDefaultConsumer();
    }

    @Override
    public void parseOne(@NotNull WireIn wireIn) {
        final Bytes<?> bytes = wireIn.bytes();
        long start = bytes.readPosition();

        if (parseOneFunction.apply(wireIn))
            return;

        bytes.readPosition(start);
        delegate().parseOne(wireIn);
    }

    @Override
    public WireParselet lookup(CharSequence name) {
        return delegate().lookup(name);
    }

    @Override
    public @NotNull VanillaWireParser register(String keyName, WireParselet valueInConsumer) {
        return delegate().register(keyName, valueInConsumer);
    }

    private WireParser delegate() {
        if (delegate == null)
            delegate = delegateSupplier.get();

        return delegate;
    }
}
