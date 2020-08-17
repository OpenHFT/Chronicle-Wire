package net.openhft.chronicle.wire.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * simple java source code formatter, will indent on a "{" and reduce the indent on a "}" all spaces before a "/n" are removed to enforce a consistent
 * format
 */
public class SourceCodeFormatter implements Appendable, CharSequence {

    private final int indentSpaces;
    private final AtomicInteger indent;
    private final StringBuilder response = new StringBuilder();
    private StringBuilder sb = new StringBuilder();

    public SourceCodeFormatter(int indentSpaces, AtomicInteger indent) {
        this.indentSpaces = indentSpaces;
        this.indent = indent;
    }

    public SourceCodeFormatter(int indentSpaces) {
        this.indentSpaces = indentSpaces;
        this.indent = new AtomicInteger(0);
    }

    public SourceCodeFormatter(int indentSpaces, int i) {
        this.indentSpaces = indentSpaces;
        this.indent = new AtomicInteger(i);
    }

    @NotNull
    public String toString() {
        return sb.toString();
    }

    @Override
    public SourceCodeFormatter append(final CharSequence csq) {
        sb.append(replaceNewLine(csq, 0, csq.length() - 1));
        return this;
    }

    private CharSequence replaceNewLine(final CharSequence csq, int start, int end) {
        response.setLength(0);
        boolean lastChargeWasNewLine = false;
        int lastNewlineIndex = 0;
        for (int i = start; i <= end; i++) {
            char c = csq.charAt(i);

            response.append(c);
            switch (c) {
                case '\n':
                    lastNewlineIndex = response.length();
                    lastChargeWasNewLine = true;
                    padding(response, indent.get());
                    break;
//                case '[':
                case '{':
                    indent.incrementAndGet();
                    break;
                case '}':
//                case ']':
                    indent.decrementAndGet();
                    if (lastNewlineIndex >= 0) {
                        response.setLength(lastNewlineIndex);
                        padding(response, indent.get());
                        response.append(c);
                    }
                    break;
                case ' ':
                    if (lastChargeWasNewLine) {
                        // ignore whitespace after newline
                        response.setLength(response.length() - 1);
                    }
                    break;
                default:
                    lastChargeWasNewLine = false;
                    break;
            }
        }

        return response;
    }

    public void setLength(int len) {
        sb.setLength(len);
    }

    private void padding(StringBuilder target, final int indent) {
        for (int i = 0; i < (indent * indentSpaces); i++) {
            target.append(' ');
        }
    }

    @Override
    public SourceCodeFormatter append(final CharSequence csq, final int start, final int end) {
        sb.append(replaceNewLine(csq, start, end), start, end);
        return this;
    }

    @Override
    public SourceCodeFormatter append(final char c) {
        sb.append(c);
        return this;
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
