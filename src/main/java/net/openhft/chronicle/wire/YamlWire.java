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
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * Represents a YAML-based wire format designed for efficient parsing and serialization of data.
 * The YamlWire class extends YamlWireOut and utilizes a custom tokenizer to convert YAML tokens into byte sequences.
 * It provides utility methods to read from and write to both byte buffers and files.
 */
@SuppressWarnings({"rawtypes", "unchecked", "this-escape"})
public class YamlWire extends YamlWireOut<YamlWire> {

    // YAML-specific tag constants for representing special constructs.
    static final String SEQ_MAP = "!seqmap";
    static final String BINARY_TAG = "!binary";
    static final String DATA_TAG = "!data";
    static final String NULL_TAG = "!null";

    //for (char ch : "?%&*@`0123456789+- ',#:{}[]|>!\\".toCharArray())
    // Internal helper for reading text-based values.
    private final TextValueIn valueIn = createValueIn();

    // Custom tokenizer for parsing YAML tokens.
    private final YamlTokeniser yt;

    // Map to store reusable content anchors defined in the YAML.
    private final Map<String, Object> anchorValues = new HashMap<>();

    // Provides default values for reading.
    private DefaultValueIn defaultValueIn;

    // Context for writing out YAML documents.
    private WriteDocumentContext writeContext;

    // Context for reading in YAML documents.
    private ReadDocumentContext readContext;

    // Instance for re-reading or re-parsing scenarios.
    private YamlWire rereadWire;

