/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceCodeFormatterTest extends WireTestCommon {

    @Test
    @DisplayName("Closing brace does not rewrite content without a newline index")
    void closingBraceWithoutNewlineIndex() throws Exception {
        SourceCodeFormatter formatter = new SourceCodeFormatter(2);
        Field field = SourceCodeFormatter.class.getDeclaredField("lastNewlineIndex");
        field.setAccessible(true);
        field.setInt(formatter, -1);

        formatter.append('}');
        assertEquals("}", formatter.toString(),
                "Closing brace should remain when no newline index is set");
    }
}
