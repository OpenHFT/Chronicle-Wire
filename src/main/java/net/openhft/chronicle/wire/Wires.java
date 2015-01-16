package net.openhft.chronicle.wire;

/**
 * Created by peter on 16/01/15.
 */
public enum Wires {
    ;
    static final ThreadLocal<StringBuilder> MyStringBuilder = new ThreadLocal<>();

    public static StringBuilder acquireStringBuilder() {
        StringBuilder sb = MyStringBuilder.get();
        if (sb == null)
            MyStringBuilder.set(sb = new StringBuilder());
        sb.setLength(0);
        return sb;
    }

    public static StringBuilder acquireAnotherStringBuilder(CharSequence cs) {
        StringBuilder sb = MyStringBuilder.get();
        if (sb == cs)
            return new StringBuilder();
        return acquireStringBuilder();
    }
}
