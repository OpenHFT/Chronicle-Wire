/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesComment;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.*;
import net.openhft.chronicle.threads.BusyPauser;
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

public class HashWire implements WireOut, BytesComment {
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
    private static final int K0 = 0x6d0f27bd;
    private static final int M0 = 0x5bc80bad;
    private static final int M1 = 0xea7585d7;
    private static final int M2 = 0x7a646e19;
    private static final int M3 = 0x855dd4db;
    private final ValueOut valueOut = new HashValueOut();
    long hash = 0;

    public static long hash64(WriteMarshallable value) {
        return hash64((Object) value);
    }

    public static long hash64(Object value) {
        @NotNull HashWire hashWire = hwTL.get();
        hashWire.getValueOut().object(value);
        return hashWire.hash64();
    }

    public static int hash32(WriteMarshallable value) {
        return hash32((Object) value);
    }

    public static int hash32(Object value) {
        @NotNull HashWire hashWire = hwTL.get();
        hashWire.getValueOut().object(value);
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
    public boolean startUse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean endUse() {
        throw new UnsupportedOperationException();
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
    public ValueOut write(@NotNull WireKey key) {
        return write(key.name());
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull CharSequence name) {
        hash += K0 + name.hashCode() * M0;
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut writeEvent(Class ignored, @NotNull Object eventKey) {
        hash += K0 + eventKey.hashCode() * M0;
        return valueOut;
    }

    @Override
    public void startEvent() {
    }

    @Override
    public void endEvent() {
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
    public long writeHeaderOfUnknownLength(final int safeLength, final long timeout, final TimeUnit timeUnit,
                                           @Nullable final LongValue lastPosition, final Sequence sequence) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long enterHeader(final int safeLength) {
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
    public void writeEndOfWire(long timeout, TimeUnit timeUnit, long lastPosition) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BytesComment<?> bytesComment() {
        return this;
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
    public BooleanValue newBooleanReference() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public LongArrayValues newLongArrayReference() {
        throw new UnsupportedOperationException();
    }

    @NotNull
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
            hash = hash * M1 + (s == null ? 0 : Maths.hash64(s));
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
        public WireOut bytes(@NotNull String type, @Nullable BytesStore fromBytes) {
            hash = hash * M1 + Maths.hash64(type) ^ Maths.hash64(fromBytes);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut rawBytes(@NotNull byte[] value) {
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
        public WireOut bytes(@NotNull byte[] fromBytes) {
            hash = hash * M1 + Maths.hash64(Bytes.wrapForRead(fromBytes));
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(@NotNull String type, @NotNull byte[] fromBytes) {
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
        public WireOut time(@NotNull LocalTime localTime) {
            hash = hash * M1 + localTime.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            hash = hash * M1 + zonedDateTime.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            hash = hash * M1 + localDate.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut dateTime(@NotNull LocalDateTime localDateTime) {
            hash = hash * M1 + localDateTime.hashCode() * M2;
            return HashWire.this;
        }

        @NotNull
        @Override
        public ValueOut typePrefix(@NotNull CharSequence typeName) {
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
        public WireOut uuid(@NotNull UUID uuid) {
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
        public WireOut boolForBinding(final boolean value, @NotNull final BooleanValue longValue) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public <T> WireOut sequence(T t, @NotNull BiConsumer<T, ValueOut> writer) {
            writer.accept(t, this);
            return HashWire.this;
        }

        @NotNull
        @Override
        public <T, K> WireOut sequence(T t, K kls, @NotNull TriConsumer<T, K, ValueOut> writer) {
            writer.accept(t, kls, this);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) {
            object.writeMarshallable(HashWire.this);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull Serializable object) {
            Wires.writeMarshallable(object, HashWire.this);
            return HashWire.this;
        }

        @NotNull
        @Override
        public WireOut map(@NotNull Map map) {
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
        public WireOut wireOut() {
            return HashWire.this;
        }

        @Override
        public void resetState() {

        }
    }
}
