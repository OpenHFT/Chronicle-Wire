package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.EscapingStopCharTester;
import net.openhft.chronicle.bytes.IORuntimeException;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;

import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.function.*;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static net.openhft.chronicle.wire.WireType.stringForCode;

/**
 * Created by peter.lawrey on 15/01/15.
 */
public class TextWire implements Wire {
    public static final String FIELD_SEP = "";
    private static final String END_FIELD = "\n";
    final Bytes<?> bytes;
    final ValueOut valueOut = new TextValueOut();
    final ValueIn valueIn = new TextValueIn();
    String sep = "";

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

    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    @Override
    public WireOut addPadding(int paddingToAdd) {
        for (int i = 0; i < paddingToAdd; i++)
            bytes.append((bytes.position() & 63) == 0 ? '\n' : ' ');
        return this;
    }

    @Override
    public void copyTo(WireOut wire) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueOut write() {
        bytes.append(sep).append("\"\": ");
        sep = "";
        return valueOut;
    }

    @Override
    public ValueOut writeValue() {
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
    public ValueIn read() {
        readField(Wires.acquireStringBuilder());
        return valueIn;
    }

    private void consumeWhiteSpace() {
        while (bytes.remaining() > 0 && Character.isWhitespace(bytes.readUnsignedByte(bytes.position())))
            bytes.skip(1);
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

            } else {
                bytes.parseUTF(sb, EscapingStopCharTester.escaping(c -> c < ' ' || c == ':'));
            }
            unescape(sb);
        } catch (BufferUnderflowException ignored) {
        }
        consumeWhiteSpace();
        return sb;
    }

    @Override
    public ValueIn read(WireKey key) {
        long position = bytes.position();
        StringBuilder sb = readField(Wires.acquireStringBuilder());
        if (sb.length() == 0 || StringInterner.isEqual(sb, key.name()))
            return valueIn;
        bytes.position(position);
        throw new UnsupportedOperationException("Unordered fields not supported yet. key=" + key.name());
    }

    @Override
    public ValueIn read(StringBuilder name) {
        consumeWhiteSpace();
        readField(name);
        return valueIn;
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

    @Override
    public boolean hasNextSequenceItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMapping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T readDocument(Function<WireIn, T> reader, Consumer<WireIn> metaDataReader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeDocument(Consumer<WireOut> writer) {
        writer.accept(this);
    }

    @Override
    public boolean hasDocument() {
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

    public String toString() {
        return bytes.toString();
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

    @Override
    public Wire writeComment(CharSequence s) {
        bytes.append(sep).append("# ").append(s).append("\n");
        sep = "";
        return TextWire.this;
    }

    @Override
    public Wire readComment(StringBuilder s) {
        throw new UnsupportedOperationException();
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

    class TextValueOut implements ValueOut {
        boolean nested = false;

        @Override
        public boolean isNested() {
            return nested;
        }

        @Override
        public WireOut nested(boolean nested) {
            this.nested = nested;
            return TextWire.this;
        }

        @Override
        public Wire text(CharSequence s) {
            if (" ".equals(sep) && startsWith(s, "//"))
                sep = "";
            bytes.append(sep).append(s == null ? "!!null" : quotes(s));
            separator();
            return TextWire.this;
        }

        public void separator() {
            if (isNested()) {
                sep = ", ";
            } else {
                bytes.append(END_FIELD);
                sep = "";
            }
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
        public Wire type(CharSequence typeName) {
            bytes.append(sep).append('!').append(typeName);
            sep = " ";
            return TextWire.this;
        }

        @Override
        public WireOut uuid(UUID uuid) {
            bytes.append(uuid.toString()).append('\n');
            return TextWire.this;
        }

        @Override
        public WireOut int64(LongValue readReady) {
            // TODO support this better
            bytes.append(readReady.getValue()).append('\n');
            return TextWire.this;
        }

        @Override
        public WireOut int32(IntValue value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut sequence(Consumer<ValueOut> writer) {
            boolean nested = isNested();
            nested(true);
            try {
                bytes.append(sep);
                sep = "";
                bytes.append("[ ");
                writer.accept(this);
                bytes.append(" ]");
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
        public Wire bool(Boolean flag) {
            bytes.append(sep).append(flag == null ? "!!null" : flag ? "true" : "false");
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
        public Wire int64(long i64) {
            bytes.append(sep).append(i64);
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
    }

    class TextValueIn implements ValueIn {
        @Override
        public boolean hasNext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn expectText(CharSequence s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn uuid(Consumer<UUID> uuid) {
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            uuid.accept(UUID.fromString(sb.toString()));
            return TextWire.this;
        }


        @Override
        public WireIn bytes(Bytes toBytes) {
            return bytes(toBytes::write);
        }

        public WireIn bytes(Consumer<byte[]> bytesConsumer) {
            // TODO needs to be made much more efficient.
            StringBuilder sb = Wires.acquireStringBuilder();
            if (peekCode() == '!') {
                bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                String str = sb.toString();
                if (str.equals("!!binary")) {
                    sb.setLength(0);
                    bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
                    byte[] decode = Base64.getDecoder().decode(sb.toString());
                    bytesConsumer.accept(decode);
                } else {
                    throw new IORuntimeException("Unsupported type " + str);
                }
            } else {
                text(sb);
                bytesConsumer.accept(sb.toString().getBytes());
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
                    case '{':

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

                    default:
                        // TODO needs to be made much more efficient.
                        bytes();
                        return bytes.position() - start;
                }
            } finally {
                bytes.position(start);
            }
        }

        @Override
        public WireIn int64(LongValue value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn int32(IntValue value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn sequence(Consumer<ValueIn> reader) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn marshallable(ReadMarshallable object) {
            consumeWhiteSpace();
            int code = readCode();
            if (code != '{')
                throw new IORuntimeException("Unsupported type " + (char) code);
            object.readMarshallable(TextWire.this);
            consumeWhiteSpace();
            code = readCode();
            if (code != '}')
                throw new IORuntimeException("Unterminated { while reading marshallable " + object);
            return TextWire.this;
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
        public Wire type(StringBuilder s) {
            int code = readCode();
            if (code != '!') {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            bytes.parseUTF(s, StopCharTesters.SPACE_STOP);
            return TextWire.this;
        }

        @Override
        public Wire text(StringBuilder s) {
            int ch = peekCode();
            StringBuilder sb = s;
            if (ch == '{') {
                sb.append(Bytes.toDebugString(bytes, bytes.position(), readLength()));
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

        @Override
        public WireIn text(Consumer<String> s) {
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

        @Override
        public Wire bool(BooleanConsumer flag) {
            StringBuilder sb = Wires.acquireStringBuilder();
            bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
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

        @Override
        public boolean bool() {
            StringBuilder sb = Wires.acquireStringBuilder();
            bytes.parseUTF(sb, StopCharTesters.SPACE_STOP);
            if (StringInterner.isEqual(sb, "true"))
                return true;
            else if (StringInterner.isEqual(sb, "false"))
                return false;
            else if (StringInterner.isEqual(sb, "!!null"))
                throw new NullPointerException("value is null");
            else
                throw new UnsupportedOperationException();
        }


        @Override
        public Wire int8(ByteConsumer i) {
            i.accept((byte) bytes.parseLong());
            return TextWire.this;
        }

        @Override
        public Wire uint8(ShortConsumer i) {
            i.accept((short) bytes.parseLong());
            return TextWire.this;
        }

        @Override
        public Wire int16(ShortConsumer i) {
            i.accept((short) bytes.parseLong());
            return TextWire.this;
        }

        @Override
        public Wire uint16(IntConsumer i) {
            i.accept((int) bytes.parseLong());
            return TextWire.this;
        }

        @Override
        public Wire uint32(LongConsumer i) {
            i.accept(bytes.parseLong());
            return TextWire.this;
        }

        @Override
        public Wire int32(IntConsumer i) {
            i.accept((int) bytes.parseLong());
            return TextWire.this;
        }

        @Override
        public Wire float32(FloatConsumer v) {
            v.accept((float) bytes.parseDouble());
            return TextWire.this;
        }

        @Override
        public Wire float64(DoubleConsumer v) {
            v.accept(bytes.parseDouble());
            return TextWire.this;
        }

        @Override
        public Wire int64(LongConsumer i) {
            i.accept(bytes.parseLong());
            return TextWire.this;
        }

        @Override
        public Wire time(Consumer<LocalTime> localTime) {
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            localTime.accept(LocalTime.parse(sb.toString()));
            return TextWire.this;
        }

        @Override
        public Wire zonedDateTime(Consumer<ZonedDateTime> zonedDateTime) {
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            zonedDateTime.accept(ZonedDateTime.parse(sb.toString()));
            return TextWire.this;
        }

        @Override
        public Wire date(Consumer<LocalDate> localDate) {
            StringBuilder sb = Wires.acquireStringBuilder();
            text(sb);
            localDate.accept(LocalDate.parse(sb.toString()));
            return TextWire.this;
        }
    }

}
