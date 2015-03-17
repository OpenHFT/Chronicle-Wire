package net.openhft.chronicle.wire;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 *
 * Created by peter.lawrey on 12/01/15.
 */
public interface Wire extends WireIn, WireOut {
    static final int NOT_READY = 1 << 31;
    static final int META_DATA = 1 << 30;
    static final int UNKNOWN_LENGTH = 0x0;
    static final int LENGTH_MASK = -1 >>> 2;
}
