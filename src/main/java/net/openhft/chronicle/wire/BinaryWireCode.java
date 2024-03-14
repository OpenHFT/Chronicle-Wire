/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Enumerates the predefined byte codes for the Binary YAML wire format.
 * Each constant in this class provides a specific purpose when working with the wire format,
 * enabling efficient serialization and deserialization processes.
 */
public enum BinaryWireCode {
    ; // Indicates no default enum instances

    // Definitions for sequence lengths:

    // Sequence of length ranging from 0 to 255 bytes.
    public static final int BYTES_LENGTH8 = 0x80;

    // Sequence of length ranging from 0 to 2^16-1 bytes.
    public static final int BYTES_LENGTH16 = 0x81;

    // Sequence of length ranging from 0 to 2^32-1 bytes.
    public static final int BYTES_LENGTH32 = 0x82;

    // Explicitly indicates BytesMarshallable sequences.
    public static final int BYTES_MARSHALLABLE = 0x86;

    // Indicates a field anchor point within the serialized structure.
    public static final int FIELD_ANCHOR = 0x87;

    // Indicates a general anchor point within the serialized structure.
    public static final int ANCHOR = 0x88;

    // Denotes an updated alias value.
    public static final int UPDATED_ALIAS = 0x89;

    // Definitions for array types:

    // Array of unsigned bytes.
    public static final int U8_ARRAY = 0x8A;
    //        public static final int U16_ARRAY = 0x8B;
//        public static final int I32_ARRAY = 0x8C;
    public static final int I64_ARRAY = 0x8D;

    // Padding sequences to ensure alignment.
    public static final int PADDING32 = 0x8E;
    public static final int PADDING = 0x8F;

    // Floating point number representations:
    public static final int FLOAT32 = 0x90;
    public static final int FLOAT64 = 0x91;
    public static final int FLOAT_STOP_2 = 0x92;
    public static final int FLOAT_STOP_4 = 0x94;
    public static final int FLOAT_STOP_6 = 0x96;
    public static final int FLOAT_SET_LOW_0 = 0x9A;
    public static final int FLOAT_SET_LOW_2 = 0x9B;
    public static final int FLOAT_SET_LOW_4 = 0x9C;
    // 0x98 - 0x9F

    // UUID representation.
    public static final int UUID = 0xA0;

    // Different integer representations.
    public static final int UINT8 = 0xA1;
    public static final int UINT16 = 0xA2;
    public static final int UINT32 = 0xA3;
    public static final int INT8 = 0xA4;
    public static final int INT16 = 0xA5;
    public static final int INT32 = 0xA6;
    public static final int INT64 = 0xA7;
    public static final int SET_LOW_INT8 = 0xA8;
    public static final int SET_LOW_INT16 = 0xA9;
    //    public static final int FIXED_5 = 0xAA;
//    public static final int FIXED_4 = 0xAB;
//    public static final int FIXED_3 = 0xAC;
//    public static final int FIXED_2 = 0xAD;
//    public static final int FIXED_1 = 0xAE;
    public static final int INT64_0x = 0xAF;

    // Representation for boolean values:
    // Represents the boolean value 'false'.
    public static final int FALSE = 0xB0;
    // Represents the boolean value 'true'.
    public static final int TRUE = 0xB1;

    // Different date and time representations:
    // Represents a time value.
    public static final int TIME = 0xB2;
    // Represents a date value.
    public static final int DATE = 0xB3;
    // Represents a date-time value.
    public static final int DATE_TIME = 0xB4;
    // Represents a zoned date-time value.
    public static final int ZONED_DATE_TIME = 0xB5;

    // Represents a type prefix.
    public static final int TYPE_PREFIX = 0xB6;

    // Denotes any field name.
    public static final int FIELD_NAME_ANY = 0xB7;

    // Denotes any string.
    public static final int STRING_ANY = 0xB8;

    // Represents an event name.
    public static final int EVENT_NAME = 0xB9;

    // Represents a field number.
    public static final int FIELD_NUMBER = 0xBA;

    // Represents a null value.
    public static final int NULL = 0xBB;

    // Represents a type literal.
    public static final int TYPE_LITERAL = 0xBC;

    // Denotes an event object.
    public static final int EVENT_OBJECT = 0xBD;

    // Denotes a comment.
    public static final int COMMENT = 0xBE;

    // Denotes a hint for serialization or deserialization.
    public static final int HINT = 0xBF;

    // Definitions for field names:
    // Represents the starting field name code.
    public static final int FIELD_NAME0 = 0xC0;
    // ...
    // Represents the ending field name code.
    public static final int FIELD_NAME31 = 0xDF;

    // Definitions for strings:
    // Represents the starting string code.
    public static final int STRING_0 = 0xE0;
    // ...
    // Represents the ending string code.
    public static final int STRING_31 = 0xFF;

    // Array containing string representations for each binary wire code.
    public static final String[] STRING_FOR_CODE = new String[256];

    // Static initializer to populate the STRING_FOR_CODE array:
    static {
        try {
            for (@NotNull Field field : BinaryWireCode.class.getDeclaredFields()) {
                if (field.getType() == int.class)
                    STRING_FOR_CODE[field.getInt(null)] = field.getName();
            }
            for (int i = FIELD_NAME0; i <= FIELD_NAME31; i++)
                STRING_FOR_CODE[i] = "FIELD_" + (i - FIELD_NAME0);
            for (int i = STRING_0; i <= STRING_31; i++)
                STRING_FOR_CODE[i] = "STRING_" + (i - STRING_0);
            for (int i = 0; i < STRING_FOR_CODE.length; i++) {
                if (STRING_FOR_CODE[i] == null)
                    if (i <= ' ' || i >= 127) {
                        STRING_FOR_CODE[i] = "Unknown_0x" + Integer.toHexString(i).toUpperCase();
                    } else {
                        STRING_FOR_CODE[i] = "Unknown_" + (char) i;
                    }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Determines if the provided code corresponds to a field name.
     *
     * @param code The binary wire code value.
     * @return True if the code corresponds to a field, false otherwise.
     */
    public static boolean isFieldCode(int code) {
        return code == FIELD_NAME_ANY ||
                code == FIELD_NUMBER ||
                (code >= FIELD_NAME0 && code <= FIELD_NAME31);
    }

    /**
     * Retrieves the string representation of a binary wire code.
     *
     * @param code The binary wire code value.
     * @return The string representation for the given code.
     */
    @NotNull
    public static String stringForCode(int code) {
        return code == -1 ? "EndOfFile" : STRING_FOR_CODE[code];
    }
}
