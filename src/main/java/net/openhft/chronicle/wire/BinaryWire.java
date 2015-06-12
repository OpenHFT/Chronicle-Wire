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

import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

import static net.openhft.chronicle.bytes.BytesUtil.append;
import static net.openhft.chronicle.bytes.BytesUtil.setLength;
import static net.openhft.chronicle.wire.BinaryWireCode.*;

/**
 * Created by peter.lawrey on 15/01/15.
 */
public class BinaryWire implements Wire, InternalWireIn {
    public static final int ANY_CODE_MATCH = -1;
    static final int END_OF_BYTES = -1;

    final Bytes<?> bytes;
    final ValueOut fixedValueOut = new FixedBinaryValueOut();
    @NotNull
    final ValueOut valueOut;
    final ValueIn valueIn = new BinaryValueIn();

    private final boolean numericFields;
    private final boolean fieldLess;
    boolean ready;

    public BinaryWire(Bytes bytes) {
        this(bytes, false, false, false);
    }

    public BinaryWire(Bytes bytes, boolean fixed, boolean numericFields, boolean fieldLess) {
        this.numericFields = numericFields;
        this.fieldLess = fieldLess;
        this.bytes = bytes;
        valueOut = fixed ? fixedValueOut : new BinaryValueOut();
    }


