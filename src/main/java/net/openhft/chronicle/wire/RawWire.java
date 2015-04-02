/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Created by peter.lawrey on 19/01/15.
 */
public class RawWire implements Wire {
    final Bytes bytes;
    final RawValueOut writeValue = new RawValueOut();
    final RawValueIn readValue = new RawValueIn();
    String lastField = "";
    StringBuilder lastSB;

    public RawWire(Bytes bytes) {
        this.bytes = bytes;
    }

    @Override
    public void copyTo(WireOut wire) {
        if (wire instanceof RawWire) {
            wire.bytes().write(bytes);
        } else {
            throw new UnsupportedOperationException("Can only copy Raw Wire format to the same format.");
        }
    }

    @Override
    public String toString() {
        return bytes.toString();
    }

    @Override
    public ValueOut write() {
        lastField = "";
        return writeValue;
    }

    @Override
    public ValueOut write(WireKey key) {
        lastField = key.name().toString();
        return writeValue;
    }

    @Override
    public ValueOut writeEventName(WireKey key) {
        lastField = "";
        bytes.writeUTFΔ(key.name());
        return writeValue;
    }

    @Override
    public ValueOut writeValue() {
        lastField = "";
        return writeValue;
    }

    @Override
    public ValueIn read() {
        lastSB = null;
        return readValue;
    }

    @Override
    public ValueIn read(WireKey key) {
        lastSB = null;
        return readValue;
    }

    @Override
    public ValueIn read(StringBuilder name) {
        lastSB = name;
        return readValue;
    }

    @Override
    public ValueIn readEventName(StringBuilder name) {
        bytes.readUTFΔ(name);
        lastSB = null;
        return readValue;
    }

