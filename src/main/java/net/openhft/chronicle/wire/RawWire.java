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
 *
 * @since 2023-09-11
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RawWire extends AbstractWire implements Wire {

    // Output mechanism for writing raw values
    private final RawValueOut valueOut = new RawValueOut();

    // Input mechanism for reading raw values
    private final RawValueIn valueIn = new RawValueIn();

    // Context for writing data to this wire
    private final WriteDocumentContext writeContext = new BinaryWriteDocumentContext(this);

    // Context for reading data from this wire without metadata
    private final BinaryReadDocumentContext readContext = new BinaryReadDocumentContext(this, false);
    @Nullable
    private StringBuilder lastSB;

    /**
     * Constructs a new instance of {@code RawWire} with the provided bytes.
     * By default, it uses an 8-bit representation.
     *
     * @param bytes The bytes to be used for the wire's underlying storage.
     */
    public RawWire(@NotNull Bytes<?> bytes) {
        this(bytes, true);
    }

    /**
     * Constructs a new instance of {@code RawWire} with the provided bytes
     * and a choice for 8-bit representation.
     *
     * @param bytes The bytes to be used for the wire's underlying storage.
     * @param use8bit A flag indicating if an 8-bit representation should be used.
     */
    public RawWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    /**
     * Resets the state of this RawWire instance.
     * All buffers and contexts will be cleared to their initial states.
     */
    @Override
    public void reset() {
        valueIn.resetState();
        valueOut.resetState();
        writeContext.reset();
        readContext.reset();
        bytes.clear();
        lastSB = null;
    }

    /**
     * Indicates whether the wire format is binary.
     *
     * @return true, as RawWire is a binary format.
     */
    @Override
    public boolean isBinary() {
        return true;
    }

    /**
     * Prepares the wire for writing a document with the given metadata state.
     * This method starts a new document context and readies the wire for receiving data.
     *
     * @param metaData If true, the document context will expect metadata. If false, it won't.
     * @return A {@link DocumentContext} instance representing the current write operation.
     */
    @NotNull
    @Override
    public DocumentContext writingDocument(boolean metaData) {
        writeContext.start(metaData);
        return writeContext;
    }

    /**
     * Acquires a {@link DocumentContext} for writing. If a context is already open,
     * it returns the open context; otherwise, a new context is started.
     * This method is particularly useful for reusing existing write contexts,
     * thereby reducing the overhead of context initialization.
     *
     * @param metaData If true, the document context will expect metadata. If false, it won't.
     * @return An open {@link DocumentContext} ready for writing.
     */
    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        if (writeContext.isOpen())
            return writeContext;
        return writingDocument(metaData);
    }

    /**
     * Prepares the wire for reading a document starting at the current read position.
     * This method starts a new read context to begin reading data.
     *
     * @return A {@link DocumentContext} instance representing the current read operation.
     */
    @NotNull
    @Override
    public DocumentContext readingDocument() {
        readContext.start();
        return readContext;
    }

    /**
     * Prepares the wire for reading a document starting at a specified read location.
     * The read position and limit are adjusted to the specified location before reading starts.
     *
     * @param readLocation The location to start reading from.
     * @return A {@link DocumentContext} instance representing the current read operation.
     */
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

    /**
     * RawWire format does not have any padding by definition.
     * This method is essentially a no-op for the RawWire.
     */
    @Override
    public void consumePadding() {
        // Do nothing
    }

    /**
     * Peeks at the YAML representation of the current reading position.
     * This method is useful for debugging or understanding the structure of data.
     *
     * @return A string representation in YAML format of the data from the current read position.
     */
    @Override
    @NotNull
    public String readingPeekYaml() {
        long start = readContext.start;
        if (start == -1)
            return "";
        return Wires.fromSizePrefixedBlobs(bytes, start);
    }

    /**
     * Copies the content of this RawWire to another WireOut instance.
     * Note: This method only supports copying to another RawWire instance.
     *
     * @param wire The destination {@link WireOut} instance to copy to.
     * @throws UnsupportedOperationException If trying to copy to a non-RawWire format.
     */
    @Override
    public void copyTo(@NotNull WireOut wire) {
        if (wire instanceof RawWire) {
            wire.bytes().write(bytes);

        } else {
            throw new UnsupportedOperationException("Can only copy Raw Wire format to the same format.");
        }
    }

    /**
     * Reads a value from the wire.
     * This method doesn't interpret any event or field name and directly focuses on the value.
     *
     * @return An instance of {@link ValueIn} representing the read value.
     */
    @NotNull
    @Override
    public ValueIn read() {
        lastSB = null;
        return valueIn;
    }

    /**
     * Reads a value associated with a specific key from the wire.
     * This method is designed for cases where the data format contains key-value pairs.
     *
     * @param key The key associated with the value to read.
     * @return An instance of {@link ValueIn} representing the read value.
     */
    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        lastSB = null;
        return valueIn;
    }

    /**
     * Reads an event name from the wire into the provided {@link StringBuilder}.
     * This event name may correspond to a field or a particular section in the wire.
     *
     * @param name A {@link StringBuilder} to which the event name is appended.
     * @return An instance of {@link ValueIn} representing the value associated with the event.
     */
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

    /**
     * Reads and interprets an event from the wire.
     *
     * @param expectedClass The expected class type of the event object.
     * @return The event object of the given expected class type.
     * @throws InvalidMarshallableException If the read data doesn't match the expected type or is malformed.
     */
    @Nullable
    @Override
    public <K> K readEvent(@NotNull Class<K> expectedClass) throws InvalidMarshallableException {
        return valueIn.object(expectedClass);
    }

    /**
     * Reads a value from the wire and associates it with a name.
     * The name represents the key or field for the given value.
     *
     * @param name A {@link StringBuilder} to which the value's associated name is appended.
     * @return An instance of {@link ValueIn} representing the read value.
     */
    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        lastSB = name;
        return valueIn;
    }

    /**
     * Retrieves the current {@link ValueIn} instance, which represents the read value.
     * This method provides direct access to the underlying ValueIn object.
     *
     * @return The current {@link ValueIn} instance.
     */
    @NotNull
    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    /**
     * Reads a comment from the wire.
     * Note: As the RawWire format does not support comments, this method is essentially a no-op and returns the wire itself.
     *
     * @param sb The {@link StringBuilder} to which the comment would be appended if present.
     * @return The current {@link Wire} instance, essentially RawWire.
     */
    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder sb) {
        return RawWire.this;
    }

    /**
     * Clears the underlying bytes of this wire.
     */
    @Override
    public void clear() {
        bytes.clear();
    }

    /**
     * Creates a new reference for a boolean value.
     *
     * @return A new boolean value reference.
     * @throws UnsupportedOperationException Currently not supported and will throw this exception if called.
     */
    @NotNull
    @Override
    public BooleanValue newBooleanReference() {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * Determines whether a self-describing message should be used based on the provided object.
     *
     * @param object The object to check.
     * @return {@code true} if the object uses a self-describing message, {@code false} otherwise.
     */
    @Override
    public boolean useSelfDescribingMessage(@NotNull CommonMarshallable object) {
        return object.usesSelfDescribingMessage();
    }

    /**
     * Provides direct access to the underlying bytes of this wire.
     *
     * @return The underlying bytes.
     */
    @NotNull
    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    /**
     * Aligns the reading position in the wire based on a specified alignment.
     *
     * @param alignment The alignment boundary to align the read position to.
     * @return The current {@link WireIn} instance.
     */
    @NotNull
    @Override
    public WireIn readAlignTo(int alignment) {
        long mod = bytes.readPosition() % alignment;
        bytes.readSkip(mod);
        return this;
    }

    /**
     * Returns a string representation of the wire based on its underlying bytes.
     *
     * @return The string representation.
     */
    @Override
    public String toString() {
        return bytes.toString();
    }

    /**
     * Initiates a write operation on the wire.
     *
     * @return An instance of {@link ValueOut} that represents the value to be written.
     */
    @NotNull
    @Override
    public ValueOut write() {
        return valueOut;
    }

    /**
     * Initiates a write operation on the wire with a specific event name.
     *
     * @param key The {@link WireKey} that represents the event name to be written.
     * @return An instance of {@link ValueOut} that represents the value associated with the event.
     */
    @NotNull
    @Override
    public ValueOut writeEventName(@NotNull WireKey key) {
        return writeEventName(key.name());
    }

    /**
     * Initiates a write operation on the wire with a specific event name.
     *
     * @param name The event name to be written.
     * @return An instance of {@link ValueOut} that represents the value associated with the event.
     */
    @NotNull
    @Override
    public ValueOut writeEventName(@NotNull CharSequence name) {
        if (use8bit)
            bytes.write8bit(name);
        else
            bytes.writeUtf8(name);
        return valueOut;
    }

    /**
     * Starts the write event. This method is a no-op for {@code RawWire} as it does not
     * deal with any metadata or delimiters.
     */
    @Override
    public void writeStartEvent() {
        // Do nothing
    }

    /**
     * Ends the write event. This method is a no-op for {@code RawWire} as it does not
     * deal with any metadata or delimiters.
     */
    @Override
    public void writeEndEvent() {
        // Do nothing
    }

    /**
     * Initiates a write operation on the wire with a specific {@link WireKey}.
     *
     * @param key The {@link WireKey} to write.
     * @return An instance of {@link ValueOut} that represents the value to be written.
     */
    @NotNull
    @Override
    public ValueOut write(@NotNull WireKey key) {
        return valueOut;
    }

    /**
     * Initiates a write operation on the wire with a specific name.
     *
     * @param name The name to write.
     * @return An instance of {@link ValueOut} that represents the value to be written.
     */
    @NotNull
    @Override
    public ValueOut write(@NotNull CharSequence name) {
        return valueOut;
    }

    /**
     * Provides direct access to the output mechanism for writing raw values to the wire.
     *
     * @return An instance of {@link ValueOut} used for writing values to this wire.
     */
    @NotNull
    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    /**
     * Adds a comment to the wire. As {@code RawWire} focuses on pure data without metadata,
     * this operation does not modify the wire and simply returns the instance itself.
     *
     * @param s The comment to add.
     * @return The current {@link Wire} instance.
     */
    @NotNull
    @Override
    public Wire writeComment(CharSequence s) {
        return RawWire.this;
    }

    /**
     * Adds padding bytes to the wire.
     *
     * @param paddingToAdd The number of padding bytes to add.
     * @return The current {@link WireOut} instance.
     */
    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        for (int i = 0; i < paddingToAdd; i++)
            bytes.writeByte((byte) 0);
        return this;
    }

    /**
     * Creates a new reference for a long value.
     *
     * @return A new long value reference.
     */
    @NotNull
    @Override
    public LongValue newLongReference() {
        return new BinaryLongReference();
    }

    /**
     * Creates a new reference for an integer value.
     *
     * @return A new integer value reference.
     */
    @NotNull
    @Override
    public IntValue newIntReference() {
        return new BinaryIntReference();
    }

    /**
     * Creates a new reference for a long array.
     *
     * @return A new long array reference.
     */
    @NotNull
    @Override
    public BinaryLongArrayReference newLongArrayReference() {
        return new BinaryLongArrayReference();
    }

    /**
     * Creates a new reference for an integer array.
     *
     * @return A new integer array reference.
     */
    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
        return new BinaryIntArrayReference();
    }

    /**
     * An inner class that facilitates the writing of raw values to the wire.
     */
    class RawValueOut implements ValueOut {

        /**
         * Writes a boolean value to the wire. If the value is {@code null}, a special
         * marker for "null" is written.
         *
         * @param flag The Boolean value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut bool(@Nullable Boolean flag) {
            if (flag == null)
                bytes.writeUnsignedByte(BinaryWireCode.NULL);
            else
                bytes.writeUnsignedByte(flag ? BinaryWireCode.TRUE : 0);
            return RawWire.this;
        }

        /**
         * Writes a text (string/char sequence) value to the wire.
         *
         * @param s The CharSequence to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            if (use8bit)
                bytes.write8bit(s);
            else
                bytes.writeUtf8(s);
            return RawWire.this;
        }

        /**
         * Writes a text value from a given {@link BytesStore} to the wire.
         *
         * @param s The BytesStore containing the text data.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut text(@Nullable BytesStore s) {
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

        /**
         * Writes a single byte to the wire.
         *
         * @param i8 The byte value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int8(byte i8) {
            bytes.writeByte(i8);
            return RawWire.this;
        }

        /**
         * Writes a provided {@link BytesStore} to the wire.
         *
         * @param bytesStore The BytesStore to write, or {@code null} if none.
         * @return The current {@link WireOut} instance.
         */
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

        /**
         * Writes a byte array to the wire, prefixed with a type descriptor.
         *
         * @param type     A descriptor or identifier for the byte array.
         * @param bytesArr The byte array to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut bytes(String type, @NotNull byte[] bytesArr) {
            typePrefix(type);
            return bytes(bytesArr);
        }

        /**
         * Writes a {@link BytesStore} to the wire, prefixed with a type descriptor.
         *
         * @param type      A descriptor or identifier for the BytesStore data.
         * @param fromBytes The BytesStore to write, or {@code null} if none.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut bytes(String type, @Nullable BytesStore fromBytes) {
            typePrefix(type);
            return bytes(fromBytes);
        }

        /**
         * Writes a raw byte array to the wire without any metadata or descriptors.
         *
         * @param value The byte array to write.
         * @throws UnsupportedOperationException if this method is not yet implemented.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut rawBytes(byte[] value) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Writes the length of the data that will follow in the wire.
         *
         * @param length The length to write.
         * @return The current {@link ValueOut} instance.
         */
        @NotNull
        @Override
        public ValueOut writeLength(long length) {
            bytes.writeStopBit(length);
            return this;
        }

        /**
         * Writes a byte array to the wire.
         *
         * @param fromBytes The byte array to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut bytes(@NotNull byte[] fromBytes) {
            writeLength(fromBytes.length);
            bytes.write(fromBytes);
            return RawWire.this;
        }

        /**
         * Writes an unsigned byte (8 bits) to the wire.
         *
         * @param u8 The unsigned byte value to write. Must be in the range [0, 255].
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            bytes.writeUnsignedByte(u8);
            return RawWire.this;
        }

        /**
         * Writes a short (16 bits) to the wire.
         *
         * @param i16 The short value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int16(short i16) {
            bytes.writeShort(i16);
            return RawWire.this;
        }

        /**
         * Writes an unsigned short (16 bits) to the wire.
         *
         * @param u16 The unsigned short value to write. Must be in the range [0, 65535].
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            bytes.writeUnsignedShort(u16);
            return RawWire.this;
        }

        /**
         * Writes a single UTF-8 encoded codepoint to the wire.
         *
         * @param codepoint The Unicode codepoint to encode and write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            bytes.appendUtf8(codepoint);
            return RawWire.this;
        }

        /**
         * Writes an integer (32 bits) to the wire.
         *
         * @param i32 The integer value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int32(int i32) {
            bytes.writeInt(i32);
            return RawWire.this;
        }

        /**
         * Writes an unsigned integer (32 bits) to the wire.
         *
         * @param u32 The unsigned integer value to write. Must be in the range [0, 4294967295].
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            bytes.writeUnsignedInt(u32);
            return RawWire.this;
        }

        /**
         * Writes a long (64 bits) to the wire.
         *
         * @param i64 The long value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int64(long i64) {
            bytes.writeLong(i64);
            return RawWire.this;
        }

        /**
         * This is a placeholder for writing 128-bit integers, split into two 64-bit longs.
         * Currently not supported.
         *
         * @param i64x0 First 64 bits.
         * @param i64x1 Second 64 bits.
         * @param longValue TODO: Clarify purpose.
         * @return The current {@link WireOut} instance.
         * @throws UnsupportedOperationException since this feature is not implemented yet.
         */
        @NotNull
        @Override
        public WireOut int128forBinding(long i64x0, long i64x1, TwoLongValue longValue) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Writes a long (64 bits) to the wire. This is equivalent to the int64 method.
         *
         * @param i64 The long value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int64_0x(long i64) {
            return int64(i64);
        }

        /**
         * Writes a long array with the given capacity to the wire.
         *
         * @param capacity Capacity of the long array.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
            return RawWire.this;
        }

        /**
         * Writes a long array with the given capacity and values to the wire.
         *
         * @param capacity Capacity of the long array.
         * @param values An instance of {@link LongArrayValues} to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int64array(long capacity, @NotNull LongArrayValues values) {
            long pos = bytes.writePosition();
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
            ((Byteable) values).bytesStore(bytes, pos, bytes.lengthWritten(pos));
            return RawWire.this;
        }

        /**
         * Writes a float (32 bits) to the wire.
         *
         * @param f The float value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut float32(float f) {
            bytes.writeFloat(f);
            return RawWire.this;
        }

        /**
         * Writes a double (64 bits) to the wire.
         *
         * @param d The double value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut float64(double d) {
            bytes.writeDouble(d);
            return RawWire.this;
        }

        /**
         * Writes the nanoseconds representation of a LocalTime to the wire.
         *
         * @param localTime The LocalTime value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut time(@NotNull LocalTime localTime) {
            long t = localTime.toNanoOfDay();
            bytes.writeLong(t);
            return RawWire.this;
        }

        /**
         * Writes a ZonedDateTime as a string to the wire.
         *
         * @param zonedDateTime The ZonedDateTime value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            if (use8bit)
                bytes.write8bit(zonedDateTime.toString());
            else
                bytes.writeUtf8(zonedDateTime.toString());
            return RawWire.this;
        }

        /**
         * Writes a LocalDate to the wire using its epoch day value.
         *
         * @param localDate The LocalDate value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            bytes.writeStopBit(localDate.toEpochDay());
            return RawWire.this;
        }

        /**
         * Writes a LocalDateTime to the wire by serializing its date and time components.
         *
         * @param localDateTime The LocalDateTime value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut dateTime(@NotNull LocalDateTime localDateTime) {
            date(localDateTime.toLocalDate());
            time(localDateTime.toLocalTime());
            return RawWire.this;
        }

        /**
         * Writes a type prefix as a UTF-8 string to the wire.
         *
         * @param typeName The type name to write as a prefix.
         * @return The current ValueOut instance.
         */
        @NotNull
        @Override
        public ValueOut typePrefix(CharSequence typeName) {
            bytes.writeUtf8(typeName);
            return this;
        }

        /**
         * Returns the current class lookup of the RawWire.
         *
         * @return The ClassLookup instance of RawWire.
         */
        @Override
        public ClassLookup classLookup() {
            return RawWire.this.classLookup();
        }

        /**
         * Writes a type literal as a UTF-8 string to the wire.
         *
         * @param type The type literal to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut typeLiteral(@Nullable CharSequence type) {
            bytes.writeUtf8(type);
            return RawWire.this;
        }

        /**
         * Writes a type literal to the wire by using a provided translator function.
         *
         * @param typeTranslator A consumer to transform the type into bytes.
         * @param type The class type to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, @Nullable Class type) {
            long position = bytes.writePosition();
            bytes.writeSkip(1);
            typeTranslator.accept(type, bytes);
            bytes.writeUnsignedByte(position, Maths.toInt8(bytes.lengthWritten(position) - 1));
            return RawWire.this;
        }

        /**
         * Writes the binary representation of a UUID to the wire.
         *
         * @param uuid The UUID value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut uuid(@NotNull UUID uuid) {
            bytes.writeLong(uuid.getMostSignificantBits());
            bytes.writeLong(uuid.getLeastSignificantBits());
            return RawWire.this;
        }

        /**
         * Writes a 32-bit integer to the wire.
         *
         * @param value The int value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            bytes.writeInt(value);
            return RawWire.this;
        }

        /**
         * Writes a 64-bit integer to the wire.
         *
         * @param value The long value to write.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            bytes.writeLong(value);
            return RawWire.this;
        }

        /**
         * Writes a 32-bit integer to the wire and binds its position to an IntValue reference.
         *
         * @param value The int value to write.
         * @param intValue The IntValue reference for the position of the int value.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int32forBinding(int value, @NotNull IntValue intValue) {
            int32forBinding(value);
            ((BinaryIntReference) intValue).bytesStore(bytes, bytes.writePosition() - 4, 4);
            return RawWire.this;
        }

        /**
         * Writes a 64-bit integer to the wire and binds its position to a LongValue reference.
         *
         * @param value The long value to write.
         * @param longValue The LongValue reference for the position of the long value.
         * @return The current {@link WireOut} instance.
         */
        @NotNull
        @Override
        public WireOut int64forBinding(long value, @NotNull LongValue longValue) {
            int64forBinding(value);
            ((BinaryLongReference) longValue).bytesStore(bytes, bytes.writePosition() - 8, 8);
            return RawWire.this;
        }

        /**
         * Writes the provided boolean value to the underlying byte stream. This method also binds
         * the serialized value to a {@link BooleanValue} reference for potential future use or inspection.
         *
         * @param value The boolean value to be serialized.
         * @param longValue The reference where the serialized value is bound.
         * @return The current instance of the RawWire.
         */
        @NotNull
        @Override
        public WireOut boolForBinding(final boolean value, @NotNull final BooleanValue longValue) {
            bool(value);
            ((BinaryLongReference) longValue).bytesStore(bytes, bytes.writePosition() - 1, 1);
            return RawWire.this;
        }

        /**
         * Serializes a sequence by invoking a provided writer callback. The method first writes
         * a placeholder for the length of the sequence and then delegates the serialization to the callback.
         * After serialization, the placeholder is replaced with the actual length.
         *
         * @param t The object representing the sequence to be serialized.
         * @param writer The callback responsible for serializing the sequence.
         * @return The current instance of the RawWire.
         */
        @NotNull
        @Override
        public <T> WireOut sequence(T t, @NotNull BiConsumer<T, ValueOut> writer) {
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writer.accept(t, this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.lengthWritten(position) - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        /**
         * Serializes a sequence with an additional classification parameter. Similar to the above method,
         * but allows for finer-grained serialization using both the sequence and its classification.
         *
         * @param t The object representing the sequence to be serialized.
         * @param kls The classification or type of the sequence.
         * @param writer The callback responsible for the combined serialization.
         * @return The current instance of the RawWire.
         * @throws InvalidMarshallableException If there's an issue during serialization.
         */
        @NotNull
        @Override
        public <T, K> WireOut sequence(T t, K kls, @NotNull TriConsumer<T, K, ValueOut> writer) throws InvalidMarshallableException {
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writer.accept(t, kls, this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.lengthWritten(position) - 4, "Document length %,d out of 32-bit int range."));
            return RawWire.this;
        }

        /**
         * Serializes a given object that implements the {@link WriteMarshallable} interface.
         * The method writes a placeholder for the length, delegates serialization to the provided object,
         * and then replaces the placeholder with the actual serialized length.
         *
         * @param object The object implementing the WriteMarshallable interface.
         * @return The current instance of the RawWire.
         * @throws InvalidMarshallableException If there's an issue during serialization.
         */
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

        /**
         * Serializes a given object that implements the {@link Serializable} interface.
         * The method starts by writing a placeholder for the serialized length, then delegates
         * the serialization to the private {@code writeSerializable} method. After serialization,
         * the placeholder is replaced with the actual length.
         *
         * @param object The object implementing the Serializable interface to be serialized.
         * @return The current instance of the RawWire.
         * @throws InvalidMarshallableException If there's an issue during serialization.
         */
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

        /**
         * Writes a given object that implements the {@link Serializable} interface to the underlying byte stream.
         * If the object also implements {@link Externalizable}, it uses the object's externalization
         * method for serialization. Otherwise, it delegates to {@code Wires.writeMarshallable}.
         *
         * @param object The object implementing the Serializable interface.
         * @throws InvalidMarshallableException If there's an issue during serialization.
         */
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

        /**
         * Placeholder method for serializing a map. Currently unsupported and will throw an exception.
         *
         * @param map The map to be serialized.
         * @return The current instance of the RawWire.
         * @throws UnsupportedOperationException Always thrown as this method is yet to be implemented.
         */
        @NotNull
        @Override
        public WireOut map(Map map) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Serializes a given object by writing its string representation to the underlying byte stream.
         * If the provided object is null, a null value is written.
         *
         * @param o The object to be serialized.
         * @return The current instance of the RawWire.
         */
        @NotNull
        @Override
        public WireOut object(@Nullable Object o) {
            bytes.writeUtf8(o == null ? null : o.toString());
            return RawWire.this;
        }

        /**
         * Retrieves the current instance of RawWire.
         *
         * @return The current instance of the RawWire.
         */
        @NotNull
        @Override
        public WireOut wireOut() {
            return RawWire.this;
        }

        /**
         * Reset method for the RawValueOut. Currently, a no-op method.
         */
        @Override
        public void resetState() {
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

        // Stack for maintaining internal state
        final ValueInStack stack = new ValueInStack();

        /**
         * Resets the internal state of the {@code RawValueIn} to its default.
         * This primarily involves resetting the state stack.
         */
        @Override
        public void resetState() {
            stack.reset();
        }

        /**
         * Pushes the current state onto the state stack, effectively
         * saving the current state for future retrieval.
         */
        public void pushState() {
            stack.push();
        }

        /**
         * Pops and restores the most recent state from the state stack,
         * reverting the internal state to the previously saved state.
         */
        public void popState() {
            stack.pop();
        }

        /**
         * Retrieves the current state from the state stack.
         *
         * @return The current {@link ValueInState}.
         */
        public ValueInState curr() {
            return stack.curr();
        }

        /**
         * Reads a boolean value from the binary wire and passes it
         * to the provided consumer. If the read value corresponds to a serialized
         * {@code NULL}, the consumer is passed a {@code null}. The method understands
         * specific wire codes to represent boolean values, such as {@code BinaryWireCode.FALSE}
         * for {@code false}.
         *
         * @param t    The target object that the boolean flag will be applied to.
         * @param flag The consumer that will accept the target object and the deserialized boolean flag.
         * @return The current instance of {@link WireIn}.
         */
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

        /**
         * Determines if the current serialized object or context has a type prefix.
         *
         * @return {@code false} as the current implementation does not support type prefixes.
         */
        @Override
        public boolean isTyped() {
            return false;
        }

        /**
         * Retrieves the type prefix for the current serialized object or context.
         *
         * @return {@code null} as the current implementation does not support type prefixes.
         */
        @Override
        public Class typePrefix() {
            return null;
        }

        /**
         * Reads a text value from the binary wire. Depending on the encoding (8-bit or UTF-8),
         * the appropriate method to read the text is chosen.
         *
         * @return The deserialized text as a {@link String} or {@code null} if no text is present.
         */
        @Nullable
        @Override
        public String text() {
            return use8bit ? bytes.readUtf8() : bytes.read8bit();
        }

        /**
         * Reads a text value from the binary wire and appends it to the provided {@link StringBuilder}.
         * Depending on the encoding (8-bit or UTF-8), the appropriate method to read the text is chosen.
         *
         * @param s The {@link StringBuilder} to which the text will be appended.
         * @return The provided {@link StringBuilder} with the appended text or {@code null} if no text is present.
         */
        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder s) {
            if (use8bit)
                return bytes.read8bit(s) ? s : null;
            else
                return bytes.readUtf8(s) ? s : null;
        }

        /**
         * Reads a text value from the binary wire and appends it to the provided {@link Bytes} object.
         * Depending on the encoding (8-bit or UTF-8), the appropriate method to read the text is chosen.
         *
         * @param s The {@link Bytes} object to which the text will be appended.
         * @return The provided {@link Bytes} object with the appended text or {@code null} if no text is present.
         */
        @Nullable
        @Override
        public Bytes<?> textTo(@NotNull Bytes<?> s) {
            if (use8bit)
                return bytes.read8bit(s) ? s : null;
            else
                return bytes.readUtf8(s) ? s : null;
        }

        /**
         * Reads a sequence of bytes from the wire and writes it to the provided {@link BytesOut} object.
         * The existing content of the {@link BytesOut} will be cleared before writing.
         *
         * @param toBytes The {@link BytesOut} object to write the sequence of bytes to.
         * @return The current instance of the {@link WireIn} interface.
         */
        @Override
        @NotNull
        public WireIn bytes(@NotNull BytesOut<?> toBytes) {
            return bytes(toBytes, true);
        }

        /**
         * Reads a sequence of bytes from the wire and writes it to the provided {@link BytesOut} object.
         * Optionally clears the content of the {@link BytesOut} before writing based on the value of clearBytes.
         *
         * @param toBytes    The {@link BytesOut} object to write the sequence of bytes to.
         * @param clearBytes If true, clears the content of {@link BytesOut} before writing.
         * @return The current instance of the {@link WireIn} interface.
         */
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

        /**
         * Sets a {@link PointerBytesStore} object with the bytes from the wire.
         *
         * @param toBytes The {@link PointerBytesStore} object to be set.
         * @return The current instance of the {@link WireIn} interface.
         * @throws UnsupportedOperationException as the current implementation is pending.
         */
        @Nullable
        @Override
        public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Reads a sequence of bytes from the wire and compares it to a provided sequence.
         * If the sequences are of the same length and match, the provided consumer is informed.
         *
         * @param compareBytes The sequence of bytes to compare against.
         * @param consumer     A {@link BooleanConsumer} that accepts the result of the comparison.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public WireIn bytesMatch(@NotNull BytesStore compareBytes, @NotNull BooleanConsumer consumer) {
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

        /**
         * Reads a sequence of bytes from the wire and processes them using the provided {@link ReadBytesMarshallable} consumer.
         * Ensures that the read limit of the bytes does not exceed the specified length, resets to original after reading.
         *
         * @param bytesConsumer The consumer to process the sequence of bytes.
         * @return The current instance of the {@link WireIn} interface.
         * @throws BufferUnderflowException If the length to be read exceeds the remaining bytes available.
         */
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

        /**
         * Retrieves a sequence of bytes from the wire into a byte array.
         * The method is not yet supported in the current implementation.
         *
         * @param using The byte array to store the sequence of bytes.
         * @return The byte array containing the sequence of bytes.
         * @throws UnsupportedOperationException as the current implementation is pending.
         */
        @NotNull
        @Override
        public byte @NotNull [] bytes(byte[] using) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Provides access to the {@link RawWire} instance associated with this value.
         *
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public WireIn wireIn() {
            return RawWire.this;
        }

        /**
         * Reads and returns the length value from the wire.
         * The length is represented as a stop-bit encoded number.
         *
         * @return The decoded length from the wire.
         */
        @Override
        public long readLength() {
            return bytes.readStopBit();
        }

        /**
         * Skips the current value being read from the wire.
         * The method is not yet supported in the current implementation.
         *
         * @return The current instance of the {@link WireIn} interface.
         * @throws UnsupportedOperationException as the current implementation is pending.
         */
        @NotNull
        @Override
        public WireIn skipValue() {
            throw new UnsupportedOperationException();
        }

        /**
         * Reads a signed 8-bit integer value from the wire and processes it using the provided consumer.
         *
         * @param t  The instance to be consumed.
         * @param tb The consumer to process the read 8-bit integer.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
            tb.accept(t, bytes.readByte());
            return RawWire.this;
        }

        /**
         * Reads an unsigned 8-bit integer value from the wire and processes it using the provided consumer.
         *
         * @param t  The instance to be consumed.
         * @param ti The consumer to process the read unsigned 8-bit integer.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            ti.accept(t, (short) bytes.readUnsignedByte());
            return RawWire.this;
        }

        /**
         * Reads a signed 16-bit integer value from the wire and processes it using the provided consumer.
         *
         * @param t  The instance to be consumed.
         * @param ti The consumer to process the read 16-bit integer.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            ti.accept(t, bytes.readShort());
            return RawWire.this;
        }

        /**
         * Reads an unsigned 16-bit integer value from the wire and processes it using the provided consumer.
         *
         * @param t  The instance to be consumed.
         * @param ti The consumer to process the read unsigned 16-bit integer.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            ti.accept(t, bytes.readUnsignedShort());
            return RawWire.this;
        }

        /**
         * Reads a signed 32-bit integer value from the wire and processes it using the provided consumer.
         *
         * @param t  The instance to be consumed.
         * @param ti The consumer to process the read 32-bit integer.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            ti.accept(t, bytes.readInt());
            return RawWire.this;
        }

        /**
         * Reads an unsigned 32-bit integer value from the wire and processes it using the provided consumer.
         *
         * @param t  The instance to be consumed.
         * @param tl The consumer to process the read unsigned 32-bit integer.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            tl.accept(t, bytes.readUnsignedInt());
            return RawWire.this;
        }

        /**
         * Reads a signed 64-bit integer value from the wire and processes it using the provided consumer.
         *
         * @param t  The instance to be consumed.
         * @param tl The consumer to process the read 64-bit integer.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            tl.accept(t, bytes.readLong());
            return RawWire.this;
        }

        /**
         * Reads a 32-bit floating point value from the wire and processes it using the provided consumer.
         *
         * @param t  The instance to be consumed.
         * @param tf The consumer to process the read 32-bit floating point value.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            tf.accept(t, bytes.readFloat());
            return RawWire.this;
        }

        /**
         * Reads a 64-bit floating point value from the wire and processes it using the provided consumer.
         *
         * @param t  The instance to be consumed.
         * @param td The consumer to process the read 64-bit floating point value.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            td.accept(t, bytes.readDouble());
            return RawWire.this;
        }

        /**
         * Reads a time value in the format of nanoseconds since the start of the day from the wire
         * and sets it to the provided instance using the provided bi-consumer.
         *
         * @param t           The instance to be set with the read time value.
         * @param setLocalTime The bi-consumer to process and set the read time value.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
            setLocalTime.accept(t, LocalTime.ofNanoOfDay(bytes.readLong()));
            return RawWire.this;
        }

        /**
         * Reads a ZonedDateTime string value from the wire and processes it using the provided bi-consumer.
         *
         * @param t              The instance to be consumed.
         * @param tZonedDateTime The bi-consumer to process and set the read ZonedDateTime value.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
            tZonedDateTime.accept(t, ZonedDateTime.parse(bytes.readUtf8()));
            return RawWire.this;
        }

        /**
         * Reads a date value represented as epoch days from the wire
         * and sets it to the provided instance using the provided bi-consumer.
         *
         * @param t         The instance to be set with the read date value.
         * @param tLocalDate The bi-consumer to process and set the read date value.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
            tLocalDate.accept(t, LocalDate.ofEpochDay(bytes.readStopBit()));
            return RawWire.this;
        }

        /**
         * Reads a UUID from the wire in the form of two long values
         * and sets it to the provided instance using the provided bi-consumer.
         *
         * @param t      The instance to be set with the read UUID value.
         * @param tuuid The bi-consumer to process and set the read UUID.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
            tuuid.accept(t, new UUID(bytes.readLong(), bytes.readLong()));
            return RawWire.this;
        }

        /**
         * Reads a 64-bit integer array value from the wire and sets it
         * to the provided instance using the provided bi-consumer.
         *
         * @param values The current array values to be populated or replaced.
         * @param t      The instance to be set with the read values.
         * @param setter The bi-consumer to process and set the read values.
         * @return The current instance of the {@link WireIn} interface.
         */
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

        /**
         * Reads a single 64-bit integer value from the wire and sets it
         * to the provided instance using the provided bi-consumer.
         *
         * @param value  The current value to be populated or replaced.
         * @param t      The instance to be set with the read value.
         * @param setter The bi-consumer to process and set the read value.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter) {
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 8) {
                value = new BinaryLongReference();
                setter.accept(t, value);
            }
            return int64(value);
        }

        /**
         * Populates the provided {@link LongValue} instance with a 64-bit integer value from the wire.
         *
         * @param value The LongValue instance to be populated.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public WireIn int64(@NotNull LongValue value) {
            @Nullable Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return RawWire.this;
        }

        /**
         * Reads a 32-bit integer value from the wire and populates the provided {@link IntValue} instance.
         *
         * @param value The IntValue instance to be populated with the read value.
         * @return The current instance of the {@link WireIn} interface.
         */
        @NotNull
        @Override
        public WireIn int32(@NotNull IntValue value) {
            @NotNull Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return RawWire.this;
        }

        /**
         * Method designed to read a boolean value from the wire.
         * Currently, it throws an exception and needs to be implemented.
         *
         * @param ret The BooleanValue instance expected to be populated.
         * @throws UnsupportedOperationException if the method is invoked.
         */
        @Override
        public WireIn bool(@NotNull final BooleanValue ret) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Reads a single 32-bit integer value from the wire and sets it
         * to the provided instance using the provided bi-consumer.
         *
         * @param value  The current value to be populated or replaced.
         * @param t      The instance to be set with the read value.
         * @param setter The bi-consumer to process and set the read value.
         * @return The current instance of the {@link WireIn} interface.
         */
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

        /**
         * Sequence reading method designed for custom processing.
         * Currently, it throws an exception and needs to be implemented.
         *
         * @param t       The instance to be set with the read sequence.
         * @param tReader A bi-consumer to process the sequence.
         * @throws UnsupportedOperationException if the method is invoked.
         */
        @Override
        public <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Reads a list sequence from the wire.
         * Currently, it throws an exception and needs to be implemented.
         *
         * @param list      The list where the read sequence will be appended.
         * @param buffer    A buffer list for the operation.
         * @param bufferAdd A supplier to add items to the buffer.
         * @param reader0   The reader to process the sequence.
         * @throws UnsupportedOperationException if the method is invoked.
         */
        @Override
        public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Reads a sequence from the wire and processes it with a tri-consumer.
         * Currently, it throws an exception and needs to be implemented.
         *
         * @param t       The instance to be set with the read sequence.
         * @param kls     An additional class or key parameter for the tri-consumer.
         * @param tReader The tri-consumer to process the sequence.
         * @throws UnsupportedOperationException if the method is invoked.
         */
        @NotNull
        @Override
        public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Reads a UTF-8 string from the wire as a type prefix and then
         * uses the provided bi-consumer to process and set the read value.
         *
         * @param t   The instance to be set with the read prefix.
         * @param ts  The bi-consumer to process and set the read value.
         * @return The current instance of the {@link ValueIn} interface.
         */
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

        /**
         * Reads a UTF-8 string from the wire as a type literal
         * and then uses the provided bi-consumer to process and set the read value.
         *
         * @param t                   The instance to be set with the read type literal.
         * @param classNameConsumer   The bi-consumer to process and set the read value.
         * @return The current instance of the {@link WireIn} interface.
         */
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

        /**
         * Retrieves the class lookup associated with the current wire.
         *
         * @return An instance of {@link ClassLookup} for the current wire.
         */
        @Override
        public ClassLookup classLookup() {
            return RawWire.this.classLookup();
        }

        /**
         * Reads a UTF-8 string from the wire as a type literal and tries
         * to resolve it into a {@link Type}. If the type is not found, it
         * invokes the provided unresolved handler.
         *
         * @param unresolvedHandler A bi-function to handle unresolved type literals.
         * @return Resolved {@link Type} or the result of the unresolved handler.
         */
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

        /**
         * Applies the given marshallable reader function to the wire input after potentially
         * setting the read limits based on a read length. If the read length is positive, the read
         * limits of the bytes are adjusted before applying the reader function.
         *
         * @param marshallableReader The function to read marshallable data from the wire input.
         * @param <T> The type of the returned object after reading the marshallable data.
         * @return An instance of the marshallable data read from the wire input.
         */
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

        /**
         * Retrieves a typed marshallable object from the wire input.
         * Note: This operation is currently unsupported and will throw an exception when invoked.
         *
         * @param <T> The type of the marshallable object to be retrieved.
         * @return An instance of the typed marshallable object or null if unsupported.
         * @throws UnsupportedOperationException when invoked.
         */
        @Nullable
        @Override
        public <T> T typedMarshallable() {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Checks if there are more bytes to read from the wire input.
         *
         * @return True if there are remaining bytes to be read, false otherwise.
         */
        @Override
        public boolean hasNext() {
            return bytes.readRemaining() > 0;
        }

        /**
         * Checks if there is a next item in a sequence on the wire input.
         * Note: This operation is currently unsupported and will throw an exception when invoked.
         *
         * @return True if there is a next sequence item, false otherwise.
         * @throws UnsupportedOperationException when invoked.
         */
        @Override
        public boolean hasNextSequenceItem() {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Reads marshallable data from the wire input using the provided object and serialization strategy.
         *
         * <p>The method begins by reading an unsigned integer which represents the length of
         * the marshallable data. If the length is `0xFFFF_FFFFL`, the method returns null.
         * If the length of the data is greater than the remaining bytes to be read, it throws
         * an {@code IllegalStateException}. If the length is non-negative, the method adjusts
         * the read limits of the bytes and applies the serialization strategy's read operation.
         * After the read operation completes, the method resets the read limits and position.</p>
         *
         * @param object The object to populate with the marshallable data.
         * @param strategy The serialization strategy to use for reading the data.
         * @return The populated object or null if the length is `0xFFFF_FFFFL`.
         * @throws InvalidMarshallableException If there's an issue during marshalling.
         * @throws IllegalStateException If the read length is greater than the remaining bytes.
         */
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

        /**
         * Reads a boolean value from the wire input.
         *
         * @return The read boolean value from the bytes.
         */
        @Override
        public boolean bool() {
            return bytes.readBoolean();
        }

        /**
         * Reads a single byte (8-bit) integer from the wire input.
         *
         * @return The read 8-bit integer value from the bytes.
         */
        @Override
        public byte int8() {
            return bytes.readByte();
        }

        /**
         * Reads a 2-byte (16-bit) integer from the wire input.
         *
         * @return The read 16-bit integer value from the bytes.
         */
        @Override
        public short int16() {
            return bytes.readShort();
        }

        /**
         * Reads an unsigned 2-byte (16-bit) integer from the wire input.
         *
         * @return The read 16-bit unsigned integer value from the bytes,
         * presented as a 32-bit integer.
         */
        @Override
        public int uint16() {
            return bytes.readUnsignedShort();
        }

        /**
         * Reads a 4-byte (32-bit) integer from the wire input.
         *
         * @return The read 32-bit integer value from the bytes.
         */
        @Override
        public int int32() {
            return bytes.readInt();
        }

        /**
         * Reads an 8-byte (64-bit) integer from the wire input.
         *
         * @return The read 64-bit integer value from the bytes.
         */
        @Override
        public long int64() {
            return bytes.readLong();
        }

        /**
         * Reads an 8-byte (64-bit) floating-point number from the wire input.
         *
         * @return The read 64-bit double precision value from the bytes.
         */
        @Override
        public double float64() {
            return bytes.readDouble();
        }

        /**
         * Reads a 4-byte (32-bit) floating-point number from the wire input.
         *
         * @return The read 32-bit single precision value from the bytes.
         */
        @Override
        public float float32() {
            return bytes.readFloat();
        }

        /**
         * Checks if the current value is null.
         *
         * @return Always returns false, as the RawValueIn does not support null checks.
         */
        @Override
        public boolean isNull() {
            return false;
        }

        /**
         * Retrieves the type of brackets used for encapsulating data.
         *
         * @return Nothing, as this method is unsupported for scalar or nested types.
         * @throws IllegalArgumentException if invoked, as only scalar or nested types are supported.
         */
        @NotNull
        @Override
        public BracketType getBracketType() {
            throw new IllegalArgumentException("Only scalar or nested types supported");
        }

        /**
         * Reads an object of a certain inferred type.
         *
         * @param using The object instance to populate with the read data.
         * @param strategy The serialization strategy used for deserialization.
         * @param type The class type of the object.
         * @return The deserialized object.
         * @throws UnsupportedOperationException when trying to read unsupported types in RawWire.
         */
        @Override
        public Object objectWithInferredType(Object using, SerializationStrategy strategy, Class type) {
            throw new UnsupportedOperationException("Cannot read " + using + " value and " + type + " type for RawWire");
        }
    }
}
