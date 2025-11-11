/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Light checks for string interning via WireInternal and simple dump content.
 */
public class WireInternerAndDumpTest extends WireTestCommon {

    @Test
    public void textWireInternsRepeatedStrings() {
        TextWire w = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("k").text("alpha");
        }
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("k").text("alpha");
        }
        String s1, s2;
        try (DocumentContext dc = w.readingDocument()) {
            s1 = dc.wire().read("k").text();
        }
        try (DocumentContext dc = w.readingDocument()) {
            s2 = dc.wire().read("k").text();
        }
        // Same canonical instance expected due to interning
        assertSame(s1, s2);
        // Do not assert on global interner counts as other tests may have already
        // populated the interner; asserting referential equality is sufficient.
    }

    @Test
    public void binaryWireHexDumpContainsKeys() {
        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(128));
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("foo").int32(42);
        }
        String hex = w.bytes().toHexString();
        assertTrue(hex.contains("foo")); // dumped as ASCII alongside hex
    }
}
