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

// Test class focusing on the serialization and deserialization of Marshallables with embedded bytes.
public class EmbeddedBytesMarshallableTest extends WireTestCommon {

    // Before each test, check the current architecture and skip if it's ARM or Azul Zing.
    @Before
    public void checkArch() {
        assumeFalse(Jvm.isArm() || Jvm.isAzulZing());
    }

    // Test clearing and appending new data to the embedded bytes.
    @Test
    public void testClear() {
        // Register the alias for the class.
        ClassAliasPool.CLASS_ALIASES.addAlias(EBM.class);

        // Initialize and set the value for embedded bytes.
        EBM e1 = new EBM();
        e1.a.append("a12345678");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        e1.writeMarshallable(bytes);

        // Deserialize the data, clear and append new values.
        EBM e2 = new EBM();
        e2.readMarshallable(bytes);
        e2.a.clear();
        e2.a.append("b0000000");

        // Ensure the deserialized and modified data is as expected.
        Assert.assertEquals("b0000000", e2.a.toString());

        // Release the bytes.
        bytes.releaseLast();
    }

    // Test serialization and deserialization with certain expected output.
    @Test
    public void ebm() {
        // Register the alias for the class.
        ClassAliasPool.CLASS_ALIASES.addAlias(EBM.class);

        // Initialize the embedded bytes object and set its values.
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
        assertEquals("00 80 04 08 00 80 04 08 1e 61 31 32 33 34 35 36\n" +
                "37 38 39 30 31 32 33 34 35 36 37 38 39 30 31 32\n" +
                "33 34 35 36 37 38 39 00 17 61 31 32 33 34 35 36\n" +
                "37 38 39 30 31 32 33 34 35 36 37 38 39 61 62 63\n" +
                "c4 5f 74 4c 00 00 00 00 0b 61 31 32 33 34 35 36\n" +
                "37 38 39 30\n", bytes.toHexString());
        EBM e2 = new EBM();
        e2.readMarshallable(bytes);
        assertEquals(expected, e2.toString());
        bytes.releaseLast();
    }

    // Test deserialization with no data. Expected to throw a DecoratedBufferUnderflowException.
    @Test(expected = DecoratedBufferUnderflowException.class)
    public void noData() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    // Test deserialization with invalid description (even bit count = 0).
    // Expected to throw an IllegalStateException.
    @Test(expected = IllegalStateException.class)
    public void invalidDescription() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.readLimit(64); // even bit count i.e. 0
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    // Test deserialization with another invalid description scenario where it tries to read more data than available.
    // Expected to throw an IllegalStateException.
    @Test(expected = IllegalStateException.class)
    public void invalidDescription2() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.append("abcd"); // tries to read too much data.
        bytes.readLimit(64);
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    // Test deserialization with yet another invalid description, characterized by both an even bit count
    // and an attempt to read more data than available. Expected to throw an IllegalStateException.
    @Test(expected = IllegalStateException.class)
    public void invalidDescription3() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.append("abce"); // even bit count &&  tries to read too much data.
        bytes.readLimit(64);
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    // Test deserialization with an even bit count description.
    // Expected to throw an IllegalStateException.
    @Test(expected = IllegalStateException.class)
    public void invalidDescription4() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        bytes.append("3\0\0\0"); // even bit count
        bytes.readLimit(64);
        EBM ebm = new EBM();
        ebm.readMarshallable(bytes);
    }

    // A class representing a Marshallable object with fields grouped into embedded Bytes.
    // This class extends SelfDescribingTriviallyCopyable, indicating that it can describe its own serialization format.
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

        // Required overrides for the SelfDescribingTriviallyCopyable class to describe its serialization format.
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

    // EBM1 class represents a marshallable object with fields of various data types.
    // It extends the SelfDescribingTriviallyCopyable class, indicating its self-descriptive serialization capability.
    static class EBM1 extends SelfDescribingTriviallyCopyable {

        // Retrieve and store the description of the EBM1 class from the BytesFieldInfo utility.
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM1.class).description();
        // Define LENGTH and START constants that describe the start position and length of serializable data.
        static final int LENGTH, START;

        static {
            // Calculate the range of trivially copyable bytes for the EBM1 class.
            final int[] range = BytesUtil.triviallyCopyableRange(EBM1.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        // Fields of the EBM1 class.
        long l0;
        int i0;
        short s0;
        byte b0;

        // Mandatory override that returns the stored description of the EBM1 class.
        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        // Mandatory override that returns the starting byte position for serialization.
        @Override
        protected int $start() {
            return START;
        }

        // Mandatory override that returns the length of the serializable data.
        @Override
        protected int $length() {
            return LENGTH;
        }
    }

    // EBM2 is similar to EBM1 but includes a second set of fields.
    static class EBM2 extends SelfDescribingTriviallyCopyable {

        // Retrieve and store the description of the EBM2 class from the BytesFieldInfo utility.
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM2.class).description();
        // Define LENGTH and START constants that describe the start position and length of serializable data.
        static final int LENGTH, START;

        static {
            // Calculate the range of trivially copyable bytes for the EBM2 class.
            final int[] range = BytesUtil.triviallyCopyableRange(EBM2.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        // Additional fields for the EBM2 class.
        long l0, l1;
        int i0, i1;
        short s0, s1;
        byte b0, b1;

        // Mandatory override that returns the stored description of the EBM2 class.
        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        // Mandatory override that returns the starting byte position for serialization.
        @Override
        protected int $start() {
            return START;
        }

        // Mandatory override that returns the length of the serializable data.
        @Override
        protected int $length() {
            return LENGTH;
        }
    }

    // EBM3 builds upon EBM2 by adding a third set of fields.
    static class EBM3 extends SelfDescribingTriviallyCopyable {

        // Retrieve and store the description of the EBM3 class from the BytesFieldInfo utility.
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM3.class).description();
        // Define LENGTH and START constants that describe the start position and length of serializable data.
        static final int LENGTH, START;

        static {
            // Calculate the range of trivially copyable bytes for the EBM3 class.
            final int[] range = BytesUtil.triviallyCopyableRange(EBM3.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        // Additional fields for the EBM3 class.
        long l0, l1, l2;
        int i0, i1, i2;
        short s0, s1, s2;
        byte b0, b1, b2;

        // Mandatory override that returns the stored description of the EBM3 class.
        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        // Mandatory override that returns the starting byte position for serialization.
        @Override
        protected int $start() {
            return START;
        }

        // Mandatory override that returns the length of the serializable data.
        @Override
        protected int $length() {
            return LENGTH;
        }
    }
}
