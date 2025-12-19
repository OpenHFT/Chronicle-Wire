/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.PointerBytesStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers DefaultValueIn branches for bytes/text and primitive defaults.
 */
public class DefaultValueInAdditionalCasesTest extends WireTestCommon {

    @Test
    public void bytesTextAndMatchBranches() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        // bytes/text from non-null default
        Bytes<?> src = Bytes.wrapForRead("hello".getBytes());
        dvi.defaultValue = src;
        Bytes<?> out = Bytes.allocateElasticOnHeap(16);
        assertSame(out, dvi.textTo(out));
        assertEquals("hello", out.toString());

        final boolean[] match = {false};
        dvi.bytesMatch(src, b -> match[0] = b);
        assertTrue(match[0]);

        // bytesSet with null default (non-null requires direct memory address)
        PointerBytesStore pbs = new PointerBytesStore();
        dvi.defaultValue = null;
        assertSame(dvi.wireIn(), dvi.bytesSet(pbs));
    }

    @Test
    public void primitiveDefaultsAreZeroWhenNull() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = null;

        final int[] gotI = {1};
        dvi.int32(gotI, (arr, v) -> arr[0] = v);
        assertEquals(0, gotI[0]);

        final long[] gotL = {1};
        dvi.int64(gotL, (arr, v) -> arr[0] = v);
        assertEquals(0L, gotL[0]);

        final float[] gotF = {1f};
        dvi.float32(gotF, (arr, v) -> arr[0] = v);
        assertEquals(0f, gotF[0], 0f);
    }

    @Test
    public void bytesArrayAccessor() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        byte[] data = {1, 2, 3};
        dvi.defaultValue = data;
        byte[] using = new byte[3];
        assertArrayEquals(data, dvi.bytes(using));
    }
}
