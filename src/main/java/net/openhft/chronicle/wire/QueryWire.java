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
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.wire.util.BooleanConsumer;
import net.openhft.chronicle.wire.util.ByteConsumer;
import net.openhft.chronicle.wire.util.FloatConsumer;
import net.openhft.chronicle.wire.util.ShortConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

/**
 * Created by peter.lawrey on 15/01/15.
 */
public class QueryWire implements Wire, InternalWireIn {
    final Bytes<?> bytes;
    final TextValueOut valueOut = new TextValueOut();
    final ValueIn valueIn = new TextValueIn();

    boolean ready;

    public QueryWire(Bytes bytes) {
        this.bytes = bytes;
    }

    public static <ACS extends Appendable & CharSequence> void unescape(@NotNull ACS sb) {
        int end = 0;
        for (int i = 0; i < sb.length(); i++) {
            char ch = sb.charAt(i);
            if (ch == '%' && i < sb.length() - 1) {
                char ch3 = sb.charAt(++i);
                char ch4 = sb.charAt(++i);
                ch = (char) Integer.parseInt("" + ch3 + ch4, 16);
            }
            BytesUtil.setCharAt(sb, end++, ch);
        }
        BytesUtil.setLength(sb, end);
    }

    public String toString() {
        return bytes.toString();
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
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ValueIn read() {
        readField(Wires.acquireStringBuilder());
        return valueIn;
    }

    @NotNull
    private StringBuilder readField(@NotNull StringBuilder sb) {
        consumeWhiteSpace();
        bytes.parseUTF(sb, QueryStopCharTesters.QUERY_FIELD_NAME);
        if (rewindAndRead() == '&')
            bytes.readSkip(-1);
        return sb;
    }

    void consumeWhiteSpace() {
        int codePoint = peekCode();
        while (Character.isWhitespace(codePoint)) {
            bytes.readSkip(1);
            codePoint = peekCode();
        }
    }

    int peekCode() {
        return bytes.peekUnsignedByte();
    }

    /**
     * returns true if the next string is {@code str}
     *
     * @param source string
     * @return true if the strings are the same
     */
    private boolean peekStringIgnoreCase(@NotNull final String source) {
        if (source.isEmpty())
            return true;

        if (bytes.readRemaining() < 1)
            return false;

        long pos = bytes.readPosition();

        try {
            for (int i = 0; i < source.length(); i++) {
                if (Character.toLowerCase(source.charAt(i)) != Character.toLowerCase(bytes.readByte()))
                    return false;
            }
        } finally {
            bytes.readPosition(pos);
        }

        return true;
    }

    private int readCode() {
        if (bytes.readRemaining() < 1)
            return -1;
        return bytes.readUnsignedByte();
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        long position = bytes.readPosition();
        StringBuilder sb = readField(Wires.acquireStringBuilder());
        if (sb.length() == 0 || StringUtils.isEqual(sb, key.name()))
            return valueIn;
        bytes.readPosition(position);
        throw new UnsupportedOperationException("Unordered fields not supported yet. key=" + key
                .name() + ", was=" + sb + ", data='" + sb + "'");
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        consumeWhiteSpace();
        readField(name);
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        bytes.clear();
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    @Override
    public boolean hasMore() {
        return bytes.readRemaining() > 0;
    }

    @NotNull
    @Override
    public ValueOut write() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull WireKey key) {
        return valueOut.write(key);
    }

    @NotNull
    @Override
    public ValueOut writeValue() {
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
        return this;
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        return this;
    }

    int rewindAndRead() {
        return bytes.readUnsignedByte(bytes.readPosition() - 1);
    }

    enum QueryStopCharTesters implements StopCharTester {
        QUERY_FIELD_NAME {
            @Override
            public boolean isStopChar(int ch) throws IllegalStateException {
                return ch == '&' || ch == '=' || ch < 0;
            }
        },
        QUERY_VALUE {
            @Override
            public boolean isStopChar(int ch) throws IllegalStateException {
                return ch == '&' || ch < 0;
            }
        }
    }

    class TextValueOut implements ValueOut {
        @NotNull
        String sep = "";
        @Nullable
        CharSequence fieldName = null;

        void prependSeparator() {
            bytes.append(sep);
            sep = "";
            if (fieldName != null) {
                bytes.append(fieldName).append("=");
                fieldName = null;
            }
        }

        @NotNull
        @Override
        public ValueOut leaf() {
            return this;
        }

        @NotNull
        @Override
        public WireOut wireOut() {
            return QueryWire.this;
        }

        public void elementSeparator() {
            sep = "&";
        }

        @NotNull
        @Override
        public WireOut bool(@Nullable Boolean flag) {
            if (flag != null) {
                prependSeparator();
                bytes.append(flag ? "true" : "false");
                elementSeparator();
            }
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            if (s != null) {
                prependSeparator();
                bytes.append(s);
                elementSeparator();
            }
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            prependSeparator();
            bytes.append(i8);
            elementSeparator();
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(BytesStore fromBytes) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut rawBytes(@Nullable byte[] value) {
            if (value != null) {
                prependSeparator();
                bytes.write(value);
                elementSeparator();
            }
            return QueryWire.this;
        }

        private boolean isText(@NotNull Bytes fromBytes) {
            for (long i = fromBytes.readPosition(); i < fromBytes.readLimit(); i++) {
                int ch = fromBytes.readUnsignedByte(i);
                if ((ch < ' ' && ch != '\t') || ch == '&' || ch >= 127)
                    return false;
            }
            return true;
        }

        @NotNull
        @Override
        public ValueOut writeLength(long remaining) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireOut bytes(byte[] byteArray) {
            prependSeparator();
            bytes.append(Base64.getEncoder().encodeToString(byteArray));
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            prependSeparator();
            bytes.append(u8);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            prependSeparator();
            bytes.append(i16);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            prependSeparator();
            bytes.append(u16);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            prependSeparator();
            StringBuilder sb = Wires.acquireStringBuilder();
            sb.appendCodePoint(codepoint);
            text(sb);
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            prependSeparator();
            bytes.append(i32);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            prependSeparator();
            bytes.append(u32);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            prependSeparator();
            bytes.append(i64);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            TextLongArrayReference.write(bytes, capacity);
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut float32(float f) {
            prependSeparator();
            bytes.append(f);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
            prependSeparator();
            bytes.append(d);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut time(@NotNull LocalTime localTime) {
            prependSeparator();
            bytes.append(localTime.toString());
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            prependSeparator();
            bytes.append(zonedDateTime.toString());
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            prependSeparator();
            bytes.append(localDate.toString());
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut type(@NotNull CharSequence typeName) {
            prependSeparator();
            bytes.append(typeName);
            sep = " ";
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull CharSequence type) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @NotNull Class type) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut uuid(@NotNull UUID uuid) {
            prependSeparator();
            bytes.append(sep).append(uuid.toString());
            elementSeparator();
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            prependSeparator();
            IntTextReference.write(bytes, value);
            elementSeparator();
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            prependSeparator();
            TextLongReference.write(bytes, value);
            elementSeparator();
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut sequence(@NotNull Consumer<ValueOut> writer) {
            prependSeparator();
            pushState();
            bytes.append("[");
            sep = ",";
            long pos = bytes.writePosition();
            writer.accept(this);
            if (pos != bytes.writePosition())
                bytes.append(",");

            popState();
            bytes.append("]");
            elementSeparator();
            return QueryWire.this;
        }

        private void popState() {
        }

        private void pushState() {
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) {
            pushState();

            prependSeparator();
            bytes.append("{");
            sep = ",";

            object.writeMarshallable(QueryWire.this);

            popState();

            bytes.append('}');
            elementSeparator();
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut map(@NotNull final Map map) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        public ValueOut write() {
            bytes.append(sep).append("\"\": ");
            sep = "";
            return this;
        }

        @NotNull
        public ValueOut write(@NotNull WireKey key) {
            fieldName = key.name();
            return this;
        }
    }

    class TextValueIn implements ValueIn {
        @NotNull
        @Override
        public WireIn bool(@NotNull BooleanConsumer flag) {
            consumeWhiteSpace();

            StringBuilder sb = Wires.acquireStringBuilder();
            if (textTo(sb) == null) {
                flag.accept(null);
                return QueryWire.this;
            }

            flag.accept(StringUtils.isEqual(sb, "true"));
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn text(@NotNull Consumer<String> s) {
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            s.accept(Wires.INTERNER.intern(sb));
            return QueryWire.this;
        }

        @Override
        public String text() {
            return StringUtils.toString(textTo(Wires.acquireStringBuilder()));
        }

        @Nullable
        @Override
        public <ACS extends Appendable & CharSequence> ACS textTo(@NotNull ACS a) {
            consumeWhiteSpace();
            bytes.parseUTF(a, QueryStopCharTesters.QUERY_VALUE);
            return a;
        }

        @NotNull
        @Override
        public WireIn int8(@NotNull ByteConsumer i) {
            consumeWhiteSpace();
            i.accept((byte) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull Bytes toBytes) {
            return bytes(wi -> toBytes.write(wi.bytes()));
        }

        @NotNull
        public WireIn bytes(@NotNull Consumer<WireIn> bytesConsumer) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        public byte[] bytes() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return QueryWire.this;
        }

        @Override
        public long readLength() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn uint8(@NotNull ShortConsumer i) {
            consumeWhiteSpace();
            i.accept((short) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn int16(@NotNull ShortConsumer i) {
            consumeWhiteSpace();
            i.accept((short) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn uint16(@NotNull IntConsumer i) {
            consumeWhiteSpace();
            i.accept((int) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntConsumer i) {
            consumeWhiteSpace();
            i.accept((int) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn uint32(@NotNull LongConsumer i) {
            consumeWhiteSpace();
            i.accept(bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongConsumer i) {
            consumeWhiteSpace();
            i.accept(bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn float32(@NotNull FloatConsumer v) {
            consumeWhiteSpace();
            v.accept((float) bytes.parseDouble());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn float64(@NotNull DoubleConsumer v) {
            consumeWhiteSpace();
            v.accept(bytes.parseDouble());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn time(@NotNull Consumer<LocalTime> localTime) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            localTime.accept(LocalTime.parse(Wires.INTERNER.intern(sb)));
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            zonedDateTime.accept(ZonedDateTime.parse(Wires.INTERNER.intern(sb)));
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn date(@NotNull Consumer<LocalDate> localDate) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            localDate.accept(LocalDate.parse(Wires.INTERNER.intern(sb)));
            return QueryWire.this;
        }

        @Override
        public boolean hasNext() {
            return bytes.readRemaining() > 0;
        }

        @Override
        public boolean hasNextSequenceItem() {
            consumeWhiteSpace();
            int ch = peekCode();
            if (ch == ',') {
                bytes.readSkip(1);
                return true;
            }
            return ch != ']';
        }

        @NotNull
        @Override
        public WireIn uuid(@NotNull Consumer<UUID> uuid) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            uuid.accept(UUID.fromString(Wires.INTERNER.intern(sb)));
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn int64array(@Nullable LongArrayValues values, @NotNull Consumer<LongArrayValues> setter) {
            consumeWhiteSpace();
            if (!(values instanceof TextLongArrayReference)) {
                setter.accept(values = new TextLongArrayReference());
            }
            Byteable b = (Byteable) values;
            long length = TextLongArrayReference.peakLength(bytes, bytes.readPosition());
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(LongValue value, @NotNull Consumer<LongValue> setter) {
            consumeWhiteSpace();
            if (!(value instanceof TextLongReference)) {
                setter.accept(value = new TextLongReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumeWhiteSpace();
            if (peekCode() == ',')
                bytes.readSkip(1);
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(IntValue value, @NotNull Consumer<IntValue> setter) {
            if (!(value instanceof IntTextReference)) {
                setter.accept(value = new IntTextReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumeWhiteSpace();
            if (peekCode() == ',')
                bytes.readSkip(1);
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn sequence(@NotNull Consumer<ValueIn> reader) {
            consumeWhiteSpace();
            int code = readCode();
            if (code != '[')
                throw new IORuntimeException("Unsupported type " + (char) code + " (" + code + ")");

            reader.accept(QueryWire.this.valueIn);

            consumeWhiteSpace();
            code = peekCode();
            if (code != ']')
                throw new IORuntimeException("Expected a ] but got " + (char) code + " (" + code + ")");

            return QueryWire.this;
        }

        @NotNull
        @Override
        public <T> T applyToMarshallable(Function<WireIn, T> marshallableReader) {
            throw new UnsupportedOperationException("todo");
        }

        @Nullable
        @Override
        public <T extends ReadMarshallable> T typedMarshallable() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn type(@NotNull StringBuilder s) {
            consumeWhiteSpace();
            bytes.parseUTF(s, QueryStopCharTesters.QUERY_VALUE);
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn typeLiteralAsText(@NotNull Consumer<CharSequence> classNameConsumer) {
            StringBuilder sb = Wires.acquireStringBuilder();
            type(sb);
            classNameConsumer.accept(sb);
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public <K, V> Map<K, V> map(@NotNull final Class<K> kClazz,
                                    @NotNull final Class<V> vClass,
                                    @NotNull final Map<K, V> usingMap) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public <K extends ReadMarshallable, V extends ReadMarshallable> void typedMap(@NotNull Map<K, V> usingMap) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public boolean bool() {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            if (textTo(sb) == null)
                throw new NullPointerException("value is null");

            return StringUtils.isEqual(sb, "true");
        }

        public byte int8() {
            long l = int64();
            if (l > Byte.MAX_VALUE || l < Byte.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Byte.MAX_VALUE/MIN_VALUE");
            return (byte) l;
        }

        public short int16() {
            long l = int64();
            if (l > Short.MAX_VALUE || l < Short.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Short.MAX_VALUE/MIN_VALUE");
            return (short) l;
        }

        public int int32() {
            long l = int64();
            if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Integer.MAX_VALUE/MIN_VALUE");
            return (int) l;
        }

        public int uint16() {
            long l = int64();
            if (l > Integer.MAX_VALUE || l < 0)
                throw new IllegalStateException("value=" + l + ", is greater or less than Integer" +
                        ".MAX_VALUE/ZERO");
            return (int) l;
        }

        @Override
        public long int64() {
            consumeWhiteSpace();
            return bytes.parseLong();
        }

        @Override
        public double float64() {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public float float32() {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * @return true if !!null, if {@code true} reads the !!null up to the next STOP, if {@code
         * false} no  data is read  ( data is only peaked if {@code false} )
         */
        public boolean isNull() {
            consumeWhiteSpace();

            if (peekStringIgnoreCase("!!null ")) {
                bytes.readSkip("!!null ".length());
                // discard the text after it.
                textTo(Wires.acquireStringBuilder());
                return true;
            }

            return false;
        }

        @Override
        @Nullable
        public <E> E object(@Nullable E using,
                            @NotNull Class<E> clazz) {
            consumeWhiteSpace();

            if (isNull())
                return null;

            if (byte[].class.isAssignableFrom(clazz))
                return (E) bytes();

            if (Marshallable.class.isAssignableFrom(clazz)) {
                final E v;
                if (using == null)
                    v = OS.memory().allocateInstance(clazz);
                else
                    v = using;

                valueIn.marshallable((Marshallable) v);
                return v;

            } else if (StringBuilder.class.isAssignableFrom(clazz)) {
                StringBuilder builder = (using == null)
                        ? Wires.acquireStringBuilder()
                        : (StringBuilder) using;
                valueIn.textTo(builder);
                return using;

            } else if (CharSequence.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) valueIn.text();

            } else if (Long.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Long) valueIn.int64();

            } else if (Double.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Double) valueIn.float64();

            } else if (Integer.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Integer) valueIn.int32();

            } else if (Float.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Float) valueIn.float32();

            } else if (Short.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Short) valueIn.int16();

            } else if (Character.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                final String text = valueIn.text();
                if (text == null || text.length() == 0)
                    return null;
                return (E) (Character) text.charAt(0);

            } else if (Byte.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Byte) valueIn.int8();

            } else if (Map.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                final Map result = new HashMap();
                valueIn.map(result);
                return (E) result;

            } else {
                throw new IllegalStateException("unsupported type=" + clazz);
            }
        }
    }
}
