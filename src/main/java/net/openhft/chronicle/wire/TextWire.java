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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.ref.TextIntReference;
import net.openhft.chronicle.bytes.ref.TextLongArrayReference;
import net.openhft.chronicle.bytes.ref.TextLongReference;
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.*;

import static net.openhft.chronicle.bytes.BytesStore.empty;
import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static net.openhft.chronicle.core.util.ReadResolvable.readResolve;

/**
 * YAML Based wire format
 */
public class TextWire extends AbstractWire implements Wire {

    public static final BytesStore TYPE = BytesStore.wrap("!type ");
    static final String SEQ_MAP = "!seqmap";
    static final String NULL = "!null \"\"";
    static final BitSet STARTS_QUOTE_CHARS = new BitSet();
    static final BitSet QUOTE_CHARS = new BitSet();
    static final Logger LOG = LoggerFactory.getLogger(TextWire.class);
    static final ThreadLocal<StopCharTester> ESCAPED_QUOTES = ThreadLocal.withInitial(StopCharTesters.QUOTES::escaping);
    static final ThreadLocal<StopCharTester> ESCAPED_SINGLE_QUOTES = ThreadLocal.withInitial(() -> StopCharTesters.SINGLE_QUOTES.escaping());
    static final ThreadLocal<StopCharsTester> ESCAPED_END_OF_TEXT = ThreadLocal.withInitial(() -> TextStopCharsTesters.END_OF_TEXT.escaping());
    static final BytesStore COMMA_SPACE = BytesStore.from(", ");
    static final BytesStore COMMA_NEW_LINE = BytesStore.from(",\n");
    static final BytesStore NEW_LINE = BytesStore.from("\n");
    static final BytesStore EMPTY_AFTER_COMMENT = BytesStore.from("");
    static final BytesStore SPACE = BytesStore.from(" ");
    static final BytesStore END_FIELD = NEW_LINE;

    static {
        for (char ch : "?0123456789+- \t\',#:{}[]|>!\0\b\\".toCharArray())
            STARTS_QUOTE_CHARS.set(ch);
        for (char ch : "?,#:{}[]|>\0\b\\".toCharArray())
            QUOTE_CHARS.set(ch);
    }

    protected final TextValueOut valueOut = createValueOut();
    protected final TextValueIn valueIn = createValueIn();
    private final WriteDocumentContext writeContext = new WriteDocumentContext(this);
    private final ReadDocumentContext readContext = new ReadDocumentContext(this);
    private final StringBuilder sb = new StringBuilder();
    protected long lineStart = 0;
    DefaultValueIn defaultValueIn;
    private boolean ready;

    public TextWire(Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    public TextWire(Bytes bytes) {
        this(bytes, false);
    }

    public static TextWire fromFile(String name) throws IOException {
        return new TextWire(Bytes.wrapForRead(IOTools.readFile(name)), true);
    }

    @NotNull
    public static TextWire from(@NotNull String text) {
        return new TextWire(Bytes.from(text));
    }

    public static String asText(@NotNull Wire wire) {
        long pos = wire.bytes().readPosition();
        TextWire tw = new TextWire(nativeBytes());
        wire.copyTo(tw);
        wire.bytes().readPosition(pos);

        return tw.toString();
    }

    public static <ACS extends Appendable & CharSequence> void unescape(@NotNull ACS sb) {
        int end = 0;
        int length = sb.length();
        for (int i = 0; i < length; i++) {
            char ch = sb.charAt(i);
            if (ch == '\\' && i < length - 1) {
                char ch3 = sb.charAt(++i);
                switch (ch3) {
                    case 'b':
                        ch = '\b';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case 'x':
                        ch = (char)
                                (Character.getNumericValue(sb.charAt(++i)) * 16 +
                                        Character.getNumericValue(sb.charAt(++i)));
                        break;
                    case 'u':
                        ch = (char)
                                (Character.getNumericValue(sb.charAt(++i)) * 4096 +
                                        Character.getNumericValue(sb.charAt(++i)) * 256 +
                                        Character.getNumericValue(sb.charAt(++i)) * 16 +
                                        Character.getNumericValue(sb.charAt(++i)));
                        break;
                    case '0':
                        ch = 0;
                        break;
                    default:
                        ch = ch3;
                }
            }
            AppendableUtil.setCharAt(sb, end++, ch);
        }
        if (length != sb.length())
            throw new IllegalStateException("Length changed from " + length + " to " + sb.length() + " for " + sb);
        AppendableUtil.setLength(sb, end);
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
        this.classLookup = classLookup;
    }

    @Override
    public ClassLookup classLookup() {
        return classLookup;
    }


    @Override
    public DocumentContext writingDocument(boolean metaData) {
        writeContext.start(metaData);
        return writeContext;
    }

    @Override
    public DocumentContext readingDocument() {
        readContext.start();
        return readContext;
    }

    @Override
    public DocumentContext readingDocument(long readLocation) {
        final long readPosition = bytes().readPosition();
        final long readLimit = bytes().readLimit();
        bytes().readPosition(readLocation);
        readContext.start();
        readContext.closeReadLimit(readLimit);
        readContext.closeReadPosition(readPosition);
        return readContext;
    }

    @NotNull
    protected TextValueOut createValueOut() {
        return new TextValueOut();
    }

    @NotNull
    protected TextValueIn createValueIn() {
        return new TextValueIn();
    }

    public String toString() {
        if (bytes.readRemaining() > (1024 * 10)) {
            final long l = bytes.readLimit();
            try {
                bytes.readLimit(bytes.readPosition() + (1024 * 10));
                return bytes.toString() + "..";
            } finally {
                bytes.readLimit(l);
            }
        } else
            return bytes.toString();
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        wire.bytes().write(bytes, bytes().readPosition(), bytes().readLimit());
    }

    @NotNull
    @Override
    public ValueIn read() {
        readField(acquireStringBuilder());
        return valueIn;
    }

    private StringBuilder acquireStringBuilder() {
        StringUtils.setCount(sb, 0);
        return sb;
    }

    @NotNull
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        consumePadding();
        if (peekCode() == ',') {
            bytes.readSkip(1);
            consumePadding();
        }
        try {
            int ch = peekCode();
            if (ch == '"') {
                bytes.readSkip(1);

                parseUntil(sb, getEscapingQuotes());

                consumePadding();
                ch = readCode();
                if (ch != ':')
                    throw new UnsupportedOperationException("Expected a : at " + bytes.toDebugString());

            } else if (ch < 0) {
                sb.setLength(0);
                return sb;

            } else {
                parseUntil(sb, getEscapingEndOfText());
            }
            unescape(sb);
        } catch (BufferUnderflowException ignored) {
        }
        //      consumePadding();
        return sb;
    }

