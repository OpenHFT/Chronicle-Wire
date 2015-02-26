package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.Threads;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.*;

import static net.openhft.chronicle.wire.WireType.*;

/**
 * Created by peter on 15/01/15.
 */
public class BinaryWire implements Wire {
    static final int NOT_READY = 1 << 31;
    static final int META_DATA = 1 << 30;
    public static final int ANY_CODE_MATCH = -1;
    static final int UNKNOWN_LENGTH = -1 >>> 2;
    static final int LENGTH_MASK = -1 >>> 2;
    static final StringBuilderPool SBP = new StringBuilderPool();

    final Bytes bytes;
    final ValueOut fixedValueOut = new FixedBinaryValueOut();
    final ValueOut valueOut;
    final ValueIn valueIn = new BinaryValueIn();
    private final boolean numericFields;
    private final boolean fieldLess;

    public BinaryWire(Bytes bytes) {
        this(bytes, false, false, false);
    }

    public BinaryWire(Bytes bytes, boolean fixed, boolean numericFields, boolean fieldLess) {
        this.numericFields = numericFields;
        this.fieldLess = fieldLess;
        this.bytes = bytes;
        valueOut = fixed ? fixedValueOut : new BinaryValueOut();
    }

    public static boolean isReady(int len) {
        return (len & NOT_READY) == 0;
    }

    public static boolean isDocument(int len) {
        return (len & META_DATA) == 0;
    }

    public static boolean isKnownLength(int len) {
        return (len & LENGTH_MASK) != UNKNOWN_LENGTH;
    }

    public static int toIntU30(long l, String error) {
        if (l < 0 || l >= UNKNOWN_LENGTH)
            throw new IllegalStateException(String.format(error, l));
        return (int) l;
    }

    @Override
    public Bytes bytes() {
        return bytes;
    }

    @Override
    public void copyTo(WireOut wire) {
        while (bytes.remaining() > 0) {
            int peekCode = peekCode();
            switch (peekCode >> 4) {
                case NUM0:
                case NUM1:
                case NUM2:
                case NUM3:
                case NUM4:
                case NUM5:
                case NUM6:
                case NUM7:
                    bytes.skip(1);
                    wire.writeValue().uint8(peekCode);
                    break;

                case CONTROL:
                    // PADDING
                    if (peekCode == 0x8F) {
                        bytes.skip(1);
                        break;
                    }
                    // PADDING32
                    if (peekCode == 0x8E) {
                        bytes.skip(1);
                        bytes.skip(bytes.readUnsignedInt());
                        break;
                    }
                    throw new UnsupportedOperationException();

                case FLOAT:
                    bytes.skip(1);
                    double d = readFloat0(peekCode);
                    wire.writeValue().float64(d);
                    break;

                case INT:
                    bytes.skip(1);
                    long l = readInt0(peekCode);
                    wire.writeValue().int64(l);
                    break;

                case SPECIAL:
                    copySpecial(wire, peekCode);
                    break;

                case FIELD0:
                case FIELD1:
                    StringBuilder fsb = readField(peekCode, ANY_CODE_MATCH, Wires.acquireStringBuilder());
                    wire.write(() -> fsb);
                    break;

                case STR0:
                case STR1:
                    bytes.skip(1);
                    StringBuilder sb = readText(peekCode, Wires.acquireStringBuilder());
                    wire.writeValue().text(sb);
                    break;

            }
        }
    }

