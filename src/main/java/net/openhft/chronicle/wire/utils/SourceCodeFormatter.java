package net.openhft.chronicle.wire.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * simple java source code formatter, will indent on a "{" and reduce the indent on a "}" all spaces before a "/n" are removed to enforce a consistent
 * format
 */
public class SourceCodeFormatter implements Appendable, CharSequence {
    private final String indentSpaces;
    private final AtomicInteger indent;
    private final StringBuilder sb = new StringBuilder();
    private int lastNewlineIndex = 0;
    private boolean lastChargeWasNewLine = false;

    public SourceCodeFormatter(int indentSpaces, AtomicInteger indent) {
        this.indentSpaces = "        ".substring(0, indentSpaces);
        this.indent = indent;
    }

    public SourceCodeFormatter(int indentSpaces) {
        this(indentSpaces, new AtomicInteger(0));
    }

    public SourceCodeFormatter(int indentSpaces, int i) {
        this(indentSpaces, new AtomicInteger(i));
    }

    @NotNull
    public String toString() {
        return sb.toString();
    }

    @Override
    public SourceCodeFormatter append(final CharSequence csq) {
        append(csq, 0, csq.length());
        return this;
    }

    @Override
    public SourceCodeFormatter append(final CharSequence csq, final int start, final int end) {
        for (int i = start; i < end; i++)
            append(csq.charAt(i));

        return this;
    }

    @Override
    public SourceCodeFormatter append(char c) {
        sb.append(c);
        switch (c) {
            case '\n':
                lastNewlineIndex = sb.length();
                lastChargeWasNewLine = true;
                padding(indent.get());
                break;
            case '{':
                indent.incrementAndGet();
                break;
            case '}':
                indent.decrementAndGet();
                if (lastNewlineIndex >= 0) {
                    sb.setLength(lastNewlineIndex);
                    padding(indent.get());
                    sb.append(c);
                }
                break;
            case ' ':
                if (lastChargeWasNewLine) {
                    // ignore whitespace after newline
                    sb.setLength(sb.length() - 1);
                }
                break;
            default:
                lastChargeWasNewLine = false;
                break;
        }
        return this;
    }

    public void setLength(int len) {
        sb.setLength(len);
    }

    private void padding(final int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(indentSpaces);
        }
    }

    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(final int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return sb.subSequence(start, end);
    }

    public SourceCodeFormatter append(long i) {
        sb.append(i);
        return this;
    }

    public SourceCodeFormatter append(double d) {
        sb.append(d);
        return this;
    }

    public SourceCodeFormatter append(boolean flag) {
        sb.append(flag);
        return this;
    }

    public <Stringable> SourceCodeFormatter append(Stringable stringable) {
        sb.append(stringable);
        return this;
    }
}
