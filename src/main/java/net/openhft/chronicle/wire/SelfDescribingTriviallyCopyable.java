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

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.core.io.IORuntimeException;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

/**
 * Self-describing marshallable that can be copied by simple memory operations.
 * The binary layout of the object is encoded by {@link #$description()} which
 * specifies how many primitive values of each type it contains.
 */
@SuppressWarnings("this-escape")
public abstract class SelfDescribingTriviallyCopyable extends SelfDescribingMarshallable {

    /**
     * Integer encoding of the field layout.  Initialised from {@link #$description()} and
     * written/read as part of the object header.
     */
    @FieldGroup("header")
    transient int description = $description();

    /**
     * @return integer encoding the number of longs, ints, shorts and bytes in
     * this object's layout.
     */
    protected abstract int $description();

    /** start offset of the trivially copyable region. */
    protected abstract int $start();

    /** total length in bytes of the trivially copyable region. */
    protected abstract int $length();

    @Override
    /**
     * Read the binary state of this object from {@code bytes}.  If the layout
     * description in the input matches {@link #$description()}, a fast unsafe
     * memory copy is used, otherwise {@link #carefulCopy(BytesIn, int)} handles
     * any schema differences field by field.
     */
    public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        int description0 = bytes.readInt();
        if (description0 != $description())
            carefulCopy(bytes, description0);
        else
            bytes.unsafeReadObject(this, $start(), $length());
    }

    /**
     * Copy fields from {@code in} into this object honouring both the source
     * description ({@code description0}) and this object's own
     * {@link #$description()} so that layout changes are tolerated.
     */
    private void carefulCopy(BytesIn<?> in, int description0) {
        // Start offset for copying data
        int offset = $start();

        // Extract the number of longs, ints, shorts, and bytes from the description0 using bitwise operations
        int longs0 = description0 >>> 24;
        int ints0 = (description0 >>> 16) & 0xFF;
        int shorts0 = (description0 >>> 8) & 0x7F;
        int bytes0 = description0 & 0xFF;

        // Calculate the total length required based on data types
        int length = longs0 * 8 + ints0 * 4 + shorts0 * 2 + bytes0;

        // Validation: Check if the description0 is even or if the length exceeds the remaining data in the input
        if (Integer.bitCount(description0) % 2 == 0 || length > in.readRemaining())
            throw new IllegalStateException("Invalid description: " + Integer.toHexString(description0) + ", length: " + length + ", remaining: " + in.readRemaining());

        // Copy long values from the input source to the object's memory
        int longs = $description() >>> 24; // max 255
        for (int i = 0; i < Math.max(longs, longs0); i++) {
            long value = 0;
            if (i < longs0)
                value = in.readLong();
            if (i < longs) {
                MEMORY.writeLong(this, offset, value);
                offset += 8; // Increment offset for next long value
            }
        }

        // Copy int values from the input source to the object's memory
        int ints = ($description() >>> 16) & 0xFF; // max 255
        for (int i = 0; i < Math.max(ints, ints0); i++) {
            int value = 0;
            if (i < ints0)
                value = in.readInt();
            if (i < ints) {
                MEMORY.writeInt(this, offset, value);
                offset += 4; // Increment offset for next int value
            }
        }

        // Copy short values from the input source to the object's memory
        int shorts = ($description() >>> 8) & 0x7F; // max 127
        for (int i = 0; i < Math.max(shorts, shorts0); i++) {
            short value = 0;
            if (i < shorts0)
                value = in.readShort();
            if (i < shorts) {
                MEMORY.writeShort(this, offset, value);
                offset += 2; // Increment offset for next short value
            }
        }

        // Copy byte values from the input source to the object's memory
        int bytes = $description() & 0xFF; // max 255
        for (int i = 0; i < Math.max(bytes, bytes0); i++) {
            byte value = 0;
            if (i < bytes0)
                value = in.readByte();
            if (i < bytes) {
                MEMORY.writeByte(this, offset, value);
                offset += 1; // Increment offset for next byte value
            }
        }
    }

    @Override
    /** Write out the layout description followed by the raw memory block. */
    public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
        bytes.writeInt($description());
        bytes.unsafeWriteObject(this, $start(), $length());
    }
}
