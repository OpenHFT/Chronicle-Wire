/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.StopCharTester;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
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
 * Parses a URL query string such as {@code "a=1&b=2"}.
 * The bytes are treated as UTF-8 text and are not percent-decoded.
 * Extends {@link TextWire} and supplies {@link QueryValueIn} and
 * {@link QueryValueOut} for query specific value handling.
 */
@SuppressWarnings({"rawtypes", "java:S2387", "deprecation"})
public class QueryWire extends TextWire {

    // The specialized output handler for query string values.
    final QueryValueOut valueOut = new QueryValueOut();

    // The specialized input handler for query string values.
    final QueryValueIn valueIn = new QueryValueIn();

    /**
     * Creates a {@code QueryWire} over the supplied bytes which must contain a query
     * string, for example {@code "key=val&flag=true"}.
     *
     * @param bytes buffer holding the UTF-8 query text
     */
    public QueryWire(@NotNull Bytes<?> bytes) {
        super(bytes);
    }

    /**
     * Returns a writer that formats values as query parameters.
     */
    @NotNull
    @Override
    @Deprecated(/* to be removed in 2027 */)
    protected QueryValueOut createValueOut() {
        return new QueryValueOut();
    }

    /**
     * Returns a reader that parses query parameter values.
     */
    @NotNull
    @Override
    @Deprecated(/* to be removed in 2027 */)
    protected TextValueIn createValueIn() {
        return new QueryValueIn();
    }

