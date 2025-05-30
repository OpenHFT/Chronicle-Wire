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
 * Provides a {@link Wire} implementation for serialising and deserialising data in
 * JavaScript Object Notation (JSON). It extends {@link TextWire} and adapts its behaviour
 * for JSON specific syntax.
 * <p>
 * While sharing much of the base behaviour with text based wires such as YAML,
 * {@code JSONWire} ensures compliance with the JSON standard. Strings are always
 * double quoted and optional type information can be emitted via
 * {@link #useTypes(boolean)}.
 * <p>
 * Key features include configurable type output and control over trimming the outer
 * curly braces with {@link #trimFirstCurly(boolean)}.
 * Suitable for interoperability with systems expecting JSON, for web based APIs or
 * where human readable configuration in JSON is preferred.
 */
@SuppressWarnings("this-escape")
public class JSONWire extends TextWire {

    /** Internal bytes for the tail of the literal "null". */
    private static final @NotNull Bytes<byte[]> _ULL = Bytes.from("ull");
    /** @deprecated use {@link #_ULL} */
    @Deprecated(/* to be removed in x.28 */)
    public static final @NotNull Bytes<byte[]> ULL = _ULL;
    /** Internal bytes for the tail of "true". */
    private static final @NotNull Bytes<byte[]> _RUE = Bytes.from("rue");
    /** Internal bytes for the tail of "false". */
    private static final @NotNull Bytes<byte[]> _ALSE = Bytes.from("alse");

    /** Bytes store for the comma separator. */
    @SuppressWarnings("rawtypes")
    static final BytesStore<?, ?> COMMA = BytesStore.from(",");

    /** Thread local cache for a JSON aware {@link StopCharsTester}. */
    static final ThreadLocal<WeakReference<StopCharsTester>> STRICT_ESCAPED_END_OF_TEXT_JSON = new ThreadLocal<>();

    /** Supplier for {@link #STRICT_ESCAPED_END_OF_TEXT_JSON}. */
    static final Supplier<StopCharsTester> STRICT_END_OF_TEXT_JSON_ESCAPING = TextStopCharsTesters.STRICT_END_OF_TEXT_JSON::escaping;

    /** When true, type information is written and expected during parsing. */
    boolean useTypes;
    /** Helper used when writing type prefixes. */
    private JSONValueOutFromStart valueOutFromStart;

    /**
     * Creates a JSONWire backed by an elastic on heap buffer. The
     * instance uses {@code use8bit=false} and {@link #trimFirstCurly(boolean)}
     * defaults to {@code false}.
     */
    @SuppressWarnings("rawtypes")
    public JSONWire() {
        this(Bytes.allocateElasticOnHeap());
    }

    /**
     * Wraps the given bytes.
     *
     * @param bytes   buffer to use
     * @param use8bit inherited flag controlling character encoding
     */
    public JSONWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
        trimFirstCurly(false);
    }

    /**
     * Wraps the given bytes using {@code use8bit=false}.
     *
     * @param bytes buffer to use
     */
    @SuppressWarnings("rawtypes")
    public JSONWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    /**
     * Creates a new instance initialised with the supplied JSON string.
     *
     * @param text JSON data
     * @return wire ready for reading
     */
    @NotNull
    public static JSONWire from(@NotNull String text) {
        return new JSONWire(Bytes.from(text));
    }

    /**
     * Returns the content of any wire as a JSON string.
     * Useful when converting between formats or for debugging.
     *
     * @param wire source wire
     * @return JSON representation of the source
     * @throws InvalidMarshallableException if marshalling fails
     */
    public static String asText(@NotNull Wire wire) throws InvalidMarshallableException {
        long pos = wire.bytes().readPosition();
        @NotNull JSONWire tw = new JSONWire(nativeBytes());
        wire.copyTo(tw);
        wire.bytes().readPosition(pos);

        return tw.toString();
    }

    /**
     * Internal utility to check if the given {@code Class} is a Java primitive
     * wrapper type, for example {@link Integer} or {@link Boolean}.
     */
    static boolean isWrapper(Class<?> type) {
        return type == Integer.class || type == Long.class || type == Float.class ||
                type == Double.class || type == Short.class || type == Character.class ||
                type == Byte.class || type == Boolean.class || type == Void.class;
    }

    /**
     * Returns {@code String.class} as JSON object keys are always strings.
     */
    @Override
    protected Class<?> defaultKeyClass() {
        return String.class;
    }

    /**
     * Configures whether this wire should emit and expect explicit type information.
     * Returns this instance for chaining.
     *
     * @param outputTypes true to include '{@literal @}type' metadata
     */
    public JSONWire useTypes(boolean outputTypes) {
        this.useTypes = outputTypes;
        return this;
    }

    /**
     * Returns {@code true} if this wire expects explicit type hints.
     */
    public boolean useTypes() {
        return useTypes;
    }

    /**
     * Configures document contexts for text-based JSON streams.
     */
    @Override
    public @NotNull TextWire useTextDocuments() {
        readContext = new JSONReadDocumentContext(this);
        writeContext = trimFirstCurly()
                ? new TextWriteDocumentContext(this)
                : new JSONWriteDocumentContext(this);
        return this;
    }

    /**
     * Factory for the {@link JSONValueOut} used by this wire.
     */
    @NotNull
    @Override
    protected JSONValueOut createValueOut() {
        return new JSONValueOut();
    }

    /**
     * Factory for the JSON-specific {@link TextValueIn} implementation.
     */
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

    /**
     * Copies the remaining JSON from this wire into {@code wire}, trimming outer
     * braces when required.
     */
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

    /**
     * Removes the outermost curly braces from the buffer when present.
     */
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
     * Peeks at the byte immediately before the current read limit.
     */
    private int peekPreviousByte() {
        // Return the byte just before the current read limit
        return bytes.peekUnsignedByte(bytes.readLimit() - 1);
    }

    /**
     * Recursively copies one JSON element from this wire to {@code wire}.
     *
     * @param destWire  destination wire
     * @param inObject  true if a key is expected next
     * @param isRoot    true if copying the outer element
     */
    public void copyOne(@NotNull WireOut destWire, boolean inObject, boolean isRoot) throws InvalidMarshallableException {
        consumePadding();
        int ch = bytes.readUnsignedByte();
        switch (ch) {
            case '\'':
            case '"':
                // Handle quoted values
                copyQuote(destWire, ch, inObject, isRoot);
                if (inObject) {
                    // For key-value pairs, consume any padding and expect a colon (:) separator
                    consumePadding();
                    int ch2 = bytes.readUnsignedByte();
                    if (ch2 != ':')
                        throw new IORuntimeException("Expected a ':' but got a '" + (char) ch);

                    // Recursively copy the associated value after the colon
                    copyOne(destWire, false, false);
                }
                return;

            case '{':
                // Determine if this is a type prefix or a standard map, and copy accordingly
                if (isTypePrefix())
                    copyTypePrefix(destWire);
                else
                    copyMap(destWire);
                return;

            case '[':
                // Handle sequences or arrays
                copySequence(destWire);
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
                copyNumber(destWire);
                return;

            case 'N':
            case 'n':
                // Special handling for the 'null' value
                if (compareRest(bytes, _ULL)) {
                    destWire.getValueOut().nu11();
                    return;
                }
                break;

            case 'f':
            case 'F':
                // Special handling for the 'false' value
                if (compareRest(bytes, _ALSE)) {
                    destWire.getValueOut().bool(false);
                    return;
                }
                break;

            case 't':
            case 'T':
                // Special handling for the 'true' value
                if (compareRest(bytes, _RUE)) {
                    destWire.getValueOut().bool(true);
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

    /**
     * Compares the remaining characters in {@code source} with {@code expected}, consuming
     * them if they match and ensuring the next char is not alphanumeric.
     */
    static boolean compareRest(@NotNull StreamingDataInput<?> source, @NotNull Bytes<?> expected)
            throws BufferUnderflowException, ClosedIllegalStateException {
        if (expected.length() > source.readRemaining())
            return false;
        long position = source.readPosition();
        for (int i = 0; i < expected.length(); i++) {
            if (source.readUnsignedByte() != expected.charAt(i)) {
                source.readPosition(position);
                return false;
            }
        }
        int ch = source.peekUnsignedByte();
        if (Character.isLetterOrDigit(ch)) {
            source.readPosition(position);
            return false;
        }
        while (ch > 0 && ch <= ' ') {
            source.readSkip(1);
            ch = source.peekUnsignedByte();
        }

        return true;
    }

    /**
     * Helper for {@link #copyOne} that reads a JSON '@type' prefix and writes it
     * using {@link ValueOut#typePrefix(CharSequence)} on {@code destWire}.
     */
    private void copyTypePrefix(WireOut destWire) throws InvalidMarshallableException {
        final StringBuilder sb = acquireStringBuilder();

        // Extract the type literal
        getValueIn().text(sb);

        // Remove the '@' prefix from the type literal
        sb.deleteCharAt(0);
        destWire.getValueOut().typePrefix(sb);

        // Consume any padding characters (e.g., whitespace)
        consumePadding();
        int ch = bytes.readUnsignedByte();
        if (ch != ':')
            throw new IORuntimeException("Expected a ':' after the type " + sb + " but got a " + (char) ch);

        // Recursively copy the associated value after the colon
        copyOne(destWire, false, false);

        consumePadding();
        int ch2 = bytes.readUnsignedByte();
        if (ch2 != '}')
            throw new IORuntimeException("Expected a '}' after the type " + sb + " but got a " + (char) ch);
    }

    /**
     * Checks if the next bytes form a JSON '@type' prefix.
     */
    private boolean isTypePrefix() {
        final long rp = bytes.readPosition();
        return bytes.peekUnsignedByte(rp) == '"'
                && bytes.peekUnsignedByte(rp + 1) == '@';
    }

    /**
     * Copies a JSON string value, unescaping it before writing to {@code wire}.
     *
     * @param destWire  destination wire
     * @param quoteChar opening quote character
     * @param parsingKey true if reading a map key
     * @param isRoot    true if copying the outer element
     */
    private void copyQuote(WireOut destWire, int quoteChar, boolean parsingKey, boolean isRoot) throws InvalidMarshallableException {
        final StringBuilder sb = acquireStringBuilder();
        // Extract the quoted text
        while (bytes.readRemaining() > 0) {
            int ch2 = bytes.readUnsignedByte();
            if (ch2 == quoteChar)
                break;
            sb.append((char) ch2);

            // If an escape character is found, append the following character as well
            if (ch2 == '\\')
                sb.append((char) bytes.readUnsignedByte());
        }

        // Process any escaped characters within the text
        unescape(sb);

        // Determine how to write the text to the wire based on the provided flags
        if (isRoot) {
            destWire.writeEvent(String.class, sb);
        } else if (parsingKey) {
            destWire.write(sb);
        } else {
            destWire.getValueOut().text(sb);
        }
    }

    /**
     * Internal helper for {@link #copyOne} that copies a JSON object ({@code {...}})
     * to {@code destWire} by recursively invoking {@link #copyOne} for each entry.
     */
    private void copyMap(WireOut destWire) throws InvalidMarshallableException {
        destWire.getValueOut().marshallable(out -> {
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
                copyOne(destWire, true, false);

                // After processing a key-value pair, expect either a comma (next pair) or the end of the map
                expectComma('}');
            }
        });
    }

    /**
     * After copying an element in a JSON array or a value in a JSON object, this
     * method consumes padding and expects either a comma (',' to separate from
     * the next element) or the specified {@code end} character ('}' for objects,
     * ']' for arrays).
     *
     * @param closingChar the terminating character that indicates the end of the current
     *                    structure
     */
    private void expectComma(char closingChar) {
        consumePadding();
        final int ch = peekNextByte();

        // If we've reached the expected end character, simply return
        if (ch == closingChar)
            return;

        // If a comma is found, move past it and consume any subsequent padding
        if (ch == ',') {
            bytes.readSkip(1);
            consumePadding();
        } else {
            throw new IORuntimeException("Expected a comma or '" + closingChar + "' not a '" + (char) ch + "'");
        }
    }

    /**
     * Internal helper for {@link #copyOne} to copy a JSON array ({@code [...]}).
     * Recursively calls {@link #copyOne} for each element in the array.
     */
    private void copySequence(WireOut destWire) {
        destWire.getValueOut().sequence(out -> {
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
                copyOne(destWire, false, false);

                // After processing a value, expect either a comma (next value) or the end of the sequence
                expectComma(']');
            }
        });
    }

    /**
     * Internal helper to peek at the next byte at the current read position
     * without consuming it.
     *
     * @return the next byte from the current read position
     */
    private int peekNextByte() {
        return bytes.peekUnsignedByte(bytes.readPosition());
    }

    /**
     * Internal helper for {@link #copyOne} to copy a JSON number (integer or
     * floating-point). It reads the sequence of digits (and optional decimal
     * point/exponent) and writes it to the target {@code wire}, attempting to
     * preserve the numeric type (for example as {@code int64} or {@code float64}
     * if the target wire is binary).
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

    /**
     * Determines if the given {@link CharSequence} {@code s} requires double
     * quotes for JSON string representation. In JSON all strings must be quoted;
     * this check looks for characters that must be escaped (control characters,
     * backslash or a double quote).
     */
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

    /**
     * Writes the given {@link CharSequence} as a JSON string, always enclosing
     * it in double quotes and escaping internal characters as required using
     * {@link #escape0(CharSequence, Quotes)}.
     */
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
     * Escapes characters in {@code s} according to JSON string encoding rules
     * (RFC&nbsp;7159, section&nbsp;7). Uses {@code \uXXXX} for control
     * characters and characters outside the printable ASCII range. Always uses
     * {@code "} as the quote character, ignoring the {@code quotes} parameter
     * from the superclass.
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

    /**
     * A specialised {@link TextReadDocumentContext} for JSON. It handles the
     * consumption of optional leading/trailing curly braces {@code {}} that
     * might enclose a top-level JSON document.
     */
    class JSONReadDocumentContext extends TextReadDocumentContext {
        private int first;

        public JSONReadDocumentContext(@Nullable Wire wire) {
            super(wire);
        }

        /**
         * Prepares for reading a JSON document. Peeks for an opening curly
         * brace '{'. If found, it is consumed and the read limit adjusted to
         * tentatively exclude a possible closing brace. Then delegates to the
         * superclass implementation.
         */
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

        /**
         * Finalises reading the JSON document. If an opening brace was consumed
         * by {@link #start()}, this method consumes any padding and a matching
         * closing curly brace '}' if present. Then delegates to the superclass
         * close.
         */
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
     * A specialised {@link TextWriteDocumentContext} for JSON. It ensures that
     * top-level JSON documents are correctly enclosed in curly braces '{}' if
     * {@link JSONWire#trimFirstCurly()} is false (the default when
     * {@link #useTextDocuments()} is invoked).
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

        /**
         * Checks if the document is empty, considering the potential initial
         * '{'.
         */
        @Override
        public boolean isEmpty() {
            return wire().bytes().writePosition() == position + 1;
        }

        /**
         * Prepares for writing a JSON document. If this is the outermost
         * document context (count == 0) and a leading brace is required, it
         * appends an opening '{' and records its position.
         */
        @Override
        public void start(boolean metaData) {
            int count = this.count;
            super.start(metaData);
            if (count == 0) {
                bytes.append('{');
                start = bytes.writePosition();
            }
        }

        /**
         * Finalises the JSON document. If this is the outermost context and an
         * opening brace was written by {@link #start(boolean)}, it appends a
         * closing '}'. If the document was empty (only the opening brace was
         * written) it backtracks to remove the empty braces.
         */
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

        /**
         * Write a special double value (e.g. NaN) as a string to the given bytes.
         *
         * @param bytes The bytes to append the stringified double value to
         * @param value The double value to convert to a string
         */
        @Override
        protected void writeSpecialDoubleValueToBytes(Bytes<?> bytes, double value) {
            bytes.append('"');
            bytes.append(Double.toString(value));
            bytes.append('"');
        }

        /**
         * Write a special double value (e.g. NaN) as a string to the given bytes.
         *
         * @param bytes The bytes to append the stringified double value to
         * @param value The double value to convert to a string
         */
        @Override
        protected void writeSpecialFloatValueToBytes(Bytes<?> bytes, float value) {
            bytes.append('"');
            bytes.append(Float.toString(value));
            bytes.append('"');
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
     * The {@link ValueIn} implementation for {@link JSONWire}.
     * Handles JSON-specific parsing nuances, such as string quoting,
     * number parsing, and type prefix ("{@literal @}type") detection
     * if {@link JSONWire#useTypes()} is enabled.
     */
    class JSONValueIn extends TextValueIn {

        /**
         * Internal helper to parse a JSON type literal expression like
         * {@code {"@type":"com.example.MyClass"}}.
         * Returns the resolved {@link Type} or a representation of an unresolved type.
         */
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
         * Checks if the current value is the JSON literal {@code null},
         * consuming it if true.
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

        /**
         * Reads a JSON string (expecting double quotes and handling escapes) or
         * a JSON literal such as {@code null}, {@code true} or {@code false}.
         * Returns {@code null} if the JSON value is {@code null}.
         */
        @Override
        public String text() {
            @Nullable String text = super.text();
            return text == null || text.equals("null") ? null : text;
        }

        /**
         * For JSON, most characters following a value (such as {@code }} or {@code ,}) act as separators.
         */
        @Override
        protected boolean isASeparator(int nextChar) {
            return true;
        }

        /**
         * If {@link JSONWire#useTypes()} is enabled, attempts to
         * {@link #parseType(Object, Class, boolean)}. Otherwise delegates to
         * the superclass for text-based object deserialisation.
         */
        @Override
        public @Nullable Object object() throws InvalidMarshallableException {
            return useTypes ? parseType() : super.object();
        }

        /**
         * If {@link JSONWire#useTypes()} is enabled, attempts to
         * {@link #parseType(Object, Class, boolean)}. Otherwise delegates to
         * the superclass for text-based object deserialisation.
         */
        @Override
        public <E> @Nullable E object(@Nullable Class<E> clazz) throws InvalidMarshallableException {
            return useTypes ? parseType(null, clazz, true) : super.object(null, clazz, true);
        }

        /**
         * If {@link JSONWire#useTypes()} is enabled, attempts to
         * {@link #parseType(Object, Class, boolean)}. Otherwise delegates to
         * the superclass for text-based object deserialisation.
         */
        @Override
        public <E> E object(@Nullable E using, @Nullable Class<? extends E> clazz) throws InvalidMarshallableException {
            return useTypes ? parseType(using, clazz, true) : super.object(using, clazz, true);
        }

        /**
         * If {@link JSONWire#useTypes()} is enabled, attempts to
         * {@link #parseType(Object, Class, boolean)}. Otherwise delegates to
         * the superclass for text-based object deserialisation.
         */
        @Override
        public <E> E object(@Nullable E using, @Nullable Class<? extends E> clazz, boolean bestEffort) throws InvalidMarshallableException {
            return useTypes ? parseType(using, clazz, bestEffort) : super.object(using, clazz, bestEffort);
        }

        /**
         * For JSON, attempts to find the {@code "@type":"..."} prefix if types
         * are enabled.
         */
        @Override
        public Class<?> typePrefix() {
            return super.typePrefix();
        }

        /**
         * As {@link #typePrefix()} but falls back to reading the whole object
         * if no prefix is present.
         */
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
         * Core logic for deserialising a typed JSON object. If
         * {@link #hasTypeDefinition()} returns {@code true} this reads the
         * class name from the {@code "@type"} field and uses it to parse the
         * remainder of the object. Otherwise it falls back to standard object
         * parsing.
         *
         * @return The parsed object.
         * @throws InvalidMarshallableException If there is an issue with
         *                                      unmarshalling the data.
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
         * Core logic for deserialising a typed JSON object with optional hints
         * from the caller. If {@link #hasTypeDefinition()} is {@code true} the
         * type is read from the {@code "@type"} field and used to guide
         * deserialisation. The supplied class or instance is validated against
         * that type. When no type definition is present this method falls back to
         * standard object parsing.
         *
         * @param using       the object instance to reuse, or {@code null}
         * @param targetType  the class expected, or {@code null}
         * @param lenient     whether to attempt deserialisation even if partially incorrect
         * @return the parsed object
         * @throws InvalidMarshallableException if unmarshalling fails
         * @throws ClassCastException           if the parsed type is incompatible
         *                                      with {@code clazz} or {@code using}
         */
        private <E> E parseType(@Nullable E using, @Nullable Class<? extends E> targetType, boolean lenient) throws InvalidMarshallableException {

            Type aClass = consumeTypeLiteral(null);
            if (aClass != null)
                return Jvm.uncheckedCast(aClass);

            if (!hasTypeDefinition()) {
                return super.object(using, targetType, lenient);
            } else {
                final StringBuilder sb = acquireStringBuilder();
                sb.setLength(0);
                readTypeDefinition(sb);
                final Class<E> overrideClass = Jvm.uncheckedCast(classLookup().forName(sb.subSequence(1, sb.length())));
                if (targetType != null && !targetType.isAssignableFrom(overrideClass))
                    throw new ClassCastException("Unable to cast " + overrideClass.getName() + " to " + targetType.getName());
                if (using != null && !overrideClass.isInstance(using))
                    throw new ClassCastException("Unable to reuse a " + using.getClass().getName() + " as a " + overrideClass.getName());
                final E result = super.object(using, overrideClass, lenient);

                // remove the closing bracket from the type definition
                consumePadding();
                final char endBracket = bytes.readChar();
                assert endBracket == '}' : "Missing end bracket }, got " + endBracket + " from " + bytes;
                consumePadding(1);

                return result;
            }
        }

        /**
         * Checks if the current JSON structure appears to be an object starting
         * with an {@code "@type"} key.
         *
         * @return {@code true} if a type definition is found
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
         * Reads the {@code "@type"} key and its string value into {@code sb}.
         *
         * @param typeBuffer the destination buffer
         * @throws IORuntimeException if the expected opening brace is missing
         */
        void readTypeDefinition(StringBuilder typeBuffer) {
            consumePadding();
            if (bytes.readChar() != '{')
                throw new IORuntimeException("Expected { but got " + bytes);
            consumePadding();
            text(typeBuffer);
            consumePadding();
            final char colon = bytes.readChar();
            assert colon == ':' : "Expected : but got " + colon;

        }

        /**
         * Returns the {@link JSONWire#useTypes()} setting.
         */
        public boolean useTypes() {
            return useTypes;
        }
    }

    /**
     * Returns the remaining content of this {@code JSONWire} as a UTF-8 string.
     */
    @Override
    public String toString() {
        return toUtf8String();
    }
}


