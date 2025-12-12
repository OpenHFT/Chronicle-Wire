/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytesDescription;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.*;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
/**
 * A specialised {@link WireOut} used only for hashing.
 * <p>
 * Values written to this wire are not serialised. The
 * {@linkplain #valueOut value writer} mixes each item into a rolling hash using
 * constants {@code K0}, {@code M0}, {@code M1}, {@code M2} and {@code M3}. An
 * instance is obtained from a {@linkplain ThreadLocal thread-local} and the hash
 * is cleared on retrieval.
 */
@SuppressWarnings({"rawtypes", "deprecation"})
public class HashWire implements WireOut, HexDumpBytesDescription {
    // Thread-local storage for HashWire instances.
    private static final ThreadLocal<HashWire> hwTL = new ThreadLocal<HashWire>() {
        @Override
        protected HashWire initialValue() {
            return new HashWire();
        }

        @Override
        public HashWire get() {
            HashWire hashWire = super.get();
            hashWire.hash = 0;
            return hashWire;
        }
    };

    // Internal constants used by the mixing algorithm
    private static final int K0 = 0x6d0f27bd;
    private static final int M0 = 0x5bc80bad;
    private static final int M1 = 0xea7585d7;
    private static final int M2 = 0x7a646e19;
    private static final int M3 = 0x855dd4db;

    // Value output for hashing.
    private final ValueOut valueOut = new HashValueOut();

    // Accumulated raw hash value
    long hash = 0;

    /**
     * Creates a hashing wire. Instances are normally obtained via the thread-local {@link #hwTL}.
     */
    public HashWire() {
    }

    /**
     * Computes a 64-bit hash for the provided {@link WriteMarshallable} value.
     *
     * @param value The {@link WriteMarshallable} value to be hashed.
     * @return The 64-bit hash value.
     */
    public static long hash64(WriteMarshallable value) {
        return hash64((Object) value);
    }

    /**
     * Computes a 64-bit hash for the provided object.
     *
     * @param value The object to be hashed.
     * @return The 64-bit hash value.
     */
    public static long hash64(Object value) {
        @NotNull HashWire hashWire = hwTL.get();
        hashWire.getValueOut().object(value);
        return hashWire.hash64();
    }

    /**
     * As {@link #hash64(WriteMarshallable)} but returning 32 bits.
     *
     * @param value value to hash
     * @return 32-bit hash
     */
    public static int hash32(WriteMarshallable value) {
        return hash32((Object) value);
    }

    /**
     * As {@link #hash64(Object)} but returning 32 bits.
     *
     * @param value value to hash
     * @return 32-bit hash
     */
    public static int hash32(Object value) {
        @NotNull HashWire hashWire = hwTL.get();
        hashWire.getValueOut().object(value);
        return hashWire.hash32();
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
        // Do nothing
    }

    /**
     * Always returns {@link ClassAliasPool#CLASS_ALIASES} as type
     * resolution is still required for consistent hashing.
     */
    @Override
    public ClassLookup classLookup() {
        return ClassAliasPool.CLASS_ALIASES;
    }

    /** Reset the accumulated {@link #hash}. */
    @Override
    public void clear() {
        hash = 0;
    }

    /**
     * Alias for {@link #clear()}.
     */
    @Override
    public void reset() {
        clear();
    }

    @Nullable
    @Override
    public Object parent() {
        return null;
    }