    @NotNull
    protected StopCharsTester getEscapingEndOfText() {
        StopCharsTester escaping = ESCAPED_END_OF_TEXT.get();
        // reset it.
        escaping.isStopChar(' ', ' ');
        return escaping;
    }

    protected StopCharTester getEscapingQuotes() {
        StopCharTester sct = ESCAPED_QUOTES.get();
        // reset it.
        sct.isStopChar(' ');
        return sct;
    }

    private StopCharTester getEscapingSingleQuotes() {
        StopCharTester sct = ESCAPED_SINGLE_QUOTES.get();
        // reset it.
        sct.isStopChar(' ');
        return sct;
    }

    public void consumePadding() {
        for (; ; ) {
            int codePoint = peekCode();
            if (codePoint == '#') {
                //noinspection StatementWithEmptyBody
                while (readCode() >= ' ') ;
                this.lineStart = bytes.readPosition();
            } else if (Character.isWhitespace(codePoint) || codePoint == ',') {
                if (codePoint == '\n' || codePoint == '\r')
                    this.lineStart = bytes.readPosition() + 1;
                bytes.readSkip(1);
            } else {
                break;
            }
        }
    }

    protected void consumeDocumentStart() {
        if (bytes.readRemaining() > 4) {
            long pos = bytes.readPosition();
            if (bytes.readByte(pos) == '-' && bytes.readByte(pos + 1) == '-' && bytes.readByte(pos + 2) == '-')
                bytes.readSkip(3);
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
    protected boolean peekStringIgnoreCase(@NotNull final String source) {
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

    protected int readCode() {
        if (bytes.readRemaining() < 1)
            return -1;
        return bytes.readUnsignedByte();
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        consumePadding();
        ValueInState curr = valueIn.curr();
        StringBuilder sb = acquireStringBuilder();
        // did we save the position last time
        // so we could go back and parseOne an older field?
        if (curr.savedPosition() > 0) {
            bytes.readPosition(curr.savedPosition() - 1);
            curr.savedPosition(0L);
        }
        CharSequence name = key.name();
        while (bytes.readRemaining() > 0) {
            long position = bytes.readPosition();
            // at the current position look for the field.
            readField(sb);
            if (sb.length() == 0 || StringUtils.isEqual(sb, name))
                return valueIn;

            // if no old field nor current field matches, set to default values.
            // we may come back and set the field later if we find it.
            curr.addUnexpected(position);
            long toSkip = valueIn.readLengthMarshallable();
            bytes.readSkip(toSkip);
            consumePadding();
        }

        return read2(key, curr, sb, name);
    }

    protected ValueIn read2(@NotNull WireKey key, ValueInState curr, StringBuilder sb, CharSequence name) {
        long position2 = bytes.readPosition();

        // if not a match go back and look at old fields.
        for (int i = 0; i < curr.unexpectedSize(); i++) {
            bytes.readPosition(curr.unexpected(i));
            readField(sb);
            if (sb.length() == 0 || StringUtils.isEqual(sb, name)) {
                // if an old field matches, remove it, save the current position
                curr.removeUnexpected(i);
                curr.savedPosition(position2 + 1);
                return valueIn;
            }
        }
        bytes.readPosition(position2);

        if (defaultValueIn == null)
            defaultValueIn = new DefaultValueIn(this);
        defaultValueIn.wireKey = key;
        return defaultValueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        consumePadding();
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
        valueIn.resetState();
        valueOut.resetState();
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    @Override
    public boolean hasMore() {
        consumePadding();

        return bytes.readRemaining() > 0;
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

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @NotNull
    @Override
    public Wire writeComment(@NotNull CharSequence s) {
        valueOut.writeComment(s);
        return this;
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        for (int i = 0; i < paddingToAdd; i++)
            bytes.writeUnsignedByte((bytes.writePosition() & 63) == 0 ? '\n' : ' ');
        return this;
    }

    void escape(@NotNull CharSequence s) {
        Quotes quotes = needsQuotes(s);
        if (quotes == Quotes.NONE) {
            escape0(s, quotes);
            return;
        }
        bytes.writeUnsignedByte(quotes.q);
        escape0(s, quotes);
        bytes.writeUnsignedByte(quotes.q);
    }

    protected void escape0(@NotNull CharSequence s, Quotes quotes) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    if (ch == quotes.q) {
                        bytes.writeUnsignedByte('\\').writeUnsignedByte(ch);
                    } else {
                        bytes.writeUnsignedByte(ch);
                    }
                    break;
                case '\'':
                    if (ch == quotes.q) {
                        bytes.appendUtf8('\\').appendUtf8(ch);
                    } else {
                        bytes.appendUtf8(ch);
                    }
                    break;
                case '\\':
                    bytes.appendUtf8('\\').appendUtf8(ch);
                    break;
                case '\b':
                    bytes.appendUtf8("\\b");
                    break;
                case '\t':
                    bytes.appendUtf8("\\t");
                    break;
                case '\r':
                    bytes.appendUtf8("\\r");
                    break;
                case '\n':
                    bytes.appendUtf8("\\n");
                    break;
                case '\0':
                    bytes.appendUtf8("\\0");
                    break;
                default:
//                    if (ch > 127) {
//                        bytes.appendUtf8("\\u");
//                        bytes.appendUtf8(HEX[(ch >> 12) & 0xF]);
//                        bytes.appendUtf8(HEX[(ch >> 8) & 0xF]);
//                        bytes.appendUtf8(HEX[(ch >> 4) & 0xF]);
//                        bytes.appendUtf8(HEX[ch & 0xF]);
//                    } else {
                    bytes.appendUtf8(ch);
//                    }
                    break;
            }
        }
    }

    protected Quotes needsQuotes(@NotNull CharSequence s) {
        Quotes quotes = Quotes.NONE;
        if (s.length() == 0)
            return Quotes.DOUBLE;

        if (STARTS_QUOTE_CHARS.get(s.charAt(0)))
            return Quotes.DOUBLE;
        if (s.charAt(0) == '"')
            return Quotes.SINGLE;
        if (Character.isWhitespace(s.charAt(s.length() - 1)))
            return Quotes.DOUBLE;
        for (int i = 1; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (QUOTE_CHARS.get(ch))
                return Quotes.DOUBLE;
            if (ch == '"')
                quotes = Quotes.SINGLE;
        }
        return quotes;
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return new TextLongReference();
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        return new TextIntReference();
    }

    @NotNull
    @Override
    public LongArrayValues newLongArrayReference() {
        return new TextLongArrayReference();
    }

    public void parseWord(StringBuilder sb) {
        parseUntil(sb, StopCharTesters.SPACE_STOP);
    }

    public void parseUntil(StringBuilder sb, StopCharTester testers) {
        if (use8bit)
            bytes.parse8bit(sb, testers);
        else
            bytes.parseUtf8(sb, testers);
    }

    public void parseUntil(StringBuilder sb, StopCharsTester testers) {
        sb.setLength(0);
        if (use8bit) {
            AppendableUtil.read8bitAndAppend(bytes, sb, testers);
        } else {
            try {
                AppendableUtil.readUTFAndAppend(bytes, sb, testers);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }
    }

    public void append(CharSequence cs) {
        if (use8bit)
            bytes.append8bit(cs);
        else
            bytes.appendUtf8(cs);
    }

    public void append(CharSequence cs, int offset, int length) {
        if (use8bit)
            bytes.append8bit(cs, offset, offset + length);
        else
            bytes.appendUtf8(cs, offset, length);
    }

    public Object readObject() {
        consumePadding();
        consumeDocumentStart();
        return readObject(0);
    }

    Object readObject(int indentation) {
        consumePadding();
        int code = peekCode();
        int indentation2 = indentation();
        if (indentation2 < indentation)
            return NoObject.NO_OBJECT;
        switch (code) {
            case '-':
                if (bytes.readByte(bytes.readPosition() + 1) == '-')
                    return NoObject.NO_OBJECT;

                return readList(indentation2);
            case '[':
                return readList();
            case '{':
                return readMap();
            case '!':
                return readTypedObject();
            default:
                return readMap(indentation2);
        }
    }

    private int indentation() {
        return Maths.toInt32(bytes.readPosition() - lineStart);
    }

    private Object readTypedObject() {
        return valueIn.object(Object.class);
    }

    private List readList() {
        throw new UnsupportedOperationException();
    }

    List readList(int indentation) {
        List<Object> objects = new ArrayList<>();
        while (peekCode() == '-') {
            if (indentation() < indentation)
                break;
            if (bytes.readByte(bytes.readPosition() + 1) == '-')
                break;
            long ls = lineStart;
            bytes.readSkip(1);
            consumePadding();
            if (lineStart == ls) {
                objects.add(valueIn.objectWithInferredType(Object.class));
            } else {
                Object e = readObject(indentation);
                if (e != NoObject.NO_OBJECT)
                    objects.add(e);
            }
            consumePadding();
        }

        return objects;
    }

    Map readMap() {
        bytes.readSkip(1);
        Map map = new LinkedHashMap<>();
        StringBuilder sb = WireInternal.acquireAnotherStringBuilder(acquireStringBuilder());
        while (bytes.readRemaining() > 0) {
            consumePadding();
            if (peekCode() == '}') {
                bytes.readSkip(1);
                break;
            }
            read(sb);
            String key = WireInternal.INTERNER.intern(sb);
            Object value = valueIn.objectWithInferredType(Object.class);
            map.put(key, value);
        }
        return map;
    }

    private Map readMap(int indentation) {
        Map map = new LinkedHashMap<>();
        StringBuilder sb = WireInternal.acquireAnotherStringBuilder(acquireStringBuilder());
        while (bytes.readRemaining() > 0) {
            consumePadding();
            if (indentation() < indentation || bytes.readRemaining() == 0)
                break;
            read(sb);
            String key = WireInternal.INTERNER.intern(sb);
            if (key.equals("..."))
                break;
            Object value = valueIn.objectWithInferredType(Object.class);
            map.put(key, value);
        }
        return map;
    }

    public void writeObject(Object o) {
        if (o instanceof Iterable) {
            for (Object o2 : (Iterable) o) {
                writeObject(o2, 2);
            }
        } else if (o instanceof Map) {
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) o).entrySet()) {
                write(() -> entry.getKey().toString()).object(entry.getValue());
            }
        } else if (o instanceof WriteMarshallable) {
            valueOut.typedMarshallable((WriteMarshallable) o);
        } else {
            valueOut.object(o);
        }
    }

