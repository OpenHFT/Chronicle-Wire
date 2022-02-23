/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.function.BiFunction;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

/**
 * JSON wire format
 * <p>
 * At the moment, this is a cut down version of the YAML wire format.
 */
public class JSONWire extends TextWire {
    @SuppressWarnings("rawtypes")
    static final BytesStore COMMA = BytesStore.from(",");
    boolean useTypes;

    @SuppressWarnings("rawtypes")
    public JSONWire() {
        this(Bytes.allocateElasticOnHeap());
    }

    public JSONWire(@NotNull Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
        trimFirstCurly(false);
    }

    @Override
    protected Class defaultKeyClass() {
        return String.class;
    }

    @SuppressWarnings("rawtypes")
    public JSONWire(@NotNull Bytes bytes) {
        this(bytes, false);
    }

    @NotNull
    public static JSONWire from(@NotNull String text) {
        return new JSONWire(Bytes.from(text));
    }

    public static String asText(@NotNull Wire wire) {
        long pos = wire.bytes().readPosition();
        @NotNull JSONWire tw = new JSONWire(nativeBytes());
        wire.copyTo(tw);
        wire.bytes().readPosition(pos);

        return tw.toString();
    }

    static boolean isWrapper(Class<?> type) {
        return type == Integer.class || type == Long.class || type == Float.class ||
                type == Double.class || type == Short.class || type == Character.class ||
                type == Byte.class || type == Boolean.class || type == Void.class;
    }

    public JSONWire useTypes(boolean outputTypes) {
        this.useTypes = outputTypes;
        return this;
    }

    public boolean useTypes() {
        return useTypes;
    }

    @NotNull
    @Override
    protected TextValueOut createValueOut() {
        return new JSONValueOut();
    }

    @NotNull
    @Override
    protected TextValueIn createValueIn() {
        return new JSONValueIn() {

            @Override
            public double float64() {
                consumePadding();
                valueIn.skipType();
                switch (peekCode()) {
                    case '[':
                    case '{':
                        Jvm.warn().on(getClass(), "Unable to read " + valueIn.object() + " as a double.");
                        return 0;
                }

                boolean isNull;

                long l = bytes.readLimit();
                try {
                    bytes.readLimit(bytes.readPosition() + 4);
                    isNull = "null".contentEquals(bytes);
                } finally {
                    bytes.readLimit(l);
                }

                if (isNull) {
                    bytes.readSkip("null".length());
                    consumePadding();
                }

                final double v = isNull ? Double.NaN : bytes.parseDouble();
                checkRewind();
                return v;
            }

            @Override
            public void checkRewind() {
                int ch = bytes.readUnsignedByte(bytes.readPosition() - 1);
                if (ch == ':' || ch == '}' || ch == ']')
                    bytes.readSkip(-1);

                    // !='l' to handle 'null' in JSON wire
                else if (ch != 'l' && (ch > 'F' && (ch < 'a' || ch > 'f'))) {
                    throw new IllegalArgumentException("Unexpected character in number '" + (char) ch + '\'');
                }
            }
        };
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    protected Quotes needsQuotesEscaped(@NotNull CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"' || ch < ' ')
                return Quotes.DOUBLE;
        }
        return Quotes.NONE;
    }

    @Override
    void escape(@NotNull CharSequence s) {
        bytes.writeUnsignedByte('"');
        if (needsQuotesEscaped(s) == Quotes.NONE) {
            bytes.appendUtf8(s);
        } else {
            escape0(s, Quotes.DOUBLE);
        }
        bytes.writeUnsignedByte('"');
    }

    @Override
    public ValueOut writeEvent(Class expectedType, Object eventKey) {
        return super.writeEvent(String.class, "" + eventKey);
    }

