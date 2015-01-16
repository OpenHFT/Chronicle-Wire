package net.openhft.chronicle.wire;

import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.ShortConsumer;
import net.openhft.lang.io.AbstractBytes;
import net.openhft.lang.io.Bytes;
import net.openhft.lang.pool.StringInterner;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.function.*;

import static net.openhft.chronicle.wire.WireType.*;

/**
 * Created by peter on 15/01/15.
 */
public class BinaryWire implements Wire {
    final AbstractBytes bytes;
    final WriteValue<Wire> writeValue = new BinaryWriteValue();
    final ReadValue<Wire> readValue = new BinaryReadValue();

    public BinaryWire(Bytes bytes) {
        this.bytes = (AbstractBytes) bytes;
    }

    @Override
    public void copyTo(Wire wire) {
        while (bytes.remaining() > 0) {
            int code = bytes.readUnsignedByte();
            switch (code >> 4) {
                case NUM0:
                case NUM1:
                case NUM2:
                case NUM3:
                case NUM4:
                case NUM5:
                case NUM6:
                case NUM7:
                    wire.writeValue().uint8(code);
                    break;

                case CONTROL:
                    break;

                case FLOAT:
                    double d = readFloat(code);
                    wire.writeValue().float64(d);
                    break;

                case INT:
                    long l = readInt(code);
                    wire.writeValue().int64(l);
                    break;

                case SPECIAL:
                    copySpecial(wire, code);
                    break;

                case FIELD0:
                case FIELD1:
                    bytes.skip(-1);
                    StringBuilder fsb = readField(code, Wires.acquireStringBuilder());
                    wire.write(fsb, null);
                    break;

                case STR0:
                case STR1:
                    bytes.skip(-1);
                    StringBuilder sb = readText(code, Wires.acquireStringBuilder());
                    wire.writeValue().text(sb);
                    break;

            }
        }
    }

    private void copySpecial(Wire wire, int code) {
        switch (code) {
            case 0xB0: // PADDING
                break;
            case 0xB1: // COMMENT
            {
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                wire.writeValue().comment(sb);
                break;
            }
            case 0xB2: // HINT(0xB2),
            {
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                wire.writeValue().hint(sb);
                break;
            }
            case 0xB3: // TIME(0xB3),
            case 0xB4: // ZONED_DATE_TIME(0xB4),
            case 0xB5: // DATE(0xB5),
                throw new UnsupportedOperationException();
            case 0xB6: // TYPE(0xB6),
            {
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                wire.writeValue().type(sb);
                break;
            }
            case 0xB7: // FIELD_NAME_ANY(0xB7),
                bytes.skip(-1);
                StringBuilder fsb = readField(code, Wires.acquireStringBuilder());
                wire.write(fsb, null);
                break;
            case 0xB8: // STRING_ANY(0xB8),
                bytes.skip(-1);
                StringBuilder sb = readText(code, Wires.acquireStringBuilder());
                wire.writeValue().text(sb);
                break;
            // Boolean
            case 0xBD:
                wire.writeValue().flag(null);
                break;
            case 0xBE: // FALSE(0xBE),
                wire.writeValue().flag(false);
                break;
            case 0xBF: // TRUE(0xBF),
                wire.writeValue().flag(true);
                break;
            default:
                throw new UnsupportedOperationException(WireType.stringForCode(code));
        }
    }

    private long readInt(int code) {
        switch (code) {
            case 0xA0: // FIELD_NUMBER(0xA0),
                throw new UnsupportedOperationException();
            case 0xA1: // UTF8(0xA1),
                throw new UnsupportedOperationException();
            case 0xA2: //INT8(0xA2),
                return bytes.readByte();
            case 0xA3: //INT16(0xA3),
                return bytes.readShort();
            case 0xA4: //INT32(0xA4),
                return bytes.readInt();
            case 0xA5: //INT64(0xA5),
                return bytes.readLong();
            case 0xA6: //UINT8(0xA6),
                return bytes.readUnsignedByte();
            case 0xA7: //UINT16(0xA7),
                return bytes.readUnsignedShort();
            case 0xA8: //UINT32(0xA8),
                return bytes.readUnsignedInt();
            case 0xA9: //FIXED_6(0xA9),
                return bytes.readStopBit() * 1000000L;
            case 0xAA: //FIXED_5(0xAA),
                return bytes.readStopBit() * 100000L;
            case 0xAB: //FIXED_4(0xAB),
                return bytes.readStopBit() * 10000L;
            case 0xAC: //FIXED_3(0xAC),
                return bytes.readStopBit() * 1000L;
            case 0xAD: //FIXED_2(0xAD),
                return bytes.readStopBit() * 100L;
            case 0xAE: //FIXED_1(0xAE),
                return bytes.readStopBit() * 10L;
            case 0xAF: //FIXED(0xAF),
                return bytes.readStopBit();

        }
        throw new UnsupportedOperationException(WireType.stringForCode(code));
    }

