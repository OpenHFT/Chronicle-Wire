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

public class JavaSourceCodeFormatter extends SourceCodeFormatter {
    private static final int INDENT_SPACES = 4;

    public JavaSourceCodeFormatter() {
        super(INDENT_SPACES);
    }

    public JavaSourceCodeFormatter(int indent) {
        super(INDENT_SPACES, indent);
    }

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
