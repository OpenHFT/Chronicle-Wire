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
    /**
     * Sequence length 0 to 255 bytes.
     */
    public static final int BYTES_LENGTH8 = 0x80;

    /**
     * Sequence length 0 to 2^16-1 bytes.
     */
    public static final int BYTES_LENGTH16 = 0x81;

    /**
     * Sequence length 0 to 2^32-1 bytes.
     */
    public static final int BYTES_LENGTH32 = 0x82;

    /**
     * Indicates a HistoryMessage follows. was BYTES_MARSHALLABLE, but only used for this purpose
     */
    public static final int HISTORY_MESSAGE = 0x86;

    /**
     * Indicates a field anchor point within the serialized structure.
     */
    @Deprecated
    public static final int FIELD_ANCHOR = 0x87;

    /**
     * Indicates a general anchor point within the serialized structure.
     */
    @Deprecated
    public static final int ANCHOR = 0x88;

    /**
     * Denotes an updated alias value.
     */
    @Deprecated
    public static final int UPDATED_ALIAS = 0x89;

    /**
     * Array of unsigned bytes.
     */
    public static final int U8_ARRAY = 0x8A;
    //        public static final int U16_ARRAY = 0x8B;
    //        public static final int I32_ARRAY = 0x8C;
    /**
     * Array of 64-bit integers.
     */
    public static final int I64_ARRAY = 0x8D;

    /**
     * padding with a 32-bit length to ensure alignment.
     */
    public static final int PADDING32 = 0x8E;

    /**
     * Single byte padding to ensure alignment.
     */
    public static final int PADDING = 0x8F;

    /**
     * Represents a 32-bit floating-point number.
     */
    public static final int FLOAT32 = 0x90;

    /**
     * Represents a 64-bit floating-point number (double precision).
     */
    public static final int FLOAT64 = 0x91;

    /**
     * Floating-point number with 2 decimal places optimized for storage.
     */
    public static final int FLOAT_STOP_2 = 0x92;

    /**
     * Floating-point number with 4 decimal places optimized for storage.
     */
    public static final int FLOAT_STOP_4 = 0x94;

    /**
     * Floating-point number with 6 decimal places optimized for storage.
     */
    public static final int FLOAT_STOP_6 = 0x96;

    /**
     * Floating-point number rounded to the nearest whole number.
     */
    public static final int FLOAT_SET_LOW_0 = 0x9A;

    /**
     * Floating-point number rounded to 2 decimal places for compact storage.
     */
    public static final int FLOAT_SET_LOW_2 = 0x9B;

    /**
     * Floating-point number rounded to 4 decimal places for compact storage.
     */
    public static final int FLOAT_SET_LOW_4 = 0x9C;

    /**
     * Represents a universally unique identifier (UUID).
     */
    public static final int UUID = 0xA0;

    /**
     * Unsigned 8-bit integer (byte).
     */
    public static final int UINT8 = 0xA1;

    /**
     * Unsigned 16-bit integer (short).
     */
    public static final int UINT16 = 0xA2;

    /**
     * Unsigned 32-bit integer.
     */
    public static final int UINT32 = 0xA3;

    /**
     * Signed 8-bit integer (byte).
     */
    public static final int INT8 = 0xA4;

    /**
     * Signed 16-bit integer (short).
     */
    public static final int INT16 = 0xA5;

    /**
     * Signed 32-bit integer.
     */
    public static final int INT32 = 0xA6;

    /**
     * Signed 64-bit integer (long).
     */
    public static final int INT64 = 0xA7;

    /**
     * 8-bit integer with optimized storage for low positive values.
     */
    public static final int SET_LOW_INT8 = 0xA8;

    /**
     * 16-bit integer with optimized storage for low positive values.
     */
    public static final int SET_LOW_INT16 = 0xA9;
    //    public static final int FIXED_5 = 0xAA;
//    public static final int FIXED_4 = 0xAB;
//    public static final int FIXED_3 = 0xAC;
//    public static final int FIXED_2 = 0xAD;
//    public static final int FIXED_1 = 0xAE;
    /**
     * 64-bit integer to be displaying in hexadecimal format.
     */
    public static final int INT64_0x = 0xAF;

    /**
     * boolean value representing 'false'.
     */
    public static final int FALSE = 0xB0;

    /**
     * boolean value representing 'true'.
     */
    public static final int TRUE = 0xB1;

    /**
     * Represents a time value.
     */
    public static final int TIME = 0xB2;

    /**
     * Represents a date value.
     */
    public static final int DATE = 0xB3;

    /**
     * Represents a date-time value.
     */
    public static final int DATE_TIME = 0xB4;

    /**
     * Represents a zoned date-time value.
     */
    public static final int ZONED_DATE_TIME = 0xB5;

    /**
     * Prefix indicating the type of the following serialized object.
     */
    public static final int TYPE_PREFIX = 0xB6;

    /**
     * A field name of any length.
     */
    public static final int FIELD_NAME_ANY = 0xB7;

    /**
     * A String of any length
     */
    public static final int STRING_ANY = 0xB8;

    /**
     * A field of any length, marked as an event name.
     */
    public static final int EVENT_NAME = 0xB9;

    /**
     * Represents a numerical field number instead of a name.
     */
    public static final int FIELD_NUMBER = 0xBA;
    /**
     * Code representing a null value in the serialized data.
     */
    public static final int NULL = 0xBB;

    /**
     * Represents a type literal.
     */
    public static final int TYPE_LITERAL = 0xBC;

    /**
     * Indicates an event object instead of the typical string.
     */
    public static final int EVENT_OBJECT = 0xBD;

    /**
     * Marks a comment, not intended for parsing as part of the data structure.
     */
    public static final int COMMENT = 0xBE;

    /**
     * Provides a hint for serialization or deserialization processes, possibly affecting how data is interpreted.
     */
    public static final int HINT = 0xBF;

    /**
     * Starting code for predefined field names of length 0, 1, 2 ... 31.
     */
    public static final int FIELD_NAME0 = 0xC0;

    /**
     * Ending code for predefined field names.
     */
    public static final int FIELD_NAME31 = 0xDF;

    /**
     * Starting code for compact string representation of length 0, 1, 2 ... 31.
     */
    public static final int STRING_0 = 0xE0;

    /**
     * Ending code for compact string representation.
     */
    public static final int STRING_31 = 0xFF;

    /**
     * Array storing the string representations for each binary wire code, facilitating easier debugging and logging.
     */
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
