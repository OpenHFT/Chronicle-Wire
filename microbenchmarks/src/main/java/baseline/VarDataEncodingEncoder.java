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

public class VarDataEncodingEncoder {
    public static final int ENCODED_LENGTH = 1;
    private MutableDirectBuffer buffer;
    private int offset;

    public static short lengthNullValue() {
        return (short) 255;
    }

    public static short lengthMinValue() {
        return (short) 0;
    }

    public static short lengthMaxValue() {
        return (short) 254;
    }

    public static short varDataNullValue() {
        return (short) 255;
    }

    public static short varDataMinValue() {
        return (short) 0;
    }

    public static short varDataMaxValue() {
        return (short) 254;
    }

    public VarDataEncodingEncoder wrap(final MutableDirectBuffer buffer, final int offset) {
        this.buffer = buffer;
        this.offset = offset;
        return this;
    }

    public int encodedLength() {
        return ENCODED_LENGTH;
    }

    public VarDataEncodingEncoder length(final short value) {
        CodecUtil.uint8Put(buffer, offset + 0, value);
        return this;
    }

    // added helper methods.
    public void write(CharSequence cs) {
        CodecUtil.uint8Put(buffer, offset + 0, (short) cs.length());
        for (int i = 0; i < cs.length(); i++)
            CodecUtil.uint8Put(buffer, offset + 1 + i, (short) cs.charAt(i));
    }
}
