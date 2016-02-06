package net.openhft.chronicle.wire;

/**
 * Created by peter.lawrey on 06/02/2016.
 */
@FunctionalInterface
public interface WireParselet<O> {
    void accept(CharSequence s, ValueIn in, O out);
}
