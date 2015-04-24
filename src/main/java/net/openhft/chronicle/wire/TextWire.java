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

import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static net.openhft.chronicle.wire.WireType.stringForCode;

/**
 * Created by peter.lawrey on 15/01/15.
 */
public class TextWire implements Wire, InternalWireIn {

    private static final Logger LOG =
            LoggerFactory.getLogger(TextWire.class);

    public static final String FIELD_SEP = "";
    private static final String END_FIELD = "\n";
    final Bytes<?> bytes;
    final ValueOut valueOut = new TextValueOut();
    final ValueIn valueIn = new TextValueIn();

    String sep = "";
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
    public void copyTo(WireOut wire) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueIn read() {
        readField(Wires.acquireStringBuilder());
        return valueIn;
    }

    private StringBuilder readField(StringBuilder sb) {
        consumeWhiteSpace();
        try {
            int ch = peekCode();
            if (ch == '"') {
                bytes.skip(1);
                bytes.parseUTF(sb, EscapingStopCharTester.escaping(c -> c == '"'));

                consumeWhiteSpace();
                ch = readCode();
                if (ch != ':')
                    throw new UnsupportedOperationException("Expected a : at " + bytes.toDebugString());

            } else if (ch < 0) {
                sb.setLength(0);
                return sb;
            } else {
                bytes.parseUTF(sb, EscapingStopCharTester.escaping(c -> c < ' ' || c == ':'));
            }
            unescape(sb);
        } catch (BufferUnderflowException ignored) {
        }
        consumeWhiteSpace();
        return sb;
    }

    private void consumeWhiteSpace() {
        int byteValue;
        while (bytes.remaining() > 0) {
            byteValue = bytes.readUnsignedByte(bytes.position());

            // white-space, comma (ascii=44)
            if (Character.isWhitespace(byteValue) || (byteValue == 44)) {
                bytes.skip(1);
            } else {
                break;
            }
        }
    }

    private int peekCode() {
        if (bytes.remaining() < 1)
            return -1;
        long pos = bytes.position();
        return bytes.readUnsignedByte(pos);
    }

    private int readCode() {
        if (bytes.remaining() < 1)
            return -1;
        return bytes.readUnsignedByte();
    }

    private void unescape(StringBuilder sb) {
        for (int i = 0; i < sb.length(); i++) {
            char ch2 = sb.charAt(i);
            if (ch2 == '\\') {
                sb.deleteCharAt(i);
                char ch3 = sb.charAt(i);
                switch (ch3) {
                    case 'n':
                        sb.setCharAt(i, '\n');
                        break;
                }
            }
        }
    }

    @Override
    public ValueIn read(WireKey key) {
        long position = bytes.position();
        StringBuilder sb = readField(Wires.acquireStringBuilder());
        if (sb.length() == 0 || StringInterner.isEqual(sb, key.name()))
            return valueIn;
        bytes.position(position);
        throw new UnsupportedOperationException("Unordered fields not supported yet. key=" + key
                .name() + ", data='" + sb + "'");
    }

