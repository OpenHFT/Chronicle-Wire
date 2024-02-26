package net.openhft.chronicle.wire.trivial;

import net.openhft.chronicle.bytes.BytesUtil;
public final class TriviallyCopyableUtils {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private TriviallyCopyableUtils() {
    }

    /**
     * Returns the start offset of the trivially copyable range for a given class.
     * This is the offset of the first byte of the trivially copyable fields.
     *
     * @param c The class to examine.
     * @return The start offset of the trivially copyable range.
     */
    public static int start(Class<?> c) {
        return BytesUtil.triviallyCopyableRange(c)[0];
    }

    /**
     * Returns the length of the trivially copyable range for a given class.
     * This is calculated as the difference between the end offset and the start
     * offset of the trivially copyable fields. It represents the total number
     * of bytes that can be copied trivially.
     *
     * @param c The class to examine.
     * @return The length of the trivially copyable range.
     */
    public static int length(Class<?> c) {
        int[] BYTE_RANGE = BytesUtil.triviallyCopyableRange(c);
        return BYTE_RANGE[1] - BYTE_RANGE[0];
    }
}