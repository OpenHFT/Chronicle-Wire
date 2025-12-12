/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Text based wire format for Comma Separated Values (CSV).
 *
 * <p>Suitable for simple tabular data where each line represents a record and fields
 * are delimited by commas. The first line is treated as a header row and defines the
 * column names for subsequent rows. Nested or complex structures are not fully
 * represented and may need to be flattened before writing to CSV.
 */
public class CSVWire extends TextWire {

    /**
     * Thread local tester used when parsing CSV fields to honour escaping rules
     * for commas within quoted text.
     */
    private static final ThreadLocal<StopCharTester> ESCAPED_END_OF_TEXT = ThreadLocal.withInitial(
            StopCharTesters.COMMA_STOP::escaping);

    /**
     * Field names parsed from the first line of the input. These are used to
     * map subsequent column values when reading records.
     */
    private final List<String> header = new ArrayList<>();

    /**
     * Creates a wire backed by the supplied bytes and parses the first line as
     * the header row.
     *
     * @param bytes   underlying bytes containing CSV text
     * @param use8bit {@code true} to read 8-bit characters, otherwise UTF-8
     */
    @SuppressWarnings("rawtypes")
    public CSVWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        super(bytes, use8bit);
        while (lineStart == 0) {
            long start = bytes.readPosition();
            header.add(valueIn.text());
            if (bytes.readPosition() == start)
                break;
        }
    }

    /**
     * Creates a wire from the given bytes using UTF-8 encoding.
     *
     * @param bytes underlying CSV data
     */
    @SuppressWarnings("rawtypes")
    public CSVWire(@NotNull Bytes<?> bytes) {
        this(bytes, false);
    }

    /**
     * Builds a {@code CSVWire} by loading the contents of the given file.
     * Uses 8-bit parsing.
     *
     * @param name path of the CSV file
     * @return new instance containing the file content
     * @throws IOException if the file cannot be read
     */
    @NotNull
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    public static CSVWire fromFile(String name) throws IOException {
        return new CSVWire(BytesUtil.readFile(name), true);
    }

    /**
     * Creates a wire from the supplied text.
     *
     * @param text CSV data in string form
     * @return new instance containing the text
     */
    @NotNull
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    public static CSVWire from(@NotNull String text) {
        return new CSVWire(Bytes.from(text));
    }

    /**
     * Returns a thread local tester configured for CSV fields and resets its
     * state before use.
     */
    @NotNull
    static StopCharTester getEscapingCSVEndOfText() {
        StopCharTester escaping = ESCAPED_END_OF_TEXT.get();
        // reset the tester.
        escaping.isStopChar(' ');
        return escaping;
    }

    /**
     * Factory for {@link CSVValueOut} instances used when writing values.
     */
    @NotNull
    @Override
    protected CSVValueOut createValueOut() {
        return new CSVValueOut();
    }

    /**
     * Factory for {@link CSVValueIn} instances used when reading values.
     */
    @NotNull
    @Override
    protected TextValueIn createValueIn() {
        return new CSVValueIn();
    }

    /**
     * Reads the next column value into {@code sb} assuming the wire is
     * positioned at the start of a field.
     */
    @Override
    @NotNull
    public StringBuilder readField(@NotNull StringBuilder sb) {
        valueIn.text(sb);
        return sb;
    }

    /**
     * Consumes leading spaces and comment lines (prefixed with {@code #}) so
     * the reader is positioned at the start of the header or first record.
     */
    public void consumePaddingStart() {
        for (; ; ) {
            int codePoint = peekCode();
            // Checks if the code point represents a comment.
            if (codePoint == '#') {
                // If so, skip characters until the end of the line.
                while (readCode() >= ' ') ;
                continue;
            }
            if (Character.isWhitespace(codePoint)) {
                // Handle newline or carriage return; set lineStart to the next position.
                if (codePoint == '\n' || codePoint == '\r')
                    this.lineStart = bytes.readPosition() + 1;
                // Skips the current whitespace character.
                bytes.readSkip(1);
            } else {
                // If the code point is neither a comment nor whitespace, exit the loop.
                break;
            }
        }
    }

    /**
     * Skips whitespace at the current position. Newlines and commas are not
     * consumed here.
     */
    @Override
    public void consumePadding() {
        for (; ; ) {
            int codePoint = peekCode();
            if (Character.isWhitespace(codePoint) && codePoint >= ' ') {
                bytes.readSkip(1);
            } else {
                break;
            }
        }
    }

    /**
     * In CSV the {@code commas} argument is ignored; this method delegates to
     * {@link #consumePadding()}.
     */
    @Override
    public void consumePadding(int commas) {
        consumePadding();
    }

    /**
     * Keys are not used when reading CSV so this simply returns {@link #valueIn}.
     */
    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return valueIn;
    }

    /**
     * Reads the next header name into {@code name} and returns the
     * corresponding {@link ValueIn} for the value.
     */
    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        consumePadding();
        readField(name);
        return valueIn;
    }

    /**
     * Clears {@code s} as inline comments are not expected in CSV.
     */
    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        s.setLength(0);
        return this;
    }

    /**
     * Represents the value output functionality specific to the CSV format.
     * It extends the YamlValueOut class to handle specific behaviors associated with
     * writing values in CSV. This includes certain restrictions, such as not supporting
     * type literals and serializable objects in CSV format.
     */
    class CSVValueOut extends YamlValueOut {
        /**
         * CSV has no concept of type literals.
         *
         * @throws UnsupportedOperationException always
         */
        @NotNull
        @Override
        public CSVWire typeLiteral(@Nullable CharSequence type) {
            if (type == null)
                return (CSVWire) nu11();
            throw new UnsupportedOperationException("Type literals not supported in CSV, cannot write " + type);
        }

        /**
         * Writing arbitrary serializable objects is not supported in CSV.
         *
         * @throws UnsupportedOperationException always
         */
        @NotNull
        @Override
        public CSVWire marshallable(@NotNull Serializable object) {
            throw new UnsupportedOperationException("Serializable objects not supported in CSV, cannot write " + object);
        }
    }

    /**
     * Represents the value input functionality specific to the CSV format.
     * It extends the TextValueIn class to handle specific behaviors associated with
     * reading values from CSV. This includes handling CSV specific escape sequences and delimiters.
     */
    class CSVValueIn extends TextValueIn {

        /**
         * Determines whether more data are available after skipping leading
         * padding and comments.
         */
        @Override
        public boolean hasNext() {
            consumePaddingStart();
            return bytes.readRemaining() > 0;
        }

        /**
         * Reads a CSV field into {@code a}, handling quoted values and escaping.
         */
        @Override
        @Nullable <T extends Appendable & CharSequence> T textTo0(@NotNull T a) {
            consumePadding();
            int ch = peekCode();

            switch (ch) {
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
                    code = peekCode();
                    if (code == ',')
                        readCode();
                    break;

                }
                case '\'': {
                    bytes.readSkip(1);
                    if (use8bit)
                        bytes.parse8bit(a, TextWire.getEscapingSingleQuotes());
                    else
                        bytes.parseUtf8(a, TextWire.getEscapingSingleQuotes());
                    unescape(a);
                    int code = peekCode();
                    if (code == '\'')
                        readCode();
                    break;

                }
                default: {
                    if (bytes.readRemaining() > 0) {
                        if (a instanceof Bytes || use8bit)
                            bytes.parse8bit(a, getEscapingCSVEndOfText());
                        else
                            bytes.parseUtf8(a, getEscapingCSVEndOfText());

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
            if (END_CHARS.get(prev))
                bytes.readSkip(-1);
            return a;
        }

        /**
         * Calculates the remaining characters for the current record by scanning
         * until the next line ending.
         */
        @Override
        protected long readLengthMarshallable() {
            long start = bytes.readPosition();
            try {
                consumePadding();
                for (; ; ) {
                    int code = readCode();
                    switch (code) {
                        case '\r':
                        case '\n':
                        case 0:
                        case -1:
                            return bytes.readPosition() - start - 1;
                        default:
                            // Continue scanning until an end-of-line marker is found
                            break;
                    }
                }
            } finally {
                bytes.readPosition(start);
            }
        }

        /**
         * Returns {@code true} if another field is present on the current line and
         * consumes a trailing comma.
         */
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

        /**
         * Reads a marshallable object from the current record.
         */
        @Override
        public boolean marshallable(@NotNull ReadMarshallable object) throws InvalidMarshallableException {
            if (isNull())
                return false;
            pushState();
            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            final long newLimit = position + len;
            try {
                // ensure that you can read past the end of this marshable object

                bytes.readLimit(newLimit);
                consumePadding();
                object.readMarshallable(CSVWire.this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
                popState();
            }

            consumePadding();
            return true;
        }
    }
}
