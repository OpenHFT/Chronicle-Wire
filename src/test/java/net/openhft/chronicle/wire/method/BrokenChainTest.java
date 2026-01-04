/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrokenChainTest extends WireTestCommon {
    @Test
    @DisplayName("Broken chain is ignored on YAML_ONLY wire")
    void brokenChainYaml() {
        assertTrue(doBrokenChain(WireType.YAML_ONLY), "broken chain: wireType=YAML_ONLY");
    }

    @Test
    @DisplayName("Broken chain is ignored on TEXT wire")
    void brokenChainText() {
        assertTrue(doBrokenChain(WireType.TEXT), "broken chain: wireType=TEXT");
    }

    @Test
    @DisplayName("Broken chain is ignored on BINARY_LIGHT wire")
    void brokenChainBinary() {
        assertTrue(doBrokenChain(WireType.BINARY_LIGHT), "broken chain: wireType=BINARY_LIGHT");
    }

    private boolean doBrokenChain(WireType wireType) {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);
        List<String> list = new ArrayList<>();
        First first = pre -> msg -> list.add("pre: " + pre + ", msg: " + msg);
        MethodReader reader = wire.methodReader(first);

        assertFalse(reader.readOne(), "Reader should not read an empty wire");
        wire.rollbackIfNotComplete();

        assertFalse(reader.readOne(), "Reader should still have no completed message");

        First writer = wire.methodWriter(First.class);
        assertTrue(wire.writingIsComplete(), "Writer should complete before chained call");
        Second second = writer.pre("pre");
        assertFalse(wire.writingIsComplete(), "Chained call should be incomplete until msg");
        second.msg("msg");
        assertTrue(wire.writingIsComplete(), "Initial chain should complete after msg");
        wire.rollbackIfNotComplete();

        assertTrue(reader.readOne(), "Reader should process completed chain");
        assertFalse(reader.readOne(), "Reader should have no additional messages after first chain");
        assertEquals("[pre: pre, msg: msg]", list.toString(), "List should capture completed chain");

        list.clear();
        writer.pre("bad-pre");
        assertFalse(wire.writingIsComplete(), "Rollback chain should be incomplete without msg");
        wire.rollbackIfNotComplete();
        assertTrue(wire.writingIsComplete(), "Rollback should restore complete state");
        assertFalse(reader.readOne(), "Reader should skip rolled back chain");
        assertEquals("[]", list.toString(), "List should remain empty after rollback");

        Second secondC = writer.pre("pre-C");
        assertFalse(wire.writingIsComplete(), "Second chain should be incomplete without msg");
        secondC.msg("msg-C");
        assertTrue(wire.writingIsComplete(), "Second chain should complete after msg");
        wire.rollbackIfNotComplete();

        assertTrue(reader.readOne(), "Reader should process second completed chain");
        assertFalse(reader.readOne(), "Reader should have no additional messages after second chain");
        assertEquals("[pre: pre-C, msg: msg-C]", list.toString(), "List should capture second completed chain");
        return true;
    }

    interface First {
        Second pre(String pre);
    }

    interface Second {
        void msg(String msg);
    }
}