    @Override
    public void parent(Object parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void usePadding(boolean usePadding) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean usePadding() {
        return false;
    }

    /**
     * Computes and returns the 64-bit hash value.
     * <p>
     * This method uses the current hash value and applies an agitation function to provide
     * a consistent and dispersed 64-bit hash result.
     *
     * @return The 64-bit agitated hash value.
     */
    public long hash64() {
        return Maths.agitate(hash);
    }

    /**
     * Computes and returns the 32-bit hash value.
     * <p>
     * This method derives the 32-bit hash from the 64-bit hash value. The derived hash is
     * the result of XOR-ing the high and low 32-bits of the 64-bit hash.
     *
     * @return The derived 32-bit hash value.
     */
    public int hash32() {
        long h = hash64();
        return (int) (h ^ (h >>> 32));
    }

    /** Hashes an anonymous value field. */
    @NotNull
    @Override
    public ValueOut write() {
        hash += K0;
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull WireKey key) {
        return write(key.name());
    }

    /** Hashes the field name before returning {@link #valueOut}. */
    @NotNull
    @Override
    public ValueOut write(@NotNull CharSequence name) {
        hash += K0 + name.hashCode() * M0;
        return valueOut;
    }

    /** Hashes the event key and returns {@link #valueOut}. */
    @NotNull
    @Override
    public ValueOut writeEvent(Class<?> ignored, @NotNull Object eventKey) {
        hash += K0 + eventKey.hashCode() * M0;
        return valueOut;
    }

    @Override
    public void writeStartEvent() {
        // No nothing
    }

    @Override
    public void writeEndEvent() {
        // No nothing
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @NotNull
    @Override
    public ObjectOutput objectOutput() {
        return new WireObjectOutput(this);
    }

    /** Comments do not affect the hash. */
    @NotNull
    @Override
    public WireOut writeComment(CharSequence s) {
        return this;
    }

    /** Padding is ignored for hashing. */
    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        return this;
    }

    @NotNull
    @Override
    public WireOut headerNumber(long headerNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long headerNumber() {
        return 0;
    }

    @NotNull
    @Override
    public DocumentContext writingDocument(boolean metaData) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long enterHeader(final long safeLength) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateHeader(long position, boolean metaData, int expectedHeader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean writeFirstHeader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFirstHeader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFirstHeader(long headerLen) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean writeEndOfWire(long timeout, TimeUnit timeUnit, long lastPosition) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported as {@code HashWire} does not hold an output buffer.
     */
    @NotNull
    @Override
    public Bytes<?> bytes() {
        throw new UnsupportedOperationException();
    }

    /** Not applicable; returns {@code this} for chaining. */
    @Override
    public HexDumpBytesDescription<?> bytesComment() {
        return this;
    }

    /** Not supported. */
    @NotNull
    @Override
    public IntValue newIntReference() {
        throw new UnsupportedOperationException();
    }

    /** Not supported. */
    @NotNull
    @Override
    public LongValue newLongReference() {
        throw new UnsupportedOperationException();
    }

    /** Not supported. */
    @NotNull
    @Override
    public BooleanValue newBooleanReference() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean useSelfDescribingMessage(@NotNull CommonMarshallable object) {
        return object.usesSelfDescribingMessage();
    }

    @Override
    public boolean isBinary() {
        return true; // text wire is orders of magnitude slower
    }

    /** Not supported. */
    @NotNull
    @Override
    public LongArrayValues newLongArrayReference() {
        throw new UnsupportedOperationException();
    }

    /** Not supported. */
    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
        throw new UnsupportedOperationException();
    }

    /** Unsupported by {@code HashWire}. */
    @NotNull
    @Override
    public Pauser pauser() {
        throw new UnsupportedOperationException();
    }

    /** Not supported. */
    @Override
    public void pauser(Pauser pauser) {
        throw new UnsupportedOperationException();
    }

    /**
     * The {@code HashValueOut} class is responsible for updating the hash value of
     * the {@link HashWire} based on the type of value being processed.
     * <p>
     * The class implements the {@link ValueOut} interface, providing methods to handle
     * various data types like booleans, text, bytes, etc., and updating the hash
     * value accordingly.
         */
    class HashValueOut implements ValueOut {
        /** Mixes {@link #M2} or {@link #M3} depending on the flag. */
        @NotNull
        @Override
        public WireOut bool(Boolean flag) {
            hash = hash * M1 + (flag ? M2 : M3);
            return HashWire.this;
        }

        /** Mixes {@code Maths.hash64(s)} into the hash. */
        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            hash = hash * M1 + (s == null ? 0 : Maths.hash64(s));
            return HashWire.this;
        }

        /** Mixes the byte value. */
        @NotNull
        @Override
        public WireOut int8(byte i8) {
            hash = hash * M1 + i8 * M2;
            return HashWire.this;
        }

        /** Mixes the bytes content via {@code Maths.hash64}. */
        @NotNull
        @Override
        public WireOut bytes(@Nullable BytesStore<?, ?> fromBytes) {
            hash = hash * M1 + Maths.hash64(fromBytes);
            return HashWire.this;
        }

        /** Mixes the type name and bytes. */
        @NotNull
        @Override
        public WireOut bytes(@NotNull String type, @Nullable BytesStore<?, ?> fromBytes) {
            hash = hash * M1 + Maths.hash64(type) ^ Maths.hash64(fromBytes);
            return HashWire.this;
        }

        /** Mixes the raw bytes array. */
        @NotNull
        @Override
        public WireOut rawBytes(@NotNull byte[] value) {
            hash = hash * M1 + Maths.hash64(Bytes.wrapForRead(value));
            return HashWire.this;
        }

        /** Lengths are mixed with {@link #M3}. */
        @NotNull
        @Override
        public ValueOut writeLength(long remaining) {
            hash = hash * M1 + remaining * M3;
            return this;
        }

        /** Mixes the byte array content. */
        @NotNull
        @Override
        public WireOut bytes(@NotNull byte[] fromBytes) {
            hash = hash * M1 + Maths.hash64(Bytes.wrapForRead(fromBytes));
            return HashWire.this;
        }

        /** Mixes the type name and array content. */
        @NotNull
        @Override
        @Deprecated(/* to be removed in 2027 */)
        public WireOut bytes(@NotNull String type, @NotNull byte[] fromBytes) {
            hash = hash * M1 + Maths.hash64(type) ^ Maths.hash64(Bytes.wrapForRead(fromBytes));
            return HashWire.this;

        }

        /** Mixes the unsigned value. */
        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            hash = hash * M1 + u8 * M2;
            return HashWire.this;
        }

        /** Mixes the short value. */
        @NotNull
        @Override
        public WireOut int16(short i16) {
            hash = hash * M1 + i16 * M2;
            return HashWire.this;
        }

        /** Mixes the unsigned short value. */
        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            hash = hash * M1 + u16 * M2;
            return HashWire.this;
        }

