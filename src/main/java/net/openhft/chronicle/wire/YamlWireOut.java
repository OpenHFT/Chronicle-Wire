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
import net.openhft.chronicle.bytes.render.GeneralDecimaliser;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
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
 * Provides functionality for writing data in a YAML-based wire format.
 * This class encapsulates methods and attributes to handle data serialization into YAML format.
 *
 * @param <T> The type that extends YamlWireOut
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class YamlWireOut<T extends YamlWireOut<T>> extends AbstractWire {

    // Default values and static configurations for the YAML writer
    private static final boolean APPEND_0 = Jvm.getBoolean("bytes.append.0", true);

    public static final BytesStore TYPE = BytesStore.from("!type ");
    static final String NULL = "!null \"\"";
    static final BitSet STARTS_QUOTE_CHARS = new BitSet();
    static final BitSet QUOTE_CHARS = new BitSet();
    static final BytesStore COMMA_SPACE = BytesStore.from(", ");
    static final BytesStore COMMA_NEW_LINE = BytesStore.from(",\n");
    static final BytesStore NEW_LINE = BytesStore.from("\n");
    static final BytesStore EMPTY_AFTER_COMMENT = BytesStore.wrap(new byte[0]); // not the same as EMPTY, so we can check this value.
    static final BytesStore EMPTY = BytesStore.from("");
    static final BytesStore SPACE = BytesStore.from(" ");
    static final BytesStore END_FIELD = NEW_LINE;
    static final char[] HEXADECIMAL = "0123456789ABCDEF".toCharArray();

    // Static initializer block to configure quote characters for the YAML writer
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
    protected final ScopedResourcePool<StringBuilder> sb = StringBuilderPool.createThreadLocal(1);
    private boolean addTimeStamps = false;
    private boolean trimFirstCurly = true;

    /**
     * Constructs a new instance of YamlWireOut with specified bytes and 8-bit flag.
     *
     * @param bytes The bytes buffer for the wire format.
     * @param use8bit Boolean flag indicating whether to use 8-bit values.
     */
    protected YamlWireOut(@NotNull Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
        bytes.decimaliser(GeneralDecimaliser.GENERAL)
                .fpAppend0(APPEND_0);
    }

    /**
     * Checks if timestamps should be added during serialization.
     *
     * @return True if timestamps should be added, otherwise false.
     */
    public boolean addTimeStamps() {
        return addTimeStamps;
    }

    /**
     * Configures whether to add timestamps during serialization.
     * This method follows the builder pattern allowing chained method calls.
     *
     * @param addTimeStamps Boolean indicating whether to add timestamps.
     * @return The current instance of YamlWireOut.
     */
    public T addTimeStamps(boolean addTimeStamps) {
        this.addTimeStamps = addTimeStamps;
        return (T) this;
    }

    /**
     * Creates and returns a new instance of {@link YamlValueOut}.
     *
     * @return A new YamlValueOut instance.
     */
    @NotNull
    protected YamlValueOut createValueOut() {
        return new YamlValueOut();
    }

    /**
     * Acquires and clears the internal StringBuilder {@code sb} for use.
     * The method ensures the StringBuilder's count is reset to 0 before returning.
     *
     * @return The internal StringBuilder after it has been cleared.
     */
    @NotNull
    protected ScopedResource<StringBuilder> acquireStringBuilder() {
        return sb.get();
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
    public ValueOut writeEvent(Class expectedType, Object eventKey) throws InvalidMarshallableException {
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

    /**
     * Escapes the given CharSequence {@code s} based on the requirements of the YAML format.
     * If the sequence requires quotes, it will be enclosed with the appropriate quote character;
     * otherwise, the sequence will be escaped without quotes.
     *
     * @param s The CharSequence to be escaped.
     */
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

    // https://yaml.org/spec/1.2.2/#escaped-characters
    /**
     * Helper method to escape special characters in the given CharSequence {@code s} based on the requirements of the YAML format.
     * The method handles the specific escaping requirements for various control and special characters.
     *
     * @param s The CharSequence to be escaped.
     * @param quotes The type of quotes used to determine how certain characters are escaped.
     */
    protected void escape0(@NotNull CharSequence s, @NotNull Quotes quotes) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\0':
                    bytes.append("\\0");  // Null character
                    break;
                case 7:
                    bytes.append("\\a");  // Bell (alert)
                    break;
                case '\b':
                    bytes.append("\\b");  // Backspace
                    break;
                case '\t':
                    bytes.append("\\t");  // Horizontal tab
                    break;
                case '\n':
                    bytes.append("\\n");  // Newline
                    break;
                case 0xB:
                    bytes.append("\\v");  // Vertical tab
                    break;
                case '\f':
                    bytes.append("\\f");  // Formfeed
                    break;
                case '\r':
                    bytes.append("\\r");  // Carriage return
                    break;
                case 0x1B:
                    bytes.append("\\e");  // Escape
                    break;
                case '"':
                    // Handling double quotes
                    if (ch == quotes.q) {
                        bytes.writeUnsignedByte('\\').writeUnsignedByte(ch);
                    } else {
                        bytes.writeUnsignedByte(ch);
                    }
                    break;
                case '\'':
                    // Handling single quotes
                    if (ch == quotes.q) {
                        bytes.writeUnsignedByte('\\').writeUnsignedByte(ch);
                    } else {
                        bytes.writeUnsignedByte(ch);
                    }
                    break;
                case '\\':
                    bytes.writeUnsignedByte('\\').writeUnsignedByte(ch);  // Escape backslash itself
                    break;
                case 0x85:
                    bytes.appendUtf8("\\N");  // Next line
                    break;
                case 0xA0:
                    bytes.appendUtf8("\\_");  // Non-breaking space
                    break;
                case 0x2028:
                    bytes.appendUtf8("\\L");  // Line separator
                    break;
                case 0x2029:
                    bytes.appendUtf8("\\P");  // Paragraph separator
                    break;
                default:
                    // Handling characters outside the ASCII range and other special characters
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

    /**
     * Appends a 2-character hexadecimal representation of the given character {@code ch} to the output bytes.
     * This is used for character escaping.
     *
     * @param ch The character to be converted to hexadecimal.
     */
    private void appendX2(char ch) {
        bytes.append('\\');
        bytes.append('x');
        bytes.append(HEXADECIMAL[(ch >> 4) & 0xF]);
        bytes.append(HEXADECIMAL[ch & 0xF]);
    }

    /**
     * Appends a 4-character hexadecimal Unicode representation of the given character {@code ch} to the output bytes.
     * This is used for character escaping.
     *
     * @param ch The character to be converted to hexadecimal Unicode representation.
     */
    protected void appendU4(char ch) {
        bytes.append('\\');
        bytes.append('u');
        bytes.append(HEXADECIMAL[ch >> 12]);
        bytes.append(HEXADECIMAL[(ch >> 8) & 0xF]);
        bytes.append(HEXADECIMAL[(ch >> 4) & 0xF]);
        bytes.append(HEXADECIMAL[ch & 0xF]);
    }

    /**
     * Determines the type of quotes (if any) required for the given CharSequence {@code s} based on the YAML format's escaping requirements.
     * This method decides between using no quotes, single quotes, or double quotes.
     *
     * @param s The CharSequence to be analyzed.
     * @return The type of quotes required.
     */
    @NotNull
    protected Quotes needsQuotes(@NotNull CharSequence s) {
        @NotNull Quotes quotes = Quotes.NONE;

        // Empty strings require double quotes.
        if (s.length() == 0)
            return Quotes.DOUBLE;

        // If string starts with special characters or ends with whitespace, use double quotes.
        if (STARTS_QUOTE_CHARS.get(s.charAt(0)) ||
                Character.isWhitespace(s.charAt(s.length() - 1)))
            return Quotes.DOUBLE;
        boolean hasSingleQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            // Characters in QUOTE_CHARS or outside ASCII range need double quotes.
            if (QUOTE_CHARS.get(ch) || ch < ' ' || ch > 127)
                return Quotes.DOUBLE;

            // Track if single quote is present in the string.
            if (ch == '\'')
                hasSingleQuote = true;

            // If a double quote is found followed by a single quote, return double quotes.
            if (ch == '"') {
                if (i < s.length() - 1 && s.charAt(i + 1) == '\'')
                    return Quotes.DOUBLE;
                quotes = Quotes.SINGLE;
            }
        }

        // If only single quotes are found, no quotes are needed.
        if (hasSingleQuote)
            return Quotes.NONE;
        return quotes;
    }

    /**
     * Appends the given CharSequence {@code cs} to the output bytes using either an 8-bit or UTF-8 encoding, depending on {@code use8bit}.
     *
     * @param cs CharSequence to be appended.
     */
    public void append(@NotNull CharSequence cs) {
        if (use8bit)
            bytes.append8bit(cs);
        else
            bytes.appendUtf8(cs);
    }

    /**
     * Appends a subsequence of the given CharSequence {@code cs} to the output bytes using either an 8-bit or UTF-8 encoding.
     *
     * @param cs     CharSequence from which a subsequence will be appended.
     * @param offset Starting index of the subsequence.
     * @param length Length of the subsequence.
     */
    public void append(@NotNull CharSequence cs, int offset, int length) {
        if (use8bit)
            bytes.append8bit(cs, offset, offset + length);
        else
            bytes.appendUtf8(cs, offset, length);
    }

    /**
     * Writes the representation of the object {@code o} to the output. Differentiates the serialization logic based on the type of the object.
     *
     * @param o The object to be serialized.
     * @throws InvalidMarshallableException if an error occurs during serialization.
     */
    public void writeObject(Object o) throws InvalidMarshallableException {
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

    /**
     * Writes the representation of the object {@code o} to the output with a specified indentation level.
     *
     * @param o           The object to be serialized.
     * @param indentation The number of spaces to use for indentation.
     * @throws InvalidMarshallableException if an error occurs during serialization.
     */
    private void writeObject(Object o, int indentation) throws InvalidMarshallableException {
        writeTwo('-', ' ');
        indentation(indentation - 2);
        valueOut.object(o);
    }

    /**
     * Inserts the specified number of spaces into the output bytes for indentation.
     *
     * @param indentation The number of spaces to insert.
     */
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

    /**
     * Writes two characters to the 'bytes' object sequentially.
     *
     * @param ch1 First character to write.
     * @param ch2 Second character to write.
     */
    void writeTwo(char ch1, char ch2) {
        bytes.writeUnsignedByte(ch1);
        bytes.writeUnsignedByte(ch2);
    }

    /**
     * Returns a flag indicating if the top-level curly brackets in the serialized YAML should be dropped.
     *
     * @return {@code true} if the top-level curly brackets should be dropped; {@code false} otherwise.
     */
    public boolean trimFirstCurly() {
        return trimFirstCurly;
    }

    /**
     * Sets whether the top-level curly brackets in the serialized YAML should be dropped.
     *
     * @param trimFirstCurly {@code true} to drop the top-level curly brackets; {@code false} to include them.
     * @return The current instance of {@code YamlWireOut} (fluent API style).
     */
    public T trimFirstCurly(boolean trimFirstCurly) {
        this.trimFirstCurly = trimFirstCurly;
        return (T) this;
    }

    /**
     * This internal class represents an output value in the YAML format. It provides functionalities related
     * to appending separators, handling whitespace, and maintaining indentation among others.
     */
    class YamlValueOut implements ValueOut, CommentAnnotationNotifier {
        protected boolean hasCommentAnnotation = false;

        // The current indentation level for the value.
        protected int indentation = 0;

        // A list of separators to be used when writing the value.
        @NotNull
        protected List<BytesStore> seps = new ArrayList<>(4);

        // The current separator being used.
        @NotNull
        protected BytesStore sep = BytesStore.empty();

        // Flag indicating if the value is a leaf node (i.e., doesn't have child elements).
        protected boolean leaf = false;

        // Flag indicating if default values should be dropped from the output.
        protected boolean dropDefault = false;

        // The name of the event associated with this value (if any).
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

        /**
         * Appends the current separator to the output bytes, and resets the separator.
         */
        void prependSeparator() {
            appendSep();
            sep = BytesStore.empty();
        }

        /**
         * Appends the current separator to the output bytes and handles any necessary whitespace trimming.
         */
        protected void appendSep() {
            append(sep);
            trimWhiteSpace();
            if (bytes.endsWith('\n') || sep == EMPTY_AFTER_COMMENT)
                indent();
        }

        /**
         * Trims excessive whitespace from the output bytes, particularly to remove double newline characters.
         */
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

        /**
         * Indents the YAML content based on the current indentation level. It uses a trick of
         * writing two spaces at a time to speed up the process.
         */
        protected void indent() {
            BytesUtil.combineDoubleNewline(bytes);
            for (int i = 0; i < indentation; i++) {
                bytes.writeUnsignedShort(' ' * 257);  // writes two spaces at once.
            }
        }

        /**
         * Determines and sets the appropriate separator for the current YAML element.
         * The separator varies based on the indentation level and whether the current value is a leaf node.
         */
        public void elementSeparator() {
            if (indentation == 0) {
                if (leaf) {
                    sep = COMMA_SPACE;
                } else {
                    sep = BytesStore.empty();
                    bytes.writeUnsignedByte('\n');  // starts a new line.
                }
            } else {
                sep = leaf ? COMMA_SPACE : COMMA_NEW_LINE;
            }
            BytesUtil.combineDoubleNewline(bytes); // ensure no double new lines.
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

        /**
         * Returns the string representation for null in YAML format.
         * It utilizes the predefined NULL static configuration for constructing the output.
         *
         * @return String representation for null in YAML.
         */
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

        /**
         * Determines if the provided BytesStore contains textual content.
         * This function checks each byte to ensure it represents a valid textual character.
         *
         * @param fromBytes The BytesStore object to inspect.
         * @return True if the content is textual, otherwise false.
         */
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
                return (T) nu11();
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
            try (final ScopedResource<StringBuilder> sbR = acquireStringBuilder()) {
                StringBuilder stringBuilder = sbR.get();
                stringBuilder.appendCodePoint(codepoint);
                text(stringBuilder);
            }
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

        /**
         * Adds a timestamp to the output in a predefined format.
         * The method appends the timestamp as a comment in YAML, depending on the range and precision
         * of the given timestamp value. The timestamp could be in milliseconds or with nanosecond precision.
         *
         * @param i64 The timestamp value to be appended.
         */
        public void addTimeStamp(long i64) {
            // Check if the timestamp is in a millisecond precision range (e.g., between 1e12 and 4.111e12)
            if ((long) 1e12 < i64 && i64 < (long) 4.111e12) {
                // Append the date and time in milliseconds as a comment in the output
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
            if (af == 0 || (af >= 1e-3 && af < 1e6))
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
            if (ad == 0) {
                bytes.append(d);
            } else if (ad >= 1e-7 && ad < 1e15) {
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

        /**
         * Converts the provided double value to its corresponding String representation.
         *
         * @param d The double value to convert.
         * @return The String representation of the provided double value.
         */
        protected String doubleToString(double d) {
            return Double.toString(d);
        }

        /**
         * Converts the provided float value to its corresponding String representation.
         *
         * @param f The float value to convert.
         * @return The String representation of the provided float value.
         */
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
            if (zonedDateTime == null)
                return (T) nu11();
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

        /**
         * Converts the provided object to its String representation and prepares it for wire output.
         * It handles null values and applies necessary formatting based on the needsQuotes method.
         * If the value is set to drop by default, the saved event name is written.
         *
         * @param stringable The object to convert to a string and process.
         * @return An instance of T, typically representing the current wire output state.
         */
        @NotNull
        private T asText(@Nullable Object stringable) {
            // Check if defaults should be dropped and handle accordingly
            if (dropDefault) {
                if (stringable == null)
                    return wireOut();
                writeSavedEventName();
            }

            // Handle null stringable objects
            if (stringable == null) {
                nu11();
            } else {
                // Add necessary separators
                prependSeparator();

                // Convert the object to its string representation
                final String s = stringable.toString();

                // Determine if the string needs quotes
                final Quotes quotes = needsQuotes(s);

                // Append the string to the wire output with necessary quotes
                asTestQuoted(s, quotes);

                // Add element separator after processing the string
                elementSeparator();
            }

            return wireOut();
        }

        /**
         * Appends the provided string to the wire output, with or without quotes based on the provided quote preference.
         * If the quote preference is NONE, the string is added directly to the wire output.
         * Otherwise, the string is escaped based on the provided quote preference.
         *
         * @param s      The string to append.
         * @param quotes The quote preference for the string.
         */
        protected void asTestQuoted(String s, Quotes quotes) {
            // Check if the string needs quotes
            if (quotes == Quotes.NONE) {
                append(s);
            } else {
                // Escape the string based on the provided quote preference
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

        /**
         * Starts a block with the given character, typically an opening bracket or brace.
         * Before writing the block starter, any necessary separators and whitespace are added.
         * The method also pushes the current state to remember the context.
         *
         * @param c The character that starts the block.
         */
        public void startBlock(char c) {
            // If defaults are to be dropped, save the event name
            if (dropDefault) {
                writeSavedEventName();
            }

            // If there's a separator defined, append it, trim whitespace and indent accordingly
            if (!sep.isEmpty()) {
                append(sep);
                trimWhiteSpace();
                indent();
                sep = EMPTY;
            }

            // Push the current state to remember it for later
            pushState();

            // Write the starting block character
            bytes.writeUnsignedByte(c);
        }

        @NotNull
        @Override
        public <E, K> T sequence(E e, K kls, @NotNull TriConsumer<E, K, ValueOut> writer) throws InvalidMarshallableException {
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

        /**
         * Ends a block with the given character, typically a closing bracket or brace.
         * Removes any double newlines to ensure a clean block closure.
         *
         * @param c The character that ends the block.
         */
        public void endBlock(char c) {
            BytesUtil.combineDoubleNewline(bytes);
            bytes.writeUnsignedByte(c);
        }

        /**
         * Adds a newline at the specified position if applicable.
         *
         * @param pos The position after which the newline should be added.
         */
        protected void addNewLine(long pos) {
            if (bytes.writePosition() > pos + 1)
                bytes.writeUnsignedByte('\n');
        }

        /**
         * Adds a space at the specified position if applicable.
         *
         * @param pos The position after which the space should be added.
         */
        protected void addSpace(long pos) {
            if (bytes.writePosition() > pos + 1)
                bytes.writeUnsignedByte(' ');
        }

        /**
         * Sets the separator to a new line for future content additions.
         */
        protected void newLine() {
            sep = NEW_LINE;
        }

        /**
         * Reverts the current state to the previous state by popping the last saved state.
         * This involves reverting the separator, decreasing the indentation, and resetting certain flags.
         */
        protected void popState() {
            sep = seps.remove(seps.size() - 1);
            indentation--;
            leaf = false;
            dropDefault = false;
        }

        /**
         * Pushes the current state, preserving the current context for later restoration.
         * This involves increasing the indentation and saving the current separator.
         */
        protected void pushState() {
            indentation++;
            seps.add(sep);
            sep = EMPTY;
        }

        @NotNull
        @Override
        public T marshallable(@NotNull WriteMarshallable object) throws InvalidMarshallableException {
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
        public T marshallable(@NotNull Serializable object) throws InvalidMarshallableException {
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

        /**
         * Writes the provided serializable object. If the object is also externalizable,
         * it writes using its external method, otherwise, it writes using the Wires class method.
         *
         * @param object The serializable object to be written.
         * @throws InvalidMarshallableException If the object cannot be serialized.
         */
        private void writeSerializable(@NotNull Serializable object) throws InvalidMarshallableException {
            try {
                if (object instanceof Externalizable)
                    // Use the externalizable's own method for writing if applicable
                    ((Externalizable) object).writeExternal(objectOutput());
                else // Use the default Wires method to serialize the object
                    Wires.writeMarshallable(object, wireOut());
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }

        /**
         * Performs actions after closing an element.
         * Sets the separator to a newline and appends the current separator to the bytes.
         */
        protected void afterClose() {
            newLine();        // Set the separator to a newline
            append(sep);      // Append the current separator
            sep = EMPTY;      // Reset the separator
        }

        /**
         * Performs actions after opening an element.
         * Sets the separator to a space.
         */
        protected void afterOpen() {
            sep = SPACE;      // Set the separator to a space
        }

        @NotNull
        @Override
        public T map(@NotNull final Map map) throws InvalidMarshallableException {
            if (dropDefault) {
                writeSavedEventName();
            }
            marshallable(map, Object.class, Object.class, false);
            return wireOut();
        }

        /**
         * Sets the separator to denote the end of a field.
         */
        protected void endField() {
            sep = END_FIELD;
        }

        /**
         * Writes a field-value separator, which is ": ".
         */
        protected void fieldValueSeperator() {
            writeTwo(':', ' ');
        }

        /**
         * Writes an empty value to the Yaml output. If the default value is dropped,
         * the event name is set to an empty string.
         *
         * @return Returns an instance of the current object, supporting chained method calls.
         */
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

        /**
         * Writes a given WireKey to the Yaml output. If the default value is dropped,
         * the event name is set to the name of the key.
         *
         * @param key The WireKey to write.
         * @return Returns an instance of the current object, supporting chained method calls.
         */
        @NotNull
        public YamlValueOut write(@NotNull WireKey key) {
            if (dropDefault) {
                eventName = key.name().toString();
            } else {
                write(key.name());
            }
            return this;
        }

        /**
         * Writes a given CharSequence name to the Yaml output. If the default value is dropped,
         * the event name is set to the given name.
         *
         * @param name The CharSequence name to write.
         * @return Returns an instance of the current object, supporting chained method calls.
         */
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

        /**
         * Writes a given objectKey of an expected type to the Yaml output. If the default value is dropped,
         * and the expected type is not a String, an exception is thrown. Otherwise, the event name is set
         * to the string representation of the objectKey.
         *
         * @param expectedType The expected type of the objectKey.
         * @param objectKey    The object key to write.
         * @return Returns an instance of the current object, supporting chained method calls.
         * @throws InvalidMarshallableException If the object cannot be serialized.
         */
        @NotNull
        public YamlValueOut write(Class expectedType, @NotNull Object objectKey) throws InvalidMarshallableException {
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

        /**
         * Writes the saved event name to the output. This method escapes the event name
         * and separates it from its value.
         */
        private void writeSavedEventName() {
            if (eventName == null)
                return;
            prependSeparator();
            escape(eventName);
            fieldValueSeperator();
            eventName = null;
        }

        /**
         * Ends the current event by checking and adjusting the position
         * of the last byte written to the output, and appending a field value separator.
         */
        public void endEvent() {
            // Check if the last written byte is a whitespace character or less
            if (bytes.readByte(bytes.writePosition() - 1) <= ' ')
                bytes.writeSkip(-1);  // Skip the last byte if it's a whitespace

            fieldValueSeperator();   // Add field value separator
            sep = empty();           // Reset the separator
        }

        /**
         * Writes a comment to the Yaml output. If a comment annotation exists,
         * specific formatting is applied. Otherwise, a standard comment format is used.
         *
         * @param s The comment text to write.
         */
        public void writeComment(@NotNull CharSequence s) {

            if (hasCommentAnnotation) {
                // Ensure the separator ends with a newline character
                if (!sep.endsWith('\n'))
                    return;

                sep = COMMA_SPACE;    // Update the separator
            } else {
                prependSeparator();   // Add separator before the comment
            }

            append(sep);  // Add the separator to the output

            // Add extra indentation for comments with annotations
            if (hasCommentAnnotation)
                writeTwo('\t', '\t');

            writeTwo('#', ' ');

            append(s);
            bytes.writeUnsignedByte('\n');
            sep = EMPTY_AFTER_COMMENT;
        }
    }
}
