package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Rob Austin
 */
public enum CoreFields implements WireKey {
    tid,
    csp,
    cid,
    reply,
    exception;

    private static StringBuilder eventName = new StringBuilder();

    private static long longEvent(@NotNull final WireKey expecting, @NotNull final WireIn wire) {
        final ValueIn valueIn = wire.readEventName(eventName);
        if (expecting.contentEquals(eventName))
            return valueIn.int64();

        throw new IllegalArgumentException("expecting a " + expecting);
    }

    private static StringBuilder stringEvent(@NotNull final WireKey expecting, StringBuilder using, @NotNull final WireIn wire) {
        final ValueIn valueIn = wire.readEventName(eventName);
        if (expecting.contentEquals(eventName)) {
            valueIn.text(using);
            return using;
        }

        throw new IllegalArgumentException("expecting a " + expecting);
    }

    public static long tid(@NotNull final WireIn wire) {
        return longEvent(CoreFields.tid, wire);
    }

    public static long cid(@NotNull final WireIn wire) {
        return longEvent(CoreFields.cid, wire);
    }

    private final static StringBuilder cpsBuilder = new StringBuilder();

    public static StringBuilder csp(@NotNull final WireIn wire) {
        return stringEvent(CoreFields.csp, cpsBuilder, wire);
    }
}
