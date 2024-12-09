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
import static net.openhft.chronicle.wire.Wires.*;

/**
 * A representation of the YAML-based wire format. `TextWire` provides functionalities
 * for reading and writing objects in a YAML-based format, and encapsulates various characteristics
 * of the YAML text format.
 *
 * <p>This class utilizes bit sets, thread locals, and regular expressions to efficiently handle
 * the YAML formatting nuances.
 *
 * <p><b>Important:</b> Some configurations and methods in this class are marked as deprecated
 * and are slated for removal in future versions, suggesting that its behavior might evolve in future releases.
 */
@SuppressWarnings({"rawtypes", "unchecked", "this-escape"})
public class TextWire extends YamlWireOut<TextWire> {

    // Constants representing specific textual constructs in YAML.
    public static final BytesStore<?, ?> BINARY = BytesStore.from("!!binary");
    public static final @NotNull Bytes<byte[]> TYPE_STR = Bytes.from("type ");
    static final String SEQ_MAP = "!seqmap";

    // A set of characters considered as "end characters" in this wire format.
    static final BitSet END_CHARS = new BitSet();

    // Thread locals for stop char testers that might need escaping in specific contexts.
    // They are weakly referenced to avoid potential memory leaks in multithreaded environments.
    static final ThreadLocal<WeakReference<StopCharTester>> ESCAPED_QUOTES = new ThreadLocal<>();//ThreadLocal.withInitial(StopCharTesters.QUOTES::escaping);
    static final ThreadLocal<WeakReference<StopCharTester>> ESCAPED_SINGLE_QUOTES = new ThreadLocal<>();//ThreadLocal.withInitial(() -> StopCharTesters.SINGLE_QUOTES.escaping());
    static final ThreadLocal<WeakReference<StopCharTester>> ESCAPED_END_OF_TEXT = new ThreadLocal<>();// ThreadLocal.withInitial(() -> TextStopCharsTesters.END_OF_TEXT.escaping());
    static final ThreadLocal<WeakReference<StopCharsTester>> STRICT_ESCAPED_END_OF_TEXT = new ThreadLocal<>();// ThreadLocal.withInitial(() -> TextStopCharsTesters.END_OF_TEXT.escaping());
    static final Pattern REGX_PATTERN = Pattern.compile("\\.|\\$");

    // Suppliers for various stop char testers.
    static final Supplier<StopCharTester> QUOTES_ESCAPING = StopCharTesters.QUOTES::escaping;
    static final Supplier<StopCharTester> SINGLE_QUOTES_ESCAPING = StopCharTesters.SINGLE_QUOTES::escaping;
    static final Supplier<StopCharTester> END_OF_TEXT_ESCAPING = TextStopCharTesters.END_OF_TEXT::escaping;
    static final Supplier<StopCharsTester> STRICT_END_OF_TEXT_ESCAPING = TextStopCharsTesters.STRICT_END_OF_TEXT::escaping;
    static final Supplier<StopCharsTester> END_EVENT_NAME_ESCAPING = TextStopCharsTesters.END_EVENT_NAME::escaping;

    // Metadata representation in bytes.
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
     * Input value parser specifically for text-based wire format.
     */
    protected final TextValueIn valueIn = createValueIn();

    /**
     * Represents the start of the current line being processed in the wire format.
     */
    protected long lineStart = 0;

    /**
     * Default value input utility.
     */
    private DefaultValueIn defaultValueIn;

    /**
     * Context for writing documents in the wire format.
     */
    protected WriteDocumentContext writeContext;

    /**
     * Context for reading documents from the wire format.
     */
    protected ReadDocumentContext readContext;

    /**
     * Flag to determine if strict parsing rules are applied.
     */
    private boolean strict = false;

