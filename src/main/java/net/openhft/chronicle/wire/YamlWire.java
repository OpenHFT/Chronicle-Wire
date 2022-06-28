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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.ref.*;
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.BytesStore.empty;

/**
 * YAML Based wire format
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class YamlWire extends AbstractWire implements Wire {
    public static final BytesStore TYPE = BytesStore.from("!type ");
    static final String SEQ_MAP = "!seqmap";
    static final String BINARY_TAG = "!binary";
    static final String DATA_TAG = "!data";
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
        for (char ch : "?%&*@`0123456789+- ',#:{}[]|>!\\".toCharArray())
            STARTS_QUOTE_CHARS.set(ch);
        for (char ch : "?,#:{}[]|>\\".toCharArray())
            QUOTE_CHARS.set(ch);
        // make sure it has loaded.
        WireInternal.INTERNER.valueCount();
    }

    private final TextValueOut valueOut = createValueOut();
    private final TextValueIn valueIn = createValueIn();
    private final StringBuilder sb = new StringBuilder();
    private final YamlTokeniser yt;
    private final Map<String, Object> anchorValues = new HashMap<>();
    private DefaultValueIn defaultValueIn;
    private WriteDocumentContext writeContext;
    private ReadDocumentContext readContext;
    private boolean addTimeStamps = false;
    private boolean trimFirstCurly = true;

    public YamlWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
        yt = new YamlTokeniser(bytes);
        defaultValueIn = new DefaultValueIn(this);
    }

    public YamlWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    @NotNull
    public static YamlWire fromFile(String name) throws IOException {
        return new YamlWire(BytesUtil.readFile(name), true);
    }

    @NotNull
    public static YamlWire from(@NotNull String text) {
        return new YamlWire(Bytes.from(text));
    }

    public static String asText(@NotNull Wire wire) {
        assert wire.startUse();
        try {
            long pos = wire.bytes().readPosition();
            @NotNull Wire tw = Wire.newYamlWireOnHeap();
            wire.copyTo(tw);
            wire.bytes().readPosition(pos);
            return tw.toString();
        } finally {
            assert wire.endUse();
        }
    }

    private static <ACS extends Appendable & CharSequence> void unescape(@NotNull ACS sb,
                                                                         char blockQuote) {
        int end = 0;
        int length = sb.length();
        boolean skip = false;
        for (int i = 0; i < length; i++) {
            if (skip) {
                skip = false;
                continue;
            }

            char ch = sb.charAt(i);
            if (blockQuote == '\"' && ch == '\\' && i < length - 1) {
                char ch3 = sb.charAt(++i);
                switch (ch3) {
                    case '0':
                        ch = 0;
                        break;
                    case 'a':
                        ch = 7;
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'v':
                        ch = 0xB;
                        break;
                    case 'f':
                        ch = 0xC;
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 'e':
                        ch = 0x1B;
                        break;
                    case 'N':
                        ch = 0x85;
                        break;
                    case '_':
                        ch = 0xA0;
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
                    default:
                        ch = ch3;
                }
            }

            if (blockQuote == '\'' && ch == '\'' && i < length - 1) {
                char ch2 = sb.charAt(i + 1);
                if (ch2 == ch) {
                    skip = true;
                }
            }

            AppendableUtil.setCharAt(sb, end++, ch);
        }
        if (length != sb.length())
            throw new IllegalStateException("Length changed from " + length + " to " + sb.length() + " for " + sb);
        AppendableUtil.setLength(sb, end);
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean hintReadInputOrder() {
        // TODO Fix YamlTextWireTest for false.
        return true;
    }

    public boolean addTimeStamps() {
        return addTimeStamps;
    }

    public YamlWire addTimeStamps(boolean addTimeStamps) {
        this.addTimeStamps = addTimeStamps;
        return this;
    }

    @Override
    @NotNull
    public <T> T methodWriter(@NotNull Class<T> tClass, Class... additional) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.YAML,
                () -> newTextMethodWriterInvocationHandler(tClass));
        for (Class aClass : additional)
            builder.addInterface(aClass);
        builder.marshallableOut(this);
        return builder.build();
    }

    @NotNull
    TextMethodWriterInvocationHandler newTextMethodWriterInvocationHandler(Class... interfaces) {
        for (Class<?> anInterface : interfaces) {
            Comment c = anInterface.getAnnotation(Comment.class);
            if (c != null)
                writeComment(c.value());
        }
        return new TextMethodWriterInvocationHandler(interfaces[0], this);
    }

    @Override
    @NotNull
    public <T> MethodWriterBuilder<T> methodWriterBuilder(@NotNull Class<T> tClass) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.YAML,
                () -> newTextMethodWriterInvocationHandler(tClass));
        builder.marshallableOut(this);
        return builder;
    }

    @Override
    public @NotNull VanillaMethodReaderBuilder methodReaderBuilder() {
        return super.methodReaderBuilder().wireType(WireType.YAML);
    }

    @NotNull
    @Override
    public DocumentContext writingDocument(boolean metaData) {
        if (writeContext == null)
            useBinaryDocuments();
        writeContext.start(metaData);
        return writeContext;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        if (writeContext != null && writeContext.isOpen())
            return writeContext;
        return writingDocument(metaData);
    }

    @NotNull
    @Override
    public DocumentContext readingDocument() {
        initReadContext();
        return readContext;
    }

    protected void initReadContext() {
        if (readContext == null)
            useBinaryDocuments();
        readContext.start();
    }

    @NotNull
    public YamlWire useBinaryDocuments() {
        readContext = new BinaryReadDocumentContext(this, false);
        writeContext = new BinaryWriteDocumentContext(this);
        return this;
    }

    @NotNull
    public YamlWire useTextDocuments() {
        readContext = new TextReadDocumentContext(this);
        writeContext = new TextWriteDocumentContext(this);
        return this;
    }

    @NotNull
    @Override
    public DocumentContext readingDocument(long readLocation) {
        final long readPosition = bytes().readPosition();
        final long readLimit = bytes().readLimit();
        bytes().readPosition(readLocation);
        initReadContext();
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
        if (bytes.readRemaining() > (1024 * 1024)) {
            final long l = bytes.readLimit();
            try {
                bytes.readLimit(bytes.readPosition() + (1024 * 1024));
                return bytes + "..";
            } finally {
                bytes.readLimit(l);
            }
        } else
            return bytes.toString();
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        if (wire instanceof TextWire || wire instanceof YamlWire) {
            final Bytes<?> bytes0 = bytes();
            wire.bytes().write(this.bytes, yt.blockStart(), bytes0.readLimit() - yt.blockStart);
            this.bytes.readPosition(this.bytes.readLimit());
        } else {
            // TODO: implement copying
            throw new UnsupportedOperationException("Not implemented yet. Can only copy YamlWire format to the same format not " + wire.getClass());
        }
    }

    @Override
    public long readEventNumber() {
        final StringBuilder stringBuilder = acquireStringBuilder();
        readField(stringBuilder);
        try {
            return StringUtils.parseInt(stringBuilder, 10);
        } catch (NumberFormatException ignored) {
            return Long.MIN_VALUE;
        }
    }

    @NotNull
    @Override
    public ValueIn read() {
        readField(acquireStringBuilder());
        switch (yt.current()) {
            case NONE:
            case MAPPING_END:
                return defaultValueIn;
            default:
                return valueIn;
        }
    }

    @NotNull
    private StringBuilder acquireStringBuilder() {
        StringUtils.setCount(sb, 0);
        return sb;
    }

    @NotNull
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        startEventIfTop();
        if (yt.current() == YamlToken.MAPPING_KEY) {
            yt.next();
            if (yt.current() == YamlToken.TEXT) {
                String text = yt.text(); // May use sb so we need to reset it after
                sb.setLength(0);
                sb.append(text);
                unescape(sb, yt.blockQuote());
                yt.next();
            } else {
                throw new IllegalStateException(yt.toString());
            }
        } else {
            sb.setLength(0);
        }
        return sb;
    }

    @Nullable
    @Override
    public <K> K readEvent(@NotNull Class<K> expectedClass) {
        startEventIfTop();
        if (yt.current() == YamlToken.MAPPING_KEY) {
            YamlToken next = yt.next();
            if (next == YamlToken.MAPPING_KEY) {
                return readEvent(expectedClass);
            }
            if (next == YamlToken.TEXT) {
                sb.setLength(0);
                sb.append(yt.text());
                unescape(sb, yt.blockQuote);
                yt.next();
                return toExpected(expectedClass, sb);
            }
        }
        throw new UnsupportedOperationException(yt.toString());
    }

    @Override
    public boolean isNotEmptyAfterPadding() {
        consumePadding();
        switch (yt.current()) {
            case MAPPING_END:
            case DOCUMENT_END:
            case SEQUENCE_END:
            case NONE:
                return false;
            default:
                return true;
        }
    }

    @Nullable
    private <K> K toExpected(Class<K> expectedClass, StringBuilder sb) {
        return ObjectUtils.convertTo(expectedClass, WireInternal.INTERNER.intern(sb));
    }

    @Override
    public void consumePadding() {
        while (true) {
            switch (yt.current()) {
                case COMMENT:
                    commentListener.accept(yt.text());
                    // fall through
                case DIRECTIVE:
                case DIRECTIVES_END:
                    yt.next();
                    break;
                default:
                    return;
            }
        }
    }

    @Override
    @NotNull
    public String readingPeekYaml() {
        return "todo";
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return read(key.name().toString());
    }

    @NotNull
    @Override
    public ValueIn read(String keyName) {
        startEventIfTop();

        // check the keys we have already seen first.
        YamlKeys keys = yt.keys();
        int count = keys.count();
        if (count > 0) {
            long pos = yt.lastKeyPosition();
            long[] offsets = keys.offsets();
            int contextSize = yt.contextSize();
            for (int i = 0; i < count; i++) {
                yt.revertToContext(contextSize);
                YamlToken next = yt.rereadAndNext(offsets[i]);
                assert next == YamlToken.MAPPING_KEY;
                if (checkForMatch(keyName)) {
                    keys.removeIndex(i);
                    return valueIn;
                }
            }
            yt.revertToContext(contextSize);
            bytes.readPosition(pos);
            yt.next();
        }

        int minIndent = yt.topContext().indent;
        // go through remaining keys
        while (yt.current() == YamlToken.MAPPING_KEY) {
            long lastKeyPosition = yt.lastKeyPosition();
            if (checkForMatch(keyName))
                return valueIn;

            keys.push(lastKeyPosition);
            valueIn.consumeAny(minIndent);
        }

        return defaultValueIn;
    }

    public String dumpContext() {
        Wire yw = Wire.newYamlWireOnHeap();
        yw.getValueOut().list(yt.contexts, YamlTokeniser.YTContext.class);
        return yw.toString();
    }

    private boolean checkForMatch(@NotNull String keyName) {
        YamlToken next = yt.next();

        if (next == YamlToken.TEXT) {
            sb.setLength(0);
            sb.append(yt.text());
            unescape(sb, yt.blockQuote());
            yt.next();
        } else {
            throw new IllegalStateException(next.toString());
        }

        return (sb.length() == 0 || StringUtils.isEqual(sb, keyName));
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        startEventIfTop();
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
        sb.setLength(0);
        if (yt.current() == YamlToken.COMMENT) {
            // Skip the initial '#'
            YamlToken next = yt.next();
            sb.append(yt.text());
        }
        return this;
    }

    @Override
    public void clear() {
        reset();
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
        boolean wasLeft = valueOut.swapLeaf(true);
        try {
            return valueOut.write(expectedType, eventKey);
        } finally {
            valueOut.swapLeaf(wasLeft);
        }
    }

    @NotNull
    @Override
    public WireOut dropDefault(boolean dropDefault) {
        valueOut.dropDefault = dropDefault;
        return this;
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

    protected void consumeDocumentStart() {
        if (bytes.readRemaining() > 4) {
            long pos = bytes.readPosition();
            if (bytes.readByte(pos) == '-' && bytes.readByte(pos + 1) == '-' && bytes.readByte(pos + 2) == '-')
                bytes.readSkip(3);

            pos = bytes.readPosition();
            @NotNull String word = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
            switch (word) {
                case "!!data":
                case "!!data-not-ready":
                case "!!meta-data":
                case "!!meta-data-not-ready":
                    break;
                default:
                    bytes.readPosition(pos);
            }
        }

        if (yt.current() == YamlToken.NONE)
            yt.next();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return new TextLongReference();
    }

    @NotNull
    @Override
    public BooleanValue newBooleanReference() {
        return new TextBooleanReference();
    }

    @Override
    public boolean useSelfDescribingMessage(@NotNull CommonMarshallable object) {
        return true;
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

    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
        return new TextIntArrayReference();
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

    @NotNull
    private Map readMap(Class valueType) {
        Map map = new LinkedHashMap();
        if (yt.current() == YamlToken.MAPPING_START) {
            while (yt.next() == YamlToken.MAPPING_KEY) {
                if (yt.next() == YamlToken.TEXT) {
                    String key = yt.text();
                    Object o;
                    if (yt.next() == YamlToken.TEXT) {
                        o = ObjectUtils.convertTo(valueType, yt.text());
                    } else {
                        throw new UnsupportedOperationException(yt.toString());
                    }
                    map.put(key, o);
                } else {
                    throw new UnsupportedOperationException(yt.toString());
                }
            }
        } else {
            throw new UnsupportedOperationException(yt.toString());
        }
        return map;
    }

    /**
     * Please use {@code valueIn().object(o))} instead.
     */
    @Deprecated(/* To be removed in x.24 */)
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

    @Deprecated(/* To be removed in x.24 */)
    private void writeObject(Object o, int indentation) {
        writeTwo('-', ' ');
        indentation(indentation - 2);
        valueOut.object(o);
    }

    @Deprecated(/* To be removed in x.24 */)
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
    public YamlWire trimFirstCurly(boolean trimFirstCurly) {
        this.trimFirstCurly = trimFirstCurly;
        return this;
    }

    @Override
    public void startEvent() {
        consumePadding();
        if (yt.current() == YamlToken.MAPPING_START) {
            yt.next(Integer.MAX_VALUE);
            return;
        }
        throw new UnsupportedOperationException(yt.toString());
    }

    void startEventIfTop() {
        consumePadding();
        if (yt.contextSize() == 3)
            if (yt.current() == YamlToken.MAPPING_START)
                yt.next();
    }

    @Override
    public boolean isEndEvent() {
        consumePadding();
        return yt.current() == YamlToken.MAPPING_END;
    }

    @Override
    public void endEvent() {
        int minIndent = yt.topContext().indent;

        switch (yt.current()) {
            case MAPPING_END:
            case DOCUMENT_END:
            case NONE:
                break;
            default:
                valueIn.consumeAny(minIndent);
                break;
        }
        if (yt.current() == YamlToken.NONE) {
            yt.next(Integer.MIN_VALUE);
        } else {
            while (yt.current() == YamlToken.MAPPING_KEY) {
                yt.next();
                valueIn.consumeAny(minIndent);
            }
        }
        if (yt.current() == YamlToken.MAPPING_END || yt.current() == YamlToken.DOCUMENT_END || yt.current() == YamlToken.NONE) {
            yt.next(Integer.MIN_VALUE);
            return;
        }
        throw new UnsupportedOperationException(yt.toString());
    }

    public void reset() {
        readContext.reset();
        writeContext.reset();
        bytes.clear();
        sb.setLength(0);
        yt.reset();
        valueIn.resetState();
        valueOut.resetState();
        anchorValues.clear();
    }

    @Override
    public boolean readDocument(@Nullable ReadMarshallable metaDataConsumer, @Nullable ReadMarshallable dataConsumer) {
        valueIn.resetState();
        return super.readDocument(metaDataConsumer, dataConsumer);
    }

    @Override
    public boolean readDocument(long position, @Nullable ReadMarshallable metaDataConsumer, @Nullable ReadMarshallable dataConsumer) {
        valueIn.resetState();
        return super.readDocument(position, metaDataConsumer, dataConsumer);
    }

    class TextValueOut implements ValueOut, CommentAnnotationNotifier {
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
            return YamlWire.this.classLookup();
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
            yt.reset();
        }

        void prependSeparator() {
            append(sep);
            if (sep.endsWith('\n') || sep == EMPTY_AFTER_COMMENT)
                indent();
            sep = BytesStore.empty();
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
        public WireOut wireOut() {
            return YamlWire.this;
        }

        private void indent() {
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
        public WireOut bool(@Nullable Boolean flag) {
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
        public WireOut text(@Nullable CharSequence s) {
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
        public WireOut bytes(@Nullable BytesStore fromBytes) {
            if (dropDefault) {
                if (fromBytes == null)
                    return wireOut();
                writeSavedEventName();
            }
            if (isText(fromBytes))
                return text(fromBytes);

            int length = Maths.toInt32(fromBytes.readRemaining());
            @NotNull byte[] byteArray = new byte[length];
            fromBytes.copyTo(byteArray);

            return bytes(byteArray);
        }

        @NotNull
        @Override
        public WireOut rawBytes(@NotNull byte[] value) {
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
        public WireOut rawText(CharSequence value) {
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
        public WireOut bytes(byte[] byteArray) {
            if (dropDefault) {
                writeSavedEventName();
            }
            return bytes(BINARY_TAG, byteArray);
        }

        @NotNull
        @Override
        public WireOut bytes(@NotNull String type, byte[] byteArray) {
            if (dropDefault) {
                writeSavedEventName();
            }
            prependSeparator();
            typePrefix(type);
            if (getClass() != TextValueOut.class)
                bytes.append('"');
            append(Base64.getEncoder().encodeToString(byteArray));
            if (getClass() != TextValueOut.class)
                bytes.append('"');
            elementSeparator();
            endTypePrefix();

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(@NotNull String type, @Nullable BytesStore bytesStore) {
            if (bytesStore == null)
                return nu11();
            if (dropDefault) {
                writeSavedEventName();
            }
            prependSeparator();
            typePrefix(type);
            append(Base64.getEncoder().encodeToString(bytesStore.toByteArray()));
            endTypePrefix();
            append(END_FIELD);
            elementSeparator();

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
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
        public WireOut uint8checked(int u8) {
            if (dropDefault) {
                if (u8 == 0)
                    return wireOut();
                writeSavedEventName();
            }

            prependSeparator();
            bytes.append(u8);
            elementSeparator();

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            if (dropDefault) {
                if (i16 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(i16);
            elementSeparator();

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            if (dropDefault) {
                if (u16 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(u16);
            elementSeparator();

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
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
            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            if (dropDefault) {
                if (i32 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(i32);
            elementSeparator();

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            if (dropDefault) {
                if (u32 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(u32);
            elementSeparator();

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            if (dropDefault) {
                if (i64 == 0)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            bytes.append(i64);
            elementSeparator();
            // 2001 to 2100 best effort basis.
            boolean addTimeStamp = YamlWire.this.addTimeStamps && !leaf;
            if (addTimeStamp) {
                addTimeStamp(i64);
            }

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut int128forBinding(long i64x0, long i64x1, TwoLongValue longValue) {
            throw new UnsupportedOperationException(yt.toString());
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
        public WireOut int64_0x(long i64) {
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

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            if (dropDefault) {
                writeSavedEventName();
            }
            TextLongArrayReference.write(bytes, capacity);
            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity, @NotNull LongArrayValues values) {
            if (dropDefault) {
                writeSavedEventName();
            }
            long pos = bytes.writePosition();
            TextLongArrayReference.write(bytes, capacity);
            ((Byteable) values).bytesStore(bytes, pos, bytes.lengthWritten(pos));
            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut float32(float f) {
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

            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
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

            return YamlWire.this;
        }

        protected String doubleToString(double d) {
            return Double.toString(d);
        }

        protected String floatToString(float f) {
            return Float.toString(f);
        }

        @NotNull
        @Override
        public WireOut time(LocalTime localTime) {
            return asText(localTime);
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@Nullable ZonedDateTime zonedDateTime) {
            if (zonedDateTime == null)
                return nu11();
            if (dropDefault) {
                writeSavedEventName();
            }
            final String s = zonedDateTime.toString();
            return s.endsWith("]") ? text(s) : asText(s);
        }

        @NotNull
        @Override
        public WireOut date(LocalDate localDate) {
            return asText(localDate);
        }

        @NotNull
        @Override
        public WireOut dateTime(LocalDateTime localDateTime) {
            return asText(localDateTime);
        }

        @NotNull
        private WireOut asText(@Nullable Object stringable) {
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

            return YamlWire.this;
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
        public ValueOut typePrefix(@NotNull CharSequence typeName) {
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
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, Class type) {
            if (dropDefault) {
                if (type == null)
                    return wireOut();
                writeSavedEventName();
            }
            prependSeparator();
            append(TYPE);
            typeTranslator.accept(type, bytes);
            elementSeparator();
            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@Nullable CharSequence type) {
            if (dropDefault) {
                if (type == null)
                    return wireOut();
                writeSavedEventName();
            }
            if (type == null)
                return nu11();
            prependSeparator();
            append(TYPE);
            escape(type);
            elementSeparator();
            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut uuid(@NotNull UUID uuid) {
            return asText(uuid);
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            if (dropDefault) {
                writeSavedEventName();
            }
            prependSeparator();
            TextIntReference.write(bytes, value);
            elementSeparator();
            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value, @NotNull IntValue intValue) {
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
        public WireOut int64forBinding(long value) {
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
        public WireOut int64forBinding(long value, @NotNull LongValue longValue) {
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
        public WireOut boolForBinding(final boolean value, @NotNull final BooleanValue longValue) {
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
        public <T> WireOut sequence(T t, @NotNull BiConsumer<T, ValueOut> writer) {
            startBlock('[');
            boolean leaf = this.leaf;
            if (!leaf)
                newLine();
            else
                bytes.writeUnsignedByte(' ');
            long pos = bytes.writePosition();
            writer.accept(t, this);
            if (!leaf)
                addNewLine(pos);

            popState();
            this.leaf = leaf;
            if (!leaf)
                indent();
            else
                addSpace(pos);
            endBlock(']');
            return wireOut();
        }

        public void startBlock(char c) {
            if (dropDefault) {
                writeSavedEventName();
            }
            if (sep.isEmpty()) {
                if (!seps.isEmpty())
                    sep = leaf ? COMMA_SPACE : COMMA_NEW_LINE;

            } else {
                append(sep);
                indent();
                sep = EMPTY;
            }
            pushState();
            bytes.writeUnsignedByte(c);
        }

        @NotNull
        @Override
        public <T, K> WireOut sequence(T t, K kls, @NotNull TriConsumer<T, K, ValueOut> writer) {
            boolean leaf = this.leaf;
            startBlock('[');
            if (leaf)
                sep = SPACE;
            else
                newLine();
            long pos = bytes.readPosition();
            writer.accept(t, kls, this);
            if (leaf)
                addSpace(pos);
            else
                addNewLine(pos);

            popState();
            if (!leaf)
                indent();
            endBlock(']');
            return wireOut();
        }

        protected void endBlock(char c) {
            BytesUtil.combineDoubleNewline(bytes);
            bytes.writeUnsignedByte(c);
            elementSeparator();
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
        public WireOut marshallable(@NotNull WriteMarshallable object) {
            WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(object.getClass());
            boolean wasLeaf0 = leaf;
            if (indentation > 1 && wm.isLeaf())
                leaf = true;

            if (dropDefault) {
                writeSavedEventName();
            }
            if (trimFirstCurly && bytes.writePosition() == 0) {
                object.writeMarshallable(YamlWire.this);
                if (bytes.writePosition() == 0)
                    bytes.append("{}");
                return YamlWire.this;
            }
            boolean wasLeaf = leaf;
            startBlock('{');

            if (wasLeaf)
                afterOpen();
            else
                newLine();

            object.writeMarshallable(YamlWire.this);
            @Nullable BytesStore popSep = null;
            if (wasLeaf) {
                if (sep.endsWith(' '))
                    append(" ");
                leaf = false;
                popState();
            } else if (!seps.isEmpty()) {
                popSep = seps.get(seps.size() - 1);
                popState();
                newLine();
            }
            writeEndOfBlock(wasLeaf);
            endBlock('}');

            leaf = wasLeaf0;

            if (popSep != null)
                sep = popSep;

            elementSeparator();
            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull Serializable object) {
            if (dropDefault) {
                writeSavedEventName();
            }
            if (bytes.writePosition() == 0) {
                writeSerializable(object);
                return YamlWire.this;
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
            writeEndOfBlock(wasLeaf);
            BytesUtil.combineDoubleNewline(bytes);
            bytes.writeUnsignedByte(object instanceof Externalizable ? ']' : '}');
            if (popSep != null)
                sep = popSep;
            if (indentation == 0) {
                afterClose();

            } else {
                elementSeparator();
            }
            return YamlWire.this;
        }

        private void writeEndOfBlock(boolean wasLeaf) {
            if (sep.startsWith(',')) {
                char ch = sep.charAt(1);
                if (bytes.peekUnsignedByte(bytes.readPosition() - 1) != ch)
                    append(sep, 1, sep.length() - 1);
                if (!wasLeaf)
                    indent();

            } else {
                prependSeparator();
            }
        }

        private void writeSerializable(@NotNull Serializable object) {
            try {
                if (object instanceof Externalizable)
                    ((Externalizable) object).writeExternal(objectOutput());
                else
                    Wires.writeMarshallable(object, YamlWire.this);
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
        public WireOut map(@NotNull final Map map) {
            if (dropDefault) {
                writeSavedEventName();
            }
            marshallable(map, Object.class, Object.class, false);
            return YamlWire.this;
        }

        protected void endField() {
            sep = END_FIELD;
        }

        protected void fieldValueSeperator() {
            writeTwo(':', ' ');
        }

        @NotNull
        public ValueOut write() {
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
        public ValueOut write(@NotNull WireKey key) {
            if (dropDefault) {
                eventName = key.name().toString();
            } else {
                write(key.name());
            }
            return this;
        }

        @NotNull
        public ValueOut write(@NotNull CharSequence name) {
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
        public ValueOut write(Class expectedType, @NotNull Object objectKey) {
            if (dropDefault) {
                if (expectedType != String.class)
                    throw new UnsupportedOperationException(yt.toString());
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

    class TextValueIn implements ValueIn {
        @Override
        public ClassLookup classLookup() {
            return YamlWire.this.classLookup();
        }

        @Override
        public void resetState() {
            yt.reset();
            anchorValues.clear();
        }

        @Nullable
        @Override
        public String text() {
            @Nullable CharSequence cs = textTo0(acquireStringBuilder());
            return cs == null ? null : WireInternal.INTERNER.intern(cs);
        }

        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder sb) {
            sb.setLength(0);
            @Nullable CharSequence cs = textTo0(sb);
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
        public Bytes<?> textTo(@NotNull Bytes<?> bytes) {
            bytes.clear();
            if (yt.current() == YamlToken.TEXT) {
                bytes.clear();
                bytes.append(yt.text());
                yt.next();
            } else {
                throw new UnsupportedOperationException(yt.toString());
            }
            return bytes;
        }

        @Override
        public BracketType getBracketType() {
            switch (yt.current()) {
                default:
                    throw new UnsupportedOperationException(yt.toString());
                case DIRECTIVES_END:
                case TAG:
                case COMMENT:
                    yt.next();
                    return getBracketType();
                case MAPPING_START:
                    return BracketType.MAP;
                case SEQUENCE_START:
                    return BracketType.SEQ;
                case NONE:
                case MAPPING_KEY:
                case SEQUENCE_ENTRY:
                case STREAM_START:
                case TEXT:
                case LITERAL:
                    return BracketType.NONE;
            }
        }

        @Nullable
        Bytes<?> textTo0(@NotNull Bytes<?> a) {
            consumePadding();
            if (yt.current() == YamlToken.TEXT) {
                a.append(yt.text());
            } else {
                throw new UnsupportedOperationException(yt.toString());
            }
            return a;
        }

        @Nullable
        StringBuilder textTo0(@NotNull StringBuilder a) {
            consumePadding();
            if (yt.current() == YamlToken.SEQUENCE_ENTRY)
                yt.next();
            if (yt.current() == YamlToken.TEXT || yt.current() == YamlToken.LITERAL) {
                a.append(yt.text());
                if (yt.current() == YamlToken.TEXT)
                    unescape(a, yt.blockQuote());
                yt.next();
            } else if (yt.current() == YamlToken.TAG) {
                if (yt.isText("!null")) {
                    yt.next();
                    yt.next();
                    return null;
                }

                if (yt.isText(BINARY_TAG)) {
                    yt.next();
                    final byte[] arr = (byte[]) decodeBinary(byte[].class);
                    for (byte b : arr) {
                        a.append((char) b);
                    }
                    return a;
                }

                throw new UnsupportedOperationException(yt.toString());
            }
            return a;
        }

        @NotNull
        @Override
        public WireIn bytesMatch(@NotNull BytesStore compareBytes, BooleanConsumer consumer) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull BytesOut<?> toBytes) {
            toBytes.clear();
            return bytes(b -> toBytes.write((BytesStore) b));
        }

        @Nullable
        @Override
        public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
            return bytes(bytes -> {
                long capacity = bytes.readRemaining();
                Bytes<Void> bytes2 = Bytes.allocateDirect(capacity);
                bytes2.write((BytesStore) bytes);
                toBytes.set(bytes2.addressForRead(bytes2.start()), capacity);
            });
        }

        @Override
        @NotNull
        public WireIn bytes(@NotNull ReadBytesMarshallable bytesConsumer) {
            consumePadding();
            // TODO needs to be made much more efficient.
            @NotNull StringBuilder sb = acquireStringBuilder();
            if (yt.current() == YamlToken.TAG) {
                bytes.readSkip(1);
                yt.text(sb);
                yt.next();
                if (yt.current() != YamlToken.TEXT)
                    throw new UnsupportedOperationException(yt.toString());
                @Nullable byte[] uncompressed = Compression.uncompress(sb, yt, t -> {
                    @NotNull StringBuilder sb2 = acquireStringBuilder();
                    t.text(sb2);
                    return Base64.getDecoder().decode(sb2.toString());
                });
                if (uncompressed != null) {
                    Bytes<byte[]> bytes = Bytes.wrapForRead(uncompressed);
                    bytesConsumer.readMarshallable(bytes);
                    bytes.releaseLast();

                } else if (StringUtils.isEqual(sb, "!null")) {
                    bytesConsumer.readMarshallable(null);
                    yt.next();

                } else {
                    throw new IORuntimeException("Unsupported type=" + sb);
                }
            } else {
                textTo(sb);
                Bytes<byte[]> bytes = Bytes.wrapForRead(sb.toString().getBytes(ISO_8859_1));
                bytesConsumer.readMarshallable(bytes);
                bytes.releaseLast();
            }
            return YamlWire.this;
        }

        @Override
        public byte @Nullable [] bytes(byte[] using) {
            return (byte[])objectWithInferredType(using, SerializationStrategies.ANY_OBJECT, byte[].class);

        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return YamlWire.this;
        }

        @Override
        public long readLength() {
            return readLengthMarshallable();
        }

        @NotNull
        @Override
        public WireIn skipValue() {
            consumeAny(yt.topContext().indent);
            return YamlWire.this;
        }

        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            try {
                consumeAny(yt.topContext().indent);
                return bytes.readPosition() - start;
            } finally {
                bytes.readPosition(start);
            }
        }

        protected void consumeAny(int minIndent) {
            consumePadding();
            int indent2 = Math.max(yt.topContext().indent, minIndent);
            switch (yt.current()) {
                case SEQUENCE_ENTRY:
                case TAG:
                    yt.next(minIndent);
                    consumeAny(minIndent);
                    break;
                case MAPPING_START:
                    consumeMap(indent2);
                    break;
                case SEQUENCE_START:
                    consumeSeq(indent2);
                    break;
                case MAPPING_KEY:
                    yt.next(minIndent);
                    consumeAny(minIndent);
                    if (yt.current() != YamlToken.MAPPING_KEY && yt.current() != YamlToken.MAPPING_END)
                        consumeAny(minIndent);
                    break;
                case SEQUENCE_END:
                    yt.next(minIndent);
                    break;
                case TEXT:
                    yt.next(minIndent);
                    break;
                case MAPPING_END:
                case STREAM_START:
                case DOCUMENT_END:
                case NONE:
                    break;
                default:
                    throw new UnsupportedOperationException(yt.toString());
            }
        }

        private void consumeSeq(int minIndent) {
            assert yt.current() == YamlToken.SEQUENCE_START;
            yt.next(minIndent);
            while (true) {
                switch (yt.current()) {
                    case SEQUENCE_ENTRY:
                        yt.next(minIndent);
                        consumeAny(minIndent);
                        break;

                    case SEQUENCE_END:
                        yt.next(minIndent);
                        return;

                    default:
                        throw new IllegalStateException(yt.toString());
                }
            }
        }

        private void consumeMap(int minIndent) {
            // consume MAPPING_START
            yt.next(minIndent);
            while (yt.current() == YamlToken.MAPPING_KEY) {
                yt.next(minIndent); // consume KEY
                consumeAny(minIndent); // consume the key
                consumeAny(minIndent); // consume the value
            }
            if (yt.current() == YamlToken.NONE)
                yt.next(Integer.MIN_VALUE);
            if (yt.current() == YamlToken.MAPPING_END)
                yt.next(minIndent);
        }

        @NotNull
        @Override
        public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag) {
            consumePadding();

            final StringBuilder stringBuilder = acquireStringBuilder();
            if (textTo(stringBuilder) == null) {
                tFlag.accept(t, null);
                return YamlWire.this;
            }

            Boolean flag = stringBuilder.length() == 0 ? null : StringUtils.isEqual(stringBuilder, "true");
            tFlag.accept(t, flag);
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
            consumePadding();
            tb.accept(t, (byte) getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (short) getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (short) getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (int) getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (int) getALong());
            return YamlWire.this;
        }

        long getALong() {
            if (yt.current() == YamlToken.TEXT) {
                long l = yt.parseLong();
                yt.next();
                return l;
            }
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumePadding();
            tl.accept(t, getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumePadding();
            tl.accept(t, getALong());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            consumePadding();
            tf.accept(t, (float) getADouble());
            return YamlWire.this;
        }

        public double getADouble() {
            if (yt.current() == YamlToken.TEXT) {
                double v = yt.parseDouble();
                yt.next();
                return v;
            } else {
                throw new UnsupportedOperationException("yt:" + yt.current());
            }
        }

        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            consumePadding();
            td.accept(t, getADouble());
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            setLocalTime.accept(t, LocalTime.parse(WireInternal.INTERNER.intern(stringBuilder)));
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            tZonedDateTime.accept(t, ZonedDateTime.parse(WireInternal.INTERNER.intern(stringBuilder)));
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            tLocalDate.accept(t, LocalDate.parse(WireInternal.INTERNER.intern(stringBuilder)));
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            tuuid.accept(t, UUID.fromString(WireInternal.INTERNER.intern(stringBuilder)));
            return YamlWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter) {
            consumePadding();
            if (!(values instanceof TextLongArrayReference)) {
                values = new TextLongArrayReference();
            }
            @NotNull Byteable b = (Byteable) values;
            long length = TextLongArrayReference.peakLength(bytes, bytes.readPosition());
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            setter.accept(t, values);
            return YamlWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongValue value) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntValue value) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @Override
        public WireIn bool(@NotNull final BooleanValue value) {
            throw new UnsupportedOperationException(yt.toString());
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
            throw new UnsupportedOperationException(yt.toString());
        }

        @Override
        public <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            consumePadding();
            if (isNull()) {
                return false;
            }
            if (yt.current() == YamlToken.SEQUENCE_START) {
                int minIndent = yt.secondTopContext().indent;
                yt.next(Integer.MAX_VALUE);
                tReader.accept(t, YamlWire.this.valueIn);
                if (yt.current() == YamlToken.NONE)
                    yt.next(minIndent);
                if (yt.current() == YamlToken.SEQUENCE_END)
                    yt.next(minIndent);

            } else if (yt.current() == YamlToken.TEXT) {
                tReader.accept(t, YamlWire.this.valueIn);

            } else {
                throw new UnsupportedOperationException(yt.toString());
            }
            return true;
        }

        public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) {
            return sequence(list, buffer, bufferAdd);
        }

        @Override
        public <T> boolean sequence(@NotNull List<T> list, @NotNull List<T> buffer, @NotNull Supplier<T> bufferAdd) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @Override
        public boolean hasNext() {
            if (yt.current() == YamlToken.DOCUMENT_END)
                yt.next(Integer.MIN_VALUE);

            consumePadding();

            switch (yt.current()) {
                case SEQUENCE_END:
                case STREAM_END:
                case DOCUMENT_END:
                case MAPPING_END:
                case NONE:
                    return false;
                default:
                    return true;
            }
        }

        @Override
        public boolean hasNextSequenceItem() {
            switch (yt.current()) {
                case SEQUENCE_START:
                case TEXT:
                case SEQUENCE_ENTRY:
                    return true;
            }
            return false;
        }

        @Override
        public <T> T applyToMarshallable(@NotNull Function<WireIn, T> marshallableReader) {
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        @Override
        public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
            consumePadding();
            if (yt.current() == YamlToken.TAG) {
                ts.accept(t, yt.text());
                yt.next();

            } else {
                ts.accept(t, "java.lang.Object");
            }
            return this;
        }

        @Override
        public Class typePrefix() {
            if (yt.current() != YamlToken.TAG)
                return null;
            final StringBuilder stringBuilder = acquireStringBuilder();
            yt.text(stringBuilder);

            // Do not handle !!binary, do not resolve to BytesStore, do not consume tag
            if (BINARY_TAG.contentEquals(stringBuilder) || DATA_TAG.contains(stringBuilder))
                return null;

            try {
                yt.next();
                return classLookup().forName(stringBuilder);
            } catch (ClassNotFoundRuntimeException e) {
                Jvm.warn().on(getClass(), "Unable to find " + stringBuilder + " " + e);
                return null;
            }
        }

        @Override
        public Object typePrefixOrObject(Class tClass) {
            consumePadding();
            switch (yt.current()) {
                case TAG: {
                    Class<?> type = typePrefix();
                    return type;
                }
                default:
                    return null;
/*

                case MAPPING_START:
                    if (tClass == null || tClass == Object.class || tClass == Map.class) {
                        return readMap();
                    }
                    return marshallable(ObjectUtils.newInstance(tClass), SerializationStrategies.MARSHALLABLE);

                case SEQUENCE_START:
                    if (tClass == null || tClass == Object.class || tClass == List.class)
                        return readList(Object.class);
                    if (tClass == Set.class)
                        return readSet(tClass);
                    break;
                case TEXT:
                    return text();
*/
            }
        }

        @Override
        public boolean isTyped() {
            consumePadding();
            int code = bytes.peekUnsignedByte();
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
            throw new UnsupportedOperationException(yt.toString());
        }

        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            consumePadding();
            if (yt.current() == YamlToken.TAG) {
                if (yt.text().equals("type")) {
                    if (yt.next() == YamlToken.TEXT) {
                        Class aClass = classLookup().forName(yt.text());
                        yt.next();
                        return aClass;
                    }
                }
            }
            throw new UnsupportedOperationException(yt.toString());
        }

        @Nullable
        @Override
        public Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy)
                throws BufferUnderflowException, IORuntimeException {
            if (isNull()) {
                consumeAny(yt.topContext().indent);
                return null;
            }

            consumePadding();
            if (yt.current() == YamlToken.SEQUENCE_ENTRY) {
                yt.next();
                consumePadding();
            }
            switch (yt.current()) {
                case TAG:
                    typePrefix(null, (o, x) -> { /* sets acquireStringBuilder(); */});
                    break;

                case SEQUENCE_START:
                    Jvm.warn().on(getClass(), "Expected a {} but was blank for type " + object.getClass());
                    consumeAny(yt.secondTopContext().indent);
                    return object;

                case MAPPING_START:
                    wireIn().startEvent();

                    object = strategy.readUsing(null, object, this, BracketType.MAP);

                    try {
                        wireIn().endEvent();
                    } catch (UnsupportedOperationException uoe) {
                        throw new IORuntimeException("Unterminated { while reading marshallable " +
                                object + ",code='" + yt.current() + "', bytes=" + Bytes.toString(bytes, 1024)
                        );
                    }
                    return object;

                default:
                    break;
            }
            throw new UnsupportedOperationException(yt.toString());
        }

        @NotNull
        public Demarshallable demarshallable(@NotNull Class clazz) {
            consumePadding();
            switch (yt.current()) {
                case TAG:
                    yt.next();
                case MAPPING_START:
                    break;
                default:
                    throw new IORuntimeException("Unsupported type " + yt.current());
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

                object = Demarshallable.newInstance(clazz, YamlWire.this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
            }

            consumePadding();
            if (yt.current() != YamlToken.MAPPING_END)
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + ",code='" + yt.current() + "', bytes=" + Bytes.toString(bytes, 1024)
                );
            yt.next();
            return object;
        }

        @Override
        @Nullable
        public <T> T typedMarshallable() {
            return (T) objectWithInferredType(null, SerializationStrategies.ANY_NESTED, null);
        }

        @Nullable
        private <K, V> Map<K, V> map(@NotNull final Class<K> kClass,
                                     @NotNull final Class<V> vClass,
                                     @Nullable Map<K, V> usingMap) {
            consumePadding();
            if (usingMap == null)
                usingMap = new LinkedHashMap<>();
            else
                usingMap.clear();

            @NotNull StringBuilder sb = acquireStringBuilder();
            switch (yt.current()) {
                case TAG:
                    return typedMap(kClass, vClass, usingMap, sb);
                case MAPPING_START:
                    return marshallableAsMap(kClass, vClass, usingMap);
                case SEQUENCE_START:
                    return readAllAsMap(kClass, vClass, usingMap);
                default:
                    throw new IORuntimeException("Unexpected code " + yt.current());
            }
        }

        @Nullable
        private <K, V> Map<K, V> typedMap(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap, @NotNull StringBuilder sb) {
            yt.text(sb);
            yt.next();
            if (("!null").contentEquals(sb)) {
                text();
                return null;

            } else if (SEQ_MAP.contentEquals(sb)) {
                consumePadding();
                if (yt.current() != YamlToken.SEQUENCE_START)
                    throw new IORuntimeException("Unsupported start of sequence : " + yt.current());
                do {
                    marshallable(r -> {
                        @Nullable final K k = r.read(() -> "key")
                                .object(kClazz);
                        @Nullable final V v = r.read(() -> "value")
                                .object(vClass);
                        usingMap.put(k, v);
                    });
                } while (hasNextSequenceItem());
                return usingMap;

            } else {
                throw new IORuntimeException("Unsupported type :" + sb);
            }
        }

        @Override
        public boolean bool() {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            if (textTo(stringBuilder) == null)
                throw new NullPointerException("value is null");

            if (ObjectUtils.isTrue(stringBuilder))
                return true;
            if (ObjectUtils.isFalse(stringBuilder))
                return false;
            Jvm.debug().on(getClass(), "Unable to parse '" + stringBuilder + "' as a boolean flag, assuming false");
            return false;
        }

        @Override
        public byte int8() {
            long l = int64();
            if (l > Byte.MAX_VALUE || l < Byte.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Byte.MAX_VALUE/MIN_VALUE");
            return (byte) l;
        }

        @Override
        public short int16() {
            long l = int64();
            if (l > Short.MAX_VALUE || l < Short.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Short.MAX_VALUE/MIN_VALUE");
            return (short) l;
        }

        @Override
        public int int32() {
            long l = int64();
            if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Integer.MAX_VALUE/MIN_VALUE");
            return (int) l;
        }

        @Override
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
            if (yt.current() != YamlToken.TEXT) {
                Jvm.warn().on(getClass(), "Unable to read " + valueIn.object() + " as a long.");
                return 0;
            }

            return getALong();
        }

        @Override
        public double float64() {
            consumePadding();
            valueIn.skipType();
            if (yt.current() != YamlToken.TEXT) {
                Jvm.warn().on(getClass(), "Unable to read " + valueIn.object() + " as a long.");
                return 0;
            }
            return getADouble();
        }

        void skipType() {
            consumePadding();
            if (yt.current() == YamlToken.TAG) {
                yt.next();
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
        @Override
        public boolean isNull() {
            consumePadding();

            if (yt.current() == YamlToken.TAG && yt.isText("!null")) {
                consumeAny(0);
                return true;
            }

            return false;
        }

        @Override
        public Object objectWithInferredType(Object using, @NotNull SerializationStrategy strategy, Class type) {
            consumePadding();
            if (yt.current() == YamlToken.SEQUENCE_ENTRY)
                yt.next();
            @Nullable Object o = objectWithInferredType0(using, strategy, type);
            consumePadding();
            return o;
        }

        @Nullable
        Object objectWithInferredType0(Object using, @NotNull SerializationStrategy strategy, Class type) {
            if (yt.current() == YamlToken.TAG) {
                Class aClass = typePrefix();
                if (type == null || type == Object.class || type.isInterface())
                    type = aClass;
            }

            switch (yt.current()) {
                case MAPPING_START:
                    if (type != null) {
                        if (type == SortedMap.class && !(using instanceof SortedMap))
                            using = new TreeMap();
                        if (type == Object.class || Map.class.isAssignableFrom(type) || using instanceof Map)
                            return map(Object.class, Object.class, (Map) using);
                    }
                    return valueIn.object(using, type);

                case SEQUENCE_START:
                    return readSequence(type);

                case TEXT:
                case LITERAL:
                    Object o = valueIn.readNumberOrText();
                    return ObjectUtils.convertTo(type, o);

                case ANCHOR:
                    String alias = yt.text();
                    yt.next();
                    o = valueIn.object(using, type);
                    // Overwriting of anchor values is permitted
                    anchorValues.put(alias, o);
                    return o;

                case ALIAS:
                    alias = yt.text();
                    o = anchorValues.get(yt.text());
                    if (o == null)
                        throw new IllegalStateException("Unknown alias " + alias + " with no corresponding anchor");

                    yt.next();
                    return o;

                case NONE:
                    return null;

                case TAG:
                    final StringBuilder stringBuilder = acquireStringBuilder();
                    yt.text(stringBuilder);

                    if (BINARY_TAG.contentEquals(stringBuilder)) {
                        yt.next();
                        return decodeBinary(type);
                    }

                    // Intentional fall-through

                default:
                    throw new UnsupportedOperationException("Cannot determine what to do with " + yt.current());
            }
        }

        @Nullable
        protected Object readNumberOrText() {
            char bq = yt.blockQuote();
            @Nullable String s = text();
            if (yt.current() == YamlToken.LITERAL)
                return s;
            if (s == null
                    || bq != 0
                    || s.length() < 1
                    || s.length() > 40
                    || "0123456789.+-".indexOf(s.charAt(0)) < 0)
                return s;

            String ss = s;
            if (s.indexOf('_') >= 0)
                ss = ss.replace("_", "");

            // YAML octal notation
            if (s.startsWith("0o"))
                ss = "0" + s.substring(2);

            try {
                return Long.decode(ss);
            } catch (NumberFormatException fallback) {
                // fallback
            }
            try {
                return Double.parseDouble(ss);
            } catch (NumberFormatException fallback) {
                // fallback
            }
            try {
                if (s.length() == 7 && s.charAt(1) == ':')
                    return LocalTime.parse('0' + s);
                if (s.length() == 8 && s.charAt(2) == ':')
                    return LocalTime.parse(s);
            } catch (DateTimeParseException fallback) {
                // fallback
            }
            try {
                if (s.length() == 10)
                    return LocalDate.parse(s);
            } catch (DateTimeParseException fallback) {
                // fallback
            }
            try {
                if (s.length() >= 22)
                    return ZonedDateTime.parse(s);
            } catch (DateTimeParseException fallback) {
                // fallback
            }
            return s;
        }

        @NotNull
        private Object readSequence(Class clazz) {
            @NotNull Collection coll =
                    clazz == SortedSet.class ? new TreeSet<>() :
                            clazz == Set.class ? new LinkedHashSet<>() :
                                    new ArrayList<>();
            @Nullable Class componentType = (clazz != null && clazz.isArray() && clazz.getComponentType().isPrimitive())
                    ? clazz.getComponentType() : null;

            readCollection(componentType, coll);
            if (clazz != null && clazz.isArray()) {
                Object o = Array.newInstance(clazz.getComponentType(), coll.size());
                if (clazz.getComponentType().isPrimitive()) {
                    Iterator iter = coll.iterator();
                    for (int i = 0; i < coll.size(); i++)
                        Array.set(o, i, iter.next());
                    return o;
                }
                return coll.toArray((Object[]) o);
            }
            return coll;
        }

        private void readCollection(@Nullable Class clazz, @NotNull Collection list) {
            sequence(list, (l, v) -> {
                while (v.hasNextSequenceItem()) {
                    l.add(v.object(clazz));
                }
            });
        }

        private Object decodeBinary(Class type) {
            Object o = objectWithInferredType(null, SerializationStrategies.ANY_SCALAR, String.class);
            byte[] decoded = Base64.getDecoder().decode(o == null ? "" : o.toString().replaceAll("\\s", ""));

            // Does this logic belong here?
            if (type == null || BytesStore.class.isAssignableFrom(type))
                return BytesStore.wrap(decoded);

            if (type.isArray() && type.getComponentType().equals(Byte.TYPE))
                return decoded;

            // For BitSet, other types may be supported in the future
            try {
                Method valueOf = type.getDeclaredMethod("valueOf", byte[].class);
                Jvm.setAccessible(valueOf);
                return valueOf.invoke(null, decoded);
            } catch (NoSuchMethodException e) {
                // ignored
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }

            throw new UnsupportedOperationException("Cannot determine how to deserialize " + type + " from binary data");
        }

        @Override
        public String toString() {
            return YamlWire.this.toString();
        }
    }

    @Override
    public boolean hasMetaDataPrefix() {
        if (yt.current() == YamlToken.TAG
                && yt.isText("!meta-data")) {
            yt.next();
            return true;
        }
        return false;
    }
}
