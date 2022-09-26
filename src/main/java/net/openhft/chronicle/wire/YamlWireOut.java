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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.ref.TextBooleanReference;
import net.openhft.chronicle.bytes.ref.TextIntReference;
import net.openhft.chronicle.bytes.ref.TextLongArrayReference;
import net.openhft.chronicle.bytes.ref.TextLongReference;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;

import static net.openhft.chronicle.bytes.BytesStore.empty;

/**
 * YAML Based wire format
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class YamlWireOut<T extends YamlWireOut<T>> extends AbstractWire {
    public static final BytesStore TYPE = BytesStore.from("!type ");
    static final String NULL = "!null \"\"";
    static final BitSet STARTS_QUOTE_CHARS = new BitSet();
    static final BitSet QUOTE_CHARS = new BitSet();
    static final BytesStore COMMA_SPACE = BytesStore.from(", ");
    static final BytesStore COMMA_NEW_LINE = BytesStore.from(",\n");
    static final BytesStore NEW_LINE = BytesStore.from("\n");
    static final BytesStore EMPTY_AFTER_COMMENT = BytesStore.from(""); // not the same as EMPTY so we can check this value.
    static final BytesStore EMPTY = BytesStore.from("");
    static final BytesStore SPACE = BytesStore.from(" ");
    static final BytesStore END_FIELD = NEW_LINE;
    static final char[] HEXADECIMAL = "0123456789ABCDEF".toCharArray();

    static {
        IOTools.unmonitor(TYPE);
        for (char ch : "?%*&@`0123456789+- ',#:{}[]|>!\\".toCharArray())
            STARTS_QUOTE_CHARS.set(ch);
        for (char ch : "?,#:{}[]|>\\^".toCharArray())
            QUOTE_CHARS.set(ch);
        // make sure it has loaded.
        WireInternal.INTERNER.valueCount();
    }

    protected final YamlValueOut valueOut = createValueOut();
    protected final StringBuilder sb = new StringBuilder();
    private boolean addTimeStamps = false;
    private boolean trimFirstCurly = true;

    protected YamlWireOut(@NotNull Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    public boolean addTimeStamps() {
        return addTimeStamps;
    }

    public T addTimeStamps(boolean addTimeStamps) {
        this.addTimeStamps = addTimeStamps;
        return (T) this;
    }

    @NotNull
    protected YamlValueOut createValueOut() {
        return new YamlValueOut();
    }

    @NotNull
    private StringBuilder acquireStringBuilder() {
        StringUtils.setCount(sb, 0);
        return sb;
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    @NotNull
    @Override
    public ValueOut write() {
        return valueOut.write();
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

    @Override
    public ValueOut writeEvent(Class expectedType, Object eventKey) {
        if (eventKey instanceof WireKey)
            return writeEventName((WireKey) eventKey);
        if (eventKey instanceof CharSequence)
            return writeEventName((CharSequence) eventKey);
        if (expectedType != null && expectedType.isInstance(eventKey)) {
            if (eventKey instanceof Enum)
                return writeEventName(((Enum) eventKey).name());
            if (eventKey instanceof DynamicEnum)
                return writeEventName(((DynamicEnum) eventKey).name());
        }
        boolean wasLeft = valueOut.swapLeaf(true);
        try {
            return valueOut.write(expectedType, eventKey);
        } finally {
            valueOut.swapLeaf(wasLeft);
        }
    }

    @NotNull
    @Override
    public T dropDefault(boolean dropDefault) {
        valueOut.dropDefault = dropDefault;
        return (T) this;
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @NotNull
    @Override
    public T writeComment(@NotNull CharSequence s) {
        valueOut.writeComment(s);
        return (T) this;
    }

    @NotNull
    @Override
    public T addPadding(int paddingToAdd) {
        for (int i = 0; i < paddingToAdd; i++)
            bytes.writeUnsignedByte((bytes.writePosition() & 63) == 0 ? '\n' : ' ');
        return (T) this;
    }

    void escape(@NotNull CharSequence s) {
        @NotNull Quotes quotes = needsQuotes(s);
        if (quotes == Quotes.NONE) {
            escape0(s, quotes);
            return;
        }
        bytes.writeUnsignedByte(quotes.q);
        escape0(s, quotes);
        bytes.writeUnsignedByte(quotes.q);
    }

    protected void escape0(@NotNull CharSequence s, @NotNull Quotes quotes) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\0':
                    bytes.appendUtf8("\\0");
                    break;
                case 7:
                    bytes.appendUtf8("\\a");
                    break;
                case '\b':
                    bytes.appendUtf8("\\b");
                    break;
                case '\t':
                    bytes.appendUtf8("\\t");
                    break;
                case '\n':
                    bytes.appendUtf8("\\n");
                    break;
                case 0xB:
                    bytes.appendUtf8("\\v");
                    break;
                case 0xC:
                    bytes.appendUtf8("\\f");
                    break;
                case '\r':
                    bytes.appendUtf8("\\r");
                    break;
                case 0x1B:
                    bytes.appendUtf8("\\e");
                    break;
                case '"':
                    if (ch == quotes.q) {
                        bytes.writeUnsignedByte('\\').writeUnsignedByte(ch);
                    } else {
                        bytes.writeUnsignedByte(ch);
                    }
                    break;
                case '\'':
                    if (ch == quotes.q) {
                        bytes.writeUnsignedByte('\\').writeUnsignedByte(ch);
                    } else {
                        bytes.writeUnsignedByte(ch);
                    }
                    break;
                case '\\':
                    bytes.writeUnsignedByte('\\').writeUnsignedByte(ch);
                    break;
                case 0x85:
                    bytes.appendUtf8("\\N");
                    break;
                case 0xA0:
                    bytes.appendUtf8("\\_");
                    break;
                default:
                    if (ch > 255)
                        appendU4(ch);
                    else if (ch < ' ' || ch > 127)
                        appendX2(ch);
                    else
                        bytes.appendUtf8(ch);
                    break;
            }
        }
    }

    private void appendX2(char ch) {
        bytes.append('\\');
        bytes.append('x');
        bytes.append(HEXADECIMAL[(ch >> 4) & 0xF]);
        bytes.append(HEXADECIMAL[ch & 0xF]);
    }

    private void appendU4(char ch) {
        bytes.append('\\');
        bytes.append('u');
        bytes.append(HEXADECIMAL[ch >> 12]);
        bytes.append(HEXADECIMAL[(ch >> 8) & 0xF]);
        bytes.append(HEXADECIMAL[(ch >> 4) & 0xF]);
        bytes.append(HEXADECIMAL[ch & 0xF]);
    }

    @NotNull
    protected Quotes needsQuotes(@NotNull CharSequence s) {
        @NotNull Quotes quotes = Quotes.NONE;
        if (s.length() == 0)
            return Quotes.DOUBLE;

        if (STARTS_QUOTE_CHARS.get(s.charAt(0)) ||
                Character.isWhitespace(s.charAt(s.length() - 1)))
            return Quotes.DOUBLE;
        boolean hasSingleQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (QUOTE_CHARS.get(ch) || ch < ' ' || ch > 127)
                return Quotes.DOUBLE;
            if (ch == '\'')
                hasSingleQuote = true;
            if (ch == '"') {
                if (i < s.length() - 1 && s.charAt(i + 1) == '\'')
                    return Quotes.DOUBLE;
                quotes = Quotes.SINGLE;
            }
        }
        if (hasSingleQuote)
            return Quotes.NONE;
        return quotes;
    }

    public void append(@NotNull CharSequence cs) {
        if (use8bit)
            bytes.append8bit(cs);
        else
            bytes.appendUtf8(cs);
    }

    public void append(@NotNull CharSequence cs, int offset, int length) {
        if (use8bit)
            bytes.append8bit(cs, offset, offset + length);
        else
            bytes.appendUtf8(cs, offset, length);
    }

    public void writeObject(Object o) {
        if (o instanceof Iterable) {
            for (Object o2 : (Iterable) o) {
                writeObject(o2, 2);
            }
        } else if (o instanceof Map) {
            for (@NotNull Map.Entry<Object, Object> entry : ((Map<Object, Object>) o).entrySet()) {
                write(() -> entry.getKey().toString()).object(entry.getValue());
            }
        } else if (o instanceof WriteMarshallable) {
            valueOut.typedMarshallable((WriteMarshallable) o);

        } else {
            valueOut.object(o);
        }
    }

    private void writeObject(Object o, int indentation) {
        writeTwo('-', ' ');
        indentation(indentation - 2);
        valueOut.object(o);
    }

    private void indentation(int indentation) {
        while (indentation-- > 0)
            bytes.writeUnsignedByte(' ');
    }

    @Override
    public void writeStartEvent() {
        valueOut.prependSeparator();
        writeTwo('?', ' ');
    }

    @Override
    public void writeEndEvent() {
        valueOut.endEvent();
    }

    void writeTwo(char ch1, char ch2) {
        bytes.writeUnsignedByte(ch1);
        bytes.writeUnsignedByte(ch2);
    }

    /**
     * @return whether the top level curly brackets is dropped
     */
    public boolean trimFirstCurly() {
        return trimFirstCurly;
    }

    /**
     * @param trimFirstCurly whether the top level curly brackets is dropped
     */
    public T trimFirstCurly(boolean trimFirstCurly) {
        this.trimFirstCurly = trimFirstCurly;
        return (T) this;
    }

    class YamlValueOut implements ValueOut, CommentAnnotationNotifier {
        protected boolean hasCommentAnnotation = false;

        protected int indentation = 0;
        @NotNull
        protected List<BytesStore> seps = new ArrayList<>(4);
        @NotNull
        protected BytesStore sep = BytesStore.empty();
        protected boolean leaf = false;
        protected boolean dropDefault = false;
        @Nullable
        private String eventName;

        @Override
        public ClassLookup classLookup() {
            return YamlWireOut.this.classLookup();
        }

        @Override
        public void hasPrecedingComment(boolean hasCommentAnnotation) {
            this.hasCommentAnnotation = hasCommentAnnotation;
        }

        @Override
        public void resetState() {
            indentation = 0;
            seps.clear();
            sep = empty();
            leaf = false;
            dropDefault = false;
            eventName = null;
        }

        void prependSeparator() {
            appendSep();
            sep = BytesStore.empty();
        }

        protected void appendSep() {
            append(sep);
            trimWhiteSpace();
            if (bytes.endsWith('\n') || sep == EMPTY_AFTER_COMMENT)
                indent();
        }

        protected void trimWhiteSpace() {
            BytesUtil.combineDoubleNewline(bytes);
        }

        @Override
        public boolean swapLeaf(boolean isLeaf) {
            if (isLeaf == leaf)
                return leaf;
            leaf = isLeaf;
            if (!isLeaf && sep.startsWith(','))
                elementSeparator();
            return !leaf;
        }

        @NotNull
        @Override
        public T wireOut() {
            return (T) YamlWireOut.this;
        }

        protected void indent() {
            BytesUtil.combineDoubleNewline(bytes);
            for (int i = 0; i < indentation; i++) {
                bytes.writeUnsignedShort(' ' * 257);
            }
        }

        public void elementSeparator() {
            if (indentation == 0) {
                if (leaf) {
                    sep = COMMA_SPACE;
                } else {
                    sep = BytesStore.empty();
                    bytes.writeUnsignedByte('\n');
                }
            } else {
                sep = leaf ? COMMA_SPACE : COMMA_NEW_LINE;
            }
            BytesUtil.combineDoubleNewline(bytes);
        }

        @NotNull
        @Override
        public T bool(@Nullable Boolean flag) {
            if (dropDefault) {
                if (flag == null)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            append(flag == null ? nullOut() : flag ? "true" : "false");
            elementSeparator();
            return wireOut();
        }

        @NotNull
        public String nullOut() {
            return "!" + NULL;
        }

        @NotNull
        @Override
        public T text(@Nullable CharSequence s) {
            if (dropDefault) {
                if (s == null)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            if (s == null) {
                append(nullOut());
            } else {
                escape(s);
            }
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T bytes(@Nullable BytesStore fromBytes) {
            if (dropDefault) {
                if (fromBytes == null)
                    return wireOut();
                writeSavedEventName();
            }
            if (isText(fromBytes))
                return (T) text(fromBytes);

            int length = Maths.toInt32(fromBytes.readRemaining());
            @NotNull byte[] byteArray = new byte[length];
            fromBytes.copyTo(byteArray);

            return (T) bytes(byteArray);
        }

        @NotNull
        @Override
        public T rawBytes(@NotNull byte[] value) {
            if (dropDefault) {
                writeSavedEventName();
            }
            prependSeparator();
            bytes.write(value);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T rawText(CharSequence value) {
            if (dropDefault) {
                writeSavedEventName();
            }
            prependSeparator();
            bytes.write(value);
            elementSeparator();
            return wireOut();
        }

        private boolean isText(@Nullable BytesStore fromBytes) {

            if (fromBytes == null)
                return true;
            for (long i = fromBytes.readPosition(); i < fromBytes.readLimit(); i++) {
                int ch = fromBytes.readUnsignedByte(i);
                if ((ch < ' ' && ch != '\t') || ch >= 127)
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
        public T bytes(byte[] byteArray) {
            if (dropDefault) {
                writeSavedEventName();
            }
            return (T) bytes("!binary", byteArray);
        }

        @NotNull
        @Override
        public T bytes(@NotNull String type, byte[] byteArray) {
            if (dropDefault) {
                writeSavedEventName();
            }
            prependSeparator();
            typePrefix(type);
            if (getClass() != YamlValueOut.class)
                bytes.append('"');
            append(Base64.getEncoder().encodeToString(byteArray));
            if (getClass() != YamlValueOut.class)
                bytes.append('"');
            elementSeparator();
            endTypePrefix();

            return wireOut();
        }

        @NotNull
        @Override
        public T bytes(@NotNull String type, @Nullable BytesStore bytesStore) {
            if (dropDefault) {
                writeSavedEventName();
            }
            if (bytesStore == null)
                return (T)nu11();
            prependSeparator();
            typePrefix(type);
            append(Base64.getEncoder().encodeToString(bytesStore.toByteArray()));
            endTypePrefix();
            append(END_FIELD);
            elementSeparator();

            return wireOut();
        }

        @NotNull
        @Override
        public T int8(byte i8) {
            if (dropDefault) {
                if (i8 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(i8);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T uint8checked(int u8) {
            if (dropDefault) {
                if (u8 == 0)
                    return wireOut();
                writeSavedEventName();
            }

            prependSeparator();
            bytes.append(u8);
            elementSeparator();

            return wireOut();
        }

        @NotNull
        @Override
        public T int16(short i16) {
            if (dropDefault) {
                if (i16 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(i16);
            elementSeparator();

            return wireOut();
        }

        @NotNull
        @Override
        public T uint16checked(int u16) {
            if (dropDefault) {
                if (u16 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(u16);
            elementSeparator();

            return wireOut();
        }

        @NotNull
        @Override
        public T utf8(int codepoint) {
            if (dropDefault) {
                if (codepoint == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            final StringBuilder stringBuilder = acquireStringBuilder();
            stringBuilder.appendCodePoint(codepoint);
            text(stringBuilder);
            sep = empty();
            return wireOut();
        }

        @NotNull
        @Override
        public T int32(int i32) {
            if (dropDefault) {
                if (i32 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(i32);
            elementSeparator();

            return wireOut();
        }

        @NotNull
        @Override
        public T uint32checked(long u32) {
            if (dropDefault) {
                if (u32 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(u32);
            elementSeparator();

            return wireOut();
        }

        @NotNull
        @Override
        public T int64(long i64) {
            if (dropDefault) {
                if (i64 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(i64);
            elementSeparator();
            // 2001 to 2100 best effort basis.
            boolean addTimeStamp = YamlWireOut.this.addTimeStamps && !leaf;
            if (addTimeStamp) {
                addTimeStamp(i64);
            }

            return wireOut();
        }

        @NotNull
        @Override
        public T int128forBinding(long i64x0, long i64x1, TwoLongValue longValue) {
            throw new UnsupportedOperationException("todo");
        }

        public void addTimeStamp(long i64) {
            if ((long) 1e12 < i64 && i64 < (long) 4.111e12) {
                bytes.append(", # ");
                bytes.appendDateMillis(i64);
                bytes.append("T");
                bytes.appendTimeMillis(i64);
                sep = NEW_LINE;
            } else if ((long) 1e18 < i64 && i64 < (long) 4.111e18) {
                long millis = i64 / 1_000_000;
                long nanos = i64 % 1_000_000;
                bytes.append(", # ");
                bytes.appendDateMillis(millis);
                bytes.append("T");
                bytes.appendTimeMillis(millis);
                bytes.append((char) ('0' + nanos / 100000));
                bytes.append((char) ('0' + nanos / 100000 % 10));
                bytes.append((char) ('0' + nanos / 10000 % 10));
                bytes.append((char) ('0' + nanos / 1000 % 10));
                bytes.append((char) ('0' + nanos / 100 % 10));
                bytes.append((char) ('0' + nanos / 10 % 10));
                bytes.append((char) ('0' + nanos % 10));
                sep = NEW_LINE;
            }
        }

        @NotNull
        @Override
        public T int64_0x(long i64) {
            if (dropDefault) {
                if (i64 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.writeUnsignedByte('0')
                    .writeUnsignedByte('x')
                    .appendBase16(i64);
            elementSeparator();

            return wireOut();
        }

        @NotNull
        @Override
        public T int64array(long capacity) {
            if (dropDefault) {
                writeSavedEventName();
            }
            TextLongArrayReference.write(bytes, capacity);
            return wireOut();
        }

        @NotNull
        @Override
        public T int64array(long capacity, @NotNull LongArrayValues values) {
            if (dropDefault) {
                writeSavedEventName();
            }
            long pos = bytes.writePosition();
            TextLongArrayReference.write(bytes, capacity);
            ((Byteable) values).bytesStore(bytes, pos, bytes.lengthWritten(pos));
            return wireOut();
        }

        @NotNull
        @Override
        public T float32(float f) {
            if (dropDefault) {
                if (f == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            double af = Math.abs(f);
            if (af >= 1e-3 && af < 1e6)
                bytes.append(f);
            else
                bytes.append(floatToString(f));
            elementSeparator();

            return wireOut();
        }

        @NotNull
        @Override
        public T float64(double d) {
            if (dropDefault) {
                if (d == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            double ad = Math.abs(d);
            if (ad >= 1e-7 && ad < 1e15) {
                if ((int) (ad / 1e6) * 1e6 == ad) {
                    bytes.append((int) (d / 1e6)).append("E6");
                } else if ((int) (ad / 1e3) * 1e3 == ad) {
                    bytes.append((int) (d / 1e3)).append("E3");
                } else if (ad < 1e-3) {
                    double d7 = Math.round(d * 1e16) / 1e9;
                    double ad7 = Math.abs(d7);
                    if (ad7 < 1e1)
                        bytes.append(d7).append("E-7");
                    else if (ad7 < 1e2)
                        bytes.append(d7 / 1e1).append("E-6");
                    else if (ad7 < 1e3)
                        bytes.append(d7 / 1e2).append("E-5");
                    else if (ad7 < 1e4)
                        bytes.append(d7 / 1e3).append("E-4");
                    else
                        bytes.append(d7 / 1e4).append("E-3");
                } else {
                    bytes.append(d);
                }
            } else {
                bytes.append(doubleToString(d));
            }
            elementSeparator();

            return wireOut();
        }

        protected String doubleToString(double d) {
            return Double.toString(d);
        }

        protected String floatToString(float f) {
            return Float.toString(f);
        }

        @NotNull
        @Override
        public T time(LocalTime localTime) {
            return asText(localTime);
        }

        @NotNull
        @Override
        public T zonedDateTime(@Nullable ZonedDateTime zonedDateTime) {
            if (dropDefault) {
                if (zonedDateTime == null)
                    return wireOut();
                writeSavedEventName();
            }
            final String s = zonedDateTime.toString();
            return (T) (s.endsWith("]") ? text(s) : asText(s));
        }

        @NotNull
        @Override
        public T date(LocalDate localDate) {
            return asText(localDate);
        }

        @NotNull
        @Override
        public T dateTime(LocalDateTime localDateTime) {
            return asText(localDateTime);
        }

        @NotNull
        private T asText(@Nullable Object stringable) {
            if (dropDefault) {
                if (stringable == null)
                    return wireOut();
                writeSavedEventName();
            }
            if (stringable == null) {
                nu11();
            } else {
                prependSeparator();
                final String s = stringable.toString();
                final Quotes quotes = needsQuotes(s);
                asTestQuoted(s, quotes);
                elementSeparator();
            }

            return wireOut();
        }

        protected void asTestQuoted(String s, Quotes quotes) {
            if (quotes == Quotes.NONE) {
                append(s);
            } else {
                escape0(s, quotes);
            }
        }

        @NotNull
        @Override
        public ValueOut optionalTyped(Class aClass) {
            return typePrefix(aClass);
        }

        @NotNull
        @Override
        public YamlValueOut typePrefix(@NotNull CharSequence typeName) {
            if (dropDefault) {
                writeSavedEventName();
            }
            prependSeparator();
            bytes.writeUnsignedByte('!');
            append(typeName);
            bytes.writeUnsignedByte(' ');
            sep = BytesStore.empty();
            return this;
        }

        @NotNull
        @Override
        public T typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, Class type) {
            if (dropDefault) {
                if (type == null)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            append(TYPE);
            typeTranslator.accept(type, bytes);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T typeLiteral(@Nullable CharSequence type) {
            if (dropDefault) {
                if (type == null)
                    return wireOut();
                writeSavedEventName();
            }
            if (type == null)
                return (T) nu11();
            prependSeparator();
            append(TYPE);
            escape(type);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T uuid(@NotNull UUID uuid) {
            return asText(uuid);
        }

        @NotNull
        @Override
        public T int32forBinding(int value) {
            if (dropDefault) {
                writeSavedEventName();
            }
            prependSeparator();
            TextIntReference.write(bytes, value);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T int32forBinding(int value, @NotNull IntValue intValue) {
            if (dropDefault) {
                writeSavedEventName();
            }
            if (!(intValue instanceof TextIntReference))
                throw new IllegalArgumentException();
            prependSeparator();
            long offset = bytes.writePosition();
            TextIntReference.write(bytes, value);
            long length = bytes.lengthWritten(offset);
            ((Byteable) intValue).bytesStore(bytes, offset, length);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T int64forBinding(long value) {
            if (dropDefault) {
                writeSavedEventName();
            }
            prependSeparator();
            TextLongReference.write(bytes, value);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T int64forBinding(long value, @NotNull LongValue longValue) {
            if (dropDefault) {
                writeSavedEventName();
            }
            if (!(longValue instanceof TextLongReference))
                throw new IllegalArgumentException();
            prependSeparator();
            long offset = bytes.writePosition();
            TextLongReference.write(bytes, value);
            long length = bytes.lengthWritten(offset);
            ((Byteable) longValue).bytesStore(bytes, offset, length);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T boolForBinding(final boolean value, @NotNull final BooleanValue longValue) {
            if (dropDefault) {
                writeSavedEventName();
            }
            if (!(longValue instanceof TextBooleanReference))
                throw new IllegalArgumentException();
            prependSeparator();
            long offset = bytes.writePosition();
            TextBooleanReference.write(value, bytes, offset);
            long length = bytes.lengthWritten(offset);
            ((Byteable) longValue).bytesStore(bytes, offset, length);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public <E> T sequence(E e, @NotNull BiConsumer<E, ValueOut> writer) {
            startBlock('[');
            boolean leaf = this.leaf;
            if (!leaf)
                newLine();
            else
                bytes.writeUnsignedByte(' ');
            long pos = bytes.writePosition();
            writer.accept(e, this);
            if (!leaf)
                addNewLine(pos);

            popState();
            this.leaf = leaf;
            if (!leaf)
                indent();
            else
                addSpace(pos);
            endBlock(']');
            elementSeparator();
            return wireOut();
        }

        public void startBlock(char c) {
            if (dropDefault) {
                writeSavedEventName();
            }
            if (!sep.isEmpty()) {
                append(sep);
                trimWhiteSpace();
                indent();
                sep = EMPTY;
            }
            pushState();
            bytes.writeUnsignedByte(c);
        }

        @NotNull
        @Override
        public <E, K> T sequence(E e, K kls, @NotNull TriConsumer<E, K, ValueOut> writer) {
            boolean leaf = this.leaf;
            startBlock('[');
            if (leaf)
                sep = SPACE;
            else
                newLine();
            long pos = bytes.readPosition();
            writer.accept(e, kls, this);
            if (leaf)
                addSpace(pos);
            else
                addNewLine(pos);

            popState();
            if (!leaf)
                indent();
            endBlock(']');
            elementSeparator();
            return wireOut();
        }

        public void endBlock(char c) {
            BytesUtil.combineDoubleNewline(bytes);
            bytes.writeUnsignedByte(c);
        }

        protected void addNewLine(long pos) {
            if (bytes.writePosition() > pos + 1)
                bytes.writeUnsignedByte('\n');
        }

        protected void addSpace(long pos) {
            if (bytes.writePosition() > pos + 1)
                bytes.writeUnsignedByte(' ');
        }

        protected void newLine() {
            sep = NEW_LINE;
        }

        protected void popState() {
            sep = seps.remove(seps.size() - 1);
            indentation--;
            leaf = false;
            dropDefault = false;
        }

        protected void pushState() {
            indentation++;
            seps.add(sep);
            sep = EMPTY;
        }

        @NotNull
        @Override
        public T marshallable(@NotNull WriteMarshallable object) {
            WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(object.getClass());
            boolean wasLeaf0 = leaf;
            if (indentation > 1 && wm.isLeaf())
                leaf = true;

            if (dropDefault) {
                writeSavedEventName();
            }
            if (trimFirstCurly && bytes.writePosition() == 0) {
                object.writeMarshallable(YamlWireOut.this);
                if (bytes.writePosition() == 0)
                    bytes.append("{}");
                return wireOut();
            }
            boolean wasLeaf = leaf;
            startBlock('{');

            if (wasLeaf)
                afterOpen();
            else
                newLine();

            object.writeMarshallable(wireOut());
            @Nullable BytesStore popSep = null;
            if (wasLeaf) {
                if (sep.endsWith(' '))
                    append(" ");
                trimWhiteSpace();
                leaf = false;
                popState();
            } else if (!seps.isEmpty()) {
                popSep = seps.get(seps.size() - 1);
                trimWhiteSpace();
                popState();
                newLine();
            }
            if (sep.startsWith(',')) {
                append(sep, 1, sep.length() - 1);
                if (!wasLeaf)
                    indent();

            } else {
                prependSeparator();
            }
            endBlock('}');

            leaf = wasLeaf0;

            if (popSep != null)
                sep = popSep;

            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public T marshallable(@NotNull Serializable object) {
            if (dropDefault) {
                writeSavedEventName();
            }
            if (bytes.writePosition() == 0) {
                writeSerializable(object);
                return wireOut();
            }
            boolean wasLeaf = leaf;
            if (!wasLeaf)
                pushState();

            prependSeparator();
            bytes.writeUnsignedByte(object instanceof Externalizable ? '[' : '{');
            if (wasLeaf)
                afterOpen();
            else
                newLine();

            writeSerializable(object);
            @Nullable BytesStore popSep = null;
            if (wasLeaf) {
                leaf = false;
            } else if (seps.size() > 0) {
                popSep = seps.get(seps.size() - 1);
                popState();
                newLine();
            }
            if (sep.startsWith(',')) {
                append(sep, 1, sep.length() - 1);
                trimWhiteSpace();
                if (!wasLeaf)
                    indent();

            } else {
                prependSeparator();
            }
            BytesUtil.combineDoubleNewline(bytes);
            bytes.writeUnsignedByte(object instanceof Externalizable ? ']' : '}');
            if (popSep != null)
                sep = popSep;
            if (indentation == 0) {
                afterClose();

            } else {
                elementSeparator();
            }
            return wireOut();
        }

        private void writeSerializable(@NotNull Serializable object) {
            try {
                if (object instanceof Externalizable)
                    ((Externalizable) object).writeExternal(objectOutput());
                else
                    Wires.writeMarshallable(object, wireOut());
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }

        protected void afterClose() {
            newLine();
            append(sep);
            sep = EMPTY;
        }

        protected void afterOpen() {
            sep = SPACE;
        }

        @NotNull
        @Override
        public T map(@NotNull final Map map) {
            if (dropDefault) {
                writeSavedEventName();
            }
            marshallable(map, Object.class, Object.class, false);
            return wireOut();
        }

        protected void endField() {
            sep = END_FIELD;
        }

        protected void fieldValueSeperator() {
            writeTwo(':', ' ');
        }

        @NotNull
        public YamlValueOut write() {
            if (dropDefault) {
                eventName = "";
            } else {
                append(sep);
                writeTwo('"', '"');
                endEvent();
            }
            return this;
        }

        @NotNull
        public YamlValueOut write(@NotNull WireKey key) {
            if (dropDefault) {
                eventName = key.name().toString();
            } else {
                write(key.name());
            }
            return this;
        }

        @NotNull
        public YamlValueOut write(@NotNull CharSequence name) {
            if (dropDefault) {
                eventName = name.toString();
            } else {
                prependSeparator();
                escape(name);
                fieldValueSeperator();
            }
            return this;
        }

        @NotNull
        public YamlValueOut write(Class expectedType, @NotNull Object objectKey) {
            if (dropDefault) {
                if (expectedType != String.class)
                    throw new UnsupportedOperationException("todo");
                eventName = objectKey.toString();
            } else {
                prependSeparator();
                writeStartEvent();
                object(expectedType, objectKey);
                endEvent();
            }
            return this;
        }

        private void writeSavedEventName() {
            if (eventName == null)
                return;
            prependSeparator();
            escape(eventName);
            fieldValueSeperator();
            eventName = null;
        }

        public void endEvent() {
            if (bytes.readByte(bytes.writePosition() - 1) <= ' ')
                bytes.writeSkip(-1);
            fieldValueSeperator();
            sep = empty();
        }

        public void writeComment(@NotNull CharSequence s) {

            if (hasCommentAnnotation) {
                if (!sep.endsWith('\n'))
                    return;
                sep = COMMA_SPACE;
            } else
                prependSeparator();

            append(sep);

            if (hasCommentAnnotation)
                writeTwo('\t', '\t');

            writeTwo('#', ' ');

            append(s);
            bytes.writeUnsignedByte('\n');
            sep = EMPTY_AFTER_COMMENT;
        }
    }
}
