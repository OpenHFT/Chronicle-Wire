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

import java.lang.reflect.Field;

/**
 * Created by peter on 18/05/15.
 */
enum BinaryWireCode {
    ;
    // sequence of length 0 - 255 bytes
//        static final int BYTES_LENGTH8 = 0x80;
    // sequence of length 0 - 2^16-1 bytes
//        static final int BYTES_LENGTH16 = 0x81;
    // sequence of length 0 - 2^32-1
    static final int BYTES_LENGTH32 = 0x82;
    // sequence of length 0 - 255
//        static final int BYTES_LENGTH64 = 0x83;

    // an array of unsigned bytes
    static final int U8_ARRAY = 0x8A;
    //        static final int U16_ARRAY = 0x8B;
//        static final int I32_ARRAY = 0x8C;
    static final int I64_ARRAY = 0x8D;
    static final int PADDING32 = 0x8E;
    static final int PADDING = 0x8F;

    static final int FLOAT32 = 0x90;
    static final int FLOAT64 = 0x91;
//        static final int FIXED1 = 0x92;
//        static final int FIXED2 = 0x93;
//        static final int FIXED3 = 0x94;
//        static final int FIXED4 = 0x95;
//        static final int FIXED5 = 0x96;
//        static final int FIXED6 = 0x97;
    // 0x9A - 0x9F

    static final int UUID = 0xA0;
    static final int UINT8 = 0xA1;
    static final int UINT16 = 0xA2;
    static final int UINT32 = 0xA3;
    static final int INT8 = 0xA4;
    static final int INT16 = 0xA5;
    static final int INT32 = 0xA6;
    static final int INT64 = 0xA7;
//    static final int FIXED_6 = 0xA9;
//    static final int FIXED_5 = 0xAA;
//    static final int FIXED_4 = 0xAB;
//    static final int FIXED_3 = 0xAC;
//    static final int FIXED_2 = 0xAD;
//    static final int FIXED_1 = 0xAE;
//    static final int FIXED = 0xAF;

    static final int FALSE = 0xB0;
    static final int TRUE = 0xB1;
    static final int TIME = 0xB2;
    static final int DATE = 0xB3;
    static final int DATE_TIME = 0xB4;
    static final int ZONED_DATE_TIME = 0xB5;
    static final int TYPE_PREFIX = 0xB6;
    static final int FIELD_NAME_ANY = 0xB7;
    static final int STRING_ANY = 0xB8;
    static final int EVENT_NAME = 0xB9;
    static final int FIELD_NUMBER = 0xBA;
    static final int NULL = 0xBB;
    static final int TYPE_LITERAL = 0xBC;
    static final int COMMENT = 0xBE;
    static final int HINT = 0xBF;

    static final int FIELD_NAME0 = 0xC0;
    // ...
    static final int FIELD_NAME31 = 0xDF;

    static final int STRING_0 = 0xE0;
    // ...
    static final int STRING_31 = 0xFF;

    static final String[] STRING_FOR_CODE = new String[256];

    public static boolean isFieldCode(int code) {
        return code == FIELD_NAME_ANY ||
                code == FIELD_NUMBER ||
                (code >= FIELD_NAME0 && code <= FIELD_NAME31);
    }

    static {
        try {
            for (Field field : BinaryWireCode.class.getDeclaredFields()) {
                if (field.getType() == int.class)
                    STRING_FOR_CODE[field.getInt(null)] = field.getName();
            }
            for (int i = FIELD_NAME0; i <= FIELD_NAME31; i++)
                STRING_FOR_CODE[i] = "FIELD_" + i;
            for (int i = STRING_0; i <= STRING_31; i++)
                STRING_FOR_CODE[i] = "STRING_" + i;
            for (int i = 0; i < STRING_FOR_CODE.length; i++) {
                if (STRING_FOR_CODE[i] == null)
                    if (i <= ' ' && i >= 127)
                        STRING_FOR_CODE[i] = "Unknown_0x" + Integer.toHexString(i).toUpperCase();
                    else
                        STRING_FOR_CODE[i] = "Unknown_" + (char) i;
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static String stringForCode(int code) {
        return STRING_FOR_CODE[code];
    }
}
