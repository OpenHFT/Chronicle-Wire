/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;


import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.pool.StringInterner;
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
import static net.openhft.chronicle.wire.BinaryWireCode.stringForCode;

/**
 * Created by peter.lawrey on 15/01/15.
 */
public class TextWire implements Wire, InternalWireIn {

    private static final Logger LOG =
            LoggerFactory.getLogger(TextWire.class);

    public static final String FIELD_SEP = "";
    private static final String END_FIELD = "\n";
    public static final String SEQ_MAP = "!seqmap";

    final Bytes<?> bytes;
    final TextValueOut valueOut = new TextValueOut();
    final ValueIn valueIn = new TextValueIn();

    boolean ready;

    public TextWire(Bytes<?> bytes) {
        this.bytes = bytes;
    }

    public static String asText(Wire wire) {
        TextWire tw = new TextWire(nativeBytes());
        wire.copyTo(tw);
        tw.flip();
        wire.flip();
        return tw.toString();
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

    @Override
    public ValueIn read() {
        readField(Wires.acquireStringBuilder());
        return valueIn;
    }

    private StringBuilder readField(StringBuilder sb) {
        consumeWhiteSpace();
        if (peekCode() == ',') {
            bytes.skip(1);
            consumeWhiteSpace();
        }
        try {
            int ch = peekCode();
            if (ch == '"') {
                bytes.skip(1);
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
        int codePoint = peekCode();
        while (Character.isWhitespace(codePoint)) {
            bytes.skip(1);
            codePoint = peekCode();
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

        if (bytes.remaining() < 1)
            return false;

        long pos = bytes.position();

        try {
            for (int i = 0; i < source.length(); i++) {
                if (Character.toLowerCase(source.charAt(i)) != Character.toLowerCase(bytes.readByte()))
                    return false;
            }
        } finally {
            bytes.position(pos);
        }

        return true;
    }

    private int readCode() {
        if (bytes.remaining() < 1)
            return -1;
        return bytes.readUnsignedByte();
    }

    public static <ACS extends Appendable & CharSequence> void unescape(ACS sb) {
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

    @Override
    public ValueIn read(@NotNull WireKey key) {
        long position = bytes.position();
        StringBuilder sb = readField(Wires.acquireStringBuilder());
        if (sb.length() == 0 || StringInterner.isEqual(sb, key.name()))
            return valueIn;
        bytes.position(position);
        throw new UnsupportedOperationException("Unordered fields not supported yet. key=" + key
                .name() + ", was=" + sb + ", data='" + sb + "'");
    }

    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        consumeWhiteSpace();
        readField(name);
        return valueIn;
    }

    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void flip() {
        bytes.flip();
    }

    @Override
    public void clear() {
        bytes.clear();
    }

    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    @Override
    public ValueOut write() {
        return valueOut.write();
    }

    @Override
    public ValueOut write(WireKey key) {
        return valueOut.write(key);
    }

    @Override
    public ValueOut writeValue() {
        return valueOut;
    }

    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @Override
    public Wire writeComment(CharSequence s) {
        valueOut.writeComment(s);
        return this;
    }

    @Override
    public boolean hasDocument() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WireOut addPadding(int paddingToAdd) {
        for (int i = 0; i < paddingToAdd; i++)
            bytes.append((bytes.position() & 63) == 0 ? '\n' : ' ');
        return this;
    }

    CharSequence quotes(CharSequence s) {
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

    static final BitSet QUOTE_CHARS = new BitSet();

    static {
        for (char ch : "\",\n\\#:{}[]".toCharArray())
            QUOTE_CHARS.set(ch);
    }

    boolean needsQuotes(CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (QUOTE_CHARS.get(ch))
                return true;
        }
        return s.length() == 0;
    }

    class TextValueOut implements ValueOut {
        int indentation = 0;
        String sep = "";
        boolean leaf = false;

        void prependSeparator() {
            bytes.append(sep);
            if (sep.endsWith("\n"))
                indent();
            sep = "";
        }

        @Override
        public ValueOut leaf() {
            leaf = true;
            return this;
        }

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

        @Override
        public Wire bool(Boolean flag) {
            prependSeparator();
            bytes.append(flag == null ? "!!null" : flag ? "true" : "false");
            elementSeparator();
            return TextWire.this;
        }

        @Override
        public Wire text(CharSequence s) {
            prependSeparator();
            bytes.append(s == null ? "!!null" : quotes(s));
            elementSeparator();
            return TextWire.this;
        }

        @Override
        public Wire int8(byte i8) {
            prependSeparator();
            bytes.append(i8);
            elementSeparator();
            return TextWire.this;
        }

        @Override
        public WireOut bytes(Bytes fromBytes) {
            if (isText(fromBytes)) {
                return text(fromBytes);
            }
            int length = Maths.toInt32(fromBytes.remaining());
            byte[] byteArray = new byte[length];
            fromBytes.read(byteArray);
            return bytes(byteArray);
        }

        @Override
        public WireOut rawBytes(byte[] value) {
            prependSeparator();
            bytes.write(value);
            elementSeparator();
            return TextWire.this;
        }

        private boolean isText(Bytes fromBytes) {
            for (long i = fromBytes.position(); i < fromBytes.readLimit(); i++) {
                int ch = fromBytes.readUnsignedByte(i);
                if ((ch < ' ' && ch != '\t') || ch >= 127)
                    return false;
            }
            return true;
        }

        @Override
        public ValueOut writeLength(long remaining) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut bytes(byte[] byteArray) {
            prependSeparator();
            bytes.append("!!binary ").append(Base64.getEncoder().encodeToString(byteArray)).append(END_FIELD);
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire uint8checked(int u8) {
            prependSeparator();
            bytes.append(u8);
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire int16(short i16) {
            prependSeparator();
            bytes.append(i16);
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire uint16checked(int u16) {
            prependSeparator();
            bytes.append(u16);
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire utf8(int codepoint) {
            prependSeparator();
            StringBuilder sb = Wires.acquireStringBuilder();
            sb.appendCodePoint(codepoint);
            text(sb);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire int32(int i32) {
            prependSeparator();
            bytes.append(i32);
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire uint32checked(long u32) {
            prependSeparator();
            bytes.append(u32);
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire int64(long i64) {
            prependSeparator();
            bytes.append(i64);
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public WireOut int64array(long capacity) {
            TextLongArrayReference.write(bytes, capacity);
            return TextWire.this;
        }

        @Override
        public Wire float32(float f) {
            prependSeparator();
            bytes.append(f);
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire float64(double d) {
            prependSeparator();
            bytes.append(d);
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire time(LocalTime localTime) {
            prependSeparator();
            bytes.append(localTime.toString());
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire zonedDateTime(ZonedDateTime zonedDateTime) {
            prependSeparator();
            bytes.append(zonedDateTime.toString());
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire date(LocalDate localDate) {
            prependSeparator();
            bytes.append(localDate.toString());
            elementSeparator();

            return TextWire.this;
        }

        @Override
        public Wire type(CharSequence typeName) {
            prependSeparator();
            bytes.append('!').append(typeName);
            sep = " ";
            return TextWire.this;
        }

        @Override
        public WireOut uuid(UUID uuid) {
            prependSeparator();
            bytes.append(sep).append(uuid.toString());
            elementSeparator();
            return TextWire.this;
        }

        @Override
        public WireOut int32forBinding(int value) {
            prependSeparator();
            IntTextReference.write(bytes, value);
            elementSeparator();
            return TextWire.this;
        }

        @Override
        public WireOut int64forBinding(long value) {
            prependSeparator();
            TextLongReference.write(bytes, value);
            elementSeparator();
            return TextWire.this;
        }

        @Override
        public WireOut sequence(Consumer<ValueOut> writer) {
            pushState();
            bytes.append("[");
            sep = "\n";
            long pos = bytes.position();
            writer.accept(this);
            if (pos != bytes.position())
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

        @Override
        public WireOut marshallable(WriteMarshallable object) {
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
                bytes.append("!!null");
            else
                text(String.valueOf(v));
        }

        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            type(SEQ_MAP);
            map.forEach((k, v) -> sequence(w -> w.marshallable(m -> m
                    .write(() -> "key").typedMarshallable(k)
                    .write(() -> "value").typedMarshallable(v))));
            return TextWire.this;
        }

        public ValueOut write() {
            bytes.append(sep).append("\"\": ");
            sep = "";
            return this;
        }

        public ValueOut write(WireKey key) {
            CharSequence name = key.name();
            if (name == null) name = Integer.toString(key.code());
            prependSeparator();
            bytes.append(quotes(name)).append(": ");
            return this;
        }

        public void writeComment(CharSequence s) {
            prependSeparator();
            bytes.append(sep).append("# ").append(s).append("\n");
            sep = "";
        }
    }

    class TextValueIn implements ValueIn {
        @NotNull
        @Override
        public Wire bool(@NotNull BooleanConsumer flag) {
            consumeWhiteSpace();


            if (isNull()) {
                flag.accept(null);
                return TextWire.this;
            }

            StringBuilder sb = Wires.acquireStringBuilder();
            bytes.parseUTF(sb, StopCharTesters.COMMA_STOP);
            if (StringInterner.isEqual(sb, "true"))
                flag.accept(true);
            else if (StringInterner.isEqual(sb, "false"))
                flag.accept(false);
            else
                throw new UnsupportedOperationException();
            return TextWire.this;
        }

        @NotNull
        @Override
        public WireIn text(@NotNull Consumer<String> s) {
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            s.accept(sb.toString());
            return TextWire.this;
        }

        @Override
        public String text() {
            if (isNull())
                return null;
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            return sb.toString();
        }

        @NotNull
        @Override
        public <ACS extends Appendable & CharSequence> WireIn text(@NotNull ACS a) {
            consumeWhiteSpace();
            int ch = peekCode();

            if (ch == '{') {
                final long len = readLength();
                try {
                    a.append(Bytes.toDebugString(bytes, bytes.position(), len));
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
                bytes.skip(len);

                // read the next comma
                bytes.skipTo(StopCharTesters.COMMA_STOP);

                return TextWire.this;
            }

            if (ch == '"') {
                bytes.skip(1);
                bytes.parseUTF(a, StopCharTesters.QUOTES.escaping());
                unescape(a);
            } else {
                if (bytes.remaining() > 0) {
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
                bytes.skip(-1);
            return TextWire.this;
        }

        private int rewindAndRead() {
            return bytes.readUnsignedByte(bytes.position() - 1);
        }

        @NotNull
        @Override
        public Wire int8(@NotNull ByteConsumer i) {
            consumeWhiteSpace();
            i.accept((byte) bytes.parseLong());
            return TextWire.this;
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
                String str = sb.toString();
                if (str.equals("!!binary")) {
                    BytesUtil.setLength(sb, 0);
                    bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                    byte[] decode = Base64.getDecoder().decode(sb.toString());
                    bytesConsumer.accept(new TextWire(Bytes.wrap(decode)));
                } else {
                    throw new IORuntimeException("Unsupported type " + str);
                }
            } else {
                text(sb);
                bytesConsumer.accept(new TextWire(Bytes.wrap(sb.toString().getBytes())));
            }
            return TextWire.this;
        }

        public byte[] bytes() {
            consumeWhiteSpace();
            // TODO needs to be made much more efficient.
            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = sb.toString();
                if (str.equals("!!binary")) {
                    BytesUtil.setLength(sb, 0);
                    bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                    byte[] decode = Base64.getDecoder().decode(sb.toString());
                    return decode;
                } else if (str.equals("!" + SEQ_MAP)) {
                    sb.append(bytes.toString());
                    return sb.toString().getBytes();
                } else {
                    throw new IllegalStateException("unsupported type");
                }

            } else {
                text(sb);
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
            long start = bytes.position();
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
                                    return bytes.position() - start;
                            }
                            // do nothing
                        }
                    }

                    case '-': {
                        for (; ; ) {
                            byte b = bytes.readByte();
                            if (b == '\n') {
                                return (bytes.position() - start) + 1;
                            }
                            if (bytes.remaining() == 0)
                                return bytes.limit() - start;
                            // do nothing
                        }
                    }
                    default:
                        // TODO needs to be made much more efficient.
                        bytes();
                        return bytes.position() - start;
                }
            } finally {
                bytes.position(start);
            }
        }

        private long readLengthMarshable() {
            long start = bytes.position();
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
                                    return bytes.position() - start;
                            }
                            // do nothing
                        }
                    }


                    default:
                        // TODO needs to be made much more efficient.
                        bytes();
                        return bytes.position() - start;
                }
            } finally {
                bytes.position(start);
            }
        }


        @NotNull
        @Override
        public Wire uint8(@NotNull ShortConsumer i) {
            consumeWhiteSpace();
            i.accept((short) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire int16(@NotNull ShortConsumer i) {
            consumeWhiteSpace();
            i.accept((short) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire uint16(@NotNull IntConsumer i) {
            consumeWhiteSpace();
            i.accept((int) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire int32(@NotNull IntConsumer i) {
            consumeWhiteSpace();
            i.accept((int) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire uint32(@NotNull LongConsumer i) {
            consumeWhiteSpace();
            i.accept(bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire int64(@NotNull LongConsumer i) {
            consumeWhiteSpace();
            i.accept(bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire float32(@NotNull FloatConsumer v) {
            consumeWhiteSpace();
            v.accept((float) bytes.parseDouble());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire float64(@NotNull DoubleConsumer v) {
            consumeWhiteSpace();
            v.accept(bytes.parseDouble());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire time(@NotNull Consumer<LocalTime> localTime) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            localTime.accept(LocalTime.parse(sb.toString()));
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            zonedDateTime.accept(ZonedDateTime.parse(sb.toString()));
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire date(@NotNull Consumer<LocalDate> localDate) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            localDate.accept(LocalDate.parse(sb.toString()));
            return TextWire.this;
        }

        @Override
        public boolean hasNext() {
            return bytes.remaining() > 0;
        }

        @Override
        public boolean hasNextSequenceItem() {
            consumeWhiteSpace();
            int ch = peekCode();
            if (ch == ',') {
                bytes.skip(1);
                return true;
            }
            return ch != ']';
        }

        @Override
        public WireIn uuid(@NotNull Consumer<UUID> uuid) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            uuid.accept(UUID.fromString(sb.toString()));
            return TextWire.this;
        }

        @Override
        public WireIn int64array(@Nullable LongArrayValues values, @NotNull Consumer<LongArrayValues> setter) {
            consumeWhiteSpace();
            if (!(values instanceof TextLongArrayReference)) {
                setter.accept(values = new TextLongArrayReference());
            }
            Byteable b = (Byteable) values;
            long length = TextLongArrayReference.peakLength(bytes, bytes.position());
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return TextWire.this;
        }

        @Override
        public WireIn int64(LongValue value, @NotNull Consumer<LongValue> setter) {
            consumeWhiteSpace();
            if (!(value instanceof TextLongReference)) {
                setter.accept(value = new TextLongReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            consumeWhiteSpace();
            if (peekCode() == ',')
                bytes.skip(1);
            return TextWire.this;
        }

        @Override
        public WireIn int32(IntValue value, @NotNull Consumer<IntValue> setter) {
            if (!(value instanceof IntTextReference)) {
                setter.accept(value = new IntTextReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            consumeWhiteSpace();
            if (peekCode() == ',')
                bytes.skip(1);
            return TextWire.this;
        }

        @Override
        public WireIn sequence(@NotNull Consumer<ValueIn> reader) {
            consumeWhiteSpace();
            int code = readCode();
            if (code != '[')
                throw new IORuntimeException("Unsupported type " + (char) code + " (" + code + ")");

            reader.accept(TextWire.this.valueIn);

            consumeWhiteSpace();
            code = peekCode();
            if (code != ']')
                throw new IORuntimeException("Expected a ] but got " + (char) code + " (" + code + ")");

            return TextWire.this;
        }

        @Override
        public <T> T applyToMarshallable(Function<WireIn, T> marshallableReader) {
            consumeWhiteSpace();
            int code = peekCode();
            if (code != '{')
                throw new IORuntimeException("Unsupported type " + (char) code);

            final long len = readLengthMarshable() - 1;


            final long limit = bytes.limit();
            final long position = bytes.position();


            try {
                // ensure that you can read past the end of this marshable object
                final long newLimit = position - 1 + len;
                bytes.limit(newLimit);
                bytes.skip(1); // skip the {
                consumeWhiteSpace();
                return marshallableReader.apply(TextWire.this);
            } finally {
                bytes.limit(limit);

                consumeWhiteSpace();
                code = readCode();
                if (code != '}')
                    throw new IORuntimeException("Unterminated { while reading marshallable "
                            + "bytes=" + Bytes.toDebugString(bytes)
                    );
            }
        }

        @NotNull
        @Override
        public Wire type(@NotNull StringBuilder s) {
            consumeWhiteSpace();
            int code = readCode();
            if (code != '!') {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            bytes.parseUTF(s, StopCharTesters.SPACE_STOP);
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
                throw new IORuntimeException("Unsupported type " + (char) code);

            final long len = readLengthMarshable() - 1;


            final long limit = bytes.limit();
            final long position = bytes.position();

            final long newLimit = position - 1 + len;
            try {
                // ensure that you can read past the end of this marshable object

                bytes.limit(newLimit);
                bytes.skip(1); // skip the {
                consumeWhiteSpace();
                object.readMarshallable(TextWire.this);

            } finally {
                bytes.limit(limit);
                bytes.position(newLimit);
            }

            consumeWhiteSpace();
            code = readCode();
            if (code != '}')
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + ",code='" + (char) code + "', bytes=" + Bytes.toDebugString(bytes)
                );
            return TextWire.this;
        }


        @Override
        public <K, V> Map<K, V> map(@NotNull final Class<K> kClazz,
                                    @NotNull final Class<V> vClass,
                                    @NotNull final Map<K, V> usingMap) {
            consumeWhiteSpace();
            usingMap.clear();

            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {

                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = sb.toString();

                if ("!!null".contentEquals(sb)) {
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
                String str = sb.toString();
                if (SEQ_MAP.contentEquals(sb)) {
                    while (hasNext()) {

                        sequence(s -> s.marshallable(r -> {
                            try {
                                @SuppressWarnings("unchecked")
                                final K k = (K) r.read(() -> "key").typedMarshallable();
                                @SuppressWarnings("unchecked")
                                final V v = (V) r.read(() -> "value").typedMarshallable();
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
            bytes.parseUTF(sb, StopCharTesters.COMMA_STOP);
            if (StringInterner.isEqual(sb, "true"))
                return true;
            else if (StringInterner.isEqual(sb, "false"))
                return false;
            if (isNull())
                throw new NullPointerException("value is null");
            else
                throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public float float32() {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * @return true if !!null, if {@code true} reads the !!null up to the next STOP, if {@code
         * false} no  data is read  ( data is only peaked if {@code false} )
         */
        @Override
        public boolean isNull() {
            consumeWhiteSpace();

            if (peekStringIgnoreCase("!!null")) {
                bytes.skip("!!null".length());
                if (bytes.remaining() > 0 && bytes.readByte(bytes.position()) == '\n')
                    bytes.skip(1);
                return true;
            }

            return false;
        }


        @Override
        @Nullable
        public <E> E object(@Nullable E using,
                            @NotNull Class<E> clazz) {

            consumeWhiteSpace();

            if (isNull())
                return null;

            if (byte[].class.isAssignableFrom(clazz))
                return (E) bytes();

            if (Marshallable.class.isAssignableFrom(clazz)) {

                final E v;
                if (using == null)
                    v = OS.memory().allocateInstance(clazz);
                else
                    v = using;

                valueIn.marshallable((Marshallable) v);
                return v;

            } else if (StringBuilder.class.isAssignableFrom(clazz)) {
                StringBuilder builder = (using == null)
                        ? Wires.acquireStringBuilder()
                        : (StringBuilder) using;
                valueIn.text(builder);
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
                throw new IllegalStateException("unsupported type=" + clazz);
            }
        }
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
        }
    }
}