    private void copySpecial(WireOut wire, int peekCode) {
        switch (peekCode) {
            case 0xB1: // COMMENT
            {
                bytes.skip(1);
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                wire.writeComment(sb);
                break;
            }
            case 0xB2: // HINT(0xB2),
            {
                bytes.skip(1);
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                break;
            }
            case 0xB3: // TIME(0xB3),
            case 0xB4: // ZONED_DATE_TIME(0xB4),
            case 0xB5: // DATE(0xB5),
                throw new UnsupportedOperationException();
            case 0xB6: // TYPE(0xB6),
            {
                bytes.skip(1);
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                wire.writeValue().type(sb);
                break;
            }
            case 0xB7: // FIELD_NAME_ANY(0xB7),
                StringBuilder fsb = readField(peekCode, ANY_CODE_MATCH, Wires.acquireStringBuilder());
                wire.write(() -> fsb);
                break;
            case 0xB8: // STRING_ANY(0xB8),
                bytes.skip(1);
                StringBuilder sb = readText(peekCode, Wires.acquireStringBuilder());
                wire.writeValue().text(sb);
                break;
            case 0xB9: {
                bytes.skip(1);
                long code2 = bytes.readStopBit();
                wire.write(new WireKey() {
                    @Override
                    public String name() {
                        return null;
                    }

                    @Override
                    public int code() {
                        return (int) code2;
                    }
                });
                break;
            }

            // Boolean
            case 0xBD:
                bytes.skip(1);
                wire.writeValue().bool(null);
                break;
            case 0xBE: // FALSE(0xBE),
                bytes.skip(1);
                wire.writeValue().bool(false);
                break;
            case 0xBF: // TRUE(0xBF),
                bytes.skip(1);
                wire.writeValue().bool(true);
                break;
            default:
                throw new UnsupportedOperationException(WireType.stringForCode(peekCode));
        }
    }

    private void consumeSpecial() {
        while (true) {
            int code = peekCode();
            switch (code) {
                case 0x8F: // PADDING
                    bytes.skip(1);
                    break;
                case 0x8E: // PADDING32
                    bytes.skip(1);
                    bytes.skip(bytes.readUnsignedInt());
                    break;
                case 0xB1: // COMMENT
                {
                    bytes.skip(1);
                    StringBuilder sb = Wires.acquireStringBuilder();
                    bytes.readUTFΔ(sb);
                    break;
                }
                case 0xB2: // HINT(0xB2),
                {
                    bytes.skip(1);
                    StringBuilder sb = Wires.acquireStringBuilder();
                    bytes.readUTFΔ(sb);
                    break;
                }
                default:
                    return;
            }
        }
    }

    long readInt(int code) {
        if (code < 128)
            return code;
        switch (code >> 4) {
            case SPECIAL:
                switch (code) {
                    case 0xBE: // FALSE(0xBE),
                        return 0;
                    case 0xBF: // TRUE(0xBF),
                        return 1;
                }
                break;
            case FLOAT:
                double d = readFloat0(code);
                return (long) d;

            case INT:
                return readInt0(code);

        }
        throw new UnsupportedOperationException(WireType.stringForCode(code));
    }

    private double readFloat(int code) {
        if (code < 128)
            return code;
        switch (code >> 4) {
            case FLOAT:
                return readFloat0(code);

            case INT:
                return readInt0(code);

        }
        throw new UnsupportedOperationException(WireType.stringForCode(code));
    }

