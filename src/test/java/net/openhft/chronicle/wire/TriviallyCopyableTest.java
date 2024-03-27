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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.junit.Test;

import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;

// Test class for validating the Trivially Copyable features with Chronicle Wire
public class TriviallyCopyableTest extends WireTestCommon {

    // Test utility to perform read and write operations on byte buffers
    // with AA instances, and then check if the original and the result are equal
    static void doTest(BiConsumer<Bytes<?>, AA> read, BiConsumer<Bytes<?>, AA> write) {
        // Allocate direct memory for bytes
        Bytes<?> bytes = Bytes.allocateDirect(40);

        // Initialize an AA instance
        AA aa = new AA((byte) 1, (byte) 2, true, false, 'Y', (short) 6, 7, 8, 9, 10);

        // Write the AA instance to bytes
        write.accept(bytes, aa);

        // Create a new instance of AA for reading
        AA a2 = ObjectUtils.newInstance(AA.class);

        // Read bytes into the new AA instance
        read.accept(bytes, a2);

        // Validate the original and the read AA instances are identical
        assertEquals(aa, a2);

        // Release the bytes memory
        bytes.releaseLast();
    }

    // Test case using the unsafe marshaller (only for non-Azul Zing JVMs)
    @Test
    public void unsafe2() {
        // Execute the test using AA's marshallers
        doTest((b, a) -> a.readMarshallable(b), (b, a) -> a.writeMarshallable(b));
    }

    // Inner class representing a binary-serializable data structure
    static class AA extends BytesInBinaryMarshallable {
        static final int FORMAT = 1;  // version format for serialization

        // natural order on a 64-bit JVM.
        int i;
        double d;
        long l;
        float f;
        short s;
        char ch;
        byte b1, b2;
        boolean flag, flag2;

        // Constructor to initialize all fields
        public AA(byte b1, byte b2, boolean flag, boolean flag2, char ch, short s, float f, int i, double d, long l) {
            this.b1 = b1;
            this.b2 = b2;
            this.flag = flag;
            this.flag2 = flag2;
            this.ch = ch;
            this.s = s;
            this.f = f;
            this.i = i;
            this.d = d;
            this.l = l;
        }

        // Read the serialized bytes into AA instance
        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException {
            int id = (int) bytes.readStopBit();
            switch (id) {
                case FORMAT:
                    if (OS.is64Bit())
                        // Perform direct memory read if 64-bit OS
                        bytes.unsafeReadObject(this, 32);
                    else
                        // Read individual fields if not 64-bit OS
                        readMarshallable1(bytes);
                    return;
                default:
                    throw new IORuntimeException("Unknown format " + id);
            }
        }

        // Read individual fields from the bytes
        void readMarshallable1(BytesIn<?> bytes) throws IORuntimeException {
            i = bytes.readInt();
            d = bytes.readDouble();
            l = bytes.readLong();
            f = bytes.readFloat();
            s = bytes.readShort();
            ch = bytes.readChar();
            b1 = bytes.readByte();
            b2 = bytes.readByte();
            flag = bytes.readBoolean();
            flag2 = bytes.readBoolean();
        }

        // Serialize the AA instance into bytes
        @Override
        public void writeMarshallable(BytesOut<?> bytes) {
            bytes.writeStopBit(FORMAT);
            if (OS.is64Bit())
                // Directly write the memory contents if 64-bit OS
                bytes.unsafeWriteObject(this, 32);
            else
                // Write individual fields if not 64-bit OS
                writeMarshallable1(bytes);
        }

        // Write individual fields into bytes
        void writeMarshallable1(BytesOut<?> bytes) {
            bytes.writeInt(i);
            bytes.writeDouble(d);
            bytes.writeLong(l);
            bytes.writeFloat(f);
            bytes.writeShort(s);
            bytes.writeChar(ch);
            bytes.writeByte(b1);
            bytes.writeByte(b2);
            bytes.writeBoolean(flag);
            bytes.writeBoolean(flag2);
        }
    }
}