    /**
     * Constructor to initialize the `TextWire` with a specific bytes representation
     * and a flag to determine if 8-bit encoding is to be used.
     *
     * @param bytes   Bytes representation.
     * @param use8bit Flag to determine if 8-bit encoding is to be used.
     */
    public TextWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    /**
     * Constructor that initializes the `TextWire` with bytes representation
     * with default 8-bit encoding turned off.
     *
     * @param bytes Bytes representation.
     */
    public TextWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    /**
     * Factory method to create a `TextWire` from a file.
     *
     * @param name Name of the file.
     * @return A new instance of `TextWire`.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public static TextWire fromFile(String name) throws IOException {
        return new TextWire(BytesUtil.readFile(name), true);
    }

    /**
     * Factory method to create a `TextWire` from a string representation.
     *
     * @param text String representation of the wire format.
     * @return A new instance of `TextWire`.
     */
    @NotNull
    public static TextWire from(@NotNull String text) {
        return new TextWire(Bytes.from(text));
    }

    /**
     * Converts any wire format into a text representation.
     *
     * @param wire The wire format to be converted.
     * @return The text representation of the wire format.
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
     * Processes and unescapes the provided {@link CharSequence} containing escaped sequences.
     * For instance, "\\n" is converted to a newline character, "\\t" to a tab, etc.
     * This method modifies the given sequence directly and adjusts its length if needed.
     *
     * @param sb A {@link CharSequence} that is also an {@link Appendable}, containing potentially escaped sequences.
     *           This sequence will be modified directly.
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
     * Acquires a {@link StopCharTester} instance related to escaping single quotes.
     * This method utilizes thread-local storage to ensure that the returned instance is thread-safe.
     *
     * @return A {@link StopCharTester} instance specifically designed for escaping single quotes,
     *         or null if such an instance could not be acquired.
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
     * Loads an Object from a file
     *
     * @param filename the file-path containing the object
     * @param <T>      the type of the object to load
     * @return an instance of the object created from the data in the file
     * @throws IOException if the file can not be found or read
     */
    public static <T> T load(String filename) throws IOException, InvalidMarshallableException {
        return (T) TextWire.fromFile(filename).readObject();
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    /**
     * Retrieves the current strict mode setting for this TextWire instance.
     *
     * @return A boolean indicating whether strict mode is enabled (true) or disabled (false).
     */
    public boolean strict() {
        return strict;
    }

    /**
     * Sets the strict mode for this TextWire instance.
     * When strict mode is enabled, the instance may enforce stricter parsing or serialization rules.
     *
     * @param strict A boolean value to set the strict mode. True to enable, false to disable.
     * @return The current TextWire instance, allowing for method chaining.
     */
    public TextWire strict(boolean strict) {
        this.strict = strict;
        return this;
    }

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
     * Creates a new textual method writer invocation handler based on provided interface(s).
     * If any of the provided interfaces have a {@link Comment} annotation,
     * the associated comment is written to the wire.
     *
     * @param interfaces One or more interfaces that the created handler should be aware of.
     * @return A newly instantiated {@link TextMethodWriterInvocationHandler} for the provided interface(s).
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
     * Configures this TextWire instance to use binary document contexts for reading and writing.
     * This will replace the current read and write contexts with binary contexts.
     *
     * @return The current TextWire instance, allowing for method chaining.
     */
    @NotNull
    public TextWire useBinaryDocuments() {
        readContext = new BinaryReadDocumentContext(this);
        writeContext = new BinaryWriteDocumentContext(this);
        return this;
    }

    /**
     * Configures this TextWire instance to use textual document contexts for reading and writing.
     * This will replace the current read and write contexts with textual contexts.
     *
     * @return The current TextWire instance, allowing for method chaining.
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
     * Creates and returns a new instance of TextValueIn.
     * This method is primarily intended for internal use to provide consistent access to TextValueIn instances.
     *
     * @return A new instance of TextValueIn.
     */
    @NotNull
    protected TextValueIn createValueIn() {
        return new TextValueIn();
    }

    /**
     * Converts the underlying bytes of this TextWire to its string representation.
     * For large byte sequences, only the initial part of the data is returned followed by "..".
     *
     * @return A string representation of the TextWire's underlying bytes.
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
     * Reads the field from the current position of the wire and appends it to the given StringBuilder.
     * The method handles different encodings and escape sequences, and ensures correct parsing of field data.
     *
     * @param sb The StringBuilder to which the field value should be appended.
     * @return The updated StringBuilder containing the field value.
     */
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

    /**
     * Trims trailing whitespace from the end of the given StringBuilder.
     * This utility method ensures that field values are read without trailing spaces.
     *
     * @param sb The StringBuilder to be trimmed.
     */
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

    /**
     * Returns the default class to be used as a key.
     * By default, this method returns the Object class.
     *
     * @return The default key class, which is {@link Object}.
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
    private <K> K toExpected(Class<K> expectedClass, StringBuilder sb) {
        return ObjectUtils.convertTo(expectedClass, WireInternal.INTERNER.intern(sb));
    }

    /**
     * Retrieves the StopCharTester that determines the end of text with escaping.
     * The tester is fetched from a thread-local storage and then reset.
     *
     * @return The StopCharTester for determining the end of text with escaping.
     */
    @NotNull
    protected StopCharTester getEscapingEndOfText() {
        StopCharTester escaping = ThreadLocalHelper.getTL(ESCAPED_END_OF_TEXT, END_OF_TEXT_ESCAPING);
        // reset it.
        escaping.isStopChar(' ');
        return escaping;
    }

    /**
     * Retrieves the StopCharsTester that determines the end of text with strict escaping.
     * The tester is fetched from thread-local storage and is then reset.
     *
     * @return The StopCharsTester for determining the end of text with strict escaping.
     */
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

        // Reset the stop characters tester to stop at space characters.
        escaping.isStopChar(' ', ' ');
        return escaping;
    }

