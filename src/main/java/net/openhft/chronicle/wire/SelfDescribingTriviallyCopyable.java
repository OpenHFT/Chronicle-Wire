package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.core.io.IORuntimeException;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

public abstract class SelfDescribingTriviallyCopyable extends SelfDescribingMarshallable {
    @FieldGroup("header")
    transient int description = $description();

    protected abstract int $description();

    protected abstract int $start();

    protected abstract int $length();

    @Override
    public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        int description0 = bytes.readInt();
        if (description0 != $description())
            carefulCopy(bytes, description0);
        else
            bytes.unsafeReadObject(this, $start(), $length());
    }

    private void carefulCopy(BytesIn<?> in, int description0) {
        int offset = $start();
        int longs0 = description0 >>> 24;
        int ints0 = (description0 >>> 16) & 0xFF;
        int shorts0 = (description0 >>> 8) & 0x7F;
        int bytes0 = description0 & 0xFF;
        int length = longs0 * 8 + ints0 * 4 + shorts0 * 2 + bytes0;
        if (Integer.bitCount(description0) % 2 == 0 || length > in.readRemaining())
            throw new IllegalStateException("Invalid description: " + Integer.toHexString(description0) + ", length: " + length + ", remaining: " + in.readRemaining());

        int longs = $description() >>> 24;// max 255
        for (int i = 0; i < Math.max(longs, longs0); i++) {
            long value = 0;
            if (i < longs0)
                value = in.readLong();
            if (i < longs) {
                MEMORY.writeLong(this, offset, value);
                offset += 8;
            }
        }
        int ints = ($description() >>> 16) & 0xFF;// max 255
        for (int i = 0; i < Math.max(ints, ints0); i++) {
            int value = 0;
            if (i < ints0)
                value = in.readInt();
            if (i < ints) {
                MEMORY.writeInt(this, offset, value);
                offset += 4;
            }
        }
        int shorts = ($description() >>> 8) & 0x7F; // max 127
        for (int i = 0; i < Math.max(shorts, shorts0); i++) {
            short value = 0;
            if (i < shorts0)
                value = in.readShort();
            if (i < shorts) {
                MEMORY.writeShort(this, offset, value);
                offset += 2;
            }
        }
        int bytes = $description() & 0xFF; // max 255
        for (int i = 0; i < Math.max(bytes, bytes0); i++) {
            byte value = 0;
            if (i < bytes0)
                value = in.readByte();
            if (i < bytes) {
                MEMORY.writeByte(this, offset, value);
                offset += 1;
            }
        }
    }

    @Override
    public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
        bytes.writeInt($description());
        bytes.unsafeWriteObject(this, $start(), $length());
    }
}