    private double readFloat(int code) {
        switch (code) {
            case 0x90: // FLOAT32(0x90),
                return bytes.readFloat();
            case 0x91: // FLOAT64(0x91),
                return bytes.readDouble();
            case 0x92: // FIXED1(0x92),
                return bytes.readStopBit() / 1e1;
            case 0x93: // FIXED2(0x93),
                return bytes.readStopBit() / 1e2;
            case 0x94: // FIXED3(0x94),
                return bytes.readStopBit() / 1e3;
            case 0x95: // FIXED4(0x95),
                return bytes.readStopBit() / 1e4;
            case 0x96: // FIXED5(0x96),
                return bytes.readStopBit() / 1e5;
            case 0x97: // FIXED6(0x97),
                return bytes.readStopBit() / 1e6;
        }
        throw new UnsupportedOperationException(WireType.stringForCode(code));
    }

    @Override
    public WriteValue<Wire> write() {
        writeField("");
        return writeValue;
    }

    @Override
    public WriteValue<Wire> writeValue() {
        return writeValue;
    }

    @Override
    public WriteValue<Wire> write(WireKey key) {
        writeField(key.name());
        return writeValue;
    }

    private void writeField(CharSequence name) {
        int len = name.length();
        if (len < 0x20) {
            long pos = bytes.position();
            bytes.writeUTFΔ(name);
            bytes.writeUnsignedByte(pos, FIELD_NAME0.code + len);
        } else {
            bytes.writeUnsignedByte(FIELD_NAME_ANY.code);
            bytes.writeUTFΔ(name);
        }
    }

    @Override
    public WriteValue<Wire> write(CharSequence name, WireKey template) {
        writeField(name);
        return writeValue;
    }

    @Override
    public ReadValue<Wire> read() {
        readField(Wires.acquireStringBuilder());
        return readValue;
    }

    @Override
    public ReadValue<Wire> read(WireKey key) {
        StringBuilder sb = readField(Wires.acquireStringBuilder());
        if (sb.length() == 0 || StringInterner.isEqual(sb, key.name()))
            return readValue;
        throw new UnsupportedOperationException("Unordered fields not supported yet.");
    }

    @Override
    public ReadValue<Wire> read(Supplier<StringBuilder> name, WireKey template) {
        readField(name.get());
        return readValue;
    }

    private StringBuilder readField(StringBuilder name) {
        int code = peekCode();
        return readField(code, name);
    }

    private StringBuilder readField(int code, StringBuilder sb) {
        switch (code >> 4) {
            case SPECIAL:
                if (code == FIELD_NAME_ANY.code) {
                    bytes.skip(1);
                    bytes.readUTFΔ(sb);
                    return sb;
                }
                return null;
            case FIELD0:
            case FIELD1:
                return getStringBuilder(code, sb);
        }
        return null;
    }

    private StringBuilder readText(int code, StringBuilder sb) {
        switch (code >> 4) {
            case SPECIAL:
                if (code == STRING_ANY.code) {
                    bytes.skip(1);
                    bytes.readUTFΔ(sb);
                    return sb;
                }
                return null;
            case STR0:
            case STR1:
                return getStringBuilder(code, sb);
        }
        return null;
    }

    private int peekCode() {
        if (bytes.remaining() < 1)
            return DOCUMENT_END.code;
        long pos = bytes.position();
        return bytes.readUnsignedByte(pos);
    }