    private long readInt0(int code) {
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

    private double readFloat0(int code) {
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
    public ValueOut write() {
        if (!fieldLess) {
            writeField("");
        }
        return valueOut;
    }

    @Override
    public ValueOut writeValue() {
        return valueOut;
    }

    @Override
    public ValueOut write(WireKey key) {
        if (!fieldLess) {
            if (numericFields)
                writeField(key.code());
            else
                writeField(key.name());
        }
        return valueOut;
    }

    private void writeField(int code) {
        writeCode(FIELD_NUMBER);
        bytes.writeStopBit(code);
    }

    private void writeField(CharSequence name) {
        int len = name.length();
        if (len < 0x20) {
            if (len > 0 && Character.isDigit(name.charAt(0))) {
                try {
                    writeField(Integer.parseInt(name.toString()));
                    return;
                } catch (NumberFormatException ignored) {
                }
            }
            bytes.writeByte((byte) (FIELD_NAME0.code + len))
                    .append(name);
        } else {
            writeCode(FIELD_NAME_ANY).writeUTFΔ(name);
        }
    }

    private Bytes writeCode(WireType wireType) {
        return bytes.writeByte((byte) wireType.code);
    }

    @Override
    public ValueIn read() {
        readField(Wires.acquireStringBuilder(), ANY_CODE_MATCH);
        return valueIn;
    }

    @Override
    public ValueIn read(WireKey key) {
        StringBuilder sb = readField(Wires.acquireStringBuilder(), key.code());
        if (fieldLess || (sb != null && (sb.length() == 0 || StringInterner.isEqual(sb, key.name()))))
            return valueIn;
        throw new UnsupportedOperationException("Unordered fields not supported yet.");
    }

    @Override
    public ValueIn read(StringBuilder name) {
        readField(name, ANY_CODE_MATCH);
        return valueIn;
    }

    private StringBuilder readField(StringBuilder name, int codeMatch) {
        consumeSpecial();
        int peekCode = peekCode();
        return readField(peekCode, codeMatch, name);
    }

    private StringBuilder readField(int peekCode, int codeMatch, StringBuilder sb) {
        switch (peekCode >> 4) {
            case SPECIAL:
                if (peekCode == FIELD_NAME_ANY.code) {
                    bytes.skip(1);
                    bytes.readUTFΔ(sb);
                    return sb;
                }
                if (peekCode == FIELD_NUMBER.code) {
                    bytes.skip(1);
                    long fieldId = bytes.readStopBit();
                    if (codeMatch >= 0 && fieldId != codeMatch)
                        throw new UnsupportedOperationException("Field was: " + fieldId + " expected " + codeMatch);
                    if (codeMatch < 0)
                        sb.append(fieldId);
                    return sb;
                }
                return null;
            case FIELD0:
            case FIELD1:
                bytes.skip(1);
                return getStringBuilder(peekCode, sb);
            default:
                break;
        }
        // if field-less accept anything in order.
        if (fieldLess) {
            sb.setLength(0);
            return sb;
        }

        return null;
    }

    StringBuilder readText(int code, StringBuilder sb) {
        switch (code >> 4) {
            case SPECIAL:
                if (code == STRING_ANY.code) {
                    bytes.readUTFΔ(sb);
                    return sb;
                }
                return null;
            case STR0:
            case STR1:
                return getStringBuilder(code, sb);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private int peekCode() {
        if (bytes.remaining() < 1)
            return END_OF_BYTES.code;
        long pos = bytes.position();
        return bytes.readUnsignedByte(pos);
    }

    private int readCode() {
        if (bytes.remaining() < 1)
            return END_OF_BYTES.code;
        return bytes.readUnsignedByte();
    }

    private StringBuilder getStringBuilder(int code, StringBuilder sb) {
        sb.setLength(0);
        BytesUtil.parseUTF(bytes, sb, code & 0x1f);
        return sb;
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
        return bytes.toDebugString(bytes.capacity());
    }

    @Override
    public Wire writeComment(CharSequence s) {
        writeCode(COMMENT);
        bytes.writeUTFΔ(s);
        return BinaryWire.this;
    }

    @Override
    public Wire readComment(StringBuilder s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WireOut addPadding(int paddingToAdd) {
        if (paddingToAdd < 0)
            throw new IllegalStateException("Cannot add " + paddingToAdd + " bytes of padding");
        if (paddingToAdd >= 5) {
            writeCode(PADDING32)
                    .writeUnsignedInt(paddingToAdd - 5)
                    .skip(paddingToAdd - 5);
        } else {
            for (int i = 0; i < paddingToAdd; i++)
                writeCode(PADDING);
        }
        return this;
    }

    @Override
    public <T> T readDocument(Function<WireIn, T> reader, Consumer<WireIn> metaDataReader) {
        int length;
        for (; ; ) {
            length = bytes.peakVolatileInt();
            if (length == 0)
                return null;
            if (isDocument(length)) {
                if (reader == null)
                    throw new IllegalStateException("Expected meta data but found a document of length " + length30(length));
                if (isReady(length))
                    return readDocument(reader, length);
                // try again.
            } else {
                if (metaDataReader == null) {
                    // no need to wait for meta data
                    if (isKnownLength(length)) {
                        int length2 = length30(length) + 4;
                        if (bytes.remaining() < length2)
                            throw new IllegalStateException();
                        bytes.skip(length2);
                    }
                    // try again.
                } else if (isReady(length)) {
                    readMetaData(metaDataReader, length);
                    // if we are only looking for meta data, stop after one.
                    if (reader == null)
                        return null;
                    // try again.
                }
            }
            Threads.checkInterrupted();
        }
    }

    private <T> T readDocument(Function<WireIn, T> reader, int length) {
        // consume the length
        bytes.readInt();
        long limit = bytes.limit();
        bytes.limit(bytes.position() + length30(length));
        try {
            return reader.apply(this);
        } finally {
            bytes.limit(limit);
        }
    }

    private void readMetaData(Consumer<WireIn> metaDataReader, int length) {
        // consume the length
        bytes.readInt();
        long limit = bytes.limit();
        long limit2 = bytes.position() + length30(length);
        bytes.limit(limit2);
        try {
            metaDataReader.accept(this);
        } finally {
            bytes.position(limit2);
            bytes.limit(limit);
        }
    }

    private int length30(int length) {
        return length & LENGTH_MASK;
    }

    @Override
    public void writeDocument(Runnable writer) {
        long position = bytes.position();
        bytes.writeInt(NOT_READY | UNKNOWN_LENGTH);
        writer.run();
        int length = toIntU30(bytes.position() - position - 4, "Document length %,d out of 30-bit int range.");
        bytes.writeOrderedInt(position, length);
    }

    @Override
    public void writeMetaData(Runnable writer) {
        long position = bytes.position();
        bytes.writeInt(NOT_READY | META_DATA | UNKNOWN_LENGTH);
        writer.run();
        int length = META_DATA | toIntU30(bytes.position() - position - 4, "Document length %,d out of 30-bit int range.");
        bytes.writeOrderedInt(position, length);
    }

    class FixedBinaryValueOut implements ValueOut {
        @Override
        public WireOut marshallable(Marshallable object) {
            writeCode(BYTES_LENGTH32);
            long position = bytes.position();
            bytes.writeInt(0);
            object.writeMarshallable(BinaryWire.this);
            bytes.writeOrderedInt(position, Maths.toInt32(bytes.position() - position - 4, "Document length %,d out of 32-bit int range."));
            return BinaryWire.this;
        }

        @Override
        public WireOut text(CharSequence s) {
            if (s == null) {
                writeCode(NULL);
            } else {
                int len = s.length();
                if (len < 0x20) {
                    bytes.writeByte((byte) (STRING0.code + len)).append(s);
                } else {
                    writeCode(STRING_ANY).writeUTFΔ(s);
                }
            }

            return BinaryWire.this;
        }

        @Override
        public WireOut type(CharSequence typeName) {
            writeCode(TYPE).writeUTFΔ(typeName);
            return BinaryWire.this;
        }

        @Override
        public WireOut bool(Boolean flag) {
            bytes.writeUnsignedByte(flag == null
                    ? NULL.code
                    : flag ? TRUE.code : FALSE.code);
            return BinaryWire.this;
        }

        @Override
        public WireOut int8(byte i8) {
            writeCode(INT8).writeByte(i8);
            return BinaryWire.this;
        }

        @Override
        public WireOut bytes(Bytes fromBytes) {
            writeLength(Maths.toInt32(fromBytes.remaining() + 1));
            writeCode(U8_ARRAY);
            bytes.write(fromBytes);
            return BinaryWire.this;
        }

        @Override
        public WireOut bytes(byte[] fromBytes) {
            writeLength(Maths.toInt32(fromBytes.length + 1));
            writeCode(U8_ARRAY);
            bytes.write(fromBytes);
            return BinaryWire.this;
        }

        @Override
        public WireOut uint8checked(int u8) {
            writeCode(UINT8).writeUnsignedByte(u8);
            return BinaryWire.this;
        }

        @Override
        public WireOut int16(short i16) {
            writeCode(INT16).writeShort(i16);
            return BinaryWire.this;
        }

        @Override
        public WireOut uint16checked(int u16) {
            writeCode(UINT16).writeUnsignedShort(u16);
            return BinaryWire.this;
        }

        @Override
        public WireOut utf8(int codepoint) {
            writeCode(UINT16);
            BytesUtil.appendUTF(bytes, codepoint);
            return BinaryWire.this;
        }

        @Override
        public WireOut int32(int i32) {
            writeCode(INT32).writeInt(i32);
            return BinaryWire.this;
        }

        @Override
        public WireOut uint32checked(long u32) {
            writeCode(UINT32).writeUnsignedInt(u32);
            return BinaryWire.this;
        }

        @Override
        public WireOut float32(float f) {
            writeCode(FLOAT32).writeFloat(f);
            return BinaryWire.this;
        }

        @Override
        public WireOut float64(double d) {
            writeCode(FLOAT64).writeDouble(d);
            return BinaryWire.this;
        }

        @Override
        public WireOut int64(long i64) {
            return fixedInt64(i64);
        }

        private WireOut fixedInt64(long i64) {
            writeCode(INT64).writeLong(i64);
            return BinaryWire.this;
        }

        @Override
        public WireOut time(LocalTime localTime) {
            writeCode(TIME).writeUTFΔ(localTime.toString());
            return BinaryWire.this;
        }

        @Override
        public WireOut zonedDateTime(ZonedDateTime zonedDateTime) {
            writeCode(ZONED_DATE_TIME).writeUTFΔ(zonedDateTime.toString());
            return BinaryWire.this;
        }

        @Override
        public WireOut date(LocalDate localDate) {
            writeCode(DATE).writeUTFΔ(localDate.toString());
            return BinaryWire.this;
        }

        @Override
        public WireOut uuid(UUID uuid) {
            writeCode(UUID).writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits());
            return BinaryWire.this;
        }

        @Override
        public WireOut int64(LongValue longValue) {
            int fromEndOfCacheLine = (int) ((-bytes.position()) & 63);
            if (fromEndOfCacheLine < 9)
                addPadding(fromEndOfCacheLine - 1);
            fixedInt64(longValue.getValue());
            return BinaryWire.this;
        }

        @Override
        public WireOut int32(IntValue value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut sequence(Runnable writer) {
            throw new UnsupportedOperationException();
        }


        public ValueOut writeLength(long length) {
            if (length < 0) {
                throw new IllegalArgumentException("Invalid length " + length);
            } else if (length < 1 << 8) {
                writeCode(BYTES_LENGTH8);
                bytes.writeUnsignedByte((int) length);
            } else if (length < 1 << 16) {
                writeCode(BYTES_LENGTH16);
                bytes.writeUnsignedShort((int) length);
            } else {
                writeCode(BYTES_LENGTH32);
                bytes.writeUnsignedInt(length);
            }
            return this;
        }
    }

    class BinaryValueOut extends FixedBinaryValueOut {
        void writeInt(long l) {
            if (l < Integer.MIN_VALUE) {
                if (l == (float) l)
                    super.float32(l);
                else
                    super.int64(l);
            } else if (l < Short.MIN_VALUE) {
                super.int32((int) l);
            } else if (l < Byte.MIN_VALUE) {
                super.int16((short) l);
            } else if (l < 0) {
                super.int8((byte) l);
            } else if (l < 128) {
                bytes.writeUnsignedByte((int) l);
            } else if (l < 1 << 8) {
                super.uint8checked((short) l);
            } else if (l < 1 << 16) {
                super.uint16checked((int) l);
            } else if (l < 1L << 32) {
                super.uint32checked(l);
            } else {
                if (l == (float) l)
                    super.float32(l);
                else
                    super.int64(l);
            }
        }

        void writeFloat(double d) {
            if (d < 1L << 32 && d >= -1L << 31) {
                long l = (long) d;
                if (l == d) {
                    writeInt(l);
                    return;
                }
            }
            float f = (float) d;
            if (f == d)
                super.float32(f);
            else
                super.float64(d);
        }

        @Override
        public WireOut int8(byte i8) {
            writeInt(i8);
            return BinaryWire.this;
        }

        @Override
        public WireOut uint8checked(int u8) {
            writeInt(u8);
            return BinaryWire.this;
        }

        @Override
        public WireOut int16(short i16) {
            writeInt(i16);
            return BinaryWire.this;
        }

        @Override
        public WireOut uint16checked(int u16) {
            writeInt(u16);
            return BinaryWire.this;
        }

        @Override
        public WireOut int32(int i32) {
            writeInt(i32);
            return BinaryWire.this;
        }

        @Override
        public WireOut uint32checked(long u32) {
            writeInt(u32);
            return BinaryWire.this;
        }

        @Override
        public WireOut float32(float f) {
            writeFloat(f);
            return BinaryWire.this;
        }

        @Override
        public WireOut float64(double d) {
            writeFloat(d);
            return BinaryWire.this;
        }

        @Override
        public WireOut int64(long i64) {
            writeInt(i64);
            return BinaryWire.this;
        }
    }

    class BinaryValueIn implements ValueIn {
        @Override
        public boolean hasNext() {
            return false;
        }

        public WireIn bytes(Bytes toBytes) {
            long length = readLength();
            int code = readCode();
            if (code != U8_ARRAY.code)
                cantRead(code);
            bytes.withLength(length - 1, toBytes::write);
            return wireIn();
        }

        public WireIn bytes(Consumer<byte[]> bytesConsumer) {
            long length = readLength();
            int code = readCode();
            if (code != U8_ARRAY.code)
                cantRead(code);
            byte[] byteArray = new byte[Maths.toInt32(length - 1)];
            bytes.read(byteArray);
            bytesConsumer.accept(byteArray);
            return wireIn();
        }
        @Override
        public WireIn type(StringBuilder s) {
            int code = readCode();
            if (code == TYPE.code) {
                bytes.readUTFΔ(s);
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public WireIn text(StringBuilder s) {
            int code = readCode();
            StringBuilder text = readText(code, s);
            if (text == null)
                cantRead(code);
            return BinaryWire.this;
        }

        @Override
        public WireIn bool(BooleanConsumer flag) {
            consumeSpecial();
            int code = readCode();
            switch (code) {
                case 0xBD: // NULL
                    // todo take the default.
                    flag.accept(null);
                    break;
                case 0xBE: // FALSE(0xBE),
                    flag.accept(false);
                    break;
                case 0xBF: // TRUE(0xBF),
                    flag.accept(true);
                    break;
                default:
                    return cantRead(code);
            }
            return BinaryWire.this;
        }

        private WireIn cantRead(int code) {
            throw new UnsupportedOperationException(stringForCode(code));
        }

        @Override
        public WireIn int8(ByteConsumer i) {
            consumeSpecial();
            i.accept((byte) readInt(bytes.readUnsignedByte()));
            return BinaryWire.this;
        }

        @Override
        public WireIn wireIn() {
            return BinaryWire.this;
        }

        @Override
        public WireIn uint8(ShortConsumer i) {
            consumeSpecial();
            i.accept((short) readInt(readCode()));
            return BinaryWire.this;
        }

        @Override
        public WireIn int16(ShortConsumer i) {
            i.accept((short) readInt(readCode()));
            return BinaryWire.this;
        }

        @Override
        public WireIn uint16(IntConsumer i) {
            consumeSpecial();
            i.accept((int) readInt(readCode()));
            return BinaryWire.this;
        }

        @Override
        public WireIn uint32(LongConsumer i) {
            consumeSpecial();
            i.accept(readInt(readCode()));
            return BinaryWire.this;
        }

        @Override
        public WireIn int32(IntConsumer i) {
            consumeSpecial();
            i.accept((int) readInt(readCode()));
            return BinaryWire.this;
        }

        @Override
        public WireIn float32(FloatConsumer v) {
            consumeSpecial();
            v.accept((float) readFloat(readCode()));
            return BinaryWire.this;
        }

        @Override
        public WireIn float64(DoubleConsumer v) {
            v.accept(readFloat(readCode()));
            return BinaryWire.this;
        }

        @Override
        public WireIn int64(LongConsumer i) {
            i.accept(readInt(readCode()));
            return BinaryWire.this;
        }

        @Override
        public WireIn time(Consumer<LocalTime> localTime) {
            consumeSpecial();
            int code = readCode();
            if (code == TIME.code) {
                StringBuilder sb = SBP.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                localTime.accept(LocalTime.parse(sb));
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public WireIn zonedDateTime(Consumer<ZonedDateTime> zonedDateTime) {
            consumeSpecial();
            int code = readCode();
            if (code == ZONED_DATE_TIME.code) {
                StringBuilder sb = SBP.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                zonedDateTime.accept(ZonedDateTime.parse(sb));
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public WireIn date(Consumer<LocalDate> localDate) {
            consumeSpecial();
            int code = readCode();
            if (code == DATE.code) {
                StringBuilder sb = SBP.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                localDate.accept(LocalDate.parse(sb));
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public WireIn text(Consumer<String> s) {
            consumeSpecial();
            int code = readCode();
            switch (code) {
                case 0xBD: // NULL
                    s.accept(null);
                    break;
                case 0xB8: // STRING_ANY
                    s.accept(bytes.readUTFΔ());
                    break;
                default:
                    if (code >= STRING0.code && code <= STRING30.code) {
                        StringBuilder sb = SBP.acquireStringBuilder();
                        BytesUtil.parseUTF(bytes, sb, code & 0b11111);
                        s.accept(sb.toString());
                    } else {
                        cantRead(code);
                    }
            }
            return BinaryWire.this;
        }

        @Override
        public WireIn expectText(CharSequence s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireIn uuid(Consumer<UUID> uuid) {
            consumeSpecial();
            int code = readCode();
            if (code == UUID.code) {
                uuid.accept(new UUID(bytes.readLong(), bytes.readLong()));
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public WireIn int64(LongValue value) {
            consumeSpecial();
            int code = readCode();
            if (code != INT64.code)
                cantRead(code);
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytes(bytes, bytes.position(), length);
            bytes.skip(length);
            return BinaryWire.this;
        }

        @Override
        public long int64() {
            consumeSpecial();
            int code = readCode();
            if (code != INT64.code)
                cantRead(code);
            return bytes.readLong();
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
        public WireIn marshallable(Marshallable object) {
            consumeSpecial();
            long length = readLength();
            if (length >= 0) {
                long limit = bytes.limit();
                long limit2 = bytes.position() + length;
                bytes.limit(limit2);
                try {
                    object.readMarshallable(BinaryWire.this);
                } finally {
                    bytes.limit(limit);
                    bytes.position(limit2);
                }
            } else {
                object.readMarshallable(BinaryWire.this);
            }
            return BinaryWire.this;
        }

        @Override
        public long readLength() {
            int code = peekCode();
            // TODO handle non length types as well.
            switch (code) {
                case 0x80:
                    bytes.skip(1);
                    return bytes.readUnsignedByte();
                case 0x81:
                    bytes.skip(1);
                    return bytes.readUnsignedShort();
                case 0x82:
                    bytes.skip(1);
                    return bytes.readUnsignedInt();
                default:
                    return ANY_CODE_MATCH;
            }
        }
    }
}
