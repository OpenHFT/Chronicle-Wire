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
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.wire.util.BooleanConsumer;
import net.openhft.chronicle.wire.util.ByteConsumer;
import net.openhft.chronicle.wire.util.FloatConsumer;
import net.openhft.chronicle.wire.util.ShortConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.*;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static net.openhft.chronicle.core.util.ReadResolvable.readResolve;

/**
 * Created by peter.lawrey on 15/01/15.
 */
public class TextWire implements Wire, InternalWireIn {

    public static final String FIELD_SEP = "";
    public static final String SEQ_MAP = "!seqmap";
    public static final String NULL = "!null \"\"";
    static final BitSet QUOTE_CHARS = new BitSet();
    private static final Logger LOG =
            LoggerFactory.getLogger(TextWire.class);
    private static final String END_FIELD = "\n";

    static {
        for (char ch : "\",\n\\#:{}[]".toCharArray())
            QUOTE_CHARS.set(ch);
    }

    final Bytes<?> bytes;
    final TextValueOut valueOut = new TextValueOut();
    final ValueIn valueIn = new TextValueIn();
    boolean ready;

    public TextWire(Bytes bytes) {
        this.bytes = bytes;
    }

    public static TextWire from(String text) {
        return new TextWire(Bytes.from(text));
    }

    public static String asText(@NotNull Wire wire) {
        long pos = wire.bytes().readPosition();
        TextWire tw = new TextWire(nativeBytes());
        wire.copyTo(tw);
        wire.bytes().readPosition(pos);

        return tw.toString();
    }

    public static <ACS extends Appendable & CharSequence> void unescape(@NotNull ACS sb) {
        int end = 0;
        for (int i = 0; i < sb.length(); i++) {
            char ch = sb.charAt(i);
            if (ch == '\\' && i < sb.length() - 1) {
                char ch3 = sb.charAt(++i);
                switch (ch3) {
                    case 'n':
                        ch = '\n';
                        break;
                }
            }
            BytesUtil.setCharAt(sb, end++, ch);
        }
        BytesUtil.setLength(sb, end);
    }