    /**
     * Constructor that initializes the YamlWire with provided bytes and a flag indicating the use of 8-bit.
     *
     * @param bytes Bytes from which YamlWire is initialized
     * @param use8bit A boolean flag to indicate the use of 8-bit
     */
    public YamlWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
        yt = new YamlTokeniser(bytes);
        defaultValueIn = new DefaultValueIn(this);
    }

    /**
     * Constructor that initializes the YamlWire with provided bytes.
     * Defaults to not using 8-bit.
     *
     * @param bytes Bytes from which YamlWire is initialized
     */
    public YamlWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    /**
     * Utility method to create a new YamlWire instance by reading bytes from a specified file.
     *
     * @param name Name of the file from which bytes are read
     * @return A new YamlWire instance initialized with bytes from the specified file
     * @throws IOException If there's an error in reading the file
     */
    @NotNull
    public static YamlWire fromFile(String name) throws IOException {
        return new YamlWire(BytesUtil.readFile(name), true);
    }

    /**
     * Utility method to create a new YamlWire instance from a given text string.
     *
     * @param text String from which the YamlWire instance is initialized
     * @return A new YamlWire instance initialized from the given string
     */
    @NotNull
    public static YamlWire from(@NotNull String text) {
        return new YamlWire(Bytes.from(text));
    }

    /**
     * Converts the content of a given {@link Wire} object into its string representation.
     *
     * @param wire The {@link Wire} object whose content needs to be converted to string.
     * @return The string representation of the wire's content.
     * @throws InvalidMarshallableException If the given wire's content cannot be marshalled.
     */
    public static String asText(@NotNull Wire wire) throws InvalidMarshallableException {
        long pos = wire.bytes().readPosition();
        @NotNull Wire tw = Wire.newYamlWireOnHeap();
        wire.copyTo(tw);
        wire.bytes().readPosition(pos);
        return tw.toString();
    }

    /**
     * Unescapes special characters in the provided Appendable based on the provided block quote character.
     * This method adheres to the YAML 1.2 specification for escaped characters
     * (see <a href="https://yaml.org/spec/1.2.2/#escaped-characters">YAML Spec 1.2.2</a>).
     *
     * @param sb The appendable containing characters to be unescaped.
     * @param blockQuote The block quote character that determines the escaping scheme (' or ").
     * @param <ACS> An appendable that also implements CharSequence interface.
     */
    private static <ACS extends Appendable & CharSequence> void unescape(@NotNull ACS sb, char blockQuote) {
        int end = 0;
        int length = sb.length();
        boolean skip = false;
        for (int i = 0; i < length; i++) {
            if (skip) {
                skip = false;
                continue;
            }

            char ch = sb.charAt(i);

            // Processing escaped characters for double quotes
            if (blockQuote == '\"' && ch == '\\' && i < length - 1) {
                char ch3 = sb.charAt(++i);
                switch (ch3) {
                    // Various cases for character unescaping based on YAML specification
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

                    // For Unicode escapes
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

            // Processing escaped characters for single quotes
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

    /**
     * Removes underscore ('_') characters from the given StringBuilder. This is useful for processing
     * YAML numbers which may use underscores as visual separators (e.g., 1_000_000 for one million).
     *
     * @param s The StringBuilder from which underscores should be removed.
     */
    static void removeUnderscore(@NotNull StringBuilder s) {
        int i = 0;
        for (int j = 0; j < s.length(); j++) {
            char ch = s.charAt(j);
            s.setCharAt(i, ch);
            if (ch != '_')
                i++;
        }
        s.setLength(i);
    }

    /**
     * Attempts to interpret the content of the given StringBuilder as a number (long, double) or
     * as a date/time, based on the YAML specification. If none of these interpretations is successful,
     * it returns the original string content.
     *
     * @param bq The block quote character (either ' or ") that initiated the string in YAML.
     * @param s The StringBuilder containing the string to be interpreted.
     * @return An Object which might be a Long, Double, Date, Time or the original String itself
     *         depending on successful interpretation.
     */
    @Nullable
    static Object readNumberOrTextFrom(char bq, final @Nullable StringBuilder s) {
        if (leaveUnparsed(bq, s))
            return s;

        StringBuilder sb = s;
        // YAML octal notation
        if (StringUtils.startsWith(s, "0o")) {
            sb = new StringBuilder(s);
            sb.deleteCharAt(1);
        }

        // Remove underscores if present, as they can be used in YAML as visual separators in numbers.
        if (s.indexOf("_") >= 0) {
            sb = new StringBuilder(s);
            removeUnderscore(sb);
        }

        String ss = sb.toString();

        // Attempt to parse as a long
        try {
            return Long.decode(ss);
        } catch (NumberFormatException fallback) {
            // Intentionally left blank to handle fallback
        }

        // Attempt to parse as a double
        try {
            return Double.parseDouble(ss);
        } catch (NumberFormatException fallback) {
            // Intentionally left blank to handle fallback
        }

        // Attempt to parse as a date or time
        try {
            return parseDateOrTime(s, ss);
        } catch (DateTimeParseException fallback) {
            // Intentionally left blank to handle fallback
        }

        // If none of the interpretations was successful, return the original string content
        return s;
    }

    /**
     * Determines if a given string representation should be left unparsed based on certain criteria.
     *
     * <p>For instance, it checks:
     * <ul>
     *     <li>If the string is null.</li>
     *     <li>If a block quote character is present.</li>
     *     <li>If the length of the string is out of a certain range.</li>
     *     <li>If the first character of the string is not a number, decimal point, or a sign.</li>
     * </ul>
     *
     * @param bq The block quote character (either ' or ") that initiated the string in YAML.
     * @param s The StringBuilder containing the string to check.
     * @return True if the string should be left unparsed, otherwise false.
     */
    private static boolean leaveUnparsed(char bq, @Nullable StringBuilder s) {
        return s == null
                || bq != 0
                || s.length() < 1
                || s.length() > 40
                || "0123456789.+-".indexOf(s.charAt(0)) < 0;
    }

    /**
     * Attempts to parse the provided string into a temporal accessor representing a date or time.
     * The method handles various formats for dates (LocalDate), times (LocalTime), and
     * date-times with timezone offsets (ZonedDateTime).
     * If none of these interpretations is successful, a DateTimeParseException is thrown.
     *
     * @param s The original StringBuilder containing the string to be parsed.
     * @param ss The string equivalent of the StringBuilder 's'.
     * @return A TemporalAccessor which could be a LocalTime, LocalDate or ZonedDateTime based on the format.
     * @throws DateTimeParseException If the string cannot be parsed into any known date or time format.
     */
    private static TemporalAccessor parseDateOrTime(StringBuilder s, String ss) {
        if (s.length() == 7 && s.charAt(1) == ':') {
            return LocalTime.parse('0' + ss);
        }
        if (s.length() == 8 && s.charAt(2) == ':') {
            return LocalTime.parse(s);
        }
        if (s.length() == 10) {
            return LocalDate.parse(s);
        }
        if (s.length() >= 22) {
            return ZonedDateTime.parse(s);
        }
        throw new DateTimeParseException("Unable to parse date or time", s, 0);
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

    @Override
    @NotNull
    public <T> T methodWriter(@NotNull Class<T> tClass, Class<?>... additional) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.YAML,
                () -> newTextMethodWriterInvocationHandler(tClass));
        for (Class<?> aClass : additional)
            builder.addInterface(aClass);
        useTextDocuments();
        builder.marshallableOut(this);
        return builder.build();
    }

    /**
     * Creates a new instance of TextMethodWriterInvocationHandler based on the provided interfaces.
     * If any of the provided interfaces have a {@link Comment} annotation, a comment is written to the wire.
     *
     * @param interfaces The array of interfaces to be used with the TextMethodWriterInvocationHandler.
     * @return A new instance of TextMethodWriterInvocationHandler.
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

    /**
     * Initializes the reading context. This method ensures that the binary document context is used if
     * none is already set and prepares the YamlTokeniser for reading from the current position.
     */
    protected void initReadContext() {
        if (readContext == null)
            useBinaryDocuments();
        readContext.start();
        yt.lineStart(bytes.readPosition());
    }

    /**
     * Configures the YamlWire to use binary format for document reading and writing.
     * This involves setting up contexts that handle the binary format of documents.
     *
     * @return The current instance of YamlWire.
     */
    @NotNull
    public YamlWire useBinaryDocuments() {
        readContext = new BinaryReadDocumentContext(this);
        writeContext = new BinaryWriteDocumentContext(this);
        return this;
    }

    /**
     * Configures the YamlWire to use text format for document reading and writing.
     * This involves setting up contexts that handle the text format of documents.
     *
     * @return The current instance of YamlWire.
     */
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

    /**
     * Creates a new instance of {@link TextValueIn}. This method can be overridden
     * by subclasses to provide a custom implementation of TextValueIn.
     *
     * @return A new instance of TextValueIn.
     */
    @NotNull
    protected TextValueIn createValueIn() {
        return new TextValueIn();
    }

    /**
     * Converts the current YamlWire instance into a string representation.
     * If the bytes to be read exceed 1MB, a truncated version of the bytes
     * (limited to the first 1MB) is returned, followed by "..".
     * Otherwise, it returns the full string representation of the bytes.
     *
     * @return The string representation of the YamlWire instance.
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
        if (wire.getClass() == TextWire.class || wire.getClass() == YamlWire.class) {
            final Bytes<?> bytes0 = bytes();
            wire.bytes().write(this.bytes, yt.blockStart(), bytes0.readLimit() - yt.blockStart);
            this.bytes.readPosition(this.bytes.readLimit());
        } else {
            try (DocumentContext dc = wire.writingDocument()) {
                while (!endOfDocument()) {
                    copyOne(dc.wire(), true);
                    yt.next();
                }
            }
        }
    }

    /**
     * Copies a single element from the current YamlWire instance to the provided wire.
     * This is a recursive method that handles different YAML elements like mappings,
     * sequences, and primitive values based on the current token from the YamlTokeniser (yt).
     *
     * @param wire   The target wire to copy to.
     * @param nested A flag indicating whether the current element is nested within another element.
     * @throws InvalidMarshallableException If there's a problem during the marshalling process.
     */
    private void copyOne(WireOut wire, boolean nested) throws InvalidMarshallableException {
        ValueOut wireValueOut = wire.getValueOut();
        switch (yt.current()) {
            case NONE:
                break;
            case COMMENT:
                wire.writeComment(yt.text());
                break;
            case TAG:
                if (yt.current() == YamlToken.TAG && yt.isText(NULL_TAG)) {
                    wireValueOut.nu11();
                    yt.next();
                    break;
                }
                ValueOut valueOut2 = wireValueOut.typePrefix(yt.text());
                yt.next();
                copyOne(wire, true);
                valueOut2.endTypePrefix();
                break;
            case DIRECTIVE:
                break;
            case DOCUMENT_END:
                break;
            case DIRECTIVES_END:
                yt.next();
                while (!endOfDocument()) {
                    copyOne(wire, false);
                    yt.next();
                }
                break;
            case MAPPING_KEY:
                copyMappingKey(wire, nested);
                break;
            case MAPPING_END:
                return;
            case MAPPING_START: {
                if (nested) {
                    yt.next();
                    wireValueOut.marshallable(w -> {
                        while (yt.current() == YamlToken.MAPPING_KEY) {
                            copyMappingKey(wire, true);
                            yt.next();
                        }
                    });
                }
                break;
            }
            case SEQUENCE_END:
                break;
            case SEQUENCE_ENTRY:
                break;
            case SEQUENCE_START: {
                yt.next();
                YamlWire yw = this;
                wireValueOut.sequence(w -> {
                    while (yt.current() != YamlToken.SEQUENCE_END) {
                        yw.copyOne(w.wireOut(), true);
                        yw.yt.next();
                    }
                });
                break;
            }
            case TEXT:
                Object o = valueIn.readNumberOrText();
                if (o instanceof Long)
                    wireValueOut.int64((long) o);
                else
                    wireValueOut.object(o);
                break;
            case LITERAL:
                wireValueOut.text(yt.text());
                break;
            case ANCHOR:
                break;
            case ALIAS:
                break;
            case RESERVED:
                break;
            case STREAM_END:
                break;
            case STREAM_START:
                break;
        }
    }

    /**
     * Determines if the current YAML structure has reached the end of its document.
     * It checks based on the current token from the YamlTokeniser.
     *
     * @return true if the current token represents the end of a document or the stream; false otherwise.
     */
    private boolean endOfDocument() {
        // Check if there's nothing to read
        if (isEmpty())
            return true;
        switch (yt.current()) {
            case STREAM_END:
            case STREAM_START:
            case DOCUMENT_END:
            case NONE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Copies a mapping key from the current YamlWire instance to the provided wire.
     * This method advances through the tokens to handle nested keys and ensures
     * the key is correctly written to the target wire.
     *
     * @param wire   The target wire to copy to.
     * @param nested A flag indicating whether the current element is nested within another element.
     * @throws InvalidMarshallableException If there's a problem during the marshalling process.
     */
    private void copyMappingKey(WireOut wire, boolean nested) throws InvalidMarshallableException {
        // Move to the next token to identify the key structure
        yt.next();

        // Skip consecutive MAPPING_KEY tokens, if any
        if (yt.current() == YamlToken.MAPPING_KEY)
            yt.next();

        // Check if the current token is of TEXT type representing the key
        if (yt.current() == YamlToken.TEXT) {
            // Differentiate between nested and non-nested keys for writing to the wire
            if (nested) {
                wire.write(yt.text());
            } else {
                wire.writeEvent(String.class, yt.text());
            }

            // Move to the next token after copying the key
            yt.next();
        } else {
            // Throw an exception if unable to determine the key structure
            throw new UnsupportedOperationException("Unable to copy key " + yt);
        }

        // Recursively handle the associated value for the key
        copyOne(wire, true);
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

    /**
     * Reads a field from the current YamlToken and appends its content to the provided StringBuilder.
     *
     * @param sb StringBuilder instance to which the field content will be appended.
     * @return The same StringBuilder instance with the appended content.
     */
    @NotNull
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        startEventIfTop();

        // If the current token indicates a key in a map
        if (yt.current() == YamlToken.MAPPING_KEY) {
            yt.next();

            // Ensure the key is textual
            if (yt.current() == YamlToken.TEXT) {
                String text = yt.text(); // Captures the key's text. Note: using sb here may modify its contents
                sb.setLength(0);         // Reset the StringBuilder
                sb.append(text);         // Append the key's text to the StringBuilder
                unescape(sb, yt.blockQuote()); // Handle any escape sequences within the key
                yt.next();
            } else {
                throw new IllegalStateException(yt.toString());
            }
        } else {
            sb.setLength(0); // Clear the StringBuilder if the current token isn't a MAPPING_KEY
        }
        return sb;
    }

    @SuppressWarnings("fallthrough")
    @Nullable
    @Override
    public <K> K readEvent(@NotNull Class<K> expectedClass) throws InvalidMarshallableException {
        startEventIfTop();
        switch (yt.current()) {
            case MAPPING_START:
                yt.next();
                assert yt.current() == YamlToken.MAPPING_KEY;
                // Deliberate fall-through
            case MAPPING_KEY:
                YamlToken next = yt.next();
                if (next == YamlToken.MAPPING_KEY) {
                    return readEvent(expectedClass);
                }

                K object = valueIn.object(expectedClass);
                if (object instanceof StringBuilder)
                    return (K) object.toString();
                return object;
            case NONE:
                return null;
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

    @Override
    @SuppressWarnings("fallthrough")
    public void consumePadding() {
        while (true) {
            switch (yt.current()) {
                case COMMENT:
                    String text = yt.text();
                    commentListener.accept(text);
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
            long[] offsets = keys.offsets();
            if (rereadWire == null) {
                initRereadWire();
            }
            int indent = yt.topContext().indent;
            for (int i = 0; i < count; i++) {
                rereadWire.yt.topContext().indent = indent;
                long end = bytes.readPosition();
                long offset = offsets[i];
                rereadWire.bytes.readPositionRemaining(offset, end - offset);
                rereadWire.yt.rereadFrom(offset);
                YamlToken next = rereadWire.yt.next();
                if (next == YamlToken.MAPPING_START) // indented rather than iin { }
                    next = rereadWire.yt.next();
                assert next == YamlToken.MAPPING_KEY : "next: " + next;
                if (rereadWire.checkForMatch(keyName)) {
                    keys.removeIndex(i);
                    return rereadWire.valueIn;
                }
            }
            // Next lines not covered by any tests
        }
        YamlTokeniser.YTContext yc = yt.topContext();
        int minIndent = yc.indent;
        // go through remaining keys
        while (yt.current() == YamlToken.MAPPING_KEY) {
            long lastKeyPosition = yt.lineStart;
            if (checkForMatch(keyName))
                return valueIn;

            if (!StringUtils.startsWith(sb, "-"))
                keys.push(lastKeyPosition);
            // Avoid consuming '}' but consume to next mapping key
            valueIn.consumeAny(minIndent >= 0 ? minIndent : Integer.MAX_VALUE);
        }

        return defaultValueIn;
    }

    /**
     * Initializes 'rereadWire', skipping any preliminary tokens to get to the main content.
     */
    private void initRereadWire() {
        rereadWire = new YamlWire(bytes.bytesStore().bytesForRead());
        YamlToken yamlToken;
        // Keep advancing until we pass headers/comments and reach the main content
        do {
            yamlToken = rereadWire.yt.next();
        } while (yamlToken == YamlToken.STREAM_START
                || yamlToken == YamlToken.DIRECTIVES_END
                || yamlToken == YamlToken.COMMENT
                || yamlToken == YamlToken.MAPPING_START);
    }

    /**
     * Produces a dump of the current parsing context. Useful for debugging.
     *
     * @return A string representation of the current parsing context.
     */
    public String dumpContext() {
        ValidatableUtil.startValidateDisabled();
        try {
            Wire yw = Wire.newYamlWireOnHeap();
            yw.getValueOut().list(yt.contexts, YamlTokeniser.YTContext.class);
            return yw.toString();
        } finally {
            ValidatableUtil.endValidateDisabled(); // Ensure the validation check is restored afterward
        }
    }

    /**
     * Checks if the next token's text matches the given key name after handling any escape sequences.
     *
     * @param keyName The expected key name.
     * @return true if the next token's text matches the given key name; false otherwise.
     */
    private boolean checkForMatch(@NotNull String keyName) {
        YamlToken next = yt.next();

        // If the next token is textual
        if (next == YamlToken.TEXT) {
            sb.setLength(0);          // Reset the StringBuilder
            sb.append(yt.text());     // Append the text of the next token to the StringBuilder
            unescape(sb, yt.blockQuote()); // Handle any escape sequences within the text
            yt.next();
        } else {
            throw new IllegalStateException(next.toString());
        }

        // Compare the processed string in the StringBuilder with the expected keyName
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
        s.setLength(0);
        if (yt.current() == YamlToken.COMMENT) {
            s.append(yt.text());
            yt.next();
        }
        return this;
    }

    @Override
    public void clear() {
        reset();
    }

    /**
     * Consumes and skips the start of a YAML document, e.g., '---'.
     * For specific keywords (like "!!data" and "!!meta-data"), the cursor position remains unchanged.
     */
    protected void consumeDocumentStart() {
        // Check if there are more than 4 bytes left to read
        if (bytes.readRemaining() > 4) {
            long pos = bytes.readPosition();

            // Check if the next three characters are '---'
            if (bytes.readByte(pos) == '-' && bytes.readByte(pos + 1) == '-' && bytes.readByte(pos + 2) == '-')
                bytes.readSkip(3); // Skip '---'

            pos = bytes.readPosition();
            // Read a word until a space is encountered
            @NotNull String word = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
            switch (word) {
                // If the word matches any of the special cases, do nothing
                case "!!data":
                case "!!data-not-ready":
                case "!!meta-data":
                case "!!meta-data-not-ready":
                    break;
                default:
                    bytes.readPosition(pos); // Reset the read position for other cases
            }
        }

        // Move to the next token if the current one is NONE
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

    /**
     * Reads a YAML map and deserializes it into a Java Map, converting values to the specified type.
     *
     * @param valueType The class type to which map values should be converted.
     * @return A Java Map representing the YAML map.
     */
    @NotNull
    private Map readMap(Class<?> valueType) {
        Map map = new LinkedHashMap();
        if (yt.current() == YamlToken.MAPPING_START) {
            while (yt.next() == YamlToken.MAPPING_KEY) {
                if (yt.next() == YamlToken.TEXT) {
                    String key = yt.text();
                    Object o;
                    if (yt.next() == YamlToken.TEXT) {
                        // Convert the text to the specified type
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

    @Override
    public void startEvent() {
        consumePadding();
        switch (yt.current()) {
            case MAPPING_START:
                yt.next(Integer.MAX_VALUE);
                return;
            case NONE:
                return;
        }
        throw new UnsupportedOperationException(yt.toString());
    }

    /**
     * Starts a YAML event if at the top level. Consumes any padding.
     * If the context size is 3 and the current token is MAPPING_START, moves to the next token.
     */
    void startEventIfTop() {
        consumePadding();
        if (yt.contextSize() == 3)
            if (yt.current() == YamlToken.MAPPING_START)
                yt.next();
    }

    @Override
    public boolean isEndEvent() {
        consumePadding();
        YamlToken current = yt.current();
        return current == YamlToken.MAPPING_END
                || current == YamlToken.NONE;
    }

    @Override
    public void endEvent() {
        YamlTokeniser.YTContext context = yt.topContext();
        int minIndent = context.indent;

        switch (yt.current()) {
            case MAPPING_END:
            case DOCUMENT_END:
            case NONE:
                break;
            default:
                do {
                    valueIn.consumeAny(minIndent);
                } while (yt.current() == YamlToken.COMMENT);
                break;
        }
        if (yt.current() == YamlToken.NONE) {
            yt.next(Integer.MIN_VALUE);
        } else {
            while (yt.current() == YamlToken.MAPPING_KEY) {
                valueIn.consumeAny(minIndent);
            }
        }
        if (yt.current() == YamlToken.MAPPING_END ||
                yt.current() == YamlToken.DOCUMENT_END ||
                yt.current() == YamlToken.NONE) {
            yt.next(Integer.MIN_VALUE);
            return;
        }
        throw new UnsupportedOperationException(yt.toString());
    }

    /**
     * Resets the state of the YamlWire instance, clearing all buffers and contexts.
     */
    public void reset() {
        // Reset reading and writing contexts if they exist
        if (readContext != null)
            readContext.reset();
        if (writeContext != null)
            writeContext.reset();

        // Clear the bytes buffer and internal StringBuilder
        bytes.clear();
        sb.setLength(0);

        // Reset the YAML tokenizer and value states
        yt.reset();
        valueIn.resetState();
        valueOut.resetState();

        // Clear the anchor values map
        anchorValues.clear();
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

    @Override
    public boolean readDocument(@Nullable ReadMarshallable metaDataConsumer, @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        valueIn.resetState();
        return super.readDocument(metaDataConsumer, dataConsumer);
    }

    @Override
    public boolean readDocument(long position, @Nullable ReadMarshallable metaDataConsumer, @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        valueIn.resetState();
        return super.readDocument(position, metaDataConsumer, dataConsumer);
    }

    /**
     * Implementation of the ValueIn interface for reading text-based values from YamlWire.
     */
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
            yt.next();
            return cs == null ? null : WireInternal.INTERNER.intern(cs);
        }

        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder sb) {
            sb.setLength(0);
            @Nullable CharSequence cs = textTo0(sb);
            yt.next();
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
                bytes.append(yt.text());
                yt.next();
            } else if (yt.current() == YamlToken.TAG) {
                if (yt.isText(NULL_TAG)) {
                    yt.next();
                    yt.next();
                    return null;
                } else if (yt.isText(BINARY_TAG)) {
                    yt.next();
                    bytes.write((byte[]) decodeBinary(byte[].class));
                } else {
                    throw new UnsupportedOperationException(yt.toString());
                }
            } else {
                throw new UnsupportedOperationException(yt.toString());
            }
            return bytes;
        }

        @Override
        public <E> E object(@Nullable E using, @Nullable Class<? extends E> clazz, boolean bestEffort) throws InvalidMarshallableException {
            YamlToken current = yt.current();
            if (current == YamlToken.ALIAS) {
                String alias = yt.text();
                Object o = anchorValues.get(alias);
                yt.next();
                if (o == null)
                    throw new IllegalStateException("Unknown alias " + alias + " with no corresponding anchor");
                return (E) o;
            } else if (current == YamlToken.ANCHOR) {
                String alias = yt.text();
                yt.next();
                Object o = Wires.object0(this, using, clazz, bestEffort);
                // Overwriting of anchor values is permitted
                anchorValues.put(alias, o);
                return (E) o;
            }

            return Wires.object0(this, using, clazz, bestEffort);
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

        /**
         * Extracts the text from the current token and appends it to a StringBuilder.
         * Handles various YAML tokens like TEXT, LITERAL, and TAG.
         * @return StringBuilder containing the text.
         */
        @Nullable
        StringBuilder textTo0(@NotNull StringBuilder a) {
            consumePadding(); // consume any padding

            // if the current token is a sequence entry, move to the next token
            if (yt.current() == YamlToken.SEQUENCE_ENTRY)
                yt.next();

            switch (yt.current()) {
                // handle text or literal tokens
                case TEXT:
                case LITERAL:
                    a.append(yt.text()); // append the text value

                    // unescape the text value if needed
                    if (yt.current() == YamlToken.TEXT)
                        unescape(a, yt.blockQuote());

                    break;

                case ANCHOR:
                    // Handle YAML anchors, which can be referred to later as aliases
                    String alias = yt.text();
                    yt.next();
                    textTo0(sb);
                    // Store the anchor for later reference
                    anchorValues.put(alias, sb.toString());
                    break;

                case ALIAS:
                    // Retrieve the actual object that an alias refers to
                    alias = yt.text();
                    Object o = anchorValues.get(alias);
                    if (o == null)
                        throw new IllegalStateException("Unknown alias " + alias + " with no corresponding anchor");

                    yt.next();
                    sb.append(o);
                    break;

                // handle tag tokens
                case TAG:
                    // check for a NULL tag and move to the next token
                    if (yt.isText(NULL_TAG)) {
                        yt.next();

                        return null;
                    }

                    // check for a BINARY tag, decode and append its value
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
        public WireIn bytesMatch(@NotNull BytesStore<?, ?> compareBytes, BooleanConsumer consumer) {
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
                    try {
                        bytesConsumer.readMarshallable(bytes);
                    } finally {
                        bytes.releaseLast();
                    }

                } else if (StringUtils.isEqual(sb, NULL_TAG)) {
                    bytesConsumer.readMarshallable(null);
                    yt.next();

                } else {
                    throw new IORuntimeException("Unsupported type=" + sb);
                }
            } else {
                textTo(sb);
                Bytes<byte[]> bytes = Bytes.wrapForRead(sb.toString().getBytes(ISO_8859_1));
                try {
                    bytesConsumer.readMarshallable(bytes);
                } finally {
                    bytes.releaseLast();
                }
            }
            return YamlWire.this;
        }

        @Override
        public byte @Nullable [] bytes(byte[] using) {
            return (byte[]) objectWithInferredType(using, SerializationStrategies.ANY_OBJECT, byte[].class);
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

        /**
         * Reads the length of a marshallable.
         * @return The length of the marshallable.
         */
        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            try {
                consumeAny(yt.topContext().indent);
                return bytes.readPosition() - start;
            } finally {
                bytes.readPosition(start);
            }
        }

        /**
         * Consumes any token type based on its indentation level.
         * @param minIndent Minimum indentation level to consume.
         */
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

        /**
         * Consumes a sequence from the YAML token stream.
         *
         * @param minIndent The minimum indentation level to consider.
         */
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

        /**
         * Consumes a map from the YAML token stream.
         *
         * @param minIndent The minimum indentation level to consider.
         */
        private void consumeMap(int minIndent) {
            yt.next(minIndent); // Move to the next token in the map.

            // While the current token signifies a key in the map:
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

        /**
         * Extracts a long value from the current YAML token.
         *
         * @return The parsed long value.
         * @throws UnsupportedOperationException If the current token is not of type TEXT.
         */
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

        /**
         * Extracts a double value from the current YAML token.
         *
         * @return The parsed double value.
         * @throws UnsupportedOperationException If the current token is not of type TEXT.
         */
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

        /**
         * Reads a sequence of items from the YAML token stream and populates the provided list.
         *
         * @param list The list to populate with the sequence items.
         * @param buffer Temporary storage used during sequence processing.
         * @param bufferAdd Supplier function to add items to the buffer.
         * @param reader0 Reader to process the tokens.
         * @return true if the sequence was successfully read, false otherwise.
         * @throws InvalidMarshallableException If there's a problem with marshalling.
         */
        public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) throws InvalidMarshallableException {
            // Delegate to the other `sequence` method without the reader.
            return sequence(list, buffer, bufferAdd);
        }

        @Override
        public <T> boolean sequence(@NotNull List<T> list, @NotNull List<T> buffer, @NotNull Supplier<T> bufferAdd) throws InvalidMarshallableException {
            consumePadding();
            if (isNull()) {
                return false;
            }
            list.clear();
            if (yt.current() == YamlToken.SEQUENCE_START) {
                int minIndent = yt.secondTopContext().indent;
                yt.next(Integer.MAX_VALUE);

                while (hasNextSequenceItem()) {
                    if (buffer.size() <= list.size())
                        buffer.add(bufferAdd.get());
                    Object using = buffer.get(list.size());
                    list.add((T) valueIn.object(using, using.getClass()));
                }

                if (yt.current() == YamlToken.NONE)
                    yt.next(minIndent);
                if (yt.current() == YamlToken.SEQUENCE_END)
                    yt.next(minIndent);

            } else {
                throw new UnsupportedOperationException(yt.toString());
            }
            return true;
        }

        @NotNull
        @Override
        public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) throws InvalidMarshallableException {
            consumePadding();
            assert yt.current() == YamlToken.SEQUENCE_START;
            yt.next(Integer.MIN_VALUE);
            while (true) {
                switch (yt.current()) {
                    case TEXT:
                    case SEQUENCE_ENTRY:
                        tReader.accept(t, kls, YamlWire.this.valueIn);
                        continue;

                    case SEQUENCE_END:
                        yt.next(Integer.MIN_VALUE);
                        return YamlWire.this;

                    default:
                        throw new IllegalStateException(yt.toString());
                }
            }
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
            consumePadding();
            switch (yt.current()) {
                // Perhaps should be negative selection instead of positive
                case SEQUENCE_START:
                case SEQUENCE_ENTRY:
                    // Allows scalar value to be converted into singleton array
                case TEXT:
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
        public Class<?> typePrefix() {
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
        public Object typePrefixOrObject(Class<?> tClass) {
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
        @Override
        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer)
                throws IORuntimeException, BufferUnderflowException {
            if (yt.current() != YamlToken.TAG)
                throw new UnsupportedOperationException(yt.toString());

            if (!yt.isText("type"))
                throw new UnsupportedOperationException(yt.text());

            if (yt.next() != YamlToken.TEXT)
                throw new UnsupportedOperationException(yt.toString());

            StringBuilder stringBuilder = acquireStringBuilder();
            textTo(stringBuilder);
            classNameConsumer.accept(t, stringBuilder);

            return YamlWire.this;
        }

        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            consumePadding();
            if (yt.current() == YamlToken.TAG) {
                if (yt.text().equals("type")) {
                    if (yt.next() == YamlToken.TEXT) {
                        String text = yt.text();
                        yt.next();
                        try {
                            return classLookup().forName(text);
                        } catch (ClassNotFoundRuntimeException e) {
                            return unresolvedHandler.apply(text, e.getCause());
                        }
                    }
                }
            }
            throw new UnsupportedOperationException(yt.toString());
        }

        @Nullable
        @Override
        public Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy)
                throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException {
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
                    Class<?> clazz = typePrefix();
                    if (clazz != object.getClass())
                        object = ObjectUtils.newInstance(clazz);
                    return marshallable(object, strategy);

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

                case TEXT:
                    return ObjectUtils.convertTo(object.getClass(), text());

                default:
                    break;
            }
            throw new UnsupportedOperationException(yt.toString());
        }

        /**
         * Creates an instance of the provided class and attempts to populate it from the YAML stream.
         *
         * @param clazz The class type to instantiate and populate.
         * @return The populated instance of the class.
         * @throws IORuntimeException If there's an error in the YAML format or during demarshalling.
         */
        @NotNull
        public Demarshallable demarshallable(@NotNull Class<?> clazz) {
            consumePadding();
            switch (yt.current()) {
                case TAG:
                    yt.next();
                    break;
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
                // Adjust read limits to ensure that we don't read past the current object in the stream.
                bytes.readLimit(newLimit);
                bytes.readSkip(1);  // skip the opening curly brace '{'
                consumePadding();

                object = Demarshallable.newInstance((Class<? extends Demarshallable>) clazz, YamlWire.this);
            } finally {
                // Restore original read limits after processing.
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
        public <T> T typedMarshallable() throws InvalidMarshallableException {
            return (T) objectWithInferredType(null, SerializationStrategies.ANY_NESTED, null);
        }

        /**
         * Reads a YAML map and populates a Java map with the given key and value types.
         *
         * @param kClass The class type of the keys.
         * @param vClass The class type of the values.
         * @param usingMap An optional pre-existing map to populate.
         * @return The populated map.
         * @throws InvalidMarshallableException If there's a problem with marshalling.
         * @throws IORuntimeException If there's an error in the YAML format or during parsing.
         */
        @Nullable
        private <K, V> Map<K, V> map(@NotNull final Class<K> kClass,
                                     @NotNull final Class<V> vClass,
                                     @Nullable Map<K, V> usingMap) throws InvalidMarshallableException {
            consumePadding();

            // If a pre-existing map isn't provided, initialize one. Otherwise, clear it.
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

        /**
         * Parses a typed map based on specific YAML tags.
         *
         * @param kClazz The class type for map keys.
         * @param vClass The class type for map values.
         * @param usingMap The map to populate based on the YAML data.
         * @param sb A StringBuilder instance used for temporary string operations.
         * @return Populated map from the YAML data or null.
         * @throws InvalidMarshallableException If there's a problem with marshalling.
         * @throws IORuntimeException If an unexpected YAML structure or tag is encountered.
         */
        @Nullable
        private <K, V> Map<K, V> typedMap(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap, @NotNull StringBuilder sb) throws InvalidMarshallableException {
            // Read the next YAML token into the provided StringBuilder.
            yt.text(sb);
            yt.next();
            if (NULL_TAG.contentEquals(sb)) {
                text();
                return null; // Return null to indicate absence of a value.

            // If the current token indicates a sequence map...
            } else if (SEQ_MAP.contentEquals(sb)) {
                consumePadding();

                // Verify that the next token is the start of a sequence.
                if (yt.current() != YamlToken.SEQUENCE_START)
                    throw new IORuntimeException("Unsupported start of sequence : " + yt.current());

                // Read and populate the provided map with key-value pairs from the YAML sequence.
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
                // If the token isn't recognized, throw an exception.
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
            // Fix an issue in MethodReaderWithHistoryTest
            if (yt.current() == YamlToken.SEQUENCE_ENTRY)
                yt.next();
            valueIn.skipType();
            if (yt.current() != YamlToken.TEXT) {
                Jvm.warn().on(getClass(), "Unable to read " + valueIn.objectBestEffort() + " as a long.");
                return 0;
            }

            return getALong();
        }

        @Override
        public double float64() {
            consumePadding();
            // Fix an issue in MethodReaderWithHistoryTest
            if (yt.current() == YamlToken.SEQUENCE_ENTRY)
                yt.next();
            valueIn.skipType();
            if (yt.current() != YamlToken.TEXT) {
                Jvm.warn().on(getClass(), "Unable to read " + valueIn.objectBestEffort() + " as a double.");
                return 0;
            }
            return getADouble();
        }

        /**
         * Skips over a YAML type declaration in the current stream.
         * If the current token indicates a YAML type (i.e., a TAG), the reading position is adjusted to skip over it.
         */
        void skipType() {
            consumePadding();
            if (yt.current() == YamlToken.TAG) {
                yt.next();
                consumePadding();
            }
        }

        @Override
        public float float32() {

            return (float) float64();
        }

        /**
         * @return true if !!null "", if {@code true} reads the !!null "" up to the next STOP, if
         * {@code false} no  data is read  ( data is only peaked if {@code false} )
         */
        @Override
        public boolean isNull() {
            consumePadding();

            if (yt.current() == YamlToken.TAG && yt.isText(NULL_TAG)) {
                consumeAny(0);
                return true;
            }

            return false;
        }

        @Override
        public Object objectWithInferredType(Object using, @NotNull SerializationStrategy strategy, Class<?> type) throws InvalidMarshallableException {
            consumePadding();
            if (yt.current() == YamlToken.SEQUENCE_ENTRY)
                yt.next();
            @Nullable Object o = objectWithInferredType0(using, strategy, type);
            consumePadding();
            return o;
        }

        /**
         * Reads an object from the YAML stream, inferring its type based on provided context and current token.
         * Depending on the encountered token, various internal methods are invoked to parse the object correctly.
         * If a YAML ANCHOR is encountered, it's mapped to the parsed object for potential future ALIAS references.
         *
         * @param using The object to potentially reuse when reading. Might be null.
         * @param strategy The serialization strategy to employ while reading the object.
         * @param type Expected type of the object to be read. If null, the method will attempt to infer the type.
         * @return The read object, possibly of the expected type. Might be null if the YAML token is NONE.
         * @throws InvalidMarshallableException if any error occurs while parsing or constructing the object.
         */
        @SuppressWarnings("fallthrough")
        @Nullable
        Object objectWithInferredType0(Object using, @NotNull SerializationStrategy strategy, Class<?> type) throws InvalidMarshallableException {
            boolean bestEffort = type != null;

            // Handle type declaration, if present
            if (yt.current() == YamlToken.TAG) {
                Class<?> aClass = typePrefix();
                if (type == null || type == Object.class || type.isInterface())
                    type = aClass;
            }

            switch (yt.current()) {
                case MAPPING_START:
                    // Parse YAML mapping, considering the expected type
                    if (type != null) {
                        // Create new TreeMap if a sorted map is expected
                        if (type == SortedMap.class && !(using instanceof SortedMap))
                            using = new TreeMap();
                        // Handle map-specific types
                        if (type == Object.class || Map.class.isAssignableFrom(type) || using instanceof Map)
                            return map(Object.class, Object.class, (Map) using);
                    }
                    return valueIn.object(using, type, bestEffort);

                case SEQUENCE_START:
                    // Read a YAML sequence
                    return readSequence(type);

                case TEXT:
                case LITERAL:
                    // Parse text or numeric value
                    Object o = valueIn.readNumberOrText();
                    yt.next();
                    if (o instanceof StringBuilder)
                        o = o.toString();
                    if (type == Class.class)
                        return classLookup.forName(o.toString());
                    return ObjectUtils.convertTo(type, o);

                case ANCHOR:
                    // Handle YAML anchors, which can be referred to later as aliases
                    String alias = yt.text();
                    yt.next();
                    o = valueIn.object(using, type);
                    // Store the anchor for later reference
                    anchorValues.put(alias, o);
                    return o;

                case ALIAS:
                    // Retrieve the actual object that an alias refers to
                    alias = yt.text();
                    o = anchorValues.get(alias);
                    if (o == null)
                        throw new IllegalStateException("Unknown alias " + alias + " with no corresponding anchor");

                    yt.next();
                    return o;

                case NONE:
                    // Handle cases with no data
                    return null;

                case TAG:
                    // Handle specific YAML tags
                    final StringBuilder stringBuilder = acquireStringBuilder();
                    yt.text(stringBuilder);

                    // Specific handling for binary data
                    if (BINARY_TAG.contentEquals(stringBuilder)) {
                        yt.next();
                        return decodeBinary(type);
                    }
                    // Intentional fall-through for other tags

                default:
                    throw new UnsupportedOperationException("Cannot determine what to do with " + yt.current());
            }
        }

        /**
         * Attempts to read either a number or a textual value from the YAML stream.
         * If the current token is a LITERAL, a StringBuilder containing the text will be returned.
         * Otherwise, the method tries to interpret the content as a number or textual data.
         *
         * @return An Object which might be a StringBuilder (for text) or a numeric representation.
         */
        @Nullable
        protected Object readNumberOrText() {
            // Determine the kind of quote used for the YAML block
            char bq = yt.blockQuote();
            // Extract the text content into a StringBuilder
            @Nullable StringBuilder s = textTo0(acquireStringBuilder());
            if (yt.current() == YamlToken.LITERAL)
                return s;
            if (StringUtils.isEqual(s, "true"))
                return true;
            if (StringUtils.isEqual(s, "false"))
                return false;
            return readNumberOrTextFrom(bq, s);
        }

        /**
         * Reads a sequence (i.e., a list or array) from the YAML stream and interprets it
         * according to the provided class type. Depending on the class type, the method creates
         * the appropriate collection (TreeSet, LinkedHashSet, ArrayList, etc.).
         *
         * @param clazz The class type that determines how the sequence should be interpreted.
         * @return An object representing the read sequence, which might be a collection or an array.
         */
        @NotNull
        private Object readSequence(Class<?> clazz) {
            @NotNull Collection coll =
                    clazz == SortedSet.class ? new TreeSet<>() :
                            clazz == Set.class ? new LinkedHashSet<>() :
                                    new ArrayList<>();
            @Nullable Class<?> componentType = (clazz != null && clazz.isArray() && clazz.getComponentType().isPrimitive())
                    ? clazz.getComponentType() : null;

            // Read the YAML sequence into the determined collection type
            readCollection(componentType, coll);
            // If the expected type is an array, convert the collection into an array
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

        /**
         * Populates a provided collection with items from the current YAML sequence.
         * This method makes use of the 'sequence' method to iterate through the sequence
         * and extract each item, casting them to the specified class if provided.
         *
         * @param clazz The class type to which each item should be casted, can be null.
         * @param list The target collection to be populated.
         */
        private void readCollection(@Nullable Class<?> clazz, @NotNull Collection list) {
            sequence(list, (l, v) -> {
                while (v.hasNextSequenceItem()) {
                    l.add(v.object(clazz));
                }
            });
        }

        /**
         * Decodes a Base64 encoded binary string from the YAML stream.
         * This method first reads the object from the YAML as a String,
         * then decodes the Base64 representation, and lastly tries to
         * convert the decoded byte array into the desired type.
         *
         * @param type The expected type of the resulting object after decoding.
         * @return The decoded object, potentially wrapped or converted according to the desired type.
         * @throws InvalidMarshallableException If there's an error during the deserialization.
         */
        private Object decodeBinary(Class<?> type) throws InvalidMarshallableException {
            Object o = objectWithInferredType(null, SerializationStrategies.ANY_SCALAR, String.class);
            byte[] decoded = Base64.getDecoder().decode(o == null ? "" : o.toString().replaceAll("\\s", ""));

            // Check if the desired type matches a BytesStore or is left unspecified
            if (type == null || BytesStore.class.isAssignableFrom(type))
                return BytesStore.wrap(decoded);

            // Check if the desired type is a byte array
            if (type.isArray() && type.getComponentType().equals(Byte.TYPE))
                return decoded;

            // Attempt to convert the byte array into other supported types, such as BitSet
            try {
                Method valueOf = type.getDeclaredMethod("valueOf", byte[].class);
                Jvm.setAccessible(valueOf);
                return valueOf.invoke(null, decoded);
            } catch (NoSuchMethodException e) {
                // ignored - method not found for conversion
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }

            // If all conversion attempts failed, throw an exception
            throw new UnsupportedOperationException("Cannot determine how to deserialize " + type + " from binary data");
        }

        @Override
        public String toString() {
            return YamlWire.this.toString();
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
