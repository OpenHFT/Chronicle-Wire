/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.PointerBytesStore;
import org.junit.Test;

import static org.junit.Assert.*;

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
        dvi.defaultValue = (BytesStore<?, ?>) src;
        Bytes<?> out = Bytes.allocateElasticOnHeap(16);
        assertSame(out, dvi.text(out));
        assertEquals("hello", out.toString());

        final boolean[] match = {false};
        dvi.bytesMatch(src, b -> match[0] = b);
        assertTrue(match[0]);

        // bytesSet with null and non-null default
        PointerBytesStore pbs = new PointerBytesStore();
        dvi.defaultValue = null;
        assertSame(dvi.wireIn(), dvi.bytesSet(pbs));
        dvi.defaultValue = (BytesStore<?, ?>) src;
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
        byte[] data = new byte[]{1, 2, 3};
        dvi.defaultValue = data;
        byte[] using = new byte[3];
        assertArrayEquals(data, dvi.bytes(using));
    }
}

