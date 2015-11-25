/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.BooleanConsumer;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static net.openhft.chronicle.bytes.Bytes.empty;
import static net.openhft.chronicle.core.util.ReadResolvable.readResolve;

/**
 * YAML Based wire format
 */
public class CSVWire extends TextWire {

    static final ThreadLocal<StopCharTester> ESCAPED_END_OF_TEXT = ThreadLocal.withInitial(() -> StopCharTesters.COMMA_STOP.escaping());

    private final List<String> header = new ArrayList<>();

    public CSVWire(Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
        while (lineStart == 0)
            header.add(valueIn.text());
    }

    public CSVWire(Bytes bytes) {
        this(bytes, false);
    }

    public static CSVWire fromFile(String name) throws IOException {
        return new CSVWire(Bytes.wrapForRead(IOTools.readFile(name)), true);
    }

    @NotNull
    public static CSVWire from(@NotNull String text) {
        return new CSVWire(Bytes.from(text));
    }

    @NotNull
    @Override
    protected TextValueOut createValueOut() {
        return new CSVValueOut();
    }

    @NotNull
    @Override
    protected TextValueIn createValueIn() {
        return new CSVValueIn();
    }

    @NotNull
    public StringBuilder readField(@NotNull StringBuilder sb) {
        valueIn.text(sb);
        return sb;
    }

    @NotNull
    private StopCharTester getEscapingEndOfText() {
        StopCharTester escaping = ESCAPED_END_OF_TEXT.get();
        // reset it.
        escaping.isStopChar(' ');
        return escaping;
    }

