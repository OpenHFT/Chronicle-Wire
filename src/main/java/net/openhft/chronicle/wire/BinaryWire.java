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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
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

import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

import static net.openhft.chronicle.bytes.BytesUtil.append;
import static net.openhft.chronicle.wire.BinaryWireCode.*;

/**
 * Created by peter.lawrey on 15/01/15.
 */
public class BinaryWire implements Wire, InternalWireIn {
    public static final int ANY_CODE_MATCH = -1;
    static final int END_OF_BYTES = -1;

    final Bytes<?> bytes;
    final ValueOut fixedValueOut = new FixedBinaryValueOut();
    final ValueOut valueOut;
    final ValueIn valueIn = new BinaryValueIn();

    private final boolean numericFields;

    private final boolean fieldLess;
    boolean ready;

    public BinaryWire(Bytes<?> bytes) {
        this(bytes, false, false, false);
    }

    public BinaryWire(Bytes<?> bytes, boolean fixed, boolean numericFields, boolean fieldLess) {
        this.numericFields = numericFields;
        this.fieldLess = fieldLess;
        this.bytes = bytes;
        valueOut = fixed ? fixedValueOut : new BinaryValueOut();
    }

    static int toIntU30(long l, String error) {
        if (l < 0 || l > Wires.LENGTH_MASK)
            throw new IllegalStateException(String.format(error, l));
        return (int) l;
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
        while (bytes.remaining() > 0) {
            int peekCode = peekCode();
            outerSwitch:
            switch (peekCode >> 4) {
                case BinaryWireHighCode.NUM0:
                case BinaryWireHighCode.NUM1:
                case BinaryWireHighCode.NUM2:
                case BinaryWireHighCode.NUM3:
                case BinaryWireHighCode.NUM4:
                case BinaryWireHighCode.NUM5:
                case BinaryWireHighCode.NUM6:
                case BinaryWireHighCode.NUM7:
                    bytes.skip(1);
                    wire.writeValue().uint8(peekCode);
                    break;

                case BinaryWireHighCode.CONTROL:
                    switch (peekCode) {
                        case PADDING:
                            bytes.skip(1);
                            break outerSwitch;
                        case PADDING32:
                            bytes.skip(1);
                            bytes.skip(bytes.readUnsignedInt());
                            break outerSwitch;
                        default:
                            throw new UnsupportedOperationException();
                    }

                case BinaryWireHighCode.FLOAT:
                    bytes.skip(1);
                    double d = readFloat0(peekCode);
                    wire.writeValue().float64(d);
                    break;

                case BinaryWireHighCode.INT:
                    bytes.skip(1);
                    long l = readInt0(peekCode);
                    wire.writeValue().int64(l);
                    break;

                case BinaryWireHighCode.SPECIAL:
                    copySpecial(wire, peekCode);
                    break;

                case BinaryWireHighCode.FIELD0:
                case BinaryWireHighCode.FIELD1:
                    StringBuilder fsb = readField(peekCode, ANY_CODE_MATCH, Wires.acquireStringBuilder());
                    wire.write(() -> fsb);
                    break;

                case BinaryWireHighCode.STR0:
                case BinaryWireHighCode.STR1:
                    bytes.skip(1);
                    StringBuilder sb = readText(peekCode, Wires.acquireStringBuilder());
                    wire.writeValue().text(sb);
                    break;

            }
        }
    }

    @Override
    public ValueIn read() {
        readField(Wires.acquireStringBuilder(), ANY_CODE_MATCH);
        return valueIn;
    }

    @Override
    public ValueIn read(@NotNull WireKey key) {
        StringBuilder sb = readField(Wires.acquireStringBuilder(), key.code());
        if (fieldLess || (sb != null && (sb.length() == 0 || StringInterner.isEqual(sb, key.name()))))
            return valueIn;
        throw new UnsupportedOperationException("Unordered fields not supported yet, " +
                "Expected=" + key.name() + " was: " + sb);
    }

    @Override
    public ValueIn readEventName(@NotNull StringBuilder name) {
        readField(name, ANY_CODE_MATCH);
        return valueIn;
    }

    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        readField(name, ANY_CODE_MATCH);
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

