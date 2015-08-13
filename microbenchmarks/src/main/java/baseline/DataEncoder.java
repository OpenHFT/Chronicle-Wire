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

/* Generated SBE (Simple Binary Encoding) message codec */
package baseline;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.sbe.codec.java.CodecUtil;

public class DataEncoder {
    // assumes text is 0 bytes long, but is up to 16.
    public static final int BLOCK_LENGTH = 23 + 16;
    public static final int TEMPLATE_ID = 1;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;

    private final DataEncoder parentMessage = this;
    private final VarDataEncodingEncoder text = new VarDataEncodingEncoder();
    protected int offset;
    protected int limit;
    protected int actingBlockLength;
    protected int actingVersion;
    private MutableDirectBuffer buffer;

    public static int smallIntNullValue() {
        return -2147483648;
    }

    public static int smallIntMinValue() {
        return -2147483647;
    }

    public static int smallIntMaxValue() {
        return 2147483647;
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

    public static double priceNullValue() {
        return Double.NaN;
    }

    public static double priceMinValue() {
        return 4.9E-324d;
    }

    public static double priceMaxValue() {
        return 1.7976931348623157E308d;
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

    public DataEncoder wrap(final MutableDirectBuffer buffer, final int offset) {
        this.buffer = buffer;
        this.offset = offset;
        limit(offset + BLOCK_LENGTH);
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

    public DataEncoder smallInt(final int value) {
        CodecUtil.int32Put(buffer, offset + 0, value, java.nio.ByteOrder.LITTLE_ENDIAN);
        return this;
    }

    public DataEncoder longInt(final long value) {
        CodecUtil.int64Put(buffer, offset + 4, value, java.nio.ByteOrder.LITTLE_ENDIAN);
        return this;
    }

    public DataEncoder price(final double value) {
        CodecUtil.doublePut(buffer, offset + 12, value, java.nio.ByteOrder.LITTLE_ENDIAN);
        return this;
    }

    public DataEncoder flag(final BooleanType value) {
        CodecUtil.uint8Put(buffer, offset + 20, value.value());
        return this;
    }

    public VarDataEncodingEncoder text() {
        text.wrap(buffer, offset + 21);
        return text;
    }

    public DataEncoder side(final Side value) {
        // text can be up to 16 bytes
        CodecUtil.uint8Put(buffer, offset + 22 + 16, value.value());
        return this;
    }
}