    @NotNull
    @Override
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        consumePadding();
        int code = peekCode();
        if (code == '}') {
            sb.setLength(0);
            return sb;
        }
        if (code == '{') {
            if (valueIn.stack.level > 0)
                throw new IORuntimeException("Expected field name, but got { at " + bytes.toDebugString(64));
            valueIn.pushState();
            bytes.readSkip(1);
        }
        return super.readField(sb);
    }

    @Override
    @NotNull
    protected TextStopCharsTesters strictEndOfText() {
        return TextStopCharsTesters.STRICT_END_OF_TEXT_JSON;
    }

    class JSONValueOut extends TextValueOut {
        @NotNull
        @Override
        public String nullOut() {
            return "null";
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@Nullable CharSequence type) {
            return text(type);
        }

        @NotNull
        @Override
        public ValueOut typePrefix(@NotNull CharSequence typeName) {
            if (useTypes) {
                startBlock('{');
                bytes.append("\"@");
                bytes.append(typeName);
                bytes.append("\":");
            }
            return this;
        }

        @Override
        public void endTypePrefix() {
            super.endTypePrefix();
            if (useTypes) {
                endBlock(true, '}');
            }
        }

        @Override
        public void elementSeparator() {
            sep = COMMA;
        }

        @Override
        protected void asTestQuoted(String s, Quotes quotes) {
            bytes.append('"');
            escape0(s, quotes);
            bytes.append('"');
        }

        @Override
        protected void popState() {
        }

        @Override
        protected void pushState() {
            leaf = true;
        }

        @Override
        protected void afterOpen() {
            sep = EMPTY;
        }

        @Override
        protected void afterClose() {

        }

        @Override
        protected void addNewLine(long pos) {
        }

        @Override
        protected void newLine() {
        }

        @Override
        protected void endField() {
            sep = COMMA;
        }

        @Override
        protected void fieldValueSeperator() {
            bytes.writeUnsignedByte(':');
        }

        @Override
        public void writeComment(@NotNull CharSequence s) {
        }

        @Override
        protected String doubleToString(double d) {
            return Double.isNaN(d) ? "null" : super.doubleToString(d);
        }

        @Override
        protected String floatToString(float f) {
            return Float.isNaN(f) ? "null" : super.floatToString(f);
        }

        @NotNull
        @Override
        public WireOut rawText(CharSequence value) {
            bytes.writeByte((byte) '\"');
            WireOut wireOut = super.rawText(value);
            bytes.writeByte((byte) '\"');
            return wireOut;
        }

        @Override
        public @NotNull WireOut date(LocalDate localDate) {
            return text(localDate.toString());
        }

        @Override
        public @NotNull WireOut dateTime(LocalDateTime localDateTime) {
            return text(localDateTime.toString());
        }

        @Override
        public @NotNull <V> WireOut object(@NotNull Class<V> expectedType, V v) {
            return useTypes ? super.object(v) : super.object(expectedType, v);
        }

        @Override
        public @NotNull ValueOut typePrefix(Class type) {
            if (type.isPrimitive() || isWrapper(type) || type.isEnum()) {
                // Do nothing because there are no other alternatives
                // and thus, the type is implicitly given in the declaration.
                return this;
            } else {
                return super.typePrefix(type);
            }
        }

        @Override
        public @NotNull <K, V> WireOut marshallable(@Nullable Map<K, V> map, @NotNull Class<K> kClass, @NotNull Class<V> vClass, boolean leaf) {
            return super.marshallable(map, (Class) String.class, vClass, leaf);
        }


        public @NotNull WireOut time(final LocalTime localTime) {
            // Todo: fix quoted text
            return super.time(localTime);
            /*return text(localTime.toString());*/
        }
    }

    class JSONValueIn extends TextValueIn {
        /**
         * @return true if !!null "", if {@code true} reads the !!null "" up to the next STOP, if
         * {@code false} no  data is read  ( data is only peaked if {@code false} )
         */
        @Override
        public boolean isNull() {
            consumePadding();

            if (peekStringIgnoreCase("null")) {
                bytes.readSkip(4);
                // Skip to the next token, consuming any padding and/or a comma
                consumePadding(1);

                // discard the text after it.
                //  text(acquireStringBuilder());
                return true;
            }

            return false;
        }

        @Override
        public String text() {
            @Nullable String text = super.text();
            return text == null || text.equals("null") ? null : text;
        }

        @Override
        protected boolean isASeparator(int nextChar) {
            return true;
        }

        @Override
        public @Nullable Object object() {
            return useTypes ? parseType() : super.object();
        }

        @Override
        public <E> @Nullable E object(@NotNull Class<E> clazz) {
            return useTypes ? parseType(null, clazz) : super.object(clazz);
        }

        @Override
        public <E> E object(@Nullable E using, @Nullable Class clazz) {
            return useTypes ? parseType(using, clazz) : super.object(using, clazz);
        }


        @Override
        public Class typePrefix() {
            return super.typePrefix();
        }

        @Override
        public Object typePrefixOrObject(Class tClass) {
            return super.typePrefixOrObject(tClass);
        }

        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            text(stringBuilder);
            try {
                return classLookup().forName(stringBuilder);
            } catch (ClassNotFoundRuntimeException e) {
                return unresolvedHandler.apply(stringBuilder, e.getCause());
            }
        }

        @Override
        public @Nullable Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy) throws BufferUnderflowException, IORuntimeException {
            return super.marshallable(object, strategy);
        }

        @Override
        public boolean isTyped() {
            // Either we use types for sure or we might use types...
            return useTypes || super.isTyped();
        }

        private Object parseType() {
            if (!hasTypeDefinition()) {
                return super.object();
            } else {
                final StringBuilder sb = Wires.acquireStringBuilder();
                sb.setLength(0);
                this.wireIn().read(sb);
                final Class<?> clazz = classLookup().forName(sb.subSequence(1, sb.length()));
                return parseType(null, clazz);
            }

/*
            consumePadding();
            final char openingBracket = bytes.readChar();
            assert openingBracket == '{';
            consumePadding();
            final char openingQuote = bytes.readChar();
            assert openingQuote == '"';
            final Class<?> typePrefix = typePrefix();
            consumePadding();
            final char closingQuote = bytes.readChar();
            assert closingQuote == '"'; */


            /*
            final StringBuilder sb = Wires.acquireStringBuilder();
            sb.setLength(0);
            this.wireIn().read(sb);
            try {
                final Bytes<?> bytes2 = wireIn().bytes();
                assert sb.charAt(0) == '@' : "Did not start with an @ character: " + bytes;
                final Class<?> clazz = Class.forName(sb.substring(1));
                this.wireIn().consumePadding();

                final long writePos = bytes.writePosition();
                bytes.writePosition(bytes.readPosition());
                bytes.writeChar('!');
                bytes.writePosition(writePos);

                // Skip opening bracket
                // this.wireIn().bytes().readChar();

                final Object object = objectWithInferredType(null, SerializationStrategies.ANY_NESTED, clazz);
                //final Object object = object(clazz);

                // this.wireIn().consumePadding();
                // Skip closing bracket (todo: assert })
                // this.wireIn().bytes().readChar();

                return object;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } */
        }

        private <E> E parseType(@Nullable E using, @NotNull Class clazz) {

            if (!hasTypeDefinition()) {
                return super.object(using, clazz);
            } else {
                final StringBuilder sb = Wires.acquireStringBuilder();
                sb.setLength(0);
                readTypeDefinition(sb);
                final Class<?> overrideClass = classLookup().forName(sb.subSequence(1, sb.length()));
                if (!clazz.isAssignableFrom(overrideClass))
                    throw new ClassCastException("Unable to cast " + overrideClass.getName() + " to " + clazz.getName());
                if (using != null && !overrideClass.isInstance(using))
                    throw new ClassCastException("Unable to reuse a " + using.getClass().getName() + " as a " + overrideClass.getName());
                final E result = super.object(using, overrideClass);

                // remove the closing bracket from the type definition
                consumePadding();
                final char endBracket = bytes.readChar();
                assert endBracket == '}' : "Missing end bracket }, got " + endBracket + " from " + bytes;
                consumePadding(1);

                return result;
            }
        }

        boolean hasTypeDefinition() {
            final long readPos = bytes.readPosition();
            try {
                // Match {"@ with any padding in between
                consumePadding();
                if (bytes.readChar() != '{')
                    return false;
                consumePadding();
                if (bytes.readChar() != '"')
                    return false;
                consumePadding();
                return bytes.readChar() == '@';
            } finally {
                bytes.readPosition(readPos);
            }
        }

        void readTypeDefinition(StringBuilder sb) {
            consumePadding();
            if (bytes.readChar() != '{')
                throw new IORuntimeException("Expected { but got " + bytes);
            consumePadding();
            text(sb);
            consumePadding();
            final char colon = bytes.readChar();
            assert colon == ':' : "Expected : but got " + colon;

        }

        public boolean useTypes() {
            return useTypes;
        }

    }

/*
    final class MapMarshaller<K, V> implements WriteMarshallable {
        private Map<K, V> map;
        private Class<K> kClass;
        private Class<V> vClass;
        private boolean leaf;

        void params(@Nullable Map<K, V> map, @NotNull Class<K> kClass, @NotNull Class<V> vClass, boolean leaf) {
            this.map = map;
            this.kClass = kClass;
            this.vClass = vClass;
            this.leaf = leaf;
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            for (@NotNull Map.Entry<K, V> entry : map.entrySet()) {
                final K key = entry.getKey();

                Bytes bytes = null;
                bytes.app



//                 StringUtils

                ValueOut valueOut = wire.write()writeEvent(kClass, entry.getKey());

                boolean wasLeaf = valueOut.swapLeaf(leaf);
                valueOut.object(vClass, entry.getValue());
                valueOut.swapLeaf(wasLeaf);
            }
        }
    }
    */


}
