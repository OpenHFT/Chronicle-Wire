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
import net.openhft.chronicle.bytes.ref.BinaryIntReference;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.bytes.ref.BinaryLongReference;
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

import static net.openhft.chronicle.core.util.ReadResolvable.readResolve;
import static net.openhft.chronicle.wire.BinaryWire.AnyCodeMatch.ANY_CODE_MATCH;
import static net.openhft.chronicle.wire.BinaryWireCode.*;

/**
 * This Wire is a binary translation of TextWire which is a sub set of YAML.
 */
public class BinaryWire implements Wire, InternalWire {
    private static final int END_OF_BYTES = -1;
    private static final UTF8StringInterner UTF8_INTERNER = new UTF8StringInterner(512);
    private final Bytes<?> bytes;
    private final ValueOut fixedValueOut = new FixedBinaryValueOut();
    @NotNull
    private final ValueOut valueOut;
    private final BinaryValueIn valueIn = new BinaryValueIn();
    private final boolean numericFields;
    private final boolean fieldLess;
    private final int compressedSize;
    private final WriteDocumentContext writeContext = new WriteDocumentContext(this);
    private final ReadDocumentContext readContext = new ReadDocumentContext(this);
    private boolean ready;
    private String compression;

    public BinaryWire(Bytes bytes) {
        this(bytes, false, false, false, Integer.MAX_VALUE, "binary");
    }

    public BinaryWire(Bytes bytes, boolean fixed, boolean numericFields, boolean fieldLess, int compressedSize, String compression) {
        this.numericFields = numericFields;
        this.fieldLess = fieldLess;
        this.bytes = bytes;
        this.compressedSize = compressedSize;
        valueOut = fixed ? fixedValueOut : new BinaryValueOut();
        this.compression = compression;
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) {
        writeContext.start(metaData);
        return writeContext;
    }

    @Override
    public DocumentContext readingDocument() {
        readContext.start();
        return readContext;
    }

