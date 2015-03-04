package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.StringBuilderPool;

/**
 * Created by peter.lawrey on 16/01/15.
 */
public enum Wires {
    ;
    static final StringBuilderPool SBP = new StringBuilderPool();
    static final StringBuilderPool ASBP = new StringBuilderPool();

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    public static StringBuilder acquireAnotherStringBuilder(CharSequence cs) {
        StringBuilder sb = ASBP.acquireStringBuilder();
        assert sb != cs;
        return sb;
    }
}