    private StopCharTester getEscapingQuotes() {
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

    void consumeWhiteSpace() {
        for (; ; ) {
            int codePoint = peekCode();
            if (Character.isWhitespace(codePoint) || codePoint == ',') {
                if (codePoint == '\n' || codePoint == '\r')
                    this.lineStart = bytes.readPosition() + 1;
                bytes.readSkip(1);
            } else {
                break;
            }
        }
    }

    void consumeDocumentStart() {
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
    private boolean peekStringIgnoreCase(@NotNull final String source) {
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

    private int readCode() {
        if (bytes.readRemaining() < 1)
            return -1;
        return bytes.readUnsignedByte();
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        consumeWhiteSpace();
        readField(name);
        return valueIn;
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        s.setLength(0);
        return this;
    }

    @Override
    public void clear() {
        bytes.clear();
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    @Override
    public boolean hasMore() {
        consumeWhiteSpace();

        return bytes.readRemaining() > 0;
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        for (int i = 0; i < paddingToAdd; i++)
            bytes.appendUtf8((bytes.writePosition() & 63) == 0 ? '\n' : ' ');
        return this;
    }

    void escape(@NotNull CharSequence s) {
        net.openhft.chronicle.wire.Quotes quotes = needsQuotes(s);
        if (quotes == net.openhft.chronicle.wire.Quotes.NONE) {
            escape0(s, quotes);
            return;
        }
        bytes.appendUtf8(quotes.q);
        escape0(s, quotes);
        bytes.appendUtf8(quotes.q);
    }

    private void escape0(@NotNull CharSequence s, net.openhft.chronicle.wire.Quotes quotes) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    if (ch == quotes.q) {
                        bytes.appendUtf8('\\').appendUtf8(ch);
                    } else {
                        bytes.appendUtf8(ch);
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
                default:
                    if (ch > 127) {
                        bytes.appendUtf8("\\u");
                        bytes.appendUtf8(HEX[(ch >> 12) & 0xF]);
                        bytes.appendUtf8(HEX[(ch >> 8) & 0xF]);
                        bytes.appendUtf8(HEX[(ch >> 4) & 0xF]);
                        bytes.appendUtf8(HEX[ch & 0xF]);
                    } else {
                        bytes.appendUtf8(ch);
                    }
                    break;
            }
        }
    }

    protected Quotes needsQuotes(@NotNull CharSequence s) {
        net.openhft.chronicle.wire.Quotes quotes = net.openhft.chronicle.wire.Quotes.NONE;
        if (s.length() == 0)
            return net.openhft.chronicle.wire.Quotes.DOUBLE;

        if (STARTS_QUOTE_CHARS.get(s.charAt(0)))
            return net.openhft.chronicle.wire.Quotes.DOUBLE;
        for (int i = 1; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (QUOTE_CHARS.get(ch))
                return net.openhft.chronicle.wire.Quotes.DOUBLE;
            if (ch == '"')
                quotes = net.openhft.chronicle.wire.Quotes.SINGLE;
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
            bytes.parseUTF(sb, testers);
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

    public void append(CharSequence cs, int offset) {
        if (use8bit)
            bytes.append8bit(cs, offset, offset + cs.length());
        else
            bytes.appendUtf8(cs, offset, cs.length());
    }

    public Object readObject() {
        consumeWhiteSpace();
        consumeDocumentStart();
        return readObject(0);
    }

    Object readObject(int indentation) {
        consumeWhiteSpace();
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
            consumeWhiteSpace();
            if (lineStart == ls) {
                objects.add(valueIn.objectWithInferredType(Object.class));
            } else {
                Object e = readObject(indentation);
                if (e != NoObject.NO_OBJECT)
                    objects.add(e);
            }
            consumeWhiteSpace();
        }

        return objects;
    }

    Map readMap() {
        bytes.readSkip(1);
        Map map = new LinkedHashMap<>();
        StringBuilder sb = WireInternal.acquireAnotherStringBuilder(WireInternal.acquireStringBuilder());
        while (bytes.readRemaining() > 0) {
            consumeWhiteSpace();
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
        StringBuilder sb = WireInternal.acquireAnotherStringBuilder(WireInternal.acquireStringBuilder());
        while (bytes.readRemaining() > 0) {
            consumeWhiteSpace();
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
        bytes.appendUtf8('-');
        bytes.appendUtf8(' ');
        indentation(indentation - 2);
        valueOut.object(o);
    }

    private void indentation(int indentation) {
        while (indentation-- > 0)
            bytes.appendUtf8(' ');
    }

    enum NoObject {NO_OBJECT}

    class CSVValueOut extends TextValueOut {
        int indentation = 0;
        @NotNull
        BytesStore sep = Bytes.empty();
        boolean leaf = false;

        void prependSeparator() {
            append(sep);
            if (sep.endsWith('\n'))
                indent();
            sep = Bytes.empty();
        }

        @NotNull
        @Override
        public ValueOut leaf() {
            leaf = true;
            return this;
        }

        @NotNull
        @Override
        public WireOut wireOut() {
            return CSVWire.this;
        }

        private void indent() {
            for (int i = 0; i < indentation; i++) {
                bytes.appendUtf8(' ');
                bytes.appendUtf8(' ');
            }
        }

        public void elementSeparator() {
            if (indentation == 0) {
                sep = Bytes.empty();
                bytes.appendUtf8('\n');

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
            return CSVWire.this;
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
            return CSVWire.this;
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            prependSeparator();
            bytes.appendUtf8(i8);
            elementSeparator();
            return CSVWire.this;
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
            return CSVWire.this;
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

        private void popState() {
            indentation--;
            leaf = false;
        }

        private void pushState() {
            indentation++;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) {
            if (!leaf)
                pushState();

            prependSeparator();
            bytes.appendUtf8('{');
            sep = leaf ? SPACE : END_FIELD;

            object.writeMarshallable(CSVWire.this);

            if (!leaf)
                popState();
            else
                leaf = false;
            if (sep.startsWith(','))
                append(sep, 1);
            else
                prependSeparator();
            bytes.appendUtf8('}');

            if (indentation == 0) {
                sep = empty();
                append(NEW_LINE);

            } else {
                sep = COMMA_NEW_LINE;
            }
            return CSVWire.this;
        }

        @NotNull
        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            typePrefix(SEQ_MAP);
            map.forEach((k, v) -> sequence(w -> w.marshallable(m -> m
                    .write(() -> "key").typedMarshallable(k)
                    .write(() -> "value").typedMarshallable(v))));
            return CSVWire.this;
        }
    }

    class CSVValueIn extends TextValueIn {
        @Override
        public String text() {
            return StringUtils.toString(textTo(WireInternal.acquireStringBuilder()));
        }

        @Nullable
        @Override
        public StringBuilder textTo(@NotNull StringBuilder sb) {
            return textTo0(sb);
        }

        @Nullable
        @Override
        public Bytes textTo(@NotNull Bytes bytes) {
            return textTo0(bytes);
        }

        @Nullable
        <ACS extends Appendable & CharSequence> ACS textTo0(@NotNull ACS a) {
            consumeWhiteSpace();
            int ch = peekCode();

            switch (ch) {
                case '{': {
                    final long len = readLength();
                    AppendableUtil.append(a, bytes, bytes.readPosition(), len);
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
                        bytes.parseUTF(a, getEscapingQuotes());
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
                        bytes.parseUTF(a, getEscapingSingleQuotes());
                    unescape(a);
                    int code = peekCode();
                    if (code == '\'')
                        readCode();
                    break;

                }
                case '!': {
                    bytes.readSkip(1);
                    ch = peekCode();
                    if (ch == '!') {
                        bytes.readSkip(1);
                        StringBuilder sb = WireInternal.acquireStringBuilder();
                        parseWord(sb);
                        if (StringUtils.isEqual(sb, "null")) {
                            textTo(sb);
                            return null;
                        } else if (StringUtils.isEqual(sb, "snappy")) {
                            textTo(sb);
                            try {
                                //todo needs to be made efficient
                                byte[] decodedBytes = Base64.getDecoder().decode(sb.toString().getBytes());
                                String csq = Snappy.uncompressString(decodedBytes);
                                return (ACS) WireInternal.acquireStringBuilder().append(csq);
                            } catch (IOException e) {
                                throw new AssertionError(e);
                            }
                        }
                    } else {
                        StringBuilder sb = WireInternal.acquireStringBuilder();
                        textTo(sb);
                        // ignore the type.
                    }
                    break;
                }
                default: {
                    if (bytes.readRemaining() > 0) {
                        if (a instanceof Bytes || use8bit)
                            bytes.parse8bit(a, getEscapingEndOfText());
                        else
                            bytes.parseUTF(a, getEscapingEndOfText());

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
            return a;
        }

        private int peekBack() {
            while (bytes.readPosition() >= bytes.start()) {
                int prev = bytes.readUnsignedByte(bytes.readPosition() - 1);
                if (prev != ' ') {
                    if (prev == '\n' || prev == '\r') {
                        CSVWire.this.lineStart = bytes.readPosition();
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
            return bytes(wi -> toBytes.write(wi.bytes()));
        }

        @NotNull
        public WireIn bytes(@NotNull ReadMarshallable bytesConsumer) {
            consumeWhiteSpace();

            // TODO needs to be made much more efficient.
            StringBuilder sb = WireInternal.acquireStringBuilder();
            if (peekCode() == '!') {
                parseWord(sb);
                String str = WireInternal.INTERNER.intern(sb);
                if (str.equals("!!binary")) {
                    AppendableUtil.setLength(sb, 0);
                    parseWord(sb);
                    byte[] decode = Base64.getDecoder().decode(sb.toString());
                    bytesConsumer.readMarshallable(new CSVWire(Bytes.wrapForRead(decode)));

                } else if (str.equals("!!null")) {
                    bytesConsumer.readMarshallable(null);
                    parseWord(sb);
                } else {
                    throw new IORuntimeException("Unsupported type=" + str);
                }
            } else {
                textTo(sb);
                bytesConsumer.readMarshallable(new CSVWire(Bytes.wrapForRead(sb.toString().getBytes())));
            }
            return CSVWire.this;
        }

        @Nullable
        public byte[] bytes() {
            consumeWhiteSpace();
            // TODO needs to be made much more efficient.
            StringBuilder sb = WireInternal.acquireStringBuilder();
            if (peekCode() == '!') {
                parseWord(sb);
                String str = WireInternal.INTERNER.intern(sb);
                if (str.equals("!!binary")) {
                    AppendableUtil.setLength(sb, 0);
                    parseWord(sb);
                    byte[] decode = Base64.getDecoder().decode(WireInternal.INTERNER.intern(sb));
                    return decode;

                } else if (str.equals("!!null")) {
                    parseWord(sb);
                    return null;
                } else if (str.equals("!" + SEQ_MAP)) {
                    sb.append(bytes);
                    // todo fix this.
                    return WireInternal.INTERNER.intern(sb).getBytes();

                } else {
                    throw new IllegalStateException("unsupported type=" + str);
                }

            } else {
                textTo(sb);
                // todo fix this.
                return sb.toString().getBytes();
            }
        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return CSVWire.this;
        }

        @Override
        public long readLength() {
            consumeWhiteSpace();
            long start = bytes.readPosition();
            try {
                consumeWhiteSpace();
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

        private long readLengthMarshallable() {
            long start = bytes.readPosition();
            try {
                consumeWhiteSpace();
                for (; ; ) {
                    int code = readCode();
                    switch (code) {
                        case '\r':
                        case '\n':
                        case 0:
                        case -1:
                            return bytes.readPosition() - start - 1;
                    }
                }
            } finally {
                bytes.readPosition(start);
            }
        }

        @Override
        public boolean hasNext() {
            return bytes.readRemaining() > 0;
        }

        @Override
        public boolean hasNextSequenceItem() {
            consumeWhiteSpace();
            int ch = peekCode();
            if (ch == ',') {
                bytes.readSkip(1);
                return true;
            }
            return ch > 0 && ch != ']';
        }

        @NotNull
        @Override
        public WireIn int64(@Nullable LongValue value) {
            consumeWhiteSpace();
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumeWhiteSpace();
            if (peekCode() == ',')
                bytes.readSkip(1);
            return CSVWire.this;
        }

        @Override
        public <T> T applyToMarshallable(@NotNull Function<WireIn, T> marshallableReader) {
            consumeWhiteSpace();

            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            try {
                // ensure that you can read past the end of this marshable object
                final long newLimit = position - 1 + len;
                bytes.readLimit(newLimit);
                bytes.readSkip(1); // skip the {
                consumeWhiteSpace();
                return marshallableReader.apply(CSVWire.this);
            } finally {
                bytes.readLimit(limit);

                consumeWhiteSpace();
            }
        }

        @NotNull
        String stringForCode(int code) {
            return code < 0 ? "Unexpected end of input" : "'" + (char) code + "'";
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) {
            consumeWhiteSpace();
            final long len = readLengthMarshallable();

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            final long newLimit = position + len;
            try {
                // ensure that you can read past the end of this marshable object

                bytes.readLimit(newLimit);
                consumeWhiteSpace();
                object.readMarshallable(CSVWire.this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
            }

            consumeWhiteSpace();
            return CSVWire.this;
        }

        @Nullable
        public <T extends ReadMarshallable> T typedMarshallable() {
            try {
                consumeWhiteSpace();
                int code = peekCode();
                if (code < 0)
                    throw new IllegalStateException("Cannot read nothing as a ReadMarshallable " + bytes.toDebugString());
                StringBuilder sb = WireInternal.acquireStringBuilder();
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
                final Class clazz = ClassAliasPool.CLASS_ALIASES.forName(sb);

                if (!ReadMarshallable.class.isAssignableFrom(clazz))
                    throw new ClassCastException("Cannot convert " + sb + " to ReadMarshallable.");

                Class<ReadMarshallable> clazz1 = (Class<ReadMarshallable>) clazz;
                final ReadMarshallable m = ObjectUtils.newInstance(clazz1);

                marshallable(m);
                return readResolve(m);
            } catch (Exception e) {
                throw new IORuntimeException(e);
            }
        }

        @Nullable
        @Override
        public <K, V> Map<K, V> map(@NotNull final Class<K> kClazz,
                                    @NotNull final Class<V> vClass,
                                    @NotNull final Map<K, V> usingMap) {
            consumeWhiteSpace();
            usingMap.clear();

            StringBuilder sb = WireInternal.acquireStringBuilder();
            if (peekCode() == '!') {
                parseUntil(sb, StopCharTesters.SPACE_STOP);
                String str = WireInternal.INTERNER.intern(sb);

                if (("!!null").contentEquals(sb)) {
                    text();
                    return null;

                } else if (("!" + SEQ_MAP).contentEquals(sb)) {
                    consumeWhiteSpace();
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
            return usingMap;
        }

        @Override
        public <K extends ReadMarshallable, V extends ReadMarshallable> void typedMap(@NotNull Map<K, V> usingMap) {
            consumeWhiteSpace();
            usingMap.clear();

            StringBuilder sb = WireInternal.acquireStringBuilder();
            if (peekCode() == '!') {
                parseUntil(sb, StopCharTesters.SPACE_STOP);
                String str = WireInternal.INTERNER.intern(sb);
                if (SEQ_MAP.contentEquals(sb)) {
                    while (hasNext()) {
                        sequence(usingMap, (map, s) -> s.marshallable(r -> {
                            try {
                                @SuppressWarnings("unchecked")
                                final K k = r.read(() -> "key").typedMarshallable();
                                @SuppressWarnings("unchecked")
                                final V v = r.read(() -> "value").typedMarshallable();
                                map.put(k, v);
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
            consumeWhiteSpace();
            StringBuilder sb = WireInternal.acquireStringBuilder();
            if (textTo(sb) == null)
                throw new NullPointerException("value is null");

            return StringUtils.isEqual(sb, "true");
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
            consumeWhiteSpace();
            return bytes.parseLong();
        }

        @Override
        public double float64() {
            consumeWhiteSpace();
            return bytes.parseDouble();
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
            consumeWhiteSpace();

            if (peekStringIgnoreCase("!!null \"\"")) {
                bytes.readSkip("!!null \"\"".length());
                // discard the text after it.
                //  text(WireInternal.acquireStringBuilder());
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
        Object object0(@Nullable Object using, @NotNull Class clazz) {
            consumeWhiteSpace();

            if (isNull())
                return null;

            if (byte[].class.isAssignableFrom(clazz))
                return bytes();

            if (BytesStore.class.isAssignableFrom(clazz)) {
                Bytes<byte[]> bytes = Bytes.wrapForRead(bytes());
                return bytes;
            }

            if (ReadMarshallable.class.isAssignableFrom(clazz)) {
                final Object v;
                if (using == null)
                    v = ObjectUtils.newInstance(clazz);
                else
                    v = using;

                valueIn.marshallable((ReadMarshallable) v);
                return readResolve(v);

            } else if (StringBuilder.class.isAssignableFrom(clazz)) {
                StringBuilder builder = (using == null)
                        ? WireInternal.acquireStringBuilder()
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

        private Object typedObject() {
            readCode();
            StringBuilder sb = WireInternal.acquireStringBuilder();
            parseUntil(sb, TextStopCharTesters.END_OF_TYPE);
            final Class clazz2;
            try {
                clazz2 = ClassAliasPool.CLASS_ALIASES.forName(sb);
            } catch (ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
            return object(null, clazz2);
        }

        @Override
        public String toString() {
            return CSVWire.this.toString();
        }
    }

}