    public String toString() {
        return bytes.toString();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ValueIn read() {
        readField(Wires.acquireStringBuilder());
        return valueIn;
    }

    @NotNull
    private StringBuilder readField(@NotNull StringBuilder sb) {
        consumeWhiteSpace();
        if (peekCode() == ',') {
            bytes.readSkip(1);
            consumeWhiteSpace();
        }
        try {
            int ch = peekCode();
            if (ch == '"') {
                bytes.readSkip(1);
                bytes.parseUTF(sb, StopCharTesters.QUOTES.escaping());

                consumeWhiteSpace();
                ch = readCode();
                if (ch != ':')
                    throw new UnsupportedOperationException("Expected a : at " + bytes.toDebugString());

            } else if (ch < 0) {
                sb.setLength(0);
                return sb;

            } else {
                bytes.parseUTF(sb, TextStopCharsTesters.END_OF_TEXT.escaping());
            }
            unescape(sb);
        } catch (BufferUnderflowException ignored) {
        }
        //      consumeWhiteSpace();
        return sb;
    }

    void consumeWhiteSpace() {
        for (; ; ) {
            int codePoint = peekCode();
            if (codePoint == '#') {
                //noinspection StatementWithEmptyBody
                while (readCode() >= ' ') ;
            } else if (Character.isWhitespace(codePoint) || codePoint == ',') {
                bytes.readSkip(1);
            } else {
                break;
            }
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
        long position = bytes.readPosition();
        StringBuilder sb = readField(Wires.acquireStringBuilder());
        if (sb.length() == 0 || StringUtils.isEqual(sb, key.name()))
            return valueIn;
        bytes.readPosition(position);
        throw new UnsupportedOperationException("Unordered fields not supported yet. key=" + key
                .name() + ", was=" + sb + ", data='" + sb + "'");
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
    public ValueIn getValueIn() {
        return valueIn;
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        throw new UnsupportedOperationException();
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
    public ValueOut writeValue() {
        return valueOut;
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
            bytes.append((bytes.writePosition() & 63) == 0 ? '\n' : ' ');
        return this;
    }

    CharSequence quotes(@NotNull CharSequence s) {
        if (!needsQuotes(s)) {
            return s;
        }
        StringBuilder sb2 = Wires.acquireAnotherStringBuilder(s);
        sb2.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                case '\\':
                    sb2.append('\\').append(ch);
                    break;

                case '\n':
                    sb2.append("\\n");
                    break;
                default:
                    sb2.append(ch);
                    break;
            }
        }
        sb2.append('"');
        return sb2;
    }

    boolean needsQuotes(@NotNull CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (QUOTE_CHARS.get(ch))
                return true;
        }
        return s.length() == 0;
    }

    @Override
    public LongValue newLongReference() {
        return new TextLongReference();
    }

    enum TextStopCharsTesters implements StopCharsTester {
        END_OF_TEXT {
            @Override
            public boolean isStopChar(int ch, int ch2) throws IllegalStateException {
                // one character stop.
                if (ch == '"' || ch == '#' || ch == '\n') return true;
                // two character stop.
                return (ch == ':' || ch == ',') && ch2 <= ' ';
            }
        },
    }

    enum TextStopCharTesters implements StopCharTester {
        END_OF_TYPE {
            @Override
            public boolean isStopChar(int ch) throws IllegalStateException {
                return ch == '"' || ch == '#' || ch == '\n' || ch == ':' || ch == ',' || ch == ' ';
            }
        }
    }

    class TextValueOut implements ValueOut {
        int indentation = 0;
        @NotNull
        String sep = "";
        boolean leaf = false;

        void prependSeparator() {
            bytes.append(sep);
            if (sep.endsWith("\n"))
                indent();
            sep = "";
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
            return TextWire.this;
        }

        private void indent() {
            for (int i = 0; i < indentation; i++)
                bytes.append("  ");
        }

        public void elementSeparator() {
            if (indentation == 0) {
                sep = "";
                bytes.append("\n");

            } else {
                sep = leaf ? ", " : ",\n";
            }
        }

        @NotNull
        @Override
        public WireOut bool(@Nullable Boolean flag) {
            prependSeparator();
            bytes.append(flag == null ? "!" + NULL : flag ? "true" : "false");
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            prependSeparator();
            bytes.append(s == null ? "!" + NULL : quotes(s));
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            prependSeparator();
            bytes.append(i8);
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(@NotNull BytesStore fromBytes) {
            if (isText(fromBytes)) {
                return text(fromBytes);
            }
            int length = Maths.toInt32(fromBytes.writeRemaining());
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
            return TextWire.this;
        }

        private boolean isText(@NotNull BytesStore fromBytes) {
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
            prependSeparator();
            bytes.append("!!binary ").append(Base64.getEncoder().encodeToString(byteArray)).append(END_FIELD);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            prependSeparator();
            bytes.append(u8);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            prependSeparator();
            bytes.append(i16);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            prependSeparator();
            bytes.append(u16);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            prependSeparator();
            StringBuilder sb = Wires.acquireStringBuilder();
            sb.appendCodePoint(codepoint);
            text(sb);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            prependSeparator();
            bytes.append(i32);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            prependSeparator();
            bytes.append(u32);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            prependSeparator();
            bytes.append(i64);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            TextLongArrayReference.write(bytes, capacity);
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut float32(float f) {
            prependSeparator();
            bytes.append(f);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
            prependSeparator();
            bytes.append(d);
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut time(@NotNull LocalTime localTime) {
            prependSeparator();
            bytes.append(localTime.toString());
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            prependSeparator();
            bytes.append(zonedDateTime.toString());
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            prependSeparator();
            bytes.append(localDate.toString());
            elementSeparator();

            return TextWire.this;
        }

        @NotNull
        @Override
        public ValueOut type(@NotNull CharSequence typeName) {
            prependSeparator();
            bytes.append('!').append(typeName);
            sep = " ";
            return this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, Class type) {
            prependSeparator();
            bytes.append("!type ");
            typeTranslator.accept(type, bytes);
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull CharSequence type) {
            prependSeparator();
            bytes.append("!type ");
            text(type);
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut uuid(@NotNull UUID uuid) {
            prependSeparator();
            bytes.append(sep).append(uuid.toString());
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            prependSeparator();
            TextIntReference.write(bytes, value);
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value, IntValue intValue) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            prependSeparator();
            TextLongReference.write(bytes, value);
            elementSeparator();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value, LongValue longValue) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut sequence(@NotNull Consumer<ValueOut> writer) {
            pushState();
            bytes.append("[");
            sep = "\n";
            long pos = bytes.readPosition();
            writer.accept(this);
            if (pos != bytes.readPosition())
                bytes.append("\n");

            popState();
            indent();
            bytes.append("]");
            sep = END_FIELD;
            return TextWire.this;
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
            bytes.append("{");
            sep = leaf ? " " : END_FIELD;

            object.writeMarshallable(TextWire.this);

            if (!leaf)
                popState();
            else
                leaf = false;
            if (sep.startsWith(","))
                bytes.append(sep.substring(1));
            else
                prependSeparator();
            bytes.append('}');

            if (indentation == 0) {
                sep = "";
                bytes.append("\n");

            } else {
                sep = ",\n";
            }
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireOut map(@NotNull final Map map) {
            type(SEQ_MAP);
            bytes.append(" [");
            pushState();
            sep = END_FIELD;
            map.forEach((k, v) -> {
                prependSeparator();
                bytes.append("{ key: ");
                leaf();
                object2(k);
                sep = ",\n";
                prependSeparator();
                bytes.append("  value: ");
                leaf();
                object2(v);
                bytes.append(" }");
                sep = ",\n";
            });
            popState();
            sep = END_FIELD;
            prependSeparator();
            bytes.append("]");
            sep = END_FIELD;
            return TextWire.this;
        }

        private void object2(Object v) {
            if (v instanceof CharSequence)
                text((CharSequence) v);
            else if (v instanceof WriteMarshallable)
                typedMarshallable((WriteMarshallable) v);
            else if (v == null)
                bytes.append("!" + NULL);
            else
                text(String.valueOf(v));
        }

        @NotNull
        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            type(SEQ_MAP);
            map.forEach((k, v) -> sequence(w -> w.marshallable(m -> m
                    .write(() -> "key").typedMarshallable(k)
                    .write(() -> "value").typedMarshallable(v))));
            return TextWire.this;
        }

        @NotNull
        public ValueOut write() {
            bytes.append(sep).append("\"\": ");
            sep = "";
            return this;
        }

        @NotNull
        public ValueOut write(@NotNull WireKey key) {
            CharSequence name = key.name();
            if (name == null) name = Integer.toString(key.code());
            prependSeparator();
            bytes.append(quotes(name)).append(": ");
            return this;
        }

        public void writeComment(@NotNull CharSequence s) {
            prependSeparator();
            bytes.append(sep).append("# ").append(s).append("\n");
            sep = "";
        }
    }

    class TextValueIn implements ValueIn {
        @NotNull
        @Override
        public WireIn bool(@NotNull BooleanConsumer flag) {
            consumeWhiteSpace();

            StringBuilder sb = Wires.acquireStringBuilder();
            if (textTo(sb) == null) {
                flag.accept(null);
                return TextWire.this;
            }

            flag.accept(StringUtils.isEqual(sb, "true"));
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn text(@NotNull Consumer<String> s) {
            StringBuilder sb = Wires.acquireStringBuilder();
            Object acs = textTo(sb);
            s.accept(acs == null ? null : acs.toString());
            return TextWire.this;
        }

        @Override
        public String text() {
            return StringUtils.toString(textTo(Wires.acquireStringBuilder()));
        }

        @Nullable
        @Override
        public <ACS extends Appendable & CharSequence> ACS textTo(@NotNull ACS a) {
            consumeWhiteSpace();
            int ch = peekCode();

            if (ch == '{') {
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

            } else if (ch == '"') {
                bytes.readSkip(1);
                bytes.parseUTF(a, StopCharTesters.QUOTES.escaping());
                unescape(a);

            } else if (ch == '!') {
                bytes.readSkip(1);
                ch = peekCode();
                if (ch == '!') {
                    bytes.readSkip(1);
                    StringBuilder sb = Wires.acquireStringBuilder();
                    bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                    if (StringUtils.isEqual(sb, "null")) {
                        textTo(sb);
                        return null;
                    }
                } else {
                    StringBuilder sb = Wires.acquireStringBuilder();
                    textTo(sb);
                    // ignore the type.
                }

            } else {
                if (bytes.readRemaining() > 0) {
                    if (a instanceof Bytes)
                        bytes.parse8bit(a, TextStopCharsTesters.END_OF_TEXT);
                    else
                        bytes.parseUTF(a, TextStopCharsTesters.END_OF_TEXT);

                } else {
                    BytesUtil.setLength(a, 0);
                }
                // trim trailing spaces.
                while (a.length() > 0)
                    if (Character.isWhitespace(a.charAt(a.length() - 1)))
                        BytesUtil.setLength(a, a.length() - 1);
                    else
                        break;
            }

            int prev = rewindAndRead();
            if (prev == ':')
                bytes.readSkip(-1);
            return a;
        }

        private int rewindAndRead() {
            return bytes.readPosition() > 0 ? bytes.readUnsignedByte(bytes.readPosition() - 1) : -1;
        }

        @NotNull
        @Override
        public WireIn int8(@NotNull ByteConsumer i) {
            consumeWhiteSpace();
            i.accept((byte) bytes.parseLong());
            return TextWire.this;
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
        public WireIn bytes(@NotNull Consumer<WireIn> bytesConsumer) {
            consumeWhiteSpace();

            // TODO needs to be made much more efficient.
            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = Wires.INTERNER.intern(sb);
                if (str.equals("!!binary")) {
                    BytesUtil.setLength(sb, 0);
                    bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                    byte[] decode = Base64.getDecoder().decode(sb.toString());
                    bytesConsumer.accept(new TextWire(Bytes.wrapForRead(decode)));

                } else {
                    throw new IORuntimeException("Unsupported type " + str);
                }
            } else {
                textTo(sb);
                bytesConsumer.accept(new TextWire(Bytes.wrapForRead(sb.toString().getBytes())));
            }
            return TextWire.this;
        }

        public byte[] bytes() {
            consumeWhiteSpace();
            // TODO needs to be made much more efficient.
            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = Wires.INTERNER.intern(sb);
                if (str.equals("!!binary")) {
                    BytesUtil.setLength(sb, 0);
                    bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                    byte[] decode = Base64.getDecoder().decode(Wires.INTERNER.intern(sb));
                    return decode;

                } else if (str.equals("!" + SEQ_MAP)) {
                    sb.append(bytes.toString());
                    // todo fix this.
                    return Wires.INTERNER.intern(sb).getBytes();

                } else {
                    throw new IllegalStateException("unsupported type");
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
            return TextWire.this;
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
                            }
                            // do nothing
                        }
                    }

                    case '-': {
                        for (; ; ) {
                            byte b = bytes.readByte();
                            if (b == '\n') {
                                return (bytes.readPosition() - start) + 1;
                            }
                            if (bytes.readRemaining() == 0)
                                return bytes.readLimit() - start;
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

        private long readLengthMarshable() {
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
                            }
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

        @NotNull
        @Override
        public WireIn uint8(@NotNull ShortConsumer i) {
            consumeWhiteSpace();
            i.accept((short) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn int16(@NotNull ShortConsumer i) {
            consumeWhiteSpace();
            i.accept((short) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn uint16(@NotNull IntConsumer i) {
            consumeWhiteSpace();
            i.accept((int) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntConsumer i) {
            consumeWhiteSpace();
            i.accept((int) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn uint32(@NotNull LongConsumer i) {
            consumeWhiteSpace();
            i.accept(bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongConsumer i) {
            consumeWhiteSpace();
            i.accept(bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn float32(@NotNull FloatConsumer v) {
            consumeWhiteSpace();
            v.accept((float) bytes.parseDouble());
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn float64(@NotNull DoubleConsumer v) {
            consumeWhiteSpace();
            v.accept(bytes.parseDouble());
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn time(@NotNull Consumer<LocalTime> localTime) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            localTime.accept(LocalTime.parse(Wires.INTERNER.intern(sb)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            zonedDateTime.accept(ZonedDateTime.parse(Wires.INTERNER.intern(sb)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn date(@NotNull Consumer<LocalDate> localDate) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            localDate.accept(LocalDate.parse(Wires.INTERNER.intern(sb)));
            return TextWire.this;
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
        public WireIn uuid(@NotNull Consumer<UUID> uuid) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            uuid.accept(UUID.fromString(Wires.INTERNER.intern(sb)));
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn int64array(@Nullable LongArrayValues values, @NotNull Consumer<LongArrayValues> setter) {
            consumeWhiteSpace();
            if (!(values instanceof TextLongArrayReference)) {
                setter.accept(values = new TextLongArrayReference());
            }
            Byteable b = (Byteable) values;
            long length = TextLongArrayReference.peakLength(bytes, bytes.readPosition());
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return TextWire.this;
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
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(LongValue value, @NotNull Consumer<LongValue> setter) {
            if (!(value instanceof TextLongReference)) {
                setter.accept(value = new TextLongReference());
            }
            return int64(value);
        }

        @NotNull
        @Override
        public WireIn int32(IntValue value, @NotNull Consumer<IntValue> setter) {
            if (!(value instanceof TextIntReference)) {
                setter.accept(value = new TextIntReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            consumeWhiteSpace();
            if (peekCode() == ',')
                bytes.readSkip(1);
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn sequence(@NotNull Consumer<ValueIn> reader) {
            consumeWhiteSpace();
            char code = (char) readCode();
            if (code != '[')
                throw new IORuntimeException("Unsupported type " + code + " (" + code + ")");

            reader.accept(TextWire.this.valueIn);

            consumeWhiteSpace();
            code = (char) peekCode();
            if (code != ']')
                throw new IORuntimeException("Expected a ] but got " + code + " (" + code + ")");

            return TextWire.this;
        }

        @Override
        public <T> T applyToMarshallable(@NotNull Function<WireIn, T> marshallableReader) {
            consumeWhiteSpace();
            int code = peekCode();
            if (code != '{')
                throw new IORuntimeException("Unsupported type " + (char) code);

            final long len = readLengthMarshable() - 1;

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            try {
                // ensure that you can read past the end of this marshable object
                final long newLimit = position - 1 + len;
                bytes.readLimit(newLimit);
                bytes.readSkip(1); // skip the {
                consumeWhiteSpace();
                return marshallableReader.apply(TextWire.this);
            } finally {
                bytes.readLimit(limit);

                consumeWhiteSpace();
                code = readCode();
                if (code != '}')
                    throw new IORuntimeException("Unterminated { while reading marshallable "
                            + "bytes=" + Bytes.toString(bytes)
                    );
            }
        }

        @NotNull
        @Override
        public WireIn type(@NotNull StringBuilder sb) {
            consumeWhiteSpace();
            int code = peekCode();
            if (code == -1) {
                sb.append("java.lang.Void");
            } else if (code != '!') {
                sb.append("java.lang.String");
            } else {
                readCode();
                bytes.parseUTF(sb, TextStopCharTesters.END_OF_TYPE);
            }
            return TextWire.this;
        }

        String stringForCode(int code) {
            return code < 0 ? "Unexpected end of input" : "'" + (char) code + "'";
        }

        @NotNull
        @Override
        public WireIn typeLiteralAsText(@NotNull Consumer<CharSequence> classNameConsumer) {
            consumeWhiteSpace();
            int code = readCode();
            if (!peekStringIgnoreCase("type "))
                throw new UnsupportedOperationException(stringForCode(code));
            bytes.readSkip("type ".length());
            StringBuilder sb = Wires.acquireStringBuilder();
            bytes.parseUTF(sb, TextStopCharTesters.END_OF_TYPE);
            classNameConsumer.accept(sb);
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) {
            consumeWhiteSpace();
            int code = peekCode();
            if (code == '!')
                type(Wires.acquireStringBuilder());
            else if (code != '{')
                throw new IORuntimeException("Unsupported type " + stringForCode(code));

            final long len = readLengthMarshable() - 1;

            final long limit = bytes.readLimit();
            final long position = bytes.readPosition();

            final long newLimit = position - 1 + len;
            try {
                // ensure that you can read past the end of this marshable object

                bytes.readLimit(newLimit);
                bytes.readSkip(1); // skip the {
                consumeWhiteSpace();
                object.readMarshallable(TextWire.this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(newLimit);
            }

            consumeWhiteSpace();
            code = readCode();
            if (code != '}')
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + ",code='" + (char) code + "', bytes=" + Bytes.toString(bytes)
                );
            return TextWire.this;
        }

        @Nullable
        public <T extends ReadMarshallable> T typedMarshallable() {
            try {
                consumeWhiteSpace();
                int code = peekCode();
                if (code < 0)
                    throw new IllegalStateException("Cannot read nothing as a Marshallable " + bytes.toDebugString());
                StringBuilder sb = Wires.acquireStringBuilder();
                if (code != '!')
                    throw new ClassCastException("Cannot convert to Marshallable. " + bytes.toDebugString());

                readCode();
                bytes.parseUTF(sb, TextStopCharTesters.END_OF_TYPE);

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

                if (!Marshallable.class.isAssignableFrom(clazz))
                    throw new ClassCastException("Cannot convert " + sb + " to Marshallable.");

                final ReadMarshallable m = OS.memory().allocateInstance((Class<ReadMarshallable>) clazz);

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

            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = Wires.INTERNER.intern(sb);

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

            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = Wires.INTERNER.intern(sb);
                if (SEQ_MAP.contentEquals(sb)) {
                    while (hasNext()) {
                        sequence(s -> s.marshallable(r -> {
                            try {
                                @SuppressWarnings("unchecked")
                                final K k = r.read(() -> "key").typedMarshallable();
                                @SuppressWarnings("unchecked")
                                final V v = r.read(() -> "value").typedMarshallable();
                                usingMap.put(k, v);
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
            StringBuilder sb = Wires.acquireStringBuilder();
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
         * @return true if !!null "", if {@code true} reads the !!null "" up to the next STOP, if {@code
         * false} no  data is read  ( data is only peaked if {@code false} )
         */
        public boolean isNull() {
            consumeWhiteSpace();

            if (peekStringIgnoreCase("!!null \"\"")) {
                bytes.readSkip("!!null \"\"".length());
                // discard the text after it.
                //  text(Wires.acquireStringBuilder());
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
        @Override
        public <E> WireIn object(@NotNull Class<E> clazz, Consumer<E> e) {
            e.accept(ObjectUtils.convertTo(clazz, object0(null, clazz)));
            return TextWire.this;
        }

        <E> E object0(@Nullable E using, @NotNull Class<E> clazz) {
            consumeWhiteSpace();

            if (isNull())
                return null;

            if (byte[].class.isAssignableFrom(clazz))
                return (E) bytes();

            if (ReadMarshallable.class.isAssignableFrom(clazz)) {
                final E v;
                if (using == null)
                    v = OS.memory().allocateInstance(clazz);
                else
                    v = using;

                valueIn.marshallable((ReadMarshallable) v);
                return readResolve(v);

            } else if (StringBuilder.class.isAssignableFrom(clazz)) {
                StringBuilder builder = (using == null)
                        ? Wires.acquireStringBuilder()
                        : (StringBuilder) using;
                valueIn.textTo(builder);
                return using;

            } else if (CharSequence.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) valueIn.text();

            } else if (Long.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Long) valueIn.int64();

            } else if (Double.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Double) valueIn.float64();

            } else if (Integer.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Integer) valueIn.int32();

            } else if (Float.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Float) valueIn.float32();

            } else if (Short.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Short) valueIn.int16();

            } else if (Character.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                final String text = valueIn.text();
                if (text == null || text.length() == 0)
                    return null;
                return (E) (Character) text.charAt(0);

            } else if (Byte.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Byte) valueIn.int8();

            } else if (Map.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                final Map result = new HashMap();
                valueIn.map(result);
                return (E) result;

            } else {
                // TODO assume for now it is a string.
                if (peekCode() == '!') {
                    readCode();
                    StringBuilder sb = Wires.acquireStringBuilder();
                    bytes.parseUTF(sb, TextStopCharTesters.END_OF_TYPE);
                    final Class clazz2 = ClassAliasPool.CLASS_ALIASES.forName(sb);
                    return (E) object(null, clazz2);
                }
                return (E) valueIn.text();
            }
        }
    }
}
