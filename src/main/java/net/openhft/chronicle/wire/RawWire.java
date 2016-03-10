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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.ref.BinaryIntReference;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.bytes.ref.BinaryLongReference;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
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
 * This format writes just the data, without meta data.
 */
public class RawWire extends AbstractWire implements Wire {
    private final RawValueOut valueOut = new RawValueOut();
    private final RawValueIn valueIn = new RawValueIn();
    private final WriteDocumentContext writeContext = new WriteDocumentContext(this);
    private final ReadDocumentContext readContext = new ReadDocumentContext(this);
    boolean use8bit;
    private ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES;
    @Nullable
    private
    StringBuilder lastSB;
    private boolean ready;

    public RawWire(Bytes bytes) {
        this(bytes, true);
    }

    public RawWire(Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
        this.classLookup = classLookup;
    }

    @Override
    public ClassLookup classLookup() {
        return classLookup;
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) {
        writeContext.start(metaData);
        return writeContext;
    }

    @Override
    public DocumentContext readingDocument() {
        readContext.start();
        return readContext;
    }


    @Override
    public DocumentContext readingDocument(long readLocation) {
        final long readPosition = bytes().readPosition();
        final long readLimit = bytes().readLimit();
        bytes().readPosition(readLocation);
        readContext.start();
        readContext.closeReadLimit(readLimit);
        readContext.closeReadPosition(readPosition);
        return readContext;
    }

