/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinaryToTextTest extends WireTestCommon {

    // Test conversion of binary data to text representation
    @Test
    public void test() {
        Bytes<?> tbytes = Bytes.allocateElasticOnHeap();
        @NotNull Wire tw = new BinaryWire(tbytes);
        tw.usePadding(true);
        tw.writeDocument(false, w -> w.write(() -> "key").text("hello"));
        assertEquals("--- !!data #binary\n" +
                        "key: hello\n",
                Wires.fromSizePrefixedBlobs(tw));
    }
}