    private void writeObject(Object o, int indentation) {
        bytes.writeUnsignedByte('-');
        bytes.writeUnsignedByte(' ');
        indentation(indentation - 2);
        valueOut.object(o);
    }

    private void indentation(int indentation) {
        while (indentation-- > 0)
            bytes.writeUnsignedByte(' ');
    }

    enum NoObject {NO_OBJECT}

    class TextValueOut implements ValueOut {
        protected int indentation = 0;
        protected List<BytesStore> seps = new ArrayList<>(4);
        @NotNull
        protected BytesStore sep = BytesStore.empty();
        protected boolean leaf = false;

        public void resetState() {
            indentation = 0;
            seps.clear();
            sep = empty();
            leaf = false;
        }

        void prependSeparator() {
            append(sep);
            if (sep.endsWith('\n') || sep == EMPTY_AFTER_COMMENT)
                indent();
            sep = BytesStore.empty();
        }

        @NotNull
        @Override
        public ValueOut leaf() {
            leaf = true;
            return this;
        }

        @Override
        public ValueOut leaf(boolean leaf) {
            this.leaf = leaf;
            return this;
        }

        @NotNull
        @Override
        public WireOut wireOut() {
            return TextWire.this;
        }

        private void indent() {
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
        }

        @NotNull
        @Override
        public WireOut bool(@Nullable Boolean flag) {
            prependSeparator();
            append(flag == null ? "!" + NULL : flag ? "true" : "false");
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            prependSeparator();
            if (s == null) {
                append("!" + NULL);
            } else {
                escape(s);
            }
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            prependSeparator();
            bytes.append(i8);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public WireOut bytes(@Nullable BytesStore fromBytes) {

            if (isText(fromBytes))
                return text(fromBytes);

            int length = Maths.toInt32(fromBytes.readRemaining());
            byte[] byteArray = new byte[length];
            fromBytes.copyTo(byteArray);

            return bytes(byteArray);
        }

        @NotNull
        @Override
        public WireOut rawBytes(@NotNull byte[] value) {
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
        public WireOut bytes(byte[] byteArray) {
            return bytes("!binary", byteArray);
        }

        @NotNull
        @Override
        public WireOut bytes(String type, byte[] byteArray) {
            prependSeparator();
            typePrefix(type);
            append(Base64.getEncoder().encodeToString(byteArray));
            append(leaf ? SPACE : END_FIELD);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(String type, BytesStore bytesStore) {
            prependSeparator();
            typePrefix(type);
            append(Base64.getEncoder().encodeToString(bytesStore.toByteArray()));
            append(END_FIELD);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            prependSeparator();
            bytes.append(u8);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            prependSeparator();
            bytes.append(i16);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            prependSeparator();
            bytes.append(u16);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            prependSeparator();
            StringBuilder sb = acquireStringBuilder();
            sb.appendCodePoint(codepoint);
            text(sb);
            sep = empty();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            prependSeparator();
            bytes.append(i32);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            prependSeparator();
            bytes.append(u32);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            prependSeparator();
            bytes.append(i64);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int64_0x(long i64) {
            prependSeparator();
            bytes.writeUnsignedByte('0')
                    .writeUnsignedByte('x')
                    .appendBase(i64, 16);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            TextLongArrayReference.write(bytes, capacity);
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity, @NotNull LongArrayValues values) {
            long pos = bytes.writePosition();
            TextLongArrayReference.write(bytes, capacity);
            ((Byteable) values).bytesStore(bytes, pos, bytes.writePosition() - pos);
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut float32(float f) {
            prependSeparator();
            bytes.append(f);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
            prependSeparator();
            bytes.append(d);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut time(@NotNull LocalTime localTime) {
            prependSeparator();
            append(localTime.toString());
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            prependSeparator();
            append(zonedDateTime.toString());
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            prependSeparator();
            append(localDate.toString());
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public ValueOut typePrefix(@NotNull CharSequence typeName) {
            prependSeparator();
            bytes.writeUnsignedByte('!');
            append(typeName);
            bytes.writeUnsignedByte(' ');
            sep = BytesStore.empty();
            return this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, Class type) {
            prependSeparator();
            append(TYPE);
            typeTranslator.accept(type, bytes);
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull CharSequence type) {
            prependSeparator();
            append(TYPE);
            escape(type);
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut uuid(@NotNull UUID uuid) {
            prependSeparator();
            append(sep);
            append(uuid.toString());
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            prependSeparator();
            TextIntReference.write(bytes, value);
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value, IntValue intValue) {
            if (!TextIntReference.class.isInstance(intValue))
                throw new IllegalArgumentException();
            prependSeparator();
            long offset = bytes.writePosition();
            TextIntReference.write(bytes, value);
            long length = bytes.writePosition() - offset;
            ((Byteable) intValue).bytesStore(bytes, offset, length);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            prependSeparator();
            TextLongReference.write(bytes, value);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value, LongValue longValue) {
            if (!TextLongReference.class.isInstance(longValue))
                throw new IllegalArgumentException();
            prependSeparator();
            long offset = bytes.writePosition();
            TextLongReference.write(bytes, value);
            long length = bytes.writePosition() - offset;
            ((Byteable) longValue).bytesStore(bytes, offset, length);
            elementSeparator();
            return wireOut();
        }

        @NotNull
        @Override
        public <T> WireOut sequence(T t, BiConsumer<T, ValueOut> writer) {
            pushState();
            bytes.writeUnsignedByte('[');
            newLine();
            long pos = bytes.readPosition();
            writer.accept(t, this);
            addNewLine(pos);

            popState();
            indent();
            bytes.writeUnsignedByte(']');
            endField();
            return wireOut();
        }

        protected void addNewLine(long pos) {
            if (bytes.writePosition() > pos + 1)
                bytes.writeUnsignedByte('\n');
        }

        protected void newLine() {
            sep = NEW_LINE;
        }

        @Override
        public WireOut array(@NotNull WriteValue writer, Class arrayType) {
            if (arrayType == String[].class) append("!String[] ");
            else {
                bytes.writeUnsignedByte('!');
                append(arrayType.getName());
                bytes.writeUnsignedByte(' ');
            }
            return sequence(writer);
        }

        protected void popState() {
            sep = seps.remove(seps.size() - 1);
            indentation--;
            leaf = false;
        }

        protected void pushState() {
            indentation++;
            seps.add(sep);
            sep = BytesStore.empty();
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) {
            if (bytes.writePosition() == 0) {
                object.writeMarshallable(TextWire.this);
                return TextWire.this;
            }
            boolean wasLeaf = leaf;
            if (!wasLeaf)
                pushState();

            prependSeparator();
            bytes.writeUnsignedByte('{');
            if (wasLeaf) sep = SPACE;
            else newLine();

            object.writeMarshallable(TextWire.this);
            BytesStore popSep = null;
            if (wasLeaf) {
                leaf = false;
            } else if (seps.size() > 0) {
                popSep = seps.get(seps.size() - 1);
                popState();
                sep = NEW_LINE;
            }
            if (sep.startsWith(',')) {
                append(sep, 1, sep.length() - 1);
                if (!wasLeaf)
                    indent();

            } else {
                prependSeparator();
            }
            bytes.writeUnsignedByte('}');
            if (popSep != null)
                sep = popSep;
            if (indentation == 0) {
                newLine();
                append(sep);
                sep = empty();

            } else {
                elementSeparator();
            }
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut map(@NotNull final Map map) {
            if (allStringKeys(map))
                stringKeys(map);
            else
                objectKeys(map);
            return TextWire.this;
        }

        private boolean allStringKeys(Map map) {
            for (Object o : map.keySet()) {
                if (!(o instanceof String))
                    return false;
            }
            return true;
        }

        private void stringKeys(Map map) {
            marshallableAsMap(map, Object.class, false);
        }

        private void objectKeys(@NotNull Map map) {
            typePrefix(SEQ_MAP);
            bytes.writeUnsignedByte('[');
            pushState();
            endField();
            map.forEach((k, v) -> {
                prependSeparator();
                append("{ key: ");
                leaf();
                object2(k);
                leaf = false;
                elementSeparator();
                prependSeparator();
                append("  value: ");
                leaf();
                object2(v);
                leaf = false;
                bytes.writeUnsignedByte(' ');
                bytes.writeUnsignedByte('}');
                elementSeparator();
            });
            popState();
            endField();
            prependSeparator();
            bytes.writeUnsignedByte(']');
            endField();
        }

        protected void endField() {
            sep = END_FIELD;
        }

        private void object2(Object v) {
            if (v instanceof CharSequence)
                text((CharSequence) v);
            else if (v instanceof WriteMarshallable)
                typedMarshallable((WriteMarshallable) v);
            else if (v == null)
                append("!" + NULL);
            else
                text(String.valueOf(v));
        }

        @NotNull
        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            typePrefix(SEQ_MAP);
            map.forEach((k, v) -> sequence(w -> w.marshallable(m -> m
                    .write(() -> "key").typedMarshallable(k)
                    .write(() -> "value").typedMarshallable(v))));
            return wireOut();
        }

        protected void fieldValueSeperator() {
            bytes.writeUnsignedByte(':');
            bytes.writeUnsignedByte(' ');
        }

        @NotNull
        public ValueOut write() {
            append(sep);
            bytes.writeUnsignedByte('"');
            bytes.writeUnsignedByte('"');
            fieldValueSeperator();
            sep = empty();
            return this;
        }

        @NotNull
        public ValueOut write(@NotNull WireKey key) {
            return write(key.name());
        }

        @NotNull
        public ValueOut write(@NotNull CharSequence name) {
            prependSeparator();
            escape(name);
            fieldValueSeperator();
            return this;
        }

        public void writeComment(@NotNull CharSequence s) {
            prependSeparator();
            append(sep);
            bytes.writeUnsignedByte('#');
            bytes.writeUnsignedByte(' ');
            append(s);
            bytes.writeUnsignedByte('\n');
            sep = EMPTY_AFTER_COMMENT;
        }

    }

    class TextValueIn implements ValueIn {
        final ValueInStack stack = new ValueInStack();

        @Override
        public void resetState() {
            stack.reset();
        }

        public void pushState() {
            stack.push();
        }

        public void popState() {
            stack.pop();
        }

        public ValueInState curr() {
            return stack.curr();
        }

        @Override
        public String text() {
            CharSequence cs = textTo0(acquireStringBuilder());
            return cs == null ? null : WireInternal.INTERNER.intern(cs);
        }

        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder sb) {
            CharSequence cs = textTo0(sb);
            if (cs == null)
                return null;
            if (cs != sb) {
                sb.setLength(0);
                sb.append(cs);
            }
            return sb;
        }

        @Nullable
        @Override
        public Bytes textTo(@NotNull Bytes bytes) {
            CharSequence cs = textTo0(bytes);
            if (cs == null)
                return null;
            if (cs != bytes) {
                bytes.clear();
                bytes.writeUtf8(cs);
            }
            return bytes;
        }

        @Nullable
        <ACS extends Appendable & CharSequence> CharSequence textTo0(@NotNull ACS a) {
            consumePadding();
            int ch = peekCode();
            CharSequence ret = a;

            switch (ch) {
                case '{': {
                    final long len = readLength();
                    try {
                        a.append(Bytes.toString(bytes, bytes.readPosition(), len));
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                    bytes.readSkip(len);

                    // read the next comma
                    bytes.skipTo(StopCharTesters.COMMA_STOP);

                    return a;

                }
                case '"': {
                    bytes.readSkip(1);
                    if (use8bit)
                        bytes.parse8bit(a, getEscapingQuotes());
                    else
                        bytes.parseUtf8(a, getEscapingQuotes());
                    unescape(a);
                    int code = peekCode();
                    if (code == '"')
                        readCode();
                    break;

                }
                case '\'': {
                    bytes.readSkip(1);
                    if (use8bit)
                        bytes.parse8bit(a, getEscapingSingleQuotes());
                    else
                        bytes.parseUtf8(a, getEscapingSingleQuotes());
                    unescape(a);
                    int code = peekCode();
                    if (code == '\'')
                        readCode();
                    break;

                }
                case '!': {
                    bytes.readSkip(1);
                    StringBuilder sb = acquireStringBuilder();
                    parseWord(sb);
                    if (StringUtils.isEqual(sb, "!null")) {
                        textTo(sb);
                        ret = null;
                    } else if (StringUtils.isEqual(sb, "snappy")) {
                        textTo(sb);
                        try {
                            //todo needs to be made efficient
                            byte[] decodedBytes = Base64.getDecoder().decode(sb.toString().getBytes());
                            String csq = Snappy.uncompressString(decodedBytes);
                            ret = acquireStringBuilder().append(csq);
                        } catch (IOException e) {
                            throw new AssertionError(e);
                        }
                    } else {
                        // ignore the type.
                        if (a instanceof StringBuilder) {
                            textTo((StringBuilder) a);
                        } else {
                            textTo(sb);
                            ret = sb;
                        }
                    }
                    break;
                }

                default: {
                    if (bytes.readRemaining() > 0) {
                        if (a instanceof Bytes || use8bit)
                            bytes.parse8bit(a, getEscapingEndOfText());
                        else
                            bytes.parseUtf8(a, getEscapingEndOfText());

                    } else {
                        AppendableUtil.setLength(a, 0);
                    }
                    // trim trailing spaces.
                    while (a.length() > 0)
                        if (Character.isWhitespace(a.charAt(a.length() - 1)))
                            AppendableUtil.setLength(a, a.length() - 1);
                        else
                            break;
                    break;
                }
            }

            int prev = peekBack();
            if (prev == ':' || prev == '#' || prev == '}')
                bytes.readSkip(-1);
            return ret;
        }

        protected int peekBack() {
            while (bytes.readPosition() >= bytes.start()) {
                int prev = bytes.readUnsignedByte(bytes.readPosition() - 1);
                if (prev != ' ') {
                    if (prev == '\n' || prev == '\r') {
                        TextWire.this.lineStart = bytes.readPosition();
                    }
                    return prev;
                }
                bytes.readSkip(-1);
            }
            return -1;
        }

        @NotNull
        @Override
        public WireIn bytesMatch(@NotNull BytesStore compareBytes, BooleanConsumer consumer) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull Bytes toBytes) {
            return bytes(toBytes::write);
        }

        @Nullable
        @Override
        public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
            return bytes(bytes -> {
                long capacity = bytes.readRemaining();
                Bytes<Void> bytes2 = Bytes.allocateDirect(capacity);
                bytes2.write(bytes);
                toBytes.set(bytes2.address(bytes2.start()), capacity);
            });
        }

        @NotNull
        public WireIn bytes(@NotNull ReadBytesMarshallable bytesConsumer) {
            consumePadding();

            // TODO needs to be made much more efficient.
            StringBuilder sb = acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.readSkip(1);
                parseWord(sb);
                byte[] uncompressed = Compression.uncompress(sb, TextWire.this, t -> {
                    StringBuilder sb2 = acquireStringBuilder();
                    AppendableUtil.setLength(sb2, 0);
                    t.parseWord(sb2);
                    byte[] decode = Base64.getDecoder().decode(sb2.toString());
                    return decode;
                });
                if (uncompressed != null) {
                    bytesConsumer.readMarshallable(Bytes.wrapForRead(uncompressed));

                } else if (StringUtils.isEqual(sb, "!null")) {
                    bytesConsumer.readMarshallable(null);
                    parseWord(sb);
                } else {
                    throw new IORuntimeException("Unsupported type=" + sb);
                }
            } else {
                textTo(sb);
                bytesConsumer.readMarshallable(Bytes.wrapForRead(sb.toString().getBytes()));
            }
            return TextWire.this;
        }

        @Nullable
        public byte[] bytes() {
            consumePadding();
            // TODO needs to be made much more efficient.
            StringBuilder sb = acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.readSkip(1);
                parseWord(sb);

                if ("byte[]".contentEquals(sb)) {
                    bytes.readSkip(1);
                    parseWord(sb);
                }

                byte[] bytes = Compression.uncompress(sb, this, t -> {
                    StringBuilder sb0 = acquireStringBuilder();
                    AppendableUtil.setLength(sb0, 0);
                    parseWord(sb0);
                    return Base64.getDecoder().decode(WireInternal.INTERNER.intern(sb));
                });
                if (bytes != null)
                    return bytes;

                if ("!null".contentEquals(sb)) {
                    parseWord(sb);
                    return null;
                }


                throw new IllegalStateException("unsupported type=" + sb);

            } else {
                textTo(sb);
                // todo fix this.
                return sb.toString().getBytes();
            }
        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return TextWire.this;
        }

        @Override
        public long readLength() {
            consumePadding();
            long start = bytes.readPosition();
            try {
                consumePadding();
                int code = readCode();
                switch (code) {
                    case '{': {
                        int count = 1;
                        for (; ; ) {
                            byte b = bytes.readByte();
                            if (b == '{')
                                count += 1;
                            else if (b == '}') {
                                count -= 1;
                                if (count == 0)
                                    return bytes.readPosition() - start;
                            } else if (b == 0) {
                                return bytes.readPosition() - start - 1;
                            }
                            // do nothing
                        }
                    }

                    case '-': {
                        for (; ; ) {
                            byte b = bytes.readByte();
                            if (b < ' ')
                                return bytes.readLimit() - start - 1;
                            // do nothing
                        }
                    }

                    default:
                        // TODO needs to be made much more efficient.
                        bytes();
                        return bytes.readPosition() - start;
                }
            } finally {
                bytes.readPosition(start);
            }
        }

        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            try {
                consumePadding();
                int code = peekCode();
                switch (code) {
                    case '{': {
                        bytes.readSkip(1);
                        int count = 1;
                        for (; ; ) {
                            int b = bytes.readByte();
                            if (b == '{') {
                                count += 1;
                            } else if (b == '}') {
                                count -= 1;
                                if (count == 0)
                                    return bytes.readPosition() - start;
                            } else if (b == 0) {
                                return bytes.readPosition() - start - 1;
                            }
                            // do nothing
                        }
                    }

                    default:
                        consumeValue();
                        return bytes.readPosition() - start;
                }
            } finally {
                bytes.readPosition(start);
            }
        }

        private void consumeValue() {
            consumePadding();
            StringBuilder sb = acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.readSkip(1);
                parseWord(sb);
                parseWord(sb);

            } else {
                textTo(sb);
            }
        }

        @NotNull
        @Override
        public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag) {
            consumePadding();

            StringBuilder sb = acquireStringBuilder();
            if (textTo(sb) == null) {
                tFlag.accept(t, null);
                return TextWire.this;
            }

            tFlag.accept(t, StringUtils.isEqual(sb, "true"));
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
            consumePadding();
            tb.accept(t, (byte) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (short) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (short) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (int) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (int) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumePadding();
            tl.accept(t, bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumePadding();
            tl.accept(t, bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            consumePadding();
            tf.accept(t, (float) bytes.parseDouble());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            consumePadding();
            td.accept(t, bytes.parseDouble());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
            consumePadding();
            StringBuilder sb = acquireStringBuilder();
            textTo(sb);
            setLocalTime.accept(t, LocalTime.parse(WireInternal.INTERNER.intern(sb)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
            consumePadding();
            StringBuilder sb = acquireStringBuilder();
            textTo(sb);
            tZonedDateTime.accept(t, ZonedDateTime.parse(WireInternal.INTERNER.intern(sb)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
            consumePadding();
            StringBuilder sb = acquireStringBuilder();
            textTo(sb);
            tLocalDate.accept(t, LocalDate.parse(WireInternal.INTERNER.intern(sb)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
            consumePadding();
            StringBuilder sb = acquireStringBuilder();
            textTo(sb);
            tuuid.accept(t, UUID.fromString(WireInternal.INTERNER.intern(sb)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter) {
            consumePadding();
            if (!(values instanceof TextLongArrayReference)) {
                setter.accept(t, values = new TextLongArrayReference());
            }
            Byteable b = (Byteable) values;
            long length = TextLongArrayReference.peakLength(bytes, bytes.readPosition());
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@Nullable LongValue value) {
            consumePadding();
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumePadding();
            if (peekCode() == ',')
                bytes.readSkip(1);
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter) {
            if (!(value instanceof TextLongReference)) {
                setter.accept(t, value = new TextLongReference());
            }
            return int64(value);
        }

        @NotNull
        @Override
        public <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter) {
            consumePadding();
            if (!(value instanceof TextIntReference)) {
                setter.accept(t, value = new TextIntReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumePadding();
            if (peekCode() == ',')
                bytes.readSkip(1);
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            consumePadding();
            char code = (char) readCode();
            if (code != '[')
                throw new IORuntimeException("Unsupported type " + code + " (" + code + ")");

            // this code was added to support empty sets
            consumePadding();
            code = (char) peekCode();
            if (code == ']')
                return TextWire.this;

            tReader.accept(t, TextWire.this.valueIn);

            consumePadding();
            code = (char) readCode();
            if (code != ']')
                throw new IORuntimeException("Expected a ] but got " + code + " (" + code + ")");
            return TextWire.this;
        }

        @Override
        public boolean hasNext() {
            return bytes.readRemaining() > 0;
        }

        @Override
        public boolean hasNextSequenceItem() {
            consumePadding();
            int ch = peekCode();
            if (ch == ',') {
                bytes.readSkip(1);
                return true;
            }
            return ch > 0 && ch != ']';
        }

        @Override
        public <T> T applyToMarshallable(@NotNull Function<WireIn, T> marshallableReader) {
            pushState();
            consumePadding();
            int code = peekCode();
            if (code != '{')
                throw new IORuntimeException("Unsupported type " + (char) code);

            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            try {
                // ensure that you can read past the end of this marshable object
                final long newLimit = position - 1 + len;
                bytes.readLimit(newLimit);
                bytes.readSkip(1); // skip the {
                consumePadding();
                return marshallableReader.apply(TextWire.this);
            } finally {
                bytes.readLimit(limit);

                consumePadding();
                code = readCode();
                popState();
                if (code != '}')
                    throw new IORuntimeException("Unterminated { while reading marshallable "
                            + "bytes=" + Bytes.toString(bytes)
                    );
            }
        }

        @NotNull
        @Override
        public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
            consumePadding();
            int code = peekCode();
            StringBuilder sb = acquireStringBuilder();
            sb.setLength(0);
            if (code == -1) {
                sb.append("java.lang.Object");
            } else if (code == '!') {
                readCode();

                parseUntil(sb, TextStopCharTesters.END_OF_TYPE);
            }
            return this;
        }

        @Override
        public Class typePrefix() {
            consumePadding();
            int code = peekCode();
            if (code == '!') {
                readCode();

                StringBuilder sb = acquireStringBuilder();
                sb.setLength(0);
                parseUntil(sb, TextStopCharTesters.END_OF_TYPE);
                try {
                    return classLookup().forName(sb);
                } catch (ClassNotFoundException e) {
                    throw new IORuntimeException(e);
                }
            }
            return Object.class;
        }

        @Override
        public boolean isTyped() {
            consumePadding();
            int code = peekCode();
            return code == '!';
        }

        @NotNull
        String stringForCode(int code) {
            return code < 0 ? "Unexpected end of input" : "'" + (char) code + "'";
        }

        @NotNull
        @Override
        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer)
                throws IORuntimeException, BufferUnderflowException {
            consumePadding();
            int code = readCode();
            if (!peekStringIgnoreCase("type "))
                throw new UnsupportedOperationException(stringForCode(code));
            bytes.readSkip("type ".length());
            StringBuilder sb = acquireStringBuilder();
            parseUntil(sb, TextStopCharTesters.END_OF_TYPE);
            classNameConsumer.accept(t, sb);
            return TextWire.this;
        }

        @Override
        public <T> Class<T> typeLiteral() throws IORuntimeException, BufferUnderflowException {
            consumePadding();
            int code = readCode();
            if (!peekStringIgnoreCase("type "))
                throw new UnsupportedOperationException(stringForCode(code));
            bytes.readSkip("type ".length());
            StringBuilder sb = acquireStringBuilder();
            parseUntil(sb, TextStopCharTesters.END_OF_TYPE);
            try {
                return classLookup().forName(sb);
            } catch (ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) {
            if (indentation() == 0) {
                object.readMarshallable(TextWire.this);
                return TextWire.this;
            }
            pushState();
            consumePadding();
            int code = peekCode();
            if (code == '!') {
                typePrefix(null, (o, x) -> { /* sets acquireStringBuilder(); */});
            } else if (code != '{') {
                throw new IORuntimeException("Unsupported type " + stringForCode(code));
            }

            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            final long newLimit = position - 1 + len;
            try {
                // ensure that you can read past the end of this marshable object

                bytes.readLimit(newLimit);
                bytes.readSkip(1); // skip the {
                consumePadding();
                object.readMarshallable(TextWire.this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
                popState();
            }

            consumePadding();
            code = readCode();
            if (code != '}')
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + ",code='" + (char) code + "', bytes=" + Bytes.toString(bytes, 1024)
                );
            return TextWire.this;
        }

        @NotNull
        public Demarshallable demarshallable(@NotNull Class clazz) {
            pushState();
            consumePadding();
            int code = peekCode();
            if (code == '!') {
                typePrefix(null, (o, x) -> { /* sets acquireStringBuilder(); */});
            } else if (code != '{') {
                throw new IORuntimeException("Unsupported type " + stringForCode(code));
            }

            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            final long newLimit = position - 1 + len;
            Demarshallable object;
            try {
                // ensure that you can read past the end of this marshable object

                bytes.readLimit(newLimit);
                bytes.readSkip(1); // skip the {
                consumePadding();

                object = Demarshallable.newInstance(clazz, TextWire.this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
                popState();
            }

            consumePadding();
            code = readCode();
            if (code != '}')
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + ",code='" + (char) code + "', bytes=" + Bytes.toString(bytes, 1024)
                );
            return object;
        }

        @Nullable
        public <T> T typedMarshallable() {
            try {
                consumePadding();
                int code = peekCode();
                if (code < 0)
                    throw new IllegalStateException("Cannot read as a ReadMarshallable " +
                            "" + bytes.toDebugString());
                StringBuilder sb = acquireStringBuilder();
                if (code != '!')
                    throw new ClassCastException("Cannot convert to ReadMarshallable. " + bytes.toDebugString());

                readCode();
                parseUntil(sb, TextStopCharTesters.END_OF_TYPE);

                if (StringUtils.isEqual(sb, "!null")) {
                    text();
                    return null;
                }


                if (StringUtils.isEqual(sb, "!binary")) {
                    bytesStore();
                    return null;
                }

                // its possible that the object that you are allocating may not have a
                // default constructor
                final Class clazz = classLookup().forName(sb);

                if (ReadMarshallable.class.isAssignableFrom(clazz)) {

                    Class<ReadMarshallable> clazz1 = (Class<ReadMarshallable>) clazz;
                    final ReadMarshallable m = ObjectUtils.newInstance(clazz1);

                    marshallable(m);
                    return readResolve(m);

                } else if (Demarshallable.class.isAssignableFrom(clazz)) {

                    return (T) demarshallable(clazz);

                } else {
                    throw new ClassCastException("Cannot convert " + sb + " to ReadMarshallable.");
                }
            } catch (Exception e) {
                throw new IORuntimeException(e);
            }
        }

        @Nullable
        @Override
        public <K, V> Map<K, V> map(@NotNull final Class<K> kClazz,
                                    @NotNull final Class<V> vClass,
                                    @NotNull final Map<K, V> usingMap) {
            consumePadding();
            usingMap.clear();

            StringBuilder sb = acquireStringBuilder();
            int code = peekCode();
            if (code == '!') {
                return typedMap(kClazz, vClass, usingMap, sb);
            } else if (code == '{') {
                return marshallableAsMap(Object.class, () -> (Map) usingMap);
            }
            return usingMap;
        }

        @Nullable
        private <K, V> Map<K, V> typedMap(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap, StringBuilder sb) {
            parseUntil(sb, StopCharTesters.SPACE_STOP);
            String str = WireInternal.INTERNER.intern(sb);

            if (("!!null").contentEquals(sb)) {
                text();
                return null;

            } else if (("!" + SEQ_MAP).contentEquals(sb)) {
                consumePadding();
                int start = readCode();
                if (start != '[')
                    throw new IORuntimeException("Unsupported start of sequence : " + (char) start);
                do {
                    marshallable(r -> {
                        final K k = r.read(() -> "key")
                                .object(kClazz);
                        final V v = r.read(() -> "value")
                                .object(vClass);
                        usingMap.put(k, v);
                    });
                } while (hasNextSequenceItem());
                return usingMap;

            } else {
                throw new IORuntimeException("Unsupported type :" + str);
            }
        }

        @Override
        public <K extends ReadMarshallable, V extends ReadMarshallable> void typedMap(@NotNull Map<K, V> usingMap) {
            consumePadding();
            usingMap.clear();

            StringBuilder sb = acquireStringBuilder();
            if (peekCode() == '!') {
                parseUntil(sb, StopCharTesters.SPACE_STOP);
                String str = WireInternal.INTERNER.intern(sb);
                if (SEQ_MAP.contentEquals(sb)) {
                    while (hasNext()) {
                        sequence(this, (o, s) -> s.marshallable(r -> {
                            try {
                                @SuppressWarnings("unchecked")
                                final K k = r.read(() -> "key").typedMarshallable();
                                @SuppressWarnings("unchecked")
                                final V v = r.read(() -> "value").typedMarshallable();
                                usingMap.put(k, v);
                            } catch (Exception e) {
                                LOG.error("", e);
                            }
                        }));
                    }
                } else {
                    throw new IORuntimeException("Unsupported type " + str);
                }
            }
        }

        @Override
        public boolean bool() {
            consumePadding();
            StringBuilder sb = acquireStringBuilder();
            if (textTo(sb) == null)
                throw new NullPointerException("value is null");

            return StringUtils.equalsCaseIgnore(sb, "true");
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
            consumePadding();
            valueIn.skipType();
            return bytes.parseLong();
        }

        @Override
        public double float64() {
            consumePadding();
            valueIn.skipType();
            return bytes.parseDouble();
        }

        private void skipType() {
            long peek = bytes.peekUnsignedByte();
            if (peek == '!') {
                StringBuilder sb = acquireStringBuilder();
                parseUntil(sb, TextStopCharTesters.END_OF_TYPE);
                consumePadding();
            }
        }

        @Override
        public float float32() {

            double d = float64();
            if ((double) (((float) d)) != d)
                throw new IllegalStateException("value=" + d + " can not be represented as a float");

            return (float) d;
        }

        /**
         * @return true if !!null "", if {@code true} reads the !!null "" up to the next STOP, if
         * {@code false} no  data is read  ( data is only peaked if {@code false} )
         */
        public boolean isNull() {
            consumePadding();

            if (peekStringIgnoreCase("!!null \"\"")) {
                bytes.readSkip("!!null \"\"".length());
                // discard the text after it.
                //  text(acquireStringBuilder());
                return true;
            }

            return false;
        }

        @Override
        @Nullable
        public <E> E object(@Nullable E using,
                            @NotNull Class<E> clazz) {
            return ObjectUtils.convertTo(clazz, object0(using, clazz));
        }

        @Nullable
        @Override
        public <T, E> WireIn object(@NotNull Class<E> clazz, T t, BiConsumer<T, E> e) {
            e.accept(t, ObjectUtils.convertTo(clazz, object0(null, clazz)));
            return TextWire.this;
        }

        @Nullable
        Object object0(@Nullable Object using, @NotNull Class clazz) {
            consumePadding();

            if (isNull())
                return null;

            if (byte[].class.isAssignableFrom(clazz))
                return bytes();

            if (BytesStore.class.isAssignableFrom(clazz)) {
                Bytes<byte[]> bytes = Bytes.wrapForRead(bytes());
                return bytes;
            }
            if (Demarshallable.class.isAssignableFrom(clazz)) {
                return Demarshallable.newInstance(clazz, wireIn());

            } else if (ReadMarshallable.class.isAssignableFrom(clazz)) {
                final Object v;
                if (using == null)
                    v = ObjectUtils.newInstance(clazz);
                else
                    v = using;

                valueIn.marshallable((ReadMarshallable) v);
                return readResolve(v);

            } else if (StringBuilder.class.isAssignableFrom(clazz)) {
                StringBuilder builder = (using == null)
                        ? acquireStringBuilder()
                        : (StringBuilder) using;
                valueIn.textTo(builder);
                return using;

            } else if (CharSequence.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return valueIn.text();

            } else if (Long.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return valueIn.int64();

            } else if (Double.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return valueIn.float64();
            } else if (Integer.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return valueIn.int32();

            } else if (Float.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return valueIn.float32();

            } else if (Short.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return valueIn.int16();

            } else if (Character.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                final String text = valueIn.text();
                if (text == null || text.length() == 0)
                    return null;
                return text.charAt(0);

            } else if (Byte.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return valueIn.int8();

            } else if (Map.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                final Map result = new HashMap();
                valueIn.map(result);
                return result;

            } else {
                return objectWithInferredType(clazz);
            }
        }

        Object objectWithInferredType(@NotNull Class clazz) {
            consumePadding();
            int code = peekCode();
            switch (code) {
                case '!':
                    return typedObject();
                case '-':
                    if (bytes.readByte(bytes.readPosition() + 1) == ' ')
                        return readList(indentation());
                    return valueIn.readNumber();
                case '[':
                    return readSequence(clazz);
                case '{':
                    return readMap();
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '+':
                    return valueIn.readNumber();
            }

            String text = valueIn.text();
            if (Enum.class.isAssignableFrom(clazz))
                return text;
            switch (text) {
                case "true":
                    return Boolean.TRUE;
                case "false":
                    return Boolean.FALSE;
                default:
                    return text;
            }
        }

        protected Object readNumber() {
            String s = text();
            String ss = s;
            if (s == null || s.length() > 40)
                return s;

            if (s.contains("_"))
                ss = s.replace("_", "");
            try {
                return Long.decode(ss);
            } catch (NumberFormatException ignored) {
            }
            try {
                return Double.parseDouble(ss);
            } catch (NumberFormatException ignored) {
            }
            try {
                if (s.length() == 7 && s.charAt(1) == ':')
                    return LocalTime.parse("0" + s);
                if (s.length() == 8 && s.charAt(2) == ':')
                    return LocalTime.parse(s);
            } catch (DateTimeParseException ignored) {
            }
            try {
                if (s.length() == 10)
                    return LocalDate.parse(s);
            } catch (DateTimeParseException ignored) {
            }
            try {
                if (s.length() >= 22)
                    return ZonedDateTime.parse(s);
            } catch (DateTimeParseException ignored) {
            }
            return s;
        }

        @NotNull
        private Object readSequence(@NotNull Class clazz) {
            if (clazz == Object[].class || clazz == Object.class) {
                //todo should this use reflection so that all array types can be handled
                List<Object> list = new ArrayList<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.object(Object.class));
                    }
                });
                return clazz == Object[].class ? list.toArray() : list;
            } else if (clazz == String[].class) {
                List<String> list = new ArrayList<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.text());
                    }
                });
                return list.toArray(new String[0]);
            } else if (clazz == List.class) {
                List<String> list = new ArrayList<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.text());
                    }
                });
                return list;
            } else if (clazz == Set.class) {
                Set<String> list = new HashSet<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.text());
                    }
                });
                return list;
            } else {
                throw new UnsupportedOperationException("Arrays of type "
                        + clazz + " not supported.");
            }
        }

        private Object typedObject() {
            readCode();
            StringBuilder sb = acquireStringBuilder();
            parseUntil(sb, TextStopCharTesters.END_OF_TYPE);
            final Class clazz2;
            try {
                clazz2 = classLookup().forName(sb);
            } catch (ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
            return object(null, clazz2);
        }

        @Override
        public String toString() {
            return TextWire.this.toString();
        }
    }

}
