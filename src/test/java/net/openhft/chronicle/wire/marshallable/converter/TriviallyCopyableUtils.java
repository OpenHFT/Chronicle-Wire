package net.openhft.chronicle.wire.marshallable.converter;

import net.openhft.chronicle.bytes.BytesUtil;

/**
 * The type Trivially copyable utils.
 */
public final class TriviallyCopyableUtils {

    private TriviallyCopyableUtils() {
    }

    public static int start(Class<?> c) {
        return BytesUtil.triviallyCopyableRange(c)[0];
    }

    public static int length(Class<?> c) {
        int[] BYTE_RANGE = BytesUtil.triviallyCopyableRange(c);
        return BYTE_RANGE[1] - BYTE_RANGE[0];
    }
}