    @Override
    public DocumentContext readingDocument(long readLocation) {
        final long readPosition = bytes().readPosition();
        final long readLimit = bytes().readLimit();
        bytes().readPosition(readLocation);
        readContext.start();
        readContext.closeReadLimit(readLimit);
        readContext.closeReadPosition(readPosition);
        return readContext;
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
        while (bytes.readRemaining() > 0) {
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
                    bytes.readSkip(1);
                    wire.getValueOut().uint8checked(peekCode);
                    break;

                case BinaryWireHighCode.CONTROL:
                    switch (peekCode) {
                        case PADDING:
                            bytes.readSkip(1);
                            break outerSwitch;
                        case PADDING32:
                            bytes.readSkip(1);
                            bytes.readSkip(bytes.readUnsignedInt());
                            break outerSwitch;
                        case BYTES_LENGTH32:
                            bytes.readSkip(1);
                            int len = bytes.readInt();
                            long lim = bytes.readLimit();
                            try {
                                bytes.readLimit(bytes.readPosition() + len);
                                if (isFieldNext())
                                    wire.getValueOut().marshallable(w -> copyTo(w));
                                else
                                    wire.getValueOut().sequence(v -> copyTo(v.wireOut()));
                            } finally {
                                bytes.readLimit(lim);
                            }
                            break outerSwitch;

                        case U8_ARRAY:
                            bytes.readSkip(1);
                            wire.getValueOut().bytes(bytes);
                            bytes.readPosition(bytes.readLimit());
                            break outerSwitch;

                        case I64_ARRAY:
                            // no supported.
                            break;

                    }
                    unknownCode(wire);
                    break;

                case BinaryWireHighCode.FLOAT:
                    bytes.readSkip(1);
                    try {
                        Number d = readFloat0(peekCode);
                        wire.getValueOut().object(d);
                    } catch (Exception e) {
                        unknownCode(wire);
                    }
                    break;

                case BinaryWireHighCode.INT:
                    bytes.readSkip(1);
                    try {
                        Number l = readInt0object(peekCode);
                        wire.getValueOut().object(l);
                    } catch (Exception e) {
                        unknownCode(wire);
                    }
                    break;

                case BinaryWireHighCode.SPECIAL:
                    copySpecial(wire, peekCode);
                    break;

                case BinaryWireHighCode.FIELD0:
                case BinaryWireHighCode.FIELD1:
                    StringBuilder fsb = readField(peekCode, ANY_CODE_MATCH, WireInternal.acquireStringBuilder(), false);
                    wire.write(() -> fsb);
                    break;

                case BinaryWireHighCode.STR0:
                case BinaryWireHighCode.STR1:
                    bytes.readSkip(1);
                    StringBuilder sb = readText(peekCode, WireInternal.acquireStringBuilder());
                    wire.getValueOut().text(sb);
                    break;
            }
        }
    }

    private void unknownCode(@NotNull WireOut wire) {
        wire.writeComment("# " + stringForCode(bytes.readUnsignedByte()));
    }

    private boolean isFieldNext() {
        int peekCode = peekCode();
        return peekCode == FIELD_NAME_ANY || (peekCode >= FIELD_NAME0 && peekCode <= FIELD_NAME31);
    }

    @NotNull
    @Override
    public ValueIn read() {
        readField(WireInternal.acquireStringBuilder(), ANY_CODE_MATCH);
        return valueIn;
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        long position = bytes.readPosition();
        StringBuilder sb = readField(WireInternal.acquireStringBuilder(), key);

        if (fieldLess || (sb != null && (sb.length() == 0 || StringUtils.isEqual(sb, key.name()))))
            return valueIn;
        return unorderedField(key, position, sb);
    }

    @NotNull
    private ValueIn unorderedField(@NotNull WireKey key, long position, @Nullable StringBuilder sb) {
        bytes.readPosition(position);
        if (sb == null)
            sb = WireInternal.acquireStringBuilder();
        readEventName(sb);
        throw new UnsupportedOperationException("Unordered fields not supported yet, " +
                "Expected=" + key.name() + " was: '" + sb + "'");
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
        consumeSpecial();
        return bytes.readRemaining() > 0;
    }

    @Nullable
    private StringBuilder readField(@NotNull StringBuilder name, WireKey key) {
        consumeSpecial();
        int peekCode = peekCode();
        return readField(peekCode, key, name, true);
    }

    void consumeSpecial() {
        consumeSpecial(false);
    }

    void consumeSpecial(boolean consumeType) {
        while (true) {
            int code = peekCode();
            switch (code) {
                case PADDING:
                    bytes.readSkip(1);
                    break;

                case PADDING32:
                    bytes.readSkip(1);
                    bytes.readSkip(bytes.readUnsignedInt());
                    break;

                case TYPE_PREFIX:
                    if (!consumeType)
                        return;
                    // fall through

                case COMMENT:
                case HINT: {
                    bytes.readSkip(1);
                    StringBuilder sb = WireInternal.acquireStringBuilder();
                    bytes.readUtf8(sb);
                    break;
                }

                default:
                    return;
            }
        }
    }

    private int peekCode() {
        if (bytes.readRemaining() < 1)
            return END_OF_BYTES;
        long pos = bytes.readPosition();
        return bytes.readUnsignedByte(pos);
    }

    private StringBuilder readField(int peekCode, WireKey key, @NotNull StringBuilder sb, boolean missingOk) {
        sb.setLength(0);
        switch (peekCode >> 4) {
            case BinaryWireHighCode.END_OF_STREAM:
                break;

            case BinaryWireHighCode.SPECIAL:
                return readSpecialField(peekCode, key, sb);

            case BinaryWireHighCode.FIELD0:
            case BinaryWireHighCode.FIELD1:
                bytes.readSkip(1);
                if (bytes.bytesStore() instanceof NativeBytesStore) {
                    AppendableUtil.parse8bit_SB1(bytes, sb, peekCode & 0x1f);
                } else {
                    AppendableUtil.parse8bit(bytes, sb, peekCode & 0x1f);
                }
                return sb;
            default:
                if (missingOk)
                    // if it's not a field, perhaps none was written.
                    break;
                throw new UnsupportedOperationException("Unknown code " + stringForCode(peekCode));
        }
        // if field-less accept anything in order.
        if (fieldLess) {
            return sb;
        }

        return null;
    }

    @Nullable
    private StringBuilder readSpecialField(int peekCode, WireKey key, @NotNull StringBuilder sb) {
        if (peekCode == FIELD_NUMBER) {
            bytes.readSkip(1);
            long fieldId = bytes.readStopBit();
            int codeMatch = key.code();
            if (codeMatch >= 0 && fieldId != codeMatch)
                throw new UnsupportedOperationException("Field was: " + fieldId + " expected " + codeMatch);
            if (codeMatch < 0)
                sb.append(fieldId);
            return sb;
        }
        if (peekCode == FIELD_NAME_ANY || peekCode == EVENT_NAME) {
            bytes.readSkip(1);
            bytes.readUtf8(sb);
            return sb;
        }
        return null;
    }

    @NotNull
    private <ACS extends Appendable & CharSequence> ACS getStringBuilder(int code, @NotNull ACS sb) {
        bytes.parseUtf8(sb, code & 0x1f);
        return sb;
    }

    private void copySpecial(@NotNull WireOut wire, int peekCode) {
        switch (peekCode) {
            case COMMENT: {
                bytes.readSkip(1);
                StringBuilder sb = WireInternal.acquireStringBuilder();
                bytes.readUtf8(sb);
                wire.writeComment(sb);
                break;
            }

            case HINT: {
                bytes.readSkip(1);
                StringBuilder sb = WireInternal.acquireStringBuilder();
                bytes.readUtf8(sb);
                break;
            }

            case TIME:
            case ZONED_DATE_TIME:
            case DATE_TIME:
                throw new UnsupportedOperationException();

            case TYPE_PREFIX: {
                long readPosition = bytes.readPosition();
                bytes.readSkip(1);
                StringBuilder sb = WireInternal.acquireStringBuilder();
                bytes.readUtf8(sb);
                if (StringUtils.isEqual("snappy", sb) || StringUtils.isEqual("gzip", sb) || StringUtils.isEqual("lzw", sb)) {
                    bytes.readPosition(readPosition);
                    wire.writeComment(sb);
                    wire.getValueOut().text(valueIn.text());
                } else {
                    wire.getValueOut().typePrefix(sb);
                }
                break;
            }

            case TYPE_LITERAL: {
                bytes.readSkip(1);
                StringBuilder sb = WireInternal.acquireStringBuilder();
                bytes.readUtf8(sb);
                wire.getValueOut().typeLiteral(sb);
                break;
            }

            case EVENT_NAME:
            case FIELD_NAME_ANY:
                StringBuilder fsb = readField(peekCode, ANY_CODE_MATCH, WireInternal.acquireStringBuilder(), false);
                wire.write(() -> fsb);
                break;

            case STRING_ANY: {
                bytes.readSkip(1);
                StringBuilder sb1 = WireInternal.acquireStringBuilder();
                bytes.readUtf8(sb1);
                wire.getValueOut().text(sb1);
                break;
            }

            case FIELD_NUMBER: {
                bytes.readSkip(1);
                long code2 = bytes.readStopBit();
                wire.write(new WireKey() {
                    @NotNull
                    @Override
                    public String name() {
                        return Integer.toString(code());
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
                bytes.readSkip(1);
                wire.getValueOut().bool(null);
                break;

            case FALSE:
                bytes.readSkip(1);
                wire.getValueOut().bool(false);
                break;

            case TRUE:
                bytes.readSkip(1);
                wire.getValueOut().bool(true);
                break;
            default:
                unknownCode(wire);
        }
    }

    private long readInt(int code) {
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
        // TODO: in some places we have already called this before invoking the function,
        // so we should review them and optimize the calls to do the check only once
        if (code < 128 && code >= 0) {
            return code;
        }

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

    private Number readFloat0object(int code) {
        // TODO: in some places we have already called this before invoking the function,
        // so we should review them and optimize the calls to do the check only once
        if (code < 128 && code >= 0) {
            return code;
        }

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

    private Number readInt0object(int code) {
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
        writeCode(EVENT_NAME).writeUtf8(key.name());
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
    public ValueOut getValueOut() {
        return valueOut;
    }

    @NotNull
    @Override
    public Wire writeComment(CharSequence s) {
        writeCode(COMMENT);
        bytes.writeUtf8(s);
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
                    .writeSkip(paddingToAdd - 5);

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
                    .write(name);

        } else {
            writeCode(FIELD_NAME_ANY).write8bit(name);
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
            AppendableUtil.append(sb, code);
            return sb;
        }
        switch (code >> 4) {
            case BinaryWireHighCode.CONTROL:
                switch (code) {
                    case BYTES_LENGTH32:
                        if (sb instanceof StringBuilder) {
                            bytes.readSkip(-1);
                            valueIn.bytesStore((StringBuilder) sb);
                        } else {
                            throw new AssertionError();
                        }
                        return sb;
                }
            case BinaryWireHighCode.SPECIAL:
                switch (code) {
                    case NULL:
                        AppendableUtil.append(sb, "null");
                        return sb;
                    case TRUE:
                        AppendableUtil.append(sb, "true");
                        return sb;
                    case FALSE:
                        AppendableUtil.append(sb, "false");
                        return sb;
                    case TIME:
                    case DATE:
                    case DATE_TIME:
                    case ZONED_DATE_TIME:
                    case TYPE_LITERAL:
                    case STRING_ANY:
                        if (bytes.readUtf8(sb))
                            return sb;
                        return null;
                    default:
                        return null;
                }

            case BinaryWireHighCode.FLOAT:
                AppendableUtil.append(sb, readFloat(code));
                return sb;
            case BinaryWireHighCode.INT:
                AppendableUtil.append(sb, readInt(code));
                return sb;
            case BinaryWireHighCode.STR0:
            case BinaryWireHighCode.STR1:
                return getStringBuilder(code, sb);
            default:
                throw new UnsupportedOperationException("code=0x" + String.format("%02X ", code).trim());
        }
    }

    private int readCode() {
        if (bytes.readRemaining() < 1)
            return END_OF_BYTES;
        return bytes.readUnsignedByte();
    }

    @NotNull
    public String toString() {
        return bytes.readRemaining() > 1024 ?
                bytes.toDebugString(1024) + "..." : bytes.toDebugString();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return new BinaryLongReference();
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        return new BinaryIntReference();
    }

    @NotNull
    @Override
    public BinaryLongArrayReference newLongArrayReference() {
        return new BinaryLongArrayReference();
    }

    enum AnyCodeMatch implements WireKey {
        ANY_CODE_MATCH;

        public int code() {
            return Integer.MIN_VALUE;
        }
    }

    class FixedBinaryValueOut implements ValueOut {
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
                    bytes.writeUnsignedByte(STRING_0 + len).appendUtf8(s);
                } else {
                    writeCode(STRING_ANY).writeUtf8(s);
                }
            }

            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut text(@Nullable BytesStore s) {
            if (s == null) {
                writeCode(NULL);

            } else {
                int len = s.length();
                if (len < 0x20) {
                    bytes.writeUnsignedByte(STRING_0 + len).appendUtf8(s);
                } else {
                    writeCode(STRING_ANY).writeUtf8(s);
                }
            }

            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(@Nullable BytesStore fromBytes) {
            if (fromBytes == null)
                return text(null);
            long remaining = fromBytes.readRemaining();
            if (remaining >= compressedSize()) {
                compress(compression, fromBytes.bytesForRead());
            } else {
                bytes0(fromBytes, remaining);
            }
            return BinaryWire.this;
        }

        @Override
        public int compressedSize() {
            return compressedSize;
        }

        public void bytes0(@Nullable BytesStore fromBytes, long remaining) {
            writeLength(Maths.toInt32(remaining + 1));
            writeCode(U8_ARRAY);
            if (remaining > 0)
                bytes.write(fromBytes);
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
        public WireOut bytes(String type, @Nullable BytesStore fromBytes) {
            typePrefix(type);
            bytes0(fromBytes, fromBytes.readRemaining());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut bytes(String type, byte[] fromBytes) {
            typePrefix(type);
            return bytes(fromBytes);
        }

        @NotNull
        @Override
        public WireOut int8(byte i8) {
            writeCode(INT8).writeByte(i8);
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
            bytes.appendUtf8(codepoint);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32(int i32) {
            return fixedInt32(i32);
        }

        @NotNull
        public WireOut fixedInt32(int i32) {
            writeCode(INT32).writeInt(i32);
            return BinaryWire.this;
        }

        @NotNull
        public WireOut fixedOrderedInt32(int i32) {
            writeCode(INT32).writeOrderedInt(i32);
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
        private WireOut fixedOrderedInt64(long i64) {
            writeCode(INT64).writeOrderedLong(i64);
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
        public WireOut int64array(long capacity, @NotNull LongArrayValues values) {
            writeCode(I64_ARRAY);
            long pos = bytes.writePosition();
            BinaryLongArrayReference.lazyWrite(bytes, capacity);
            ((Byteable) values).bytesStore(bytes, pos, bytes.writePosition() - pos);
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
            writeCode(TIME).writeUtf8(localTime.toString());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut zonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
            writeCode(ZONED_DATE_TIME).writeUtf8(zonedDateTime.toString());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut date(@NotNull LocalDate localDate) {
            writeCode(DATE_TIME).writeUtf8(localDate.toString());
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public ValueOut typePrefix(CharSequence typeName) {
            writeCode(TYPE_PREFIX).writeUtf8(typeName);
            return this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull CharSequence type) {
            writeCode(TYPE_LITERAL).writeUtf8(type);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull Class type) {
            writeCode(TYPE_LITERAL).writeUtf8(ClassAliasPool.CLASS_ALIASES.nameFor(type));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @NotNull Class type) {
            writeCode(TYPE_LITERAL);
            typeTranslator.accept(type, bytes);
            return BinaryWire.this;
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
            int fromEndOfCacheLine = (int) ((-bytes.readPosition() - 1) & 63);
            if (fromEndOfCacheLine < 4)
                addPadding(fromEndOfCacheLine - 1);
            fixedInt32(value);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value) {
            int fromEndOfCacheLine = (int) ((-bytes.readPosition() - 1) & 63);
            if (fromEndOfCacheLine < 8)
                addPadding(fromEndOfCacheLine);
            fixedOrderedInt64(value);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int32forBinding(int value, @NotNull IntValue intValue) {
            int32forBinding(value);
            ((BinaryIntReference) intValue).bytesStore(bytes, bytes.writePosition() - 4, 4);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut int64forBinding(long value, @NotNull LongValue longValue) {
            int64forBinding(value);
            ((BinaryLongReference) longValue).bytesStore(bytes, bytes.writePosition() - 8, 8);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut sequence(@NotNull WriteValue writer) {
            writeCode(BYTES_LENGTH32);
            long position = bytes.writePosition();
            bytes.writeInt(0);

            writer.writeValue(this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.writePosition() - position - 4, "Document length %,d out of 32-bit int range."));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireOut marshallable(@NotNull WriteMarshallable object) {
            writeCode(BYTES_LENGTH32);
            long position = bytes.writePosition();
            bytes.writeInt(0);

            object.writeMarshallable(BinaryWire.this);

            bytes.writeOrderedInt(position, Maths.toInt32(bytes.writePosition() - position - 4, "Document length %,d out of 32-bit int range."));
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
        WireIn text(@NotNull Consumer<String> s) {
            consumeSpecial();
            int code = readCode();
            switch (code) {
                case NULL:
                    s.accept(null);
                    break;

                case STRING_ANY:
                    s.accept(bytes.readUtf8());
                    break;
                default:
                    if (code >= STRING_0 && code <= STRING_31) {
                        StringBuilder sb = WireInternal.acquireStringBuilder();
                        bytes.parseUtf8(sb, code & 0b11111);
                        s.accept(WireInternal.INTERNER.intern(sb));

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
        public StringBuilder textTo(@NotNull StringBuilder sb) {
            int code = readCode();
            boolean wasNull = code == NULL;
            if (wasNull) {
                sb.setLength(0);
                return null;

            } else {
                StringBuilder text = readText(code, sb);
                if (text == null)
                    cantRead(code);
                return sb;
            }
        }

        @Nullable
        @Override
        public Bytes textTo(@NotNull Bytes bytes) {
            int code = readCode();
            boolean wasNull = code == NULL;
            if (wasNull) {
                bytes.readPosition(0);
                return null;

            } else {
                Bytes text = readText(code, bytes);
                if (text == null)
                    cantRead(code);
                return bytes;
            }
        }

        @Nullable
        @Override
        public String text() {
            int code = readCode();
            switch (code) {
                case NULL:
                    return null;

                case STRING_ANY: {
                    long len0 = bytes.readStopBit();
                    if (len0 == -1L) {
                        return null;

                    }
                    int len = Maths.toUInt31(len0);
                    long limit = bytes.readLimit();
                    try {
                        bytes.readLimit(bytes.readPosition() + len);
                        return UTF8_INTERNER.intern(bytes);
                    } finally {
                        bytes.readPosition(bytes.readLimit());
                        bytes.readLimit(limit);
                    }
                }

                case TYPE_PREFIX: {
                    StringBuilder sb = WireInternal.acquireStringBuilder();
                    if (bytes.readUtf8(sb)) {
                        byte[] bytes = Compression.uncompress(sb, this, ValueIn::bytes);
                        if (bytes != null)
                            return new String(bytes, StandardCharsets.UTF_8);
                    }
                    StringBuilder text = readText(code, sb);
                    return WireInternal.INTERNER.intern(text);
                }

                default: {
                    StringBuilder text = readText(code, WireInternal.acquireStringBuilder());
                    if (text == null)
                        cantRead(code);
                    return WireInternal.INTERNER.intern(text);
                }
            }
        }

        @NotNull
        public WireIn bytes(@NotNull Bytes toBytes) {
            long length = readLength();
            int code = readCode();
            if (code == NULL) {
                toBytes.isPresent(false);
                return BinaryWire.this;
            }
            if (code == TYPE_PREFIX) {
                StringBuilder sb = WireInternal.acquireStringBuilder();
                if (bytes.readUtf8(sb)) {
                    long length2 = readLength();
                    int code2 = readCode();
                    if (code2 != U8_ARRAY)
                        cantRead(code);
                    toBytes.clear();
                    bytes.readWithLength(length2 - 1, b -> Compression.uncompress(sb, b, toBytes));
                    return wireIn();
                } else {
                    throw new AssertionError();
                }
            }
            if (code != U8_ARRAY)
                cantRead(code);
            toBytes.clear();
            bytes.readWithLength(length - 1, toBytes::write);
            return wireIn();
        }

        @Nullable
        public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
            long length = readLength();
            int code = readCode();
            if (code == NULL) {
                toBytes.isPresent(false);
                return BinaryWire.this;
            }
            if (code != U8_ARRAY)
                cantRead(code);
            long startAddr = bytes.address(bytes.readPosition());
            toBytes.set(startAddr, length - 1);
            bytes.readSkip(length - 1);
            return wireIn();
        }

        @NotNull
        @Override
        public WireIn bytesMatch(@NotNull BytesStore compareBytes, @NotNull BooleanConsumer consumer) {
            long length = readLength();
            int code = readCode();
            if (code != U8_ARRAY)
                cantRead(code);
            length--;
            if (compareBytes.readRemaining() == length) {
                consumer.accept(bytes.equalBytes(compareBytes, length));
            } else {
                consumer.accept(false);
            }
            bytes.readSkip(length);
            return wireIn();

        }

        @Nullable
        public BytesStore bytesStore() {
            long length = readLength() - 1;
            int code = readCode();
            switch (code) {
                case U8_ARRAY:
                    BytesStore toBytes = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(length);
                    toBytes.write(0, bytes, bytes.readPosition(), length);
                    bytes.readSkip(length);
                    return toBytes;

                case TYPE_PREFIX: {
                    StringBuilder sb = WireInternal.acquireStringBuilder();
                    bytes.readUtf8(sb);
                    byte[] bytes = Compression.uncompress(sb, this, ValueIn::bytes);
                    if (bytes != null)
                        return BytesStore.wrap(bytes);
                    throw new UnsupportedOperationException("Unsupported type " + sb);
                }
                case NULL:
                    return null;
                default:
                    cantRead(code);
                    throw new AssertionError();
            }
        }

        public void bytesStore(@NotNull StringBuilder sb) {
            sb.setLength(0);
            long length = readLength() - 1;
            int code = readCode();
            if (code != U8_ARRAY)
                cantRead(code);
            for (long i = 0; i < length; i++)
                sb.append((char) bytes.readUnsignedByte());
        }

        public void bytesStore(@NotNull Bytes toBytes) {
            toBytes.clear();
            long length = readLength() - 1;
            int code = readCode();
            if (code == NULL) {
                toBytes.isPresent(false);
                return;
            }
            if (code != U8_ARRAY)
                cantRead(code);
            toBytes.write(0, bytes, bytes.readPosition(), length);
            toBytes.readLimit(length);
            bytes.readSkip(length);
        }

        @NotNull
        public WireIn bytes(@NotNull ReadMarshallable bytesConsumer) {
            long length = readLength() - 1;
            int code = readCode();
            if (code != U8_ARRAY)
                cantRead(code);

            if (length > bytes.readRemaining())
                throw new BufferUnderflowException();
            long limit0 = bytes.readLimit();
            long limit = bytes.readPosition() + length;
            try {
                bytes.readLimit(limit);
                bytesConsumer.readMarshallable(wireIn());
            } finally {
                bytes.readLimit(limit0);
                bytes.readPosition(limit);
            }
            return wireIn();
        }

        @NotNull
        @Override
        public byte[] bytes() {
            long length = readLength();
            int code = readCode();
            if (code == NULL) {
                return null;
            }
            if (code != U8_ARRAY)
                cantRead(code);
            byte[] bytes2 = new byte[Maths.toUInt31(length - 1)];
            bytes.readWithLength(length - 1, b -> b.read(bytes2));
            return bytes2;
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
                    bytes.readSkip(1);
                    return bytes.readUnsignedByte();
                case BYTES_LENGTH16:
                    bytes.readSkip(1);
                    return bytes.readUnsignedShort();
*/
                case BYTES_LENGTH32:
                    bytes.readSkip(1);
                    return bytes.readUnsignedInt();
                default:
                    return ANY_CODE_MATCH.code();
            }
        }

        @NotNull
        @Override
        public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag) {
            consumeSpecial();
            int code = readCode();
            switch (code) {
                case NULL:
                    // todo take the default.
                    tFlag.accept(t, null);
                    break;

                case FALSE:
                    tFlag.accept(t, false);
                    break;

                case TRUE:
                    tFlag.accept(t, true);
                    break;
                default:
                    throw cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
            consumeSpecial();

            final int code = bytes.readUnsignedByte();

            if (isText(code))
                tb.accept(t, Byte.parseByte(text()));
            else
                tb.accept(t, (byte) readInt(code));

            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            consumeSpecial();

            final int code = readCode();
            if (isText(code))
                ti.accept(t, Short.parseShort(text()));
            else
                ti.accept(t, (short) readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
            final int code = readCode();
            if (isText(code))
                ti.accept(t, Short.parseShort(text()));
            else
                ti.accept(t, (short) readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumeSpecial();
            final int code = readCode();
            if (isText(code))
                ti.accept(t, Integer.parseInt(text()));
            else
                ti.accept(t, (int) readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
            consumeSpecial();
            final int code = readCode();
            if (isText(code))
                ti.accept(t, Integer.parseInt(text()));
            else
                ti.accept(t, (int) readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            consumeSpecial();
            final int code = readCode();
            if (isText(code))
                tl.accept(t, Long.parseLong(text()));
            else
                tl.accept(t, readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
            final int code = readCode();
            if (isText(code))
                tl.accept(t, Long.parseLong(text()));
            else
                tl.accept(t, readInt(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
            consumeSpecial();
            final int code = readCode();
            if (isText(code))
                tf.accept(t, Float.parseFloat(text()));
            else
                tf.accept(t, (float) readFloat(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
            final int code = readCode();
            td.accept(t, readFloat(code));
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
            consumeSpecial();
            int code = readCode();
            if (code == TIME) {
                setLocalTime.accept(t, readLocalTime());

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        private LocalTime readLocalTime() {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            bytes.readUtf8(sb);
            return LocalTime.parse(sb);
        }

        @NotNull
        @Override
        public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
            consumeSpecial();
            int code = readCode();
            if (code == ZONED_DATE_TIME) {
                StringBuilder sb = WireInternal.acquireStringBuilder();
                bytes.readUtf8(sb);
                tZonedDateTime.accept(t, ZonedDateTime.parse(sb));

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
            consumeSpecial();
            int code = readCode();
            if (code == DATE_TIME) {
                StringBuilder sb = WireInternal.acquireStringBuilder();
                bytes.readUtf8(sb);
                tLocalDate.accept(t, LocalDate.parse(sb));

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
            return bytes.readRemaining() > 0;
        }

        @NotNull
        @Override
        public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
            consumeSpecial();
            int code = readCode();
            if (code == UUID) {
                tuuid.accept(t, new UUID(bytes.readLong(), bytes.readLong()));

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter) {
            consumeSpecial();
            int code = readCode();
            if (code == I64_ARRAY) {
                if (!(values instanceof BinaryLongArrayReference))
                    setter.accept(t, values = new BinaryLongArrayReference());
                Byteable b = (Byteable) values;
                long length = BinaryLongArrayReference.peakLength(bytes, bytes.readPosition());
                b.bytesStore(bytes, bytes.readPosition(), length);
                bytes.readSkip(length);

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(LongValue value) {
            consumeSpecial();
            int code = readCode();
            if (code != INT64)
                cantRead(code);

            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter) {
            // if the value is null, then we will create a LongDirectReference to write the data
            // into and then call setter.accept(), this will then update the value
            if (!(value instanceof BinaryLongReference)) {
                setter.accept(t, value = new BinaryLongReference());
            }
            return int64(value);
        }

        @NotNull
        @Override
        public <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter) {
            consumeSpecial();
            int code = readCode();
            if (code != INT32)
                cantRead(code);
            if (!(value instanceof Byteable) || ((Byteable) value).maxSize() != 4) {
                setter.accept(t, value = new BinaryIntReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.readPosition(), length);
            bytes.readSkip(length);
            return BinaryWire.this;
        }

        @NotNull
        @Override
        public <T> WireIn sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
            consumeSpecial();
            int code = readCode();
            if (code != BYTES_LENGTH32)
                cantRead(code);
            final int length = bytes.readInt();
            long limit = bytes.readLimit();
            long limit2 = bytes.readPosition() + length;
            bytes.readLimit(limit2);
            try {
                tReader.accept(t, this);
            } finally {
                bytes.readLimit(limit);
                bytes.readPosition(limit2);
            }
            return BinaryWire.this;
        }

        @Override
        public <T> T applyToMarshallable(@NotNull Function<WireIn, T> marshallableReader) {
            consumeSpecial();
            long length = readLength();
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.readPosition() + length;
                bytes.readLimit(limit2);
                try {
                    return marshallableReader.apply(BinaryWire.this);
                } finally {
                    bytes.readLimit(limit);
                    bytes.readPosition(limit2);
                }
            } else {
                return marshallableReader.apply(BinaryWire.this);
            }
        }

        @Override
        public boolean isTyped() {
            int code = peekCode();
            return code == TYPE_PREFIX;
        }

        @Nullable
        public <T extends ReadMarshallable> T typedMarshallable() throws IORuntimeException {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            int code = readCode();
            switch (code) {
                case TYPE_PREFIX:
                    bytes.readUtf8(sb);
                    // its possible that the object that you are allocating may not have a
                    // default constructor
                    final Class clazz;
                    try {
                        clazz = ClassAliasPool.CLASS_ALIASES.forName(sb);
                    } catch (ClassNotFoundException e) {
                        throw new IORuntimeException(e);
                    }

                    if (!Marshallable.class.isAssignableFrom(clazz))
                        throw new IllegalStateException("its not possible to Marshallable and object that" +
                                " is not of type Marshallable, type=" + sb);

                    final ReadMarshallable m = ObjectUtils.newInstance((Class<ReadMarshallable>) clazz);

                    marshallable(m);
                    return readResolve(m);

                case NULL:
                    return null;

                default:
                    cantRead(code);
                    return null; // only if the throw doesn't work.
            }
        }

        @Override
        public Class typePrefix() {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            int code = readCode();
            if (code == TYPE_PREFIX) {
                bytes.readUtf8(sb);

            } else if (code == NULL) {
                sb.setLength(0);
                sb.append("!null");
            } else {
                cantRead(code);
            }
            try {
                return ClassAliasPool.CLASS_ALIASES.forName(sb);
            } catch (ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
        }

        @NotNull
        @Override
        public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            int code = readCode();
            if (code == TYPE_PREFIX) {
                bytes.readUtf8(sb);

            } else if (code == NULL) {
                sb.setLength(0);
                sb.append("!null");
            } else {
                cantRead(code);
            }
            ts.accept(t, sb);
            return this;
        }

        @NotNull
        @Override
        public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer) {
            int code = readCode();
            if (code == TYPE_LITERAL) {
                StringBuilder sb = WireInternal.acquireStringBuilder();
                bytes.readUtf8(sb);
                classNameConsumer.accept(t, sb);

            } else {
                cantRead(code);
            }
            return BinaryWire.this;
        }

        @Override
        public <T> Class<T> typeLiteral() {
            StringBuilder sb = WireInternal.acquireStringBuilder();
            int code = readCode();
            if (code == TYPE_LITERAL) {
                bytes.readUtf8(sb);
                try {
                    return ClassAliasPool.CLASS_ALIASES.forName(sb);
                } catch (ClassNotFoundException e) {
                    throw new IORuntimeException(e);
                }
            } else if (code == NULL) {
                return null;

            } else {
                throw cantRead(code);
            }
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) throws BufferUnderflowException, IORuntimeException {
            consumeSpecial(true);

            long length = readLength();
            if (length >= 0) {
                long limit = bytes.readLimit();
                long limit2 = bytes.readPosition() + length;
                bytes.readLimit(limit2);
                try {
                    object.readMarshallable(BinaryWire.this);
                } finally {
                    bytes.readLimit(limit);
                    bytes.readPosition(limit2);
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

        private long readTextAsLong() throws IORuntimeException, BufferUnderflowException {
            bytes.readSkip(-1);
            final String text = text();
            if (text == null)
                throw new NullPointerException();
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return Math.round(Double.parseDouble(text));
            }
        }

        private double readTextAsDouble() throws IORuntimeException, BufferUnderflowException {
            bytes.readSkip(-1);
            final String text = text();
            if (text == null || text.length() == 0)
                return Double.NaN;
            return Double.parseDouble(text);
        }

        @Override
        public boolean bool() throws IORuntimeException {
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
            throw new IORuntimeException(stringForCode(code));
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
            if (code >> 4 == BinaryWireHighCode.FLOAT)
                return readFloat0(code);
            return isText(code) ? readTextAsDouble() : readInt0(code);
        }

        @Override
        public float float32() {
            consumeSpecial();
            int code = readCode();
            final double value = isText(code) ? readTextAsDouble() : readFloat0(code);

            if (Double.isFinite(value) && (value > Float.MAX_VALUE || value < Float.MIN_VALUE))
                throw new IllegalStateException("Cannot convert " + value + " to float");

            return (float) value;
        }

        @NotNull
        private RuntimeException cantRead(int code) {
            throw new UnsupportedOperationException(stringForCode(code));
        }

        @Nullable
        @Override
        public <E> E object(@Nullable E using, @NotNull Class<E> clazz) {
            return ObjectUtils.convertTo(clazz, object0(using, clazz));
        }

        @Nullable
        @Override
        public <T, E> WireIn object(@NotNull Class<E> clazz, T t, BiConsumer<T, E> e) {
            final Object o = object0(null, clazz);
            final E u = ObjectUtils.convertTo(clazz, o);
            e.accept(t, u);
            return BinaryWire.this;
        }

        @Nullable
        Object object0(@Nullable Object using, @NotNull Class clazz) {

            final int code = peekCode();
            if (code == 0xBB) {
                readCode();
                return null;
            }

            if (ReadMarshallable.class.isAssignableFrom(clazz)) {
                final Object v;
                if (using == null)
                    v = ObjectUtils.newInstance(clazz);
                else
                    v = using;

                marshallable((ReadMarshallable) v);
                return readResolve(v);

            } else if (CharSequence.class.isAssignableFrom(clazz)) {
                if (StringBuilder.class.isAssignableFrom(clazz)) {
                    StringBuilder builder = (using == null)
                            ? WireInternal.acquireStringBuilder()
                            : (StringBuilder) using;
                    textTo(builder);
                    return builder;
                }
                return text();

            } else if (Map.class.isAssignableFrom(clazz)) {
                final Map result = new LinkedHashMap();
                map(result);
                return result;

            } else if (byte[].class.isAssignableFrom(clazz)) {
                return bytes();

            } else {
                return object(using);
            }
        }

        @Nullable
        private Object object(@Nullable Object using) {
            int code = peekCode();
            if ((code & 0x80) == 0) {
                bytes.readSkip(1);
                return code;
            }
            switch (code >> 4) {
                case BinaryWireHighCode.CONTROL:
                    switch (code) {
                        case BYTES_LENGTH32:
                            if (using instanceof StringBuilder) {
                                bytesStore((StringBuilder) using);
                                return using;
                            } else if (using instanceof Bytes) {
                                bytesStore((Bytes) using);
                                return using;
                            } else {
                                return bytesStore();
                            }
                    }
                    break;
                case BinaryWireHighCode.SPECIAL:
                    switch (code) {
                        case FALSE:
                            bytes.readSkip(1);
                            return Boolean.FALSE;
                        case TRUE:
                            bytes.readSkip(1);
                            return Boolean.TRUE;
                        case NULL:
                            bytes.readSkip(1);
                            return null;
                        case STRING_ANY:
                            return text();
                        case TYPE_PREFIX: {
                            readCode();
                            StringBuilder sb = WireInternal.acquireStringBuilder();
                            bytes.readUtf8(sb);
                            final Class clazz2;
                            try {
                                clazz2 = ClassAliasPool.CLASS_ALIASES.forName(sb);
                            } catch (ClassNotFoundException e) {
                                throw new IORuntimeException(e);
                            }
                            return object(null, clazz2);
                        }
                    }
                    break;

                case BinaryWireHighCode.FLOAT:
                    return readFloat0object(code);

                case BinaryWireHighCode.INT:
                    return readInt0object(code);
            }
            // assume it a String
            return text();
        }
    }
}

