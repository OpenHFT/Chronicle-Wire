package net.openhft.chronicle.wire.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * simple java source code formatter, will indent on a "{" and reduce the indent on a "}" all spaces before a "/n" are removed to enforce a consistent
 * format
 */
public class JavaSouceCodeFormatter implements Appendable, CharSequence {

    private static final int INDENT_SPACES = 4;
    private final AtomicInteger indent;
    private final StringBuilder response = new StringBuilder();
    private StringBuilder sb = new StringBuilder();

    public JavaSouceCodeFormatter(AtomicInteger indent) {
        this.indent = indent;
    }

    public JavaSouceCodeFormatter() {
        this.indent = new AtomicInteger(0);
    }

    public JavaSouceCodeFormatter(int i) {
        this.indent = new AtomicInteger(i);
    }

    @NotNull
    public String toString() {
        return sb.toString();
    }

    @Override
    public Appendable append(final CharSequence csq) {
        return sb.append(replaceNewLine(csq, 0, csq.length() - 1));
    }

    private CharSequence replaceNewLine(final CharSequence csq, int start, int end) {
        response.setLength(0);
        boolean lastChargeWasNewLine = true;
        int lastNewlineIndex = 0;
        for (int i = start; i <= end; i++) {
            char c = csq.charAt(i);

            response.append(c);
            if (c == '\n') {

                lastNewlineIndex = response.length();
                lastChargeWasNewLine = true;
                padding(response, indent.get());
            } else if (c == '{') {
                indent.incrementAndGet();
            } else if (c == '}') {
                indent.decrementAndGet();
                if (lastNewlineIndex >= 0) {
                    response.setLength(lastNewlineIndex);
                    padding(response, indent.get());
                    response.append("}");
                }

            } else if (lastChargeWasNewLine && c == ' ') {
                // ignore whitespace after newline
                response.setLength(response.length() - 1);
            } else {
                lastChargeWasNewLine = false;
            }
        }

        return response;
    }

    public void setLength(int len) {
        sb.setLength(len);
    }

    private void padding(StringBuilder target, final int indent) {
        for (int i = 0; i < (indent * INDENT_SPACES); i++) {
            target.append(' ');
        }
    }

    @Override
    public Appendable append(final CharSequence csq, final int start, final int end) {
        return sb.append(replaceNewLine(csq, start, end), start, end);
    }

    @Override
    public Appendable append(final char c) {
        return sb.append(c);
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
}
