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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.ref.*;
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.threads.ThreadLocalHelper;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static net.openhft.chronicle.wire.TextStopCharTesters.END_OF_TYPE;
import static net.openhft.chronicle.wire.Wires.GENERATE_TUPLES;
import static net.openhft.chronicle.wire.Wires.THROW_CNFRE;

/**
 * YAML Based wire format
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TextWire extends YamlWireOut<TextWire> {
    public static final BytesStore BINARY = BytesStore.from("!!binary");
    public static final @NotNull Bytes<byte[]> TYPE_STR = Bytes.from("type ");
    static final String SEQ_MAP = "!seqmap";
    static final BitSet END_CHARS = new BitSet();
    static final ThreadLocal<WeakReference<StopCharTester>> ESCAPED_QUOTES = new ThreadLocal<>();//ThreadLocal.withInitial(StopCharTesters.QUOTES::escaping);
    static final ThreadLocal<WeakReference<StopCharTester>> ESCAPED_SINGLE_QUOTES = new ThreadLocal<>();//ThreadLocal.withInitial(() -> StopCharTesters.SINGLE_QUOTES.escaping());
    static final ThreadLocal<WeakReference<StopCharTester>> ESCAPED_END_OF_TEXT = new ThreadLocal<>();// ThreadLocal.withInitial(() -> TextStopCharsTesters.END_OF_TEXT.escaping());
    static final ThreadLocal<WeakReference<StopCharsTester>> STRICT_ESCAPED_END_OF_TEXT = new ThreadLocal<>();// ThreadLocal.withInitial(() -> TextStopCharsTesters.END_OF_TEXT.escaping());
    static final Pattern REGX_PATTERN = Pattern.compile("\\.|\\$");
    static final Supplier<StopCharTester> QUOTES_ESCAPING = StopCharTesters.QUOTES::escaping;
    static final Supplier<StopCharTester> SINGLE_QUOTES_ESCAPING = StopCharTesters.SINGLE_QUOTES::escaping;
    static final Supplier<StopCharTester> END_OF_TEXT_ESCAPING = TextStopCharTesters.END_OF_TEXT::escaping;
    static final Supplier<StopCharsTester> STRICT_END_OF_TEXT_ESCAPING = TextStopCharsTesters.STRICT_END_OF_TEXT::escaping;
    static final Supplier<StopCharsTester> END_EVENT_NAME_ESCAPING = TextStopCharsTesters.END_EVENT_NAME::escaping;
    static final Bytes<?> META_DATA = Bytes.from("!!meta-data");

    static {
        IOTools.unmonitor(BINARY);
        //for (char ch : "?%*&@`0123456789+- ',#:{}[]|>!\\".toCharArray())
        //for (char ch : "?,#:{}[]|>\\^".toCharArray())
        for (char ch : "#:}]".toCharArray())
            END_CHARS.set(ch);
        // make sure it has loaded.
        WireInternal.INTERNER.valueCount();
    }

    protected final TextValueIn valueIn = createValueIn();
    protected long lineStart = 0;
    private DefaultValueIn defaultValueIn;
    protected WriteDocumentContext writeContext;
    protected ReadDocumentContext readContext;
    private boolean strict = false;

    public TextWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    public TextWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    @NotNull
    public static TextWire fromFile(String name) throws IOException {
        return new TextWire(BytesUtil.readFile(name), true);
    }

    @NotNull
    public static TextWire from(@NotNull String text) {
        return new TextWire(Bytes.from(text));
    }

    public static String asText(@NotNull Wire wire) {
        NativeBytes<Void> bytes = nativeBytes();
        ValidatableUtil.startValidateDisabled();
        try {
            long pos = wire.bytes().readPosition();
            @NotNull Wire tw = WireType.TEXT.apply(bytes);
            wire.copyTo(tw);
            wire.bytes().readPosition(pos);
            return tw.toString();
        } finally {
            ValidatableUtil.endValidateDisabled();
            bytes.releaseLast();
        }
    }

    // https://yaml.org/spec/1.2.2/#escaped-characters
    public static <ACS extends Appendable & CharSequence> void unescape(@NotNull ACS sb) {
        int end = 0;
        int length = sb.length();
        for (int i = 0; i < length; i++) {
            char ch = sb.charAt(i);
            if (ch == '\\' && i < length - 1) {
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
                    case 'L':
                        ch = 0x2028;
                        break;
                    case 'P':
                        ch = 0x2029;
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
            AppendableUtil.setCharAt(sb, end++, ch);
        }
        if (length != sb.length())
            throw new IllegalStateException("Length changed from " + length + " to " + sb.length() + " for " + sb);
        AppendableUtil.setLength(sb, end);
    }

    @Nullable
    static StopCharTester getEscapingSingleQuotes() {
        StopCharTester sct = ThreadLocalHelper.getTL(ESCAPED_SINGLE_QUOTES, SINGLE_QUOTES_ESCAPING);
        // reset it.
        sct.isStopChar(' ');
        return sct;
    }

    /**
     * Loads an Object from a file
     *
     * @param filename the file-path containing the object
     * @param <T>      the type of the object to load
     * @return an instance of the object created fromt the data in the file
     * @throws IOException if the file can not be found or read
     */
    public static <T> T load(String filename) throws IOException, InvalidMarshallableException {
        return (T) TextWire.fromFile(filename).readObject();
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    public boolean strict() {
        return strict;
    }

    public TextWire strict(boolean strict) {
        this.strict = strict;
        return this;
    }

    @Override
    @NotNull
    public <T> T methodWriter(@NotNull Class<T> tClass, Class... additional) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.TEXT,
                () -> newTextMethodWriterInvocationHandler(tClass));
        for (Class aClass : additional)
            builder.addInterface(aClass);
        useTextDocuments();
        builder.marshallableOut(this);
        return builder.build();
    }

    @NotNull
    TextMethodWriterInvocationHandler newTextMethodWriterInvocationHandler(Class... interfaces) {
        for (Class<?> anInterface : interfaces) {
            Comment c = Jvm.findAnnotation(anInterface, Comment.class);
            if (c != null)
                writeComment(c.value());
        }
        return new TextMethodWriterInvocationHandler(interfaces[0], this);
    }

    @Override
    @NotNull
    public <T> MethodWriterBuilder<T> methodWriterBuilder(@NotNull Class<T> tClass) {
        VanillaMethodWriterBuilder<T> text = new VanillaMethodWriterBuilder<>(tClass,
                WireType.TEXT,
                () -> newTextMethodWriterInvocationHandler(tClass));
        text.marshallableOut(this);
        return text;
    }

    @Override
    public @NotNull VanillaMethodReaderBuilder methodReaderBuilder() {
        return super.methodReaderBuilder().wireType(WireType.TEXT);
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
        this.classLookup = classLookup;
    }

    @Override
    public ClassLookup classLookup() {
        return classLookup;
    }

    @NotNull
    @Override
    public DocumentContext writingDocument(boolean metaData) {
        if (writeContext == null)
            useTextDocuments();
        writeContext.start(metaData);
        return writeContext;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        if (writeContext != null && writeContext.isOpen() && writeContext.chainedElement())
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
    public TextWire useBinaryDocuments() {
        readContext = new BinaryReadDocumentContext(this, false);
        writeContext = new BinaryWriteDocumentContext(this);
        return this;
    }

    @NotNull
    public TextWire useTextDocuments() {
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
    public void copyTo(@NotNull WireOut wire) throws InvalidMarshallableException {
        if (wire instanceof TextWire || wire instanceof YamlWire) {
            final Bytes<?> bytes0 = bytes();
            final long length = bytes0.readRemaining();
            wire.bytes().write(this.bytes, bytes0.readPosition(), length);
            this.bytes.readSkip(length);
        } else {
            // TODO: implement copying
            throw new UnsupportedOperationException("Not implemented yet. Can only copy TextWire format to the same format  not " + wire.getClass());
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
        return valueIn;
    }

    @NotNull
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        consumePadding();
        try {
            int ch = peekCode();
            // 10xx xxxx, 1111 xxxx
            if (ch > 0x80 && ((ch & 0xC0) == 0x80 || (ch & 0xF0) == 0xF0)) {
                throw new IllegalStateException("Attempting to read binary as TextWire ch=" + Integer.toHexString(ch));
            }
            if (ch < 0 || ch == '!' || ch == '[' || ch == '{') {
                sb.setLength(0);
                return sb;
            }
            if (ch == '?') {
                bytes.readSkip(1);
                consumePadding();
                ch = peekCode();
            }
            if (ch == '"') {
                bytes.readSkip(1);

                parseUntil(sb, getEscapingQuotes());

                consumePadding();
                ch = readCode();
                if (ch != ':')
                    throw new UnsupportedOperationException("Expected a : at " + bytes.toDebugString() + " was " + (char) ch);

            } else if (ch == '\'') {
                bytes.readSkip(1);

                parseUntil(sb, getEscapingSingleQuotes());

                consumePadding();
                ch = readCode();
                if (ch != ':')
                    throw new UnsupportedOperationException("Expected a : at " + bytes.toDebugString() + " was " + (char) ch);

            } else if (ch < 0) {
                sb.setLength(0);
                return sb;

            } else {
                parseUntil(sb, getEscapingEndOfText());
                trimTheEnd(sb);

            }
            unescape(sb);

        } catch (BufferUnderflowException e) {
            Jvm.debug().on(getClass(), e);
        }
        return sb;
    }

    private void trimTheEnd(@NotNull StringBuilder sb) {
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1)))
            sb.setLength(sb.length() - 1);
    }

    @Nullable
    @Override
    public <K> K readEvent(@NotNull Class<K> expectedClass) throws InvalidMarshallableException {
        consumePadding(0);
        @NotNull StringBuilder sb = acquireStringBuilder();
        try {
            int ch = peekCode();
            // 10xx xxxx, 1111 xxxx
            if (ch > 0x80 && ((ch & 0xC0) == 0x80 || (ch & 0xF0) == 0xF0)) {
                throw new IllegalStateException("Attempting to read binary as TextWire ch=" + Integer.toHexString(ch));

            } else if (ch == '?') {
                bytes.readSkip(1);
                consumePadding();
                @Nullable final K object;
                // if we don't know what type of key we are looking for, and it is not being defined with !
                // then we force it to be String as otherwise valueIn.object gets confused and gives us back a Map
                int ch3 = peekCode();
                if (ch3 != '!' && expectedClass == Object.class) {
                    object = (K) valueIn.objectWithInferredType0(null, SerializationStrategies.ANY_SCALAR, defaultKeyClass());
                } else {
                    object = valueIn.object(expectedClass);
                }
                consumePadding();
                int ch2 = readCode();
                if (ch2 != ':')
                    throw new IllegalStateException("Unexpected character after field " + ch + " '" + (char) ch2 + "'");
                return object;

            } else if (ch == '[') {
                return valueIn.object(expectedClass);

            } else if (ch == '"' || ch == '\'') {
                bytes.readSkip(1);

                final StopCharTester escapingQuotes = ch == '"' ? getEscapingQuotes() : getEscapingSingleQuotes();
                parseUntil(sb, escapingQuotes);

                consumePadding(1);
                ch = readCode();
                if (ch != ':')
                    throw new UnsupportedOperationException("Expected a : at " + bytes.toDebugString());

            } else if (ch < 0) {
                sb.setLength(0);
                return null;

            } else {
                parseUntil(sb, getEscapingEndOfText());
            }
            unescape(sb);
        } catch (BufferUnderflowException e) {
            Jvm.debug().on(getClass(), e);
        }
        //      consumePadding();
        return toExpected(expectedClass, sb);
    }

    protected Class defaultKeyClass() {
        return Object.class;
    }

    @Nullable
    private <K> K toExpected(Class<K> expectedClass, StringBuilder sb) {
        return ObjectUtils.convertTo(expectedClass, WireInternal.INTERNER.intern(sb));
    }

    @NotNull
    protected StopCharTester getEscapingEndOfText() {
        StopCharTester escaping = ThreadLocalHelper.getTL(ESCAPED_END_OF_TEXT, END_OF_TEXT_ESCAPING);
        // reset it.
        escaping.isStopChar(' ');
        return escaping;
    }

    @NotNull
    protected StopCharsTester getStrictEscapingEndOfText() {
        StopCharsTester escaping = ThreadLocalHelper.getTL(STRICT_ESCAPED_END_OF_TEXT, STRICT_END_OF_TEXT_ESCAPING);
        // reset it.
        escaping.isStopChar(' ', ' ');
        return escaping;
    }

    @NotNull
    protected StopCharsTester getEscapingEndEventName() {
        StopCharsTester escaping = ThreadLocalHelper.getTL(STRICT_ESCAPED_END_OF_TEXT, END_EVENT_NAME_ESCAPING);
        // reset it.
        escaping.isStopChar(' ', ' ');
        return escaping;
    }

    @Nullable
    protected StopCharTester getEscapingQuotes() {
        StopCharTester sct = ThreadLocalHelper.getTL(ESCAPED_QUOTES, QUOTES_ESCAPING);
        // reset it.
        sct.isStopChar(' ');
        return sct;
    }

    @Override
    public void consumePadding() {
        consumePadding(0);
    }

    @Override
    @NotNull
    public String readingPeekYaml() {
        return "todo";
    }

    // TODO Move to valueIn
    public void consumePadding(int commas) {
        for (; ; ) {
            int codePoint = peekCode();
            switch (codePoint) {
                case '#':
                    readCode();
                    while (peekCode() == ' ')
                        readCode();
                    try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                        final StringBuilder sb = stlSb.get();
                        for (int ch; notNewLine(ch = readCode()); )
                            sb.append((char) ch);
                        if (!valueIn.consumeAny)
                            commentListener.accept(sb);
                    }
                    this.lineStart = bytes.readPosition();
                    break;
                case ',':
                    if (valueIn.isASeparator(peekCodeNext()) && commas-- <= 0)
                        return;
                    bytes.readSkip(1);
                    if (commas == 0)
                        return;
                    break;
                case ' ':
                case '\t':
                    bytes.readSkip(1);
                    break;
                case '\n':
                case '\r':
                    this.lineStart = bytes.readPosition() + 1;
                    bytes.readSkip(1);
                    break;
                default:
                    return;
            }
        }
    }

    private boolean notNewLine(int readCode) {
        return readCode >= 0 && readCode != '\r' && readCode != '\n';
    }

    protected void consumeDocumentStart() {
        if (bytes.readRemaining() > 4) {
            long pos = bytes.readPosition();
            if (bytes.readByte(pos) == '-' && bytes.readByte(pos + 1) == '-' && bytes.readByte(pos + 2) == '-') {
                bytes.readSkip(3);

                consumeWhiteSpace();

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
        }
    }

    int peekCode() {
        return bytes.peekUnsignedByte();
    }

    int peekCodeNext() {
        return bytes.peekUnsignedByte(bytes.readPosition() + 1);
    }

    /**
     * returns {@code true} if the next string is {@code str}
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
        return read(key.name(), key.code(), key.defaultValue());
    }

    private ValueIn read(@NotNull CharSequence keyName, int keyCode, Object defaultValue) {
        consumePadding();
        ValueInState curr = valueIn.curr();
        final StringBuilder stringBuilder = acquireStringBuilder();
        // did we save the position last time
        // so we could go back and parseOne an older field?
        if (curr.savedPosition() > 0) {
            bytes.readPosition(curr.savedPosition() - 1);
            curr.savedPosition(0L);
        }
        while (bytes.readRemaining() > 0) {
            long position = bytes.readPosition();
            // at the current position look for the field.
            readField(stringBuilder);
            // might have changed due to readField in JSONWire
            curr = valueIn.curr();

            if (StringUtils.equalsCaseIgnore(stringBuilder, keyName))
                return valueIn;
            if (stringBuilder.length() == 0) {
                if (curr.unexpectedSize() > 0)
                    break;
                return valueIn;
            }

            // if no old field nor current field matches, set to default values.
            // we may come back and set the field later if we find it.
            curr.addUnexpected(position);
            long toSkip = valueIn.readLengthMarshallable();
            bytes.readSkip(toSkip);
            consumePadding(1);
        }

        return read2(keyName, keyCode, defaultValue, curr, stringBuilder, keyName);
    }

    protected ValueIn read2(CharSequence keyName, int keyCode, Object defaultValue, @NotNull ValueInState curr, @NotNull StringBuilder sb, @NotNull CharSequence name) {
        final long position2 = bytes.readPosition();

        // if not a match go back and look at old fields.
        for (int i = 0; i < curr.unexpectedSize(); i++) {
            bytes.readPosition(curr.unexpected(i));
            readField(sb);
            if (sb.length() == 0 || StringUtils.equalsCaseIgnore(sb, name)) {
                // if an old field matches, remove it, save the current position
                curr.removeUnexpected(i);
                curr.savedPosition(position2 + 1);
                return valueIn;
            }
        }
        bytes.readPosition(position2);

        if (defaultValueIn == null)
            defaultValueIn = new DefaultValueIn(this);
        defaultValueIn.defaultValue = defaultValue;
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
        consumeWhiteSpace();
        if (peekCode() == '#') {
            bytes.readSkip(1);
            consumeWhiteSpace();
            bytes.parseUtf8(s, StopCharTesters.CONTROL_STOP);
        }
        return this;
    }

    public void consumeWhiteSpace() {
        while (Character.isWhitespace(peekCode()))
            bytes.readSkip(1);
    }

    @Override
    public void clear() {
        bytes.clear();
        valueIn.resetState();
        valueOut.resetState();
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

    public void parseWord(@NotNull StringBuilder sb) {
        parseUntil(sb, StopCharTesters.SPACE_STOP);
    }

    public void parseUntil(@NotNull StringBuilder sb, @NotNull StopCharTester testers) {
        if (use8bit)
            bytes.parse8bit(sb, testers);
        else
            bytes.parseUtf8(sb, testers);
    }

    public void parseUntil(@NotNull StringBuilder sb, @NotNull StopCharsTester testers) {
        sb.setLength(0);
        if (use8bit) {
            AppendableUtil.read8bitAndAppend(bytes, sb, testers);
        } else {
            AppendableUtil.readUTFAndAppend(bytes, sb, testers);
        }
    }

    @Nullable
    public Object readObject() throws InvalidMarshallableException {
        consumePadding();
        consumeDocumentStart();
        return getValueIn().object(Object.class);
    }

    @Nullable
    Object readObject(int indentation) throws InvalidMarshallableException {
        consumePadding();
        int code = peekCode();
        int indentation2 = indentation();
        if (indentation2 < indentation)
            return NoObject.NO_OBJECT;
        switch (code) {
            case '-':
                if (peekCodeNext() == '-')
                    return NoObject.NO_OBJECT;

                return readList(indentation2, null);
            case '[':
                return readList();
            case '{':
                return valueIn.marshallableAsMap(Object.class, Object.class);
            case '!':
                return readTypedObject();
            default:
                return readMap(indentation2, null);
        }
    }

    private int indentation() {
        long pos = bytes.readPosition();
        if (pos < lineStart) {
            lineStart = pos;
            return 0;
        }
        return Maths.toInt32(pos - lineStart);
    }

    @Nullable
    private Object readTypedObject() throws InvalidMarshallableException {
        return valueIn.object(Object.class);
    }

    @NotNull
    private List readList() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    List readList(int indentation, Class elementType) throws InvalidMarshallableException {
        @NotNull List<Object> objects = new ArrayList<>();
        while (peekCode() == '-') {
            if (indentation() < indentation)
                break;
            if (peekCodeNext() == '-')
                break;
            long ls = lineStart;
            bytes.readSkip(1);
            consumePadding();
            if (lineStart == ls) {
                objects.add(valueIn.objectWithInferredType(null, SerializationStrategies.ANY_OBJECT, elementType));
            } else {
                @Nullable Object e = readObject(indentation);
                if (e != NoObject.NO_OBJECT)
                    objects.add(e);
            }
            consumePadding(1);
        }

        return objects;
    }

    @NotNull
    private Map readMap(int indentation, Class valueType) throws InvalidMarshallableException {
        @NotNull Map map = new LinkedHashMap<>();
        consumePadding();
        while (bytes.readRemaining() > 0) {
            if (indentation() < indentation || bytes.readRemaining() == 0)
                break;
            @Nullable String key = readAndIntern();
            if (key.equals("..."))
                break;
            @Nullable Object value = valueIn.objectWithInferredType(null, SerializationStrategies.ANY_OBJECT, valueType);
            map.put(key, value);
            consumePadding(1);
        }
        return map;
    }

    private String readAndIntern() {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            StringBuilder sb = stlSb.get();
            read(sb);
            return WireInternal.INTERNER.intern(sb);
        }
    }

    @Override
    public void reset() {
        writeContext.reset();
        readContext.reset();
        sb.setLength(0);
        lineStart = 0;
        valueIn.resetState();
        valueOut.resetState();
        bytes.clear();
    }

    @Override
    public boolean hasMetaDataPrefix() {
        if (bytes.startsWith(META_DATA)
                && bytes.peekUnsignedByte(bytes.readPosition() + 11) <= ' ') {
            bytes.readSkip(12);
            return true;
        }
        return false;
    }

    enum NoObject {NO_OBJECT}

    public class TextValueIn implements ValueIn {
        final ValueInStack stack = new ValueInStack();
        int sequenceLimit = 0;
        private boolean consumeAny;

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
            @Nullable CharSequence cs = textTo0(bytes);
            consumePadding(1);

            if (cs == null)
                return null;
            if (cs != bytes) {
                bytes.clear();
                bytes.writeUtf8(cs);
            }
            return bytes;
        }

        @NotNull
        @Override
        public BracketType getBracketType() {
            consumePadding();
            switch (peekCode()) {
                case '{':
                    return BracketType.MAP;
                case '[':
                    return BracketType.SEQ;
                default:
                    return BracketType.NONE;
            }
        }

        @Nullable <ACS extends Appendable & CharSequence> CharSequence textTo0(@NotNull ACS a) {
            consumePadding();
            int ch = peekCode();
            @Nullable CharSequence ret = a;

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
                case '"':
                    readText(a, getEscapingQuotes());
                    break;

                case '\'':
                    readText(a, getEscapingSingleQuotes());
                    break;

                case '!': {
                    bytes.readSkip(1);
                    final StringBuilder stringBuilder = acquireStringBuilder();
                    parseWord(stringBuilder);
                    if (StringUtils.isEqual(stringBuilder, "!null")) {
                        textTo(stringBuilder);
                        ret = null;
                    } else {
                        // ignore the type.
                        if (a instanceof StringBuilder) {
                            textTo((StringBuilder) a);
                        } else {
                            textTo(stringBuilder);
                            ret = stringBuilder;
                        }
                    }
                    break;
                }

                case -1:
                    return "";

                case '$':
                    if (peekCodeNext() == '{') {
                        unsubstitutedString(a);
                        return a;
                    }
                    // fall through

                default: {
                    final long rem = bytes.readRemaining();
                    if (rem > 0) {
                        if (a instanceof Bytes) {
                            bytes.parse8bit((Bytes) a, getStrictEscapingEndOfText());
                        } else if (use8bit) {
                            bytes.parse8bit((StringBuilder) a, getStrictEscapingEndOfText());
                        } else {
                            bytes.parseUtf8(a, getStrictEscapingEndOfText());
                        }
                        if (rem == bytes.readRemaining())
                            throw new IORuntimeException("Nothing to read at " + bytes.toDebugString(32));
                    } else {
                        AppendableUtil.setLength(a, 0);
                    }
                    // trim trailing spaces.
                    while (a.length() > 0) {
                        if (Character.isWhitespace(a.charAt(a.length() - 1)))
                            AppendableUtil.setLength(a, a.length() - 1);
                        else
                            break;
                    }
                    break;
                }
            }

            int prev = peekBack();
            if (END_CHARS.get(prev))
                bytes.readSkip(-1);
            return ret;
        }

        private <ACS extends Appendable & CharSequence> void unsubstitutedString(@NotNull ACS a) {
            String text = bytes.toString();
            if (text.length() > 32)
                text = text.substring(0, 32);
            Jvm.warn().on(getClass(), "Found an unsubstituted ${} as " + text);
            char c;
            do {
                c = bytes.readChar();
                try {
                    a.append(c);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            } while (!bytes.isEmpty() && c != '}');
        }

        private <ACS extends Appendable & CharSequence> void readText(@NotNull ACS a, @NotNull StopCharTester quotes) {
            bytes.readSkip(1);
            if (use8bit)
                bytes.parse8bit(a, quotes);
            else
                bytes.parseUtf8(a, quotes);
            unescape(a);
            consumePadding(1);
        }

        protected int peekBack() {
            while (bytes.readPosition() > bytes.start()) {
                int prev = bytes.readUnsignedByte(bytes.readPosition() - 1);
                if (prev != ' ') {
                    if (prev == '\n' || prev == '\r') {
                        // TODO doesn't look right.
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
            try {
                // TODO needs to be made much more efficient.
                @NotNull StringBuilder sb = acquireStringBuilder();
                if (peekCode() == '!') {
                    bytes.readSkip(1);
                    parseWord(sb);
                    @Nullable byte[] uncompressed = Compression.uncompress(sb, TextWire.this, t -> {
                        @NotNull StringBuilder sb2 = acquireStringBuilder();
                        AppendableUtil.setLength(sb2, 0);
                        t.parseUntil(sb2, StopCharTesters.COMMA_SPACE_STOP);
                        return Base64.getDecoder().decode(sb2.toString());
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
                    bytesConsumer.readMarshallable(Bytes.wrapForRead(sb.toString().getBytes(ISO_8859_1)));
                }
                return TextWire.this;
            } finally {
                consumePadding(1);
            }
        }

        @Override
        public byte[] bytes(byte[] using) {
            consumePadding();
            try {
                // TODO needs to be made much more efficient.
                final StringBuilder stringBuilder = acquireStringBuilder();
                if (peekCode() == '!') {
                    bytes.readSkip(1);
                    parseWord(stringBuilder);

                    if ("byte[]".contentEquals(stringBuilder)) {
                        bytes.readSkip(1);
                        parseWord(stringBuilder);
                    }

                    byte @Nullable [] bytes = Compression.uncompress(stringBuilder, this, t -> {
                        @NotNull StringBuilder sb0 = acquireStringBuilder();
                        parseUntil(sb0, StopCharTesters.COMMA_SPACE_STOP);
                        return Base64.getDecoder().decode(WireInternal.INTERNER.intern(sb0));
                    });
                    if (bytes != null)
                        return bytes;

                    if ("!null".contentEquals(stringBuilder)) {
                        parseWord(stringBuilder);
                        return null;
                    }

                    throw new IllegalStateException("unsupported type=" + stringBuilder);

                } else {
                    textTo(stringBuilder);
                    if (using != null && stringBuilder.length() == using.length) {
                        for (int i = 0; i < using.length; i++)
                            using[i] = (byte) stringBuilder.charAt(i);
                        return using;
                    }
                    // todo fix this.
                    return stringBuilder.toString().getBytes(ISO_8859_1);
                }
            } finally {
                consumePadding(1);
            }
        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return TextWire.this;
        }

        @Override
        public long readLength() {
            return readLengthMarshallable();
        }

        @NotNull
        @Override
        public WireIn skipValue() {
            consumeAny();
            if (peekCode() == ',')
                bytes.readSkip(1);
            return TextWire.this;
        }

        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            this.consumeAny = true;
            try {
                consumeAny();
                return bytes.readPosition() - start;
            } finally {
                this.consumeAny = false;
                bytes.readPosition(start);
            }
        }

        protected void consumeAny() {
            consumePadding();
            int code = peekCode();
            switch (code) {
                case '$': {
                    bytes.readSkip(1);
                    if (peekCode() == '{')
                        bytes.parse8bit(StopCharTesters.CURLY_STOP);
                    break;
                }
                case '{': {
                    consumeMap();
                    break;
                }
                case '[': {
                    consumeSeq();
                    break;
                }
                case '}':
                    break;
                case ']':
                    break;
                case '?':
                    bytes.readSkip(1);
                    consumeAny();
                    if (peekCode() == ':') {
                        bytes.readSkip(1);
                        consumeAny();
                    }
                    break;
                case '!':
                    consumeType2();
                    break;

                case '"':
                case '\'':
                default:
                    consumeValue();
                    while (peekBack() <= ' ' && bytes.readPosition() >= 0)
                        bytes.readSkip(-1);
                    if (peekBack() == ',') {
                        bytes.readSkip(-1);
                        break;
                    }
                    consumePadding();
                    if (peekCode() == ':' && isASeparator(peekCodeNext())) {
                        readCode();
                        consumeAny();
                    }
                    break;
            }
        }

        protected boolean isASeparator(int nextChar) {
            return TextStopCharsTesters.isASeparator(nextChar);
        }

        private void consumeType2() {
            bytes.readSkip(1);
            boolean type = bytes.startsWith(TYPE_STR);
            if (type)
                bytes.readSkip(TYPE_STR.length());
            while (!END_OF_TYPE.isStopChar(peekCode()))
                bytes.readSkip(1);
            if (peekCode() == ';')
                bytes.readSkip(1);
            if (!type)
                consumeAny();
        }

        private void consumeSeq() {
            int code;
            bytes.readSkip(1);
            for (; ; ) {
                long pos = bytes.readPosition();
                consumeAny();
                if (peekCode() == ',' && isASeparator(peekCodeNext()))
                    readCode();
                else
                    break;
                if (bytes.readPosition() == pos)
                    throw new IllegalStateException("Stuck at pos " + pos + " " + bytes);
            }
            consumePadding();
            code = readCode();
            if (code != ']') {
                bytes.readSkip(-1);
                throw new IllegalStateException("Expected a ] was " + bytes);
            }
        }

        private void consumeMap() {
            int code;
            bytes.readSkip(1);
            for (; ; ) {
                long pos = bytes.readPosition();
                consumeAny();
                int code2 = peekCode();
                if (code2 == '}' || code2 == ']' || code2 <= 0) {
                    break;
                } else if (code2 == ',' && isASeparator(peekCodeNext())) {
                    readCode();
                }
                if (bytes.readPosition() == pos)
                    throw new IllegalStateException("Stuck at pos " + pos + " " + bytes);
            }
            consumePadding();
            code = readCode();
            if (code != '}') {
                bytes.readSkip(-1);
                throw new IllegalStateException("Expected a } was " + (char) code);
            }
        }

        private void consumeValue() {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.readSkip(1);
                parseWord(stringBuilder);
                if (StringUtils.isEqual(stringBuilder, "type")) {
                    consumeType();
                } else {
                    consumeAny();
                }
            } else {
                textTo(stringBuilder);
            }
        }

        private void consumeType() {
            parseUntil(acquireStringBuilder(), StopCharTesters.COMMA_SPACE_STOP);
        }

        @NotNull
        @Override
        public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag) {
            consumePadding();

            final StringBuilder stringBuilder = acquireStringBuilder();
            if (textTo(stringBuilder) == null) {
                tFlag.accept(t, null);
                return TextWire.this;
            }

            tFlag.accept(t, StringUtils.isEqual(stringBuilder, "true"));
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
            consumePadding();
            tb.accept(t, (byte) getALong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (short) getALong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (short) getALong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (int) getALong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumePadding();
            ti.accept(t, (int) getALong());
            return TextWire.this;
        }

        long getALong() {
            final int code = peekCode();
            switch (code) {
                case '"':
                case '\'':
                    bytes.readSkip(1);
                    break;

                case 't':
                case 'T':
                case 'f':
                case 'F':
                    return bool() ? 1 : 0;
                case '$':
                    unsubstitutedNumber();
                    return 0;
                case '{':
                case '[':
                    throw new IORuntimeException("Cannot read a " + (char) code + " as a number");
            }
            return bytes.parseLong();
        }

        private void unsubstitutedNumber() {
            String s = bytes.parse8bit(StopCharTesters.CURLY_STOP);
            Jvm.warn().on(getClass(), "Cannot read " + s + "} as a number, treating as 0");
            // to prevent the } being rewound.
            if (",\n ".indexOf(peekCode()) >= 0)
                bytes.readSkip(1);
            else
                throw new IllegalStateException("Unable to continue after ${} in number.");
        }

        @NotNull
        @Override
        public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumePadding();
            tl.accept(t, getALong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumePadding();
            tl.accept(t, getALong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            consumePadding();
            if (peekCode() == '$') {
                unsubstitutedNumber();
            } else {
                tf.accept(t, (float) bytes.parseDouble());
            }
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            consumePadding();
            if (peekCode() == '$') {
                unsubstitutedNumber();
            } else {
                td.accept(t, bytes.parseDouble());
            }
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            setLocalTime.accept(t, LocalTime.parse(WireInternal.INTERNER.intern(stringBuilder)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            tZonedDateTime.accept(t, ZonedDateTime.parse(WireInternal.INTERNER.intern(stringBuilder)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            tLocalDate.accept(t, LocalDate.parse(WireInternal.INTERNER.intern(stringBuilder)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            tuuid.accept(t, UUID.fromString(WireInternal.INTERNER.intern(stringBuilder)));
            return TextWire.this;
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
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongValue value) {
            consumePadding();
            @NotNull Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumePadding(1);
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntValue value) {
            consumePadding();
            @NotNull Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumePadding(1);
            return TextWire.this;
        }

        @Override
        public WireIn bool(@NotNull final BooleanValue value) {
            consumePadding();
            @NotNull Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumePadding(1);
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
            @Nullable Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumePadding(1);
            return TextWire.this;
        }

        @Override
        public <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            consumePadding();

            char code = (char) peekCode();
            if (code == '!') {
                @Nullable final Class typePrefix = typePrefix();
                if (typePrefix == void.class) {
                    text();
                    return false;
                }
                consumePadding();
                code = (char) readCode();
            }
            if (code == '[') {
                bytes.readSkip(1);
                sequenceLimit = Integer.MAX_VALUE;
            } else {
                sequenceLimit = 1;
            }

            tReader.accept(t, TextWire.this.valueIn);

            if (code == '[') {
                consumePadding(1);
                char code2 = (char) readCode();
                if (code2 != ']')
                    throw new IORuntimeException("Expected a ] but got " + code2 + " (" + code2 + ")");
            }
            consumePadding(1);
            return true;
        }

        public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) throws InvalidMarshallableException {
            return sequence(list, buffer, bufferAdd);
        }

        @Override
        public <T> boolean sequence(@NotNull List<T> list, @NotNull List<T> buffer, @NotNull Supplier<T> bufferAdd) throws InvalidMarshallableException {

            list.clear();
            consumePadding();

            char code = (char) peekCode();
            if (code == '!') {
                @Nullable final Class typePrefix = typePrefix();
                if (typePrefix == void.class) {
                    text();
                    return false;
                }
                consumePadding();
                code = (char) readCode();
            }
            if (code == '[') {
                bytes.readSkip(1);
                sequenceLimit = Integer.MAX_VALUE;
            } else {
                sequenceLimit = 1;
            }

            while (hasNextSequenceItem()) {
                int size = list.size();
                if (buffer.size() <= size) buffer.add(bufferAdd.get());

                final T t = buffer.get(size);
                if (t instanceof Resettable) ((Resettable) t).reset();
                list.add(object(t, t.getClass()));
            }

            if (code == '[') {
                consumePadding(1);
                char code2 = (char) readCode();
                if (code2 != ']')
                    throw new IORuntimeException("Expected a ] but got " + code2 + " (" + code2 + ")");
            }
            consumePadding(1);
            return true;
        }

        @NotNull
        @Override
        public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) throws InvalidMarshallableException {

            consumePadding();
            char code = (char) peekCode();

            if (code == '[') {
                bytes.readSkip(1);
                sequenceLimit = Integer.MAX_VALUE;
            } else {
                sequenceLimit = 1;
            }

            // this code was added to support empty sets
            consumePadding();
            char code2 = (char) peekCode();
            if (code2 == ']') {
                readCode();
            } else {
                tReader.accept(t, kls, TextWire.this.valueIn);

                if (code == '[') {
                    consumePadding();
                    char code3 = (char) readCode();
                    if (code3 != ']')
                        throw new IORuntimeException("Expected a ] but got " + code3 + " (" + code3 + ")");
                }
            }

            consumePadding(1);
            return TextWire.this;
        }

        @Override
        public boolean hasNext() {
            consumePadding();
            return bytes.readRemaining() > 0;
        }

        @Override
        public boolean hasNextSequenceItem() {
            if (sequenceLimit-- <= 0)
                return false;
            consumePadding();
            int ch = peekCode();
            // don't test for next char as any comma still left here is to be consumed.
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
            boolean endsNormally = false;

            try {
                // ensure that you can read past the end of this marshable object
                final long newLimit = position - 1 + len;
                bytes.readLimit(newLimit);
                bytes.readSkip(1); // skip the {
                consumePadding();
                final T apply = marshallableReader.apply(TextWire.this);
                endsNormally = true;
                return apply;
            } finally {
                bytes.readLimit(limit);

                consumePadding(1);
                code = readCode();
                popState();
                if (code != '}' && endsNormally)
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
            final StringBuilder stringBuilder = acquireStringBuilder();
            stringBuilder.setLength(0);
            if (code == -1) {
                stringBuilder.append("java.lang.Object");
            } else if (code == '!') {
                readCode();

                parseUntil(stringBuilder, END_OF_TYPE);
                bytes.readSkip(-1);
                consumePadding();
            }
            return this;
        }

        @Override
        public Class typePrefix() {
            consumePadding();
            int code = peekCode();
            if (code == '!' || code == '@') {
                readCode();

                final StringBuilder stringBuilder = acquireStringBuilder();
                stringBuilder.setLength(0);
                parseUntil(stringBuilder, END_OF_TYPE);
                bytes.readSkip(-1);
                try {
                    return classLookup().forName(stringBuilder);
                } catch (ClassNotFoundRuntimeException e) {
                    // Note: it's not possible to generate a Tuple without an interface implied.
                    if (THROW_CNFRE)
                        throw e;
                    String message = "Unable to find " + stringBuilder + " " + e.getCause();
                    Jvm.warn().on(getClass(), message);
                    return null;
                }
            }
            return null;
        }

        @Override
        public Object typePrefixOrObject(Class tClass) {
            consumePadding();
            int code = peekCode();
            if (code == '!') {
                readCode();

                final StringBuilder stringBuilder = acquireStringBuilder();
                stringBuilder.setLength(0);
                parseUntil(stringBuilder, END_OF_TYPE);
                bytes.readSkip(-1);
                try {
                    return classLookup().forName(stringBuilder);
                } catch (ClassNotFoundRuntimeException e) {
                    Object o = handleCNFE(tClass, e, stringBuilder);
                    if (o != null)
                        return o;
                }
            }
            if (Wires.dtoInterface(tClass) && GENERATE_TUPLES && ObjectUtils.implementationToUse(tClass) == tClass)
                return Wires.tupleFor(tClass, null);
            return null;
        }

        @Nullable
        private Object handleCNFE(Class tClass, ClassNotFoundRuntimeException e, StringBuilder stringBuilder) {
            if (tClass == null) {
                if (GENERATE_TUPLES) {
                    return Wires.tupleFor(null, stringBuilder.toString());
                }
                String message = "Unable to load " + stringBuilder + ", is a class alias missing.";
                if (THROW_CNFRE)
                    throw new ClassNotFoundRuntimeException(new ClassNotFoundException(message));
                Jvm.warn().on(TextWire.class, message);
                return null;
            }

            final String className = tClass.getName();

            String[] split = REGX_PATTERN.split(stringBuilder);
            if (split[split.length - 1].equalsIgnoreCase(tClass.getSimpleName())) {
                try {

                    return tClass.isInterface()
                            ? Wires.tupleFor(tClass, stringBuilder.toString())
                            : classLookup().forName(className);

                } catch (ClassNotFoundRuntimeException e1) {
                    if (!THROW_CNFRE) {
                        Jvm.warn().on(getClass(), "ClassNotFoundException class=" + className);
                        return Wires.tupleFor(tClass, className);
                    }
                }

            } else if (GENERATE_TUPLES && tClass.getClassLoader() != null && tClass.isInterface()) {
                return Wires.tupleFor(tClass, stringBuilder.toString());
            }

            if (THROW_CNFRE || tClass.isInterface())
                throw e;
            Jvm.warn().on(TextWire.class, "Cannot find a class for " + stringBuilder + " are you missing an alias?");
            return null;
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
            final StringBuilder stringBuilder = acquireStringBuilder();
            parseUntil(stringBuilder, END_OF_TYPE);
            classNameConsumer.accept(t, stringBuilder);
            return TextWire.this;
        }

        @Override
        public ClassLookup classLookup() {
            return TextWire.this.classLookup();
        }

        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            consumePadding();
            int code = readCode();
            if (!peekStringIgnoreCase("type "))
                throw new UnsupportedOperationException(stringForCode(code));
            bytes.readSkip("type ".length());
            final StringBuilder stringBuilder = acquireStringBuilder();
            parseUntil(stringBuilder, END_OF_TYPE);
            try {
                return classLookup().forName(stringBuilder);
            } catch (ClassNotFoundRuntimeException e) {
                return unresolvedHandler.apply(stringBuilder, e.getCause());
            }
        }

        @Nullable
        @Override
        public Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy)
                throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException {
            long position0 = bytes.readPosition();
            if (isNull()) {
                consumePadding(1);
                return null;
            }
            if (indentation() == 0 && peekCode() != '{') {
                strategy.readUsing(null, object, this, BracketType.UNKNOWN);
                return object;
            }
            pushState();
            consumePadding();
            int code = peekCode();
            if (code == '!') {
                typePrefix(null, (o, x) -> { /* sets acquireStringBuilder(); */});

            } else if (code == ',') {
                Jvm.warn().on(getClass(), "Expected a {} but was blank for type " + object.getClass());
                readCode();
                return object;

            } else if (code != '{') {
                if ("[]?}&".indexOf(code) < 0 && ObjectUtils.canConvertText(object.getClass())) {
                    Object o = ObjectUtils.convertTo(object.getClass(), text());
                    consumePadding(1);
                    return o;
                }
                consumeValue();
                long position00 = bytes.readPosition();
                final String s = bytes.readPosition(position0).toDebugString(128);
                bytes.readPosition(position00);
                throw new IORuntimeException("Trying to read marshallable " + object.getClass() + " at " + s + " expected to find a {");

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
                object = strategy.readUsing(null, object, this, BracketType.MAP);

            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
                popState();
            }

            consumePadding(1);
            code = readCode();
            if (code != '}')
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + ",code='" + (char) code + "', bytes=" + Bytes.toString(bytes, 1024)
                );
            consumePadding(1);
            return object;
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

            consumePadding(1);
            code = readCode();
            if (code != '}')
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + ",code='" + (char) code + "', bytes=" + Bytes.toString(bytes, 1024)
                );
            return object;
        }

        @Override
        @Nullable
        public <T> T typedMarshallable() throws InvalidMarshallableException {
            return (T) objectWithInferredType(null, SerializationStrategies.ANY_NESTED, null);
        }

        @Nullable <K, V> Map<K, V> map(@NotNull final Class<K> kClass,
                                       @NotNull final Class<V> vClass,
                                       @Nullable Map<K, V> usingMap) throws InvalidMarshallableException {
            consumePadding();
            if (usingMap == null)
                usingMap = new LinkedHashMap<>();
            else
                usingMap.clear();

            final StringBuilder stringBuilder = acquireStringBuilder();
            int code = peekCode();
            switch (code) {
                case '!':
                    return typedMap(kClass, vClass, usingMap, stringBuilder);
                case '{':
                    return marshallableAsMap(kClass, vClass, usingMap);
                case '?':
                    return readAllAsMap(kClass, vClass, usingMap);
                default:
                    throw new IORuntimeException("Unexpected code " + (char) code);
            }
        }

        @Nullable
        private <K, V> Map<K, V> typedMap(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap, @NotNull StringBuilder sb) throws InvalidMarshallableException {
            parseUntil(sb, StopCharTesters.SPACE_STOP);
            @Nullable String str = WireInternal.INTERNER.intern(sb);

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
                        @Nullable final K k = r.read(() -> "key")
                                .object(kClazz);
                        @Nullable final V v = r.read(() -> "value")
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
            switch (peekCode()) {
                case '[':
                case '{':
                    Jvm.warn().on(getClass(), "Unable to read " + valueIn.objectBestEffort() + " as a long.");
                    return 0;
            }

            long l = getALong();
            checkRewind();
            return l;
        }

        public void checkRewind() {
            int ch = peekBack();
            if (END_CHARS.get(ch))
                bytes.readSkip(-1);
        }

        public void checkRewindDouble() {
            checkRewind();
        }

        /**
         * @return the value as a float, or -0.0 indicates that we have not been able to parse this data ( we don't throw an exception ),
         * Note: "1e" is assumed to be "1e0"
         */
        @Override
        public double float64() {
            consumePadding();
            valueIn.skipType();
            switch (peekCode()) {
                case '$':
                    unsubstitutedNumber();
                    return 0;
                case '[':
                case '{':
                    Jvm.warn().on(getClass(), "Unable to read " + valueIn.objectBestEffort() + " as a double.");
                    return 0;
            }
            final double v = bytes.parseDouble();

            checkRewindDouble();
            return v;
        }

        void skipType() {
            long peek = bytes.peekUnsignedByte();
            if (peek == '!') {
                final StringBuilder stringBuilder = acquireStringBuilder();
                parseUntil(stringBuilder, END_OF_TYPE);
                consumePadding();
            }
        }

        @Override
        public float float32() {
            // this parses a double and casts to a float, so there may be some loss of precision
            return (float) float64();
        }

        /**
         * @return true if !!null "", if {@code true} reads the !!null "" up to the next STOP, if
         * {@code false} no  data is read  ( data is only peaked if {@code false} )
         */
        @Override
        public boolean isNull() {
            consumePadding();

            if (peekStringIgnoreCase("!!null \"\"")) {
                bytes.readSkip("!!null \"\"".length());
                // Skip to the next token, consuming any padding and/or a comma
                consumePadding(1);

                // discard the text after it.
                //  text(acquireStringBuilder());
                return true;
            }

            return false;
        }

        @Override
        public Object objectWithInferredType(Object using, @NotNull SerializationStrategy strategy, Class type) throws InvalidMarshallableException {
            consumePadding();
            @Nullable Object o = objectWithInferredType0(using, strategy, type);
            consumePadding();
            int code = peekCode();
            if (code == ':' && strategy.bracketType() != BracketType.NONE) {
                return readRestOfMap(using, o);
            }
            return o;
        }

        @NotNull
        Object readRestOfMap(Object using, Object o) throws InvalidMarshallableException {
            readCode();
            consumePadding();
            @Nullable Object value = objectWithInferredType0(using, SerializationStrategies.ANY_OBJECT, Object.class);
            @NotNull Map map = using instanceof Map ? (Map) using : new LinkedHashMap();
            map.put(o, value);
            readAllAsMap(Object.class, Object.class, map);
            return map;
        }

        @Nullable
        Object objectWithInferredType0(Object using, @NotNull SerializationStrategy strategy, Class type) throws InvalidMarshallableException {
            int code = peekCode();
            switch (code) {
                case '?':
                    return map(Object.class, Object.class, (Map) using);
                case '!':
                    return object(using, type);
                case '-':
                    if (peekCodeNext() == ' ')
                        return readList(indentation(), null);
                    return valueIn.readNumber();
                case '[':
                    return readSequence(strategy.type());
                case '{':
                    return valueIn.marshallableAsMap(Object.class, Object.class);
                case ']':
                    throw new IORuntimeException("Unexpected ] at " + bytes.toDebugString(32));
                case '}':
                    throw new IORuntimeException("Unexpected } at " + bytes.toDebugString(32));
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

            if (using instanceof Bytes)
                return valueIn.textTo((Bytes) using);

            if (using instanceof StringBuilder)
                return valueIn.textTo((StringBuilder) using);

            @Nullable String text = valueIn.text();
            if (text == null || Enum.class.isAssignableFrom(strategy.type()))
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

        @Nullable
        protected Object readNumber() {
            @Nullable String s = text();
            @Nullable String ss = s;
            if (s == null || s.length() > 40)
                return s;

            if (s.contains("_"))
                ss = s.replace("_", "");
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
                    return LocalTime.parse("0" + s);
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
        private Object readSequence(@NotNull Class clazz) {
            if (clazz == Object[].class || clazz == Object.class) {
                //todo should this use reflection so that all array types can be handled
                @NotNull List<Object> list = new ArrayList<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.object(Object.class));
                    }
                });
                return clazz == Object[].class ? list.toArray() : list;
            } else if (clazz == String[].class) {
                @NotNull List<String> list = new ArrayList<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.text());
                    }
                });
                return list.toArray(new String[0]);
            } else if (clazz == List.class) {
                @NotNull List<String> list = new ArrayList<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.text());
                    }
                });
                return list;
            } else if (clazz == Set.class) {
                @NotNull Set<String> list = new HashSet<>();
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

        @Override
        public String toString() {
            return TextWire.this.toString();
        }
    }

    @Override
    public boolean writingIsComplete() {
        return !writeContext.isNotComplete();
    }

    @Override
    public void rollbackIfNotComplete() {
        writeContext.rollbackIfNotComplete();
    }
}
