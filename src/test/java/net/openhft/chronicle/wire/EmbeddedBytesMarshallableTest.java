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
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.internal.BytesFieldInfo;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class EmbeddedBytesMarshallableTest extends WireTestCommon {
    @Before
    public void checkArch() {
        assumeFalse(Jvm.isArm() || Jvm.isAzulZing());
    }

    @Test
    public void testClear() {
        ClassAliasPool.CLASS_ALIASES.addAlias(EBM.class);
        EBM e1 = new EBM();
        e1.a.append("a12345678");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        e1.writeMarshallable(bytes);

        EBM e2 = new EBM();
        e2.readMarshallable(bytes);
        e2.a.clear();
        e2.a.append("b0000000");

        Assert.assertEquals("b0000000", e2.a.toString());

        bytes.releaseLast();
    }

    @Test
    public void ebm() {
        ClassAliasPool.CLASS_ALIASES.addAlias(EBM.class);
        EBM e1 = new EBM();
        e1.number = Base85LongConverter.INSTANCE.parse("Hello!", 0, 5);
        e1.a.append("a12345678901234567890123456789");
        e1.b.append("a1234567890123456789abc");
        e1.c.append("a1234567890");
        final String expected = "!EBM {\n" +
                "  number: Hello,\n" +
                "  a: a12345678901234567890123456789,\n" +
                "  b: a1234567890123456789abc,\n" +
                "  c: a1234567890\n" +
                "}\n";
        assertEquals(expected, e1.toString());
        Bytes<?> bytes = new HexDumpBytes();
        e1.writeMarshallable(bytes);
        assertEquals("00 00 03 08 1e 61 31 32 33 34 35 36 37 38 39 30\n" +
                "31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36\n" +
                "37 38 39 00 17 61 31 32 33 34 35 36 37 38 39 30\n" +
                "31 32 33 34 35 36 37 38 39 61 62 63 c4 5f 74 4c\n" +
                "00 00 00 00 0b 61 31 32 33 34 35 36 37 38 39 30\n", bytes.toHexString());
        EBM e2 = new EBM();
        e2.readMarshallable(bytes);
        assertEquals(expected, e2.toString());
        bytes.releaseLast();
    }

    @Test
    public void schemaChanges() {
        ClassAliasPool.CLASS_ALIASES.addAlias(EBM1.class, EBM2.class, EBM3.class);
        EBM3 e3 = Marshallable.fromString("!EBM3 {\n" +
                "  l0: 80,\n" +
                "  l1: 81,\n" +
                "  l2: 82,\n" +
                "  i0: 40,\n" +
                "  i1: 41,\n" +
                "  i2: 42,\n" +
                "  s0: 20,\n" +
                "  s1: 21,\n" +
                "  s2: 22,\n" +
                "  b0: 10,\n" +
                "  b1: 11,\n" +
                "  b2: 12\n" +
                "}");
        Bytes<?> bytes = new HexDumpBytes();
        e3.writeMarshallable(bytes);
        assertEquals("03 83 03 03 50 00 00 00 00 00 00 00 51 00 00 00\n" +
                "00 00 00 00 52 00 00 00 00 00 00 00 28 00 00 00\n" +
                "29 00 00 00 2a 00 00 00 14 00 15 00 16 00 0a 0b\n" +
                "0c\n", bytes.toHexString());
        EBM2 e2 = new EBM2();
        e2.readMarshallable(bytes);
        assertEquals("!EBM2 {\n" +
                "  l0: 80,\n" +
                "  l1: 81,\n" +
                "  i0: 40,\n" +
                "  i1: 41,\n" +
                "  s0: 20,\n" +
                "  s1: 21,\n" +
                "  b0: 10,\n" +
                "  b1: 11\n" +
                "}\n", e2.toString());
        bytes.readPosition(0);
        EBM1 e1 = new EBM1();
        e1.readMarshallable(bytes);
        assertEquals("!EBM1 {\n" +
                "  l0: 80,\n" +
                "  i0: 40,\n" +
                "  s0: 20,\n" +
                "  b0: 10\n" +
                "}\n", e1.toString());

        bytes.clear();
        e1.writeMarshallable(bytes);
        assertEquals("01 81 01 01 50 00 00 00 00 00 00 00 28 00 00 00\n" +
                "14 00 0a\n", bytes.toHexString());
        e2.readMarshallable(bytes);
        assertEquals("!EBM2 {\n" +
                "  l0: 80,\n" +
                "  l1: 0,\n" +
                "  i0: 40,\n" +
                "  i1: 0,\n" +
                "  s0: 20,\n" +
                "  s1: 0,\n" +
                "  b0: 10,\n" +
                "  b1: 0\n" +
                "}\n", e2.toString());
        bytes.readPosition(0);
        e3.readMarshallable(bytes);
        assertEquals("!EBM3 {\n" +
                "  l0: 80,\n" +
                "  l1: 0,\n" +
                "  l2: 0,\n" +
                "  i0: 40,\n" +
                "  i1: 0,\n" +
                "  i2: 0,\n" +
                "  s0: 20,\n" +
                "  s1: 0,\n" +
                "  s2: 0,\n" +
                "  b0: 10,\n" +
                "  b1: 0,\n" +
                "  b2: 0\n" +
                "}\n", e3.toString());

        bytes.releaseLast();
    }

    @Test(expected = DecoratedBufferUnderflowException.class)
    public void noData() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidDescription() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.readLimit(64); // even bit count i.e. 0
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidDescription2() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.append("abcd"); // tries to read too much data.
        bytes.readLimit(64);
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidDescription3() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.append("abce"); // even bit count &&  tries to read too much data.
        bytes.readLimit(64);
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidDescription4() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.append("3\0\0\0"); // even bit count
        bytes.readLimit(64);
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    static class EBM extends SelfDescribingTriviallyCopyable {
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM.class).description();
        static final int LENGTH, START;

        static {
            final int[] range = BytesUtil.triviallyCopyableRange(EBM.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        @FieldGroup("a")
        transient long a0, a1, a2, a3;
        @FieldGroup("b")
        transient long b0, b1, b2;
        @FieldGroup("c")
        transient int c0, c1, c3;
        @LongConversion(Base85LongConverter.class)
        long number;
        Bytes<?> a = Bytes.forFieldGroup(this, "a");
        Bytes<?> b = Bytes.forFieldGroup(this, "b");
        Bytes<?> c = Bytes.forFieldGroup(this, "c");

        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        @Override
        protected int $start() {
            return START;
        }

        @Override
        protected int $length() {
            return LENGTH;
        }
    }

    static class EBM1 extends SelfDescribingTriviallyCopyable {
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM1.class).description();
        static final int LENGTH, START;

        static {
            final int[] range = BytesUtil.triviallyCopyableRange(EBM1.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        long l0;
        int i0;
        short s0;
        byte b0;

        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        @Override
        protected int $start() {
            return START;
        }

        @Override
        protected int $length() {
            return LENGTH;
        }
    }

    static class EBM2 extends SelfDescribingTriviallyCopyable {
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM2.class).description();
        static final int LENGTH, START;

        static {
            final int[] range = BytesUtil.triviallyCopyableRange(EBM2.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        long l0, l1;
        int i0, i1;
        short s0, s1;
        byte b0, b1;

        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        @Override
        protected int $start() {
            return START;
        }

        @Override
        protected int $length() {
            return LENGTH;
        }
    }

    static class EBM3 extends SelfDescribingTriviallyCopyable {
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM3.class).description();
        static final int LENGTH, START;

        static {
            final int[] range = BytesUtil.triviallyCopyableRange(EBM3.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        long l0, l1, l2;
        int i0, i1, i2;
        short s0, s1, s2;
        byte b0, b1, b2;

        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        @Override
        protected int $start() {
            return START;
        }

        @Override
        protected int $length() {
            return LENGTH;
        }
    }
}