    static int toIntU30(long l, @NotNull String error) {
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

    @NotNull
    @Override
    public ValueIn read() {
        readField(Wires.acquireStringBuilder(), ANY_CODE_MATCH);
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        StringBuilder sb = readField(Wires.acquireStringBuilder(), key.code());

        if (fieldLess || (sb != null && (sb.length() == 0 || StringUtils.isEqual(sb, key.name()))))
            return valueIn;
        throw new UnsupportedOperationException("Unordered fields not supported yet, " +
                "Expected=" + key.name() + " was: " + sb);
    }

    @NotNull
    @Override
    public ValueIn readEventName(@NotNull StringBuilder name) {
        readField(name, ANY_CODE_MATCH);
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        readField(name, ANY_CODE_MATCH);
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

    @Nullable
    private StringBuilder readField(@NotNull StringBuilder name, int codeMatch) {
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

    private StringBuilder readField(int peekCode, int codeMatch, @NotNull StringBuilder sb) {
        switch (peekCode >> 4) {
            case BinaryWireHighCode.END_OF_STREAM:
                sb.setLength(0);
                break;

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
                // if it's not a field, perhaps none was written.
                break;
        }
        // if field-less accept anything in order.
        if (fieldLess) {
            sb.setLength(0);
            return sb;
        }

        return null;
    }

    @NotNull
    private <ACS extends Appendable & CharSequence> ACS getStringBuilder(int code, @NotNull ACS sb) {
        BytesUtil.setLength(sb, 0);
        BytesUtil.parseUTF(bytes, sb, code & 0x1f);
        return sb;
    }

    private void copySpecial(@NotNull WireOut wire, int peekCode) {
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

            case TYPE_PREFIX: {
                bytes.skip(1);
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                wire.writeValue().type(sb);
                break;
            }

            case TYPE_LITERAL: {
                bytes.skip(1);
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                wire.writeValue().typeLiteral(sb);
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
                    @Nullable
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

    long readInt0(int code) {
        if (isSmallInt(code))
            return code;

        switch (code) {
//            case UUID:
//                throw new UnsupportedOperationException();
//            case UTF8:
//                throw new UnsupportedOperationException();
            case INT8:
                return bytes.readByte();
            case UINT8:
                return bytes.readUnsignedByte();
            case INT16:
                return bytes.readShort();
            case UINT16:
                return bytes.readUnsignedShort();
            case INT32:
                return bytes.readInt();
            case UINT32:
                return bytes.readUnsignedInt();
            case INT64:
                return bytes.readLong();
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

    private boolean isSmallInt(int code) {
        return (code & 128) == 0;
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

    @NotNull
    @Override
    public ValueOut write() {
        if (!fieldLess) {
            writeField("");
        }
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut writeEventName(@NotNull WireKey key) {
        writeCode(EVENT_NAME).writeUTFΔ(key.name());
        return valueOut;
    }

    @NotNull
    @Override
    public ValueOut write(@NotNull WireKey key) {
        if (!fieldLess) {
            if (numericFields)
                writeField(key.code());
            else
                writeField(key.name());
        }
        return valueOut;
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
    public Wire writeComment(CharSequence s) {
        writeCode(COMMENT);
        bytes.writeUTFΔ(s);
        return BinaryWire.this;
    }

    @NotNull
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

    private void writeField(@NotNull CharSequence name) {
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

    @Nullable
    <ACS extends Appendable & CharSequence> ACS readText(int code, @NotNull ACS sb) {
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
                    case TYPE_LITERAL:
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
                throw new UnsupportedOperationException("code=0x" + String.format("%02X ", code).trim());
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

        @NotNull
        @Override
        public ValueOut leaf() {
            return this;
        }

        @NotNull
        @Override
        public WireOut bool(@Nullable Boolean flag) {
            bytes.writeUnsignedByte(flag == null
                    ? NULL
                    : (flag ? TRUE : FALSE));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable CharSequence s) {
            if (s == null) {
                writeCode(NULL);

            } else {
                int len = s.length();
                if (len < 0x20) {
                    bytes.writeUnsignedByte(STRING_0 + len).append(s);
                } else {
                    writeCode(STRING_ANY).writeUTFΔ(s);
                }
            }

            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            writeCode(INT8).writeByte(i8);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(@NotNull Bytes fromBytes) {
            writeLength(Maths.toInt32(fromBytes.remaining() + 1));
            writeCode(U8_ARRAY);
            bytes.write(fromBytes);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut rawBytes(byte[] value) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
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

        @NotNull
        @Override
        public WireOut bytes(@NotNull byte[] fromBytes) {
            writeLength(Maths.toInt32(fromBytes.length + 1));
            writeCode(U8_ARRAY);
            bytes.write(fromBytes);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            writeCode(UINT8).writeUnsignedByte(u8);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            writeCode(INT16).writeShort(i16);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            writeCode(UINT16).writeUnsignedShort(u16);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut utf8(int codepoint) {
            writeCode(UINT16);
            BytesUtil.appendUTF(bytes, codepoint);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            writeCode(INT32).writeInt(i32);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            writeCode(UINT32).writeUnsignedInt(u32);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            return fixedInt64(i64);
        }

        @NotNull
        private WireOut fixedInt64(long i64) {
            writeCode(INT64).writeLong(i64);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64array(long capacity) {
            writeCode(I64_ARRAY);
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut float32(float f) {
            writeCode(FLOAT32).writeFloat(f);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
            writeCode(FLOAT64).writeDouble(d);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut time(@NotNull LocalTime localTime) {
            writeCode(TIME).writeUTFΔ(localTime.toString());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            writeCode(ZONED_DATE_TIME).writeUTFΔ(zonedDateTime.toString());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            writeCode(DATE_TIME).writeUTFΔ(localDate.toString());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut type(CharSequence typeName) {
            writeCode(TYPE_PREFIX).writeUTFΔ(typeName);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull CharSequence type) {
            writeCode(TYPE_LITERAL).writeUTFΔ(type);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @NotNull Class type) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut uuid(@NotNull UUID uuid) {
            writeCode(UUID).writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value) {
            int fromEndOfCacheLine = (int) ((-bytes.position()) & 63);
            if (fromEndOfCacheLine < 5)
                addPadding(fromEndOfCacheLine - 1);
            fixedInt64(value);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            int fromEndOfCacheLine = (int) ((-bytes.position()) & 63);
            if (fromEndOfCacheLine < 9)
                addPadding(fromEndOfCacheLine - 1);
            fixedInt64(value);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut sequence(@NotNull Consumer<ValueOut> writer) {
            writeCode(BYTES_LENGTH32);
            long position = bytes.position();
            bytes.writeInt(0);

            writer.accept(this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.position() - position - 4, "Document length %,d out of 32-bit int range."));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) {
            writeCode(BYTES_LENGTH32);
            long position = bytes.position();
            bytes.writeInt(0);

            object.writeMarshallable(BinaryWire.this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.position() - position - 4, "Document length %,d out of 32-bit int range."));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut map(Map map) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireOut wireOut() {
            return BinaryWire.this;
        }
    }

    class BinaryValueOut extends FixedBinaryValueOut {
        @NotNull
        @Override
        public WireOut int8(byte i8) {
            writeNumber(i8);
            return BinaryWire.this;
        }

        void writeNumber(long l) {

            if (l >= 0 && l <= 127) {
                // used when the value is written directly into the code byte
                bytes.writeUnsignedByte((int) l);
                return;
            }

            if (l >= 0) {

                if (l <= (1 << 8) - 1) {
                    super.uint8checked((short) l);
                    return;
                }

                if (l <= (1 << 16) - 1) {
                    super.uint16checked((int) l);
                    return;
                }

                if (l <= (1L << 32L) - 1L) {
                    super.uint32checked(l);
                    return;
                }

                if ((long) (float) l == l) {
                    super.float32(l);
                    return;
                }

                super.int64(l);
                return;

            }

            if (l >= Byte.MIN_VALUE && l <= Byte.MAX_VALUE) {
                super.int8((byte) l);
                return;
            }

            if (l >= Short.MIN_VALUE && l <= Short.MAX_VALUE) {
                super.int16((short) l);
                return;
            }

            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                super.int32((int) l);
                return;
            }

            if ((long) (float) l == l) {
                super.float32(l);
                return;
            }

            super.int64(l);
        }

        void writeNumber(double l) {

            boolean canOnlyBeRepresentedAsFloatingPoint = ((long) l) != l;

            if (l >= 0 && l <= 127 && !canOnlyBeRepresentedAsFloatingPoint) {
                // used when the value is written directly into the code byte
                bytes.writeUnsignedByte((int) l);
                return;
            }

            if (l >= 0) {

                if (l <= (1 << 8) - 1 && !canOnlyBeRepresentedAsFloatingPoint) {
                    super.uint8checked((short) l);
                    return;
                }

                if (l <= (1 << 16) - 1 && !canOnlyBeRepresentedAsFloatingPoint) {
                    super.uint16checked((int) l);
                    return;
                }

                if (((double) (float) l) == l) {
                    super.float32((float) l);
                    return;
                }

                if (l <= (1L << 32L) - 1 && !canOnlyBeRepresentedAsFloatingPoint) {
                    super.uint32checked((int) l);
                    return;
                }

                super.float64(l);
                return;
            }

            if (l >= Byte.MIN_VALUE && l <= Byte.MAX_VALUE && !canOnlyBeRepresentedAsFloatingPoint) {
                super.int8((byte) l);
                return;
            }

            if (l >= Short.MIN_VALUE && l <= Short.MAX_VALUE && !canOnlyBeRepresentedAsFloatingPoint) {
                super.int16((short) l);
                return;
            }

            if (((double) (float) l) == l) {
                super.float32((float) l);
                return;
            }

            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE && !canOnlyBeRepresentedAsFloatingPoint) {
                super.int32((int) l);
                return;
            }

            super.float64(l);

        }

        @NotNull
        @Override
        public WireOut uint8checked(int u8) {
            writeNumber(u8);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int16(short i16) {
            writeNumber(i16);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint16checked(int u16) {
            writeNumber(u16);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            writeNumber(i32);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut uint32checked(long u32) {
            writeNumber(u32);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64(long i64) {
            writeNumber(i64);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut float32(float f) {
            writeNumber(f);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut float64(double d) {
            writeNumber(d);
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

        private boolean isText(int code) {
            return code == STRING_ANY ||
                    (code >= STRING_0 && code <= STRING_31);
        }

        @Nullable
        @Override
        public <ACS extends Appendable & CharSequence> ACS textTo(@NotNull ACS s) {
            int code = readCode();
            boolean wasNull = code == NULL;
            if (wasNull) {
                setLength(s, 0);
                return null;

            } else {
                ACS text = readText(code, s);
                if (text == null)
                    cantRead(code);
                return s;
            }
        }

        @NotNull
        @Override
        public WireIn int8(@NotNull ByteConsumer i) {
            consumeSpecial();

            final int code = bytes.readUnsignedByte();

            if (isText(code))
                i.accept(Byte.valueOf(text()));
            else
                i.accept((byte) readInt(code));

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
        public BytesStore bytesStore() {
            long length = readLength() - 1;
            int code = readCode();
            if (code != U8_ARRAY)
                cantRead(code);
            BytesStore toBytes = NativeBytesStore.nativeStore(length);
            toBytes.write(0, bytes);
            return toBytes;
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

        @NotNull
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

            final int code = readCode();
            if (isText(code))
                i.accept(Short.valueOf(text()));
            else
                i.accept((short) readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int16(@NotNull ShortConsumer i) {
            final int code = readCode();
            if (isText(code))
                i.accept(Short.valueOf(text()));
            else
                i.accept((short) readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn uint16(@NotNull IntConsumer i) {
            consumeSpecial();
            final int code = readCode();
            if (isText(code))
                i.accept(Integer.valueOf(text()));
            else
                i.accept((int) readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntConsumer i) {
            consumeSpecial();
            final int code = readCode();
            if (isText(code))
                i.accept(Integer.valueOf(text()));
            else
                i.accept((int) readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn uint32(@NotNull LongConsumer i) {
            consumeSpecial();
            final int code = readCode();
            if (isText(code))
                i.accept(Long.valueOf(text()));
            else
                i.accept(readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongConsumer i) {
            final int code = readCode();
            if (isText(code))
                i.accept(Long.valueOf(text()));
            else
                i.accept(readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn float32(@NotNull FloatConsumer v) {
            consumeSpecial();
            final int code = readCode();
            if (isText(code))
                v.accept(Float.valueOf(text()));
            else
                v.accept((float) readFloat(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn float64(@NotNull DoubleConsumer v) {
            final int code = readCode();
            v.accept(readFloat(code));
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
            return bytes.remaining() > 0;
        }

        @NotNull
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

        @NotNull
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

        @NotNull
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

        @NotNull
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

        @NotNull
        @Override
        public WireIn sequence(@NotNull Consumer<ValueIn> reader) {
            consumeSpecial();
            int code = readCode();
            if (code != BYTES_LENGTH32)
                cantRead(code);
            final int length = bytes.readInt();
            long limit = bytes.readLimit();
            long limit2 = bytes.position() + length;
            bytes.limit(limit2);
            bytes.limit(limit2);
            try {
                reader.accept(this);
            } finally {
                bytes.limit(limit);
                bytes.position(limit2);
            }
            return BinaryWire.this;
        }

        @Override
        public <T> T applyToMarshallable(@NotNull Function<WireIn, T> marshallableReader) {
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
            if (code == TYPE_PREFIX) {
                bytes.readUTFΔ(s);

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn typeLiteral(@NotNull Consumer<CharSequence> classNameConsumer) {
            int code = readCode();
            if (code == TYPE_LITERAL) {
                StringBuilder sb = Wires.acquireStringBuilder();
                bytes.readUTFΔ(sb);
                classNameConsumer.accept(sb);

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

        @NotNull
        @Override
        public <K, V> Map<K, V> map(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap) {
            throw new UnsupportedOperationException("todo");
        }

        private long readTextAsLong() {
            bytes.skip(-1);
            final String text = text();
            if (text == null)
                throw new NullPointerException();
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return (long) Math.round(Double.parseDouble(text));
            }
        }

        private double readTextAsDouble() {
            bytes.skip(-1);
            final String text = text();
            if (text == null)
                throw new NullPointerException();

            return Double.parseDouble(text);
        }

        @Override
        public boolean bool() {
            consumeSpecial();
            int code = readCode();
            if (isText(code))
                return Boolean.valueOf(text());

            switch (code) {
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
            final long value = isText(code) ? readTextAsLong() : readInt0(code);

            if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE)
                throw new IllegalStateException();
            return (byte) value;

        }

        @Override
        public short int16() {
            consumeSpecial();
            int code = readCode();
            final long value = isText(code) ? readTextAsLong() : readInt0(code);
            if (value > Short.MAX_VALUE || value < Short.MIN_VALUE)
                throw new IllegalStateException();
            return (short) value;
        }

        @Override
        public int uint16() {
            consumeSpecial();
            int code = readCode();

            final long value = isText(code) ? readTextAsLong() : readInt0(code);

            if (value > (1L << 32L) || value < 0)
                throw new IllegalStateException();

            return (int) value;

        }

        private boolean isSmallInt(int code) {
            return (code & 128) == 0;
        }

        @Override
        public int int32() {
            consumeSpecial();
            int code = readCode();
            final long value = isText(code) ? readTextAsLong() : readInt0(code);

            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
                throw new IllegalStateException();

            return (int) value;
        }

        @Override
        public long int64() {
            consumeSpecial();
            int code = readCode();

            if (code >> 4 == BinaryWireHighCode.FLOAT)
                return (long) readFloat0(code);
            return isText(code) ? readTextAsLong() : readInt0(code);
        }

        @Override
        public double float64() {
            int code = readCode();
            return isText(code) ? readTextAsLong() : readFloat0(code);
        }

        @Override
        public float float32() {
            consumeSpecial();
            int code = readCode();
            final double value = isText(code) ? readTextAsLong() : readFloat0(code);

            if (value > Float.MAX_VALUE || value < Float.MIN_VALUE)
                throw new IllegalStateException();

            return (float) value;
        }

        @NotNull
        private WireIn cantRead(int code) {
            throw new UnsupportedOperationException(stringForCode(code));
        }

        @Nullable
        @Override
        public <E> E object(@Nullable E using, @NotNull Class<E> clazz) {
            if (byte[].class.isAssignableFrom(clazz))
                return (E) bytes();

            else if (Marshallable.class.isAssignableFrom(clazz)) {
                final E v;
                if (using == null)
                    try {
                        v = clazz.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                else
                    v = using;

                marshallable((Marshallable) v);
                return v;

            } else if (StringBuilder.class.isAssignableFrom(clazz)) {
                StringBuilder builder = (using == null)
                        ? Wires.acquireStringBuilder()
                        : (StringBuilder) using;
                textTo(builder);
                return (E) builder;

            } else if (CharSequence.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) text();

            } else if (Long.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Long) int64();

            } else if (Double.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Double) float64();

            } else if (Integer.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Integer) int32();

            } else if (Float.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Float) float32();

            } else if (Short.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Short) int16();

            } else if (Character.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                final String text = text();
                if (text == null || text.length() == 0)
                    return null;
                return (E) (Character) text.charAt(0);

            } else if (Byte.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Byte) int8();

            } else if (Map.class.isAssignableFrom(clazz)) {
                //noinspection unchecked

                final HashMap result = new HashMap();
                map(result);
                return (E) result;

            } else if (Object.class.isAssignableFrom(clazz)) {
                int code = peekCode();
                if (code == TYPE_PREFIX)
                    return (E) typedMarshallable();
                if (code == BYTES_LENGTH32)
                    return (E) bytesStore();
                // assume it a String
                return (E) text();

            } else {
                throw new IllegalStateException("unsupported " + clazz);
            }
        }
    }
    }

