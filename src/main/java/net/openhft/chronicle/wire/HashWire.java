/*
 *
 *  *     Copyright (C) ${YEAR}  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.threads.BusyPauser;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.StreamCorruptedException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * Created by peter.lawrey on 05/02/2016.
 */
public class HashWire implements WireOut {
    private static final int K0 = 0x6d0f27bd;
    private static final int M0 = 0x5bc80bad;
    private static final int M1 = 0xea7585d7;
    private static final int M2 = 0x7a646e19;
    private static final int M3 = 0x855dd4db;
    private final ValueOut valueOut = new HashValueOut();
    long hash = 0;

    public static long hash64(WriteMarshallable value) {
        HashWire hashWire = new HashWire();
        hashWire.getValueOut().marshallable(value);
        return hashWire.hash64();
    }

    public static int hash32(WriteMarshallable value) {
        HashWire hashWire = new HashWire();
        hashWire.getValueOut().marshallable(value);
        return hashWire.hash32();
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
    }

    @Override
    public ClassLookup classLookup() {
        return ClassAliasPool.CLASS_ALIASES;
    }

    @Override
    public void clear() {
        hash = 0;
    }

    public long hash64() {
        return Maths.agitate(hash);
    }

    public int hash32() {
        long h = hash64();
        return (int) (h ^ (h >>> 32));
    }

    @NotNull
    @Override
    public ValueOut write() {
        hash += K0;
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut write(WireKey key) {
        hash += K0 + key.name().hashCode() * M0;
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @NotNull
    @Override
    public WireOut writeComment(CharSequence s) {
        return this;
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        return this;
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public long writeHeader(int length, long timeout, TimeUnit timeUnit) throws TimeoutException, EOFException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateHeader(int length, long position, boolean metaData) throws StreamCorruptedException {
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
    public void writeEndOfWire(long timeout, TimeUnit timeUnit) throws TimeoutException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public LongArrayValues newLongArrayReference() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pauser pauser() {
        return BusyPauser.INSTANCE;
    }

    @Override
    public void pauser(Pauser pauser) {
        throw new UnsupportedOperationException();
    }

    class HashValueOut implements ValueOut {
        @NotNull
        @Override
        public WireOut bool(Boolean flag) {
            hash = hash * M1 + (flag ? M2 : M3);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            hash = hash * M1 + Maths.hash64(s);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            hash = hash * M1 + i8 * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(@Nullable BytesStore fromBytes) {
            hash = hash * M1 + Maths.hash64(fromBytes);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(String type, @Nullable BytesStore fromBytes) {
            hash = hash * M1 + Maths.hash64(type) ^ Maths.hash64(fromBytes);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut rawBytes(byte[] value) {
            hash = hash * M1 + Maths.hash64(Bytes.wrapForRead(value));
            return HashWire.this;
        }

        @NotNull
        @Override
        public ValueOut writeLength(long remaining) {
            hash = hash * M1 + remaining * M3;
            return this;
        }

        @NotNull
        @Override
        public WireOut bytes(byte[] fromBytes) {
            hash = hash * M1 + Maths.hash64(Bytes.wrapForRead(fromBytes));
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(String type, byte[] fromBytes) {
            hash = hash * M1 + Maths.hash64(type) ^ Maths.hash64(Bytes.wrapForRead(fromBytes));
            return HashWire.this;

        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            hash = hash * M1 + u8 * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            hash = hash * M1 + i16 * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            hash = hash * M1 + u16 * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            hash = hash * M1 + codepoint * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            hash = hash * M1 + i32 * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            hash = hash * M1 + u32 * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            hash = hash * M1 + i64 * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            hash = hash * M1 + capacity * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity, LongArrayValues values) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireOut float32(float f) {
            hash = hash * M1 + Float.floatToRawIntBits(f) * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
            hash = hash * M1 + Double.doubleToRawLongBits(d) * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut time(LocalTime localTime) {
            hash = hash * M1 + localTime.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(ZonedDateTime zonedDateTime) {
            hash = hash * M1 + zonedDateTime.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut date(LocalDate localDate) {
            hash = hash * M1 + localDate.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public ValueOut typePrefix(CharSequence typeName) {
            hash = hash * M1 + typeName.hashCode() * M2;
            return this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull CharSequence type) {
            hash = hash * M1 + type.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @NotNull Class type) {
            hash = hash * M1 + type.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut uuid(UUID uuid) {
            hash = hash * M1 + uuid.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value, IntValue intValue) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value, LongValue longValue) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public <T> WireOut sequence(T t, BiConsumer<T, ValueOut> writer) {
            writer.accept(t, this);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(WriteMarshallable object) {
            object.writeMarshallable(HashWire.this);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut map(Map map) {
            hash = hash * M1 + map.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            hash = hash * M1 + map.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public ValueOut leaf() {
            return this;
        }

        @NotNull
        @Override
        public WireOut wireOut() {
            return HashWire.this;
        }
    }
}
