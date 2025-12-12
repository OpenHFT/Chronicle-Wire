/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A formatter specifically tailored for formatting Java source code.
 * <p>
 * By default, the formatter uses an indentation of 4 spaces for Java code,
 * in line with common Java coding conventions. However, this can be customized
 * if needed.
 *
 * @see SourceCodeFormatter
 */
public class JavaSourceCodeFormatter extends SourceCodeFormatter {

    // Default number of spaces for indentation in Java source code
    private static final int INDENT_SPACES = 4;

    /**
     * Constructs a new Java source code formatter with the default indentation level.
     */
    public JavaSourceCodeFormatter() {
        super(INDENT_SPACES);
    }

    /**
     * Constructs a new Java source code formatter with a specified initial indentation level.
     *
     * @param indent Initial indentation level.
     */
    @Deprecated(/* to be removed in 2027 */)
    public JavaSourceCodeFormatter(int indent) {
        super(INDENT_SPACES, indent);
    }

    /**
     * Constructs a new Java source code formatter with a provided AtomicInteger to manage the indentation level.
     * <p>
     * This can be useful in scenarios where the indentation needs to be managed or adjusted externally.
     *
     * @param indent AtomicInteger managing the indentation level.
     */
    public JavaSourceCodeFormatter(AtomicInteger indent) {
        super(INDENT_SPACES, indent);
    }

    @Override
    public SourceCodeFormatter append(long i) {
        super.append(i);
        if ((int) i != i)
            append('L');
        return this;
    }
}
