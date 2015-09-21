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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.IORuntimeException;
import net.openhft.chronicle.bytes.StopCharTester;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * THis wire decodes URL query strings.
 */
public class QueryWire extends TextWire {
    final QueryValueOut valueOut = new QueryValueOut();
    final ValueIn valueIn = new QueryValueIn();

    public QueryWire(Bytes bytes) {
        super(bytes);
    }

    @NotNull
    @Override
    protected QueryValueOut createValueOut() {
        return new QueryValueOut();
    }

    @NotNull
    @Override
    protected TextValueIn createValueIn() {
        return new QueryValueIn();
    }

    @NotNull
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        consumeWhiteSpace();
        bytes.parseUTF(sb, QueryStopCharTesters.QUERY_FIELD_NAME);
        if (rewindAndRead() == '&')
            bytes.readSkip(-1);
        return sb;
    }

    @ForceInline
    void consumeWhiteSpace() {
        int codePoint = peekCode();
        while (Character.isWhitespace(codePoint)) {
            bytes.readSkip(1);
            codePoint = peekCode();
        }
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
        StringBuilder sb = readField(WireInternal.acquireStringBuilder());
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

    @NotNull
    @Override
    public LongValue newLongReference() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public BinaryLongArrayReference newLongArrayReference() {
        throw new UnsupportedOperationException();
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

    class QueryValueOut extends TextValueOut {
        @NotNull
        String sep = "";
        @Nullable
        CharSequence fieldName = null;

        void prependSeparator() {
            bytes.appendUtf8(sep);
            sep = "";
            if (fieldName != null) {
                bytes.appendUtf8(fieldName).appendUtf8('=');
                fieldName = null;
            }
        }

        @NotNull
        @Override
        public ValueOut leaf() {
            return this;
        }

        public void elementSeparator() {
            sep = "&";
        }

        @NotNull
        @Override
        public WireOut bool(@Nullable Boolean flag) {
            if (flag != null) {
                prependSeparator();
                bytes.appendUtf8(flag ? "true" : "false");
                elementSeparator();
            }
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            if (s != null) {
                prependSeparator();
                bytes.appendUtf8(s);
                elementSeparator();
            }
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            prependSeparator();
            bytes.appendUtf8(i8);
            elementSeparator();
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(@Nullable BytesStore fromBytes) {
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
        public WireOut bytes(byte[] byteArray) {
            prependSeparator();
            bytes.appendUtf8(Base64.getEncoder().encodeToString(byteArray));
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            prependSeparator();
            bytes.appendUtf8(u8);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            prependSeparator();
            bytes.appendUtf8(i16);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            prependSeparator();
            bytes.appendUtf8(u16);
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            prependSeparator();
            StringBuilder sb = WireInternal.acquireStringBuilder();
            sb.appendCodePoint(codepoint);
            text(sb);
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            prependSeparator();
            bytes.appendUtf8(i32);
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
        public WireOut int64array(long capacity, LongArrayValues values) {
            throw new UnsupportedOperationException("todo");
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
            bytes.appendUtf8(localTime.toString());
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            prependSeparator();
            bytes.appendUtf8(zonedDateTime.toString());
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            prependSeparator();
            bytes.appendUtf8(localDate.toString());
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public ValueOut typePrefix(@NotNull CharSequence typeName) {
            prependSeparator();
            bytes.appendUtf8(typeName);
            sep = " ";
            return this;
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
            bytes.appendUtf8(sep).appendUtf8(uuid.toString());
            elementSeparator();
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value, IntValue intValue) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value, LongValue longValue) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireOut sequence(@NotNull Consumer<ValueOut> writer) {
            prependSeparator();
            pushState();
            bytes.appendUtf8("[");
            sep = ",";
            long pos = bytes.writePosition();
            writer.accept(this);
            if (pos != bytes.writePosition())
                bytes.appendUtf8(",");

            popState();
            bytes.appendUtf8("]");
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
            bytes.appendUtf8("{");
            sep = ",";

            object.writeMarshallable(QueryWire.this);

            popState();

            bytes.appendUtf8('}');
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
            bytes.appendUtf8(sep).appendUtf8("\"\": ");
            sep = "";
            return this;
        }

        @NotNull
        public ValueOut write(@NotNull WireKey key) {
            fieldName = key.name();
            return this;
        }
    }

    class QueryValueIn extends TextValueIn {
        @Override
        public String text() {
            return StringUtils.toString(textTo(WireInternal.acquireStringBuilder()));
        }

        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder a) {
            consumeWhiteSpace();
            bytes.parseUTF(a, QueryStopCharTesters.QUERY_VALUE);
            return a;
        }

        @Nullable
        @Override
        public Bytes textTo(@NotNull Bytes a) {
            consumeWhiteSpace();
            bytes.parseUTF(a, QueryStopCharTesters.QUERY_VALUE);
            return a;
        }

        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer) {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            textTo(sb);
            classNameConsumer.accept(t, sb);
            return wireIn();
        }

        @Override
        public Class typeLiteral() {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            textTo(sb);
            try {
                return ClassAliasPool.CLASS_ALIASES.forName(sb);
            } catch (ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull Bytes toBytes) {
            return bytes(wi -> toBytes.write(wi.bytes()));
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
    }
}