    /**
     * Retrieves the StopCharTester that determines the end of a quoted text section with escaping.
     * The tester is fetched from thread-local storage and is then reset.
     *
     * @return The StopCharTester for determining the end of a quoted text section with escaping.
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
     * Consumes padding characters from the current reading position.
     * Padding characters include spaces, tabs, new lines, commas, and comments. This method also
     * handles skipping over any comments encountered during this process.
     *
     * @param commas The number of comma characters to consume. Once this count is reached, the method will return.
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
     * Consumes the start of a document in the byte stream. The start is determined by
     * the presence of three consecutive '-' characters followed by certain words
     * (e.g., "!!data", "!!meta-data").
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
     * Peeks the next unsigned byte from the current read position without advancing the pointer.
     *
     * @return The next unsigned byte as an integer.
     */
    int peekCode() {
        return bytes.peekUnsignedByte();
    }

    /**
     * Peeks the unsigned byte after the current read position without advancing the pointer.
     *
     * @return The unsigned byte after the current read position as an integer.
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
     * Reads the next byte as an unsigned integer.
     *
     * @return The next byte if available or -1 if end-of-file.
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
     * Reads the value associated with a given key name, code, and provides a default value.
     *
     * @param keyName The name of the key.
     * @param keyCode The code for the key.
     * @param defaultValue The default value to return if the key isn't found.
     * @return The value associated with the given key or the default value if not found.
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
        return read2(keyName, keyCode, defaultValue, curr, stringBuilder, keyName);
    }

    /**
     * Attempts to read the value of a given key, continuing the read operation from the primary `read` method.
     * If the current and old fields do not match the specified key, the default value is returned.
     *
     * @param keyName       The name of the key for which the value needs to be read.
     * @param keyCode       The code for the key.
     * @param defaultValue  The default value to return if the key isn't found.
     * @param curr          The current state of the ValueIn.
     * @param sb            The StringBuilder used to capture the field name.
     * @param name          The name of the key (same as keyName, possibly added for clarity in some cases).
     * @return              The value associated with the key or the default value if the key is not found.
     */
    protected ValueIn read2(CharSequence keyName, int keyCode, Object defaultValue, @NotNull ValueInState curr, @NotNull StringBuilder sb, @NotNull CharSequence name) {
        final long position2 = bytes.readPosition();

        // if not a match go back and look at old fields.
        for (int i = 0; i < curr.unexpectedSize(); i++) {
            bytes.readPosition(curr.unexpected(i));
            valueIn.consumeAny = true;
            readField(sb);
            valueIn.consumeAny = false;
            if (sb.length() == 0 || StringUtils.equalsCaseIgnore(sb, name)) {
                // if an old field matches, remove it, save the current position
                curr.removeUnexpected(i);
                curr.savedPosition(position2 + 1);
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
     * Parses a word from the current byte position until it encounters a space or stop character.
     * The parsed word is then appended to the provided StringBuilder.
     *
     * @param sb The StringBuilder to which the parsed word will be appended.
     */
    public void parseWord(@NotNull StringBuilder sb) {
        parseUntil(sb, StopCharTesters.SPACE_STOP);
    }

    /**
     * Parses characters from the current byte position until one of the specified stop characters
     * in the tester is encountered. The parsed characters are then appended to the provided StringBuilder.
     *
     * @param sb       The StringBuilder to which the parsed characters will be appended.
     * @param testers  A StopCharTester which determines which characters should stop the parsing.
     */
    public void parseUntil(@NotNull StringBuilder sb, @NotNull StopCharTester testers) {
        if (use8bit)
            bytes.parse8bit(sb, testers);
        else
            bytes.parseUtf8(sb, testers);
    }

    /**
     * Clears the StringBuilder and then parses characters from the current byte position
     * until one of the specified stop characters in the tester is encountered.
     * The parsed characters are then appended to the provided StringBuilder.
     *
     * @param sb       The StringBuilder to which the parsed characters will be appended.
     * @param testers  A StopCharsTester which determines which characters should stop the parsing.
     */
    public void parseUntil(@NotNull StringBuilder sb, @NotNull StopCharsTester testers) {
        sb.setLength(0);
        if (use8bit) {
            AppendableUtil.read8bitAndAppend(bytes, sb, testers);
        } else {
            AppendableUtil.readUTFAndAppend(bytes, sb, testers);
        }
    }

    /**
     * Reads and returns an object from the current position in the bytes stream.
     * Any padding and document start metadata are consumed before attempting to read the object.
     *
     * @return An instance of the read object, or null if the end of the stream is reached.
     * @throws InvalidMarshallableException If the object could not be properly unmarshalled.
     */
    @Nullable
    public Object readObject() throws InvalidMarshallableException {
        consumePadding();
        consumeDocumentStart();
        return getValueIn().object(Object.class);
    }

    /**
     * Attempts to read an object from the current position in the bytes stream based on its
     * detected type (e.g., list, map, typed object). The type of the object is inferred from
     * the current and next code. The method also considers the given indentation for nested structures.
     *
     * @param indentation The current indentation level to handle nested objects.
     * @return An instance of the read object, `NoObject.NO_OBJECT` if the object denotes
     *         an end of block or structure, or null if the end of the stream is reached.
     * @throws InvalidMarshallableException If the object could not be properly unmarshalled.
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
     * Reads a typed object from the current position in the bytes stream. The type
     * of the object is determined by the bytes' content.
     *
     * @return An instance of the read object.
     * @throws InvalidMarshallableException If the object could not be properly unmarshalled.
     */
    @Nullable
    private Object readTypedObject() throws InvalidMarshallableException {
        return valueIn.object(Object.class);
    }

    /**
     * Attempts to read a list from the current position in the bytes stream. This method
     * currently throws an UnsupportedOperationException, indicating that reading lists
     * directly is not supported in this context.
     *
     * @return This method does not currently return a value due to the exception.
     * @throws UnsupportedOperationException Always thrown since this method is not supported.
     */
    @NotNull
    private List readList() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads a list of objects from the current position in the bytes stream.
     * The method consumes lines starting with '-' as list elements, and utilizes
     * the provided indentation to understand nested structures.
     *
     * @param indentation The current indentation level to handle nested list items.
     * @param elementType The expected type of elements within the list. Used when inferring
     *                    the type of the read object.
     * @return A list containing objects read from the bytes stream.
     * @throws InvalidMarshallableException If any object within the list could not be properly unmarshalled.
     */
    @NotNull
    List readList(int indentation, Class<?> elementType) throws InvalidMarshallableException {
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

    /**
     * Reads a map of key-value pairs from the current position in the bytes stream.
     * This method utilizes the provided indentation to understand nested structures.
     * Each key is followed by its value. If a key named "..." is encountered, the parsing breaks.
     *
     * @param indentation The current indentation level to handle nested key-value pairs.
     * @param valueType The expected type of values within the map. Used when inferring
     *                  the type of the read object.
     * @return A map containing key-value pairs read from the bytes stream.
     * @throws InvalidMarshallableException If any key-value pair within the map could not be properly unmarshalled.
     */
    @NotNull
    private Map readMap(int indentation, Class<?> valueType) throws InvalidMarshallableException {
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

    /**
     * Enum representing the absence of an object.
     */
    enum NoObject {NO_OBJECT}

    /**
     * Represents a textual input value for deserialization. It manages a stack
     * of states, allowing for nested or sequential value reading.
     */
    public class TextValueIn implements ValueIn {

        /**
         * Stack maintaining the states of value readings,
         * allowing for nested structure reading.
         */
        final ValueInStack stack = new ValueInStack();

        /**
         * Limit for sequence reading.
         */
        int sequenceLimit = 0;

        /**
         * Flag to denote if any kind of reading should be consumed.
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

        @SuppressWarnings("fallthrough")
        @Nullable <ACS extends Appendable & CharSequence> CharSequence textTo0(@NotNull ACS a) {
            consumePadding();
            int ch = peekCode();
            @Nullable CharSequence ret = a;

            switch (ch) {
                case '{': {
                    // For map-like structures: read the length of the content and append to the target appendable
                    final long len = readLength();
                    try {
                        a.append(Bytes.toString(bytes, bytes.readPosition(), len));
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                    // Advance the reading position by length of the content
                    bytes.readSkip(len);

                    // Move to the next comma or the end of the map
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
                    // Handle explicit typing (e.g. "!null" or "!type")

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
                    // End of input
                    return "";

                case '$':
                    // For variable substitution syntax (e.g. "${variable}")
                    if (peekCodeNext() == '{') {
                        unsubstitutedString(a);
                        return a;
                    }
                    // fall through

                default: {
                    // Handle other types of inputs

                    final long rem = bytes.readRemaining();
                    if (rem > 0) {
                        if (a instanceof Bytes) {
                            bytes.parse8bit((Bytes) a, getStrictEscapingEndOfText());
                        } else if (use8bit) {
                            bytes.parse8bit((StringBuilder) a, getStrictEscapingEndOfText());
                        } else {
                            bytes.parseUtf8(a, getStrictEscapingEndOfText());
                        }
                        // If nothing was read, throw an exception
                        if (rem == bytes.readRemaining())
                            throw new IORuntimeException("Nothing to read at " + bytes.toDebugString(32));
                    } else {
                        // Clear the target appendable if no remaining content
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

            // Peek the previous character and revert position if it's an end character (e.g., ',', ']', '}')
            int prev = peekBack();
            if (END_CHARS.get(prev))
                bytes.readSkip(-1);
            return ret;
        }

        private <ACS extends Appendable & CharSequence> void unsubstitutedString(@NotNull ACS a) {
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
                    a.append(c);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
                // Continue reading until the end of the variable substitution syntax (i.e., '}')
            } while (!bytes.isEmpty() && c != '}');
        }

        private <ACS extends Appendable & CharSequence> void readText(@NotNull ACS a, @NotNull StopCharTester quotes) {
            // Skip the initial quote (either ' or ")
            bytes.readSkip(1);
            // Read the content based on the character encoding being used
            if (use8bit)
                bytes.parse8bit(a, quotes);  // Parse using 8-bit encoding
            else
                bytes.parseUtf8(a, quotes);  // Parse using UTF-8 encoding
            // Unescape any escape sequences found in the content
            unescape(a);
            // Consume any padding characters (e.g. whitespace)
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

        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            this.consumeAny = true;
            try {
                // Consume all data until a meaningful stopping point
                consumeAny();
                // Calculate and return the length of the consumed data
                return bytes.readPosition() - start;
            } finally {
                // Reset the consumption flag and reading position
                this.consumeAny = false;
                bytes.readPosition(start);
                // @TODO - use ScopedResource<StringBuilder> for consistency throughout YamlWireOut - https://github.com/OpenHFT/Chronicle-Wire/issues/879
                sb.setLength(0);
            }
        }

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
         * Consumes the type annotation (e.g., `!type`) in the text format.
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
         * Consumes a sequence or list (e.g., `[item1, item2]`) in the text format.
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
         * Consumes a map structure (e.g., `{key1: value1, key2: value2}`) in the text format.
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
         * Consumes a value, which can be a primitive, type-annotated value, or another structure.
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
         * Consumes a type, which is expected to end with a comma, space or another stop character.
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
         * Retrieves a long value from the current position in the stream.
         * It can handle quotes, booleans, or actual numbers.
         *
         * @return the long value from the stream or a default/fallback value in case of unconventional formats.
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
         * Handles the scenario where a number is expected, but an unsubstituted expression is found instead.
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
         * @param sb           A StringBuilder to use during deserialization.
         * @return The populated map or null if the input represents a null value.
         * @throws InvalidMarshallableException If there's a problem deserializing the input.
         */
        @Nullable
        private <K, V> Map<K, V> typedMap(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap, @NotNull StringBuilder sb) throws InvalidMarshallableException {
            // Parse the input until a space character is encountered.
            parseUntil(sb, StopCharTesters.SPACE_STOP);

            // Intern the parsed string to reduce memory usage.
            @Nullable String str = WireInternal.INTERNER.intern(sb);

            // If the string represents a null value.
            if (("!!null").contentEquals(sb)) {
                text();
                return null;

            // If the string indicates a sequence map type.
            } else if (("!" + SEQ_MAP).contentEquals(sb)) {
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
         * Checks if the previous character is an end character, and if so, moves the read position back by one byte.
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
         * Checks for rewind condition. Currently, this just calls the checkRewind() method.
         * This might be a placeholder for additional functionality or for overriding in subclasses.
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
         * If the next character indicates the start of a type (i.e., it's a '!'),
         * this method reads and discards the type string.
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
         * Reads an object from the byte stream based on inferred data type.
         * This method dynamically determines the object's type based on specific
         * indicators or sequences in the stream and then invokes the appropriate
         * deserialization logic for that type.
         *
         * @param using An object to potentially reuse during deserialization for efficiency.
         * @param strategy The serialization strategy to be applied during deserialization.
         * @param type The expected type of the resulting object.
         * @return The deserialized object.
         * @throws InvalidMarshallableException If any issues are encountered during the deserialization process.
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
         * Attempts to read a number from the current stream context, dynamically determining
         * its potential type and format. If it's not a recognizable number, the method may
         * also consider the string as a date or time format.
         * <p>
         * The method supports various formats including long, double, {@link LocalTime},
         * {@link LocalDate}, and {@link ZonedDateTime}. If the string representation is not
         * recognizable as any of these formats, the original string is returned.
         *
         * @return The decoded number, date, time, or original string. Returns null if the
         *         string is either null or exceeds 40 characters in length.
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
         * Reads a sequence from the current stream context and attempts to interpret
         * it based on the provided class type. This method has specialized handling
         * for arrays and collections including {@link Object[]}, {@link String[]},
         * {@link List}, and {@link Set}.
         * <p>
         * If the class type isn't one of the recognized specialized types, an
         * {@link UnsupportedOperationException} will be thrown.
         *
         * @param clazz The expected type of the sequence to be read.
         * @return An array or collection representing the read sequence.
         * @throws UnsupportedOperationException if the provided class type isn't supported.
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
