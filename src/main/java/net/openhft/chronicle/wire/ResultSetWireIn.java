/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.BooleanValue;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.ObjectInput;
import java.io.StreamCorruptedException;
import java.nio.BufferUnderflowException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.*;

/*
 * Created by Peter Lawrey on 06/04/16.
 */
public class ResultSetWireIn implements WireIn, BytesComment {
    @NotNull
    private final ResultSet resultSet;
    private final ResultSetMetaData metaData;
    private final ValueIn valueIn = new RSValueIn();
    @Nullable
    private WireKey key;
    private int index = 0;
    private Object parent;

    public ResultSetWireIn(@NotNull ResultSet resultSet) throws SQLException {
        this.resultSet = resultSet;
        metaData = resultSet.getMetaData();
    }

    @Override
    public void clear() {
        key = null;
        index = 0;
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public long readEventNumber() {
        return Long.MIN_VALUE;
    }

    @NotNull
    @Override
    public ValueIn read() {
        key = null;
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        this.key = key;
        index++;
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        name.setLength(0);
        try {
            name.append(metaData.getColumnName(++index));
        } catch (SQLException e) {
            throw Jvm.rethrow(e);
        }
        return valueIn;
    }

    @Nullable
    @Override
    public <K> K readEvent(Class<K> expectedClass) {
        try {
            return ObjectUtils.convertTo(expectedClass, metaData.getColumnName(++index));
        } catch (SQLException e) {
            throw Jvm.rethrow(e);
        }
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    @NotNull
    @Override
    public WireIn readComment(@NotNull StringBuilder sb) {
        sb.setLength(0);
        return this;
    }

    @Override
    public boolean isEmpty() {
        try {
            return index >= metaData.getColumnCount();
        } catch (SQLException e) {
            throw Jvm.rethrow(e);
        }
    }

    @NotNull
    @Override
    public DocumentContext readingDocument() {
        throw new UnsupportedOperationException("TODO");
    }

    @NotNull
    @Override
    public DocumentContext readingDocument(long readLocation) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void consumePadding() {
    }

    @NotNull
    @Override
    public HeaderType readDataHeader(boolean includeMetaData) throws EOFException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readAndSetLength(long position) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFirstHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, StreamCorruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readMetaDataHeader() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public String readingPeekYaml() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ClassLookup classLookup() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pauser(Pauser pauser) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Pauser pauser() {
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
    public LongArrayValues newLongArrayReference() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object parent() {
        return parent;
    }

    @Override
    public void parent(Object parent) {
        this.parent = parent;
    }

    @Override
    public boolean startUse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean endUse() {
        throw new UnsupportedOperationException();
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
    public BooleanValue newBooleanReference() {
        throw new UnsupportedOperationException("todo");
    }

    @NotNull
    @Override
    public ObjectInput objectInput() {
        return new WireObjectInput(this);
    }

    class RSValueIn implements ValueIn {
        @Nullable
        @Override
        public String text() {
            try {
                return key == null ? resultSet.getString(index) : resultSet.getString(key.name().toString());
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder sb) {
            sb.setLength(0);
            sb.append(text());
            return sb;
        }

        @Nullable
        @Override
        public Bytes textTo(@NotNull Bytes bytes) {
            bytes.clear();
            bytes.appendUtf8(text());
            return bytes;
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull BytesOut bytes) {
            return bytes(bytes, false);
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull BytesOut toBytes, boolean clearBytes) {
            if (clearBytes)
                toBytes.clear();
            toBytes.appendUtf8(text());
            return wireIn();
        }

        @Nullable
        @Override
        public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireIn bytesMatch(@NotNull BytesStore compareBytes, BooleanConsumer consumer) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull ReadBytesMarshallable bytesMarshallable) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public byte[] bytes() {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return ResultSetWireIn.this;
        }

        @Override
        public long readLength() {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireIn skipValue() {
            return ResultSetWireIn.this;
        }

        @Override
        public boolean bool() {
            try {
                return key == null ? resultSet.getBoolean(index) : resultSet.getBoolean(key.name().toString());
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Override
        public byte int8() {
            try {
                return Maths.toInt8(key == null ? resultSet.getInt(index) : resultSet.getInt(key.name().toString()));
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Override
        public short int16() {
            try {
                return Maths.toInt16(key == null ? resultSet.getInt(index) : resultSet.getInt(key.name().toString()));
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Override
        public int uint16() {
            try {
                return Maths.toUInt16(key == null ? resultSet.getInt(index) : resultSet.getInt(key.name().toString()));
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Override
        public int int32() {
            try {
                return key == null ? resultSet.getInt(index) : resultSet.getInt(key.name().toString());
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Override
        public long int64() {
            try {
                return key == null ? resultSet.getLong(index) : resultSet.getLong(key.name().toString());
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Override
        public double float64() {
            try {
                return key == null ? resultSet.getDouble(index) : resultSet.getDouble(key.name().toString());
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Override
        public float float32() {
            try {
                return key == null ? resultSet.getFloat(index) : resultSet.getFloat(key.name().toString());
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @NotNull
        @Override
        public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag) {
            tFlag.accept(t, bool());
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
            tb.accept(t, int8());
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            try {
                int b = key == null ? resultSet.getInt(index) : resultSet.getInt(key.name().toString());
                ti.accept(t, Maths.toUInt8(b));
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            ti.accept(t, int16());
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            ti.accept(t, uint16());
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            ti.accept(t, int32());
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            try {
                long b = key == null ? resultSet.getLong(index) : resultSet.getLong(key.name().toString());
                tl.accept(t, Maths.toUInt32(b));
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            tl.accept(t, int64());
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            tf.accept(t, float32());
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            td.accept(t, float64());
            return wireIn();
        }

        @NotNull
        @Override
        public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNextSequenceItem() {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongValue value) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntValue value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn bool(@NotNull final BooleanValue ret) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> T applyToMarshallable(Function<WireIn, T> marshallableReader) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public <T> T typedMarshallable() throws IORuntimeException {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer) throws IORuntimeException, BufferUnderflowException {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public Object marshallable(@NotNull Object object, SerializationStrategy strategy) throws BufferUnderflowException, IORuntimeException {
            throw new UnsupportedOperationException();
        }

        @Override
        public <K extends ReadMarshallable, V extends ReadMarshallable> void typedMap(@NotNull Map<K, V> usingMap) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public <K, V> Map<K, V> map(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> Class<T> typeLiteral() throws IORuntimeException, BufferUnderflowException {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public BracketType getBracketType() {
            return BracketType.NONE;
        }

        @Override
        public boolean isNull() {
            try {
                return resultSet.wasNull();
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Override
        public Object objectWithInferredType(Object using, SerializationStrategy hint, Class type) {
            try {
                return key == null ? resultSet.getObject(index) : resultSet.getObject(key.name().toString());
            } catch (SQLException e) {
                throw Jvm.rethrow(e);
            }
        }

        @Override
        public boolean isTyped() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class typePrefix() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void resetState() {
            clear();
        }
    }
}
