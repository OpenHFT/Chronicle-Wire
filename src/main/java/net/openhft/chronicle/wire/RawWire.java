/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.bytes.ref.BinaryIntArrayReference;
import net.openhft.chronicle.bytes.ref.BinaryIntReference;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.bytes.ref.BinaryLongReference;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

/**
 * Represents a wire type that focuses on writing pure data, omitting any metadata.
 * The {@code RawWire} class is specifically designed for efficient binary serialization
 * where headers and other metadata might not be necessary.
 */
@SuppressWarnings({"rawtypes", "unchecked", "this-escape"})
public class RawWire extends AbstractWire implements Wire {

    // Output mechanism for writing raw values
    private final RawValueOut valueOut = new RawValueOut();

    // Input mechanism for reading raw values
    private final RawValueIn valueIn = new RawValueIn();

    // Context for writing data to this wire
    private final WriteDocumentContext writeContext = new BinaryWriteDocumentContext(this);

    // Context for reading data from this wire without metadata
    private final BinaryReadDocumentContext readContext = new BinaryReadDocumentContext(this);
    @Nullable
    private StringBuilder lastSB;

    public RawWire(@NotNull Bytes<?> bytes) {
        this(bytes, true);
    }

    public RawWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    @Override
    public void reset() {
        valueIn.resetState();
        valueOut.resetState();
        writeContext.reset();
        readContext.reset();
        bytes.clear();
        lastSB = null;
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @NotNull
    @Override
    public DocumentContext writingDocument(boolean metaData) {
        writeContext.start(metaData);
        return writeContext;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        if (writeContext.isOpen())
            return writeContext;
        return writingDocument(metaData);
    }

    @NotNull
    @Override
    public DocumentContext readingDocument() {
        readContext.start();
        return readContext;
    }

    @NotNull
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
        // Do nothing
    }

