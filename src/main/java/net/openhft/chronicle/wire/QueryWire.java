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
import net.openhft.chronicle.bytes.StopCharTester;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.Map;
import java.util.function.BiConsumer;

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
        consumePadding();
        bytes.parseUtf8(sb, QueryStopCharTesters.QUERY_FIELD_NAME);
        if (rewindAndRead() == '&')
            bytes.readSkip(-1);
        return sb;
    }

    @ForceInline
    public void consumePadding() {
        int codePoint = peekCode();
        while (Character.isWhitespace(codePoint)) {
            bytes.readSkip(1);
            codePoint = peekCode();
        }
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
        public WireOut int64array(long capacity) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity, LongArrayValues values) {
            throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @NotNull Class type) {
            throw new UnsupportedOperationException();
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
        public <T> WireOut sequence(T t, BiConsumer<T, ValueOut> writer) {
            prependSeparator();
            pushState();
            bytes.appendUtf8("[");
            sep = ",";
            long pos = bytes.writePosition();
            writer.accept(t, this);
            if (pos != bytes.writePosition())
                bytes.appendUtf8(",");

            popState();
            bytes.appendUtf8("]");
            elementSeparator();
            return QueryWire.this;
        }

        protected void popState() {
        }

        protected void pushState() {
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
            throw new UnsupportedOperationException();
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
            consumePadding();
            bytes.parseUtf8(a, QueryStopCharTesters.QUERY_VALUE);
            return a;
        }

        @Nullable
        @Override
        public Bytes textTo(@NotNull Bytes a) {
            consumePadding();
            bytes.parseUtf8(a, QueryStopCharTesters.QUERY_VALUE);
            return a;
        }

        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer) {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            textTo(sb);
            classNameConsumer.accept(t, sb);
            return wireIn();
        }

        @Override
        public <T> Class<T> typeLiteral() {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            textTo(sb);
            try {
                return ClassAliasPool.CLASS_ALIASES.forName(sb);
            } catch (ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
        }

        @Override
        public boolean hasNextSequenceItem() {
            consumePadding();
            int ch = peekCode();
            if (ch == ',') {
                bytes.readSkip(1);
                return true;
            }
            return ch != ']';
        }
    }
}
