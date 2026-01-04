/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JavaSourceCodeFormatterTest extends WireTestCommon {
    // Test method for JavaSourceCodeFormatter's append functionality
    @Test
    @DisplayName("Formats Java source with expected indentation")
    void testAppend() {
        // Creating an expected string of Java source code and asserting that the formatted code matches the expected string
        Assertions.assertEquals("public Appendable append(final CharSequence csq) {\n" +
                        "    return sb.append(replaceNewLine(csq, 0, csq.length() - 1));\n" +
                        "}\n",
                new JavaSourceCodeFormatter()
                        .append("public Appendable append(final CharSequence csq) {\n")
                        .append("return sb.append(replaceNewLine(csq, 0, csq.length() - 1));\n")
                        .append('}').append('\n')
                        .toString(),
                "Formatter should indent the method body");
    }
}
