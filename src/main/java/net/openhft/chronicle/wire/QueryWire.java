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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.StopCharTester;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.IntArrayValues;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * This wire decodes URL query strings.
 */
@SuppressWarnings("rawtypes")
public class QueryWire extends TextWire {
    final QueryValueOut valueOut = new QueryValueOut();
    final QueryValueIn valueIn = new QueryValueIn();

    public QueryWire(@NotNull Bytes<?> bytes) {
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

    @Override
    @NotNull
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        consumePadding();
        bytes.parseUtf8(sb, QueryStopCharTesters.QUERY_FIELD_NAME);
        if (rewindAndRead() == '&')
            bytes.readSkip(-1);
        return sb;
    }

    @Override
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
    public ValueOut write(@NotNull CharSequence name) {
        return valueOut.write(name);
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @Override
    public @NotNull ValueIn getValueIn() {
        return valueIn;
    }

    @NotNull
    @Override
    public QueryWire writeComment(@NotNull CharSequence s) {
        return this;
    }

    @NotNull
    @Override
    public QueryWire addPadding(int paddingToAdd) {
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

    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
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

    class QueryValueOut extends YamlValueOut {
        @NotNull
        String sep = "";
        @Nullable
        CharSequence fieldName = null;

        @Override
        void prependSeparator() {
            bytes.appendUtf8(sep);
            sep = "";
            if (fieldName != null) {
                bytes.appendUtf8(fieldName).appendUtf8('=');
                fieldName = null;
            }
        }

        @Override
        public void elementSeparator() {
            sep = "&";
        }

        @NotNull
        @Override
        public QueryWire bool(@Nullable Boolean flag) {
            if (flag != null) {
                prependSeparator();
                bytes.appendUtf8(flag ? "true" : "false");
                elementSeparator();
            }
            return QueryWire.this;
        }

        @NotNull
        @Override
        public QueryWire text(@Nullable CharSequence s) {
            if (s != null) {
                prependSeparator();
                bytes.appendUtf8(s);
                elementSeparator();
            }
            return QueryWire.this;
        }

        @NotNull
        @Override
        public QueryWire int8(byte i8) {
            prependSeparator();
            bytes.appendUtf8(i8);
            elementSeparator();
            return QueryWire.this;
        }

        @NotNull
        @Override
        public QueryWire bytes(@Nullable BytesStore fromBytes) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public QueryWire rawBytes(@Nullable byte[] value) {
            if (value != null) {
                prependSeparator();
                bytes.write(value);
                elementSeparator();
            }
            return QueryWire.this;
        }

        @NotNull
        @Override
        public QueryWire bytes(byte[] byteArray) {
            prependSeparator();
            bytes.appendUtf8(Base64.getEncoder().encodeToString(byteArray));
            elementSeparator();

            return QueryWire.this;
        }

        @NotNull
        @Override
        public QueryWire int64array(long capacity) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public QueryWire int64array(long capacity, @NotNull LongArrayValues values) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public QueryValueOut typePrefix(@NotNull CharSequence typeName) {
            prependSeparator();
            bytes.appendUtf8(typeName);
            sep = " ";
            return this;
        }

        @NotNull
        @Override
        public QueryWire typeLiteral(@Nullable CharSequence type) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public QueryWire typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, @NotNull Class<?> type) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public QueryWire int32forBinding(int value) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public QueryWire int32forBinding(int value, @NotNull IntValue intValue) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public QueryWire int64forBinding(long value) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public QueryWire int64forBinding(long value, @NotNull LongValue longValue) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public <T> QueryWire sequence(T t, @NotNull BiConsumer<T, ValueOut> writer) {
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

        @NotNull
        @Override
        public <T, K> QueryWire sequence(T t, K kls, @NotNull TriConsumer<T, K, ValueOut> writer) throws InvalidMarshallableException {
            prependSeparator();
            pushState();
            bytes.appendUtf8("[");
            sep = ",";
            long pos = bytes.writePosition();
            writer.accept(t, kls, this);
            if (pos != bytes.writePosition())
                bytes.appendUtf8(",");

            popState();
            bytes.appendUtf8("]");
            elementSeparator();
            return QueryWire.this;
        }

        @Override
        protected void popState() {
        }

        @Override
        protected void pushState() {
        }

        @NotNull
        @Override
        public QueryWire marshallable(@NotNull WriteMarshallable object) throws InvalidMarshallableException {
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
        public QueryWire map(@NotNull final Map map) {
            throw new UnsupportedOperationException();
        }

        @Override
        @NotNull
        public QueryValueOut write() {
            throw new UnsupportedOperationException();
        }

        @Override
        @NotNull
        public QueryValueOut write(@NotNull WireKey key) {
            fieldName = key.name();
            return this;
        }

        @Override
        @NotNull
        public QueryValueOut write(@NotNull CharSequence name) {
            fieldName = name;
            return this;
        }
    }

    class QueryValueIn extends TextValueIn {
        @Override
        public String text() {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                return StringUtils.toString(textTo(stlSb.get()));
            }
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
        public Bytes<?> textTo(@NotNull Bytes<?> a) {
            consumePadding();
            bytes.parseUtf8(a, QueryStopCharTesters.QUERY_VALUE);
            return a;
        }

        @Override
        @NotNull
        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                StringBuilder sb = stlSb.get();
                textTo(sb);
                classNameConsumer.accept(t, sb);
            }
            return wireIn();
        }

        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                StringBuilder sb = stlSb.get();
                textTo(sb);
                return classLookup().forName(sb);
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
