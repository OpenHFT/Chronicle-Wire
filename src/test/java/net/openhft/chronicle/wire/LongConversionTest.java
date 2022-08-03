/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class LongConversionTest extends WireTestCommon {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(LongHolder.class);
    }

    @Test
    public void dto() {
        LongHolder lh = new LongHolder();
        lh.hex = 0XFEDCBA9876543210L;
        lh.unsigned = Long.MIN_VALUE;
        lh.timestamp = 0x05432108090a0bL;
        assertEquals("!LongHolder {\n" +
                "  unsigned: 9223372036854775808,\n" +
                "  hex: fedcba9876543210,\n" +
                "  timestamp: 2016-12-08T08:00:31.345163\n" +
                "}\n", lh.toString());
        LongConversionTest.LongHolder lh2 = Marshallable.fromString(lh.toString());
        assertEquals(lh2, lh);
    }

    @Test
    public void method() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(64))
                .useTextDocuments();
        LongConversionTest.WriteWithLong write = wire.methodWriter(LongConversionTest.WriteWithLong.class);
        assertSame(write, write.to(0x12345));

        assertEquals("to: 12345\n", wire.toString());

        StringWriter sw = new StringWriter();
        LongConversionTest.WriteWithLong read = Mocker.logging(LongConversionTest.WriteWithLong.class, "", sw);
        wire.methodReader(read)
                .readOne();

        // NOTE: Mocker which is in Core, ignores the LongConverter
        assertEquals("to[74565]\n", sw.toString().replaceAll("\r", ""));
    }

    @Test
    public void oxmethod() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(64))
                .useTextDocuments();
        LongConversionTest.OxWriteWithLong write = wire.methodWriter(LongConversionTest.OxWriteWithLong.class);
        assertSame(write, write.to(0x12345));

        assertEquals("to: 0x12345\n", wire.toString());

        StringWriter sw = new StringWriter();
        LongConversionTest.OxWriteWithLong read = Mocker.logging(LongConversionTest.OxWriteWithLong.class, "", sw);
        wire.methodReader(read).readOne();

        // NOTE: Mocker which is in Core, ignores the LongConverter
        assertEquals("to[74565]\n", sw.toString().replaceAll("\r", ""));
    }

    interface WriteWithLong {
        LongConversionTest.WriteWithLong to(@LongConversion(HexadecimalLongConverter.class) int x);
    }

    interface OxWriteWithLong {
        LongConversionTest.OxWriteWithLong to(@LongConversion(OxHexadecimalLongConverter.class) int x);
    }

    static class LongHolder extends SelfDescribingMarshallable {
        @LongConversion(UnsignedLongConverter.class)
        long unsigned;
        @LongConversion(HexadecimalLongConverter.class)
        long hex;
        @LongConversion(MicroTimestampLongConverter.class)
        long timestamp;
    }
}
