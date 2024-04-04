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
 * This class represents a wire format designed to decode URL query strings.
 * It extends the TextWire class and provides custom implementations for reading fields and consuming padding
 * in the context of URL query strings. Specialized value input and output handlers, namely {@code QueryValueIn}
 * and {@code QueryValueOut}, are used to manage the specific intricacies of the query string format.
 */
@SuppressWarnings("rawtypes")
public class QueryWire extends TextWire {

    // The specialized output handler for query string values.
    final QueryValueOut valueOut = new QueryValueOut();

    // The specialized input handler for query string values.
    final QueryValueIn valueIn = new QueryValueIn();

    /**
     * Constructs a new instance of {@code QueryWire} using the provided bytes.
     *
     * @param bytes The bytes that represent the query string to be processed.
     */
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

    /**
     * Reads an unsigned byte at the current position minus one.
     *
     * @return The unsigned byte value at the position one byte before the current read position.
     */
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

    /**
     * An enumeration defining testers for stopping characters when parsing fields and values
     * in the context of URL query strings.
     */
    enum QueryStopCharTesters implements StopCharTester {

        /**
         * Tester for identifying stopping characters when parsing a field name in a query string.
         * The method returns true if the character represents an '&' (delimiter) or '=' (assignment)
         * or if the character is a negative value (end of input).
         */
        QUERY_FIELD_NAME {
            @Override
            public boolean isStopChar(int ch) throws IllegalStateException {
                return ch == '&' || ch == '=' || ch < 0;
            }
        },

        /**
         * Tester for identifying stopping characters when parsing a value in a query string.
         * The method returns true if the character represents an '&' (delimiter)
         * or if the character is a negative value (end of input).
         */
        QUERY_VALUE {
            @Override
            public boolean isStopChar(int ch) throws IllegalStateException {
                return ch == '&' || ch < 0;
            }
        }
    }

    /**
     * Represents an output handler specialized for writing values in the context of URL query strings.
     * This class extends the {@code YamlValueOut} and provides custom implementations for
     * appending values to the wire with appropriate separators and field assignments.
     */
    class QueryValueOut extends YamlValueOut {

        // The separator to prepend before writing the next value.
        @NotNull
        String sep = "";

        // The field name to prepend before writing the next value.
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

    /**
     * Represents an input handler specialized for reading values in the context of URL query strings.
     * This class extends the {@code TextValueIn} and provides custom implementations for
     * extracting text values from the wire with appropriate handling of separators and encodings.
     */
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
