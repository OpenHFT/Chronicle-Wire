/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
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
    private final Bytes bytes;
    private final RawValueOut valueOut = new RawValueOut();
    private final RawValueIn valueIn = new RawValueIn();
    @NotNull
    private
    String lastField = "";
    @Nullable
    private
    StringBuilder lastSB;
    private boolean ready;

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

    @NotNull
    @Override
    public ValueIn read() {
        lastSB = null;
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        lastSB = null;
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn readEventName(@NotNull StringBuilder name) {
        bytes.readUTFΔ(name);
        lastSB = null;
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        lastSB = name;
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder sb) {
        return RawWire.this;
    }

    @Override
    public void clear() {
        bytes.clear();
    }

    @NotNull
    @Override
    public Bytes bytes() {
        return bytes;
    }

    @Override
    public boolean hasMore() {
        return bytes.readRemaining() > 0;
    }

    @NotNull
    @Override
    public WireIn readAlignTo(int alignment) {
        long mod = bytes.readPosition() % alignment;
        bytes.readSkip(mod);
        return this;
    }

    @Override
    public String toString() {
        return bytes.toString();
    }

    @NotNull
    @Override
    public ValueOut write() {
        lastField = "";
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut writeEventName(@NotNull WireKey key) {
        lastField = "";
        bytes.writeUTFΔ(key.name());
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull WireKey key) {
        lastField = key.name().toString();
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut writeValue() {
        lastField = "";
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @NotNull
    @Override
    public Wire writeComment(CharSequence s) {
        return RawWire.this;
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return new BinaryLongReference();
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        return new BinaryIntReference();
    }

    @NotNull
    @Override
    public BinaryLongArrayReference newLongArrayReference() {
        return new BinaryLongArrayReference();
    }

    class RawValueOut implements ValueOut {
        @NotNull
        @Override
        public ValueOut leaf() {
            return this;
        }

        @NotNull
        @Override
        public WireOut bool(@Nullable Boolean flag) {
            if (flag == null)
                bytes.writeUnsignedByte(BinaryWireCode.NULL);
            else
                bytes.writeUnsignedByte(flag ? BinaryWireCode.TRUE : 0);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            bytes.writeUTFΔ(s);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            bytes.writeByte(i8);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(@Nullable BytesStore fromBytes) {
            writeLength(fromBytes.readRemaining());
            bytes.write(fromBytes);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut rawBytes(byte[] value) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public ValueOut writeLength(long length) {
            bytes.writeStopBit(length);
            return this;
        }

        @NotNull
        @Override
        public WireOut bytes(@NotNull byte[] fromBytes) {
            writeLength(fromBytes.length);
            bytes.write(fromBytes);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            bytes.writeUnsignedByte(u8);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            bytes.writeShort(i16);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            bytes.writeUnsignedShort(u16);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            BytesUtil.appendUTF(bytes, codepoint);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            bytes.writeInt(i32);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            bytes.writeUnsignedInt(u32);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            bytes.writeLong(i64);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
            return RawWire.this;
        }


        @NotNull
        @Override
        public WireOut int64array(long capacity, @NotNull LongArrayValues values) {
            long pos = bytes.writePosition();
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
            ((ByteableLongArrayValues) values).bytesStore(bytes, pos, bytes.writePosition() - pos);
            return RawWire.this;
        }


        @NotNull
        @Override
        public WireOut float32(float f) {
            bytes.writeFloat(f);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
            bytes.writeDouble(d);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut time(@NotNull LocalTime localTime) {
            long t = localTime.toNanoOfDay();
            bytes.writeLong(t);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            bytes.writeUTFΔ(zonedDateTime.toString());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            bytes.writeStopBit(localDate.toEpochDay());
            return RawWire.this;
        }

        @NotNull
        @Override
        public ValueOut type(CharSequence typeName) {
            bytes.writeUTFΔ(typeName);
            return this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull CharSequence type) {
            bytes.writeUTFΔ(type);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @NotNull Class type) {
            long position = bytes.writePosition();
            bytes.writeSkip(1);
            typeTranslator.accept(type, bytes);
            bytes.writeUnsignedByte(position, Maths.toInt8(bytes.writePosition() - position - 1));
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut uuid(@NotNull UUID uuid) {
            bytes.writeLong(uuid.getMostSignificantBits());
            bytes.writeLong(uuid.getLeastSignificantBits());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            bytes.writeInt(value);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            bytes.writeLong(value);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value, @NotNull IntValue intValue) {
            int32forBinding(value);
            ((BinaryIntReference) intValue).bytesStore(bytes, bytes.writePosition() - 4, 4);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value, @NotNull LongValue longValue) {
            int64forBinding(value);
            ((BinaryLongReference) longValue).bytesStore(bytes, bytes.writePosition() - 8, 8);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut sequence(@NotNull Consumer<ValueOut> writer) {
            text(lastField);
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writer.accept(this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.writePosition() - position - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) {
            text(lastField);
            long position = bytes.writePosition();
            bytes.writeInt(0);

            object.writeMarshallable(RawWire.this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.writePosition() - position - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut map(Map map) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut object(@Nullable Object o) {
            bytes.writeUTFΔ(o == null ? null : o.toString());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut wireOut() {
            return RawWire.this;
        }
    }

    class RawValueIn implements ValueIn {

        @NotNull
        @Override
        public WireIn bool(@NotNull BooleanConsumer flag) {
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

        @NotNull
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
        public WireIn int8(@NotNull ByteConsumer i) {
            i.accept(bytes.readByte());
            return RawWire.this;
        }

        @NotNull
        public WireIn bytes(@NotNull Bytes toBytes) {
            toBytes.clear();

            long length = readLength();
            Bytes<?> bytes = wireIn().bytes();

            toBytes.write(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return wireIn();
        }

        @NotNull
        @Override
        public WireIn bytesMatch(@NotNull BytesStore compareBytes, @NotNull BooleanConsumer consumer) {
            long length = readLength();
            Bytes<?> bytes = wireIn().bytes();

            if (length == compareBytes.readRemaining()) {
                consumer.accept(bytes.equalBytes(compareBytes, length));
            } else {
                consumer.accept(false);
            }
            bytes.readSkip(length);
            return wireIn();

        }

        @NotNull
        public WireIn bytes(@NotNull Consumer<WireIn> bytesConsumer) {
            long length = readLength();

            if (length > bytes.readRemaining())
                throw new BufferUnderflowException();
            long limit0 = bytes.readLimit();
            long limit = bytes.readPosition() + length;
            try {
                bytes.readLimit(limit);
                bytesConsumer.accept(wireIn());
            } finally {
                bytes.readLimit(limit0);
                bytes.readPosition(limit);
            }
            return wireIn();
        }

        @NotNull
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
        public WireIn uint8(@NotNull ShortConsumer i) {
            i.accept((short) bytes.readUnsignedByte());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn int16(@NotNull ShortConsumer i) {
            i.accept(bytes.readShort());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn uint16(@NotNull IntConsumer i) {
            i.accept(bytes.readUnsignedShort());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntConsumer i) {
            i.accept(bytes.readInt());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn uint32(@NotNull LongConsumer i) {
            i.accept(bytes.readUnsignedInt());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongConsumer i) {
            i.accept(bytes.readLong());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn float32(@NotNull FloatConsumer v) {
            v.accept(bytes.readFloat());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn float64(@NotNull DoubleConsumer v) {
            v.accept(bytes.readDouble());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn time(@NotNull Consumer<LocalTime> localTime) {
            localTime.accept(LocalTime.ofNanoOfDay(bytes.readLong()));
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime) {
            zonedDateTime.accept(ZonedDateTime.parse(bytes.readUTFΔ()));
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn date(@NotNull Consumer<LocalDate> localDate) {
            localDate.accept(LocalDate.ofEpochDay(bytes.readStopBit()));
            return RawWire.this;
        }

        @Override
        public boolean hasNext() {
            return bytes.readRemaining() > 0;
        }

        @Override
        public boolean hasNextSequenceItem() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn uuid(@NotNull Consumer<UUID> uuid) {
            uuid.accept(new UUID(bytes.readLong(), bytes.readLong()));
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn int64array(@Nullable LongArrayValues values, @NotNull Consumer<LongArrayValues> setter) {
            if (!(values instanceof Byteable)) {
                setter.accept(values = new BinaryLongArrayReference());
            }
            Byteable b = (Byteable) values;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(LongValue value, @NotNull Consumer<LongValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                setter.accept(value = new BinaryLongReference());
            }
            return int64(value);
        }

        @NotNull
        @Override
        public WireIn int64(@Nullable LongValue value) {
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(IntValue value, @NotNull Consumer<IntValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                setter.accept(value = new BinaryIntReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn sequence(@NotNull Consumer<ValueIn> reader) {
            textTo(lastSB);

            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T applyToMarshallable(@NotNull Function<WireIn, T> marshallableReader) {
            textTo(lastSB);

            long length = bytes.readUnsignedInt();
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.readPosition() + length;
                bytes.readLimit(limit2);
                try {
                    return marshallableReader.apply(RawWire.this);
                } finally {
                    bytes.readLimit(limit);
                    bytes.readPosition(limit2);
                }
            } else {
                return marshallableReader.apply(RawWire.this);
            }
        }

        @Nullable
        @Override
        public <T extends ReadMarshallable> T typedMarshallable() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn type(@NotNull StringBuilder s) {
            bytes.readUTFΔ(s);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn typeLiteralAsText(@NotNull Consumer<CharSequence> classNameConsumer) {
            StringBuilder sb = Wires.acquireStringBuilder();
            type(sb);
            classNameConsumer.accept(sb);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) {
            long length = bytes.readUnsignedInt();
            if (length > bytes.readRemaining()) {
                throw new IllegalStateException("Length was " + length
                        + " greater than remaining " + bytes.readRemaining());
            }
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.readPosition() + length;
                bytes.readLimit(limit2);
                try {
                    object.readMarshallable(RawWire.this);
                } finally {
                    bytes.readLimit(limit);
                    bytes.readPosition(limit2);
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

        @NotNull
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

        @Nullable
        @Override
        public <E> WireIn object(@NotNull Class<E> clazz, Consumer<E> e) {
            throw new UnsupportedOperationException("todo");
        }
    }
}
