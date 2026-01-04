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
        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire().writeComment("one");
            dc.wire().writeEventId("dto", 1);
            dc.wire().writeComment("two");
            dc.wire().getValueOut().object(new BinaryWireTest.DTO("text"));
            dc.wire().writeComment("three");
            dc.wire().commentListener(cs ->
                    sb.append(cs).append('\n'));
        }
        MethodReader reader = wire.methodReader((BinaryWireTest.IDTO) dto -> sb.append("dto: ").append(dto).append('\n'));
        assertTrue(reader.readOne(), "First read should consume the queued dto");
        assertFalse(reader.readOne(), "Second read should find no remaining messages");
        return sb.toString();
    }
}