    private StringBuilder getStringBuilder(int code, StringBuilder sb) {
        bytes.skip(1);
        sb.setLength(0);
        try {
            AbstractBytes.readUTF0(bytes, sb, code & 0x1f);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return sb;
    }

    @Override
    public boolean hasNextSequenceItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readSequenceEnd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wire writeComment(CharSequence s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wire readComment(StringBuilder sb) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMapping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wire writeDocumentStart() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeDocumentEnd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasDocument() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void consumeDocumentEnd() {
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
        return bytes.toDebugString(bytes.capacity());
    }

    class BinaryWriteValue implements WriteValue<Wire> {
        @Override
        public Wire sequence(Object... array) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire sequence(Iterable array) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire sequenceStart() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire sequenceEnd() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long startLength(int bytes) {
            return 0;
        }

        @Override
        public boolean endLength(long startPosition) {
            return false;
        }

        @Override
        public Wire text(CharSequence s) {
            if (s == null) {
                bytes.writeUnsignedByte(NULL.code);
            } else {
                int len = s.length();
                if (len < 0x20) {
                    long pos = bytes.position();
                    bytes.writeUTFΔ(s);
                    bytes.writeUnsignedByte(pos, STRING0.code + len);
                } else {
                    bytes.writeUnsignedByte(STRING_ANY.code);
                    bytes.writeUTFΔ(s);
                }
            }

            return BinaryWire.this;
        }

        @Override
        public Wire type(CharSequence typeName) {
            bytes.writeUnsignedByte(TYPE.code);
            bytes.writeUTFΔ(typeName);
            return BinaryWire.this;
        }

        @Override
        public Wire flag(Boolean flag) {
            bytes.writeUnsignedByte(flag == null
                    ? NULL.code
                    : flag ? TRUE.code : FALSE.code);
            return BinaryWire.this;
        }

        @Override
        public Wire int8(int i8) {
            bytes.writeUnsignedByte(INT8.code);
            bytes.writeByte(i8);
            return BinaryWire.this;
        }

        @Override
        public Wire uint8(int u8) {
            bytes.writeUnsignedByte(UINT8.code);
            bytes.writeUnsignedByte(u8);
            return BinaryWire.this;
        }

        @Override
        public Wire int16(int i16) {
            bytes.writeUnsignedByte(INT16.code);
            bytes.writeShort(i16);
            return BinaryWire.this;
        }

        @Override
        public Wire uint16(int u16) {
            bytes.writeUnsignedByte(UINT16.code);
            bytes.writeUnsignedShort(u16);
            return BinaryWire.this;
        }

        @Override
        public Wire utf8(int codepoint) {
            bytes.writeUnsignedByte(UINT16.code);
            StringBuilder sb = Wires.acquireStringBuilder();
            sb.appendCodePoint(codepoint);
            AbstractBytes.writeUTF0(bytes, sb, 1);
            return BinaryWire.this;
        }

        @Override
        public Wire int32(int i32) {
            bytes.writeUnsignedByte(INT32.code);
            bytes.writeInt(i32);
            return BinaryWire.this;
        }

        @Override
        public Wire uint32(long u32) {
            bytes.writeUnsignedByte(UINT32.code);
            bytes.writeUnsignedInt(u32);
            return BinaryWire.this;
        }

        @Override
        public Wire float32(float f) {
            bytes.writeUnsignedByte(FLOAT32.code);
            bytes.writeFloat(f);
            return BinaryWire.this;
        }

        @Override
        public Wire float64(double d) {
            bytes.writeUnsignedByte(FLOAT64.code);
            bytes.writeDouble(d);
            return BinaryWire.this;
        }

        @Override
        public Wire int64(long i64) {
            bytes.writeUnsignedByte(INT64.code);
            bytes.writeLong(i64);
            return BinaryWire.this;
        }

        @Override
        public Wire comment(CharSequence s) {
            bytes.writeUnsignedByte(COMMENT.code);
            bytes.writeUTFΔ(s);
            return BinaryWire.this;
        }

        @Override
        public Wire hint(CharSequence s) {
            bytes.writeUnsignedByte(HINT.code);
            bytes.writeUTFΔ(s);
            return BinaryWire.this;
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
            throw new UnsupportedOperationException();
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

    class BinaryReadValue implements ReadValue<Wire> {
        @Override
        public Wire sequenceLength(IntConsumer length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire sequence(Supplier<Collection> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Wire type(Supplier<StringBuilder> s) {
            int code = peekCode();
            if (code == TYPE.code) {
                bytes.skip(1);
                bytes.readUTFΔ(s.get());
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire text(Supplier<StringBuilder> s) {
            int code = peekCode();
            StringBuilder text = readText(code, s.get());
            if (text == null)
                throw new UnsupportedOperationException(stringForCode(code));
            return BinaryWire.this;
        }

        @Override
        public Wire flag(BooleanConsumer flag) {
            int code = peekCode();
            switch (code) {
                case 0xBD: // NULL
                    flag.accept(null);
                    break;
                case 0xBE: // FALSE(0xBE),
                    flag.accept(false);
                    break;
                case 0xBF: // TRUE(0xBF),
                    flag.accept(true);
                    break;
                default:
                    throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire int8(ByteConsumer i) {
            int code = peekCode();
            if (code == INT8.code) {
                bytes.skip(1);
                i.accept(bytes.readByte());
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire uint8(ShortConsumer i) {
            int code = peekCode();
            if (code == UINT8.code) {
                bytes.skip(1);
                i.accept((short) bytes.readUnsignedByte());
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire int16(ShortConsumer i) {
            int code = peekCode();
            if (code == INT16.code) {
                bytes.skip(1);
                i.accept(bytes.readShort());
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire uint16(IntConsumer i) {
            int code = peekCode();
            if (code == UINT16.code) {
                bytes.skip(1);
                i.accept(bytes.readUnsignedShort());
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire uint32(LongConsumer i) {
            int code = peekCode();
            if (code == UINT32.code) {
                bytes.skip(1);
                i.accept(bytes.readUnsignedInt());
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire int32(IntConsumer i) {
            int code = peekCode();
            if (code == INT32.code) {
                bytes.skip(1);
                i.accept(bytes.readInt());
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire float64(DoubleConsumer v) {
            int code = peekCode();
            if (code == FLOAT64.code) {
                bytes.skip(1);
                v.accept(bytes.readDouble());
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire int64(LongConsumer i) {
            int code = peekCode();
            if (code == INT64.code) {
                bytes.skip(1);
                i.accept(bytes.readLong());
            } else {
                throw new UnsupportedOperationException(stringForCode(code));
            }
            return BinaryWire.this;
        }

        @Override
        public Wire comment(Supplier<StringBuilder> s) {
            throw new UnsupportedOperationException();
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
}