    private StringBuilder readField(StringBuilder name, int codeMatch) {
        consumeSpecial();
        int peekCode = peekCode();
        return readField(peekCode, codeMatch, name);
    }

    private void consumeSpecial() {
        while (true) {
            int code = peekCode();
            switch (code) {
                case PADDING:
                    bytes.skip(1);
                    break;
                case PADDING32:
                    bytes.skip(1);
                    bytes.skip(bytes.readUnsignedInt());
                    break;
                case COMMENT: {
                    bytes.skip(1);
                    StringBuilder sb = Wires.acquireStringBuilder();
                    bytes.readUTFΔ(sb);
                    break;
                }
                case HINT: {
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

    private int peekCode() {
        if (bytes.remaining() < 1)
            return END_OF_BYTES;
        long pos = bytes.position();
        return bytes.readUnsignedByte(pos);
    }

    private StringBuilder readField(int peekCode, int codeMatch, StringBuilder sb) {
        switch (peekCode >> 4) {
            case BinaryWireHighCode.SPECIAL:
                if (peekCode == FIELD_NAME_ANY || peekCode == EVENT_NAME) {
                    bytes.skip(1);
                    bytes.readUTFΔ(sb);
                    return sb;
                }
                if (peekCode == FIELD_NUMBER) {
                    bytes.skip(1);
                    long fieldId = bytes.readStopBit();
                    if (codeMatch >= 0 && fieldId != codeMatch)
                        throw new UnsupportedOperationException("Field was: " + fieldId + " expected " + codeMatch);
                    if (codeMatch < 0)
                        sb.append(fieldId);
                    return sb;
                }
                return null;
            case BinaryWireHighCode.FIELD0:
            case BinaryWireHighCode.FIELD1:
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

    private <ACS extends Appendable & CharSequence> ACS getStringBuilder(int code, ACS sb) {
        BytesUtil.setLength(sb, 0);
        BytesUtil.parseUTF(bytes, sb, code & 0x1f);
        return sb;
    }

    private void copySpecial(WireOut wire, int peekCode) {
        switch (peekCode) {
            case COMMENT: {
                bytes.skip(1);
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                wire.writeComment(sb);
                break;
            }
            case HINT: {
                bytes.skip(1);
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                break;
            }
            case TIME:
            case ZONED_DATE_TIME:
            case DATE_TIME:
                throw new UnsupportedOperationException();
            case TYPE: {
                bytes.skip(1);
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                wire.writeValue().type(sb);
                break;
            }
            case FIELD_NAME_ANY:
                StringBuilder fsb = readField(peekCode, ANY_CODE_MATCH, Wires.acquireStringBuilder());
                wire.write(() -> fsb);
                break;
            case STRING_ANY:
                bytes.skip(1);
                StringBuilder sb = readText(peekCode, Wires.acquireStringBuilder());
                wire.writeValue().text(sb);
                break;
            case FIELD_NUMBER: {
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
            case NULL:
                bytes.skip(1);
                wire.writeValue().bool(null);
                break;
            case FALSE:
                bytes.skip(1);
                wire.writeValue().bool(false);
                break;
            case TRUE:
                bytes.skip(1);
                wire.writeValue().bool(true);
                break;
            default:
                throw new UnsupportedOperationException(stringForCode(peekCode));
        }
    }

    long readInt(int code) {
        if (code < 128)
            return code;
        switch (code >> 4) {
            case BinaryWireHighCode.SPECIAL:
                switch (code) {
                    case FALSE:
                        return 0;
                    case TRUE:
                        return 1;
                }
                break;
            case BinaryWireHighCode.FLOAT:
                double d = readFloat0(code);
                return (long) d;

            case BinaryWireHighCode.INT:
                return readInt0(code);
        }
        throw new UnsupportedOperationException(stringForCode(code));
    }

    private double readFloat0(int code) {
        switch (code) {
            case FLOAT32:
                return bytes.readFloat();
            case FLOAT64:
                return bytes.readDouble();
/*            case FIXED1:
                return bytes.readStopBit() / 1e1;
            case FIXED2:
                return bytes.readStopBit() / 1e2;
            case FIXED3:
                return bytes.readStopBit() / 1e3;
            case FIXED4:
                return bytes.readStopBit() / 1e4;
            case FIXED5:
                return bytes.readStopBit() / 1e5;
            case FIXED6:
                return bytes.readStopBit() / 1e6;*/
        }
        throw new UnsupportedOperationException(stringForCode(code));
    }

    private long readInt0(int code) {
        switch (code) {
//            case UUID:
//                throw new UnsupportedOperationException();
//            case UTF8:
//                throw new UnsupportedOperationException();
            case INT8:
                return bytes.readByte();
            case INT16:
                return bytes.readShort();
            case INT32:
                return bytes.readInt();
            case INT64:
                return bytes.readLong();
            case UINT8:
                return bytes.readUnsignedByte();
            case UINT16:
                return bytes.readUnsignedShort();
            case UINT32:
                return bytes.readUnsignedInt();
/*
            case FIXED_6:
                return bytes.readStopBit() * 1000000L;
            case FIXED_5:
                return bytes.readStopBit() * 100000L;
            case FIXED_4:
                return bytes.readStopBit() * 10000L;
            case FIXED_3:
                return bytes.readStopBit() * 1000L;
            case FIXED_2:
                return bytes.readStopBit() * 100L;
            case FIXED_1:
                return bytes.readStopBit() * 10L;
            case FIXED:
                return bytes.readStopBit();
*/

        }
        throw new UnsupportedOperationException(stringForCode(code));
    }

    private double readFloat(int code) {
        if (code < 128)
            return code;
        switch (code >> 4) {
            case BinaryWireHighCode.FLOAT:
                return readFloat0(code);

            case BinaryWireHighCode.INT:
                return readInt0(code);

        }
        throw new UnsupportedOperationException(stringForCode(code));
    }

    @Override
    public ValueOut write() {
        if (!fieldLess) {
            writeField("");
        }
        return valueOut;
    }

    @Override
    public ValueOut writeEventName(WireKey key) {
        writeCode(EVENT_NAME).writeUTFΔ(key.name());
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
        writeCode(COMMENT);
        bytes.writeUTFΔ(s);
        return BinaryWire.this;
    }

    @Override
    public boolean hasDocument() {
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
            bytes.writeByte((byte) (FIELD_NAME0 + len))
                    .append(name);
        } else {
            writeCode(FIELD_NAME_ANY).writeUTFΔ(name);
        }
    }

    private void writeField(int code) {
        writeCode(FIELD_NUMBER);
        bytes.writeStopBit(code);
    }

    private Bytes writeCode(int code) {
        return bytes.writeByte((byte) code);
    }

    <ACS extends Appendable & CharSequence> ACS readText(int code, ACS sb) {
        if (code <= 127) {
            append(sb, code);
            return sb;
        }
        switch (code >> 4) {
            case BinaryWireHighCode.SPECIAL:
                switch (code) {
                    case NULL:
                        append(sb, "null");
                        return sb;
                    case TRUE:
                        append(sb, "true");
                        return sb;
                    case FALSE:
                        append(sb, "false");
                        return sb;
                    case TIME:
                    case DATE:
                    case DATE_TIME:
                    case ZONED_DATE_TIME:
                    case TYPE:
                    case STRING_ANY:
                        bytes.readUTFΔ(sb);
                        return sb;
                    default:
                        return null;
                }

            case BinaryWireHighCode.FLOAT:
                append(sb, readFloat(code));
                return sb;
            case BinaryWireHighCode.INT:
                append(sb, readInt(code));
                return sb;
            case BinaryWireHighCode.STR0:
            case BinaryWireHighCode.STR1:
                return getStringBuilder(code, sb);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private int readCode() {
        if (bytes.remaining() < 1)
            return END_OF_BYTES;
        return bytes.readUnsignedByte();
    }

    public String toString() {
        return bytes.toDebugString(bytes.capacity());
    }

    class FixedBinaryValueOut implements ValueOut {
        boolean nested = false;

        @Override
        public ValueOut leaf() {
            return this;
        }

        @Override
        public WireOut bool(Boolean flag) {
            bytes.writeUnsignedByte(flag == null
                    ? NULL
                    : (flag ? TRUE : FALSE));
            return BinaryWire.this;
        }

        @Override
        public WireOut text(CharSequence s) {
            if (s == null) {
                writeCode(NULL);
            } else {
                int len = s.length();
                if (len < 0x20) {
                    bytes.writeByte((byte) (STRING_0 + len)).append(s);
                } else {
                    writeCode(STRING_ANY).writeUTFΔ(s);
                }
            }

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
        public WireOut rawBytes(byte[] value) {
            throw new UnsupportedOperationException("todo");
        }

        public ValueOut writeLength(long length) {
            if (length < 0) {
                throw new IllegalArgumentException("Invalid length " + length);
            } /*else if (length < 1 << 8) {
                writeCode(BYTES_LENGTH8);
                bytes.writeUnsignedByte((int) length);
            } else if (length < 1 << 16) {
                writeCode(BYTES_LENGTH16);
                bytes.writeUnsignedShort((int) length);
            } */ else {
                writeCode(BYTES_LENGTH32);
                bytes.writeUnsignedInt(length);
            }
            return this;
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
        public WireOut int64(long i64) {
            return fixedInt64(i64);
        }

        private WireOut fixedInt64(long i64) {
            writeCode(INT64).writeLong(i64);
            return BinaryWire.this;
        }

        @Override
        public WireOut int64array(long capacity) {
            writeCode(I64_ARRAY);
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
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
            writeCode(DATE_TIME).writeUTFΔ(localDate.toString());
            return BinaryWire.this;
        }

        @Override
        public WireOut type(CharSequence typeName) {
            writeCode(TYPE).writeUTFΔ(typeName);
            return BinaryWire.this;
        }

        @Override
        public WireOut uuid(UUID uuid) {
            writeCode(UUID).writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits());
            return BinaryWire.this;
        }

        @Override
        public WireOut int32forBinding(int value) {
            int fromEndOfCacheLine = (int) ((-bytes.position()) & 63);
            if (fromEndOfCacheLine < 5)
                addPadding(fromEndOfCacheLine - 1);
            fixedInt64(value);
            return BinaryWire.this;
        }

        @Override
        public WireOut int64forBinding(long value) {
            int fromEndOfCacheLine = (int) ((-bytes.position()) & 63);
            if (fromEndOfCacheLine < 9)
                addPadding(fromEndOfCacheLine - 1);
            fixedInt64(value);
            return BinaryWire.this;
        }

        @Override
        public WireOut sequence(Consumer<ValueOut> writer) {
            writeCode(BYTES_LENGTH32);
            long position = bytes.position();
            bytes.writeInt(0);

            writer.accept(this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.position() - position - 4, "Document length %,d out of 32-bit int range."));
            return BinaryWire.this;
        }

        @Override
        public WireOut marshallable(WriteMarshallable object) {
            writeCode(BYTES_LENGTH32);
            long position = bytes.position();
            bytes.writeInt(0);

            object.writeMarshallable(BinaryWire.this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.position() - position - 4, "Document length %,d out of 32-bit int range."));
            return BinaryWire.this;
        }

        @Override
        public WireOut map(Map map) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireOut wireOut() {
            return BinaryWire.this;
        }
    }

    class BinaryValueOut extends FixedBinaryValueOut {
        @Override
        public WireOut int8(byte i8) {
            writeInt(i8);
            return BinaryWire.this;
        }

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
        public WireOut int64(long i64) {
            writeInt(i64);
            return BinaryWire.this;
        }

        @Override
        public WireOut float32(float f) {
            writeFloat(f);
            return BinaryWire.this;
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
        public WireOut float64(double d) {
            writeFloat(d);
            return BinaryWire.this;
        }
    }

    class BinaryValueIn implements ValueIn {
        @NotNull
        @Override
        public WireIn bool(@NotNull BooleanConsumer flag) {
            consumeSpecial();
            int code = readCode();
            switch (code) {
                case NULL:
                    // todo take the default.
                    flag.accept(null);
                    break;
                case FALSE:
                    flag.accept(false);
                    break;
                case TRUE:
                    flag.accept(true);
                    break;
                default:
                    return cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn text(@NotNull Consumer<String> s) {
            consumeSpecial();
            int code = readCode();
            switch (code) {
                case NULL:
                    s.accept(null);
                    break;
                case STRING_ANY:
                    s.accept(bytes.readUTFΔ());
                    break;
                default:
                    if (code >= STRING_0 && code <= STRING_31) {
                        StringBuilder sb = Wires.acquireStringBuilder();
                        BytesUtil.parseUTF(bytes, sb, code & 0b11111);
                        s.accept(sb.toString());
                    } else {
                        cantRead(code);
                    }
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <ACS extends Appendable & CharSequence> WireIn text(@NotNull ACS s) {
            int code = readCode();
            ACS text = readText(code, s);
            if (text == null)
                cantRead(code);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int8(@NotNull ByteConsumer i) {
            consumeSpecial();
            i.accept((byte) readInt(bytes.readUnsignedByte()));
            return BinaryWire.this;
        }

        @NotNull
        public WireIn bytes(@NotNull Bytes toBytes) {
            long length = readLength();
            int code = readCode();
            if (code != U8_ARRAY)
                cantRead(code);
            bytes.withLength(length - 1, toBytes::write);
            return wireIn();
        }

        @NotNull
        public WireIn bytes(@NotNull Consumer<WireIn> bytesConsumer) {
            long length = readLength();
            int code = readCode();
            if (code != U8_ARRAY)
                cantRead(code);

            if (length > bytes.remaining())
                throw new BufferUnderflowException();
            long limit0 = bytes.limit();
            long limit = bytes.position() + length;
            try {
                bytes.limit(limit);
                bytesConsumer.accept(wireIn());
            } finally {
                bytes.limit(limit0);
                bytes.position(limit);
            }
            return wireIn();
        }

        @Override
        public byte[] bytes() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return BinaryWire.this;
        }

        @Override
        public long readLength() {
            int code = peekCode();
            // TODO handle non length types as well.
            switch (code) {
/*
                case BYTES_LENGTH8:
                    bytes.skip(1);
                    return bytes.readUnsignedByte();
                case BYTES_LENGTH16:
                    bytes.skip(1);
                    return bytes.readUnsignedShort();
*/
                case BYTES_LENGTH32:
                    bytes.skip(1);
                    return bytes.readUnsignedInt();
                default:
                    return ANY_CODE_MATCH;
            }
        }

        @NotNull
        @Override
        public WireIn uint8(@NotNull ShortConsumer i) {
            consumeSpecial();
            i.accept((short) readInt(readCode()));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int16(@NotNull ShortConsumer i) {
            i.accept((short) readInt(readCode()));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn uint16(@NotNull IntConsumer i) {
            consumeSpecial();
            i.accept((int) readInt(readCode()));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntConsumer i) {
            consumeSpecial();
            i.accept((int) readInt(readCode()));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn uint32(@NotNull LongConsumer i) {
            consumeSpecial();
            i.accept(readInt(readCode()));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongConsumer i) {
            i.accept(readInt(readCode()));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn float32(@NotNull FloatConsumer v) {
            consumeSpecial();
            v.accept((float) readFloat(readCode()));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn float64(@NotNull DoubleConsumer v) {
            v.accept(readFloat(readCode()));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn time(@NotNull Consumer<LocalTime> localTime) {
            consumeSpecial();
            int code = readCode();
            if (code == TIME) {
                localTime.accept(readLocalTime());
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        private LocalTime readLocalTime() {
            StringBuilder sb = Wires.acquireStringBuilder();
            bytes.readUTFΔ(sb);
            return LocalTime.parse(sb);
        }

        @NotNull
        @Override
        public WireIn zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime) {
            consumeSpecial();
            int code = readCode();
            if (code == ZONED_DATE_TIME) {
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                zonedDateTime.accept(ZonedDateTime.parse(sb));
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn date(@NotNull Consumer<LocalDate> localDate) {
            consumeSpecial();
            int code = readCode();
            if (code == DATE_TIME) {
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                localDate.accept(LocalDate.parse(sb));
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public boolean hasNext() {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public boolean hasNextSequenceItem() {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireIn uuid(@NotNull Consumer<UUID> uuid) {
            consumeSpecial();
            int code = readCode();
            if (code == UUID) {
                uuid.accept(new UUID(bytes.readLong(), bytes.readLong()));
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public WireIn int64array(@Nullable LongArrayValues values, @NotNull Consumer<LongArrayValues> setter) {
            consumeSpecial();
            int code = readCode();
            if (code == I64_ARRAY) {
                if (!(values instanceof BinaryLongArrayReference))
                    setter.accept(values = new BinaryLongArrayReference());
                Byteable b = (Byteable) values;
                long length = BinaryLongArrayReference.peakLength(bytes, bytes.position());
                b.bytesStore(bytes, bytes.position(), length);
                bytes.skip(length);
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public WireIn int64(LongValue value, @NotNull Consumer<LongValue> setter) {
            consumeSpecial();
            int code = readCode();
            if (code != INT64)
                cantRead(code);

            // if the value is null, then we will create a LongDirectReference to write the data
            // into and then call setter.accept(), this will then update the value
            if (!(value instanceof BinaryLongReference)) {
                setter.accept(value = new BinaryLongReference());
            }

            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return BinaryWire.this;
        }

        @Override
        public WireIn int32(IntValue value, @NotNull Consumer<IntValue> setter) {
            consumeSpecial();
            int code = readCode();
            if (code != INT32)
                cantRead(code);
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 4) {
                setter.accept(value = new IntBinaryReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return BinaryWire.this;
        }

        @Override
        public WireIn sequence(@NotNull Consumer<ValueIn> reader) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T applyToMarshallable(Function<WireIn, T> marshallableReader) {
            consumeSpecial();
            long length = readLength();
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.position() + length;
                bytes.limit(limit2);
                try {
                    return marshallableReader.apply(BinaryWire.this);
                } finally {
                    bytes.limit(limit);
                    bytes.position(limit2);
                }
            } else {
                return marshallableReader.apply(BinaryWire.this);
            }
        }

        @NotNull
        @Override
        public WireIn type(@NotNull StringBuilder s) {
            int code = readCode();
            if (code == TYPE) {
                bytes.readUTFΔ(s);
            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) {
            consumeSpecial();
            long length = readLength();
            if (length >= 0) {
                long limit = bytes.readLimit();
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
        public <K extends ReadMarshallable, V extends ReadMarshallable> void typedMap(@NotNull Map<K, V> usingMap) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public <K, V> Map<K, V> map(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public boolean bool() {
            consumeSpecial();
            int code = readCode();
            if (code != UINT8)
                cantRead(code);

            switch (bytes.readUnsignedByte()) {
                case TRUE:
                    return true;
                case FALSE:
                    return false;
            }
            throw new IllegalStateException();
        }

        @Override
        public byte int8() {
            consumeSpecial();
            int code = readCode();
            if (code != INT8)
                cantRead(code);
            return bytes.readByte();
        }

        @Override
        public short int16() {
            consumeSpecial();
            int code = readCode();
            if (code != INT16)
                cantRead(code);
            return bytes.readShort();
        }

        @Override
        public int uint16() {
            return 0;
        }

        @Override
        public int int32() {
            consumeSpecial();
            int code = readCode();
            if (code != INT32)
                cantRead(code);
            return bytes.readInt();
        }

        @Override
        public long int64() {
            consumeSpecial();
            return readInt(readCode());
        }

        @Override
        public double float64() {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public float float32() {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public boolean isNull() {
            throw new UnsupportedOperationException();
        }

        private WireIn cantRead(int code) {
            throw new UnsupportedOperationException(stringForCode(code));
        }
    }
}
