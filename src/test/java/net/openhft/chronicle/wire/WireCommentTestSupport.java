/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.DocumentContext;
import org.jetbrains.annotations.NotNull;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
final class WireCommentTestSupport {
    private WireCommentTestSupport() {
    }

    static String exerciseReadComments(@NotNull Wire wire) {
        StringBuilder sb = new StringBuilder();
        try (DocumentContext ignored = wire.writingDocument()) {
            ignored.isData(); // touch resource to avoid unused warning
            wire.writeComment("one");
            wire.writeEventId("dto", 1);
            wire.writeComment("two");
            wire.getValueOut().object(new BinaryWireTest.DTO("text"));
            wire.writeComment("three");
            wire.commentListener(cs ->
                    sb.append(cs).append('\n'));
        }
        MethodReader reader = wire.methodReader((BinaryWireTest.IDTO) dto -> sb.append("dto: ").append(dto).append('\n'));
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        return sb.toString();
    }
}