    @Override
    public void consumePadding() {

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
        if (use8bit)
            bytes.read8bit(name);
        else
            bytes.readUtf8(name);
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
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut writeEventName(@NotNull WireKey key) {
        if (use8bit)
            bytes.write8bit(key.name());
        else
            bytes.writeUtf8(key.name());
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull WireKey key) {
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
            if (use8bit)
                bytes.write8bit(s);
            else
                bytes.writeUtf8(s);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable BytesStore s) {
            if (use8bit)
                bytes.write8bit(s);
            else
                bytes.writeUtf8(s);
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
        public WireOut bytes(@Nullable BytesStore bytesStore) {
            if (bytesStore == null) {
                writeLength(-1);
            } else {
                writeLength(bytesStore.readRemaining());
                bytes.write(bytesStore);
            }
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(String type, byte[] bytesArr) {
            typePrefix(type);
            return bytes(bytesArr);
        }

        @NotNull
        @Override
        public WireOut bytes(String type, @Nullable BytesStore fromBytes) {
            typePrefix(type);
            return bytes(fromBytes);
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
            bytes.appendUtf8(codepoint);
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
            ((Byteable) values).bytesStore(bytes, pos, bytes.writePosition() - pos);
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
            if (use8bit)
                bytes.write8bit(zonedDateTime.toString());
            else
                bytes.writeUtf8(zonedDateTime.toString());
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
        public ValueOut typePrefix(CharSequence typeName) {
            bytes.writeUtf8(typeName);
            return this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull CharSequence type) {
            bytes.writeUtf8(type);
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
        public <T> WireOut sequence(T t, BiConsumer<T, ValueOut> writer) {
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writer.accept(t, this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.writePosition() - position - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) {
            long position = bytes.writePosition();
            bytes.writeInt(0);

            object.writeMarshallable(RawWire.this);

            int length = Maths.toInt32(bytes.writePosition() - position - 4, "Document length %,d out of 32-bit int range.");
            bytes.writeOrderedInt(position, length);
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
            bytes.writeUtf8(o == null ? null : o.toString());
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut wireOut() {
            return RawWire.this;
        }
    }

    class RawValueIn implements ValueIn {
        final ValueInStack stack = new ValueInStack();

        @Override
        public void resetState() {
            stack.reset();
        }

        public void pushState() {
            stack.push();
        }

        public void popState() {
            stack.pop();
        }

        public ValueInState curr() {
            return stack.curr();
        }

        @NotNull
        @Override
        public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> flag) {
            int b = bytes.readUnsignedByte();
            if (b == BinaryWireCode.NULL)
                flag.accept(t, null);
            else if (b == 0 || b == BinaryWireCode.FALSE)
                flag.accept(t, false);
            else
                flag.accept(t, true);
            return RawWire.this;
        }

        @Override
        public boolean isTyped() {
            return false;
        }

        @Override
        public Class typePrefix() {
            return Object.class;
        }

        @Nullable
        @Override
        public String text() {
            return use8bit ? bytes.readUtf8() : bytes.read8bit();
        }

        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder s) {
            if (use8bit)
                return bytes.read8bit(s) ? s : null;
            else
                return bytes.readUtf8(s) ? s : null;
        }

        @Nullable
        @Override
        public Bytes textTo(@NotNull Bytes s) {
            if (use8bit)
                return bytes.read8bit(s) ? s : null;
            else
                return bytes.readUtf8(s) ? s : null;
        }

        @NotNull
        public WireIn bytes(@NotNull Bytes toBytes) {
            toBytes.clear();

            long length = readLength();
            Bytes<?> bytes = wireIn().bytes();

            toBytes.write((BytesStore) bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return wireIn();
        }

        @Nullable
        @Override
        public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
            throw new UnsupportedOperationException("todo");
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
        public WireIn bytes(@NotNull ReadBytesMarshallable bytesConsumer) {
            long length = readLength();

            if (length > bytes.readRemaining())
                throw new BufferUnderflowException();
            long limit0 = bytes.readLimit();
            long limit = bytes.readPosition() + length;
            try {
                bytes.readLimit(limit);
                bytesConsumer.readMarshallable(bytes);
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
        public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
            tb.accept(t, bytes.readByte());
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            ti.accept(t, (short) bytes.readUnsignedByte());
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            ti.accept(t, bytes.readShort());
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            ti.accept(t, bytes.readUnsignedShort());
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            ti.accept(t, bytes.readInt());
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            tl.accept(t, bytes.readUnsignedInt());
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            tl.accept(t, bytes.readLong());
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            tf.accept(t, bytes.readFloat());
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            td.accept(t, bytes.readDouble());
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
            setLocalTime.accept(t, LocalTime.ofNanoOfDay(bytes.readLong()));
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
            tZonedDateTime.accept(t, ZonedDateTime.parse(bytes.readUtf8()));
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
            tLocalDate.accept(t, LocalDate.ofEpochDay(bytes.readStopBit()));
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
            tuuid.accept(t, new UUID(bytes.readLong(), bytes.readLong()));
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter) {
            if (!(values instanceof Byteable)) {
                values = new BinaryLongArrayReference();
            }
            Byteable b = (Byteable) values;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            setter.accept(t, values);
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                setter.accept(t, value = new BinaryLongReference());
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
        public <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                setter.accept(t, value = new BinaryIntReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            bytes.readUtf8(sb);
            ts.accept(t, sb);
            return this;
        }

        @NotNull
        @Override
        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer) {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            bytes.readUtf8(sb);
            classNameConsumer.accept(t, sb);
            return RawWire.this;
        }

        @Override
        public <T> Class<T> typeLiteral() {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            bytes.readUtf8(sb);
            try {
                return classLookup.forName(sb);
            } catch (ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
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
        public <T> T typedMarshallable() {
            throw new UnsupportedOperationException("todo");
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
            return bytes.readUnsignedShort();
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
            return bytes.readDouble();
        }

        @Override
        public float float32() {
            return bytes.readFloat();
        }

        @Nullable
        @Override
        public <E> E object(@Nullable E using, @NotNull Class<E> clazz) {
            throw new UnsupportedOperationException("todo");
        }

        @Nullable
        @Override
        public <T, E> WireIn object(@NotNull Class<E> clazz, T t, BiConsumer<T, E> e) {
            throw new UnsupportedOperationException("todo");
        }
    }
}