        /** Mixes the code point. */
        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            hash = hash * M1 + codepoint * M2;
            return HashWire.this;
        }

        /** Mixes the int value. */
        @NotNull
        @Override
        public WireOut int32(int i32) {
            hash = hash * M1 + i32 * M2;
            return HashWire.this;
        }

        /** Mixes the unsigned int value. */
        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            hash = hash * M1 + u32 * M2;
            return HashWire.this;
        }

        /** Mixes the long value. */
        @NotNull
        @Override
        public WireOut int64(long i64) {
            hash = hash * M1 + i64 * M2;
            return HashWire.this;
        }

        /** Unsupported. */
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

        /** Hashes the capacity only. */
        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            hash = hash * M1 + capacity * M2;
            return HashWire.this;
        }

        /** Not supported. */
        @NotNull
        @Override
        public WireOut int64array(long capacity, LongArrayValues values) {
            throw new UnsupportedOperationException();
        }

        /** Mixes the float bits. */
        @NotNull
        @Override
        public WireOut float32(float f) {
            hash = hash * M1 + Float.floatToRawIntBits(f) * M2;
            return HashWire.this;
        }

        /** Mixes the double bits. */
        @NotNull
        @Override
        public WireOut float64(double d) {
            hash = hash * M1 + Double.doubleToRawLongBits(d) * M2;
            return HashWire.this;
        }

        /** Mixes the time's hash code. */
        @NotNull
        @Override
        public WireOut time(@NotNull LocalTime localTime) {
            hash = hash * M1 + localTime.hashCode() * M2;
            return HashWire.this;
        }

        /** Mixes the zonedDateTime hash code. */
        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            hash = hash * M1 + zonedDateTime.hashCode() * M2;
            return HashWire.this;
        }

        /** Mixes the date hash code. */
        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            hash = hash * M1 + localDate.hashCode() * M2;
            return HashWire.this;
        }

        /** Mixes the dateTime hash code. */
        @NotNull
        @Override
        public WireOut dateTime(@NotNull LocalDateTime localDateTime) {
            hash = hash * M1 + localDateTime.hashCode() * M2;
            return HashWire.this;
        }

        /** Mixes the type name before writing the object. */
        @NotNull
        @Override
        public ValueOut typePrefix(@NotNull CharSequence typeName) {
            hash = hash * M1 + typeName.hashCode() * M2;
            return this;
        }

        @Override
        public ClassLookup classLookup() {
            return HashWire.this.classLookup();
        }

        /** Mixes the literal type name. */
        @NotNull
        @Override
        public WireOut typeLiteral(@Nullable CharSequence type) {
            hash = hash * M1 + (type == null ? 0 : type.hashCode() * M2);
            return HashWire.this;
        }

        /** Mixes the literal class name. */
        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, @Nullable Class<?> type) {
            hash = hash * M1 + (type == null ? 0 : type.hashCode() * M2);
            return HashWire.this;
        }

        /** Mixes the UUID's hash code. */
        @NotNull
        @Override
        public WireOut uuid(@NotNull UUID uuid) {
            hash = hash * M1 + uuid.hashCode() * M2;
            return HashWire.this;
        }

        /** Unsupported. */
        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            throw new UnsupportedOperationException("todo");
        }

        /** Unsupported. */
        @NotNull
        @Override
        public WireOut int32forBinding(int value, @NotNull IntValue intValue) {
            throw new UnsupportedOperationException("todo");
        }

        /** Unsupported. */
        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            throw new UnsupportedOperationException("todo");
        }

        /** Unsupported. */
        @NotNull
        @Override
        public WireOut int64forBinding(long value, @NotNull LongValue longValue) {
            throw new UnsupportedOperationException("todo");
        }

        /** Unsupported. */
        @NotNull
        @Override
        public WireOut boolForBinding(final boolean value, @NotNull final BooleanValue longValue) {
            throw new UnsupportedOperationException("todo");
        }

        /** Writes each element in sequence for hashing. */
        @NotNull
        @Override
        public <T> WireOut sequence(T t, @NotNull BiConsumer<T, ValueOut> writer) {
            writer.accept(t, this);
            return HashWire.this;
        }

        /** Writes each element of the sequence for hashing. */
        @NotNull
        @Override
        public <T, K> WireOut sequence(T t, K kls, @NotNull TriConsumer<T, K, ValueOut> writer) throws InvalidMarshallableException {
            writer.accept(t, kls, this);
            return HashWire.this;
        }

        /** Delegates to the object to update the hash. */
        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) throws InvalidMarshallableException {
            object.writeMarshallable(HashWire.this);
            return HashWire.this;
        }

        /** Uses {@link Wires#writeMarshallable} for hashing. */
        @NotNull
        @Override
        public WireOut marshallable(@NotNull Serializable object) throws InvalidMarshallableException {
            Wires.writeMarshallable(object, HashWire.this);
            return HashWire.this;
        }

        /** Mixes the map's hash code. */
        @NotNull
        @Override
        public WireOut map(@NotNull Map map) {
            hash = hash * M1 + map.hashCode() * M2;
            return HashWire.this;
        }

        /** Returns the outer {@code HashWire}. */
        @NotNull
        @Override
        public WireOut wireOut() {
            return HashWire.this;
        }

        /** No-op. */
        @Override
        public void resetState() {
            // No nothing
        }

        /** No-op. */
        @Override
        public void elementSeparator() {
        }
    }
}
