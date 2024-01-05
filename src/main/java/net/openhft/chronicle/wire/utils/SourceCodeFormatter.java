/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a simple Java source code formatter.
 * The formatter aims to enhance code readability by managing indentation for block-level code scopes.
 * It increases the indent on encountering a '{' and decreases on a '}'.
 * All trailing spaces before a newline character are removed to maintain a consistent format.
 */
public class SourceCodeFormatter implements Appendable, CharSequence {
    private final String indentSpaces;
    private final AtomicInteger indent;
    // StringBuilder to accumulate and store the formatted code.
    private final StringBuilder sb = new StringBuilder();
    private int lastNewlineIndex = 0;
    private boolean lastChargeWasNewLine = false;

    /**
     * Constructor to initialize the formatter with given indent spaces and an atomic integer value.
     *
     * @param indentSpaces Number of spaces to use for indentation.
     * @param indent Initial value for the atomic integer representing indentation count.
     */
    public SourceCodeFormatter(int indentSpaces, AtomicInteger indent) {
        this.indentSpaces = "        ".substring(0, indentSpaces);
        this.indent = indent;
    }

    /**
     * Constructor to initialize the formatter with given indent spaces and sets the default indentation count to 0.
     *
     * @param indentSpaces Number of spaces to use for indentation.
     */
    public SourceCodeFormatter(int indentSpaces) {
        this(indentSpaces, new AtomicInteger(0));
    }

    /**
     * Constructor to initialize the formatter with given indent spaces and a specific integer value.
     *
     * @param indentSpaces Number of spaces to use for indentation.
     * @param i Initial value for indentation count.
     */
    public SourceCodeFormatter(int indentSpaces, int i) {
        this(indentSpaces, new AtomicInteger(i));
    }

    /**
     * Returns the formatted string.
     *
     * @return The formatted code string.
     */
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

    /**
     * Sets the length of the current formatted string.
     * This can effectively truncate the existing content or extend it.
     *
     * @param len The new length for the formatted string.
     */
    public void setLength(int len) {
        sb.setLength(len);
    }

    /**
     * Appends padding (indentation) to the formatted string.
     * The number of indentations is determined by the provided indent value.
     *
     * @param indent The number of indentations to append.
     */
    private void padding(final int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(indentSpaces);
        }
    }

    /**
     * Retrieves the length of the current formatted string.
     *
     * @return The length of the formatted string.
     */
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

    /**
     * Appends the provided long value to the formatted string.
     *
     * @param i The long value to append.
     * @return The current SourceCodeFormatter instance.
     */
    public SourceCodeFormatter append(long i) {
        sb.append(i);
        return this;
    }

    /**
     * Appends the provided double value to the formatted string.
     *
     * @param d The double value to append.
     * @return The current SourceCodeFormatter instance.
     */
    public SourceCodeFormatter append(double d) {
        sb.append(d);
        return this;
    }

    /**
     * Appends the provided boolean value to the formatted string.
     *
     * @param flag The boolean value to append.
     * @return The current SourceCodeFormatter instance.
     */
    public SourceCodeFormatter append(boolean flag) {
        sb.append(flag);
        return this;
    }

    /**
     * Appends the provided object's string representation to the formatted string.
     * The object should have a meaningful string representation for this operation to be effective.
     *
     * @param <Stringable> The type of the object to be appended.
     * @param stringable The object whose string representation will be appended.
     * @return The current SourceCodeFormatter instance.
     */
    public <Stringable> SourceCodeFormatter append(Stringable stringable) {
        sb.append(stringable);
        return this;
    }

    /**
     * Checks if the formatted string contains the specified text.
     *
     * @param text The text to search for within the formatted string.
     * @return True if the text exists within the formatted string, otherwise false.
     */
    public boolean contains(String text) {
        return sb.indexOf(text) >= 0;
    }
}
