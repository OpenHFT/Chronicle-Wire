/*
 *     Copyright (C) 2015-2020 chronicle.software
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

/* Generated SBE (Simple Binary Encoding) message codec */
package baseline;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.sbe.codec.java.CodecUtil;

public class DataDecoder {
    public static final int BLOCK_LENGTH = 23;
    public static final int TEMPLATE_ID = 1;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;

    private final DataDecoder parentMessage = this;
    private final VarDataEncodingDecoder text = new VarDataEncodingDecoder();
    protected int offset;
    protected int limit;
    protected int actingBlockLength;
    protected int actingVersion;
    private DirectBuffer buffer;

    public static int smallIntId() {
        return 1;
    }

    public static String smallIntMetaAttribute(final MetaAttribute metaAttribute) {
        switch (metaAttribute) {
            case EPOCH:
                return "unix";
            case TIME_UNIT:
                return "nanosecond";
            case SEMANTIC_TYPE:
                return "";
        }

        return "";
    }

    public static int smallIntNullValue() {
        return -2147483648;
    }

    public static int smallIntMinValue() {
        return -2147483647;
    }

    public static int smallIntMaxValue() {
        return 2147483647;
    }

    public static int longIntId() {
        return 2;
    }

    public static String longIntMetaAttribute(final MetaAttribute metaAttribute) {
        switch (metaAttribute) {
            case EPOCH:
                return "unix";
            case TIME_UNIT:
                return "nanosecond";
            case SEMANTIC_TYPE:
                return "";
        }

        return "";
    }

    public static long longIntNullValue() {
        return -9223372036854775808L;
    }

    public static long longIntMinValue() {
        return -9223372036854775807L;
    }

    public static long longIntMaxValue() {
        return 9223372036854775807L;
    }

    public static int priceId() {
        return 3;
    }

    public static String priceMetaAttribute(final MetaAttribute metaAttribute) {
        switch (metaAttribute) {
            case EPOCH:
                return "unix";
            case TIME_UNIT:
                return "nanosecond";
            case SEMANTIC_TYPE:
                return "";
        }

        return "";
    }

    public static double priceNullValue() {
        return Double.NaN;
    }

    public static double priceMinValue() {
        return 4.9E-324d;
    }

    public static double priceMaxValue() {
        return 1.7976931348623157E308d;
    }

    public static int flagId() {
        return 4;
    }

    public static String flagMetaAttribute(final MetaAttribute metaAttribute) {
        switch (metaAttribute) {
            case EPOCH:
                return "unix";
            case TIME_UNIT:
                return "nanosecond";
            case SEMANTIC_TYPE:
                return "";
        }

        return "";
    }

    public static int textId() {
        return 5;
    }

    public static String textMetaAttribute(final MetaAttribute metaAttribute) {
        switch (metaAttribute) {
            case EPOCH:
                return "unix";
            case TIME_UNIT:
                return "nanosecond";
            case SEMANTIC_TYPE:
                return "";
        }

        return "";
    }

    public static int sideId() {
        return 6;
    }

    public static String sideMetaAttribute(final MetaAttribute metaAttribute) {
        switch (metaAttribute) {
            case EPOCH:
                return "unix";
            case TIME_UNIT:
                return "nanosecond";
            case SEMANTIC_TYPE:
                return "";
        }

        return "";
    }

    public int sbeBlockLength() {
        return BLOCK_LENGTH;
    }

    public int sbeTemplateId() {
        return TEMPLATE_ID;
    }

    public int sbeSchemaId() {
        return SCHEMA_ID;
    }

    public int sbeSchemaVersion() {
        return SCHEMA_VERSION;
    }

    public String sbeSemanticType() {
        return "";
    }

    public int offset() {
        return offset;
    }

    public DataDecoder wrap(
            final DirectBuffer buffer, final int offset, final int actingBlockLength, final int actingVersion) {
        this.buffer = buffer;
        this.offset = offset;
        this.actingBlockLength = actingBlockLength;
        this.actingVersion = actingVersion;
        limit(offset + actingBlockLength);

        return this;
    }

    public int encodedLength() {
        return limit - offset;
    }

    public int limit() {
        return limit;
    }

    public void limit(final int limit) {
        buffer.checkLimit(limit);
        this.limit = limit;
    }

    public int smallInt() {
        return CodecUtil.int32Get(buffer, offset + 0, java.nio.ByteOrder.LITTLE_ENDIAN);
    }

    public long longInt() {
        return CodecUtil.int64Get(buffer, offset + 4, java.nio.ByteOrder.LITTLE_ENDIAN);
    }

    public double price() {
        return CodecUtil.doubleGet(buffer, offset + 12, java.nio.ByteOrder.LITTLE_ENDIAN);
    }

    public BooleanType flag() {
        return BooleanType.get(CodecUtil.uint8Get(buffer, offset + 20));
    }

    public VarDataEncodingDecoder text() {
        text.wrap(buffer, offset + 21);
        return text;
    }

    public Side side() {
        return Side.get(CodecUtil.uint8Get(buffer, offset + 22 + 16));
    }
}
