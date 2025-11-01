/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Assert;
import org.junit.Test;

public class JavaSourceCodeFormatterTest extends WireTestCommon {
    // Test method for JavaSourceCodeFormatter's append functionality
    @Test
    public void testAppend() {
        // Creating an expected string of Java source code and asserting that the formatted code matches the expected string
        Assert.assertEquals("" +
                        "public Appendable append(final CharSequence csq) {\n" +
                        "    return sb.append(replaceNewLine(csq, 0, csq.length() - 1));\n" +
                        "}\n",
                new JavaSourceCodeFormatter()
                        .append("public Appendable append(final CharSequence csq) {\n")
                        .append("return sb.append(replaceNewLine(csq, 0, csq.length() - 1));\n")
                        .append('}').append('\n')
                        .toString());
    }
}