    @Override
    public boolean hasNextSequenceItem() {
        return false;
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
    public boolean hasDocument() {
        return false;
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

    @Override
    public WireOut addPadding(int paddingToAdd) {
        throw new UnsupportedOperationException();
    }

    class RawValueOut implements ValueOut {
        boolean nested = false;

        @Override
        public boolean isNested() {
            return nested;
        }

        @Override
        public WireOut nested(boolean nested) {
            this.nested = nested;
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
        public Wire int8(byte i8) {
            bytes.writeByte(i8);
            return RawWire.this;
        }

        @Override
        public WireOut bytes(Bytes fromBytes) {
            writeLength(fromBytes.remaining());
            bytes.write(fromBytes);
            return RawWire.this;
        }

        @Override
        public ValueOut writeLength(long length) {
            bytes.writeStopBit(length);
            return this;
        }

        @Override
        public WireOut bytes(byte[] fromBytes) {
            writeLength(fromBytes.length);
            bytes.write(fromBytes);
            return RawWire.this;
        }

        @Override
        public Wire uint8checked(int u8) {
            bytes.writeUnsignedByte(u8);
            return RawWire.this;
        }

        @Override
        public Wire int16(short i16) {
            bytes.writeShort(i16);
            return RawWire.this;
        }

        @Override
        public Wire uint16checked(int u16) {
            bytes.writeUnsignedShort(u16);
            return RawWire.this;
        }

        @Override
        public Wire utf8(int codepoint) {
            BytesUtil.appendUTF(bytes, codepoint);
            return RawWire.this;
        }

        @Override
        public Wire int32(int i32) {
            bytes.writeInt(i32);
            return RawWire.this;
        }

        @Override
        public Wire uint32checked(long u32) {
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
        public WireOut int64array(long capacity) {
            LongArrayDirectReference.lazyWrite(bytes, capacity);
            return RawWire.this;
        }

        @Override
        public Wire time(LocalTime localTime) {
            long t = localTime.toNanoOfDay();
            bytes.writeLong(t);
            return RawWire.this;
        }

        @Override
        public Wire zonedDateTime(ZonedDateTime zonedDateTime) {
            bytes.writeUTFΔ(zonedDateTime.toString());
            return RawWire.this;
        }

        @Override
        public Wire date(LocalDate localDate) {
            bytes.writeStopBit(localDate.toEpochDay());
            return RawWire.this;
        }

        @Override
        public Wire type(CharSequence typeName) {
            bytes.writeUTFΔ(typeName);
            return RawWire.this;
        }

        @Override
        public WireOut uuid(UUID uuid) {
            bytes.writeLong(uuid.getMostSignificantBits());
            bytes.writeLong(uuid.getLeastSignificantBits());
            return RawWire.this;
        }

        @Override
        public WireOut int64forBinding(long value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut int32forBinding(int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut sequence(Consumer<ValueOut> writer) {
            text(lastField);
            long position = bytes.position();
            bytes.writeInt(0);
            boolean nested = isNested();
            try {
                nested(true);
                writer.accept(this);
            } finally {
                nested(nested);
            }

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.position() - position - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        @Override
        public WireOut marshallable(WriteMarshallable object) {
            text(lastField);
            long position = bytes.position();
            bytes.writeInt(0);
            boolean nested = isNested();
            try {
                nested(true);
                object.writeMarshallable(RawWire.this);
            } finally {
                nested(nested);
            }

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.position() - position - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }
    }

    class RawValueIn implements ValueIn {

        public WireIn bytes(Bytes toBytes) {
            wireIn().bytes().withLength(readLength(), toBytes::write);
            return wireIn();
        }

        public WireIn bytes(Consumer<byte[]> bytesConsumer) {
            long length = readLength();
            byte[] byteArray = new byte[Maths.toInt32(length)];
            bytes.read(byteArray);
            bytesConsumer.accept(byteArray);
            return wireIn();
        }

        @Override
        public byte[] bytes() {
            throw new UnsupportedOperationException("todo");
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
        public WireIn text(Consumer<String> s) {
            throw new UnsupportedOperationException();
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
        public WireIn wireIn() {
            return RawWire.this;
        }

        @Override
        public long readLength() {
            return bytes.readStopBit();
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
        public boolean bool() {
            return bytes.readBoolean();
        }

        @Override
        public byte int8() {
            return bytes.readByte();
        }

        @Override
        public short int16() {
            return bytes.readShort();
        }

        @Override
        public int int32() {
            return bytes.readInt();
        }

        @Override
        public long int64() {
            return bytes.readLong();
        }

        @Override
        public double float64() {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public float float32() {
            throw new UnsupportedOperationException("todo");
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
        public Wire time(Consumer<LocalTime> localTime) {
            localTime.accept(LocalTime.ofNanoOfDay(bytes.readLong()));
            return RawWire.this;
        }

        @Override
        public Wire zonedDateTime(Consumer<ZonedDateTime> zonedDateTime) {
            zonedDateTime.accept(ZonedDateTime.parse(bytes.readUTFΔ()));
            return RawWire.this;
        }

        @Override
        public Wire date(Consumer<LocalDate> localDate) {
            localDate.accept(LocalDate.ofEpochDay(bytes.readStopBit()));
            return RawWire.this;
        }

        @Override
        public boolean hasNext() {
            return bytes.remaining() > 0;
        }

        @Override
        public WireIn expectText(CharSequence s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn uuid(Consumer<UUID> uuid) {
            uuid.accept(new UUID(bytes.readLong(), bytes.readLong()));
            return RawWire.this;
        }

        @Override
        public WireIn int64(LongValue value, Consumer<LongValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                setter.accept(value = new LongDirectReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return RawWire.this;
        }

        @Override
        public WireIn int64array(LongArrayValues values, Consumer<LongArrayValues> setter) {
            if (!(values instanceof Byteable)) {
                setter.accept(values = new LongArrayDirectReference());
            }
            Byteable b = (Byteable) values;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return RawWire.this;
        }

        @Override
        public WireIn int32(IntValue value, Consumer<IntValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                setter.accept(value = new IntDirectReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return RawWire.this;
        }

        @Override
        public WireIn sequence(Consumer<ValueIn> reader) {
            text(lastSB);

            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn marshallable(ReadMarshallable object) {
            text(lastSB);

            long length = bytes.readUnsignedInt();
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.position() + length;
                bytes.limit(limit2);
                try {
                    object.readMarshallable(RawWire.this);
                } finally {
                    bytes.limit(limit);
                    bytes.position(limit2);
                }
            } else {
                object.readMarshallable(RawWire.this);
            }
            return RawWire.this;
        }


    }
}
