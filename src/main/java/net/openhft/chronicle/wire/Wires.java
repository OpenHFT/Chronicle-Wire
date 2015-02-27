package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.StringBuilderPool;

/**
 * Created by peter on 16/01/15.
 */
public enum Wires {
    ;
    static final StringBuilderPool SBP = new StringBuilderPool();

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    public static StringBuilder acquireAnotherStringBuilder(CharSequence cs) {
        StringBuilder sb = SBP.acquireStringBuilder();
        if (sb == cs)
            return new StringBuilder();
        return acquireStringBuilder();
    }
}
