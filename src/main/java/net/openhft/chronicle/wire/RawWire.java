package net.openhft.chronicle.wire;

import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;
import net.openhft.lang.io.AbstractBytes;
import net.openhft.lang.io.Bytes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.function.*;

/**
 * Created by peter on 19/01/15.
 */
public class RawWire implements Wire {
    final AbstractBytes bytes;

    final RawWriteValue writeValue = new RawWriteValue();
    final RawReadValue readValue = new RawReadValue();

    public RawWire(Bytes bytes) {
        this.bytes = (AbstractBytes) bytes;
    }

    @Override
    public void copyTo(Wire wire) {
        if (wire instanceof RawWire) {
            wire.bytes().write(bytes);
        } else {
            throw new UnsupportedOperationException("Can only copy Raw Wire format to the same format.");
        }
    }

    @Override
    public WriteValue write() {
        return writeValue;
    }

    @Override
    public WriteValue write(WireKey key) {
        return writeValue;
    }

    @Override
    public WriteValue write(CharSequence name, WireKey template) {
        return writeValue;
    }

    @Override
    public WriteValue writeValue() {
        return writeValue;
    }

    @Override
    public ReadValue read() {
        return readValue;
    }

    @Override
    public ReadValue read(WireKey key) {
        return readValue;
    }

    @Override
    public ReadValue read(StringBuilder name, WireKey template) {
        return readValue;
    }

    @Override
    public boolean hasNextSequenceItem() {
        return false;
    }

    @Override
    public void readSequenceEnd() {

    }

    @Override
    public Wire writeComment(CharSequence s) {
        return RawWire.this;
    }

    @Override
    public Wire readComment(StringBuilder sb) {
        return RawWire.this;
    }

    @Override
    public boolean hasMapping() {
        return false;
    }

    @Override
    public Wire writeDocumentStart() {
        return RawWire.this;
    }

    @Override
    public void writeDocumentEnd() {

    }

    @Override
    public boolean hasDocument() {
        return false;
    }

    @Override
    public void consumeDocumentEnd() {

    }

    @Override
    public void flip() {
        bytes.flip();
    }

    @Override
    public void clear() {
        bytes.clear();
    }

    @Override
    public Bytes bytes() {
        return bytes;
    }

    class RawWriteValue implements WriteValue {


        @Override
        public WriteValue sequenceStart() {
            return this;
        }

        @Override
        public Wire sequenceEnd() {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire bool(Boolean flag) {
            if (flag == null)
                bytes.writeUnsignedByte(WireType.NULL.code);
            else
                bytes.writeUnsignedByte(flag ? WireType.TRUE.code : 0);
            return RawWire.this;
        }

        @Override
        public Wire text(CharSequence s) {
            bytes.writeUTFΔ(s);
            return RawWire.this;
        }

        @Override
        public Wire int8(int i8) {
            bytes.writeByte(i8);
            return RawWire.this;
        }

        @Override
        public Wire uint8(int u8) {
            bytes.writeUnsignedByte(u8);
            return RawWire.this;
        }

        @Override
        public Wire int16(int i16) {
            bytes.writeShort(i16);
            return RawWire.this;
        }

        @Override
        public Wire uint16(int u16) {
            bytes.writeUnsignedShort(u16);
            return RawWire.this;
        }

        @Override
        public Wire utf8(int codepoint) {
            StringBuilder sb = Wires.acquireStringBuilder();
            sb.appendCodePoint(codepoint);
            AbstractBytes.writeUTF0(bytes, sb, 1);
            return RawWire.this;
        }

        @Override
        public Wire int32(int i32) {
            bytes.writeInt(i32);
            return RawWire.this;
        }

        @Override
        public Wire uint32(long u32) {
            bytes.writeUnsignedInt(u32);
            return RawWire.this;
        }

        @Override
        public Wire float32(float f) {
            bytes.writeFloat(f);
            return RawWire.this;
        }

        @Override
        public Wire float64(double d) {
            bytes.writeDouble(d);
            return RawWire.this;
        }

        @Override
        public Wire int64(long i64) {
            bytes.writeLong(i64);
            return RawWire.this;
        }

        @Override
        public Wire hint(CharSequence hint) {
            return RawWire.this;
        }

        @Override
        public Wire mapStart() {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire mapEnd() {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire time(LocalTime localTime) {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire zonedDateTime(ZonedDateTime zonedDateTime) {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire date(LocalDate zonedDateTime) {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire object(Marshallable type) {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire type(CharSequence typeName) {
            return RawWire.this;
        }
    }

    class RawReadValue implements ReadValue {

        @Override
        public ReadValue sequenceStart() {
            if (true) throw new UnsupportedOperationException();
            return this;
        }

        @Override
        public Wire sequenceEnd() {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire bool(BooleanConsumer flag) {
            int b = bytes.readUnsignedByte();
            if (b == WireType.NULL.code)
                flag.accept(null);
            else if (b == 0 || b == WireType.FALSE.code)
                flag.accept(false);
            else
                flag.accept(true);
            return RawWire.this;
        }

        @Override
        public Wire text(StringBuilder s) {
            bytes.readUTFΔ(s);
            return RawWire.this;
        }

        @Override
        public Wire type(StringBuilder s) {
            bytes.readUTFΔ(s);
            return RawWire.this;
        }

        @Override
        public Wire int8(ByteConsumer i) {
            i.accept(bytes.readByte());
            return RawWire.this;
        }

        @Override
        public Wire uint8(ShortConsumer i) {
            i.accept((short) bytes.readUnsignedByte());
            return RawWire.this;
        }

        @Override
        public Wire int16(ShortConsumer i) {
            i.accept(bytes.readShort());
            return RawWire.this;
        }

        @Override
        public Wire uint16(IntConsumer i) {
            i.accept(bytes.readUnsignedShort());
            return RawWire.this;
        }

        @Override
        public Wire int32(IntConsumer i) {
            i.accept(bytes.readInt());
            return RawWire.this;
        }

        @Override
        public Wire uint32(LongConsumer i) {
            i.accept(bytes.readUnsignedInt());
            return RawWire.this;
        }

        @Override
        public Wire int64(LongConsumer i) {
            i.accept(bytes.readLong());
            return RawWire.this;
        }

        @Override
        public Wire float32(FloatConsumer v) {
            v.accept(bytes.readFloat());
            return RawWire.this;
        }

        @Override
        public Wire float64(DoubleConsumer v) {
            v.accept(bytes.readDouble());
            return RawWire.this;
        }

        @Override
        public Wire mapStart() {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire mapEnd() {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire time(Consumer<LocalTime> localTime) {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire zonedDateTime(Consumer<ZonedDateTime> zonedDateTime) {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire date(Consumer<LocalDate> zonedDateTime) {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public Wire object(Supplier<Marshallable> type) {
            if (true) throw new UnsupportedOperationException();
            return RawWire.this;
        }

        @Override
        public boolean hasNext() {
            return bytes.remaining() > 0;
        }
    }
}