    @Override
    public ValueIn read(StringBuilder name) {
        consumeWhiteSpace();
        readField(name);
        return valueIn;
    }

    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    @Override
    public boolean hasNextSequenceItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wire readComment(StringBuilder s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMapping() {
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
        bytes.append(sep).append("\"\": ");
        sep = "";
        return valueOut;
    }

    @Override
    public ValueOut write(WireKey key) {
        CharSequence name = key.name();
        if (name == null) name = Integer.toString(key.code());
        bytes.append(sep).append(quotes(name)).append(":");
        sep = " ";
        return valueOut;
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
        bytes.append(sep).append("# ").append(s).append("\n");
        sep = "";
        return TextWire.this;
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

    boolean needsQuotes(CharSequence s) {
        for (int i = 0; i < s.length(); i++)
            if ("\" ,\n\\".indexOf(s.charAt(i)) >= 0)
                return true;
        return s.length() == 0;
    }

    class TextValueOut implements ValueOut {
        boolean nested = false;


        public void separator() {
            if (isNested()) {
                sep = ", ";
            } else {
                bytes.append(END_FIELD);
                sep = "";
            }
        }

        @Override
        public Wire bool(Boolean flag) {
            bytes.append(sep).append(flag == null ? "!!null" : flag ? "true" : "false");
            separator();
            return TextWire.this;
        }

        @Override
        public Wire text(CharSequence s) {
            if (s != null && " ".equals(sep) && startsWith(s, "//"))
                sep = "";
            bytes.append(sep).append(s == null ? "!!null" : quotes(s));
            separator();
            return TextWire.this;
        }

        @Override
        public Wire int8(byte i8) {
            bytes.append(sep).append(i8);
            separator();
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

        private boolean isText(Bytes fromBytes) {
            for (long i = fromBytes.position(); i < fromBytes.readLimit(); i++) {
                int ch = fromBytes.readUnsignedByte(i);
                if ((ch < ' ' && ch != '\t') || ch >= 127)
                    return false;
            }
            return true;
        }

        private boolean startsWith(CharSequence s, String starts) {
            if (s.length() < starts.length())
                return false;
            for (int i = 0; i < starts.length(); i++)
                if (s.charAt(i) != starts.charAt(i))
                    return false;
            return true;
        }

        @Override
        public ValueOut writeLength(long remaining) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut bytes(byte[] byteArray) {
            bytes.append(sep).append("!!binary ").append(Base64.getEncoder().encodeToString(byteArray)).append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire uint8checked(int u8) {
            bytes.append(sep).append(u8);
            separator();

            return TextWire.this;
        }

        @Override
        public Wire int16(short i16) {
            bytes.append(sep).append(i16);
            separator();

            return TextWire.this;
        }

        @Override
        public Wire uint16checked(int u16) {
            bytes.append(sep).append(u16);
            separator();

            return TextWire.this;
        }

        @Override
        public Wire utf8(int codepoint) {
            StringBuilder sb = Wires.acquireStringBuilder();
            sb.appendCodePoint(codepoint);
            text(sb);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire int32(int i32) {
            bytes.append(sep).append(i32);
            separator();

            return TextWire.this;
        }

        @Override
        public Wire uint32checked(long u32) {
            bytes.append(sep).append(u32);
            separator();

            return TextWire.this;
        }

        @Override
        public Wire int64(long i64) {
            bytes.append(sep).append(i64);
            separator();

            return TextWire.this;
        }

        @Override
        public WireOut int64array(long capacity) {
            TextLongArrayReference.write(bytes, capacity);
            return TextWire.this;
        }

        @Override
        public Wire float32(float f) {
            bytes.append(sep).append(f);
            separator();

            return TextWire.this;
        }

        @Override
        public Wire float64(double d) {
            bytes.append(sep).append(d);
            separator();

            return TextWire.this;
        }

        @Override
        public Wire time(LocalTime localTime) {
            bytes.append(localTime.toString());
            separator();

            return TextWire.this;
        }

        @Override
        public Wire zonedDateTime(ZonedDateTime zonedDateTime) {
            bytes.append(zonedDateTime.toString());
            separator();

            return TextWire.this;
        }

        @Override
        public Wire date(LocalDate localDate) {
            bytes.append(localDate.toString());
            separator();

            return TextWire.this;
        }

        @Override
        public Wire type(CharSequence typeName) {
            bytes.append(sep).append('!').append(typeName);
            sep = " ";
            return TextWire.this;
        }

        @Override
        public WireOut uuid(UUID uuid) {
            bytes.append(sep).append(uuid.toString());
            separator();
            return TextWire.this;
        }

        @Override
        public WireOut int32forBinding(int value) {
            bytes.append(sep);
            IntTextReference.write(bytes, value);
            separator();
            return TextWire.this;
        }

        @Override
        public WireOut int64forBinding(long value) {
            bytes.append(sep);
            TextLongReference.write(bytes, value);
            separator();
            return TextWire.this;
        }

        @Override
        public WireOut sequence(Consumer<ValueOut> writer) {
            boolean nested = isNested();
            nested(true);
            try {
                bytes.append(sep);
                sep = "";
                bytes.append("\n  - ");
                writer.accept(this);
                sep = "";

            } finally {
                nested(nested);
            }
            return TextWire.this;
        }

        @Override
        public WireOut marshallable(WriteMarshallable object) {
            bytes.append(sep);
            bytes.append("{ ");
            sep = "";
            boolean nested = isNested();
            try {
                nested(true);
                object.writeMarshallable(TextWire.this);
            } finally {
                nested(nested);
            }
            bytes.append(' ');
            bytes.append('}');
            sep = nested ? ", " : END_FIELD;
            return TextWire.this;
        }

        @Override
        public WireOut map(@NotNull final Map map) {
            nested = true;
            type("!map");
            map.forEach((k, v) -> sequence(w -> w.marshallable(m -> m
                    .write(() -> "key").object(k)
                    .write(() -> "value").object(v))));
            return TextWire.this;
        }


        @Override
        public boolean isNested() {
            return nested;
        }

        @Override
        public WireOut nested(boolean nested) {
            this.nested = nested;
            return TextWire.this;
        }


    }

    class TextValueIn implements ValueIn {
        @NotNull
        @Override
        public Wire bool(@NotNull BooleanConsumer flag) {
            StringBuilder sb = Wires.acquireStringBuilder();
            bytes.parseUTF(sb, StopCharTesters.COMMA_STOP);
            if (StringInterner.isEqual(sb, "true"))
                flag.accept(true);
            else if (StringInterner.isEqual(sb, "false"))
                flag.accept(false);
            else if (StringInterner.isEqual(sb, "!!null"))
                flag.accept(null);
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
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            return sb.toString();
        }

        @NotNull
        @Override
        public Wire text(@NotNull StringBuilder s) {
            int ch = peekCode();
            StringBuilder sb = s;
            if (ch == '{') {
                final long len = readLength();
                sb.append(Bytes.toDebugString(bytes, bytes.position(), len));
                bytes.skip(len);

                // read the next comma
                bytes.skipTo(StopCharTesters.COMMA_STOP);

                return TextWire.this;
            }

            if (ch == '"') {
                bytes.skip(1);
                bytes.parseUTF(sb, EscapingStopCharTester.escaping(c -> c == '"'));
                consumeWhiteSpace();
            } else {
                bytes.parseUTF(sb, EscapingStopCharTester.escaping(StopCharTesters.COMMA_STOP));
            }
            unescape(sb);
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire int8(@NotNull ByteConsumer i) {
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
            // TODO needs to be made much more efficient.
            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = sb.toString();
                if (str.equals("!!binary")) {
                    sb.setLength(0);
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
            // TODO needs to be made much more efficient.
            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = sb.toString();
                if (str.equals("!!binary")) {
                    sb.setLength(0);
                    bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                    byte[] decode = Base64.getDecoder().decode(sb.toString());
                    return decode;
                } else {
                    throw new IORuntimeException("Unsupported type " + str);
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

        @NotNull
        @Override
        public Wire uint8(@NotNull ShortConsumer i) {
            i.accept((short) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire int16(@NotNull ShortConsumer i) {
            i.accept((short) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire uint16(@NotNull IntConsumer i) {
            i.accept((int) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire int32(@NotNull IntConsumer i) {
            i.accept((int) bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire uint32(@NotNull LongConsumer i) {
            i.accept(bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire int64(@NotNull LongConsumer i) {
            i.accept(bytes.parseLong());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire float32(@NotNull FloatConsumer v) {
            v.accept((float) bytes.parseDouble());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire float64(@NotNull DoubleConsumer v) {
            v.accept(bytes.parseDouble());
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire time(@NotNull Consumer<LocalTime> localTime) {
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            localTime.accept(LocalTime.parse(sb.toString()));
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime) {
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            zonedDateTime.accept(ZonedDateTime.parse(sb.toString()));
            return TextWire.this;
        }

        @NotNull
        @Override
        public Wire date(@NotNull Consumer<LocalDate> localDate) {
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
        public WireIn expectText(@NotNull CharSequence s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn uuid(@NotNull Consumer<UUID> uuid) {
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            uuid.accept(UUID.fromString(sb.toString()));
            return TextWire.this;
        }

        @Override
        public WireIn int64array(LongArrayValues values, @NotNull Consumer<LongArrayValues> setter) {
            if (!(values instanceof TextLongReference)) {
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
            if (!(value instanceof TextLongReference)) {
                setter.accept(value = new TextLongReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
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
            return TextWire.this;
        }

        @Override
        public WireIn sequence(@NotNull Consumer<ValueIn> reader) {
            consumeWhiteSpace();
            int code = peekCode();
            if (code != '-')
                throw new IORuntimeException("Unsupported type " + (char) code);

            final long len = readLength();


            final long limit = bytes.limit();
            final long position = bytes.position();


            try {
                // ensure that you can read past the end of this sequence object
                final long newLimit = position + len;
                bytes.limit(newLimit);
                bytes.skip(1); // skip the [
                consumeWhiteSpace();
                reader.accept(TextWire.this.valueIn);
            } finally {
                bytes.limit(limit);
            }

            consumeWhiteSpace();
            return TextWire.this;
        }

        @Override
        public <T> T applyToMarshallable(Function<WireIn, T> marshallableReader) {
            consumeWhiteSpace();
            int code = peekCode();
            if (code != '{')
                throw new IORuntimeException("Unsupported type " + (char) code);

            final long len = readLength() - 1;


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
            if (code != '{')
                throw new IORuntimeException("Unsupported type " + (char) code);

            final long len = readLength() - 1;


            final long limit = bytes.limit();
            final long position = bytes.position();


            try {
                // ensure that you can read past the end of this marshable object
                final long newLimit = position - 1 + len;
                bytes.limit(newLimit);
                bytes.skip(1); // skip the {
                consumeWhiteSpace();
                object.readMarshallable(TextWire.this);
            } finally {
                bytes.limit(limit);
            }

            consumeWhiteSpace();
            code = readCode();
            if (code != '}')
                throw new IORuntimeException("Unterminated { while reading marshallable " +
                        object + "bytes=" + Bytes.toDebugString(bytes)
                );
            return TextWire.this;
        }


        @Override
        public <K, V> void map(@NotNull final Class<K> kClazz, @NotNull final Class<V> vClass, @NotNull final Map<K, V> usingMap) {

            usingMap.clear();

            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = sb.toString();
                if ("!!map".contentEquals(sb)) {
                    while (hasNext()) {

                        sequence(s -> s.marshallable(r -> {
                            try {
                                final K k = r.read(() -> "key").object(kClazz);
                                final V v = r.read(() -> "value").object(vClass);
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
            StringBuilder sb = Wires.acquireStringBuilder();
            bytes.parseUTF(sb, StopCharTesters.COMMA_STOP);
            if (StringInterner.isEqual(sb, "true"))
                return true;
            else if (StringInterner.isEqual(sb, "false"))
                return false;
            else if (StringInterner.isEqual(sb, "!!null"))
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

        @Override
        public long int64() {
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
         * returns
         *
         * @return true if !!null, if {@code true} reads the !!null up to the next STOP, if {@code false} no  data is
         * read  ( data is only peaked if {@code false} )
         */
        @Override
        public boolean isNull() {
            long pos = bytes.position();
            for (byte b : "!!null".getBytes()) {
                if (bytes.readByte(pos++) != b)
                    return false;
            }
            bytes.skipTo(StopCharTesters.COMMA_STOP);
            return true;
        }


        @Override
        @Nullable
        public <E> E object(@Nullable E using,
                            @NotNull Class<E> clazz) {

            if (Marshallable.class.isAssignableFrom(clazz)) {

                final E v;
                if (using == null)
                    try {
                        v = clazz.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
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


            } else {
                throw new IllegalStateException("unsupported type");
            }
        }


    }

}
