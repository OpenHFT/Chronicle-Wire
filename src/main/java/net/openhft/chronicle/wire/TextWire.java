/*
 * Copyright 2016-2025 chronicle.software
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
import static net.openhft.chronicle.wire.Wires.*;

/**
 * A YAML-based wire format optimised for human readability. It supports reading
 * and writing objects in a YAML-like form and encapsulates the
 * peculiarities of that text format.
 *
 * <p>It is often used for configuration, debugging and interoperability with
 * systems that expect YAML. While compatibility is a goal, the
 * implementation is tuned for performance within the Chronicle ecosystem.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked", "this-escape"})
public class TextWire extends YamlWireOut<TextWire> {

    /** Prefix used when emitting binary data. */
    public static final BytesStore<?, ?> BINARY = BytesStore.from("!!binary");

    /** Marker for explicit type information within the text wire. */
    public static final @NotNull Bytes<byte[]> TYPE_STR = Bytes.from("type ");

    /** Keyword for representing a sequence as a map. */
    static final String SEQ_MAP = "!seqmap";

    /** Characters that terminate events or values when reading. */
    static final BitSet END_CHARS = new BitSet();

    /**
     * Provides thread-local testers for escaping rules so allocations are
     * avoided during parsing.
     */
    static final ThreadLocal<WeakReference<StopCharTester>> ESCAPED_QUOTES = new ThreadLocal<>();

    /** Thread-local tester for text inside single quotes. */
    static final ThreadLocal<WeakReference<StopCharTester>> ESCAPED_SINGLE_QUOTES = new ThreadLocal<>();

    /** Thread-local tester for content that ends at the first terminator. */
    static final ThreadLocal<WeakReference<StopCharTester>> ESCAPED_END_OF_TEXT = new ThreadLocal<>();

    /** Thread-local tester enforcing strict end-of-text rules. */
    static final ThreadLocal<WeakReference<StopCharsTester>> STRICT_ESCAPED_END_OF_TEXT = new ThreadLocal<>();
    /** Pattern used when processing regular expression escapes. */
    static final Pattern REGX_PATTERN = Pattern.compile("\\.|\\$");

    /** Suppliers returning new stop char testers when thread locals are empty. */
    static final Supplier<StopCharTester> QUOTES_ESCAPING = StopCharTesters.QUOTES::escaping;

    /** Supplier for a tester that escapes single quoted text. */
    static final Supplier<StopCharTester> SINGLE_QUOTES_ESCAPING = StopCharTesters.SINGLE_QUOTES::escaping;

    /** Supplier for an end-of-text tester. */
    static final Supplier<StopCharTester> END_OF_TEXT_ESCAPING = TextStopCharTesters.END_OF_TEXT::escaping;

    /** Supplier for a strict end-of-text tester. */
    static final Supplier<StopCharsTester> STRICT_END_OF_TEXT_ESCAPING = TextStopCharsTesters.STRICT_END_OF_TEXT::escaping;

    /** Supplier for escaping event name delimiters. */
    static final Supplier<StopCharsTester> END_EVENT_NAME_ESCAPING = TextStopCharsTesters.END_EVENT_NAME::escaping;

    /** Marker used to denote meta-data documents. */
    static final Bytes<?> META_DATA = Bytes.from("!!meta-data");

    static {
        IOTools.unmonitor(BINARY);
        //for (char ch : "?%*&@`0123456789+- ',#:{}[]|>!\\".toCharArray())
        //for (char ch : "?,#:{}[]|>\\^".toCharArray())
        for (char ch : "#:}]".toCharArray())
            END_CHARS.set(ch);
        // Ensure the interner has loaded.
        WireInternal.INTERNER.valueCount();
    }

    /**
     * Primary {@link TextValueIn} instance used when deserialising values from
     * this wire.
     */
    protected final TextValueIn valueIn = createValueIn();

    /**
     * Byte position of the start of the current line used when calculating
     * indentation.
     */
    protected long lineStart = 0;

    /**
     * Supplies a fallback {@link ValueIn} when a field is absent.
     */
    private DefaultValueIn defaultValueIn;

    /**
     * The active {@link WriteDocumentContext} managing document boundaries.
     */
    protected WriteDocumentContext writeContext;

    /**
     * The active {@link ReadDocumentContext} managing document boundaries.
     */
    protected ReadDocumentContext readContext;

    /**
     * If true, parsing adheres to strict YAML rules and is less forgiving of
     * deviations.
     */
    private boolean strict = false;

    /**
     * Creates a wire backed by the provided bytes.
     *
     * @param bytes   underlying data store
     * @param use8bit if true strings are read and written using ISO-8859-1
     *                rather than UTF-8
     */
    public TextWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    /**
     * Creates a UTF-8 based wire backed by the provided bytes.
     *
     * @param bytes underlying data store
     */
    public TextWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    /**
     * Returns a new {@code TextWire} initialised with the contents of the named
     * file.
     *
     * @param name file path
     * @return wire over the file contents
     * @throws IOException if the file cannot be read
     */
    @NotNull
    public static TextWire fromFile(String name) throws IOException {
        return new TextWire(BytesUtil.readFile(name), true);
    }

    /**
     * Returns a new wire over the supplied text.
     *
     * @param text YAML-like string
     * @return wire instance containing that text
     */
    @NotNull
    public static TextWire from(@NotNull String text) {
        return new TextWire(Bytes.from(text));
    }

    /**
     * Converts any given wire into its textual representation. Handy for
     * debugging or logging.
     *
     * @param wire source wire
     * @return YAML-style text
     */
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
    /**
     * Unescapes YAML escape sequences in-place. See the YAML&nbsp;1.2.2 spec
     * section&nbsp;5.7 for details.
     *
     * @param sb text to modify in-place
     */
    public static <ACS extends Appendable & CharSequence> void unescape(@NotNull ACS sb) {
        int end = 0;
        int length = sb.length();
        for (int i = 0; i < length; i++) {
            char ch = sb.charAt(i);
            // Check if the character is an escape character and if there's a character after it
            if (ch == '\\' && i < length - 1) {
                char ch3 = sb.charAt(++i);
                // Handle different escaped characters
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
            // Set the unescaped character into the sequence
            AppendableUtil.setCharAt(sb, end++, ch);
        }
        // Validate the length consistency after unescaping
        if (length != sb.length())
            throw new IllegalStateException("Length changed from " + length + " to " + sb.length() + " for " + sb);
        AppendableUtil.setLength(sb, end);
    }

    /**
     * Returns a thread-local {@link StopCharTester} for text inside single
     * quotes.
     */
    @Nullable
    static StopCharTester getEscapingSingleQuotes() {
        // Fetch or create the StopCharTester from thread-local storage
        StopCharTester sct = ThreadLocalHelper.getTL(ESCAPED_SINGLE_QUOTES, SINGLE_QUOTES_ESCAPING);
        // Reset the StopCharTester instance
        sct.isStopChar(' ');
        return sct;
    }

    /**
     * Static utility to load and deserialise an object from a YAML-formatted file.
     *
     * @param filename file-path containing the YAML representation
     * @param <T>      the expected object type
     * @return deserialised instance created from the file contents
     * @throws IOException if the file can not be read
     */
    public static <T> T load(String filename) throws IOException, InvalidMarshallableException {
        return (T) TextWire.fromFile(filename).readObject();
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    /**
     * Returns whether strict parsing is enabled.
     */
    public boolean strict() {
        return strict;
    }

    /**
     * Enables or disables strict parsing mode.
     */
    public TextWire strict(boolean strict) {
        this.strict = strict;
        return this;
    }

    /**
     * Creates a method writer proxy for the given interface(s). Method calls on
     * the proxy will be serialised to this {@code TextWire} instance using
     * {@link WireType#TEXT}.
     */
    @Override
    @NotNull
    public <T> T methodWriter(@NotNull Class<T> tClass, Class<?>... additional) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.TEXT,
                () -> newTextMethodWriterInvocationHandler(tClass));
        for (Class<?> aClass : additional)
            builder.addInterface(aClass);
        useTextDocuments();
        builder.marshallableOut(this);
        return builder.build();
    }

    /**
     * Internal factory for creating the invocation handler used by text based
     * method writers.
     */
    @NotNull
    TextMethodWriterInvocationHandler newTextMethodWriterInvocationHandler(Class<?>... interfaces) {
        for (Class<?> anInterface : interfaces) {
            Comment c = Jvm.findAnnotation(anInterface, Comment.class);
            if (c != null)
                writeComment(c.value());
        }
        return new TextMethodWriterInvocationHandler(interfaces[0], this);
    }

    /**
     * Creates a builder for a text-based method writer. The resulting writer
     * will serialise method calls to this {@code TextWire} instance.
     */
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

    /**
     * Initializes the read context for this TextWire instance.
     * If the read context is not already set, the default behavior is to use binary documents.
     */
    protected void initReadContext() {
        if (readContext == null)
            useBinaryDocuments();
        readContext.start();
    }

    /**
     * Switches to binary document mode where document boundaries are length
     * prefixed and metadata is kept separate from data.
     */
    @NotNull
    public TextWire useBinaryDocuments() {
        readContext = new BinaryReadDocumentContext(this);
        writeContext = new BinaryWriteDocumentContext(this);
        return this;
    }

    /**
     * Switches to text document mode where documents are separated by the
     * {@code ---} and {@code ...} markers.
     */
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

    /**
     * Protected factory method to create the {@link TextValueIn} used by this wire.
     */
    @NotNull
    protected TextValueIn createValueIn() {
        return new TextValueIn();
    }

    /**
     * Returns the remaining readable content of the wire's buffer.
     * If more than 1&nbsp;MiB remains only the first MiB is returned followed by "..".
     */
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

    /**
     * Returns the remaining readable content as an ISO-8859-1 string.
     */
    public String to8bitString() {
        return bytes.to8bitString();
    }

    /**
     * Returns the remaining readable content as a UTF-8 string.
     */
    public String toUtf8String() {
        return bytes.toUtf8String();
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

    /**
     * Reads the next field name and appends it to the provided {@code StringBuilder}.
     */
    @NotNull
    protected StringBuilder readField(@NotNull StringBuilder builder) {
        consumePadding();
        try {
            int ch = peekCode();
            // 10xx xxxx, 1111 xxxx
            if (ch > 0x80 && ((ch & 0xC0) == 0x80 || (ch & 0xF0) == 0xF0)) {
                throw new IllegalStateException("Attempting to read binary as TextWire ch=" + Integer.toHexString(ch));
            }
            if (ch < 0 || ch == '!' || ch == '[' || ch == '{') {
                builder.setLength(0);
                return builder;
            }
            if (ch == '?') {
                bytes.readSkip(1);
                consumePadding();
                ch = peekCode();
            }
            if (ch == '"') {
                bytes.readSkip(1);

                parseUntil(builder, getEscapingQuotes());

                consumePadding();
                ch = readCode();
                if (ch != ':')
                    throw new UnsupportedOperationException("Expected a : at " + bytes.toDebugString() + " was " + (char) ch);

            } else if (ch == '\'') {
                bytes.readSkip(1);

                parseUntil(builder, getEscapingSingleQuotes());

                consumePadding();
                ch = readCode();
                if (ch != ':')
                    throw new UnsupportedOperationException("Expected a : at " + bytes.toDebugString() + " was " + (char) ch);

            } else if (ch < 0) {
                builder.setLength(0);
                return builder;

            } else {
                parseUntil(builder, getEscapingEndOfText());
                trimTheEnd(builder);

            }
            unescape(builder);

        } catch (BufferUnderflowException e) {
            Jvm.debug().on(getClass(), e);
        }
        return builder;
    }

    /**
     * Internal utility to remove trailing whitespace from a {@code StringBuilder}.
     */
    private void trimTheEnd(@NotNull StringBuilder builder) {
        while (builder.length() > 0 && Character.isWhitespace(builder.charAt(builder.length() - 1)))
            builder.setLength(builder.length() - 1);
    }

    /**
     * Reads the next event key and attempts to convert it to {@code expectedClass}.
     */
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
        return toExpected(expectedClass, builder);
    }

    /**
     * Specifies the default class to assume for map keys if not otherwise specified.
     */
    protected Class<?> defaultKeyClass() {
        return Object.class;
    }

    /**
     * Converts the provided StringBuilder's content to an instance of the expected class.
     * The content of the StringBuilder is interned before the conversion.
     *
     * @param expectedClass The class to which the StringBuilder's content should be converted.
     * @param sb The StringBuilder containing the data to be converted.
     * @return An instance of the expected class, converted from the StringBuilder's content.
     */
    @Nullable
    private <K> K toExpected(Class<K> expectedClass, StringBuilder builder) {
        return ObjectUtils.convertTo(expectedClass, WireInternal.INTERNER.intern(builder));
    }

    /**
     * Returns a thread-local {@link StopCharTester} configured for parsing text
     * until the end of a scalar value, respecting YAML escapes.
     */
    @NotNull
    protected StopCharTester getEscapingEndOfText() {
        StopCharTester escaping = ThreadLocalHelper.getTL(ESCAPED_END_OF_TEXT, END_OF_TEXT_ESCAPING);
        // reset it.
        escaping.isStopChar(' ');
        return escaping;
    }

    /**
     * Returns a thread-local {@link StopCharsTester} for strict end-of-text detection.
     */
    @NotNull
    protected StopCharsTester getStrictEscapingEndOfText() {
        StopCharsTester escaping = ThreadLocalHelper.getTL(STRICT_ESCAPED_END_OF_TEXT, STRICT_END_OF_TEXT_ESCAPING);
        // reset it.
        escaping.isStopChar(' ', ' ');
        return escaping;
    }

    /**
     * Returns a thread-local {@link StopCharsTester} for parsing an event name.
     */
    @NotNull
    protected StopCharsTester getEscapingEndEventName() {
        StopCharsTester escaping = ThreadLocalHelper.getTL(STRICT_ESCAPED_END_OF_TEXT, END_EVENT_NAME_ESCAPING);
        escaping.isStopChar(' ', ' ');
        return escaping;
    }

    /**
     * Returns a thread-local {@link StopCharTester} for parsing a quoted section.
     */
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
    /**
     * Consumes whitespace and comment lines. The {@code commas} parameter
     * indicates how many comma separators are expected to be consumed as part of
     * this padding; if more commas are found than expected and they are followed
     * by structural characters, padding stops.
     */
    public void consumePadding(int commas) {
        for (; ; ) {
            int codePoint = peekCode();
            switch (codePoint) {
                case '#':
                    // Handle comment lines.
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
                    // Handle commas.
                    if (valueIn.isASeparator(peekCodeNext()) && commas-- <= 0)
                        return;
                    bytes.readSkip(1);
                    if (commas == 0)
                        return;
                    break;
                case ' ':
                case '\t':
                    // Consume spaces and tabs.
                    bytes.readSkip(1);
                    break;
                case '\n':
                case '\r':
                    // Handle new lines.
                    this.lineStart = bytes.readPosition() + 1;
                    bytes.readSkip(1);
                    break;
                default:
                    return;
            }
        }
    }

    /**
     * Checks if the given character code is not a newline character.
     *
     * @param readCode The character code to be checked.
     * @return True if the code is not a newline character and not end-of-file, otherwise false.
     */
    private boolean notNewLine(int readCode) {
        return readCode >= 0 && readCode != '\r' && readCode != '\n';
    }

    /**
     * Consumes the YAML document start marker ({@code ---}) and any associated
     * directives or leading whitespace/comments.
     */
    protected void consumeDocumentStart() {
        // Check if there are at least 4 bytes remaining to read.
        if (bytes.readRemaining() > 4) {
            long pos = bytes.readPosition();
            // Look for the sequence of three '-' characters.
            if (bytes.readByte(pos) == '-' && bytes.readByte(pos + 1) == '-' && bytes.readByte(pos + 2) == '-') {
                bytes.readSkip(3);

                consumeWhiteSpace();

                pos = bytes.readPosition();
                // Parse the next word in the byte stream.
                @NotNull String word = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
                // Check the word against known document start words.
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

    /**
     * Internal method to peek the next character code without advancing.
     */
    int peekCode() {
        return bytes.peekUnsignedByte();
    }

    /**
     * Internal method to peek one character ahead without advancing.
     */
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

    /**
     * Internal method to read the next character code from the buffer.
     */
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

    /**
     * Core implementation used by the various {@code read} overloads. The
     * method first checks any previously parsed fields stored in the current
     * {@link ValueInState}. New fields are then read from the wire until a match
     * for {@code keyName} is found. If the name matches (case-insensitive) the
     * associated value becomes available via the returned {@link ValueIn}.
     * Fields that do not match are remembered in the state so that a later read
     * with a different key may pick them up. If no match is found a
     * {@link DefaultValueIn} initialised with {@code defaultValue} is returned.
     *
     * @param keyName      the name of the field to search for
     * @param keyCode      identifier used when marshalling numbers
     * @param defaultValue default value to supply when the field is absent
     * @return a {@link ValueIn} positioned on the matched value or a default one
     */
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

        // Iterate while bytes remain.
        while (bytes.readRemaining() > 0) {
            long position = bytes.readPosition();
            // at the current position look for the field.
            valueIn.consumeAny = true;
            readField(stringBuilder);
            valueIn.consumeAny = false;
            // might have changed due to readField in JSONWire
            curr = valueIn.curr();

            // If the field matches the required key, return its value.
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

        // Continuation of the read operation (possibly handles edge cases or fallbacks).
        return read2(keyName, keyCode, defaultValue, curr, stringBuilder);
    }

    /**
     * Continuation of the named-field lookup used by {@link #read(WireKey)}. It
     * scans any positions remembered as out-of-order by {@code curr}. When a
     * remembered field matches {@code keyName} its value is returned and the
     * remembered position removed. If none of the stored positions match the
     * search key a {@link DefaultValueIn} containing {@code defaultValue} is
     * returned.
     *
     * @param keyName      field name being searched for
     * @param keyCode      identifier used when marshalling numbers
     * @param defaultValue default value to supply when the field is absent
     * @param state        state tracking previously read fields
     * @param scratch      scratch buffer used for name comparison
     * @return a {@link ValueIn} positioned on the matched value or a default one
     */
    protected ValueIn read2(CharSequence keyName, int keyCode, Object defaultValue,
                            @NotNull ValueInState state,
                            @NotNull StringBuilder scratch) {
        final long position2 = bytes.readPosition();

        // if not a match go back and look at old fields.
        for (int i = 0; i < state.unexpectedSize(); i++) {
            bytes.readPosition(state.unexpected(i));
            valueIn.consumeAny = true;
            readField(scratch);
            valueIn.consumeAny = false;
            if (scratch.length() == 0 || StringUtils.equalsCaseIgnore(scratch, keyName)) {
                // if an old field matches, remove it, save the current position
                state.removeUnexpected(i);
                state.savedPosition(position2 + 1);
                return valueIn;
            }
        }
        bytes.readPosition(position2);

        // If no matching field is found, return the default value.
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

    /**
     * Consumes and skips over white space characters from the current position in the byte stream.
     */
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

    /**
     * Parses a single word from the wire and appends it to {@code sb}. A word is
     * delimited by whitespace.
     *
     * @param sb destination for the parsed characters
     */
    public void parseWord(@NotNull StringBuilder sb) {
        parseUntil(sb, StopCharTesters.SPACE_STOP);
    }

    /**
     * Parses text from the wire into {@code sb} until {@code testers} signals a
     * stop.
     *
     * @param target      destination for the parsed characters
     * @param stopTester stop condition
     */
    public void parseUntil(@NotNull StringBuilder target, @NotNull StopCharTester stopTester) {
        if (use8bit)
            bytes.parse8bit(target, stopTester);
        else
            bytes.parseUtf8(target, stopTester);
    }

    /**
     * Clears {@code sb} and parses text until {@code testers} requests a stop.
     *
     * @param target      destination builder that will be cleared before use
     * @param stopTester stop condition operating on multiple characters
     */
    public void parseUntil(@NotNull StringBuilder target, @NotNull StopCharsTester stopTester) {
        target.setLength(0);
        if (use8bit) {
            AppendableUtil.read8bitAndAppend(bytes, target, stopTester);
        } else {
            AppendableUtil.readUTFAndAppend(bytes, target, stopTester);
        }
    }

    /**
     * Attempts to parse the next YAML structure from the wire. Document start
     * markers and indentation are consumed before delegating to
     * {@link #getValueIn()}.
     *
     * @return the parsed object or {@code null} if at end of data
     * @throws InvalidMarshallableException if the structure cannot be read
     */
    @Nullable
    public Object readObject() throws InvalidMarshallableException {
        consumePadding();
        consumeDocumentStart();
        return getValueIn().object(Object.class);
    }

    /**
     * Variant of {@link #readObject()} that expects nested structures indented
     * at the supplied level. The method inspects the next character to decide
     * whether a list, map, typed object or scalar should be parsed.
     *
     * @param indentation indentation level of the current block
     * @return the parsed object, {@link NoObject#NO_OBJECT} when a closing token
     *         is encountered, or {@code null} at end of data
     * @throws InvalidMarshallableException if the structure cannot be read
     */
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

    /**
     * Determines the indentation of the current line by calculating the difference
     * between the current read position and the start of the line.
     *
     * @return The amount of indentation in terms of the number of characters from
     *         the start of the line to the current read position.
     */
    private int indentation() {
        long pos = bytes.readPosition();
        if (pos < lineStart) {
            lineStart = pos;
            return 0;
        }
        return Maths.toInt32(pos - lineStart);
    }

    /**
     * Helper for {@link #readObject(int)} that processes a {@code !type} entry.
     * The next value is read with the explicit type from the text.
     *
     * @return the typed object
     * @throws InvalidMarshallableException if marshalling fails
     */
    @Nullable
    private Object readTypedObject() throws InvalidMarshallableException {
        return valueIn.object(Object.class);
    }

    /**
     * Parses a bracketed list such as {@code [a, b]}. The text wire does not yet
     * support this syntax so the method always throws.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @NotNull
    private List readList() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads a YAML sequence where each item is denoted by {@code -}. Parsing
     * continues until the indentation drops below {@code indentation}.
     *
     * @param indentation indentation level marking list items
     * @param itemType    expected element class or {@code null}
     * @return list of parsed elements
     * @throws InvalidMarshallableException if an element cannot be parsed
     */
    @NotNull
    List readList(int indentation, Class<?> itemType) throws InvalidMarshallableException {
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
                objects.add(valueIn.objectWithInferredType(null, SerializationStrategies.ANY_OBJECT, itemType));
            } else {
                @Nullable Object e = readObject(indentation);
                if (e != NoObject.NO_OBJECT)
                    objects.add(e);
            }
            consumePadding(1);
        }

        return objects;
    }

    /**
     * Reads a YAML mapping at the supplied indentation. Keys are parsed as
     * strings followed by values obtained via {@link ValueIn#object(Class)}.
     * Parsing stops when the indentation decreases or the special key
     * {@code ...} is encountered.
     *
     * @param indentation indentation level marking map entries
     * @param mapValueType   expected value class or {@code null}
     * @return the populated map
     * @throws InvalidMarshallableException if a value cannot be parsed
     */
    @NotNull
    private Map readMap(int indentation, Class<?> mapValueType) throws InvalidMarshallableException {
        @NotNull Map map = new LinkedHashMap<>();
        consumePadding();
        while (bytes.readRemaining() > 0) {
            if (indentation() < indentation || bytes.readRemaining() == 0)
                break;
            @Nullable String key = readAndIntern();
            if (key.equals("..."))
                break;
            @Nullable Object value = valueIn.objectWithInferredType(null, SerializationStrategies.ANY_OBJECT, mapValueType);
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
    /**
     * Checks whether the current wire position starts with the
     * {@code !!meta-data} tag. If present the tag is consumed.
     *
     * @return {@code true} when the prefix was found and skipped
     */
    public boolean hasMetaDataPrefix() {
        if (bytes.startsWith(META_DATA)
                && bytes.peekUnsignedByte(bytes.readPosition() + 11) <= ' ') {
            bytes.readSkip(12);
            return true;
        }
        return false;
    }

    /**
     * Enum representing the absence of an object.
     */
    enum NoObject {NO_OBJECT}

    /**
     * Provides the {@link ValueIn} implementation for {@link TextWire},
     * handling deserialisation of values in a YAML-like text format.
     */
    public class TextValueIn implements ValueIn {

        /**
         * Maintains nested parsing state so complex structures can be read
         * incrementally.
         */
        final ValueInStack stack = new ValueInStack();

        /**
         * Tracks how many sequence items remain to be read in the current
         * context.
         */
        int sequenceLimit = 0;

        /**
         * Set while {@link #readLengthMarshallable()} consumes the underlying
         * text to measure its length.
         */
        private boolean consumeAny;

        @Override
        public void resetState() {
            stack.reset();
        }

        /**
         * Pushes the current reading state onto the stack, allowing for
         * nested or sequential value reading.
         */
        public void pushState() {
            stack.push();
        }

        /**
         * Pops the most recent reading state from the stack, reverting
         * to the previous state.
         */
        public void popState() {
            stack.pop();
        }

        /**
         * Retrieves the current state from the stack.
         *
         * @return The current state of reading.
         */
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

        /**
         * Core logic for reading a textual value into {@code a}. Handles quoted
         * strings, unquoted text, YAML tags such as {@code !null} and
         * {@code !binary}, and resolves anchors or aliases. The raw text from
         * the tokeniser is unescaped before returning.
         */
        @SuppressWarnings("fallthrough")
        @Nullable <ACS extends Appendable & CharSequence> CharSequence textTo0(@NotNull ACS dest) {
            consumePadding();
            int ch = peekCode();
            @Nullable CharSequence ret = dest;

            switch (ch) {
                case '{': {
                    // For map-like structures: read the length of the content and append to the target appendable
                    final long len = readLength();
                    try {
                        dest.append(Bytes.toString(bytes, bytes.readPosition(), len));
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                    // Advance the reading position by length of the content
                    bytes.readSkip(len);

                    // Move to the next comma or the end of the map
                    bytes.skipTo(StopCharTesters.COMMA_STOP);

                    return dest;

                }
                case '"':
                    readText(dest, getEscapingQuotes());
                    break;

                case '\'':
                    readText(dest, getEscapingSingleQuotes());
                    break;

                case '!': {
                    // Handle explicit typing (e.g. "!null" or "!type")

                    bytes.readSkip(1);
                    final StringBuilder stringBuilder = acquireStringBuilder();
                    parseWord(stringBuilder);
                    if (StringUtils.isEqual(stringBuilder, "!null")) {
                        textTo(stringBuilder);
                        ret = null;
                    } else {
                        // ignore the type.
                        if (dest instanceof StringBuilder) {
                            textTo((StringBuilder) dest);
                        } else {
                            textTo(stringBuilder);
                            ret = stringBuilder;
                        }
                    }
                    break;
                }

                case -1:
                    // End of input
                    return "";

                case '$':
                    // For variable substitution syntax (e.g. "${variable}")
                    if (peekCodeNext() == '{') {
                        unsubstitutedString(dest);
                        return dest;
                    }
                    // fall through

                default: {
                    // Handle other types of inputs

                    final long rem = bytes.readRemaining();
                    if (rem > 0) {
                        if (dest instanceof Bytes) {
                            bytes.parse8bit((Bytes) dest, getStrictEscapingEndOfText());
                        } else if (use8bit) {
                            bytes.parse8bit((StringBuilder) dest, getStrictEscapingEndOfText());
                        } else {
                            bytes.parseUtf8(dest, getStrictEscapingEndOfText());
                        }
                        // If nothing was read, throw an exception
                        if (rem == bytes.readRemaining())
                            throw new IORuntimeException("Nothing to read at " + bytes.toDebugString(32));
                    } else {
                        // Clear the target appendable if no remaining content
                        AppendableUtil.setLength(dest, 0);
                    }
                    // trim trailing spaces.
                    while (dest.length() > 0) {
                        if (Character.isWhitespace(dest.charAt(dest.length() - 1)))
                            AppendableUtil.setLength(dest, dest.length() - 1);
                        else
                            break;
                    }
                    break;
                }
            }

            // Peek the previous character and revert position if it's an end character (e.g., ',', ']', '}')
            int prev = peekBack();
            if (END_CHARS.get(prev))
                bytes.readSkip(-1);
            return ret;
        }

        /**
         * Called when a variable substitution such as {@code ${name}} is
         * encountered but not expanded. A warning is logged and the literal
         * characters are copied into {@code a}.
         */
        private <ACS extends Appendable & CharSequence> void unsubstitutedString(@NotNull ACS dest) {
            String text = bytes.toString();
            // Limit the log output to 32 characters for brevity
            if (text.length() > 32)
                text = text.substring(0, 32);
            // Log a warning if an unsubstituted variable (e.g. ${var}) is found
            Jvm.warn().on(getClass(), "Found an unsubstituted ${} as " + text);
            char c;
            do {
                // Read the next character from bytes
                c = bytes.readChar();
                try {
                    // Append the read character to the provided appendable
                    dest.append(c);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
                // Continue reading until the end of the variable substitution syntax (i.e., '}')
            } while (!bytes.isEmpty() && c != '}');
        }

        /**
         * Helper used by {@link #textTo0(Appendable)} to read text delimited by
         * {@code quotes}. The surrounding quote is skipped, the body parsed and
         * unescaped, then any trailing padding is consumed.
         */
        private <ACS extends Appendable & CharSequence> void readText(@NotNull ACS dest, @NotNull StopCharTester quotes) {
            bytes.readSkip(1); // consume opening quote
            if (use8bit)
                bytes.parse8bit(dest, quotes);
            else
                bytes.parseUtf8(dest, quotes);
            unescape(dest);
            consumePadding(1);
        }

        /**
         * Peeks at the last significant character that was written or parsed,
         * rewinding over spaces. If a newline is encountered the line start
         * position is adjusted. The value is used when deciding whether to
         * terminate values or handle optional commas.
         */
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
        public WireIn bytesMatch(@NotNull BytesStore<?, ?> compareBytes, BooleanConsumer consumer) {
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

        /**
         * Calculates the length of the current textual value by temporarily
         * consuming it. The read position is restored once the measurement is
         * complete.
         */
        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            this.consumeAny = true;
            try {
                consumeAny();
                return bytes.readPosition() - start;
            } finally {
                this.consumeAny = false;
                bytes.readPosition(start);
                // @TODO - use ScopedResource<StringBuilder> for consistency throughout YamlWireOut - https://github.com/OpenHFT/Chronicle-Wire/issues/879
                sb.setLength(0);
            }
        }

        /**
         * Recursively consumes the current YAML value without constructing an
         * object. Maps, sequences, scalars and typed values are skipped so that
         * the caller can move to the next field or token.
         */
        protected void consumeAny() {
            consumePadding();
            int code = peekCode();
            switch (code) {
                case '$': {
                    // Skip the '$' character
                    bytes.readSkip(1);
                    // If it's a variable (e.g., ${var}), consume until the ending curly brace
                    if (peekCode() == '{')
                        bytes.parse8bit(StopCharTesters.CURLY_STOP);
                    break;
                }
                case '{': {
                    // Consume the content of a map structure
                    consumeMap();
                    break;
                }
                case '[': {
                    // Consume the content of a sequence/array
                    consumeSeq();
                    break;
                }
                case '}':
                    break;
                case ']':
                    // Just break if the end of a map or sequence is detected
                    break;
                case '?':
                    // Consume a conditional statement (? key : value)
                    bytes.readSkip(1); // Skip the '?' character
                    consumeAny();
                    if (peekCode() == ':') {
                        bytes.readSkip(1); // Skip the ':' character
                        consumeAny();
                    }
                    break;
                case '!':
                    // Consume a type annotation (e.g., !int)
                    consumeType2();
                    break;

                case '"':
                case '\'':
                default:
                    // Consume any other value
                    consumeValue();
                    // Skip any trailing whitespace or padding
                    while (peekBack() <= ' ' && bytes.readPosition() >= 0)
                        bytes.readSkip(-1);
                    // Check for comma separator, if present skip it
                    if (peekBack() == ',') {
                        bytes.readSkip(-1);
                        break;
                    }
                    // Consume any padding after the value
                    consumePadding();
                    // Check for a key-value separator and consume the associated value if present
                    if (peekCode() == ':' && isASeparator(peekCodeNext())) {
                        readCode();
                        consumeAny();
                    }
                    break;
            }
        }

        /**
         * Checks whether the provided character acts as a separator.
         *
         * @param nextChar Character to be checked
         * @return true if it's a separator, otherwise false
         */
        protected boolean isASeparator(int nextChar) {
            return TextStopCharsTesters.isASeparator(nextChar);
        }

        /**
         * Consumes a {@code !type} tag and any trailing characters. The
         * following value is then consumed by {@link #consumeAny()}.
         */
        private void consumeType2() {
            // Skip the '!' character which indicates the start of a type annotation
            bytes.readSkip(1);

            // Check if the next characters match the constant TYPE_STR
            boolean type = bytes.startsWith(TYPE_STR);
            if (type)
                bytes.readSkip(TYPE_STR.length()); // Skip the matched length

            // Consume characters until the end of the type annotation is found
            while (!END_OF_TYPE.isStopChar(peekCode()))
                bytes.readSkip(1);

            // If there's a semicolon after the type annotation, skip it
            if (peekCode() == ';')
                bytes.readSkip(1);

            // If the TYPE_STR was not matched earlier, consume any subsequent characters
            if (!type)
                consumeAny();
        }

        /**
         * Consumes a YAML sequence without creating objects. Used by
         * {@link #consumeAny()} when skipping over values.
         */
        private void consumeSeq() {
            int code;

            // Skip the opening '[' character
            bytes.readSkip(1);
            for (; ; ) {
                // Save the current reading position
                long pos = bytes.readPosition();

                // Consume any type of data within the sequence
                consumeAny();

                // If a comma separator is found, skip it, and continue consumption
                if (peekCode() == ',' && isASeparator(peekCodeNext()))
                    readCode();
                else
                    break; // Break if no comma separator is found

                // Prevent infinite loops by checking if reading position hasn't advanced
                if (bytes.readPosition() == pos)
                    throw new IllegalStateException("Stuck at pos " + pos + " " + bytes);
            }

            // Consume any leading whitespace or padding
            consumePadding();

            // Read the next character
            code = readCode();

            // Ensure that the sequence is properly closed with a ']'
            if (code != ']') {
                bytes.readSkip(-1);
                throw new IllegalStateException("Expected a ] was " + bytes);
            }
        }

        /**
         * Consumes a YAML map without materialising its contents. Invoked from
         * {@link #consumeAny()} when skipping values or reading out of order.
         */
        private void consumeMap() {
            int code;

            // Skip the opening '{' character for the map
            bytes.readSkip(1);
            for (; ; ) {
                // Save the current reading position
                long pos = bytes.readPosition();

                // Consume any type of data within the map (both keys and values)
                consumeAny();

                // Check the next character
                int code2 = peekCode();

                // Break if we've reached the end of the map or another structure, or end of the stream
                if (code2 == '}' || code2 == ']' || code2 <= 0) {
                    break;
                } else if (code2 == ',' && isASeparator(peekCodeNext())) { // Consume the separator between key-value pairs
                    readCode();
                }

                // Prevent infinite loops by checking if the reading position hasn't advanced
                if (bytes.readPosition() == pos)
                    throw new IllegalStateException("Stuck at pos " + pos + " " + bytes);
            }
            consumePadding();

            // Read the next character to ensure the map is closed properly
            code = readCode();
            if (code != '}') {
                bytes.readSkip(-1);
                throw new IllegalStateException("Expected a } was " + (char) code);
            }
        }

        /**
         * Consumes a scalar or typed value. This method defers to
         * {@link #consumeAny()} for nested structures.
         */
        private void consumeValue() {
            consumePadding();
            final StringBuilder stringBuilder = acquireStringBuilder();

            // If the value has a type annotation, handle it
            if (peekCode() == '!') {
                bytes.readSkip(1); // Skip the '!' character
                parseWord(stringBuilder);
                if (StringUtils.isEqual(stringBuilder, "type")) { // If it's a type value, consume the type
                    consumeType();
                } else { // Otherwise, consume whatever comes next
                    consumeAny();
                }
            } else {
                // Convert the remaining value to text
                textTo(stringBuilder);
            }
        }

        /**
         * Consumes characters making up a type name, stopping at a comma or
         * space. Used by {@link #consumeValue()}.
         */
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
            ti.accept(t, (int) int64());
            return TextWire.this;
        }

        /**
         * Parses the current text token as a {@code long}. Quotes and boolean
         * literals are handled and the method expects the token to contain plain
         * text.
         */
        long getALong() {
            final int code = peekCode();
            switch (code) {
                case '"':
                case '\'':
                    // Skip quote characters if present around a number (e.g., "123")
                    bytes.readSkip(1);
                    break;

                case 't':
                case 'T':
                case 'f':
                case 'F':
                    // For boolean values, return 1 for true and 0 for false
                    return bool() ? 1 : 0;
                case '$':
                    // Handle unsubstituted numbers, typically of the form ${someValue}
                    unsubstitutedNumber();
                    return 0; // return a default value of 0 for unsubstituted numbers

                case '{':
                case '[':
                    // Throw an exception if attempting to read a map or list as a number
                    throw new IORuntimeException("Cannot read a " + (char) code + " as a number");
            }

            // Read and return the long value from the stream
            return bytes.parseLong();
        }

        /**
         * Logs a warning when an unsubstituted variable such as {@code ${id}}
         * is encountered where a number was expected and then skips the literal
         * characters.
         */
        private void unsubstitutedNumber() {
            // Parse up to the closing character of the unsubstituted expression
            String s = bytes.parse8bit(StopCharTesters.CURLY_STOP);

            // Log a warning as this situation typically indicates a malformed or unexpected input
            Jvm.warn().on(getClass(), "Cannot read " + s + "} as a number, treating as 0");

            // Check the next character to see how to proceed
            if (",\n ".indexOf(peekCode()) >= 0)
                bytes.readSkip(1); // skip the current character if it's a comma, newline or space
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
            tl.accept(t, int64());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            // this parses a double and casts to a float, so there may be some loss of precision
            tf.accept(t, (float) float64());
            return TextWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            td.accept(t, float64());
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
                @Nullable final Class<?> typePrefix = typePrefix();
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

        /**
         * Handles the processing of a sequence, delegating to an overloaded version of itself.
         *
         * @param <T> The type of items in the lists.
         * @param list The main list that should be populated based on the buffer.
         * @param buffer A temporary buffer used for staging data.
         * @param bufferAdd A supplier function that can add items to the buffer.
         * @param reader0 This seems to be an unused reader, possibly for future extensions.
         * @return Returns a boolean indicating success/failure or some other status.
         * @throws InvalidMarshallableException if there's an error during the sequence processing.
         */
        public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) throws InvalidMarshallableException {
            // Currently, this method delegates to an overloaded version of itself, ignoring the reader0 parameter.
            return sequence(list, buffer, bufferAdd);
        }

        @Override
        public <T> boolean sequence(@NotNull List<T> list, @NotNull List<T> buffer, @NotNull Supplier<T> bufferAdd) throws InvalidMarshallableException {

            list.clear();
            consumePadding();

            char code = (char) peekCode();
            if (code == '!') {
                @Nullable final Class<?> typePrefix = typePrefix();
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
                list.add(object(t, (Class<T>) t.getClass()));
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
        public Class<?> typePrefix() {
            consumePadding();
            int code = peekCode();
            if (code == '!' || code == '@') {
                readCode();

                final StringBuilder stringBuilder = acquireStringBuilder();
                stringBuilder.setLength(0);
                parseUntil(stringBuilder, END_OF_TYPE);
                bytes.readSkip(-1);
                return classLookup().forName(stringBuilder);
            }
            return null;
        }

        @Override
        public Object typePrefixOrObject(Class<?> tClass) {
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
        private Object handleCNFE(Class<?> tClass, ClassNotFoundRuntimeException e, StringBuilder stringBuilder) {
            if (tClass == null) {
                if (GENERATE_TUPLES) {
                    return Wires.tupleFor(null, stringBuilder.toString());
                }
                String message = "Unable to load " + stringBuilder + ", is a class alias missing.";
                throw new ClassNotFoundRuntimeException(new ClassNotFoundException(message));
            }

            final String className = tClass.getName();

            String[] split = REGX_PATTERN.split(stringBuilder);
            if (split[split.length - 1].equalsIgnoreCase(tClass.getSimpleName())) {
                try {

                    return tClass.isInterface()
                            ? Wires.tupleFor(tClass, stringBuilder.toString())
                            : classLookup().forName(className);

                } catch (ClassNotFoundRuntimeException e1) {
                    throw e;
                }

            } else if (GENERATE_TUPLES && tClass.getClassLoader() != null && tClass.isInterface()) {
                return Wires.tupleFor(tClass, stringBuilder.toString());
            }

            throw e;
        }

        @Override
        public boolean isTyped() {
            consumePadding();
            int code = peekCode();
            return code == '!';
        }

        /**
         * Convert a code to a string representation, typically for error messages.
         *
         * @param code The code to convert.
         * @return A string representation of the code.
         */
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

        /**
         * Create and initialize an instance of a given class using the wire input.
         *
         * @param clazz The class to instantiate.
         * @return A new instance of the class initialized with the data from the wire.
         */
        @NotNull
        public Demarshallable demarshallable(@NotNull Class<?> clazz) {
            pushState();

            // Skip any padding or whitespace.
            consumePadding();
            int code = peekCode();

            // Handle type prefix indicated by '!' character.
            if (code == '!') {
                typePrefix(null, (o, x) -> { /* sets acquireStringBuilder(); */});
            }
            // Throw exception if unsupported type is encountered.
            else if (code != '{') {
                throw new IORuntimeException("Unsupported type " + stringForCode(code));
            }

            // Determine the length of the marshalled object.
            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            final long newLimit = position - 1 + len;
            Demarshallable object;
            try {
                // Limit reading to the size of the marshalled object to prevent reading past its end.
                bytes.readLimit(newLimit);
                bytes.readSkip(1); // skip the opening brace '{'
                consumePadding();

                object = Demarshallable.newInstance((Class<? extends Demarshallable>) clazz, TextWire.this);
            } finally {
                // Restore the original limit and position of the byte buffer.
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
                popState();
            }

            // Consume trailing padding after the marshalled object.
            consumePadding(1);
            code = readCode();
            if (code != '}')
                // If the object doesn't end with a closing brace '}', throw an exception.
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + ",code='" + (char) code + "', bytes=" + Bytes.toString(bytes, 1024)
                );

            // Return the created object.
            return object;
        }

        @Override
        @Nullable
        public <T> T typedMarshallable() throws InvalidMarshallableException {
            return (T) objectWithInferredType(null, SerializationStrategies.ANY_NESTED, null);
        }

        /**
         * Deserialize the wire input into a Map of a given key and value type.
         *
         * @param kClass    The class type of the key.
         * @param vClass    The class type of the value.
         * @param usingMap  An optional map to populate. If null, a new map will be created.
         * @return A Map populated with deserialized keys and values.
         * @throws InvalidMarshallableException If there's a problem deserializing the input.
         */
        @Nullable <K, V> Map<K, V> map(@NotNull final Class<K> kClass,
                                       @NotNull final Class<V> vClass,
                                       @Nullable Map<K, V> usingMap) throws InvalidMarshallableException {
            consumePadding();

            // If no map is provided, initialize a new one.
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

        /**
         * Deserialize a typed map from the wire input.
         *
         * @param kClazz       The class type of the key.
         * @param vClass       The class type of the value.
         * @param usingMap     The map to populate.
         * @param builder      A StringBuilder to use during deserialization.
         * @return The populated map or null if the input represents a null value.
         * @throws InvalidMarshallableException If there's a problem deserializing the input.
         */
        @Nullable
        private <K, V> Map<K, V> typedMap(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap, @NotNull StringBuilder builder) throws InvalidMarshallableException {
            // Parse the input until a space character is encountered.
            parseUntil(builder, StopCharTesters.SPACE_STOP);

            // Intern the parsed string to reduce memory usage.
            @Nullable String str = WireInternal.INTERNER.intern(builder);

            // If the string represents a null value.
            if (("!!null").contentEquals(builder)) {
                text();
                return null;

            // If the string indicates a sequence map type.
            } else if (("!" + SEQ_MAP).contentEquals(builder)) {
                consumePadding();
                int start = readCode();
                if (start != '[')
                    throw new IORuntimeException("Unsupported start of sequence : " + (char) start);

                // Read each map entry and populate the provided map.
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

            // Unsupported type.
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
            consumePadding(1);
            return l;
        }

        /**
         * After parsing a number this verifies whether the following byte is a
         * delimiter such as '}', ']' or ','. If it is, the read position is
         * rewound by one byte so the delimiter can be processed by the caller.
         */
        public void checkRewind() {
            // Peek at the previous character without changing the read position.
            int ch = peekBack();

            // Check if the character is one of the defined end characters.
            if (END_CHARS.get(ch))
                // Move the read position back by one byte.
                bytes.readSkip(-1);
        }

        /**
         * Variant used after reading a double. Currently delegates to
         * {@link #checkRewind()}.
         */
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
            int sep = 0;
            switch (peekCode()) {
                case '$':
                    unsubstitutedNumber();
                    return 0;
                case '[':
                case '{':
                    Jvm.warn().on(getClass(), "Unable to read " + valueIn.objectBestEffort() + " as a double.");
                    return 0;
                case '\'':
                case '"':
                    sep = bytes.readUnsignedByte();
                    break;
            }
            final double v = bytes.parseDouble();

            if (sep != 0) {
                int end = peekBack();
                if (end != sep)
                    throw new IORuntimeException("Expected " + (char) sep + " but was " + (char) end);
            } else {
                checkRewindDouble();
            }

            consumePadding(1);
            return v;
        }

        /**
         * If the current token begins with {@code !} this method consumes the
         * tag name so that subsequent parsing sees only the value.
         */
        void skipType() {
            // Peek at the next byte without changing the read position.
            long peek = bytes.peekUnsignedByte();

            // If the next byte is '!', indicating the start of a type string.
            if (peek == '!') {
                final StringBuilder stringBuilder = acquireStringBuilder();

                // Parse the type string until reaching an end-of-type character.
                parseUntil(stringBuilder, END_OF_TYPE);

                // Consume any padding after the type string.
                consumePadding();
            }
        }

        @Override
        public float float32() {
            // this parses a double and casts to a float, so there may be some loss of precision
            return (float) float64();
        }

        /**
         * Checks whether the next token is {@code !!null ""}. If so the token
         * is consumed and {@code true} returned; otherwise the stream is left
         * untouched and {@code false} is returned.
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
        public Object objectWithInferredType(Object using, @NotNull SerializationStrategy strategy, Class<?> type) throws InvalidMarshallableException {
            consumePadding();
            @Nullable Object o = objectWithInferredType0(using, strategy, type);
            consumePadding();
            int code = peekCode();
            if (code == ':' && strategy.bracketType() != BracketType.NONE) {
                return readRestOfMap(using, o);
            }
            return o;
        }

        /**
         * Reads the remaining content of the byte stream and builds it into a Map representation.
         * This method assumes that a key has already been read and consumes the corresponding value
         * from the stream, appending both to the resulting Map.
         *
         * @param using The object instance to be used for the result. It can be reused for efficiency.
         * @param o The key that has been read earlier.
         * @return The constructed map containing all key-value pairs.
         * @throws InvalidMarshallableException If any errors occur during the deserialization process.
         */
        @NotNull
        Object readRestOfMap(Object using, Object o) throws InvalidMarshallableException {
            readCode();
            consumePadding();

            // Infer the type of the value and deserialize it.
            @Nullable Object value = objectWithInferredType0(using, SerializationStrategies.ANY_OBJECT, Object.class);

            // Determine if the provided 'using' object is an instance of Map or create a new LinkedHashMap.
            @NotNull Map map = using instanceof Map ? (Map) using : new LinkedHashMap();
            map.put(o, value);
            readAllAsMap(Object.class, Object.class, map);
            return map;
        }

        /**
         * Core logic for deserialising an object when its type may be deduced
         * from tags, anchors or surrounding structure.
         *
         * @param using     optional instance to reuse
         * @param strategy  strategy describing how the value is bracketed
         * @param type      the default type to instantiate
         * @return the resulting object
         * @throws InvalidMarshallableException if an error occurs while reading
         */
        @Nullable
        Object objectWithInferredType0(Object using, @NotNull SerializationStrategy strategy, Class<?> type) throws InvalidMarshallableException {
            int code = peekCode();
            switch (code) {
                // Different cases for different object types or data representations.
                // Each case handles the deserialization logic for that specific representation.
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

            // Convert the content to a Bytes or StringBuilder if the using object is of that type.
            if (using instanceof Bytes)
                return valueIn.textTo((Bytes) using);

            if (using instanceof StringBuilder)
                return valueIn.textTo((StringBuilder) using);

            @Nullable String text = valueIn.text();
            if (text == null || Enum.class.isAssignableFrom(strategy.type()))
                return text;
            switch (text) {
                // Interpretation for boolean values.
                case "true":
                    return Boolean.TRUE;
                case "false":
                    return Boolean.FALSE;
                default:
                    return text;
            }
        }

        /**
         * Attempts to parse the current text token as a number or common
         * date/time format. Falls back to returning the text itself if no known
         * representation matches.
         *
         * @return the decoded value or the original string
         */
        @Nullable
        protected Object readNumber() {
            @Nullable String s = text();
            @Nullable String ss = s;

            // Return early if the string is null or unusually long.
            if (s == null || s.length() > 40)
                return s;

            // Handle possible number formatting using underscores.
            if (s.contains("_"))
                ss = s.replace("_", "");

            // Try decoding the string as a long.
            try {
                return Long.decode(ss);
            } catch (NumberFormatException fallback) {
                // If not a long, proceed to other formats.
            }

            // Try parsing the string as a double.
            try {
                return Double.parseDouble(ss);
            } catch (NumberFormatException fallback) {
                // If not a double, proceed to other formats.
            }

            // Try parsing the string as a LocalTime.
            try {
                if (s.length() == 7 && s.charAt(1) == ':')
                    return LocalTime.parse("0" + s);
                if (s.length() == 8 && s.charAt(2) == ':')
                    return LocalTime.parse(s);
            } catch (DateTimeParseException fallback) {
                // If not a LocalTime, proceed to other formats.
            }

            // Try parsing the string as a LocalDate.
            try {
                if (s.length() == 10)
                    return LocalDate.parse(s);
            } catch (DateTimeParseException fallback) {
                // If not a LocalDate, proceed to other formats.
            }

            // Try parsing the string as a ZonedDateTime.
            try {
                if (s.length() >= 22)
                    return ZonedDateTime.parse(s);
            } catch (DateTimeParseException fallback) {
                // If not a ZonedDateTime, fallback to returning the original string.
            }
            return s;
        }

        /**
         * Deserialises a YAML sequence into either an array or a {@link Collection}
         * of the requested type. Only a small set of collection types are
         * supported.
         */
        @NotNull
        private Object readSequence(@NotNull Class<?> clazz) {
            if (clazz == Object[].class || clazz == Object.class) {
                // TODO: Consider using reflection to handle all array types.
                @NotNull List<Object> list = new ArrayList<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.object(Object.class));
                    }
                });
                return clazz == Object[].class ? list.toArray() : list;

            // Handle sequences expected to be of type String[].
            } else if (clazz == String[].class) {
                @NotNull List<String> list = new ArrayList<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.text());
                    }
                });
                return list.toArray(new String[0]);

            // Handle sequences expected to be of type List.
            } else if (clazz == List.class) {
                @NotNull List<String> list = new ArrayList<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.text());
                    }
                });
                return list;

            // Handle sequences expected to be of type Set.
            } else if (clazz == Set.class) {
                @NotNull Set<String> list = new HashSet<>();
                sequence(list, (l, v) -> {
                    while (v.hasNextSequenceItem()) {
                        l.add(v.text());
                    }
                });
                return list;

            // Throw an exception if the class type is unsupported.
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
