package net.openhft.chronicle.wire;

import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;
import net.openhft.lang.io.*;
import net.openhft.lang.pool.StringInterner;
import net.openhft.lang.values.IntValue;
import net.openhft.lang.values.LongValue;

import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.*;

import static net.openhft.chronicle.wire.WireType.stringForCode;

/**
 * Created by peter on 15/01/15.
 */
public class TextWire implements Wire {
    public static final String FIELD_SEP = "";
    private static final String END_FIELD = "\n";
    final AbstractBytes bytes;
    final ValueOut valueOut = new TextValueOut();
    final ValueIn valueIn = new TextValueIn();
    String sep = "";

    public TextWire(Bytes bytes) {
        this.bytes = (AbstractBytes) bytes;
    }

    @Override
    public Bytes bytes() {
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
        String name = key.name();
        if (name == null) name = Integer.toString(key.code());
        bytes.append(sep).append(name).append(": ");
        sep = "";
        return valueOut;
    }

    @Override
    public ValueOut write(CharSequence name, WireKey template) {
        if (name == null) {
            return write(template);
        }
        bytes.append(sep).append(name.length() == 0 ? "\"\"" : quotes(name)).append(": ");
        sep = "";
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
                ch = peekCode();
                if (ch == ':')
                    bytes.skip(1);
                else
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
        StringBuilder sb = readField(Wires.acquireStringBuilder());
        if (sb.length() == 0 || StringInterner.isEqual(sb, key.name()))
            return valueIn;
        throw new UnsupportedOperationException("Unordered fields not supported yet.");
    }

    @Override
    public ValueIn read(StringBuilder name, WireKey template) {
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
    public void writeDocument(Runnable writer) {
        writer.run();
    }

    @Override
    public void writeMetaData(Runnable writer) {
        throw new UnsupportedOperationException();
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
        return false;
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
        @Override
        public Wire text(CharSequence s) {
            bytes.append(sep).append(s == null ? "!!null" : quotes(s)).append(END_FIELD);
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
            bytes.append(uuid.toString()).append('\n');
            return TextWire.this;
        }

        @Override
        public ValueOut cacheAlign() {
            throw new UnsupportedOperationException();
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
        public WireOut sequence(Runnable writer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut writeMarshallable(Marshallable object) {
            bytes.append("{ ");
            object.writeMarshallable(TextWire.this);
            bytes.append("}");
            return TextWire.this;
        }

        @Override
        public Wire bool(Boolean flag) {
            bytes.append(sep).append(flag == null ? "!!null" : flag ? "true" : "false").append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire int8(int i8) {
            bytes.append(sep).append(i8).append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire uint8(int u8) {
            bytes.append(sep).append(u8).append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire int16(int i16) {
            bytes.append(sep).append(i16).append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire uint16(int u16) {
            bytes.append(sep).append(u16).append(END_FIELD);
            sep = FIELD_SEP;
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
            bytes.append(sep).append(i32).append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire uint32(long u32) {
            bytes.append(sep).append(u32).append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire float32(float f) {
            bytes.append(sep).append(f).append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire float64(double d) {
            bytes.append(sep).append(d).append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire int64(long i64) {
            bytes.append(sep).append(i64).append(END_FIELD);
            sep = FIELD_SEP;
            return TextWire.this;
        }

        @Override
        public Wire hint(CharSequence s) {
            bytes.append(sep).append("##").append(s).append("\n");
            sep = "";
            return TextWire.this;
        }

        @Override
        public Wire mapStart() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire mapEnd() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire time(LocalTime localTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire zonedDateTime(ZonedDateTime zonedDateTime) {
            bytes.append(zonedDateTime.toString()).append('\n');
            return TextWire.this;
        }

        @Override
        public Wire date(LocalDate zonedDateTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire object(Marshallable type) {
            throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException();
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
        public WireIn readMarshallable(Marshallable object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire type(StringBuilder s) {
            int code = peekCode();
            if (code == '!') {
                bytes.skip(1);
                bytes.parseUTF(s, StopCharTesters.SPACE_STOP);
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return TextWire.this;
        }

        @Override
        public Wire text(StringBuilder s) {
            int ch = peekCode();
            StringBuilder sb = s;
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
            throw new UnsupportedOperationException();
        }

        @Override
        public String text() {
            throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire zonedDateTime(Consumer<ZonedDateTime> zonedDateTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire date(Consumer<LocalDate> zonedDateTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire object(Supplier<Marshallable> type) {
            throw new UnsupportedOperationException();
        }
    }

    public static String asText(Wire wire) {
        TextWire tw = new TextWire(new DirectStore(1024).bytes());
        wire.copyTo(tw);
        tw.flip();
        wire.flip();
        return tw.toString();
    }

}