    @Override
    @NotNull
    public String readingPeekYaml() {
        long start = readContext.start;
        if (start == -1)
            return "";
        return Wires.fromSizePrefixedBlobs(bytes, start);
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

    @Nullable
    @Override
    public <K> K readEvent(@NotNull Class<K> expectedClass) throws InvalidMarshallableException {
        return valueIn.object(expectedClass);
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
    public BooleanValue newBooleanReference() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean useSelfDescribingMessage(@NotNull CommonMarshallable object) {
        return object.usesSelfDescribingMessage();
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        return bytes;
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
        return writeEventName(key.name());
    }

    @NotNull
    @Override
    public ValueOut writeEventName(@NotNull CharSequence name) {
        if (use8bit)
            bytes.write8bit(name);
        else
            bytes.writeUtf8(name);
        return valueOut;
    }

    @Override
    public void writeStartEvent() {
        // Do nothing
    }

    @Override
    public void writeEndEvent() {
        // Do nothing
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull WireKey key) {
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull CharSequence name) {
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
        for (int i = 0; i < paddingToAdd; i++)
            bytes.writeByte((byte) 0);
        return this;
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

    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
        return new BinaryIntArrayReference();
    }

    /**
     * An inner class that facilitates the writing of raw values to the wire.
     */
    class RawValueOut implements ValueOut {

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
        public WireOut text(@Nullable BytesStore<?, ?> s) {
            if (use8bit)
                if (s == null) {
                    bytes.writeStopBit(-1);
                } else {
                    long offset = s.readPosition();
                    long readRemaining = Math.min(bytes.writeRemaining(), s.readLimit() - offset);
                    bytes.writeStopBit(readRemaining);
                    try {
                        bytes.write(s, offset, readRemaining);
                    } catch (BufferUnderflowException | IllegalArgumentException e) {
                        throw new AssertionError(e);
                    }
                }
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
        public WireOut bytes(@Nullable BytesStore<?, ?> bytesStore) {
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
        public WireOut bytes(String type, @NotNull byte[] bytesArr) {
            typePrefix(type);
            return bytes(bytesArr);
        }

        @NotNull
        @Override
        public WireOut bytes(String type, @Nullable BytesStore<?, ?> fromBytes) {
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
        public WireOut int128forBinding(long i64x0, long i64x1, TwoLongValue longValue) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut int64_0x(long i64) {
            return int64(i64);
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
            ((Byteable) values).bytesStore(bytes, pos, bytes.lengthWritten(pos));
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
        public WireOut dateTime(@NotNull LocalDateTime localDateTime) {
            date(localDateTime.toLocalDate());
            time(localDateTime.toLocalTime());
            return RawWire.this;
        }

        @NotNull
        @Override
        public ValueOut typePrefix(CharSequence typeName) {
            bytes.writeUtf8(typeName);
            return this;
        }

        @Override
        public ClassLookup classLookup() {
            return RawWire.this.classLookup();
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@Nullable CharSequence type) {
            bytes.writeUtf8(type);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, @Nullable Class<?> type) {
            long position = bytes.writePosition();
            bytes.writeSkip(1);
            typeTranslator.accept(type, bytes);
            bytes.writeUnsignedByte(position, Maths.toInt8(bytes.lengthWritten(position) - 1));
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
        public WireOut boolForBinding(final boolean value, @NotNull final BooleanValue longValue) {
            bool(value);
            ((BinaryLongReference) longValue).bytesStore(bytes, bytes.writePosition() - 1, 1);
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T> WireOut sequence(T t, @NotNull BiConsumer<T, ValueOut> writer) {
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writer.accept(t, this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.lengthWritten(position) - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        @NotNull
        @Override
        public <T, K> WireOut sequence(T t, K kls, @NotNull TriConsumer<T, K, ValueOut> writer) throws InvalidMarshallableException {
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writer.accept(t, kls, this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.lengthWritten(position) - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) throws InvalidMarshallableException {
            long position = bytes.writePosition();
            bytes.writeInt(0);

            object.writeMarshallable(RawWire.this);

            int length = Maths.toInt32(bytes.lengthWritten(position) - 4, "Document length %,d out of 32-bit int range.");
            bytes.writeOrderedInt(position, length);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull Serializable object) throws InvalidMarshallableException {
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writeSerializable(object);

            int length = Maths.toInt32(bytes.lengthWritten(position) - 4, "Document length %,d out of 32-bit int range.");
            bytes.writeOrderedInt(position, length);
            return RawWire.this;
        }

        private void writeSerializable(@NotNull Serializable object) throws InvalidMarshallableException {
            try {
                if (object instanceof Externalizable)
                    ((Externalizable) object).writeExternal(objectOutput());
                else
                    Wires.writeMarshallable(object, RawWire.this);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }

        @NotNull
        @Override
        public WireOut map(Map map) {
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

        @Override
        public void resetState() {
            // Do nothing
        }

        @Override
        public void elementSeparator() {
            // Do nothing
        }
    }

    /**
     * The {@code RawValueIn} class implements the {@link ValueIn} interface,
     * providing functionality for reading values from a binary wire format.
     * Internally, it maintains a state stack that facilitates tracking
     * and managing nested serialized objects or contexts.
     */
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
            switch (b) {
                case BinaryWireCode.NULL:
                    flag.accept(t, null);
                    break;
                case 0:
                case BinaryWireCode.FALSE:
                    flag.accept(t, false);
                    break;
                default:
                    flag.accept(t, true);
                    break;
            }
            return RawWire.this;
        }

        @Override
        public boolean isTyped() {
            return false;
        }

        @Override
        public Class<?> typePrefix() {
            return null;
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
        public Bytes<?> textTo(@NotNull Bytes<?> s) {
            if (use8bit)
                return bytes.read8bit(s) ? s : null;
            else
                return bytes.readUtf8(s) ? s : null;
        }

        @Override
        @NotNull
        public WireIn bytes(@NotNull BytesOut<?> toBytes) {
            return bytes(toBytes, true);
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull BytesOut<?> toBytes, boolean clearBytes) {
            if (clearBytes)
                toBytes.clear();

            long length = readLength();
            @NotNull Bytes<?> bytes = wireIn().bytes();

            toBytes.write(bytes, bytes.readPosition(), length);
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
        public WireIn bytesMatch(@NotNull BytesStore<?, ?> compareBytes, @NotNull BooleanConsumer consumer) {
            long length = readLength();
            @NotNull Bytes<?> bytes = wireIn().bytes();

            if (length == compareBytes.readRemaining()) {
                consumer.accept(bytes.equalBytes(compareBytes, length));
            } else {
                consumer.accept(false);
            }
            bytes.readSkip(length);
            return wireIn();

        }

        @Override
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
        public byte @NotNull [] bytes(byte[] using) {
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
        public WireIn skipValue() {
            throw new UnsupportedOperationException();
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
            @Nullable Byteable b = (Byteable) values;
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
                value = new BinaryLongReference();
                setter.accept(t, value);
            }
            return int64(value);
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongValue value) {
            @Nullable Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return RawWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntValue value) {
            @NotNull Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return RawWire.this;
        }

        @Override
        public WireIn bool(@NotNull final BooleanValue ret) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                value = new BinaryIntReference();
                setter.accept(t, value);
            }
            @Nullable Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return RawWire.this;
        }

        @Override
        public <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                StringBuilder sb = stlSb.get();
                bytes.readUtf8(sb);
                ts.accept(t, sb);
            }
            return this;
        }

        @NotNull
        @Override
        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                StringBuilder sb = stlSb.get();
                bytes.readUtf8(sb);
                classNameConsumer.accept(t, sb);
            }
            return RawWire.this;
        }

        @Override
        public ClassLookup classLookup() {
            return RawWire.this.classLookup();
        }

        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                StringBuilder sb = stlSb.get();
                bytes.readUtf8(sb);
                try {
                    return classLookup.forName(sb);
                } catch (ClassNotFoundRuntimeException e) {
                    return unresolvedHandler.apply(sb, e.getCause());
                }
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

        @Override
        @Nullable
        public Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy) throws InvalidMarshallableException {
            long length = bytes.readUnsignedInt();
            if (length == 0xFFFF_FFFFL)
                return null;
            if (length > bytes.readRemaining()) {
                throw new IllegalStateException("Length was " + length
                        + " greater than remaining " + bytes.readRemaining());
            }
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.readPosition() + length;
                bytes.readLimit(limit2);
                try {
                    strategy.readUsing(null, object, this, BracketType.MAP);

                } finally {
                    bytes.readLimit(limit);
                    bytes.readPosition(limit2);
                }
            } else {
                strategy.readUsing(null, object, this, BracketType.MAP);
            }
            return object;
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

        @Override
        public boolean isNull() {
            return false;
        }

        @NotNull
        @Override
        public BracketType getBracketType() {
            throw new IllegalArgumentException("Only scalar or nested types supported");
        }

        @Override
        public Object objectWithInferredType(Object using, SerializationStrategy strategy, Class<?> type) {
            throw new UnsupportedOperationException("Cannot read " + using + " value and " + type + " type for RawWire");
        }
    }
}
