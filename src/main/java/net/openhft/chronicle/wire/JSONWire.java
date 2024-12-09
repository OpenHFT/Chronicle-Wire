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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.threads.ThreadLocalHelper;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.core.util.UnresolvedType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

/**
 * Represents the JSON wire format.
 * <p>
 * This class provides functionality for managing JSON data in a wire format.
 * It currently provides a subset of functionalities similar to the YAML wire format.
 * The core capability of this class is to handle JSON data structures as {@code Bytes}
 * objects, allowing for efficient manipulation and parsing.
 */
@SuppressWarnings("this-escape")
public class JSONWire extends TextWire {

    // The rest of null
    private static final @NotNull Bytes<byte[]> _ULL = Bytes.from("ull");
    @Deprecated(/* to be removed in x.28 */)
    public static final @NotNull Bytes<byte[]> ULL = _ULL;
    // the rest of true
    private static final @NotNull Bytes<byte[]> _RUE = Bytes.from("rue");
    // the rest of false
    private static final @NotNull Bytes<byte[]> _ALSE = Bytes.from("alse");

    // Bytes for comma, commonly used as JSON separator.
    @SuppressWarnings("rawtypes")
    static final BytesStore<?, ?> COMMA = BytesStore.from(",");

    // A thread-local variable to store a reference to the stop characters tester for JSON parsing.
    static final ThreadLocal<WeakReference<StopCharsTester>> STRICT_ESCAPED_END_OF_TEXT_JSON = new ThreadLocal<>();

    // Supplier for stop character tester for strict JSON text that escapes specific characters.
    static final Supplier<StopCharsTester> STRICT_END_OF_TEXT_JSON_ESCAPING = TextStopCharsTesters.STRICT_END_OF_TEXT_JSON::escaping;

    // Flag to determine whether to use types or not during parsing.
    boolean useTypes;
    private JSONValueOutFromStart valueOutFromStart;

    /**
     * Default constructor, initializes with elastic bytes allocated on heap.
     */
    @SuppressWarnings("rawtypes")
    public JSONWire() {
        this(Bytes.allocateElasticOnHeap());
    }

