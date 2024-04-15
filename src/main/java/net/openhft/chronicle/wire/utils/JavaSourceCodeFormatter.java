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

    /** Default number of spaces for indentation in Java source code. */
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
