/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinaryToTextTest extends WireTestCommon {

    // Test conversion of binary data to text representation
    @Test
    @DisplayName("Renders binary wire content as text")
    void test() {
        Bytes<?> tbytes = Bytes.allocateElasticOnHeap();
        @NotNull Wire tw = new BinaryWire(tbytes);
        tw.usePadding(true);
        tw.writeDocument(false, w -> w.write(() -> "key").text("hello"));
        assertEquals("--- !!data #binary\n" +
                        "key: hello\n",
                Wires.fromSizePrefixedBlobs(tw),
                "Text output should match binary wire document");
    }
}