    /**
     * Reads the next field name into {@code sb} stopping at '=' or '&amp;'.
     * If an '&amp;' has been consumed it is rewound.
     */
    @Override
    @NotNull
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        consumePadding();
        bytes.parseUtf8(sb, QueryStopCharTesters.QUERY_FIELD_NAME);
        if (rewindAndRead() == '&')
            bytes.readSkip(-1);
        return sb;
    }

    /**
     * Skips any leading whitespace from the current read position.
     */
    @Override
    public void consumePadding() {
        int codePoint = peekCode();
        while (Character.isWhitespace(codePoint)) {
            bytes.readSkip(1);
            codePoint = peekCode();
        }
    }

    /**
     * Query strings do not support unnamed writes.
     * Always throws UnsupportedOperationException.
     */
    @NotNull
    @Override
    public ValueOut write() {
        throw new UnsupportedOperationException();
    }

    /**
     * Starts a query parameter using the supplied key.
     * The value is written when a primitive writer is invoked.
     */
    @NotNull
    @Override
    public ValueOut write(@NotNull WireKey key) {
        return valueOut.write(key);
    }

    /**
     * Starts a query parameter using the supplied key.
     * The value is written when a primitive writer is invoked.
     */
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
     * Reads the byte immediately before the current read position.
     */
    int rewindAndRead() {
        return bytes.readUnsignedByte(bytes.readPosition() - 1);
    }

    /**
     * Unsupported for query strings.
     */
    @NotNull
    @Override
    public LongValue newLongReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported for query strings.
     */
    @NotNull
    @Override
    public IntValue newIntReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported for query strings.
     */
    @NotNull
    @Override
    public BinaryLongArrayReference newLongArrayReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported for query strings.
     */
    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Stop character testers used while parsing query strings.
     */
    enum QueryStopCharTesters implements StopCharTester {

        /**
         * Terminates a field name at '&' (delimiter), '=' (assignment) or end of input.
         */
        QUERY_FIELD_NAME {
            @Override
            public boolean isStopChar(int ch) throws IllegalStateException {
                return ch == '&' || ch == '=' || ch < 0;
            }
        },

        /**
         * Terminates a value at '&' (delimiter) or end of input.
         */
        QUERY_VALUE {
            @Override
            public boolean isStopChar(int ch) throws IllegalStateException {
                return ch == '&' || ch < 0;
            }
        }
    }

    /**
     * {@link ValueOut} implementation for query strings.
     * Builds fragments such as {@code &key=value}.
     */
    class QueryValueOut extends YamlValueOut {

        // The separator to prepend before writing the next value.
        @NotNull
        String separator = "";

        // The field name to prepend before writing the next value.
        @Nullable
        CharSequence fieldName = null;

        /**
         * Writes the current separator and pending field name.
         */
        @Override
        void prependSeparator() {
            bytes.appendUtf8(separator);
            separator = "";
            if (fieldName != null) {
                bytes.appendUtf8(fieldName).appendUtf8('=');
                fieldName = null;
            }
        }

        @Override
        public void elementSeparator() {
            separator = "&";
        }

        /**
         * Writes the flag if non-null, encoding it as {@code true} or {@code false}.
         */
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

        /**
         * Writes the text value if it is not {@code null}.
         */
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

        /**
         * Writes the byte value in decimal form.
         */
        @NotNull
        @Override
        public QueryWire int8(byte i8) {
            prependSeparator();
            bytes.appendUtf8(i8);
            elementSeparator();
            return QueryWire.this;
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire bytes(@Nullable BytesStore<?, ?> fromBytes) {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * Writes the raw bytes as-is.
         */
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

        /**
         * Writes the byte array as a Base64 string.
         */
        @NotNull
        @Override
        public QueryWire bytes(byte[] byteArray) {
            prependSeparator();
            bytes.appendUtf8(Base64.getEncoder().encodeToString(byteArray));
            elementSeparator();

            return QueryWire.this;
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire int64array(long capacity) {
            throw new UnsupportedOperationException();
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire int64array(long capacity, @NotNull LongArrayValues values) {
            throw new UnsupportedOperationException();
        }

        /**
         * Writes a type prefix followed by a space.
         */
        @NotNull
        @Override
        public QueryValueOut typePrefix(@NotNull CharSequence typeName) {
            prependSeparator();
            bytes.appendUtf8(typeName);
            separator = " ";
            return this;
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire typeLiteral(@Nullable CharSequence type) {
            throw new UnsupportedOperationException();
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, @NotNull Class<?> type) {
            throw new UnsupportedOperationException();
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire int32forBinding(int value) {
            throw new UnsupportedOperationException();
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire int32forBinding(int value, @NotNull IntValue intValue) {
            throw new UnsupportedOperationException();
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire int64forBinding(long value) {
            throw new UnsupportedOperationException();
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire int64forBinding(long value, @NotNull LongValue longValue) {
            throw new UnsupportedOperationException();
        }

        /**
         * Writes a sequence using comma separated values.
         */
        @NotNull
        @Override
        public <T> QueryWire sequence(T t, @NotNull BiConsumer<T, ValueOut> writer) {
            prependSeparator();
            pushState();
            bytes.appendUtf8("[");
            separator = ",";
            long pos = bytes.writePosition();
            writer.accept(t, this);
            if (pos != bytes.writePosition())
                bytes.appendUtf8(",");

            popState();
            bytes.appendUtf8("]");
            elementSeparator();
            return QueryWire.this;
        }

        /**
         * Writes a sequence with a provided class.
         */
        @NotNull
        @Override
        public <T, K> QueryWire sequence(T t, K kls, @NotNull TriConsumer<T, K, ValueOut> writer) throws InvalidMarshallableException {
            prependSeparator();
            pushState();
            bytes.appendUtf8("[");
            separator = ",";
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

        /**
         * Writes a marshallable object in braces.
         */
        @NotNull
        @Override
        public QueryWire marshallable(@NotNull WriteMarshallable object) throws InvalidMarshallableException {
            pushState();

            prependSeparator();
            bytes.appendUtf8("{");
            separator = ",";

            object.writeMarshallable(QueryWire.this);

            popState();

            bytes.appendUtf8('}');
            elementSeparator();
            return QueryWire.this;
        }

        /**
         * Unsupported for query strings.
         */
        @NotNull
        @Override
        public QueryWire map(@NotNull final Map map) {
            throw new UnsupportedOperationException();
        }

        /**
         * Unsupported for query strings.
         */
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
     * {@link ValueIn} implementation for query strings.
     */
    class QueryValueIn extends TextValueIn {
        /**
         * Returns the value text of the current parameter.
         */
        @Override
        public String text() {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                return StringUtils.toString(textTo(stlSb.get()));
            }
        }

        /**
         * Reads the value text up to '&' and appends it to {@code a}.
         */
        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder a) {
            consumePadding();
            bytes.parseUtf8(a, QueryStopCharTesters.QUERY_VALUE);
            return a;
        }

        /**
         * Reads the value text up to '&' and appends it to {@code a}.
         */
        @Nullable
        @Override
        public Bytes<?> textTo(@NotNull Bytes<?> a) {
            consumePadding();
            bytes.parseUtf8(a, QueryStopCharTesters.QUERY_VALUE);
            return a;
        }

        /**
         * Passes the value text to {@code classNameConsumer} as a type name.
         */
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

        /**
         * Returns the class for the value text using the wire's class lookup.
         */
        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                StringBuilder sb = stlSb.get();
                textTo(sb);
                return classLookup().forName(sb);
            }
        }

        /**
         * Checks for another item in a comma separated list.
         */
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
