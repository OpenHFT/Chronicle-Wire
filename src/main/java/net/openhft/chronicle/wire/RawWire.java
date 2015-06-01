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
import net.openhft.chronicle.wire.util.BooleanConsumer;
import net.openhft.chronicle.wire.util.ByteConsumer;
import net.openhft.chronicle.wire.util.FloatConsumer;
import net.openhft.chronicle.wire.util.ShortConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

/**
 * Created by peter.lawrey on 19/01/15.
 */
public class RawWire implements Wire, InternalWireIn {
    final Bytes bytes;
    final RawValueOut valueOut = new RawValueOut();
    final RawValueIn valueIn = new RawValueIn();
    String lastField = "";
    StringBuilder lastSB;
    boolean ready;

    public RawWire(Bytes bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        if (wire instanceof RawWire) {
            wire.bytes().write(bytes);

        } else {
            throw new UnsupportedOperationException("Can only copy Raw Wire format to the same format.");
        }
    }

    @Override
    public ValueIn read() {
        lastSB = null;
        return valueIn;
    }

    @Override
    public ValueIn read(@NotNull WireKey key) {
        lastSB = null;
        return valueIn;
    }

    @Override
    public ValueIn readEventName(@NotNull StringBuilder name) {
        bytes.readUTFΔ(name);
        lastSB = null;
        return valueIn;
    }

    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        lastSB = name;
        return valueIn;
    }

    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    @Override
    public Wire readComment(@NotNull StringBuilder sb) {
        return RawWire.this;
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
    public String toString() {
        return bytes.toString();
    }

    @Override
    public ValueOut write() {
        lastField = "";
        return valueOut;
    }

    @Override
    public ValueOut writeEventName(WireKey key) {
        lastField = "";
        bytes.writeUTFΔ(key.name());
        return valueOut;
    }

    @Override
    public ValueOut write(WireKey key) {
        lastField = key.name().toString();
        return valueOut;
    }

    @Override
    public ValueOut writeValue() {
        lastField = "";
        return valueOut;
    }

    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @Override
    public Wire writeComment(CharSequence s) {
        return RawWire.this;
    }

    @Override
    public WireOut addPadding(int paddingToAdd) {
        throw new UnsupportedOperationException();
    }

    class RawValueOut implements ValueOut {
        @Override
        public ValueOut leaf() {
            return this;
        }

        @Override
        public Wire bool(Boolean flag) {
            if (flag == null)
                bytes.writeUnsignedByte(BinaryWireCode.NULL);
            else
                bytes.writeUnsignedByte(flag ? BinaryWireCode.TRUE : 0);
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
        public WireOut rawBytes(byte[] value) {
            throw new UnsupportedOperationException("todo");
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
        public Wire int64(long i64) {
            bytes.writeLong(i64);
            return RawWire.this;
        }

        @Override
        public WireOut int64array(long capacity) {
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
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
        public WireOut typeLiteral(@NotNull CharSequence type) {
            return type(type);
        }

        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @NotNull Class type) {
            long position = bytes.position();
            bytes.skip(1);
            typeTranslator.accept(type, bytes);
            bytes.writeUnsignedByte(position, Maths.toInt8(bytes.position() - position - 1));
            return RawWire.this;
        }

        @Override
        public WireOut uuid(UUID uuid) {
            bytes.writeLong(uuid.getMostSignificantBits());
            bytes.writeLong(uuid.getLeastSignificantBits());
            return RawWire.this;
        }

        @Override
        public WireOut int32forBinding(int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut int64forBinding(long value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut sequence(Consumer<ValueOut> writer) {
            text(lastField);
            long position = bytes.position();
            bytes.writeInt(0);

                writer.accept(this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.position() - position - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        @Override
        public WireOut marshallable(WriteMarshallable object) {
            text(lastField);
            long position = bytes.position();
            bytes.writeInt(0);

                object.writeMarshallable(RawWire.this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.position() - position - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        @Override
        public WireOut map(Map map) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map)  {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireOut object(Object o) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireOut wireOut() {
            return RawWire.this;
        }
    }

    class RawValueIn implements ValueIn {

        @NotNull
        @Override
        public Wire bool(@NotNull BooleanConsumer flag) {
            int b = bytes.readUnsignedByte();
            if (b == BinaryWireCode.NULL)
                flag.accept(null);
            else if (b == 0 || b == BinaryWireCode.FALSE)
                flag.accept(false);
            else
                flag.accept(true);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn text(@NotNull Consumer<String> s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String text() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public <ACS extends Appendable & CharSequence> ACS textTo(@NotNull ACS s) {
            return bytes.readUTFΔ(s) ? s : null;
        }

        @NotNull
        @Override
        public Wire int8(@NotNull ByteConsumer i) {
            i.accept(bytes.readByte());
            return RawWire.this;
        }

        @NotNull
        public WireIn bytes(@NotNull Bytes toBytes) {
            wireIn().bytes().withLength(readLength(), toBytes::write);
            return wireIn();
        }

        @NotNull
        public WireIn bytes(@NotNull Consumer<WireIn> bytesConsumer) {
            long length = readLength();

            if (length > bytes.remaining())
                throw new BufferUnderflowException();
            long limit0 = bytes.limit();
            long limit = bytes.position() + length;
            try {
                bytes.limit(limit);
                bytesConsumer.accept(wireIn());
            } finally {
                bytes.limit(limit0);
                bytes.position(limit);
            }
            return wireIn();
        }

        @Override
        public byte[] bytes() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return RawWire.this;
        }

        @Override
        public long readLength() {
            return bytes.readStopBit();
        }

        @NotNull
        @Override
        public Wire uint8(@NotNull ShortConsumer i) {
            i.accept((short) bytes.readUnsignedByte());
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire int16(@NotNull ShortConsumer i) {
            i.accept(bytes.readShort());
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire uint16(@NotNull IntConsumer i) {
            i.accept(bytes.readUnsignedShort());
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire int32(@NotNull IntConsumer i) {
            i.accept(bytes.readInt());
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire uint32(@NotNull LongConsumer i) {
            i.accept(bytes.readUnsignedInt());
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire int64(@NotNull LongConsumer i) {
            i.accept(bytes.readLong());
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire float32(@NotNull FloatConsumer v) {
            v.accept(bytes.readFloat());
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire float64(@NotNull DoubleConsumer v) {
            v.accept(bytes.readDouble());
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire time(@NotNull Consumer<LocalTime> localTime) {
            localTime.accept(LocalTime.ofNanoOfDay(bytes.readLong()));
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime) {
            zonedDateTime.accept(ZonedDateTime.parse(bytes.readUTFΔ()));
            return RawWire.this;
        }

        @NotNull
        @Override
        public Wire date(@NotNull Consumer<LocalDate> localDate) {
            localDate.accept(LocalDate.ofEpochDay(bytes.readStopBit()));
            return RawWire.this;
        }

        @Override
        public boolean hasNext() {
            return bytes.remaining() > 0;
        }

        @Override
        public boolean hasNextSequenceItem() {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireIn uuid(@NotNull Consumer<UUID> uuid) {
            uuid.accept(new UUID(bytes.readLong(), bytes.readLong()));
            return RawWire.this;
        }

        @Override
        public WireIn int64array(@Nullable LongArrayValues values, @NotNull Consumer<LongArrayValues> setter) {
            if (!(values instanceof Byteable)) {
                setter.accept(values = new BinaryLongArrayReference());
            }
            Byteable b = (Byteable) values;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return RawWire.this;
        }

        @Override
        public WireIn int64(LongValue value, @NotNull Consumer<LongValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                setter.accept(value = new BinaryLongReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return RawWire.this;
        }

        @Override
        public WireIn int32(IntValue value, @NotNull Consumer<IntValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                setter.accept(value = new IntBinaryReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return RawWire.this;
        }

        @Override
        public WireIn sequence(@NotNull Consumer<ValueIn> reader) {
            textTo(lastSB);

            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T applyToMarshallable(Function<WireIn, T> marshallableReader) {
            textTo(lastSB);

            long length = bytes.readUnsignedInt();
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.position() + length;
                bytes.limit(limit2);
                try {
                    return marshallableReader.apply(RawWire.this);
                } finally {
                    bytes.limit(limit);
                    bytes.position(limit2);
                }
            } else {
                return marshallableReader.apply(RawWire.this);
            }
        }

        @NotNull
        @Override
        public Wire type(@NotNull StringBuilder s) {
            bytes.readUTFΔ(s);
            return RawWire.this;
        }

        @Override
        public WireIn typeLiteral(@NotNull Consumer<CharSequence> classNameConsumer) {
            StringBuilder sb = Wires.acquireStringBuilder();
            type(sb);
            classNameConsumer.accept(sb);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) {
            textTo(lastSB);

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

        @Override
        public <K extends ReadMarshallable, V extends ReadMarshallable> void typedMap(@NotNull Map<K, V> usingMap) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public <K, V> Map<K, V> map(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap) {
            throw new UnsupportedOperationException("todo");
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
        public int uint16() {
            throw new UnsupportedOperationException("todo");
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

        @Nullable
        @Override
        public <E> E object(@Nullable E using, @NotNull Class<E> clazz) {
            throw new UnsupportedOperationException("todo");
        }
    }
}