    /**
     * Constructs a JSONWire with the given bytes and a flag for using 8-bit.
     *
     * @param bytes   The bytes to be used for initializing.
     * @param use8bit Flag indicating whether to use 8-bit representation.
     */
    public JSONWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
        trimFirstCurly(false);
    }

    /**
     * Constructs a JSONWire with the given bytes.
     *
     * @param bytes The bytes to be used for initializing.
     */
    @SuppressWarnings("rawtypes")
    public JSONWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    /**
     * Static method to construct a JSONWire from a string representation of JSON.
     *
     * @param text The string containing JSON data.
     * @return A new instance of JSONWire.
     */
    @NotNull
    public static JSONWire from(@NotNull String text) {
        return new JSONWire(Bytes.from(text));
    }

    /**
     * Converts the content of the provided wire to a JSON string.
     *
     * @param wire The wire instance to be converted.
     * @return The string representation of the JSON content.
     * @throws InvalidMarshallableException If there's an error during conversion.
     */
    public static String asText(@NotNull Wire wire) throws InvalidMarshallableException {
        long pos = wire.bytes().readPosition();
        @NotNull JSONWire tw = new JSONWire(nativeBytes());
        wire.copyTo(tw);
        wire.bytes().readPosition(pos);

        return tw.toString();
    }

    /**
     * Determines if a given class is a wrapper type in Java.
     * <p>
     * This is useful for handling certain JSON conversion scenarios where
     * native types have wrapper counterparts, such as int and Integer.
     *
     * @param type The class to be checked.
     * @return {@code true} if the class is a Java wrapper type, otherwise {@code false}.
     */
    static boolean isWrapper(Class<?> type) {
        return type == Integer.class || type == Long.class || type == Float.class ||
                type == Double.class || type == Short.class || type == Character.class ||
                type == Byte.class || type == Boolean.class || type == Void.class;
    }

    @Override
    protected Class<?> defaultKeyClass() {
        return String.class;
    }

    /**
     * Sets the flag to determine whether to use types during the JSON parsing or not.
     * <p>
     * This method is designed to follow the builder pattern, allowing it to be chained with other method calls on the {@code JSONWire} object.
     *
     * @param outputTypes A boolean value indicating whether to use types.
     * @return The current instance of the {@code JSONWire} class.
     */
    public JSONWire useTypes(boolean outputTypes) {
        this.useTypes = outputTypes;
        return this;
    }

    /**
     * Gets the current setting for the use of types during JSON parsing.
     *
     * @return {@code true} if types are being used in the current instance, otherwise {@code false}.
     */
    public boolean useTypes() {
        return useTypes;
    }

    @Override
    public @NotNull TextWire useTextDocuments() {
        readContext = new JSONReadDocumentContext(this);
        writeContext = trimFirstCurly()
                ? new TextWriteDocumentContext(this)
                : new JSONWriteDocumentContext(this);
        return this;
    }

    @NotNull
    @Override
    protected JSONValueOut createValueOut() {
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

                if (isNull())
                    return Double.NaN;

                return super.float64();
            }

            @Override
            public void checkRewind() {
                int ch = peekBack();
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
    public void copyTo(@NotNull WireOut wire) throws InvalidMarshallableException {
        if (wire.getClass() == getClass()) {
            final Bytes<?> bytes0 = bytes();
            final long length = bytes0.readRemaining();
            wire.bytes().write(this.bytes, bytes0.readPosition(), length);
            this.bytes.readSkip(length);
            return;
        }

        consumePadding();
        trimCurlyBrackets();
        while (bytes.readRemaining() > 1) {
            copyOne(wire, true, true);
            consumePadding();
        }
    }

    private void trimCurlyBrackets() {
        // If the next byte is a closing curly bracket
        if (peekNextByte() == '}') {
            // Move past the closing curly bracket
            bytes.readSkip(1);

            // Consume any padding characters (e.g., whitespace)
            consumePadding();

            // Loop backwards through the byte buffer, trimming whitespace or other padding characters
            while (peekPreviousByte() <= ' ')
                bytes.writeSkip(-1);

            // If the previous character is also a closing curly bracket, skip past it
            if (peekPreviousByte() == '}')
                bytes.writeSkip(-1);

            // TODO: Handle the case where an expected '}' character is missing (potential error situation)
        }
    }

    /**
     * Peeks the previous byte from the current read position without moving the read pointer.
     *
     * @return The byte value just before the current read position.
     */
    private int peekPreviousByte() {
        // Return the byte just before the current read limit
        return bytes.peekUnsignedByte(bytes.readLimit() - 1);
    }

    /**
     * Copies one segment of data from this wire to the given wire output.
     * The segment copied depends on the first character encountered (e.g., '{' indicates a map).
     * This method understands JSON structural elements and translates them appropriately.
     *
     * @param wire The wire output to copy the data to.
     * @param expectKeyValues Flag indicating if the current position is inside a map structure.
     * @param topLevel Flag indicating if this is the topmost level of the copy operation.
     * @throws InvalidMarshallableException if there's a problem with copying the data.
     */
    public void copyOne(@NotNull WireOut wire, boolean expectKeyValues, boolean topLevel) throws InvalidMarshallableException {
        consumePadding();
        int ch = bytes.readUnsignedByte();
        switch (ch) {
            case '\'':
            case '"':
                // Handle quoted values
                copyQuote(wire, ch, expectKeyValues, topLevel);
                if (expectKeyValues) {
                    // For key-value pairs, consume any padding and expect a colon (:) separator
                    consumePadding();
                    int ch2 = bytes.readUnsignedByte();
                    if (ch2 != ':')
                        throw new IORuntimeException("Expected a ':' but got a '" + (char) ch);

                    // Recursively copy the associated value after the colon
                    copyOne(wire, false, false);
                }
                return;

            case '{':
                // Determine if this is a type prefix or a standard map, and copy accordingly
                if (isTypePrefix())
                    copyTypePrefix(wire);
                else
                    copyMap(wire);
                return;

            case '[':
                // Handle sequences or arrays
                copySequence(wire);
                return;

            case '+':
            case '-':
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
            case '.':
                // Handle numeric values
                copyNumber(wire);
                return;

            case 'N':
            case 'n':
                // Special handling for the 'null' value
                if (compareRest(bytes, _ULL)) {
                    wire.getValueOut().nu11();
                    return;
                }
                break;

            case 'f':
            case 'F':
                // Special handling for the 'false' value
                if (compareRest(bytes, _ALSE)) {
                    wire.getValueOut().bool(false);
                    return;
                }
                break;

            case 't':
            case 'T':
                // Special handling for the 'true' value
                if (compareRest(bytes, _RUE)) {
                    wire.getValueOut().bool(true);
                    return;
                }
                break;

            default:
                break;
        }

        // If the code reaches here, an unexpected character sequence was found
        bytes.readSkip(-1);
        throw new IORuntimeException("Unexpected chars '" + bytes.parse8bit(StopCharTesters.CONTROL_STOP) + "'");
    }

    static boolean compareRest(@NotNull StreamingDataInput<?> in, @NotNull Bytes<?> s)
            throws BufferUnderflowException, ClosedIllegalStateException {
        if (s.length() > in.readRemaining())
            return false;
        long position = in.readPosition();
        for (int i = 0; i < s.length(); i++) {
            if (in.readUnsignedByte() != s.charAt(i)) {
                in.readPosition(position);
                return false;
            }
        }
        int ch = in.peekUnsignedByte();
        if (Character.isLetterOrDigit(ch)) {
            in.readPosition(position);
            return false;
        }
        while (ch > 0 && ch <= ' ') {
            in.readSkip(1);
            ch = in.peekUnsignedByte();
        }

        return true;
    }

    /**
     * Copies a type prefix from the input to the given wire output.
     * The type prefix is assumed to be a text value prefixed with '@'. This method will extract
     * the type prefix and pass it on to the wire output.
     *
     * @param wire The wire output to copy the type prefix to.
     * @throws InvalidMarshallableException if there's a problem with copying the data.
     */
    private void copyTypePrefix(WireOut wire) throws InvalidMarshallableException {
        final StringBuilder sb = acquireStringBuilder();

        // Extract the type literal
        getValueIn().text(sb);

        // Remove the '@' prefix from the type literal
        sb.deleteCharAt(0);
        wire.getValueOut().typePrefix(sb);

        // Consume any padding characters (e.g., whitespace)
        consumePadding();
        int ch = bytes.readUnsignedByte();
        if (ch != ':')
            throw new IORuntimeException("Expected a ':' after the type " + sb + " but got a " + (char) ch);

        // Recursively copy the associated value after the colon
        copyOne(wire, false, false);

        consumePadding();
        int ch2 = bytes.readUnsignedByte();
        if (ch2 != '}')
            throw new IORuntimeException("Expected a '}' after the type " + sb + " but got a " + (char) ch);
    }

    /**
     * Determines if the current position in the byte buffer represents a type prefix.
     * A type prefix is recognized by a leading '"' character followed by '@'.
     *
     * @return True if the current position indicates a type prefix, false otherwise.
     */
    private boolean isTypePrefix() {
        final long rp = bytes.readPosition();
        return bytes.peekUnsignedByte(rp) == '"'
                && bytes.peekUnsignedByte(rp + 1) == '@';
    }

    /**
     * Copies a quoted string value from the input to the given wire output.
     * This method handles escaped characters within the quoted string.
     *
     * @param wire The wire output to copy the quoted string to.
     * @param ch The starting quote character (either single or double quote).
     * @param inMap Flag indicating if the current position is inside a map structure.
     * @param topLevel Flag indicating if this is the topmost level of the copy operation.
     * @throws InvalidMarshallableException if there's a problem with copying the data.
     */
    private void copyQuote(WireOut wire, int ch, boolean inMap, boolean topLevel) throws InvalidMarshallableException {
        final StringBuilder sb = acquireStringBuilder();
        // Extract the quoted text
        while (bytes.readRemaining() > 0) {
            int ch2 = bytes.readUnsignedByte();
            if (ch2 == ch)
                break;
            sb.append((char) ch2);

            // If an escape character is found, append the following character as well
            if (ch2 == '\\')
                sb.append((char) bytes.readUnsignedByte());
        }

        // Process any escaped characters within the text
        unescape(sb);

        // Determine how to write the text to the wire based on the provided flags
        if (topLevel) {
            wire.writeEvent(String.class, sb);
        } else if (inMap) {
            wire.write(sb);
        } else {
            wire.getValueOut().text(sb);
        }
    }

    /**
     * Copies a map structure from the input to the given wire output.
     * A map is assumed to be a set of key-value pairs enclosed in curly braces '{}'.
     *
     * @param wire The wire output to copy the map structure to.
     * @throws InvalidMarshallableException if there's a problem with copying the data.
     */
    private void copyMap(WireOut wire) throws InvalidMarshallableException {
        wire.getValueOut().marshallable(out -> {
            consumePadding();

            // Process each key-value pair within the map until the end is reached or the buffer is exhausted
            while (bytes.readRemaining() > 0) {
                final int ch = peekNextByte();

                // If we've reached the end of the map, move past the closing brace and exit
                if (ch == '}') {
                    bytes.readSkip(1);
                    return;
                }

                // Process one key-value pair within the map
                copyOne(wire, true, false);

                // After processing a key-value pair, expect either a comma (next pair) or the end of the map
                expectComma('}');
            }
        });
    }

    /**
     * Consumes padding and expects either a comma (indicating another entry) or a given end character.
     *
     * @param end The expected end character (e.g., '}' for maps or ']' for sequences).
     */
    private void expectComma(char end) {
        consumePadding();
        final int ch = peekNextByte();

        // If we've reached the expected end character, simply return
        if (ch == end)
            return;

        // If a comma is found, move past it and consume any subsequent padding
        if (ch == ',') {
            bytes.readSkip(1);
            consumePadding();
        } else {
            throw new IORuntimeException("Expected a comma or '" + end + "' not a '" + (char) ch + "'");
        }
    }

    /**
     * Copies a sequence structure from the input to the given wire output.
     * A sequence is assumed to be a list of values enclosed in square brackets '[]'.
     *
     * @param wire The wire output to copy the sequence to.
     */
    private void copySequence(WireOut wire) {
        wire.getValueOut().sequence(out -> {
            // Consume any padding characters (e.g., whitespace) before the sequence content
            consumePadding();

            // Process each value within the sequence until the end is reached or the buffer is almost exhausted
            while (bytes.readRemaining() > 1) {
                final int ch = peekNextByte();

                // If we've reached the end of the sequence, move past the closing bracket and exit
                if (ch == ']') {
                    bytes.readSkip(1);
                    return;
                }

                // Process one value within the sequence
                copyOne(wire, false, false);

                // After processing a value, expect either a comma (next value) or the end of the sequence
                expectComma(']');
            }
        });
    }

    /**
     * Peeks at the next byte in the buffer without advancing the read position.
     *
     * @return The next byte from the current read position.
     */
    private int peekNextByte() {
        return bytes.peekUnsignedByte(bytes.readPosition());
    }

    /**
     * Copies a numeric value from the input buffer to the given wire output.
     * The method can handle both integer and decimal numbers. For binary wire outputs,
     * it can distinguish between the two and write the appropriate format. For textual wire outputs,
     * the number is written as is.
     *
     * @param wire The wire output to which the numeric value should be copied.
     */
    private void copyNumber(WireOut wire) {
        // Move back one position to re-read the first character of the number
        bytes.readSkip(-1);
        long rp = bytes.readPosition();
        boolean decimal = false;

        // Continuously read the buffer until a non-numeric character is encountered
        while (true) {
            int ch2 = peekNextByte();
            switch (ch2) {
                case '+':
                case '-':
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
                case '.':
                    bytes.readSkip(1);
                    // If we're dealing with a binary wire format
                    if (wire.isBinary()) {
                        // Check if the character represents a decimal point
                        decimal |= ch2 == '.';
                    } else {
                        // For textual wire formats, simply append the character
                        wire.bytes().append((char) ch2);
                    }
                    break;

                // If we encounter an end of structure character or any non-numeric character, stop parsing
                case '}':
                case ']':
                case ',':
                default:
                    if (wire.isBinary()) {
                        long rl = bytes.readLimit();
                        try {
                            // Set the read position and limit to parse just the number
                            bytes.readPositionRemaining(rp, bytes.readPosition() - rp);

                            // If the number had a decimal point, treat it as a double, otherwise as a long
                            if (decimal)
                                wire.getValueOut().float64(bytes.parseDouble());
                            else
                                wire.getValueOut().int64(bytes.parseLong());
                        } finally {
                            bytes.readLimit(rl);
                        }
                    } else {
                        // For textual wire outputs, append a comma after the number
                        wire.getValueOut().elementSeparator();
                    }
                    return;
            }
        }
    }

    @NotNull
    @Override
    protected Quotes needsQuotes(@NotNull CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"' || ch < ' ' || ch == '\\')
                return Quotes.DOUBLE;
        }
        return Quotes.NONE;
    }

    @Override
    void escape(@NotNull CharSequence s) {
        bytes.writeUnsignedByte('"');
        if (needsQuotes(s) == Quotes.NONE) {
            bytes.appendUtf8(s);
        } else {
            escape0(s, Quotes.DOUBLE);
        }
        bytes.writeUnsignedByte('"');
    }

    /**
     * Escapes special characters in a CharSequence as per JSON String encoding standards detailed in RFC 7159, Section 7.
     * This ensures that the resulting string can be safely embedded within a JSON string while preserving its meaning.
     * See <a href="https://www.rfc-editor.org/rfc/rfc7159#section-7">RFC 7159, Section 7</a> for more details.
     *
     * @param s The CharSequence to escape.
     * @param quotes Specifies the type of quotes used in the CharSequence and guides escaping.
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7159#section-7">RFC 7159, Section 7</a>
     */
    protected void escape0(@NotNull CharSequence s, @NotNull Quotes quotes) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            // Switch on each character and apply the appropriate escape sequence
            switch (ch) {
                case '\b': // Backspace
                    bytes.append("\\b");
                    break;
                case '\t': // Horizontal tab
                    bytes.append("\\t");
                    break;
                case '\f': // Form feed
                    bytes.append("\\f");
                    break;
                case '\n': // Line feed
                    bytes.append("\\n");
                    break;
                case '\r': // Carriage return
                    bytes.append("\\r");
                    break;
                case '"':
                    // If the character is the same as the quote type, escape it
                    if (ch == quotes.q) {
                        bytes.writeUnsignedByte('\\').writeUnsignedByte(ch);
                    } else {
                        bytes.writeUnsignedByte(ch);
                    }
                    break;
                case '\\': // Backslash
                    bytes.writeUnsignedByte('\\').writeUnsignedByte(ch);
                    break;
                default:
                    // For characters outside the ASCII range, or control characters below ASCII 32, use Unicode escape
                    if (ch < ' ' || ch > 127)
                        appendU4(ch);
                    else
                        bytes.append(ch);
                    break;
            }
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ValueOut writeEvent(Class<?> expectedType, Object eventKey) throws InvalidMarshallableException {
        return super.writeEvent(String.class, "" + eventKey);
    }

    @Override
    public void writeStartEvent() {
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
    protected StopCharsTester getStrictEscapingEndOfText() {
        StopCharsTester escaping = ThreadLocalHelper.getTL(STRICT_ESCAPED_END_OF_TEXT_JSON, STRICT_END_OF_TEXT_JSON_ESCAPING);
        // reset it.
        escaping.isStopChar(' ', ' ');
        return escaping;
    }

    class JSONReadDocumentContext extends TextReadDocumentContext {
        private int first;

        public JSONReadDocumentContext(@Nullable Wire wire) {
            super(wire);
        }

        @Override
        public void start() {
            first = bytes.peekUnsignedByte();
            if (first == '{') {
                bytes.readSkip(1);
                long lastOffset = bytes.readLimit() - 1;
                if (bytes.peekUnsignedByte(lastOffset) == '}')
                    bytes.readLimit(lastOffset);
            }
            super.start();
        }

        @Override
        public void close() {
            if (first == '{') {
                consumePadding();
                if (bytes.peekUnsignedByte() == '}')
                    bytes.readSkip(1);
            }
            super.close();
        }
    }

    /**
     * The JSONWriteDocumentContext class extends the TextWriteDocumentContext class.
     * It provides a specialized context for writing JSON data, adjusting writing positions
     * and handling JSON-specific syntax such as curly braces.
     *
         */
    class JSONWriteDocumentContext extends TextWriteDocumentContext {
        // Position marker to track the start of a JSON object
        private long start;

        /**
         * Constructor for JSONWriteDocumentContext.
         *
         * @param wire The wire to be used for writing data
         */
        public JSONWriteDocumentContext(Wire wire) {
            super(wire);
        }

        @Override
        public boolean isEmpty() {
            return wire().bytes().writePosition() == position + 1;
        }

        @Override
        public void start(boolean metaData) {
            int count = this.count;
            super.start(metaData);
            if (count == 0) {
                bytes.append('{');
                start = bytes.writePosition();
            }
        }

        @Override
        public void close() {
            super.close();
            if (count == 0) {
                if (bytes.writePosition() == start) {
                    bytes.writeSkip(-1);
                } else {
                    bytes.append('}');
                }
            }
        }
    }

    /**
     * The JSONValueOut class extends the YamlValueOut class.
     * It provides methods for adjusting and outputting values in JSON format.
     */
    class JSONValueOut extends YamlValueOut {

        @SuppressWarnings("rawtypes")
        @NotNull
        @Override
        public TextWire typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, Class<?> type) {
            prependSeparator();
            append("{\"@type\":\"");
            typeTranslator.accept(type, bytes);
            append("\"}");
            elementSeparator();
            return wireOut();
        }

        @Override
        protected void trimWhiteSpace() {
            if (bytes.endsWith('\n') || bytes.endsWith(' '))
                bytes.writeSkip(-1);
        }

        @Override
        protected void indent() {
            // No-op.
        }

        @NotNull
        @Override
        public String nullOut() {
            return "null";
        }

        @NotNull
        @Override
        public JSONWire typeLiteral(@Nullable CharSequence type) {

            startBlock('{');
            bytes.append("\"@type\":\"" + type + "\"");
            endBlock('}');

            return (JSONWire) wireOut();
        }

        @NotNull
        @Override
        public JSONValueOut typePrefix(@NotNull CharSequence typeName) {
            if (useTypes) {
                boolean nested = bytes.peekUnsignedByte(bytes.writePosition() - 1) == '{';
                if (!nested)
                    startBlock('{');
                bytes.append("\"@");
                bytes.append(applyAsAlias(classLookup, typeName));
                bytes.append("\":");
                if (nested) {
                    if (valueOutFromStart == null)
                         valueOutFromStart = new JSONValueOutFromStart();
                    return valueOutFromStart;
                }
            }
            return this;
        }

        private CharSequence applyAsAlias(ClassLookup classLookup, CharSequence typeName) {
            // TODO use classLookup.applyAsAlias(typeName);
            try {
                return classLookup.nameFor(classLookup.forName(typeName));
            } catch (Exception e) {
                return typeName;
            }
        }

        @Override
        public void endTypePrefix() {
            super.endTypePrefix();
            if (useTypes) {
                endBlock('}');
                elementSeparator();
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
        public JSONWire rawText(CharSequence value) {
            bytes.writeByte((byte) '\"');
            super.rawText(value);
            bytes.writeByte((byte) '\"');
            return JSONWire.this;
        }

        @Override
        public @NotNull JSONWire date(LocalDate localDate) {
            return (JSONWire) text(localDate.toString());
        }

        @Override
        public @NotNull JSONWire dateTime(LocalDateTime localDateTime) {
            return (JSONWire) text(localDateTime.toString());
        }

        @Override
        public @NotNull <V> JSONWire object(@NotNull Class<? extends V> expectedType, V v) throws InvalidMarshallableException {
            return (JSONWire) (useTypes ? super.object(v) : super.object(expectedType, v));
        }

        @Override
        public @NotNull JSONValueOut typePrefix(Class<?> type) {
            if (type.isPrimitive() || isWrapper(type) || type.isEnum()) {
                // Do nothing because there are no other alternatives
                // and thus, the type is implicitly given in the declaration.
                return this;
            } else {
                return (JSONValueOut) super.typePrefix(type);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull <K, V> JSONWire marshallable(@Nullable Map<K, V> map, @NotNull Class<K> kClass, @NotNull Class<V> vClass, boolean leaf) throws InvalidMarshallableException {
            return (JSONWire) super.marshallable(map, (Class<K>) String.class, vClass, leaf);
        }

        public @NotNull JSONWire time(final LocalTime localTime) {
            // Todo: fix quoted text
            return (JSONWire) super.time(localTime);
            /*return text(localTime.toString());*/
        }
    }

    class JSONValueOutFromStart extends JSONValueOut {
        @Override
        public void endTypePrefix() {
            elementSeparator();
        }
    }

    /**
     * The JSONValueIn class extends the TextValueIn class.
     * It provides specialized methods for interpreting values from JSON data,
     * ensuring proper handling of JSON-specific constructs like the "null" value.
     */
    class JSONValueIn extends TextValueIn {

        @Nullable
        private Type consumeTypeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            long start = bytes.readPosition();
            consumePadding();
            StringBuilder sb = Wires.acquireStringBuilderScoped().get();

            int code = readCode();
            if (code != '{') {
                bytes.readPosition(start);
                return null;
            }

            consumePadding();

            sb.setLength(0);
            text(sb);

            if (!"@type".contentEquals(sb)) {
                bytes.readPosition(start);
                return null;
            }

            consumePadding();

            if (readCode() != ':') {
                bytes.readPosition(start);
                return null;
            }

            consumePadding();

            sb.setLength(0);
            text(sb);

            String clazz = sb.toString().trim();
            if (clazz.isEmpty()) {
                bytes.readPosition(start);
                return null;
            }

            consumePadding();
            if (bytes.readRemaining() == 0 || bytes.readChar() != '}') {
                bytes.readPosition(start);
                return null;
            }
            consumePadding();

            if (bytes.readRemaining() > 0 || peekCode() == ',') {
                bytes.readSkip(1);
            }
            try {
                return classLookup.forName(clazz);
            } catch (ClassNotFoundRuntimeException e1) {
                if (unresolvedHandler != null)
                    unresolvedHandler.apply(clazz, e1.getCause());
                return UnresolvedType.of(clazz);
            }
        }

        /**
         * Determines if the current value represents a JSON null value.
         *
         * @return True if the value is "null" in the JSON context; otherwise, False.
         *         When true, it consumes the "null" and moves to the next token.
         *         When false, no data is read, only peaked.
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
        public @Nullable Object object() throws InvalidMarshallableException {
            return useTypes ? parseType() : super.object();
        }

        @Override
        public <E> @Nullable E object(@Nullable Class<E> clazz) throws InvalidMarshallableException {
            return useTypes ? parseType(null, clazz, true) : super.object(null, clazz, true);
        }

        @Override
        public <E> E object(@Nullable E using, @Nullable Class<? extends E> clazz) throws InvalidMarshallableException {
            return useTypes ? parseType(using, clazz, true) : super.object(using, clazz, true);
        }

        @Override
        public <E> E object(@Nullable E using, @Nullable Class<? extends E> clazz, boolean bestEffort) throws InvalidMarshallableException {
            return useTypes ? parseType(using, clazz, bestEffort) : super.object(using, clazz, bestEffort);
        }

        @Override
        public Class<?> typePrefix() {
            return super.typePrefix();
        }

        @Override
        public Object typePrefixOrObject(Class<?> tClass) {
            return super.typePrefixOrObject(tClass);
        }

        @Override
        public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
            return consumeTypeLiteral(unresolvedHandler);
        }

        @Override
        public @Nullable Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy) throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException {
            return super.marshallable(object, strategy);
        }

        @Override
        public boolean isTyped() {
            // Either we use types for sure or we might use types...
            return useTypes || super.isTyped();
        }

        /**
         * Parses the type of the object based on the data. If a type definition is present,
         * it will use that to determine the class of the object. Otherwise, it falls back
         * to the default parsing mechanism.
         *
         * @return The parsed object.
         * @throws InvalidMarshallableException If there's an issue with unmarshalling the data.
         */
        private Object parseType() throws InvalidMarshallableException {
            if (!hasTypeDefinition()) {
                return super.object();
            } else {
                final StringBuilder sb = acquireStringBuilder();
                sb.setLength(0);
                consume('{');
                this.wireIn().read(sb);
                final Class<?> clazz = classLookup().forName(sb.subSequence(1, sb.length()));
                Object object = parseType(null, clazz, true);
                consume('}');
                consumePadding(1);
                return object;
            }
        }

        private void consume(char c) {
            consumePadding();
            if (bytes.peekUnsignedByte() == c)
                bytes.readByte();
        }

        /**
         * Parses the type of the object based on the data and the given parameters. It will
         * either use the provided class or, if a type definition is present in the data, will
         * override with that. If the provided class or object instance is incompatible with the
         * type definition, it will throw a ClassCastException.
         *
         * @param using The object instance to use, or null if not provided.
         * @param clazz The class to parse the object as, or null if not provided.
         * @param bestEffort Indicates whether to give a best effort attempt to parse the object even if it's partially incorrect.
         * @return The parsed object.
         * @throws InvalidMarshallableException If there's an issue with unmarshalling the data.
         * @throws ClassCastException If there's a type mismatch between the provided class or instance and the type definition.
         */
        private <E> E parseType(@Nullable E using, @Nullable Class<? extends E> clazz, boolean bestEffort) throws InvalidMarshallableException {

            Type aClass = consumeTypeLiteral(null);
            if (aClass != null)
                return Jvm.uncheckedCast(aClass);

            if (!hasTypeDefinition()) {
                return super.object(using, clazz, bestEffort);
            } else {
                final StringBuilder sb = acquireStringBuilder();
                sb.setLength(0);
                readTypeDefinition(sb);
                final Class<E> overrideClass = Jvm.uncheckedCast(classLookup().forName(sb.subSequence(1, sb.length())));
                if (clazz != null && !clazz.isAssignableFrom(overrideClass))
                    throw new ClassCastException("Unable to cast " + overrideClass.getName() + " to " + clazz.getName());
                if (using != null && !overrideClass.isInstance(using))
                    throw new ClassCastException("Unable to reuse a " + using.getClass().getName() + " as a " + overrideClass.getName());
                final E result = super.object(using, overrideClass, bestEffort);

                // remove the closing bracket from the type definition
                consumePadding();
                final char endBracket = bytes.readChar();
                assert endBracket == '}' : "Missing end bracket }, got " + endBracket + " from " + bytes;
                consumePadding(1);

                return result;
            }
        }

        /**
         * Checks if the next set of characters in the bytes stream represents a type definition.
         * A type definition is expected to start with the pattern {"@ after consuming any padding.
         *
         * @return true if a type definition is found, false otherwise.
         */
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

        /**
         * Reads the type definition from the bytes stream into the provided StringBuilder.
         * It assumes that the current position in the bytes stream is the start of the type
         * definition and consumes characters until it encounters a colon (":").
         *
         * @param sb The StringBuilder to which the type definition will be appended.
         * @throws IORuntimeException If the expected opening bracket "{" is not found.
         */
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

        /**
         * Indicates whether types are being used in the current context or not.
         *
         * @return true if types are being used, false otherwise.
         */
        public boolean useTypes() {
            return useTypes;
        }
    }
}
